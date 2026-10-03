package dev.xpolion.xpotriad.targeting;

import dev.xpolion.xpotriad.ability.AbilityContext;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.List;

/**
 * Represents an area-of-effect targeting centered on a fixed origin.
 * The entity list is resolved on demand; the origin and radius are stable.
 */
public final class AoeTargeting implements Targeting {

    private final Location origin;
    private final double radius;

    /**
     * @param origin the center of the AOE; cloned internally
     * @param radius must be non-negative
     */
    public AoeTargeting(Location origin, double radius) {
        if (origin == null) {
            throw new IllegalArgumentException("Origin cannot be null");
        }
        if (radius < 0) {
            throw new IllegalArgumentException("Radius cannot be negative");
        }
        this.origin = origin.clone();
        this.radius = radius;
    }

    @Override
    public TargetingType getType() {
        return TargetingType.AOE;
    }

    @Override
    public List<Entity> resolve(AbilityContext context) {
        return TargetResolver.entitiesNear(origin, radius);
    }

    /** Returns a defensive copy of the stored origin. */
    public Location getOrigin() {
        return origin.clone();
    }

    public double getRadius() {
        return radius;
    }
}
