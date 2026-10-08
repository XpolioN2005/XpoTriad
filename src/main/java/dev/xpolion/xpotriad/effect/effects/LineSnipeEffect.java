package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.runtime.states.LineSnipeState;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

/**
 * Fires a fast projectile along the caster's aimed direction.
 *
 * Targeting: LINE — eye raycast direction. Max travel = 30 blocks,
 * lifetime = 20 ticks, first valid target takes 8 damage.
 * Caster is always excluded (multiplayer safe).
 *
 * Runtime required to track the projectile; trail stops on impact,
 * wall, or after 20 ticks.
 */
public final class LineSnipeEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        Location origin = source.getEyeLocation().clone();
        Vector direction = origin.getDirection().normalize();

        Main plugin = JavaPlugin.getPlugin(Main.class);

        LineSnipeState state = new LineSnipeState(
                source,
                origin,
                direction,
                plugin,
                plugin.getParticleSystem()
        );

        plugin.getRuntimeManager().start(state);
    }
}