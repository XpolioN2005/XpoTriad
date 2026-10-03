package dev.xpolion.xpotriad.targeting;

import dev.xpolion.xpotriad.ability.AbilityContext;
import org.bukkit.entity.Entity;

import java.util.Collections;
import java.util.List;

/**
 * Represents a single already-determined target.
 * Used for melee hits and projectile-entity hits.
 * Supports a null target for projectile-block hits.
 */
public final class SingleTargeting implements Targeting {

    private final Entity primaryTarget;

    public SingleTargeting(Entity target) {
        this.primaryTarget = target;
    }

    @Override
    public TargetingType getType() {
        return TargetingType.SINGLE;
    }

    @Override
    public List<Entity> resolve(AbilityContext context) {
        if (primaryTarget == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(primaryTarget);
    }

    public Entity getPrimaryTarget() {
        return primaryTarget;
    }
}
