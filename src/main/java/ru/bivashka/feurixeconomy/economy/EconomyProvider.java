package ru.bivashka.feurixeconomy.economy;

import ru.bivashka.feurixeconomy.db.DatabaseManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/** Operates on the existing users.rubles balance. */
public final class EconomyProvider {

    public enum TransferResult {
        SUCCESS,
        INVALID_AMOUNT,
        SAME_ACCOUNT,
        INSUFFICIENT_FUNDS,
        RECIPIENT_UNAVAILABLE
    }

    private final DataSource dataSource;

    public EconomyProvider(DatabaseManager database) {
        this(Objects.requireNonNull(database, "database").getDataSource());
    }

    public EconomyProvider(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    public long getBalance(String name) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT rubles FROM users WHERE username = ?")) {
            statement.setString(1, name);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong("rubles") : 0L;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to read economy balance", e);
        }
    }

    public void setBalance(String name, long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Balance must not be negative");
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET rubles = ? WHERE username = ?")) {
            statement.setLong(1, amount);
            statement.setString(2, name);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to set economy balance", e);
        }
    }

    public void addBalance(String name, long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must not be negative");
        }
        if (amount == 0) return;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET rubles = rubles + ? " +
                     "WHERE username = ? AND rubles >= 0 AND rubles <= ?")) {
            statement.setLong(1, amount);
            statement.setString(2, name);
            statement.setLong(3, Long.MAX_VALUE - amount);
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("Account missing or balance would overflow");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to add economy balance", e);
        }
    }

    public void takeBalance(String name, long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must not be negative");
        }
        if (amount == 0) return;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET rubles = GREATEST(0, rubles - ?) WHERE username = ?")) {
            statement.setLong(1, amount);
            statement.setString(2, name);
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("Account not found");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to remove economy balance", e);
        }
    }

    /**
     * Debit and credit are committed together; a missing receiver or a failed
     * credit leaves the sender's balance untouched.
     */
    public TransferResult transfer(String sender, String recipient, long amount) {
        if (amount <= 0) return TransferResult.INVALID_AMOUNT;
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(recipient, "recipient");
        if (sender.equalsIgnoreCase(recipient)) return TransferResult.SAME_ACCOUNT;

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement debit = connection.prepareStatement(
                        "UPDATE users SET rubles = rubles - ? " +
                        "WHERE username = ? AND rubles >= ?")) {
                    debit.setLong(1, amount);
                    debit.setString(2, sender);
                    debit.setLong(3, amount);
                    if (debit.executeUpdate() != 1) {
                        connection.rollback();
                        return TransferResult.INSUFFICIENT_FUNDS;
                    }
                }

                try (PreparedStatement credit = connection.prepareStatement(
                        "UPDATE users SET rubles = rubles + ? " +
                        "WHERE username = ? AND rubles >= 0 AND rubles <= ?")) {
                    credit.setLong(1, amount);
                    credit.setString(2, recipient);
                    credit.setLong(3, Long.MAX_VALUE - amount);
                    if (credit.executeUpdate() != 1) {
                        connection.rollback();
                        return TransferResult.RECIPIENT_UNAVAILABLE;
                    }
                }

                connection.commit();
                return TransferResult.SUCCESS;
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to transfer economy balance", e);
        }
    }
}
