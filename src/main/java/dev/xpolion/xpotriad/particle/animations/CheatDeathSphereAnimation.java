package dev.xpolion.xpotriad.particle.animations;

import dev.xpolion.xpotriad.particle.ParticleAnimation;
import dev.xpolion.xpotriad.particle.ParticleContext;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

/**
 * Rotating golden particle sphere (radius 0.8) around the caster's
 * body — built from three rotating latitude rings. Entity-attached;
 * finite lifetime (40 ticks) supplied by ParticleSystem.playPersistent.
 */
public final class CheatDeathSphereAnimation implements ParticleAnimation {

    private static final double RADIUS = 0.8;
    private static final int POINTS = 16;
    private static final double ROTATION_SPEED = 3.5;
    private static final double BODY_CENTER_HEIGHT = 1.0;

    private static final double[] LATITUDES = {-0.4, 0.0, 0.4};

    private static final Particle.DustOptions DUST_GOLD =
            new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.2f);

    @Override
    public void render(ParticleContext context) {
        Location origin = context.getOrigin();

        if (origin.getWorld() == null) {
            return;
        }

        double rotation = context.getElapsedSeconds() * ROTATION_SPEED;

        for (int ring = 0; ring < LATITUDES.length; ring++) {
            double dy = LATITUDES[ring];
            double ringRadius = Math.sqrt(Math.max(RADIUS * RADIUS - dy * dy, 0.0));
            double angleStep = (2.0 * Math.PI) / POINTS;
            // Alternate rotation direction per ring.
            double direction = (ring % 2 == 0) ? 1.0 : -1.0;

            for (int i = 0; i < POINTS; i++) {
                double angle = i * angleStep + rotation * direction;

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