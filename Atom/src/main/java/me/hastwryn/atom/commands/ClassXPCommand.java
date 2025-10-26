package me.hastwryn.atom.commands;

import me.hastwryn.atom.PlayerXPManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ClassXPCommand implements CommandExecutor, TabCompleter {

    private final PlayerXPManager xpManager;

    public ClassXPCommand(PlayerXPManager xpManager) {
        this.xpManager = xpManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You must be an operator to use this command.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /classxp <reset|add|remove> <username> [class] [amount]");
            return true;
        }

        String action = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
            return true;
        }

        switch (action) {
            case "reset" -> {
                xpManager.resetXP(target);
                sender.sendMessage(ChatColor.GREEN + "Reset all XP for " + target.getName() + ".");
            }

            case "add", "remove" -> {
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /classxp " + action + " <username> <class> <amount>");
                    return true;
                }

                String profession = args[2].toLowerCase();
                double amount;

                try {
                    amount = Double.parseDouble(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid number: " + args[3]);
                    return true;
                }

                if (action.equals("add")) {
                    xpManager.addXP(target, profession, amount);
                    sender.sendMessage(ChatColor.GREEN + "Added " + amount + " XP to " + target.getName() + " (" + profession + ").");
                } else {
                    xpManager.removeXP(target, profession, amount);
                    sender.sendMessage(ChatColor.YELLOW + "Removed " + amount + " XP from " + target.getName() + " (" + profession + ").");
                }
            }

            default -> sender.sendMessage(ChatColor.RED + "Unknown action: " + action);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.isOp()) return Collections.emptyList();

        List<String> suggestions = new ArrayList<>();

        switch (args.length) {
            case 1 -> suggestions.addAll(List.of("reset", "add", "remove"));
            case 2 -> suggestions.addAll(
                    Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList())
            );
            case 3 -> suggestions.addAll(xpManager.getLoadedClasses());
            case 4 -> suggestions.addAll(List.of("100", "500", "1000", "2500"));
        }

        String current = args[args.length - 1].toLowerCase();
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(current))
                .collect(Collectors.toList());
    }
}
