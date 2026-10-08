package dev.xpolion.xpotriad.particle.animations;

import dev.xpolion.xpotriad.particle.ParticleAnimation;
import dev.xpolion.xpotriad.particle.ParticleContext;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

/**
 * Exactly two blue particle rings rotating in opposite directions
 * around the caster. Entity-attached; finite lifetime (10 ticks)
 * supplied by ParticleSystem.playPersistent.
 */
public final class RewindRingsAnimation implements ParticleAnimation {

    private static final double RING_RADIUS = 0.9;
    private static final int POINTS = 12;
    private static final double ROTATION_SPEED = 4.0;
    private static final double RING_A_HEIGHT = 0.9;
    private static final double RING_B_HEIGHT = 1.4;

    private static final Particle.DustOptions DUST_BLUE =
            new Particle.DustOptions(Color.fromRGB(90, 140, 255), 1.2f);

    @Override
    public void render(ParticleContext context) {
        Location origin = context.getOrigin();

        if (origin.getWorld() == null) {
            return;
        }

        double angleStep = (2.0 * Math.PI) / POINTS;
        double rotation = context.getElapsedSeconds() * ROTATION_SPEED;

        // Ring A rotates clockwise.
        for (int i = 0; i < POINTS; i++) {
            double angle = i * angleStep + rotation;

            origin.getWorld().spawnParticle(
                    Particle.DUST,
                    origin.clone().add(
                            Math.cos(angle) * RING_RADIUS,
                            RING_A_HEIGHT,
                            Math.sin(angle) * RING_RADIUS
                    ),
                    1,
                    0, 0, 0,
                    0,
                    DUST_BLUE
            );
        }

        // Ring B rotates counter-clockwise.
        for (int i = 0; i < POINTS; i++) {
            double angle = i * angleStep - rotation;

            origin.getWorld().spawnParticle(
                    Particle.DUST,
                    origin.clone().add(
                            Math.cos(angle) * RING_RADIUS,
                            RING_B_HEIGHT,
                            Math.sin(angle) * RING_RADIUS
                    ),
                    1,
                    0, 0, 0,
                    0,
                    DUST_BLUE
            );
        }
    }
}