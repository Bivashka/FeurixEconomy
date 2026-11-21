package ru.bivashka.feurixeconomy.economy;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EconomyProviderTest {

    private JdbcDataSource dataSource;
    private EconomyProvider economy;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:eco_" + UUID.randomUUID().toString().replace("-", "") +
                ";MODE=MySQL;DB_CLOSE_DELAY=-1");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE users (username VARCHAR(64) PRIMARY KEY, " +
                    "rubles BIGINT NOT NULL DEFAULT 0)");
        }
        economy = new EconomyProvider(dataSource);
    }

    private void createAccount(String name, long balance) throws Exception {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO users (username, rubles) VALUES (?, ?)")) {
            statement.setString(1, name);
            statement.setLong(2, balance);
            statement.executeUpdate();
        }
    }

    @Test
    void successfulTransferUpdatesBothAccounts() throws Exception {
        createAccount("Alice", 100);
        createAccount("Bob", 20);

        assertEquals(EconomyProvider.TransferResult.SUCCESS,
                economy.transfer("Alice", "Bob", 30));
        assertEquals(70, economy.getBalance("Alice"));
        assertEquals(50, economy.getBalance("Bob"));
    }

    @Test
    void insufficientFundsLeaveBalancesUntouched() throws Exception {
        createAccount("Alice", 10);
        createAccount("Bob", 20);

        assertEquals(EconomyProvider.TransferResult.INSUFFICIENT_FUNDS,
                economy.transfer("Alice", "Bob", 11));
        assertEquals(10, economy.getBalance("Alice"));
        assertEquals(20, economy.getBalance("Bob"));
    }

    @Test
    void missingRecipientRollsBackDebit() throws Exception {
        createAccount("Alice", 100);

        assertEquals(EconomyProvider.TransferResult.RECIPIENT_UNAVAILABLE,
                economy.transfer("Alice", "Missing", 30));
        assertEquals(100, economy.getBalance("Alice"));
    }

    @Test
    void overflowingRecipientRollsBackDebit() throws Exception {
        createAccount("Alice", 100);
        createAccount("Bob", Long.MAX_VALUE - 1);

        assertEquals(EconomyProvider.TransferResult.RECIPIENT_UNAVAILABLE,
                economy.transfer("Alice", "Bob", 5));
        assertEquals(100, economy.getBalance("Alice"));
        assertEquals(Long.MAX_VALUE - 1, economy.getBalance("Bob"));
    }

    @Test
    void rejectsSelfTransfersAndInvalidAmounts() throws Exception {
        createAccount("Alice", 100);

        assertEquals(EconomyProvider.TransferResult.SAME_ACCOUNT,
                economy.transfer("Alice", "alice", 5));
        assertEquals(EconomyProvider.TransferResult.INVALID_AMOUNT,
                economy.transfer("Alice", "Bob", 0));
        assertEquals(EconomyProvider.TransferResult.INVALID_AMOUNT,
                economy.transfer("Alice", "Bob", -10));
        assertEquals(100, economy.getBalance("Alice"));
    }

    @Test
    void administratorChangesAreAtomicAndCannotOverflow() throws Exception {
        createAccount("Alice", Long.MAX_VALUE - 1);

        assertThrows(IllegalStateException.class, () -> economy.addBalance("Alice", 5));
        assertEquals(Long.MAX_VALUE - 1, economy.getBalance("Alice"));
        economy.takeBalance("Alice", Long.MAX_VALUE);
        assertEquals(0, economy.getBalance("Alice"));
    }

    @Test
    void databaseFailureMustNotLookLikeZeroBalance() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE users");
        }
        assertThrows(IllegalStateException.class, () -> economy.getBalance("Alice"));
    }
}
