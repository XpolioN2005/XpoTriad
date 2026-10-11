package dev.xpolion.xpotriad.particle.animations;

import dev.xpolion.xpotriad.particle.ParticleAnimation;
import dev.xpolion.xpotriad.particle.ParticleContext;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A churning green ground cloud filling a radius around its origin. Hugs the
 * ground and slowly drifts, so the area reads as poisoned for the whole cloud
 * lifetime. Finite lifetime supplied by ParticleSystem.playPersistent.
 */
public final class PoisonCloudAnimation implements ParticleAnimation {

    private static final Particle.DustOptions DUST_GREEN =
            new Particle.DustOptions(Color.fromRGB(40, 160, 40), 1.2f);

    private final double radius;

    public PoisonCloudAnimation(double radius) {
        if (radius <= 0.0) {
            throw new IllegalArgumentException("Radius must be positive");
        }

        this.radius = radius;
    }

    @Override
    public void render(ParticleContext context) {
        Location origin = context.getOrigin();

        if (origin.getWorld() == null) {
            return;
        }

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double time = context.getElapsedSeconds();

        for (int i = 0; i < 8; i++) {
            double angle = rng.nextDouble(Math.PI * 2.0);
            double r = rng.nextDouble() * radius;
            double x = origin.getX() + Math.cos(angle) * r;
            double z = origin.getZ() + Math.sin(angle) * r;
            double y = origin.getY() + 0.1 + rng.nextDouble() * 0.7
                    + Math.sin(time * 2.0 + i) * 0.15;

            origin.getWorld().spawnParticle(
                    Particle.DUST,
                    x, y, z,
                    1,
                    0, 0, 0,
                    0,
                    DUST_GREEN
            );
        }

        // Faint rising ash mixed in for volume.
        origin.getWorld().spawnParticle(
                Particle.WHITE_ASH,
                origin.getX(), origin.getY() + 0.2, origin.getZ(),
                3,
                radius * 0.6, 0.3, radius * 0.6,
                0.01
        );
    }
}
