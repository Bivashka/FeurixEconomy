package ru.bivashka.feurixeconomy;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.bivashka.feurixeconomy.commands.RublesCommand;
import ru.bivashka.feurixeconomy.db.DatabaseManager;
import ru.bivashka.feurixeconomy.economy.EconomyProvider;
import ru.bivashka.feurixeconomy.placeholders.RublesPlaceholder;

public class FeurixEconomy extends JavaPlugin {

    private static FeurixEconomy instance;
    private DatabaseManager database;
    private EconomyProvider economy;

    @Override
    public void onEnable() {
        instance = this;

        // Загружаем конфиг, если его ещё нет
        saveDefaultConfig();

        // Запускаем базу данных
        this.database = new DatabaseManager(this);
        this.economy = new EconomyProvider(database);

        // Регистрируем команду /rubles
        if (getCommand("rubles") != null) {
            getCommand("rubles").setExecutor(new RublesCommand(this));
        }

        // Регистрируем PlaceholderAPI
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new RublesPlaceholder(this).register();
            getLogger().info("Registered PlaceholderAPI expansion: %rubles_balance%");
        }

        getLogger().info("FeurixEconomy enabled!");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }
        getLogger().info("FeurixEconomy disabled!");
    }

    public static FeurixEconomy getInstance() {
        return instance;
    }

    public DatabaseManager getDatabase() {
        return database;
    }

    public EconomyProvider getEconomy() {
        return economy;
    }
}