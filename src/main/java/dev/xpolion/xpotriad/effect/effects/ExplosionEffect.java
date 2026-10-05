package dev.xpolion.xpotriad.effect.effects;

import dev.xpolion.xpotriad.ability.Ability;
import dev.xpolion.xpotriad.ability.AbilityContext;
import dev.xpolion.xpotriad.effect.Effect;
import dev.xpolion.xpotriad.targeting.TargetResolver;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Creates an explosion and affects nearby entities using TargetResolver.
 *
 * WeaponType controls behavior:
 *   MELEE  - explosion centered at source player location
 *   RANGED - explosion centered at raycasted target or impact location
 */
public final class ExplosionEffect implements Effect {

    private static final double EXPLOSION_RADIUS = 4.0;
    private static final float EXPLOSION_POWER = 4.0f;
    private static final double RANGED_RAYCAST_DISTANCE = 15.0;

    @Override
    public void apply(AbilityContext context) {
        Player source = context.getSource();
        Location explosionLocation;

        if (context.getAbility().getWeaponType() == Ability.WeaponType.RANGED) {
            List<LivingEntity> raycastTargets = TargetResolver.raycast(source, RANGED_RAYCAST_DISTANCE);
            if (!raycastTargets.isEmpty()) {
                explosionLocation = raycastTargets.get(0).getLocation();
            } else {
                var blockHit = source.getWorld().rayTraceBlocks(
                        source.getEyeLocation(),
                        source.getEyeLocation().getDirection(),
                        RANGED_RAYCAST_DISTANCE
                );
                if (blockHit != null && blockHit.getHitPosition() != null) {
                    explosionLocation = blockHit.getHitPosition().toLocation(source.getWorld());
                } else {
                    explosionLocation = source.getLocation();
                }
            }
        } else {
            explosionLocation = source.getLocation();
        }

        // Query entities in the explosion area using TargetResolver (excluding source)
        List<LivingEntity> affected = TargetResolver.entitiesNear(explosionLocation, EXPLOSION_RADIUS, source);

        explosionLocation.getWorld().createExplosion(
                explosionLocation,
                EXPLOSION_POWER,
                false,
                false,
                source
        );
    }
}