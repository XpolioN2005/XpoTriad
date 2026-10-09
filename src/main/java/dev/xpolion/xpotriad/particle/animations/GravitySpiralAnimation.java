package dev.xpolion.xpotriad.particle.animations;

import dev.xpolion.xpotriad.particle.ParticleAnimation;
import dev.xpolion.xpotriad.particle.ParticleContext;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

/**
 * Ground ring at the configured radius with particles spiralling inward
 * toward the center. Animates continuously while the gravity field is
 * active (finite lifetime supplied by ParticleSystem.playPersistent).
 */
public final class GravitySpiralAnimation implements ParticleAnimation {

    private static final int RING_POINTS = 24;
    private static final int SPIRAL_ARMS = 4;
    private static final double ROTATION_SPEED = 1.5;
    private static final double SPIRAL_CIRCUITS_PER_SECOND = 0.7;

    private static final Particle.DustOptions DUST_GRAVITY =
            new Particle.DustOptions(Color.fromRGB(150, 90, 255), 1.1f);

    private final double radius;

    public GravitySpiralAnimation(double radius) {
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

        double elapsed = context.getElapsedSeconds();
        double rotation = elapsed * ROTATION_SPEED;

        // Outer ring.
        double ringStep = (2.0 * Math.PI) / RING_POINTS;

        for (int i = 0; i < RING_POINTS; i++) {
            double angle = i * ringStep + rotation;

            Location particleLoc = origin.clone().add(
                    Math.cos(angle) * radius,
                    0.05,
                    Math.sin(angle) * radius
            );

            origin.getWorld().spawnParticle(
                    Particle.DUST,
                    particleLoc,
                    1,
                    0, 0, 0,
                    0,
                    DUST_GRAVITY
            );
        }

        // Spiral arms moving inward.
        for (int arm = 0; arm < SPIRAL_ARMS; arm++) {
            double phase = (elapsed * SPIRAL_CIRCUITS_PER_SECOND
                    + (double) arm / SPIRAL_ARMS) % 1.0;
            double armRadius = radius * (1.0 - phase);
            double angle = phase * Math.PI * 4.0 + rotation;

            Location particleLoc = origin.clone().add(
                    Math.cos(angle) * armRadius,
                    0.05,
                    Math.sin(angle) * armRadius
            );

            origin.getWorld().spawnParticle(
                    Particle.DUST,
                    particleLoc,
                    2,
                    0.05, 0.05, 0.05,
                    0,
                    DUST_GRAVITY
            );
        }
    }
}