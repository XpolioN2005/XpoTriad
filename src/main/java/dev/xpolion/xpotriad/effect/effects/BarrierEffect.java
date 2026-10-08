package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.runtime.states.BarrierState;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Creates a circular boundary around the selected point.
 *
 * Targeting: POINT — block raycast up to 15 blocks (flows.md convention),
 * else the point 15 blocks along the view ray.
 *
 * Radius = 3 blocks, duration = 100 ticks. Entities inside at activation
 * (caster excluded — multiplayer safe) cannot cross the boundary while it
 * lasts. Runtime required; stops automatically after 100 ticks.
 * Visual: evenly spaced circular boundary, removed with the runtime.
 */
public final class BarrierEffect implements Effect {

    private static final double POINT_RANGE = 15.0;

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        World world = source.getWorld();
        Location eye = source.getEyeLocation();
        Vector direction = eye.getDirection();

        Location point;

        RayTraceResult hit = world.rayTraceBlocks(
                eye,
                direction,
                POINT_RANGE,
                FluidCollisionMode.NEVER,
                true
        );

        if (hit != null && hit.getHitPosition() != null) {
            point = hit.getHitPosition().toLocation(world);
        } else {
            point = eye.clone().add(direction.clone().multiply(POINT_RANGE));
        }

        Main plugin = JavaPlugin.getPlugin(Main.class);

        BarrierState state = new BarrierState(
                source,
                point,
                plugin,
                plugin.getParticleSystem()
        );

        plugin.getRuntimeManager().start(state);
    }
}