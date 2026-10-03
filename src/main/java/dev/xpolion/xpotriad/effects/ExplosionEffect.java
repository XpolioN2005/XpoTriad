package dev.xpolion.xpotriad.effects;

import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.targeting.AoeTargeting;
import dev.xpolion.xpotriad.targeting.SingleTargeting;
import dev.xpolion.xpotriad.targeting.Targeting;
import dev.xpolion.xpotriad.targeting.TargetingType;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Creates an explosion at the relevant targeting origin.
 *
 * If targeting is AOE: explodes at the AoE origin (e.g. projectile impact point).
 * If targeting is SINGLE with a live entity: explodes at that entity's location.
 * Otherwise: falls back to the source player's location.
 *
 * power = 4, fire = false, block damage = false
 */
public final class ExplosionEffect implements Effect {

    @Override
    public void apply(AbilityContext context) {
        Player source    = context.getSource();
        Targeting targeting = context.getTargeting();

        Location explosionLocation = resolveLocation(context, source, targeting);

        explosionLocation.getWorld().createExplosion(
            explosionLocation,
            4.0f,
            false,
            false,
            source
        );
    }

    private Location resolveLocation(AbilityContext context, Player source, Targeting targeting) {
        if (targeting.getType() == TargetingType.AOE) {
            return ((AoeTargeting) targeting).getOrigin();
        }

        if (targeting.getType() == TargetingType.SINGLE) {
            var targets = targeting.resolve(context);

            if (!targets.isEmpty()) {
                return targets.get(0).getLocation();
            }
        }

        // Fallback: source player location
        return source.getLocation();
    }
}