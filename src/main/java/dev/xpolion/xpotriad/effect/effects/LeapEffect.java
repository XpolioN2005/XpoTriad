package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.config.BalanceConfig;
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
 * Targeting: SELF. Forward velocity and vertical velocity come from config.
 * Immediate effect — no gameplay runtime.
 * Visual: dust burst at the feet + upward trail lasting 6 ticks
 * (finite persistent particle, auto-stops).
 */
public final class LeapEffect implements Effect {

    private static final double TRAIL_DURATION_SECONDS = 6.0 / 20.0;

    private static final Particle.DustOptions DUST_BLUE =
            new Particle.DustOptions(Color.AQUA, 1.0f);

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        BalanceConfig cfg = BalanceConfig.get();
        double forwardVelocity = cfg.effectDouble("leap", "forward-velocity", 1.0);
        double verticalVelocity = cfg.effectDouble("leap", "vertical-velocity", 0.8);

        Vector forward = source.getLocation().getDirection();
        forward.setY(0.0);

        if (forward.lengthSquared() < 1.0E-4) {
            forward = new Vector(0.0, 0.0, 1.0);
        }

        forward.normalize().multiply(forwardVelocity);
        forward.setY(verticalVelocity);

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