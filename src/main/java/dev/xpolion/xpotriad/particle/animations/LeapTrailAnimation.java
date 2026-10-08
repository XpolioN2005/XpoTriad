package dev.xpolion.xpotriad.particle.animations;

import dev.xpolion.xpotriad.particle.ParticleAnimation;
import dev.xpolion.xpotriad.particle.ParticleContext;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Short upward particle trail emitted from the caster's feet.
 * Runs for exactly 6 ticks; drifts upward as the leap progresses.
 */
public final class LeapTrailAnimation implements ParticleAnimation {

    private static final double TRAIL_DURATION_SECONDS = 6.0 / 20.0;
    private static final double MAX_RISE = 0.8;
    private static final int PARTICLES_PER_FRAME = 6;

    private static final Particle.DustOptions DUST_AQUA =
            new Particle.DustOptions(Color.AQUA, 1.0f);

    @Override
    public void render(ParticleContext context) {
        Location origin = context.getOrigin();

        if (origin.getWorld() == null) {
            return;
        }

        double progress = Math.min(1.0, context.getElapsedSeconds() / TRAIL_DURATION_SECONDS);
        double baseHeight = 0.1 + progress * MAX_RISE;

        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < PARTICLES_PER_FRAME; i++) {
            Location particleLoc = origin.clone().add(
                    random.nextDouble(-0.25, 0.25),
                    baseHeight + random.nextDouble(0.3),
                    random.nextDouble(-0.25, 0.25)
            );

            origin.getWorld().spawnParticle(
                    Particle.DUST,
                    particleLoc,
                    1,
                    0, 0, 0,
                    0,
                    DUST_AQUA
            );
        }
    }
}