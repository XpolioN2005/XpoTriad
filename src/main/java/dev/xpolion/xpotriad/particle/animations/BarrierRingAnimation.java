package dev.xpolion.xpotriad.particle.animations;

import dev.xpolion.xpotriad.particle.ParticleAnimation;
import dev.xpolion.xpotriad.particle.ParticleContext;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

/**
 * Circular boundary at the configured radius with particle walls rising
 * from it — vertical lines whose dust shrinks row by row, so each wall
 * fades upward. Visible for the full barrier duration; finite lifetime
 * supplied by ParticleSystem.playPersistent.
 */
public final class BarrierRingAnimation implements ParticleAnimation {

    private static final int POINTS = 28;
    private static final double ROTATION_SPEED = 0.8;
    private static final double HEIGHT_OFFSET = 0.1;

    /** Wall columns rise this high above the ring. */
    private static final double WALL_HEIGHT = 3.0;
    /** Rows per column; dust size shrinks row by row = fade upward. */
    private static final int WALL_ROWS = 5;

    private static final Particle.DustOptions DUST_RING =
            new Particle.DustOptions(Color.fromRGB(255, 200, 80), 1.2f);

    /** Per-row wall dust: big at the base, small at the top. */
    private static final Particle.DustOptions[] DUST_WALL =
            new Particle.DustOptions[WALL_ROWS];

    static {
        for (int row = 0; row < WALL_ROWS; row++) {
            float fraction = 1.0f - row / (float) WALL_ROWS;
            DUST_WALL[row] = new Particle.DustOptions(
                    Color.fromRGB(255, 200, 80), 0.3f + 0.9f * fraction);
        }
    }

    private final double radius;

    public BarrierRingAnimation(double radius) {
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
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            // Ground ring.
            origin.getWorld().spawnParticle(
                    Particle.DUST,
                    origin.clone().add(x, HEIGHT_OFFSET, z),
                    1,
                    0, 0, 0,
                    0,
                    DUST_RING
            );

            // Wall column rising from the ring, fading upward.
            for (int row = 0; row < WALL_ROWS; row++) {
                double height = HEIGHT_OFFSET + WALL_HEIGHT * (row + 1) / WALL_ROWS;

                origin.getWorld().spawnParticle(
                        Particle.DUST,
                        origin.clone().add(x, height, z),
                        1,
                        0, 0, 0,
                        0,
                        DUST_WALL[row]
                );
            }
        }
    }
}