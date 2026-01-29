package vn.casino.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import vn.casino.economy.CurrencyManager;
import vn.casino.economy.TransactionType;
import vn.casino.i18n.MessageKey;
import vn.casino.i18n.MessageManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Casino economy management command.
 * Requires casino.economy permission.
 *
 * Commands:
 * - /casinoeco give <player> <amount> - Give VND to player
 * - /casinoeco take <player> <amount> - Take VND from player
 * - /casinoeco set <player> <amount> - Set player balance
 * - /casinoeco check <player> - Check player balance
 * - /casinoeco top [count] - Show top balances
 */
public class EconomyCommand implements CommandExecutor, TabCompleter {

    private final CurrencyManager currencyManager;
    private final MessageManager messageManager;

    public EconomyCommand(
        CurrencyManager currencyManager,
        MessageManager messageManager
    ) {
        this.currencyManager = currencyManager;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("casino.economy")) {
            sender.sendMessage(messageManager.getRawMessage(MessageKey.GENERAL_NO_PERMISSION));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "take" -> handleTake(sender, args);
            case "set" -> handleSet(sender, args);
            case "check" -> handleCheck(sender, args);
            case "top" -> handleTop(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /casinoeco give <player> <amount>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage("§c✗ Player not found or offline: " + args[1]);
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(args[2]);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                sender.sendMessage("§c✗ Amount must be positive");
                return;
            }

            currencyManager.deposit(
                target.getUniqueId(),
                amount,
                TransactionType.ADMIN_GIVE,
                null,
                null,
                "Admin give by " + sender.getName()
            ).thenAccept(newBalance -> {
                sender.sendMessage(String.format("§a✓ Gave %s VND to %s (New balance: %s)",
                    amount, target.getName(), newBalance));
                target.sendMessage(String.format("§a✓ You received %s VND from an administrator", amount));
            }).exceptionally(ex -> {
                sender.sendMessage("§c✗ Failed to give currency: " + ex.getMessage());
                return null;
            });

        } catch (NumberFormatException e) {
            sender.sendMessage("§c✗ Invalid amount. Must be a number.");
        }
    }

    private void handleTake(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /casinoeco take <player> <amount>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage("§c✗ Player not found or offline: " + args[1]);
            return;
        }

        try {
            BigDecimal amount = new BigDecimal(args[2]);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                sender.sendMessage("§c✗ Amount must be positive");
                return;
            }

            currencyManager.withdraw(
                target.getUniqueId(),
                amount,
                TransactionType.ADMIN_TAKE,
                null,
                null,
                "Admin take by " + sender.getName()
            ).thenAccept(newBalance -> {
                sender.sendMessage(String.format("§a✓ Took %s VND from %s (New balance: %s)",
                    amount, target.getName(), newBalance));
                target.sendMessage(String.format("§c%s VND was removed from your balance by an administrator", amount));
            }).exceptionally(ex -> {
                sender.sendMessage("§c✗ Failed to take currency: " + ex.getMessage());
                return null;
            });

        } catch (NumberFormatException e) {
            sender.sendMessage("§c✗ Invalid amount. Must be a number.");
        }
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /casinoeco set <player> <amount>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage("§c✗ Player not found or offline: " + args[1]);
            return;
        }

        try {
            BigDecimal newBalance = new BigDecimal(args[2]);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                sender.sendMessage("§c✗ Balance cannot be negative");
                return;
            }

            currencyManager.setBalance(
                target.getUniqueId(),
                newBalance,
                "Admin set by " + sender.getName()
            ).thenAccept(balance -> {
                sender.sendMessage(String.format("§a✓ Set %s's balance to %s VND",
                    target.getName(), balance));
                target.sendMessage(String.format("§eYour balance was set to %s VND by an administrator", balance));
            }).exceptionally(ex -> {
                sender.sendMessage("§c✗ Failed to set balance: " + ex.getMessage());
                return null;
            });

        } catch (NumberFormatException e) {
            sender.sendMessage("§c✗ Invalid amount. Must be a number.");
        }
    }

    private void handleCheck(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /casinoeco check <player>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

        currencyManager.getBalance(target.getUniqueId()).thenAccept(balance -> {
            sender.sendMessage(String.format("§6%s's balance: §e%s VND",
                target.getName() != null ? target.getName() : "Unknown", balance));
        }).exceptionally(ex -> {
            sender.sendMessage("§c✗ Failed to check balance: " + ex.getMessage());
            return null;
        });
    }

    private void handleTop(CommandSender sender, String[] args) {
        int count = 10;
        if (args.length > 1) {
            try {
                count = Integer.parseInt(args[1]);
                count = Math.min(Math.max(count, 1), 50); // Limit between 1-50
            } catch (NumberFormatException e) {
                sender.sendMessage("§c✗ Invalid count. Using default: 10");
            }
        }

        sender.sendMessage("§6§l=== Top " + count + " Richest Players ===");
        sender.sendMessage("§7(Top balances feature not yet implemented)");
        // Note: This would require a leaderboard query from the database
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§6§lCasino Economy Commands:");
        sender.sendMessage("§e/casinoeco give <player> <amount> §7- Give VND to player");
        sender.sendMessage("§e/casinoeco take <player> <amount> §7- Take VND from player");
        sender.sendMessage("§e/casinoeco set <player> <amount> §7- Set player balance");
        sender.sendMessage("§e/casinoeco check <player> §7- Check player balance");
        sender.sendMessage("§e/casinoeco top [count] §7- Show top balances");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("casino.economy")) {
            return List.of();
        }

        if (args.length == 1) {
            return filterCompletions(args[0], List.of("give", "take", "set", "check", "top"));
        }

        if (args.length == 2 && !args[0].equalsIgnoreCase("top")) {
            // Tab complete online player names
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }

        return List.of();
    }

    private List<String> filterCompletions(String input, List<String> options) {
        List<String> completions = new ArrayList<>();
        String lowercaseInput = input.toLowerCase();

        for (String option : options) {
            if (option.toLowerCase().startsWith(lowercaseInput)) {
                completions.add(option);
            }
        }

        return completions;
    }
}
