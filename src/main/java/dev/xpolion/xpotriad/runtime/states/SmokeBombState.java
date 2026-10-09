package dev.xpolion.xpotriad.runtime.states;

import dev.xpolion.xpotriad.particle.ParticleHandle;
import dev.xpolion.xpotriad.particle.ParticleSystem;
import dev.xpolion.xpotriad.particle.animations.SmokeCloudAnimation;
import dev.xpolion.xpotriad.runtime.RuntimeState;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Maintains a smoke area centered on the caster for exactly 100 ticks.
 *
 * Visual cover only — the smoke deals no damage and listens to nothing.
 * Lifecycle: start -> dense smoke (r1.0 -> 2.5 over first 20 ticks, then
 * held) -> 100 ticks -> cleanup (stops the smoke particle handle).
 * Early stop: target becomes invalid.
 */
public final class SmokeBombState implements RuntimeState {

    private final Player target;
    private final ParticleHandle smokeHandle;

    private int ticksRemaining;
    private volatile boolean finished = false;

    public SmokeBombState(Player target, JavaPlugin plugin, ParticleSystem particleSystem,
                          int durationTicks) {
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
        this.ticksRemaining = durationTicks;

        double durationSeconds = durationTicks / 20.0;

        this.smokeHandle = particleSystem.playPersistent(
                new SmokeCloudAnimation(),
                target,
                durationSeconds
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

        ticksRemaining--;

        if (ticksRemaining <= 0) {
            stop();
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

        if (smokeHandle != null && smokeHandle.isActive()) {
            smokeHandle.stop();
        }
    }
}