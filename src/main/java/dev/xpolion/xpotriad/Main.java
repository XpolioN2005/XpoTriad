package dev.xpolion.xpotriad;

import dev.xpolion.xpotriad.fragment.ExplosionFragment;
import dev.xpolion.xpotriad.fragment.InvisibilityFragment;
import dev.xpolion.xpotriad.fragment.SpeedFragment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private AbilityEngine abilityEngine;

    @Override
    public void onEnable() {
        abilityEngine = new AbilityEngine(this);

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

        if (command.getName().equalsIgnoreCase("xptest")) {
            Ability ability = new Ability(
                "test_combo",
                Ability.WeaponType.MELEE
            );

            ability.setFragment(
                Ability.Stage.PRE_CAST,
                new InvisibilityFragment()
            );

            ability.setDelay(
                Ability.Stage.PRE_CAST,
                20
            );

            ability.setFragment(
                Ability.Stage.CAST,
                new SpeedFragment()
            );

            ability.setDelay(
                Ability.Stage.CAST,
                20
            );

            ability.setFragment(
                Ability.Stage.POST_CAST,
                new ExplosionFragment()
            );

            abilityEngine.execute(player, ability);

            return true;
        }

        return false;
    }
}