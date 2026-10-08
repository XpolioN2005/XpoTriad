package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.targeting.TargetResolver;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Swaps the caster and target positions immediately.
 *
 * Targeting: sight raycast (12 blocks) -> fallback nearest within 3 blocks.
 * Caster is always excluded (multiplayer safe).
 *
 * Both locations are validated (feet + head passable) before either
 * teleport happens; invalid locations abort with no effect.
 * No runtime. Visual: simultaneous bursts + spirals rising 5 ticks.
 */
public final class SwapEffect implements Effect {

    private static final double RAYCAST_RANGE = 12.0;
    private static final double NEARBY_RADIUS = 3.0;
    private static final double SPIRAL_DURATION_SECONDS = 5.0 / 20.0;

    private static final Particle.DustOptions DUST_SWAP =
            new Particle.DustOptions(Color.fromRGB(255, 140, 255), 1.1f);

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        LivingEntity target = null;

        List<LivingEntity> raycastTargets = TargetResolver.raycast(source, RAYCAST_RANGE);
        if (!raycastTargets.isEmpty()) {
            target = raycastTargets.get(0);
        } else {
            List<LivingEntity> nearby = TargetResolver.entitiesNear(source.getLocation(), NEARBY_RADIUS, source);
            if (!nearby.isEmpty()) {
                target = nearby.get(0);
            }
        }

        if (target == null) {
            return;
        }

        Location sourceLoc = source.getLocation().clone();
        Location targetLoc = target.getLocation().clone();

        // Validate both destinations first — abort if either is blocked.
        if (!isStandable(sourceLoc) || !isStandable(targetLoc)) {
            return;
        }

        // Target moves first so its old spot is free for the caster.
        target.teleport(sourceLoc);
        source.teleport(targetLoc);

        Main plugin = JavaPlugin.getPlugin(Main.class);

        // Simultaneous circular bursts at both positions.
        for (Location position : List.of(sourceLoc, targetLoc)) {
            plugin.getParticleSystem().play(animationContext -> {
                Location origin = animationContext.getOrigin();

                if (origin.getWorld() != null) {
                    origin.getWorld().spawnParticle(
                            Particle.DUST,
                            origin.clone().add(0, 1.0, 0),
                            18,
                            0.4, 0.8, 0.4,
                            0.0,
                            DUST_SWAP
                    );
                }
            }, position);

            // Particles spiral upward for 5 ticks at each position.
            Location spiralOrigin = position.clone();

            plugin.getParticleSystem().playPersistent(animationContext -> {
                Location origin = animationContext.getOrigin();

                if (origin.getWorld() == null) {
                    return;
                }

                double angle = animationContext.getElapsedSeconds() * 12.0;
                double height = 0.2 + animationContext.getElapsedSeconds() * 6.0;

                origin.getWorld().spawnParticle(
                        Particle.DUST,
                        origin.clone().add(Math.cos(angle) * 0.5, height, Math.sin(angle) * 0.5),
                        1,
                        0, 0, 0,
                        0,
                        DUST_SWAP
                );
            }, spiralOrigin, SPIRAL_DURATION_SECONDS);
        }
    }

    private static boolean isStandable(Location location) {
        World world = location.getWorld();

        if (world == null) {
            return false;
        }

        Block feet = world.getBlockAt(location);
        Block head = feet.getRelative(BlockFace.UP);

        return feet.isPassable() && head.isPassable();
    }
}