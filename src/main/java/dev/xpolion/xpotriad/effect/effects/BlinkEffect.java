package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effect.Effect;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Teleports the caster to a validated location ahead.
 *
 * Targeting: POINT — block raycast up to 8 blocks, walking back along the
 * ray until a passable standing spot (feet + head) is found. Aborts with
 * no teleport when no valid destination exists.
 *
 * Immediate effect — no gameplay runtime.
 * Visual: burst at origin, burst at destination, destination rises 5 ticks.
 */
public final class BlinkEffect implements Effect {

    private static final double MAX_DISTANCE = 8.0;
    private static final double STEP = 0.25;
    private static final double RISE_DURATION_SECONDS = 5.0 / 20.0;

    private static final Particle.DustOptions DUST_BLINK =
            new Particle.DustOptions(Color.fromRGB(190, 130, 255), 1.1f);

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        World world = source.getWorld();
        Location eye = source.getEyeLocation();
        Vector direction = eye.getDirection();

        RayTraceResult hit = world.rayTraceBlocks(
                eye,
                direction,
                MAX_DISTANCE,
                FluidCollisionMode.NEVER,
                true
        );

        // Candidate endpoint: just before the block hit, or the full distance.
        Vector endpoint = (hit != null && hit.getHitPosition() != null)
                ? hit.getHitPosition().subtract(direction.clone().multiply(0.6))
                : eye.toVector().add(direction.clone().multiply(MAX_DISTANCE));

        // Walk back along the ray until feet and head are passable.
        Location destination = null;

        for (double back = 0.0; back <= MAX_DISTANCE; back += STEP) {
            Vector candidate = endpoint.clone().subtract(direction.clone().multiply(back));

            if (candidate.getY() < 1.0 || candidate.getY() >= world.getMaxHeight() - 1.0) {
                continue;
            }

            Location candidateLoc = candidate.toLocation(
                    world,
                    source.getLocation().getYaw(),
                    source.getLocation().getPitch()
            );

            Block feet = world.getBlockAt(candidateLoc);
            Block head = feet.getRelative(BlockFace.UP);

            if (feet.isPassable() && head.isPassable()) {
                destination = candidateLoc;
                break;
            }
        }

        if (destination == null) {
            return; // No valid destination — no teleport, no visual.
        }

        Location origin = source.getLocation().clone();
        source.teleport(destination);

        Main plugin = JavaPlugin.getPlugin(Main.class);

        // Burst at origin.
        plugin.getParticleSystem().play(animationContext -> {
            Location burstOrigin = animationContext.getOrigin();

            if (burstOrigin.getWorld() != null) {
                burstOrigin.getWorld().spawnParticle(
                        Particle.DUST,
                        burstOrigin.clone().add(0, 1.0, 0),
                        18,
                        0.4, 0.6, 0.4,
                        0.0,
                        DUST_BLINK
                );
            }
        }, origin);

        // Burst at destination.
        plugin.getParticleSystem().play(animationContext -> {
            Location burstOrigin = animationContext.getOrigin();

            if (burstOrigin.getWorld() != null) {
                burstOrigin.getWorld().spawnParticle(
                        Particle.DUST,
                        burstOrigin.clone().add(0, 1.0, 0),
                        18,
                        0.4, 0.6, 0.4,
                        0.0,
                        DUST_BLINK
                );
            }
        }, destination);

        // Destination particles rise for 5 ticks (finite particle runtime).
        plugin.getParticleSystem().playPersistent(animationContext -> {
            Location riseOrigin = animationContext.getOrigin();

            if (riseOrigin.getWorld() == null) {
                return;
            }

            double height = 0.2 + animationContext.getElapsedSeconds() * 4.0;

            riseOrigin.getWorld().spawnParticle(
                    Particle.DUST,
                    riseOrigin.clone().add(0, height, 0),
                    2,
                    0.15, 0.1, 0.15,
                    0.0,
                    DUST_BLINK
            );
        }, destination, RISE_DURATION_SECONDS);
    }
}