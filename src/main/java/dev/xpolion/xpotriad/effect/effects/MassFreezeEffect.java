package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.runtime.states.MassFreezeState;
import dev.xpolion.xpotriad.targeting.TargetResolver;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Freezes every entity in a 5-block area for 60 ticks.
 *
 * Targeting: AREA — point via block raycast (15 blocks) then entitiesNear.
 * Caster is always excluded (multiplayer safe).
 *
 * Movement prevention is pure per-tick velocity zeroing, so it works on
 * players as well as mobs. Runtime required; releases entities after
 * 60 ticks and ends early for invalid entities.
 */
public final class MassFreezeEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        BalanceConfig cfg = BalanceConfig.get();
        double pointRange = cfg.pointRange();
        double areaRadius = cfg.effectDouble("mass_freeze", "area-radius", 5.0);
        int durationTicks = cfg.effectInt("mass_freeze", "duration-ticks", 60);

        World world = source.getWorld();
        Location eye = source.getEyeLocation();
        Vector direction = eye.getDirection();

        Location point;

        RayTraceResult hit = world.rayTraceBlocks(
                eye,
                direction,
                pointRange,
                FluidCollisionMode.NEVER,
                true
        );

        if (hit != null && hit.getHitPosition() != null) {
            point = hit.getHitPosition().toLocation(world);
        } else {
            point = eye.clone().add(direction.clone().multiply(pointRange));
        }

        List<LivingEntity> frozen = TargetResolver.entitiesNear(point, areaRadius, source);

        if (frozen.isEmpty()) {
            return; // Nothing to freeze — no runtime, no visuals.
        }

        Main plugin = JavaPlugin.getPlugin(Main.class);

        MassFreezeState state = new MassFreezeState(
                source,
                point,
                frozen,
                plugin,
                plugin.getParticleSystem(),
                durationTicks
        );

        plugin.getRuntimeManager().start(state);
    }
}