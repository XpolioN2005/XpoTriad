package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
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

        if (target == null) {
            return;
        }

        Main plugin = JavaPlugin.getPlugin(Main.class);
        RuntimeManager runtimeManager = plugin.getRuntimeManager();
        ParticleSystem particleSystem = plugin.getParticleSystem();

        MarkState state = new MarkState(target, plugin, particleSystem);
        runtimeManager.start(state);
    }
}
