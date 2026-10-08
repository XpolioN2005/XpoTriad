package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.targeting.TargetResolver;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Lifts the target into the air with Levitation for 60 ticks (amplifier 0).
 *
 * Targeting: sight raycast (12 blocks) -> fallback nearest within 3 blocks.
 * Caster is always excluded (multiplayer safe).
 * No custom runtime required — the potion effect handles the lift.
 *
 * Visual: vanilla-style rising particles around the target (one-shot).
 */
public final class LevitationEffect implements Effect {

    private static final double RAYCAST_RANGE = 12.0;
    private static final double NEARBY_RADIUS = 3.0;
    private static final int DURATION_TICKS = 60;

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

        target.addPotionEffect(new PotionEffect(
                PotionEffectType.LEVITATION,
                DURATION_TICKS,
                0,
                false,
                true,
                true
        ));

        // One-shot rising particles around the target.
        Main plugin = JavaPlugin.getPlugin(Main.class);

        plugin.getParticleSystem().play(animationContext -> {
            Location origin = animationContext.getOrigin();

            if (origin.getWorld() == null) {
                return;
            }

            for (int i = 0; i < 14; i++) {
                double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2.0);
                double radius = ThreadLocalRandom.current().nextDouble(0.6);

                origin.getWorld().spawnParticle(
                        Particle.CLOUD,
                        origin.clone().add(
                                Math.cos(angle) * radius,
                                ThreadLocalRandom.current().nextDouble(2.2),
                                Math.sin(angle) * radius
                        ),
                        1,
                                0, 0.05, 0,
                        0
                );
            }
        }, target.getLocation());
    }
}