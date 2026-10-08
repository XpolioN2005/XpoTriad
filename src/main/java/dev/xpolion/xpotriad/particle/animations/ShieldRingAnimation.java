package dev.xpolion.xpotriad.particle.animations;

import dev.xpolion.xpotriad.particle.ParticleAnimation;
import dev.xpolion.xpotriad.particle.ParticleContext;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

/**
 * Rotating particle ring at waist height around the caster.
 * Radius = 0.7 blocks, rotates continuously (finite lifetime supplied
 * by ParticleSystem.playPersistent).
 */
public final class ShieldRingAnimation implements ParticleAnimation {

    private static final double RING_RADIUS = 0.7;
    private static final double HEIGHT_OFFSET = 1.0;
    private static final int POINTS = 14;
    private static final double ROTATION_SPEED = 3.0;

    private static final Particle.DustOptions DUST_SHIELD =
            new Particle.DustOptions(Color.fromRGB(140, 200, 255), 1.0f);

    @Override
    public void render(ParticleContext context) {
        Location origin = context.getOrigin();

        if (origin.getWorld() == null) {
            return;
        }

        Location center = origin.clone().add(0, HEIGHT_OFFSET, 0);
        double angleStep = (2.0 * Math.PI) / POINTS;
        double rotation = context.getElapsedSeconds() * ROTATION_SPEED;

        for (int i = 0; i < POINTS; i++) {
            double angle = i * angleStep + rotation;

            Location particleLoc = center.clone().add(
                    Math.cos(angle) * RING_RADIUS,
                    0,
                    Math.sin(angle) * RING_RADIUS
            );

            origin.getWorld().spawnParticle(
                    Particle.DUST,
                    particleLoc,
                    1,
                    0, 0, 0,
                    0,
                    DUST_SHIELD
            );
        }
    }
}