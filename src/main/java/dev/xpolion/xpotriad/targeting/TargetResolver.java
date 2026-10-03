package dev.xpolion.xpotriad.targeting;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Performs entity detection using Bukkit/Paper APIs.
 * Searches the world around an origin for living, valid entities.
 */
public final class TargetResolver {

    private TargetResolver() {
    }

    /**
     * Returns all valid living entities within {@code radius} blocks of {@code origin}.
     *
     * @param origin the center location
     * @param radius the search radius (non-negative)
     * @return an unmodifiable list of matching entities
     */
    public static List<Entity> entitiesNear(Location origin, double radius) {
        if (origin == null) {
            throw new IllegalArgumentException("Origin cannot be null");
        }
        if (radius < 0) {
            throw new IllegalArgumentException("Radius cannot be negative");
        }

        World world = origin.getWorld();

        if (world == null) {
            return List.of();
        }

        Collection<Entity> nearby = world.getNearbyEntities(origin, radius, radius, radius);

        List<Entity> result = new ArrayList<>();

        for (Entity entity : nearby) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (living.isDead()) {
                continue;
            }
            result.add(entity);
        }

        return List.copyOf(result);
    }
}
