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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

/**
 * Blinds every enemy within a short radius around the caster.
 *
 * Targeting: self-centred AOE — {@code entitiesNear(player, radius, player)},
 * so the caster is never blinded. One-shot: applies Blindness to each caught
 * entity and plays a brief dark ring puff. No runtime required.
 */
public final class BlindEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        BalanceConfig cfg = BalanceConfig.get();
        double radius = cfg.effectDouble("blind", "radius", 6.0);
        int durationTicks = cfg.effectInt("blind", "potion-duration-ticks", 60);
        int amplifier = cfg.effectInt("blind", "amplifier", 0);

        List<LivingEntity> targets =
                TargetResolver.entitiesNear(source.getLocation(), radius, source);

        for (LivingEntity entity : targets) {
            entity.addPotionEffect(new PotionEffect(
                    PotionEffectType.BLINDNESS,
                    durationTicks,
                    amplifier,
                    false,
                    true,
                    false
            ));
        }

        // Brief darkening ring at the caster's feet so the cast is not silent.
        Main plugin = JavaPlugin.getPlugin(Main.class);

        plugin.getParticleSystem().play(ctx -> {
            Location origin = ctx.getOrigin();
            if (origin.getWorld() == null) {
                return;
            }

            Particle.DustOptions dark = new Particle.DustOptions(Color.fromRGB(20, 20, 25), 1.4f);

            for (int i = 0; i < 40; i++) {
                double angle = (2.0 * Math.PI / 40) * i;
                double r = radius * 0.9;
                origin.getWorld().spawnParticle(
                        Particle.DUST,
                        origin.clone().add(Math.cos(angle) * r, 0.6, Math.sin(angle) * r),
                        1,
                        0, 0, 0,
                        0,
                        dark
                );
            }
        }, source.getLocation());
    }
}
