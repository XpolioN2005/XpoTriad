package dev.xpolion.xpotriad.particle.animations;

import dev.xpolion.xpotriad.particle.ParticleAnimation;
import dev.xpolion.xpotriad.particle.ParticleContext;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

/**
 * Frost ring around the frozen area (configurable radius) on the ground.
 * Finite lifetime supplied by ParticleSystem.playPersistent; removed
 * with the freeze runtime.
 */
public final class MassFreezeRingAnimation implements ParticleAnimation {

    private static final int POINTS = 36;
    private static final double ROTATION_SPEED = 0.6;
    private static final double HEIGHT_OFFSET = 0.1;

    private static final Particle.DustOptions DUST_FROST =
            new Particle.DustOptions(Color.fromRGB(170, 230, 255), 1.1f);

    private final double radius;

    public MassFreezeRingAnimation(double radius) {
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

        double angleStep = (2.0 * Math.PI) / POINTS;
        double rotation = context.getElapsedSeconds() * ROTATION_SPEED;

        for (int i = 0; i < POINTS; i++) {
            double angle = i * angleStep + rotation;

            Location particleLoc = origin.clone().add(
                    Math.cos(angle) * radius,
                    HEIGHT_OFFSET,
                    Math.sin(angle) * radius
            );

            origin.getWorld().spawnParticle(
                    Particle.SNOWFLAKE,
                    particleLoc,
                    1,
                    0, 0, 0,
                    0
            );

            origin.getWorld().spawnParticle(
                    Particle.DUST,
                    particleLoc,
                    1,
                    0, 0, 0,
                    0,
                    DUST_FROST
            );
        }
    }
}