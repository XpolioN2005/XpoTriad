package dev.xpolion.xpotriad.runtime.states;

import dev.xpolion.xpotriad.particle.ParticleHandle;
import dev.xpolion.xpotriad.particle.ParticleSystem;
import dev.xpolion.xpotriad.particle.animations.RewindRingsAnimation;
import dev.xpolion.xpotriad.runtime.RuntimeState;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Single-snapshot rewind: captures position + health ONCE at activation
 * and restores that snapshot after exactly 100 ticks.
 *
 * The two blue rings (opposite directions) rotate around the caster for
 * the first 10 ticks only — a finite particle, not gameplay state.
 *
 * Lifecycle: start (snapshot + rings) -> count to 100 -> burst at current
 * position, restore, burst at restored position -> cleanup (stops rings).
 * Early stop: caster becomes invalid.
 */
public final class RewindState implements RuntimeState {

    private static final int TRIGGER_TICKS = 100;
    private static final double RECORDING_VISUAL_SECONDS = 10.0 / 20.0;

    private static final Particle.DustOptions DUST_BLUE =
            new Particle.DustOptions(Color.fromRGB(90, 140, 255), 1.3f);

    private final Player target;
    private final Location snapshotLocation;
    private final double snapshotHealth;
    private final ParticleSystem particleSystem;
    private final ParticleHandle ringsHandle;

    private int ticksElapsed = 0;
    private volatile boolean finished = false;

    public RewindState(Player target, JavaPlugin plugin, ParticleSystem particleSystem) {
        if (target == null) {
            throw new IllegalArgumentException("Target cannot be null");
        }
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        if (particleSystem == null) {
            throw new IllegalArgumentException("ParticleSystem cannot be null");
        }

        this.target = target;
        this.particleSystem = particleSystem;

        // Single capture at the start — that's it.
        this.snapshotLocation = target.getLocation().clone();
        this.snapshotHealth = target.getHealth();

        this.ringsHandle = particleSystem.playPersistent(
                new RewindRingsAnimation(),
                target,
                RECORDING_VISUAL_SECONDS
        );
    }

    @Override
    public void tick() {
        if (finished) {
            return;
        }

        if (!target.isValid() || target.isDead()) {
            stop();
            return;
        }

        ticksElapsed++;

        if (ticksElapsed >= TRIGGER_TICKS) {
            trigger();
            stop();
        }
    }

    private void trigger() {
        // Burst at the current position.
        playBurst(target.getLocation().clone());

        // Restore the snapshot taken at activation.
        target.teleport(snapshotLocation);

        var maxHealthAttribute = target.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : target.getHealth();
        target.setHealth(Math.min(snapshotHealth, maxHealth));

        // Burst at the restored position.
        playBurst(target.getLocation().clone());
    }

    private void playBurst(Location position) {
        particleSystem.play(animationContext -> {
            Location origin = animationContext.getOrigin();

            if (origin.getWorld() != null) {
                origin.getWorld().spawnParticle(
                        Particle.DUST,
                        origin.clone().add(0, 1.0, 0),
                        30,
                        0.5, 1.0, 0.5,
                        0.0,
                        DUST_BLUE
                );
            }
        }, position);
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

        if (ringsHandle != null && ringsHandle.isActive()) {
            ringsHandle.stop();
        }
    }
}