package dev.xpolion.xpotriad;

import dev.xpolion.xpotriad.fragment.ExplosionFragment;
import dev.xpolion.xpotriad.fragment.InvisibilityFragment;
import dev.xpolion.xpotriad.fragment.SpeedFragment;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private AbilityEngine abilityEngine;

    @Override
    public void onEnable() {
        AbilityItem.initialize(this);
        FragmentItem.initialize(this);

        abilityEngine = new AbilityEngine(this);

        getServer().getPluginManager().registerEvents(
                new AbilityListener(abilityEngine),
                this
        );

        getLogger().info("XpoTriad enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("XpoTriad disabled!");
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

        if (!command.getName().equalsIgnoreCase("xptest")) {
            return false;
        }

        InvisibilityFragment invisibility =
                new InvisibilityFragment();

        SpeedFragment speed =
                new SpeedFragment();

        ExplosionFragment explosion =
                new ExplosionFragment();

        Ability ability = new Ability(
                Ability.WeaponType.MELEE
        );

        ability.setFragment(
                Ability.Stage.PRE_CAST,
                invisibility
        );

        ability.setDelay(
                Ability.Stage.PRE_CAST,
                0
        );

        ability.setFragment(
                Ability.Stage.CAST,
                speed
        );

        ability.setDelay(
                Ability.Stage.CAST,
                20
        );

        ability.setFragment(
                Ability.Stage.POST_CAST,
                explosion
        );

        ability.setDelay(
                Ability.Stage.POST_CAST,
                20
        );

        ItemStack weapon = new ItemStack(
                Material.DIAMOND_SWORD
        );

        AbilityItem.engrave(
                weapon,
                ability
        );

        player.getInventory().addItem(weapon);

        player.getInventory().addItem(
                FragmentItem.create(invisibility)
        );

        player.getInventory().addItem(
                FragmentItem.create(speed)
        );

        player.getInventory().addItem(
                FragmentItem.create(explosion)
        );

        return true;
    }
}