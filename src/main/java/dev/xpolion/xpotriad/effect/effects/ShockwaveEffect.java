package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.runtime.states.ShockwaveState;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Slams the caster straight down to the ground and erupts a shockwave scaled
 * to the drop. The runtime scans the ground, teleports the player down, plays
 * a descending slam trail (light smoke at the feet), then on impact knocks
 * back and damages everything in the scaled radius. Fires on right-click and
 * resolves fully inside the runtime (no teleport-slam: the player lands where
 * the ground actually is).
 */
public final class ShockwaveEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        BalanceConfig cfg = BalanceConfig.get();
        double radiusPerBlock = cfg.effectDouble("shockwave", "radius-per-block", 0.5);
        double maxRadius = cfg.effectDouble("shockwave", "max-radius", 8.0);
        double minFall = cfg.effectDouble("shockwave", "min-fall-distance", 3.0);
        double scanDistance = cfg.effectDouble("shockwave", "ground-scan-distance", 64.0);
        double damage = cfg.effectDouble("shockwave", "damage", 6.0);
        double knockback = cfg.effectDouble("shockwave", "knockback-strength", 1.2);
        int slamTicks = cfg.effectInt("shockwave", "slam-ticks", 5);
        int ringTicks = cfg.effectInt("shockwave", "ring-duration-ticks", 20);
        int failsafeTicks = cfg.effectInt("shockwave", "failsafe-ticks", 60);

        double startY = source.getLocation().getY();

        Main plugin = JavaPlugin.getPlugin(Main.class);

        ShockwaveState state = new ShockwaveState(
                source,
                startY,
                plugin,
                plugin.getParticleSystem(),
                radiusPerBlock,
                maxRadius,
                minFall,
                scanDistance,
                damage,
                knockback,
                slamTicks,
                ringTicks,
                failsafeTicks
        );

        plugin.getRuntimeManager().start(state);
    }
}
