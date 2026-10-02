package dev.xpolion.xpotriad;

import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("XpoTriad enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("XpoTriad disabled!");
    }
}