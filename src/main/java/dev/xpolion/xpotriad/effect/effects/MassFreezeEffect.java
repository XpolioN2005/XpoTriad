package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.runtime.states.MassFreezeState;
import dev.xpolion.xpotriad.targeting.TargetResolver;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Freezes every entity in a configured radius around the caster's
 * position at cast time.
 *
 * Targeting: SELF — the center is static (never follows the caster);
 * entitiesNear resolves everyone in area-radius, caster excluded
 * (multiplayer safe).
 *
 * Movement prevention is pure per-tick velocity zeroing, so it works on
 * players as well as mobs. Runtime required; releases entities after
 * the duration and drops invalid entities.
 */
public final class MassFreezeEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        BalanceConfig cfg = BalanceConfig.get();
        double areaRadius = cfg.effectDouble("mass_freeze", "area-radius", 5.0);
        int durationTicks = cfg.effectInt("mass_freeze", "duration-ticks", 60);

        // Static center: caster's position at cast time — never follows.
        Location center = source.getLocation().clone();

        List<LivingEntity> frozen = TargetResolver.entitiesNear(center, areaRadius, source);

        if (frozen.isEmpty()) {
            return; // Nothing to freeze — no runtime, no visuals.
        }

        Main plugin = JavaPlugin.getPlugin(Main.class);

        MassFreezeState state = new MassFreezeState(
                source,
                center,
                frozen,
                plugin,
                plugin.getParticleSystem(),
                areaRadius,
                durationTicks
        );

        plugin.getRuntimeManager().start(state);
    }
}