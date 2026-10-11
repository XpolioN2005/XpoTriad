package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.config.BalanceConfig;
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

    private static final Particle.DustOptions DUST_WHITE =
            new Particle.DustOptions(Color.WHITE, 1.0f);

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        BalanceConfig cfg = BalanceConfig.get();
        double raycastRange = cfg.singleTargetRaycastRange();
        double nearbyRadius = cfg.singleTargetNearbyRadius();
        double horizontalStrength = cfg.effectDouble("knockback", "horizontal-strength", 1.5);
        double verticalVelocity = cfg.effectDouble("knockback", "vertical-velocity", 0.35);

        LivingEntity target = null;

        List<LivingEntity> raycastTargets = TargetResolver.raycast(source, raycastRange);
        if (!raycastTargets.isEmpty()) {
            target = raycastTargets.get(0);
        } else {
            List<LivingEntity> nearby = TargetResolver.entitiesNear(source.getLocation(), nearbyRadius, source);
            if (!nearby.isEmpty()) {
                target = nearby.get(0);
            }
        }

        if (target == null) {
            // No target: play the same gust from just in front of the caster,
            // streaking along their facing, so the swing still reads.
            Vector facing = source.getLocation().getDirection().clone();
            facing.setY(0.0);

            if (facing.lengthSquared() < 1.0E-4) {
                facing = new Vector(1.0, 0.0, 0.0);
            }

            facing.normalize();
            Location origin = source.getLocation()
                    .add(facing.clone().multiply(1.5))
                    .add(0, 1.0, 0);

            playGust(origin, facing);
            return;
        }

        // Away vector: horizontal from caster to target, fallback view direction.
        Vector away = target.getLocation().toVector().subtract(source.getLocation().toVector());
        away.setY(0.0);

        if (away.lengthSquared() < 1.0E-4) {
            away = source.getLocation().getDirection();
            away.setY(0.0);
        }

        away.normalize().multiply(horizontalStrength);
        away.setY(verticalVelocity);

        target.setVelocity(away);

        // Small impact burst + dust briefly spreading in the knockback direction.
        Vector direction = away.clone();
        direction.setY(0.0);

        if (direction.lengthSquared() < 1.0E-4) {
            direction = new Vector(1.0, 0.0, 0.0);
        }

        Vector finalDirection = direction.normalize();
        playGust(target.getLocation().add(0, 1.0, 0), finalDirection);
    }

    /**
     * The knockback gust: a dust burst at {@code origin} with dust briefly
     * spreading along {@code direction}. Shared by the hit and miss paths so
     * both look identical.
     */
    private void playGust(Location origin, Vector direction) {
        Main plugin = JavaPlugin.getPlugin(Main.class);

        plugin.getParticleSystem().play(animationContext -> {
            Location animOrigin = animationContext.getOrigin();

            if (animOrigin.getWorld() == null) {
                return;
            }

            animOrigin.getWorld().spawnParticle(
                    Particle.DUST,
                    animOrigin,
                    12,
                    0.3, 0.3, 0.3,
                    0.0,
                    DUST_WHITE
            );

            for (int i = 1; i <= 3; i++) {
                animOrigin.getWorld().spawnParticle(
                        Particle.DUST,
                        animOrigin.clone().add(direction.clone().multiply(i * 0.5)),
                        4,
                        0.1, 0.1, 0.1,
                        0.0,
                        DUST_WHITE
                );
            }
        }, origin);
    }
}