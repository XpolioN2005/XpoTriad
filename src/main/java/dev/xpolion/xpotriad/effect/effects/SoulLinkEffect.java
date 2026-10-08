package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.runtime.states.SoulLinkState;
import dev.xpolion.xpotriad.targeting.TargetResolver;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Links caster and target; 30% of qualifying damage transfers between
 * them for 100 ticks.
 *
 * Targeting: sight raycast (12 blocks) -> fallback nearest within 3 blocks.
 * Caster is always excluded (multiplayer safe).
 * Runtime required; ends immediately if either entity becomes invalid.
 *
 * Visual: persistent tether following both entities, with particles
 * travelling along the tether on each transfer; stopped with the link.
 */
public final class SoulLinkEffect implements Effect {

    private static final double RAYCAST_RANGE = 12.0;
    private static final double NEARBY_RADIUS = 3.0;

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

        if (target == null || target.equals(source)) {
            return;
        }

        Main plugin = JavaPlugin.getPlugin(Main.class);

        SoulLinkState state = new SoulLinkState(
                source,
                target,
                plugin,
                plugin.getParticleSystem()
        );

        plugin.getRuntimeManager().start(state);
    }
}