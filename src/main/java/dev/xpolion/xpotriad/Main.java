package dev.xpolion.xpotriad;

import dev.xpolion.xpotriad.ability.AbilityEngine;
import dev.xpolion.xpotriad.ability.AbilityItem;
import dev.xpolion.xpotriad.ability.AbilityListener;
import dev.xpolion.xpotriad.command.XptCommand;
import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.fragment.FragmentItem;
import dev.xpolion.xpotriad.fragment.FragmentLootListener;
import dev.xpolion.xpotriad.particle.ParticleSystem;
import dev.xpolion.xpotriad.runtime.RuntimeManager;
import dev.xpolion.xpotriad.visual.EtchLoom;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private AbilityEngine abilityEngine;
    private EtchLoom etchLoom;
    private RuntimeManager runtimeManager;
    private ParticleSystem particleSystem;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadBalanceConfig();

        AbilityItem.initialize(this);
        FragmentItem.initialize(this);

        abilityEngine = new AbilityEngine(this);
        etchLoom = new EtchLoom(this);

        runtimeManager = new RuntimeManager(this);
        runtimeManager.start();

        particleSystem = new ParticleSystem(this);
        particleSystem.start();

        getServer().getPluginManager().registerEvents(
                new AbilityListener(abilityEngine),
                this
        );

        getServer().getPluginManager().registerEvents(
                new FragmentLootListener(),
                this
        );

        XptCommand xptCommand = new XptCommand(this, etchLoom);
        getCommand("xpt").setExecutor(xptCommand);
        getCommand("xpt").setTabCompleter(xptCommand);

        getLogger().info("XpoTriad enabled!");
    }

    @Override
    public void onDisable() {
        if (particleSystem != null) {
            particleSystem.stop();
        }
        if (runtimeManager != null) {
            runtimeManager.stop();
        }
        getLogger().info("XpoTriad disabled!");
    }

    /**
     * Re-reads config.yml and atomically installs a fresh BalanceConfig.
     * Safe to call at any time; running abilities keep their snapshot values.
     */
    public void reloadBalanceConfig() {
        reloadConfig();

        BalanceConfig balanceConfig = new BalanceConfig();
        balanceConfig.load(getConfig(), getLogger());
        BalanceConfig.set(balanceConfig);
    }

    public RuntimeManager getRuntimeManager() {
        return runtimeManager;
    }

    public ParticleSystem getParticleSystem() {
        return particleSystem;
    }
}