package dev.xpolion.xpotriad.particle.animations;

import dev.xpolion.xpotriad.particle.ParticleAnimation;
import dev.xpolion.xpotriad.particle.ParticleContext;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

/**
 * Evenly spaced circular boundary at radius 3 around the barrier center.
 * Visible for the full barrier duration; finite lifetime supplied by
 * ParticleSystem.playPersistent.
 */
public final class BarrierRingAnimation implements ParticleAnimation {

    private static final double RADIUS = 3.0;
    private static final int POINTS = 28;
    private static final double ROTATION_SPEED = 0.8;
    private static final double HEIGHT_OFFSET = 0.1;

    private static final Particle.DustOptions DUST_BARRIER =
            new Particle.DustOptions(Color.fromRGB(255, 200, 80), 1.2f);

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
                    Math.cos(angle) * RADIUS,
                    HEIGHT_OFFSET,
                    Math.sin(angle) * RADIUS
            );

            origin.getWorld().spawnParticle(
                    Particle.DUST,
                    particleLoc,
                    1,
                    0, 0, 0,
                    0,
                    DUST_BARRIER
            );
        }
    }
}