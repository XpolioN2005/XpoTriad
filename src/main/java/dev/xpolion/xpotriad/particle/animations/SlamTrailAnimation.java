package dev.xpolion.xpotriad.particle.animations;

import dev.xpolion.xpotriad.particle.ParticleAnimation;
import dev.xpolion.xpotriad.particle.ParticleContext;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Descending slam trail from the captured start height down to the ground,
 * plus a light-smoke puff at the feet on arrival. Sells a fast downward slam.
 * Finite lifetime supplied by ParticleSystem.playPersistent.
 */
public final class SlamTrailAnimation implements ParticleAnimation {

    private static final Particle.DustOptions DUST_LIGHT =
            new Particle.DustOptions(Color.fromRGB(230, 230, 235), 1.0f);

    private final double startY;
    private final double groundY;
    private final double durationSeconds;
    private boolean burstDone = false;

    public SlamTrailAnimation(double startY, double groundY, int slamTicks) {
        this.startY = startY;
        this.groundY = groundY;
        this.durationSeconds = Math.max(1, slamTicks) / 20.0;
    }

    @Override
    public void render(ParticleContext context) {
        Location origin = context.getOrigin();
        if (origin.getWorld() == null) {
            return;
        }

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double elapsed = context.getElapsedSeconds();
        double progress = Math.min(1.0, elapsed / durationSeconds);

        // Light smoke puff at the feet, once, right at the start of the slam.
        if (!burstDone) {
            burstDone = true;
            origin.getWorld().spawnParticle(
                    Particle.WHITE_ASH,
                    origin.clone().add(0.0, 0.1, 0.0),
                    40,
                    0.5, 0.1, 0.5,
                    0.05
            );
        }

        // Head of the streak descends from startY to groundY.
        double headY = startY + (groundY - startY) * progress;
        double span = startY - groundY;
        int steps = 24;

        for (int i = 0; i < steps; i++) {
            double frac = (double) i / steps;
            double y = headY - span * frac;
            if (y < groundY) {
                continue;
            }
            double x = origin.getX() + (rng.nextDouble() - 0.5) * 0.25;
            double z = origin.getZ() + (rng.nextDouble() - 0.5) * 0.25;
            origin.getWorld().spawnParticle(
                    Particle.DUST,
                    x, y, z,
                    1,
                    0, 0, 0,
                    0,
                    DUST_LIGHT
            );
        }

        // A little light smoke trailing under the descending head.
        origin.getWorld().spawnParticle(
                Particle.WHITE_ASH,
                origin.getX(), headY, origin.getZ(),
                4,
                0.3, 0.2, 0.3,
                0.01
        );
    }
}
