package dev.xpolion.xpotriad.particle;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

public final class MarkAnimation implements ParticleAnimation {

    private static final double RING_RADIUS = 0.5;
    private static final int POINTS = 10;
    private static final double ROTATION_SPEED = 3.0;
    private static final double HEIGHT_OFFSET = 2.1;
    private static final Particle.DustOptions DUST_RED = new Particle.DustOptions(Color.RED, 1.0f);

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
            double x = Math.cos(angle) * RING_RADIUS;
            double z = Math.sin(angle) * RING_RADIUS;

            Location particleLoc = center.clone().add(x, 0, z);
            origin.getWorld().spawnParticle(
                    Particle.DUST,
                    particleLoc,
                    1,
                    0, 0, 0,
                    0,
                    DUST_RED
            );
        }
    }
}
