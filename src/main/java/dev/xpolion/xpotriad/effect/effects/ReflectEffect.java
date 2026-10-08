package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.runtime.states.ReflectState;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Reflects incoming projectiles back at their shooter for 80 ticks.
 *
 * Targeting: SELF. Projectiles ONLY — melee attacks are never reflected,
 * so Reflect cannot act as pseudo-immunity (that is Cheat Death's job).
 * Runtime required because it listens for incoming attacks.
 * Reflected projectiles are tracked by UUID and never re-trigger Reflect.
 */
public final class ReflectEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        Main plugin = JavaPlugin.getPlugin(Main.class);

        ReflectState state = new ReflectState(
                source,
                plugin,
                plugin.getParticleSystem()
        );

        plugin.getRuntimeManager().start(state);
    }
}