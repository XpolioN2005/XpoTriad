package dev.xpolion.xpotriad.particle.animations;

import dev.xpolion.xpotriad.particle.ParticleAnimation;
import dev.xpolion.xpotriad.particle.ParticleContext;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

/**
 * Sparse see-through particle sphere enveloping the target's body
 * (radius 1.2, low-density lat/lon grid with alternating ring rotation,
 * so the shell reads as a transparent bubble). Entity-attached; finite
 * lifetime supplied by ParticleSystem.playPersistent.
 */
public final class ReflectSphereAnimation implements ParticleAnimation {

    private static final double RADIUS = 1.2;
    private static final int RINGS = 6;
    private static final int POINTS_PER_RING = 10;
    private static final double BODY_CENTER_HEIGHT = 1.0;
    private static final double ROTATION_SPEED = 1.4;

    private static final Particle.DustOptions DUST_GOLD =
            new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.1f);

    @Override
    public void render(ParticleContext context) {
        Location origin = context.getOrigin();

        if (origin.getWorld() == null) {
            return;
        }

        double rotation = context.getElapsedSeconds() * ROTATION_SPEED;

        for (int ring = 0; ring < RINGS; ring++) {
            // Evenly spaced latitudes, poles excluded — shell stays open.
            double phi = Math.PI * (ring + 1) / (RINGS + 1);
            double dy = RADIUS * Math.cos(phi);
            double ringRadius = RADIUS * Math.sin(phi);
            double direction = (ring % 2 == 0) ? 1.0 : -1.0;

            for (int i = 0; i < POINTS_PER_RING; i++) {
                double angle = (2.0 * Math.PI * i) / POINTS_PER_RING
                        + rotation * direction;

                origin.getWorld().spawnParticle(
                        Particle.DUST,
                        origin.clone().add(
                                Math.cos(angle) * ringRadius,
                                BODY_CENTER_HEIGHT + dy,
                                Math.sin(angle) * ringRadius
                        ),
                        1,
                        0, 0, 0,
                        0,
                        DUST_GOLD
                );
            }
        }
    }
}