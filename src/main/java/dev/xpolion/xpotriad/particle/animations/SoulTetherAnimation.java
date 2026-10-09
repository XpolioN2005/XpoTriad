package dev.xpolion.xpotriad.particle.animations;

import dev.xpolion.xpotriad.particle.ParticleAnimation;
import dev.xpolion.xpotriad.particle.ParticleContext;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

/**
 * Sparse tether drawn between the link source (entity-attached
 * origin) and the linked target — only the middle band of the line is
 * drawn, so the tether never touches either player, and the points are
 * spaced far apart for a sparse look. Follows both entities every
 * frame; finite lifetime supplied by ParticleSystem.playPersistent.
 */
public final class SoulTetherAnimation implements ParticleAnimation {

    /** Few, widely spaced points — sparse by design. */
    private static final int POINTS = 5;
    private static final double HEIGHT_OFFSET = 1.0;
    /** Tether spans only the middle of the line (gaps near both players). */
    private static final double BAND_START = 0.3;
    private static final double BAND_END = 0.7;

    private static final Particle.DustOptions DUST_SOUL =
            new Particle.DustOptions(Color.fromRGB(120, 220, 255), 1.0f);

    private final LivingEntity target;

    public SoulTetherAnimation(LivingEntity target) {
        if (target == null) {
            throw new IllegalArgumentException("Target cannot be null");
        }

        this.target = target;
    }

    @Override
    public void render(ParticleContext context) {
        if (!target.isValid() || target.isDead()) {
            return;
        }

        Location origin = context.getOrigin();
        Location start = origin.clone().add(0, HEIGHT_OFFSET, 0);
        Location end = target.getLocation().add(0, HEIGHT_OFFSET, 0);

        if (start.getWorld() == null || !start.getWorld().equals(end.getWorld())) {
            return;
        }

        org.bukkit.util.Vector delta = end.toVector().subtract(start.toVector());
        double length = delta.length();

        if (length < 1.0E-6) {
            return;
        }

        org.bukkit.util.Vector unit = delta.multiply(1.0 / length);

        for (int i = 0; i < POINTS; i++) {
            // Middle band only — t runs BAND_START..BAND_END, never the ends.
            double t = BAND_START + (BAND_END - BAND_START) * (i / (double) (POINTS - 1));

            origin.getWorld().spawnParticle(
                    Particle.DUST,
                    start.clone().add(unit.clone().multiply(length * t)),
                    1,
                    0, 0, 0,
                    0,
                    DUST_SOUL
            );
        }
    }
}