package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.particle.ParticleSystem;
import dev.xpolion.xpotriad.runtime.RuntimeManager;
import dev.xpolion.xpotriad.runtime.states.MarkState;
import dev.xpolion.xpotriad.targeting.TargetResolver;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class MarkEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();
        LivingEntity target = null;

        BalanceConfig cfg = BalanceConfig.get();
        double raycastRange = cfg.singleTargetRaycastRange();
        double nearbyRadius = cfg.singleTargetNearbyRadius();
        double durationSeconds = cfg.effectLong("mark", "duration-ticks", 200L) / 20.0;
        double damageMultiplier = cfg.effectDouble("mark", "damage-multiplier", 1.5);

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

        Main plugin = JavaPlugin.getPlugin(Main.class);
        RuntimeManager runtimeManager = plugin.getRuntimeManager();
        ParticleSystem particleSystem = plugin.getParticleSystem();

        MarkState state = new MarkState(target, plugin, particleSystem, durationSeconds, damageMultiplier);
        runtimeManager.start(state);
    }
}
