package dev.xpolion.xpotriad.runtime.states;

import dev.xpolion.xpotriad.particle.ParticleHandle;
import dev.xpolion.xpotriad.particle.ParticleSystem;
import dev.xpolion.xpotriad.particle.animations.GravitySpiralAnimation;
import dev.xpolion.xpotriad.runtime.RuntimeState;
import dev.xpolion.xpotriad.targeting.TargetResolver;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Pull field at a fixed point.
 *
 * Radius = 4, duration = 60 ticks, pull every 5 ticks at strength 0.35.
 * Lifecycle: start -> pull entities (caster excluded) -> 60 ticks -> cleanup
 * (stops the spiralling ground-ring particle handle).
 * Early stop: duration expires. Entities are re-queried each pull, so
 * invalid entities are naturally skipped.
 */
public final class GravityPullState implements RuntimeState {

    private final double radius;
    private final int pullIntervalTicks;
    private final double pullStrength;

    private final Player source;
    private final Location center;
    private final ParticleHandle ringHandle;

    private int ticksRemaining;
    private int ticksUntilPull;
    private volatile boolean finished = false;

    public GravityPullState(Player source, Location center, JavaPlugin plugin, ParticleSystem particleSystem,
                            double radius, int durationTicks, int pullIntervalTicks, double pullStrength) {
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
        this.pullIntervalTicks = Math.max(pullIntervalTicks, 1);
        this.ticksUntilPull = this.pullIntervalTicks;
        this.pullStrength = pullStrength;

        this.ringHandle = particleSystem.playPersistent(
                new GravitySpiralAnimation(),
                this.center.clone(),
                durationTicks / 20.0
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

        ticksUntilPull--;

        if (ticksUntilPull <= 0) {
            ticksUntilPull = pullIntervalTicks;
            pull();
        }
    }

    private void pull() {
        List<LivingEntity> entities = TargetResolver.entitiesNear(center, radius, source);

        Vector pullBase = center.clone().add(0, 1.0, 0).toVector();

        for (LivingEntity entity : entities) {
            Vector pull = pullBase.subtract(entity.getLocation().toVector());

            if (pull.lengthSquared() < 1.0E-6) {
                continue;
            }

            pull.normalize().multiply(pullStrength);
            entity.setVelocity(entity.getVelocity().add(pull));
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