package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.runtime.states.SmokeBombState;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Creates a smoke area centered on the caster (visual cover only).
 *
 * Targeting: SELF. Duration from config.
 * Runtime required (maintains the temporary area and expires it).
 * Visual: dense smoke covering the full body; expands from radius
 * 1.0 to 2.5 over the first 20 ticks, then holds until expiry.
 */
public final class SmokeBombEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        BalanceConfig cfg = BalanceConfig.get();
        int durationTicks = cfg.effectInt("smoke_bomb", "duration-ticks", 100);

        Main plugin = JavaPlugin.getPlugin(Main.class);

        SmokeBombState state = new SmokeBombState(
                source,
                plugin,
                plugin.getParticleSystem(),
                durationTicks
        );

        plugin.getRuntimeManager().start(state);
    }
}