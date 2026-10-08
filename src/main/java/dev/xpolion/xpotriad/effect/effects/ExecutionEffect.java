package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.targeting.TargetResolver;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Deals 10 bonus damage to a target at or below 25% of maximum health.
 *
 * Targeting: sight raycast (12 blocks) -> fallback nearest within 3 blocks.
 * Caster is always excluded (multiplayer safe).
 *
 * Immediate effect — no runtime. One-shot dark/red burst only when
 * the threshold is met.
 */
public final class ExecutionEffect implements Effect {

    private static final double RAYCAST_RANGE = 12.0;
    private static final double NEARBY_RADIUS = 3.0;
    private static final double HEALTH_THRESHOLD_RATIO = 0.25;
    private static final double BONUS_DAMAGE = 10.0;

    private static final Particle.DustOptions DUST_DARK_RED =
            new Particle.DustOptions(Color.fromRGB(120, 0, 0), 1.4f);

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        LivingEntity target = null;

        List<LivingEntity> raycastTargets = TargetResolver.raycast(source, RAYCAST_RANGE);
        if (!raycastTargets.isEmpty()) {
            target = raycastTargets.get(0);
        } else {
            List<LivingEntity> nearby = TargetResolver.entitiesNear(source.getLocation(), NEARBY_RADIUS, source);
            if (!nearby.isEmpty()) {
                target = nearby.get(0);
            }
        }

        if (target == null) {
            return;
        }

        var maxHealthAttribute = target.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : 20.0;
        double threshold = maxHealth * HEALTH_THRESHOLD_RATIO;

        if (target.getHealth() > threshold) {
            return; // Target is healthy — no bonus damage, no visual.
        }

        target.damage(BONUS_DAMAGE);

        // Dark/red concentrated burst + short outward spray on the target.
        Main plugin = JavaPlugin.getPlugin(Main.class);

        plugin.getParticleSystem().play(animationContext -> {
            Location origin = animationContext.getOrigin();

            if (origin.getWorld() == null) {
                return;
            }

            origin.getWorld().spawnParticle(
                    Particle.DUST,
                    origin,
                    24,
                    0.3, 0.3, 0.3,
                    0.0,
                    DUST_DARK_RED
            );

            for (int i = 0; i < 8; i++) {
                double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2.0);
                double radius = ThreadLocalRandom.current().nextDouble(1.2);

                origin.getWorld().spawnParticle(
                        Particle.DUST,
                        origin.clone().add(
                                Math.cos(angle) * radius,
                                ThreadLocalRandom.current().nextDouble(1.5),
                                Math.sin(angle) * radius
                        ),
                        1,
                        0, 0, 0,
                        0,
                        DUST_DARK_RED
                );
            }
        }, target.getLocation().add(0, 1.0, 0));
    }
}