package dev.xpolion.xpotriad.targeting;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Concrete spatial-query utility providing geometry implementations.
 *
 * Answers: "Which entities are inside this geometry?"
 * Effects decide what should happen to the resolved entities.
 */
public final class TargetResolver {

    private TargetResolver() {
    }

    /**
     * Performs a raycast from the source entity's eye location along its view direction.
     *
     * @param source the source entity
     * @param range  maximum raycast distance (non-negative)
     * @return ordered list of living entities intersected by the ray (closest first), excluding source
     */
    public static List<LivingEntity> raycast(LivingEntity source, double range) {
        if (source == null) {
            return List.of();
        }
        if (range < 0) {
            throw new IllegalArgumentException("Range cannot be negative");
        }
        return raycast(source.getEyeLocation(), source.getEyeLocation().getDirection(), range, source);
    }

    /**
     * Performs a raycast from origin along direction up to range, stopping at solid blocks.
     *
     * @param origin    start location of ray
     * @param direction direction vector
     * @param range     maximum distance (non-negative)
     * @param excluded  entity to exclude from hits (e.g. source)
     * @return ordered list of living entities intersected by the ray (closest first)
     */
    public static List<LivingEntity> raycast(Location origin, Vector direction, double range, Entity excluded) {
        if (origin == null || origin.getWorld() == null || direction == null) {
            return List.of();
        }
        if (range < 0) {
            throw new IllegalArgumentException("Range cannot be negative");
        }

        World world = origin.getWorld();
        Vector dir = direction.clone().normalize();
        Vector start = origin.toVector();

        double effectiveRange = range;
        RayTraceResult blockHit = world.rayTraceBlocks(origin, dir, range, FluidCollisionMode.NEVER, true);
        if (blockHit != null && blockHit.getHitPosition() != null) {
            effectiveRange = start.distance(blockHit.getHitPosition());
        }

        Vector end = start.clone().add(dir.clone().multiply(effectiveRange));
        BoundingBox searchBox = BoundingBox.of(start, end).expand(1.0);
        Collection<Entity> candidates = world.getNearbyEntities(searchBox);

        record HitEntity(LivingEntity entity, double distance) {}
        List<HitEntity> hits = new ArrayList<>();

        for (Entity entity : candidates) {
            if (entity == excluded) {
                continue;
            }
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (living.isDead() || !living.isValid()) {
                continue;
            }

            BoundingBox bb = living.getBoundingBox().expand(0.1);
            RayTraceResult hit = bb.rayTrace(start, dir, effectiveRange);
            if (hit != null && hit.getHitPosition() != null) {
                double dist = start.distance(hit.getHitPosition());
                hits.add(new HitEntity(living, dist));
            }
        }

        hits.sort(Comparator.comparingDouble(HitEntity::distance));

        List<LivingEntity> result = new ArrayList<>(hits.size());
        for (HitEntity hit : hits) {
            result.add(hit.entity());
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Resolves all valid living entities within radius of center.
     *
     * @param center center location
     * @param radius sphere radius (non-negative)
     * @return list of living entities within the radius
     */
    public static List<LivingEntity> entitiesNear(Location center, double radius) {
        return entitiesNear(center, radius, null);
    }

    /**
     * Resolves all valid living entities within radius of center, excluding an entity.
     *
     * @param center   center location
     * @param radius   sphere radius (non-negative)
     * @param excluded entity to exclude (e.g. source)
     * @return list of living entities within the radius
     */
    public static List<LivingEntity> entitiesNear(Location center, double radius, Entity excluded) {
        if (center == null || center.getWorld() == null) {
            return List.of();
        }
        if (radius < 0) {
            throw new IllegalArgumentException("Radius cannot be negative");
        }

        World world = center.getWorld();
        Collection<Entity> nearby = world.getNearbyEntities(center, radius, radius, radius);
        double radiusSquared = radius * radius;
        List<LivingEntity> result = new ArrayList<>();

        for (Entity entity : nearby) {
            if (entity == excluded) {
                continue;
            }
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (living.isDead() || !living.isValid()) {
                continue;
            }
            if (living.getLocation().distanceSquared(center) <= radiusSquared) {
                result.add(living);
            }
        }

        return Collections.unmodifiableList(result);
    }
}
