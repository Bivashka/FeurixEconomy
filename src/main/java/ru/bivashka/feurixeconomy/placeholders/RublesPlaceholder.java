package ru.bivashka.feurixeconomy.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import ru.bivashka.feurixeconomy.FeurixEconomy;

public class RublesPlaceholder extends PlaceholderExpansion {

    private final FeurixEconomy plugin;

    public RublesPlaceholder(FeurixEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "rubles";
    }

    @Override
    public String getAuthor() {
        return "FeurixMC";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";

        if (identifier.equalsIgnoreCase("balance")) {
            long balance = plugin.getEconomy().getBalance(player.getName());
            return String.valueOf(balance);
        }

        return null;
    }
}
