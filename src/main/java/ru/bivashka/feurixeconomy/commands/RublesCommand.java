package ru.bivashka.feurixeconomy.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.bivashka.feurixeconomy.FeurixEconomy;
import ru.bivashka.feurixeconomy.economy.EconomyProvider.TransferResult;

import java.util.logging.Level;

public class RublesCommand implements CommandExecutor {

    private final FeurixEconomy plugin;

    public RublesCommand(FeurixEconomy plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        try {
            return handleCommand(sender, args);
        } catch (IllegalStateException e) {
            plugin.getLogger().log(Level.SEVERE, "Economy operation failed", e);
            sender.sendMessage(color("&cОперация не выполнена. Сообщите администратору сервера."));
            return true;
        }
    }

    private boolean handleCommand(CommandSender sender, String[] args) {
        // /rubles
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                long balance = plugin.getEconomy().getBalance(player.getName());
                player.sendMessage(color("&eВаш баланс: &a" + balance + "₽"));
            } else {
                sender.sendMessage("Только игроки могут проверять баланс без аргументов!");
            }
            return true;
        }

        // /rubles <sub> <player> <amount>
        if (args.length >= 2) {
            String sub = args[0].toLowerCase();
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

            if (target == null || target.getName() == null) {
                sender.sendMessage(color("&cИгрок не найден!"));
                return true;
            }

            // /rubles give <ник> <сумма>
            if (sub.equals("give") && args.length == 3) {
                if (!sender.hasPermission("feurixeconomy.admin")) {
                    sender.sendMessage(color("&cНет прав!"));
                    return true;
                }
                long amount = parseAmount(args[2], sender);
                if (amount < 0) return true;

                plugin.getEconomy().addBalance(target.getName(), amount);
                sender.sendMessage(color("&aВы выдали " + amount + "₽ игроку " + target.getName()));
                return true;
            }

            // /rubles take <ник> <сумма>
            if (sub.equals("take") && args.length == 3) {
                if (!sender.hasPermission("feurixeconomy.admin")) {
                    sender.sendMessage(color("&cНет прав!"));
                    return true;
                }
                long amount = parseAmount(args[2], sender);
                if (amount < 0) return true;

                plugin.getEconomy().takeBalance(target.getName(), amount);
                sender.sendMessage(color("&cВы забрали " + amount + "₽ у игрока " + target.getName()));
                return true;
            }

            // /rubles set <ник> <сумма>
            if (sub.equals("set") && args.length == 3) {
                if (!sender.hasPermission("feurixeconomy.admin")) {
                    sender.sendMessage(color("&cНет прав!"));
                    return true;
                }
                long amount = parseAmount(args[2], sender);
                if (amount < 0) return true;

                plugin.getEconomy().setBalance(target.getName(), amount);
                sender.sendMessage(color("&eБаланс игрока " + target.getName() + " установлен на " + amount + "₽"));
                return true;
            }

            // /rubles pay <ник> <сумма>
            if (sub.equals("pay") && args.length == 3 && sender instanceof Player) {
                Player player = (Player) sender;
                long amount = parseAmount(args[2], sender);
                if (amount < 0) return true;

                TransferResult result = plugin.getEconomy().transfer(player.getName(), target.getName(), amount);
                switch (result) {
                    case INVALID_AMOUNT:
                        player.sendMessage(color("&cСумма перевода должна быть больше нуля."));
                        return true;
                    case SAME_ACCOUNT:
                        player.sendMessage(color("&cНельзя переводить рубли самому себе."));
                        return true;
                    case INSUFFICIENT_FUNDS:
                        player.sendMessage(color("&cУ вас недостаточно средств!"));
                        return true;
                    case RECIPIENT_UNAVAILABLE:
                        player.sendMessage(color("&cПолучатель не найден в базе или его баланс переполнен."));
                        return true;
                    case SUCCESS:
                        player.sendMessage(color("&aВы перевели " + amount + "₽ игроку " + target.getName()));
                        if (target.isOnline()) {
                            target.getPlayer().sendMessage(color("&aВы получили " + amount + "₽ от " + player.getName()));
                        }
                        return true;
                    default:
                        throw new IllegalStateException("Unknown transfer result: " + result);
                }
            }
        }

        sender.sendMessage(color("&eИспользование: /rubles [give|take|set|pay] <ник> <сумма>"));
        return true;
    }

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private long parseAmount(String input, CommandSender sender) {
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            sender.sendMessage(color("&cНеверное число: " + input));
            return -1;
        }
    }
}
