package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.runtime.states.MeteorState;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bombards a static zone around the caster with meteors falling from
 * the sky.
 *
 * Targeting: SELF — the zone center is the caster's location at cast
 * time and never follows them. Barrage shape comes from config: count
 * meteors, staggered stagger-ticks apart, each spawned spawn-height
 * above the cast plane and falling straight down at fall-speed.
 *
 * On impact each meteor deals impact-damage within aoe-radius (caster
 * excluded — multiplayer safe), with an expanding burst and NO terrain
 * destruction (yield 0, no incendiary). Runtime required to schedule
 * and track every projectile; lifetime-ticks is the failsafe cap.
 */
public final class MeteorEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();

        BalanceConfig cfg = BalanceConfig.get();
        double zoneRadius = cfg.effectDouble("meteor", "zone-radius", 4.0);
        int count = cfg.effectInt("meteor", "count", 5);
        int staggerTicks = cfg.effectInt("meteor", "stagger-ticks", 4);
        int spawnHeight = cfg.effectInt("meteor", "spawn-height", 24);
        double fallSpeed = cfg.effectDouble("meteor", "fall-speed", 1.5);
        int lifetimeTicks = cfg.effectInt("meteor", "lifetime-ticks", 60);
        double aoeRadius = cfg.effectDouble("meteor", "aoe-radius", 4.0);
        double impactDamage = cfg.effectDouble("meteor", "impact-damage", 20.0);

        // Static zone center: caster's position at cast time.
        Location center = source.getLocation().clone();

        Main plugin = JavaPlugin.getPlugin(Main.class);

        MeteorState state = new MeteorState(
                source,
                center,
                plugin,
                plugin.getParticleSystem(),
                zoneRadius,
                count,
                staggerTicks,
                spawnHeight,
                fallSpeed,
                lifetimeTicks,
                aoeRadius,
                impactDamage
        );

        plugin.getRuntimeManager().start(state);
    }
}