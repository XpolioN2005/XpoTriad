package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.runtime.states.MeteorState;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Launches a giant fireball toward the selected point.
 *
 * Targeting: POINT — block raycast up to 15 blocks (flows.md convention).
 *
 * Lifetime = 40 ticks, impact damage = 20, AOE = 4 blocks, NO terrain
 * destruction (yield 0, no incendiary). Runtime required to track the
 * projectile until impact or expiry. Caster is excluded from the AOE
 * (multiplayer safe).
 *
 * Visual: scaled-up giant fireball look — dense fire/orange trail with
 * heavy smoke on the projectile, large expanding burst on impact.
 */
public final class MeteorEffect implements Effect {

    private static final double POINT_RANGE = 15.0;

    /** Trajectory speed: crosses the 15-block max range well within 40 ticks. */
    private static final double LAUNCH_SPEED = 1.5;

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

        Location spawnLoc = eye.clone().add(direction.clone().multiply(1.5));

        Vector velocity = point.toVector()
                .subtract(spawnLoc.toVector())
                .normalize()
                .multiply(LAUNCH_SPEED);

        Vector flightDirection = velocity.clone();

        LargeFireball fireball = world.spawn(spawnLoc, LargeFireball.class, meteor -> {
            meteor.setShooter(source);
            meteor.setDirection(flightDirection);
            meteor.setVelocity(flightDirection.clone());
            meteor.setYield(0.0f);          // no terrain destruction
            meteor.setIsIncendiary(false);  // no fire spread
        });

        Main plugin = JavaPlugin.getPlugin(Main.class);

        MeteorState state = new MeteorState(
                source,
                fireball,
                plugin,
                plugin.getParticleSystem()
        );

        plugin.getRuntimeManager().start(state);
    }
}