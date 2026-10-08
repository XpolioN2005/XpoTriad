package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.targeting.TargetResolver;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Pushes the target directly away from the caster.
 *
 * Targeting: sight raycast (12 blocks) -> fallback nearest within 3 blocks.
 * Caster is always excluded (multiplayer safe).
 *
 * Horizontal strength = 1.5, vertical velocity = 0.35. Immediate — no runtime.
 */
public final class KnockbackEffect implements Effect {

    private static final double RAYCAST_RANGE = 12.0;
    private static final double NEARBY_RADIUS = 3.0;
    private static final double HORIZONTAL_STRENGTH = 1.5;
    private static final double VERTICAL_VELOCITY = 0.35;

    private static final Particle.DustOptions DUST_WHITE =
            new Particle.DustOptions(Color.WHITE, 1.0f);

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

        // Away vector: horizontal from caster to target, fallback view direction.
        Vector away = target.getLocation().toVector().subtract(source.getLocation().toVector());
        away.setY(0.0);

        if (away.lengthSquared() < 1.0E-4) {
            away = source.getLocation().getDirection();
            away.setY(0.0);
        }

        away.normalize().multiply(HORIZONTAL_STRENGTH);
        away.setY(VERTICAL_VELOCITY);

        target.setVelocity(away);

        // Small impact burst + dust briefly spreading in the knockback direction.
        Vector direction = away.clone();
        direction.setY(0.0);

        if (direction.lengthSquared() < 1.0E-4) {
            direction = new Vector(1.0, 0.0, 0.0);
        }

        Vector finalDirection = direction.normalize();
        Main plugin = JavaPlugin.getPlugin(Main.class);

        plugin.getParticleSystem().play(animationContext -> {
            Location origin = animationContext.getOrigin();

            if (origin.getWorld() == null) {
                return;
            }

            origin.getWorld().spawnParticle(
                    Particle.DUST,
                    origin,
                    12,
                    0.3, 0.3, 0.3,
                    0.0,
                    DUST_WHITE
            );

            for (int i = 1; i <= 3; i++) {
                origin.getWorld().spawnParticle(
                        Particle.DUST,
                        origin.clone().add(finalDirection.clone().multiply(i * 0.5)),
                        4,
                        0.1, 0.1, 0.1,
                        0.0,
                        DUST_WHITE
                );
            }
        }, target.getLocation().add(0, 1.0, 0));
    }
}