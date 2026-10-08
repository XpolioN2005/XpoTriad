package dev.xpolion.xpotriad.particle.animations;

import dev.xpolion.xpotriad.particle.ParticleAnimation;
import dev.xpolion.xpotriad.particle.ParticleContext;

import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Dense smoke covering the caster's full body.
 *
 * Expands from radius 1.0 to 2.5 blocks over the first 20 ticks (1.0 s),
 * then holds at full size. Particles span y 0 -> 2.2 so the whole player
 * body is covered, not only the feet.
 */
public final class SmokeCloudAnimation implements ParticleAnimation {

    private static final double START_RADIUS = 1.0;
    private static final double MAX_RADIUS = 2.5;
    private static final double GROW_DURATION_SECONDS = 1.0;
    private static final double MAX_HEIGHT = 2.2;
    private static final int PARTICLES_PER_FRAME = 28;

    @Override
    public void render(ParticleContext context) {
        Location origin = context.getOrigin();

        if (origin.getWorld() == null) {
            return;
        }

        double elapsed = context.getElapsedSeconds();

        double radius = elapsed < GROW_DURATION_SECONDS
                ? START_RADIUS + (MAX_RADIUS - START_RADIUS) * (elapsed / GROW_DURATION_SECONDS)
                : MAX_RADIUS;

        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < PARTICLES_PER_FRAME; i++) {
            double angle = random.nextDouble(Math.PI * 2.0);
            double radial = radius * Math.sqrt(random.nextDouble());
            double height = random.nextDouble(MAX_HEIGHT);

            Location particleLoc = origin.clone().add(
                    Math.cos(angle) * radial,
                    height,
                    Math.sin(angle) * radial
            );

            origin.getWorld().spawnParticle(
                    Particle.LARGE_SMOKE,
                    particleLoc,
                    1,
                    0, 0, 0,
                    0
            );
        }
    }
}