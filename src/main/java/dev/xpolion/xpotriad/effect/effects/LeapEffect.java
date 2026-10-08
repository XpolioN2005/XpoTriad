package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.particle.animations.LeapTrailAnimation;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

/**
 * Launches the caster forward and upward.
 *
 * Targeting: SELF. Forward velocity = 1.0, vertical velocity = 0.8.
 * Immediate effect — no gameplay runtime.
 * Visual: dust burst at the feet + upward trail lasting 6 ticks
 * (finite persistent particle, auto-stops).
 */
public final class LeapEffect implements Effect {

    private static final double FORWARD_VELOCITY = 1.0;
    private static final double VERTICAL_VELOCITY = 0.8;
    private static final double TRAIL_DURATION_SECONDS = 6.0 / 20.0;

    private static final Particle.DustOptions DUST_BLUE =
            new Particle.DustOptions(Color.AQUA, 1.0f);

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        Vector forward = source.getLocation().getDirection();
        forward.setY(0.0);

        if (forward.lengthSquared() < 1.0E-4) {
            forward = new Vector(0.0, 0.0, 1.0);
        }

        forward.normalize().multiply(FORWARD_VELOCITY);
        forward.setY(VERTICAL_VELOCITY);

        source.setVelocity(forward);

        Main plugin = JavaPlugin.getPlugin(Main.class);

        // Compact dust burst around the caster's feet.
        plugin.getParticleSystem().play(animationContext -> {
            Location origin = animationContext.getOrigin();

            if (origin.getWorld() == null) {
                return;
            }

            origin.getWorld().spawnParticle(
                    Particle.DUST,
                    origin,
                    16,
                    0.4, 0.1, 0.4,
                    0.0,
                    DUST_BLUE
            );
        }, source.getLocation());

        // Short upward particle trail (6 ticks, follows the caster).
        plugin.getParticleSystem().playPersistent(
                new LeapTrailAnimation(),
                source,
                TRAIL_DURATION_SECONDS
        );
    }
}