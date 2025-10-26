package me.hastwryn.atom;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class EventListener implements Listener {

    private final PlayerXPManager xpManager;
    private final Main plugin;
    private final Set<Player> craftingCooldown = new HashSet<>();

    public EventListener(Main plugin, PlayerXPManager xpManager) {
        this.plugin = plugin;
        this.xpManager = xpManager;
    }

    // Crafting restrictions
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack result = inv.getResult();
        if (result == null) return;
        if (!(event.getView().getPlayer() instanceof Player)) return;
        Player player = (Player) event.getView().getPlayer();

        Material mat = result.getType();

        int stoneLevel = 2;
        int copperLevel = 3;
        int ironLevel = 4;
        int diamondLevel = 5;

        // Blacksmith unlock checks (stone, copper, iron, diamond)
        if ((mat == Material.STONE_PICKAXE || mat == Material.STONE_AXE || mat == Material.STONE_SWORD
                || mat == Material.STONE_SHOVEL || mat == Material.STONE_HOE) && xpManager.getLevel(player, "blacksmith") < stoneLevel)
            inv.setResult(null);


        //Was testing in 1.21.10,
       //if ((mat == Material.COPPER_PICKAXE || mat == Material.COPPER_AXE || mat == Material.COPPER_SWORD
       //         || mat == Material.COPPER_SHOVEL || mat == Material.COPPER_HOE || mat == Material.COPPER_HELMET
       //         || mat == Material.COPPER_CHESTPLATE || mat == Material.COPPER_LEGGINGS || mat == Material.COPPER_BOOTS)
       //         && xpManager.getLevel(player, "blacksmith") < copperLevel)
       //     inv.setResult(null);

        if ((mat == Material.IRON_PICKAXE || mat == Material.IRON_AXE || mat == Material.IRON_SWORD
                || mat == Material.IRON_SHOVEL || mat == Material.IRON_HOE || mat == Material.IRON_HELMET
                || mat == Material.IRON_CHESTPLATE || mat == Material.IRON_LEGGINGS || mat == Material.IRON_BOOTS
                || mat == Material.SHIELD || mat == Material.SHEARS) && xpManager.getLevel(player, "blacksmith") < ironLevel)
            inv.setResult(null);

        if ((mat == Material.DIAMOND_PICKAXE || mat == Material.DIAMOND_AXE || mat == Material.DIAMOND_SWORD
                || mat == Material.DIAMOND_SHOVEL || mat == Material.DIAMOND_HOE || mat == Material.DIAMOND_HELMET
                || mat == Material.DIAMOND_CHESTPLATE || mat == Material.DIAMOND_LEGGINGS || mat == Material.DIAMOND_BOOTS
                || mat == Material.SMITHING_TABLE) && xpManager.getLevel(player, "blacksmith") < diamondLevel)
            inv.setResult(null);

        if (mat == Material.ANVIL && xpManager.getLevel(player, "blacksmith") < 2)
            inv.setResult(null);

        if (mat == Material.COMPOSTER && xpManager.getLevel(player, "farmer") < 3)
            inv.setResult(null);
    }

    // XP for crafting
    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack result = event.getRecipe() == null ? null : event.getRecipe().getResult();
        if (result == null) return;

        // Prevent multiple XP grants for the same click in the crafting table
        if (craftingCooldown.contains(player)) return;
        craftingCooldown.add(player);

        // removes CD
        Bukkit.getScheduler().runTaskLater(plugin, () -> craftingCooldown.remove(player), 1L);

        Material mat = result.getType();

        // Blacksmith XP
        int playerLevel = xpManager.getLevel(player, "blacksmith");
        int requiredLevel;

        switch (mat) {
            // Wooden tools
            case WOODEN_PICKAXE, WOODEN_AXE, WOODEN_SWORD, WOODEN_SHOVEL, WOODEN_HOE -> {
                requiredLevel = 1;
                if (playerLevel >= requiredLevel) xpManager.addXP(player, "blacksmith", 9, true);
            }

            // Stone tools
            case STONE_PICKAXE, STONE_AXE, STONE_SWORD, STONE_SHOVEL, STONE_HOE -> {
                requiredLevel = 2;
                if (playerLevel >= requiredLevel) xpManager.addXP(player, "blacksmith", 12,true);
            }

            // Copper tools & armor, only un comment if compiling for 1.21.10. maybe code something for this in the future???
           // case COPPER_PICKAXE, COPPER_AXE, COPPER_SWORD, COPPER_SHOVEL, COPPER_HOE,
           //      COPPER_HELMET, COPPER_CHESTPLATE, COPPER_LEGGINGS, COPPER_BOOTS -> {
            //    requiredLevel = 3;
            //    if (playerLevel >= requiredLevel) xpManager.addXP(player, "blacksmith", 15, true);
           // }

            // Iron tools & armor
            case IRON_PICKAXE, IRON_AXE, IRON_SWORD, IRON_SHOVEL, IRON_HOE,
                 IRON_HELMET, IRON_CHESTPLATE, IRON_LEGGINGS, IRON_BOOTS,
                 SHIELD, SHEARS -> {
                requiredLevel = 4;
                if (playerLevel >= requiredLevel) xpManager.addXP(player, "blacksmith", 20, true);
            }

            // Diamond tools & armor
            case DIAMOND_PICKAXE, DIAMOND_AXE, DIAMOND_SWORD, DIAMOND_SHOVEL, DIAMOND_HOE,
                 DIAMOND_HELMET, DIAMOND_CHESTPLATE, DIAMOND_LEGGINGS, DIAMOND_BOOTS,
                 SMITHING_TABLE -> {
                requiredLevel = 5;
                if (playerLevel >= requiredLevel) xpManager.addXP(player, "blacksmith", 25,true);
            }

            case ANVIL -> {
                requiredLevel = 3;
                if (playerLevel >= requiredLevel) xpManager.addXP(player, "blacksmith", 15,true);
            }

            default -> {
                // No XP for other items
            }
        }
    }


    // Block Break XP
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material block = event.getBlock().getType();

        switch (block) {
            // Stone blocks
            case STONE, DEEPSLATE -> xpManager.addXP(player, "miner", 2, true);

            // Ore blocks
            case COAL_ORE, DEEPSLATE_COAL_ORE,
                 IRON_ORE, DEEPSLATE_IRON_ORE,
                 COPPER_ORE, DEEPSLATE_COPPER_ORE,
                 GOLD_ORE, DEEPSLATE_GOLD_ORE,
                 REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE,
                 LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
                 DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
                 EMERALD_ORE, DEEPSLATE_EMERALD_ORE,
                 NETHER_GOLD_ORE, ANCIENT_DEBRIS,
                 NETHER_QUARTZ_ORE -> xpManager.addXP(player, "miner", 25, true);

            // Farming blocks
            case WHEAT, CARROTS, POTATOES, BEETROOTS,
                 NETHER_WART, SWEET_BERRY_BUSH,
                 PUMPKIN, MELON, COCOA, SUGAR_CANE,
                 BAMBOO, KELP, SEA_PICKLE, CACTUS -> xpManager.addXP(player, "farmer", 2, true);

                case RED_MUSHROOM, BROWN_MUSHROOM -> xpManager.addXP(player, "farmer", 2, true);

            // Grass
            case SHORT_GRASS, TALL_GRASS -> xpManager.addXP(player, "farmer", 1, true);

            // logs
            case OAK_LOG, SPRUCE_LOG, BIRCH_LOG, JUNGLE_LOG,
                 ACACIA_LOG, DARK_OAK_LOG, MANGROVE_LOG,
                 CHERRY_LOG, CRIMSON_STEM, WARPED_STEM -> xpManager.addXP(player, "farmer", 5, true);


        }
    }

    // Block PLacement XP
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material block = event.getBlockPlaced().getType();

        switch (block) {
            // All bricks
            case BRICKS,
                 STONE_BRICKS, MOSSY_STONE_BRICKS, CRACKED_STONE_BRICKS, CHISELED_STONE_BRICKS,
                 NETHER_BRICKS, RED_NETHER_BRICKS, CRACKED_NETHER_BRICKS, CHISELED_NETHER_BRICKS,
                 END_STONE_BRICKS, PURPUR_BLOCK,
                 PRISMARINE, PRISMARINE_BRICKS, DARK_PRISMARINE,
                 SANDSTONE, CUT_SANDSTONE, SMOOTH_SANDSTONE,
                 RED_SANDSTONE, CUT_RED_SANDSTONE, SMOOTH_RED_SANDSTONE,
                 QUARTZ_BLOCK, CHISELED_QUARTZ_BLOCK, SMOOTH_QUARTZ ->
                    xpManager.addXP(player, "builder", 10, true);


        }
    }


    // Block interactions (needs re do)
    @EventHandler
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Player player = event.getPlayer();
        Material type = block.getType();

        if (type == Material.ANVIL && xpManager.getLevel(player, "blacksmith") < 2) {
            event.setCancelled(true);
            player.sendActionBar((ChatColor.RED + "Not Enough Levels"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
        if (type == Material.COMPOSTER && xpManager.getLevel(player, "farmer") < 3) {
            event.setCancelled(true);
            player.sendActionBar((ChatColor.RED + "Not Enough Levels"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }

        // Grant XP for other interactable blocks
        if (type == Material.ENCHANTING_TABLE) xpManager.addXP(player, "librarian", 2, true);
        else if (type == Material.BREWING_STAND) xpManager.addXP(player, "healer", 3, true);
    }

    // entity interactions
    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        EntityType type = event.getRightClicked().getType();
        Player player = event.getPlayer();

        // Restrict low-level farmers
        if ((type == EntityType.COW || type == EntityType.CHICKEN || type == EntityType.SHEEP)
                && xpManager.getLevel(player, "farmer") < 2) {
            event.setCancelled(true);
            player.sendActionBar(ChatColor.RED + "Not Enough Levels");
            return;
        }

        // Reward XP for farming interactions
        switch (type) {
            case COW -> xpManager.addXP(player, "farmer", 5, true);        // Milking
            case SHEEP -> xpManager.addXP(player, "farmer", 5, true);       // Shearing
            case CHICKEN -> xpManager.addXP(player, "farmer", 5, true);     // Collecting eggs
            case PIG, HORSE, MULE, LLAMA, RABBIT -> xpManager.addXP(player, "farmer", 2, true); // Optional: animal care XP
        }
    }


    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player player)) return;
        EntityType type = event.getEntityType();

        // Every entity gives guardsman XP
        xpManager.addXP(player, "guardsman", 25, true);
    }


    // item use XP (for future implementation of bandages or custom items)
    @EventHandler
    public void onUseSpecialItem(PlayerInteractEvent event) {
        if (event.getItem() == null) return;
        Player player = event.getPlayer();
        Material item = event.getItem().getType();


    }
}
