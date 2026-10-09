package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.runtime.states.HoundCallState;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Summons exactly 5 tamed wolves owned by the caster.
 *
 * Targeting: SELF. Wolves last 200 ticks and are removed by the runtime.
 * Runtime required for lifetime tracking and cleanup.
 *
 * Visual: small smoke/dust burst at each summon position; no permanent
 * particle effect attached to the wolves.
 */
public final class HoundCallEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        BalanceConfig cfg = BalanceConfig.get();
        int wolfCount = Math.max(cfg.effectInt("hound_call", "count", 5), 1);
        double summonRingRadius = cfg.effectDouble("hound_call", "summon-ring-radius", 1.5);
        int lifetimeTicks = cfg.effectInt("hound_call", "lifetime-ticks", 200);

        Location base = source.getLocation();
        List<Wolf> wolves = new ArrayList<>(wolfCount);

        for (int i = 0; i < wolfCount; i++) {
            double angle = (double) i / wolfCount * Math.PI * 2.0;

            Location summonPos = base.clone().add(
                    Math.cos(angle) * summonRingRadius,
                    0.0,
                    Math.sin(angle) * summonRingRadius
            );

            Wolf wolf = source.getWorld().spawn(summonPos, Wolf.class, summoned -> {
                summoned.setTamed(true);
                summoned.setOwner(source);
            });

            wolves.add(wolf);
        }

        Main plugin = JavaPlugin.getPlugin(Main.class);

        // Small smoke/dust burst at each summon position.
        for (int i = 0; i < wolfCount; i++) {
            double angle = (double) i / wolfCount * Math.PI * 2.0;

            Location burstPos = base.clone().add(
                    Math.cos(angle) * summonRingRadius,
                    0.5,
                    Math.sin(angle) * summonRingRadius
            );

            plugin.getParticleSystem().play(animationContext -> {
                Location origin = animationContext.getOrigin();

                if (origin.getWorld() != null) {
                    origin.getWorld().spawnParticle(
                            Particle.CLOUD,
                            origin,
                            10,
                            0.3, 0.4, 0.3,
                            0.02
                    );
                }
            }, burstPos);
        }

        HoundCallState state = new HoundCallState(
                source,
                wolves,
                plugin,
                lifetimeTicks
        );

        plugin.getRuntimeManager().start(state);
    }
}