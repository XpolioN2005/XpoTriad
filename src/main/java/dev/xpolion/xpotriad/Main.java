package dev.xpolion.xpotriad;

import dev.xpolion.xpotriad.ability.AbilityEngine;
import dev.xpolion.xpotriad.ability.AbilityItem;
import dev.xpolion.xpotriad.ability.AbilityListener;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.FragmentItem;
import dev.xpolion.xpotriad.fragment.FragmentLootListener;
import dev.xpolion.xpotriad.fragment.FragmentRegistry;
import dev.xpolion.xpotriad.particle.ParticleSystem;
import dev.xpolion.xpotriad.runtime.RuntimeManager;
import dev.xpolion.xpotriad.visual.EtchLoom;
import org.bukkit.Material;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

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

        getServer().getPluginManager().registerEvents(
        new FragmentLootListener(),
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

        // Chest(s) full of every registered fragment — nothing else is given.
        // A single chest holds 27 slots; additional chests cover any overflow.
        List<ItemStack> fragmentItems = new ArrayList<>();

        for (Fragment fragment : FragmentRegistry.all()) {
            fragmentItems.add(FragmentItem.create(fragment));
        }

        int chestCount = (fragmentItems.size() + 26) / 27;

        for (int chestIndex = 0; chestIndex < chestCount; chestIndex++) {
            ItemStack chestItem = new ItemStack(Material.CHEST);
            BlockStateMeta meta = (BlockStateMeta) chestItem.getItemMeta();

            if (meta != null) {
                Chest chest = (Chest) meta.getBlockState();

                int from = chestIndex * 27;
                int to = Math.min(from + 27, fragmentItems.size());

                for (int i = from; i < to; i++) {
                    chest.getInventory().addItem(fragmentItems.get(i));
                }

                meta.setBlockState(chest);
                chestItem.setItemMeta(meta);
            }

            player.getInventory().addItem(chestItem);
        }

        return true;
    }
}