package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.runtime.states.GravityPullState;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Creates a pull field at the selected point.
 *
 * Targeting: POINT — block raycast up to 15 blocks (flows.md convention),
 * else the point 15 blocks along the view ray. Caster is always excluded
 * from affected entities (multiplayer safe).
 *
 * Radius = 4 blocks, duration = 60 ticks, pull every 5 ticks at strength
 * 0.35. Runtime required; stops immediately at expiry.
 * Visual: ground ring r4 with particles spiralling inward (stops with state).
 */
public final class GravityPullEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        BalanceConfig cfg = BalanceConfig.get();
        double pointRange = cfg.pointRange();

        double radius = cfg.effectDouble("gravity_pull", "radius", 4.0);
        int durationTicks = cfg.effectInt("gravity_pull", "duration-ticks", 60);
        int pullIntervalTicks = cfg.effectInt("gravity_pull", "pull-interval-ticks", 5);
        double pullStrength = cfg.effectDouble("gravity_pull", "pull-strength", 0.35);

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

        Main plugin = JavaPlugin.getPlugin(Main.class);

        GravityPullState state = new GravityPullState(
                source,
                point,
                plugin,
                plugin.getParticleSystem(),
                radius,
                durationTicks,
                pullIntervalTicks,
                pullStrength
        );

        plugin.getRuntimeManager().start(state);
    }
}