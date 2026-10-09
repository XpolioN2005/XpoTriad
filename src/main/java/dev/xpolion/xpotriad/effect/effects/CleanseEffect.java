package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.targeting.TargetResolver;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Removes all negative potion effects from the target (immediate, no runtime).
 *
 * Targeting: sight raycast -> fallback nearest within 3 blocks (shared
 * single-target ranges from config). Caster is always excluded
 * (multiplayer safe).
 *
 * Visual: villager happy particle burst at the target, radius ~1.5.
 */
public final class CleanseEffect implements Effect {

    private static final double VISUAL_RADIUS = 1.5;

    /** Negative status effects removed by the cleanse. */
    private static final Set<PotionEffectType> NEGATIVE_EFFECTS = Set.of(
            PotionEffectType.SLOWNESS,
            PotionEffectType.MINING_FATIGUE,
            PotionEffectType.WEAKNESS,
            PotionEffectType.POISON,
            PotionEffectType.WITHER,
            PotionEffectType.BLINDNESS,
            PotionEffectType.HUNGER,
            PotionEffectType.UNLUCK,
            PotionEffectType.LEVITATION,
            PotionEffectType.DARKNESS
    );

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        BalanceConfig cfg = BalanceConfig.get();
        double raycastRange = cfg.singleTargetRaycastRange();
        double nearbyRadius = cfg.singleTargetNearbyRadius();

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
            return;
        }

        for (var effect : target.getActivePotionEffects()) {
            if (NEGATIVE_EFFECTS.contains(effect.getType())) {
                target.removePotionEffect(effect.getType());
            }
        }

        // One-shot villager happy burst centered on the target.
        Main plugin = JavaPlugin.getPlugin(Main.class);

        plugin.getParticleSystem().play(animationContext -> {
            Location origin = animationContext.getOrigin();

            if (origin.getWorld() == null) {
                return;
            }

            for (int i = 0; i < 20; i++) {
                double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2.0);
                double radial = VISUAL_RADIUS * Math.sqrt(ThreadLocalRandom.current().nextDouble());
                double height = ThreadLocalRandom.current().nextDouble(2.0);

                origin.getWorld().spawnParticle(
                        Particle.HAPPY_VILLAGER,
                        origin.clone().add(
                                Math.cos(angle) * radial,
                                height,
                                Math.sin(angle) * radial
                        ),
                        1,
                        0, 0, 0, 0
                );
            }
        }, target.getLocation());
    }
}