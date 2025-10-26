package me.hastwryn.atom;

import me.hastwryn.atom.commands.ClassCommand;
import me.hastwryn.atom.commands.ClassXPCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private PlayerXPManager xpManager;

    @Override
    public void onEnable() {
        // Load config
        saveDefaultConfig();
        reloadConfig();

        // start XP manager
        xpManager = new PlayerXPManager(this);

        // start event listeners
        Bukkit.getPluginManager().registerEvents(new EventListener(this, xpManager), this);

        // start commands
        PluginCommand classCmd = getCommand("class");
        if (classCmd != null) classCmd.setExecutor(new ClassCommand(xpManager));

        PluginCommand classXpCmd = getCommand("classxp");
        if (classXpCmd != null) {
            ClassXPCommand cmd = new ClassXPCommand(xpManager);
            classXpCmd.setExecutor(cmd);
            classXpCmd.setTabCompleter(cmd);
        }

        getLogger().info("Atom enabled!");
    }

    @Override
    public void onDisable() {
        if (xpManager != null) xpManager.close();
        getLogger().info("Atom disabled.");
    }

    public PlayerXPManager getXPManager() {
        return xpManager;
    }
}
