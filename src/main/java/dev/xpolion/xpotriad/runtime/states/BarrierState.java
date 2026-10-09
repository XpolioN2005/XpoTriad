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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Circular boundary around a fixed point that prevents the entities
 * captured inside it from crossing outward.
 *
 * Radius = 3, duration = 100 ticks. Caster is excluded at capture
 * (multiplayer safe). Lifecycle: start -> clamp crossings each tick ->
 * 100 ticks -> cleanup (stops the boundary ring particle handle).
 * Early stop: captured entities that become invalid are dropped from
 * tracking.
 */
public final class BarrierState implements RuntimeState {

    private final double radius;

    private final Player source;
    private final Location center;
    private final List<LivingEntity> inside = new ArrayList<>();
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

        // Capture the entities currently inside the boundary (caster excluded).
        this.inside.addAll(TargetResolver.entitiesNear(this.center, radius, source));

        double ringDurationSeconds = durationTicks / 20.0;

        this.ringHandle = particleSystem.playPersistent(
                new BarrierRingAnimation(),
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

        Iterator<LivingEntity> iterator = inside.iterator();

        while (iterator.hasNext()) {
            LivingEntity entity = iterator.next();

            if (entity.isDead() || !entity.isValid()) {
                iterator.remove(); // Invalid entities stop being tracked.
                continue;
            }

            double dx = entity.getLocation().getX() - center.getX();
            double dz = entity.getLocation().getZ() - center.getZ();
            double distanceSquared = dx * dx + dz * dz;

            if (distanceSquared > radius * radius) {
                // Clamp back onto the boundary circle (keep entity height).
                double distance = Math.sqrt(distanceSquared);
                double scale = radius / distance;

                Location clamped = entity.getLocation().clone();
                clamped.setX(center.getX() + dx * scale);
                clamped.setZ(center.getZ() + dz * scale);

                entity.teleport(clamped);
                entity.setVelocity(new Vector(0.0, 0.0, 0.0));
            }
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