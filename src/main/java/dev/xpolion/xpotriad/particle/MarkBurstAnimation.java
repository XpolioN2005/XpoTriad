package dev.xpolion.xpotriad.particle;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

public final class MarkBurstAnimation implements ParticleAnimation {

    private static final Particle.DustOptions DUST_RED = new Particle.DustOptions(Color.RED, 1.5f);

    @Override
    public void render(ParticleContext context) {
        Location origin = context.getOrigin().add(0, 1.0, 0);
        if (origin.getWorld() != null) {
            origin.getWorld().spawnParticle(
                    Particle.DUST,
                    origin,
                    25,
                    0.4, 0.4, 0.4,
                    0,
                    DUST_RED
            );
        }
    }
}
