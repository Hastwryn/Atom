package me.hastwryn.atom;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;

public class PlayerXPManager {

    private final JavaPlugin plugin;
    private Connection connection;
    private final Map<Integer, Integer> skillUpgradeRequirements = new HashMap<>();
    private final Map<String, Map<Integer, Integer>> levelRequirements = new HashMap<>();
    private double xpBalanceFactor = 4.0; // default

    public PlayerXPManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        initDatabase();
    }

    private void loadConfig() {
        xpBalanceFactor = plugin.getConfig().getDouble("xp-balance-factor", 4.0);

        // Load skill upgrade requirements
        ConfigurationSection upgrades = plugin.getConfig().getConfigurationSection("skill-upgrade-requirements");
        if (upgrades != null) {
            for (String key : upgrades.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    int xp = upgrades.getInt(key);
                    skillUpgradeRequirements.put(level, xp);
                } catch (NumberFormatException ignored) {}
            }
        }
        plugin.getLogger().info("Loaded " + skillUpgradeRequirements.size() + " skill upgrade requirements.");

        // Load class XP requirements
        ConfigurationSection classes = plugin.getConfig().getConfigurationSection("level-requirements");
        if (classes != null) {
            for (String profession : classes.getKeys(false)) {
                ConfigurationSection section = classes.getConfigurationSection(profession);
                if (section == null) continue;

                Map<Integer, Integer> levels = new HashMap<>();
                for (String levelKey : section.getKeys(false)) {
                    try {
                        int level = Integer.parseInt(levelKey);
                        int xp = section.getInt(levelKey);
                        levels.put(level, xp);
                    } catch (NumberFormatException ignored) {}
                }
                levelRequirements.put(profession.toLowerCase(), levels);
            }
        }

        plugin.getLogger().info("Loaded XP balance factor = " + xpBalanceFactor);
        plugin.getLogger().info("Loaded " + levelRequirements.size() + " class level tables.");
    }

    private void initDatabase() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "playerdata.db");
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS player_xp (" +
                        "uuid TEXT NOT NULL, " +
                        "profession TEXT NOT NULL, " +
                        "total_xp REAL DEFAULT 0, " +
                        "level INTEGER DEFAULT 1, " +
                        "PRIMARY KEY (uuid, profession));");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // logic for xp/levels

    public void addXP(Player player, String profession, double amount, boolean applyBalancing) {
        if (amount == 0) return;
        ensureAllClasses(player);

        double totalXP = getTotalXP(player, profession) + amount;
        if (totalXP < 0) totalXP = 0;
        setTotalXP(player, profession, totalXP);

        int oldLevel = getLevel(player, profession);
        int newLevel = calculateLevelFromTotalXP(profession, totalXP);

        if (newLevel > oldLevel) {
            setLevel(player, profession, newLevel);
            player.sendActionBar("§6§lLEVEL UP! §r§eYour " + capitalize(profession) + " level is now " + newLevel + "!");
        } else if (newLevel < oldLevel) {
            setLevel(player, profession, newLevel);
            player.sendActionBar("§c§lLEVEL DOWN! §r§eYour " + capitalize(profession) + " level is now " + newLevel + "!");
        } else {
            player.sendActionBar((amount >= 0 ? "+" : "") + amount + " " + capitalize(profession) + " XP");
        }

        if (applyBalancing) applyXPBalancing(player, profession, amount);

        plugin.getLogger().info("[DEBUG] " + player.getName() + " " + profession +
                " totalXP=" + totalXP + ", level=" + newLevel);
    }

    public void addXP(Player player, String profession, double amount) {
        addXP(player, profession, amount, true);
    }

    public void removeXP(Player player, String profession, double amount) {
        addXP(player, profession, -amount, true);
    }

    public int calculateLevelFromTotalXP(String profession, double totalXP) {
        Map<Integer, Integer> table = levelRequirements.getOrDefault(profession.toLowerCase(), Collections.emptyMap());
        int level = 1;
        double accumulated = 0;

        for (int l = 1; l <= table.size(); l++) {
            int required = table.getOrDefault(l, 1000 + l * 500);
            accumulated += required;
            if (totalXP < accumulated) break;
            level = l;
        }
        return level;
    }

    public double getProgressInLevel(String profession, double totalXP) {
        Map<Integer, Integer> table = levelRequirements.getOrDefault(profession.toLowerCase(), Collections.emptyMap());
        double accumulated = 0;
        for (int l = 1; l <= table.size(); l++) {
            int required = table.getOrDefault(l, 1000 + l * 500);
            if (totalXP < accumulated + required) {
                return totalXP - accumulated;
            }
            accumulated += required;
        }
        return 0;
    }

    public double getXPNeededForNextLevel(String profession, double totalXP) {
        Map<Integer, Integer> table = levelRequirements.getOrDefault(profession.toLowerCase(), Collections.emptyMap());
        double accumulated = 0;
        for (int l = 1; l <= table.size(); l++) {
            int required = table.getOrDefault(l, 1000 + l * 500);
            if (totalXP < accumulated + required) {
                return required;
            }
            accumulated += required;
        }
        return 0;
    }

    private void applyXPBalancing(Player player, String mainProfession, double gainedXP) {
        if (xpBalanceFactor <= 0) return;
        if (gainedXP <= 0) return;

        double xpLoss = gainedXP / xpBalanceFactor;
        if (xpLoss <= 0) return;

        Map<String, Double> all = getAllTotalXP(player);
        for (String profession : all.keySet()) {
            if (!profession.equalsIgnoreCase(mainProfession)) {
                double current = all.get(profession);
                double newXP = Math.max(0, current - xpLoss);
                setTotalXP(player, profession, newXP);

                int newLevel = calculateLevelFromTotalXP(profession, newXP);
                setLevel(player, profession, newLevel);

                plugin.getLogger().info("[DEBUG] XP Balancing: -" + xpLoss + " from " + profession +
                        " (now " + newXP + " total XP, level " + newLevel + ")");
            }
        }
    }



    public void setTotalXP(Player player, String profession, double amount) {
        amount = Math.max(0, amount);
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO player_xp (uuid, profession, total_xp, level) VALUES (?, ?, ?, 1) " +
                        "ON CONFLICT(uuid, profession) DO UPDATE SET total_xp = excluded.total_xp;")) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, profession.toLowerCase());
            ps.setDouble(3, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public double getTotalXP(Player player, String profession) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT total_xp FROM player_xp WHERE uuid = ? AND profession = ?")) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, profession.toLowerCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("total_xp");
            setTotalXP(player, profession, 0);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Map<String, Double> getAllTotalXP(Player player) {
        Map<String, Double> map = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT profession, total_xp FROM player_xp WHERE uuid = ?")) {
            ps.setString(1, player.getUniqueId().toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                map.put(rs.getString("profession"), rs.getDouble("total_xp"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    public void setLevel(Player player, String profession, int level) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO player_xp (uuid, profession, total_xp, level) VALUES (?, ?, 0, ?) " +
                        "ON CONFLICT(uuid, profession) DO UPDATE SET level = excluded.level;")) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, profession.toLowerCase());
            ps.setInt(3, level);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getLevel(Player player, String profession) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT level FROM player_xp WHERE uuid = ? AND profession = ?")) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, profession.toLowerCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("level");
            setLevel(player, profession, 1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1;
    }

    public void resetXP(Player player) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM player_xp WHERE uuid = ?")) {
            ps.setString(1, player.getUniqueId().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // utils

    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public List<String> getLoadedClasses() {
        return new ArrayList<>(levelRequirements.keySet());
    }

    public void ensureAllClasses(Player player) {
        for (String profession : getLoadedClasses()) {
            getTotalXP(player, profession);
        }
    }

    public String getPrimaryClass(Player player) {
        Map<String, Double> xpMap = getAllTotalXP(player);
        return xpMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public int getSkillUpgradeRequirement(int upgradeLevel) {
        return skillUpgradeRequirements.getOrDefault(upgradeLevel, upgradeLevel * 100);
    }

    public Map<Integer, Integer> getSkillUpgradeRequirements() {
        Map<Integer, Integer> map = new HashMap<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("skill-upgrade-requirements");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    int tier = Integer.parseInt(key);
                    int xp = section.getInt(key);
                    map.put(tier, xp);
                } catch (NumberFormatException ignored) {}
            }
        }
        return map;
    }
}
