package dev.xpolion.xpotriad.runtime.states;

import dev.xpolion.xpotriad.particle.ParticleHandle;
import dev.xpolion.xpotriad.particle.ParticleSystem;
import dev.xpolion.xpotriad.particle.animations.MassFreezeRingAnimation;
import dev.xpolion.xpotriad.runtime.RuntimeState;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Freezes captured entities for exactly 60 ticks by zeroing their
 * velocity every tick — works on players as well as mobs (no AI
 * manipulation involved).
 *
 * Lifecycle: start -> freeze + frost particles -> 60 ticks -> cleanup
 * (stops the area frost ring). Early stop: frozen entities that become
 * invalid are dropped from tracking.
 * Caster was excluded when the list was captured (multiplayer safe).
 */
public final class MassFreezeState implements RuntimeState {

    private static final Vector ZERO_VELOCITY = new Vector(0.0, 0.0, 0.0);

    private final Player source;
    private final Location center;
    private final List<LivingEntity> frozen;
    private final ParticleSystem particleSystem;
    private final ParticleHandle ringHandle;

    private int ticksRemaining;
    private volatile boolean finished = false;

    public MassFreezeState(
            Player source,
            Location center,
            List<LivingEntity> frozen,
            JavaPlugin plugin,
            ParticleSystem particleSystem,
            double radius,
            int durationTicks
    ) {
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null");
        }
        if (center == null || center.getWorld() == null) {
            throw new IllegalArgumentException("Center cannot be null");
        }
        if (frozen == null || frozen.isEmpty()) {
            throw new IllegalArgumentException("Frozen list cannot be empty");
        }
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        if (particleSystem == null) {
            throw new IllegalArgumentException("ParticleSystem cannot be null");
        }

        this.source = source;
        this.center = center.clone();
        this.frozen = new ArrayList<>(frozen);
        this.particleSystem = particleSystem;
        this.ticksRemaining = durationTicks;

        this.ringHandle = particleSystem.playPersistent(
                new MassFreezeRingAnimation(radius),
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

        Iterator<LivingEntity> iterator = frozen.iterator();

        while (iterator.hasNext()) {
            LivingEntity entity = iterator.next();

            if (entity.isDead() || !entity.isValid()) {
                iterator.remove(); // End early for invalid entities.
                continue;
            }

            // Freeze: movement = 0 each tick (player-compatible).
            entity.setVelocity(ZERO_VELOCITY);

            // Thick frost around the entity's legs ("stuck" look).
            particleSystem.play(animationContext -> {
                Location origin = animationContext.getOrigin();

                if (origin.getWorld() == null) {
                    return;
                }

                for (int i = 0; i < 6; i++) {
                    double angle = Math.random() * Math.PI * 2.0;
                    double radius = 0.25 + Math.random() * 0.15;

                    origin.getWorld().spawnParticle(
                            org.bukkit.Particle.SNOWFLAKE,
                            origin.clone().add(
                                    Math.cos(angle) * radius,
                                    Math.random() * 0.4,
                                    Math.sin(angle) * radius
                            ),
                            1,
                            0, 0, 0,
                            0
                    );
                }
            }, entity.getLocation());
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

        frozen.clear();

        if (ringHandle != null && ringHandle.isActive()) {
            ringHandle.stop();
        }
    }
}