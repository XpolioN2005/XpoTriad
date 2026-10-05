package dev.xpolion.xpotriad;

import dev.xpolion.xpotriad.ability.Ability;
import dev.xpolion.xpotriad.ability.AbilityEngine;
import dev.xpolion.xpotriad.ability.AbilityItem;
import dev.xpolion.xpotriad.ability.AbilityListener;
import dev.xpolion.xpotriad.fragment.FragmentItem;
import dev.xpolion.xpotriad.fragment.FragmentRegistry;
import dev.xpolion.xpotriad.fragment.fragments.*;
import dev.xpolion.xpotriad.particle.ParticleSystem;
import dev.xpolion.xpotriad.runtime.RuntimeManager;
import dev.xpolion.xpotriad.visual.EtchLoom;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private AbilityEngine abilityEngine;
    private EtchLoom etchLoom;
    private RuntimeManager runtimeManager;
    private ParticleSystem particleSystem;

    @Override
    public void onEnable() {
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

    public RuntimeManager getRuntimeManager() {
        return runtimeManager;
    }

    public ParticleSystem getParticleSystem() {
        return particleSystem;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        if (command.getName().equalsIgnoreCase("xpbind")) {
            etchLoom.open(player);
            return true;
        }

        if (!command.getName().equalsIgnoreCase("xptest")) {
            return false;
        }

        InvisibilityFragment invisibility =
                (InvisibilityFragment) FragmentRegistry.get("invisibility");
        SpeedFragment speed =
                (SpeedFragment) FragmentRegistry.get("speed");
        ExplosionFragment explosion =
                (ExplosionFragment) FragmentRegistry.get("explosion");
        MarkFragment mark = (MarkFragment) FragmentRegistry.get("mark");

        Ability ability = new Ability();

        ability.setFragment(Ability.Stage.PRE_CAST,  invisibility);
        ability.setFragment(Ability.Stage.CAST,      speed);
        ability.setFragment(Ability.Stage.POST_CAST, explosion);

        ItemStack weapon = new ItemStack(Material.DIAMOND_SWORD);

        AbilityItem.engrave(weapon, ability);

        player.getInventory().addItem(weapon);

        player.getInventory().addItem(FragmentItem.create(invisibility));
        player.getInventory().addItem(FragmentItem.create(speed));
        player.getInventory().addItem(FragmentItem.create(explosion));
        player.getInventory().addItem(FragmentItem.create(mark));

        return true;
    }
}