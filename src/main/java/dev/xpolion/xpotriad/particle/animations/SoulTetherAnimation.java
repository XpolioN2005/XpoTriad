package dev.xpolion.xpotriad.particle.animations;

import dev.xpolion.xpotriad.particle.ParticleAnimation;
import dev.xpolion.xpotriad.particle.ParticleContext;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

/**
 * Persistent tether drawn between the link source (entity-attached
 * origin) and the linked target. Follows both entities every frame;
 * finite lifetime supplied by ParticleSystem.playPersistent.
 */
public final class SoulTetherAnimation implements ParticleAnimation {

    private static final int POINTS = 14;
    private static final double HEIGHT_OFFSET = 1.0;

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

        org.bukkit.util.Vector step = delta.multiply(1.0 / length / POINTS);

        for (int i = 1; i <= POINTS; i++) {
            origin.getWorld().spawnParticle(
                    Particle.DUST,
                    start.clone().add(step.clone().multiply(i)),
                    1,
                    0, 0, 0,
                    0,
                    DUST_SOUL
            );
        }
    }
}