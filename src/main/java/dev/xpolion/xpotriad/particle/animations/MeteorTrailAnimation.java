package dev.xpolion.xpotriad.particle.animations;

import dev.xpolion.xpotriad.particle.ParticleAnimation;
import dev.xpolion.xpotriad.particle.ParticleContext;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Giant scaled-up meteor trail: dense fiery/orange core with heavy
 * smoke trailing behind. Entity-attached to the fireball; finite
 * lifetime supplied by ParticleSystem.playPersistent, and it stops
 * automatically when the fireball entity dies.
 */
public final class MeteorTrailAnimation implements ParticleAnimation {

    private static final int FLAMES_PER_FRAME = 14;
    private static final int SMOKE_PER_FRAME = 8;

    private static final Particle.DustOptions DUST_EMBER =
            new Particle.DustOptions(Color.fromRGB(255, 130, 30), 1.6f);

    @Override
    public void render(ParticleContext context) {
        Location origin = context.getOrigin();

        if (origin.getWorld() == null) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();

        // Fiery core with jitter — lingers behind as a trail.
        for (int i = 0; i < FLAMES_PER_FRAME; i++) {
            origin.getWorld().spawnParticle(
                    Particle.FLAME,
                    origin.clone().add(
                            random.nextDouble(-0.5, 0.5),
                            random.nextDouble(-0.5, 0.5),
                            random.nextDouble(-0.5, 0.5)
                    ),
                    1,
                    0, 0, 0,
                    0.02
            );
        }

        origin.getWorld().spawnParticle(
                Particle.DUST,
                origin,
                6,
                0.4, 0.4, 0.4,
                0.0,
                DUST_EMBER
        );

        // Heavy smoke trailing behind.
        for (int i = 0; i < SMOKE_PER_FRAME; i++) {
            origin.getWorld().spawnParticle(
                    Particle.LARGE_SMOKE,
                    origin.clone().add(
                            random.nextDouble(-0.7, 0.7),
                            random.nextDouble(-0.4, 0.8),
                            random.nextDouble(-0.7, 0.7)
                    ),
                    1,
                    0, 0.05, 0,
                    0
            );
        }
    }
}