package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.runtime.states.SoulLinkState;
import dev.xpolion.xpotriad.targeting.TargetResolver;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Links caster and target; a configured ratio of qualifying damage transfers
 * between them for a configured duration.
 *
 * Targeting: sight raycast -> fallback nearest within the shared nearby
 * radius. Caster is always excluded (multiplayer safe).
 * Runtime required; ends immediately if either entity becomes invalid.
 *
 * Visual: persistent tether following both entities, with particles
 * travelling along the tether on each transfer; stopped with the link.
 */
public final class SoulLinkEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        BalanceConfig cfg = BalanceConfig.get();
        double raycastRange = cfg.singleTargetRaycastRange();
        double nearbyRadius = cfg.singleTargetNearbyRadius();
        int durationTicks = cfg.effectInt("soul_link", "duration-ticks", 100);
        double transferRatio = cfg.effectDouble("soul_link", "transfer-ratio", 0.3);

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

        if (target == null || target.equals(source)) {
            return;
        }

        Main plugin = JavaPlugin.getPlugin(Main.class);

        SoulLinkState state = new SoulLinkState(
                source,
                target,
                plugin,
                plugin.getParticleSystem(),
                durationTicks,
                transferRatio
        );

        plugin.getRuntimeManager().start(state);
    }
}