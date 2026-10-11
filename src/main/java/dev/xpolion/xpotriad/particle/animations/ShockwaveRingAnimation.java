package dev.xpolion.xpotriad.particle.animations;

import dev.xpolion.xpotriad.particle.ParticleAnimation;
import dev.xpolion.xpotriad.particle.ParticleContext;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

/**
 * Expanding shockwave on impact: a growing main ring, layered inner sub-rings,
 * and a heavy-air dust push from the centre. Scales to the shockwave radius
 * and expands over the ring lifetime. Finite lifetime supplied by
 * ParticleSystem.playPersistent.
 */
public final class ShockwaveRingAnimation implements ParticleAnimation {

    private static final int POINTS = 36;
    private static final Particle.DustOptions DUST_WAVE =
            new Particle.DustOptions(Color.fromRGB(235, 235, 240), 1.4f);
    private static final Particle.DustOptions DUST_INNER =
            new Particle.DustOptions(Color.fromRGB(190, 190, 200), 1.0f);

    private final double radius;
    private final double durationSeconds;

    public ShockwaveRingAnimation(double radius, int ringTicks) {
        if (radius <= 0.0) {
            throw new IllegalArgumentException("Radius must be positive");
        }
        this.radius = radius;
        this.durationSeconds = Math.max(1, ringTicks) / 20.0;
    }

    @Override
    public void render(ParticleContext context) {
        Location origin = context.getOrigin();
        if (origin.getWorld() == null) {
            return;
        }

        double elapsed = context.getElapsedSeconds();
        double progress = Math.min(1.0, elapsed / durationSeconds);
        double eased = 1.0 - (1.0 - progress) * (1.0 - progress); // ease-out
        double current = radius * eased;

        double angleStep = (2.0 * Math.PI) / POINTS;

        for (int i = 0; i < POINTS; i++) {
            double angle = i * angleStep;
            double x = origin.getX() + Math.cos(angle) * current;
            double z = origin.getZ() + Math.sin(angle) * current;
            origin.getWorld().spawnParticle(
                    Particle.DUST,
                    x, origin.getY() + 0.15, z,
                    1,
                    0, 0, 0,
                    0,
                    DUST_WAVE
            );
        }

        // Inner sub-rings.
        for (int ring = 1; ring <= 3; ring++) {
            double r = current * (ring / 4.0);
            if (r <= 0.05) {
                continue;
            }
            for (int i = 0; i < POINTS; i++) {
                double angle = i * angleStep + ring;
                double x = origin.getX() + Math.cos(angle) * r;
                double z = origin.getZ() + Math.sin(angle) * r;
                origin.getWorld().spawnParticle(
                        Particle.DUST,
                        x, origin.getY() + 0.1, z,
                        1,
                        0, 0, 0,
                        0,
                        DUST_INNER
                );
            }
        }

        // Heavy air push: rising smoke from the centre.
        origin.getWorld().spawnParticle(
                Particle.CAMPFIRE_COSY_SMOKE,
                origin.getX(), origin.getY() + 0.2, origin.getZ(),
                8,
                current * 0.3, 0.1, current * 0.3,
                0.02
        );
    }
}
