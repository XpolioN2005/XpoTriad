package dev.xpolion.xpotriad.runtime.states;

import dev.xpolion.xpotriad.particle.ParticleHandle;
import dev.xpolion.xpotriad.particle.ParticleSystem;
import dev.xpolion.xpotriad.particle.animations.BarrierRingAnimation;
import dev.xpolion.xpotriad.runtime.RuntimeState;
import dev.xpolion.xpotriad.targeting.TargetResolver;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Circular boundary around a fixed point that nobody may cross in
 * either direction — only the caster can leave (and re-enter) freely.
 *
 * Radius and duration come from config. Every other living entity is
 * baselined on the side of the line where it was first seen; if it
 * later crosses the line it is pushed straight back (inside -> outside
 * attempts are clamped in, outside -> inside attempts are pushed out).
 *
 * Lifecycle: start -> clamp crossings each tick -> duration -> cleanup
 * (stops the boundary ring/wall particle handle).
 * Early stop: invalid entities are dropped from tracking.
 */
public final class BarrierState implements RuntimeState {

    /** Zeroed momentum after every boundary clamp. */
    private static final Vector ZERO_VELOCITY = new Vector(0.0, 0.0, 0.0);

    /** Entities are still queried this far outside the boundary. */
    private static final double QUERY_MARGIN = 1.5;
    /** Offset putting an entity decisively on one side of the line. */
    private static final double EDGE_EPSILON = 0.05;

    private final double radius;

    private final Player source;
    private final Location center;
    /** Tracked entities keyed by UUID — value holds their baseline side. */
    private final Map<UUID, Tracked> tracked = new HashMap<>();
    private final ParticleHandle ringHandle;

    private int ticksRemaining;
    private volatile boolean finished = false;

    public BarrierState(Player source, Location center, JavaPlugin plugin, ParticleSystem particleSystem,
                        double radius, int durationTicks) {
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null");
        }
        if (center == null || center.getWorld() == null) {
            throw new IllegalArgumentException("Center cannot be null");
        }
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        if (particleSystem == null) {
            throw new IllegalArgumentException("ParticleSystem cannot be null");
        }

        this.source = source;
        this.center = center.clone();
        this.radius = radius;
        this.ticksRemaining = durationTicks;

        double ringDurationSeconds = durationTicks / 20.0;

        this.ringHandle = particleSystem.playPersistent(
                new BarrierRingAnimation(radius),
                this.center.clone(),
                ringDurationSeconds
        );
    }

    @Override
    public void tick() {
        if (finished) {
            return;
        }

        ticksRemaining--;

        if (ticksRemaining <= 0) {
            stop();
            return;
        }

        List<LivingEntity> nearby =
                TargetResolver.entitiesNear(center, radius + QUERY_MARGIN);

        for (LivingEntity entity : nearby) {
            if (entity.equals(source)) {
                continue; // Caster may leave (and re-enter) freely.
            }

            boolean insideNow = isInside(entity);
            Tracked entry = tracked.get(entity.getUniqueId());

            if (entry == null) {
                // First sighting — baseline the side this entity started on.
                tracked.put(entity.getUniqueId(), new Tracked(entity, insideNow));
                continue;
            }

            if (entry.wasInside != insideNow) {
                // Crossed the line — push it back onto its baseline side.
                clamp(entity, entry.wasInside);
            }
        }

        // Invalid entities stop being tracked.
        tracked.entrySet().removeIf(entry ->
                entry.getValue().entity.isDead()
                        || !entry.getValue().entity.isValid());
    }

    private boolean isInside(LivingEntity entity) {
        double dx = entity.getLocation().getX() - center.getX();
        double dz = entity.getLocation().getZ() - center.getZ();
        return dx * dx + dz * dz <= radius * radius;
    }

    /**
     * Teleports the entity back onto its baseline side of the boundary
     * (keeps height, zeroes momentum).
     */
    private void clamp(LivingEntity entity, boolean toInside) {
        double dx = entity.getLocation().getX() - center.getX();
        double dz = entity.getLocation().getZ() - center.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance < 1.0E-6) {
            // Exactly on the center — push out along +X.
            dx = 1.0;
            dz = 0.0;
            distance = 1.0;
        }

        double targetDistance = toInside
                ? radius - EDGE_EPSILON
                : radius + EDGE_EPSILON;
        double scale = targetDistance / distance;

        Location clamped = entity.getLocation().clone();
        clamped.setX(center.getX() + dx * scale);
        clamped.setZ(center.getZ() + dz * scale);

        entity.teleport(clamped);
        entity.setVelocity(ZERO_VELOCITY);
    }

    /** Tracked entity + the side of the boundary it baselined on. */
    private static final class Tracked {

        private final LivingEntity entity;
        private final boolean wasInside;

        private Tracked(LivingEntity entity, boolean wasInside) {
            this.entity = entity;
            this.wasInside = wasInside;
        }
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public void stop() {
        if (finished) {
            return;
        }

        finished = true;

        if (ringHandle != null && ringHandle.isActive()) {
            ringHandle.stop();
        }
    }
}