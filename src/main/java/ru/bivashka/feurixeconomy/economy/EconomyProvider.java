package ru.bivashka.feurixeconomy.economy;

import ru.bivashka.feurixeconomy.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * EconomyProvider — API для работы с валютой "rubles" по нику.
 */
public class EconomyProvider {

    private final DatabaseManager database;

    public EconomyProvider(DatabaseManager database) {
        this.database = database;
    }

    public long getBalance(String name) {
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT rubles FROM users WHERE username = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("rubles");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0L;
    }

    public void setBalance(String name, long amount) {
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE users SET rubles = ? WHERE username = ?")) {
            ps.setLong(1, amount);
            ps.setString(2, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addBalance(String name, long amount) {
        long current = getBalance(name);
        setBalance(name, current + amount);
    }

    public void takeBalance(String name, long amount) {
        long current = getBalance(name);
        setBalance(name, Math.max(0, current - amount));
    }
}
