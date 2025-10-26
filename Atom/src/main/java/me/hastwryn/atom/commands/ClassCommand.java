package me.hastwryn.atom.commands;

import me.hastwryn.atom.PlayerXPManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ClassCommand implements CommandExecutor, Listener {

    private final PlayerXPManager xpManager;

    public ClassCommand(PlayerXPManager xpManager) {
        this.xpManager = xpManager;
        Bukkit.getPluginManager().registerEvents(this, Bukkit.getPluginManager().getPlugin("Adam"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }
        Player player = (Player) sender;
        openClassGUI(player);
        return true;
    }

    private void openClassGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, ChatColor.GOLD + "Classes");

        String[] professions = {"miner", "blacksmith", "farmer", "librarian", "guardsman", "builder", "healer"};
        Material[] icons = {
                Material.COAL, Material.FURNACE, Material.HAY_BLOCK, Material.BOOKSHELF,
                Material.SKELETON_SKULL, Material.OAK_PLANKS, Material.ENDER_EYE
        };

        int startColumn = 1;


        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(ChatColor.RED + "Close");
            closeButton.setItemMeta(closeMeta);
        }
        gui.setItem(0 * 9 + 8, closeButton);

        for (int i = 0; i < professions.length; i++) {
            String prof = professions[i];
            double totalXP = xpManager.getTotalXP(player, prof);
            int level = xpManager.calculateLevelFromTotalXP(prof, totalXP);

            double xpInLevel = xpManager.getProgressInLevel(prof, totalXP);
            double xpNeeded = xpManager.getXPNeededForNextLevel(prof, totalXP);

            if (xpNeeded <= 0) xpNeeded = 1;

            int totalPanes = 3;
            int filled = (int) Math.round((xpInLevel / xpNeeded) * totalPanes);
            filled = Math.min(filled, totalPanes);

            int column = startColumn + i;


            for (int j = 0; j < totalPanes; j++) {
                int row = 3 - j;
                int slot = row * 9 + column;

                boolean isFilled = j < filled;
                Material mat = isFilled ? Material.GREEN_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;

                ItemStack pane = new ItemStack(mat);
                ItemMeta meta = pane.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.YELLOW + capitalize(prof) + " XP:");
                    meta.setLore(Arrays.asList(
                            ChatColor.GRAY + "" + (int) xpInLevel + "/" + (int) xpNeeded + " XP in this level",
                            ChatColor.DARK_GRAY + "Total XP: " + (int) totalXP
                    ));
                    pane.setItemMeta(meta);
                }
                gui.setItem(slot, pane);
            }


            int iconSlot = 4 * 9 + column;
            ItemStack icon = new ItemStack(icons[i], Math.min(level, 64));
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.AQUA + capitalize(prof));
                meta.setLore(Arrays.asList(
                        ChatColor.YELLOW + "Level: " + level,
                        ChatColor.GRAY + "XP in level: " + (int) xpInLevel + "/" + (int) xpNeeded,
                        ChatColor.DARK_GRAY + "Total XP: " + (int) totalXP
                ));
                icon.setItemMeta(meta);
            }
            gui.setItem(iconSlot, icon);
        }

        player.openInventory(gui);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        if (title.equals(ChatColor.GOLD + "Classes")) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) return;

            String name = ChatColor.stripColor(meta.getDisplayName());

            if (clicked.getType() == Material.BARRIER && name.equalsIgnoreCase("Close")) {
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.equals(ChatColor.GOLD + "Classes")) {
            event.setCancelled(true);
        }
    }
}
