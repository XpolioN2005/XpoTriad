package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.targeting.TargetResolver;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Creates an explosion centered at the source player's location and affects nearby entities.
 *
 * Targeting behavior is decided by this Effect: explosion is always self-positioned (melee-style).
 * TargetResolver resolves affected entities within the explosion radius.
 */
public final class ExplosionEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();
        Location explosionLocation = source.getLocation();

        BalanceConfig cfg = BalanceConfig.get();
        double radius = cfg.effectDouble("explosion", "radius", 4.0);
        float power = cfg.effectFloat("explosion", "power", 4.0f);

        // Query entities in the explosion area using TargetResolver (excluding source)
        List<LivingEntity> affected = TargetResolver.entitiesNear(explosionLocation, radius, source);

        explosionLocation.getWorld().createExplosion(
                explosionLocation,
                power,
                false,
                false,
                source
        );
    }
}