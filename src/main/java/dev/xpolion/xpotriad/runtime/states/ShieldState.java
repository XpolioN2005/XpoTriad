package dev.xpolion.xpotriad.runtime.states;

import dev.xpolion.xpotriad.particle.ParticleHandle;
import dev.xpolion.xpotriad.particle.ParticleSystem;
import dev.xpolion.xpotriad.particle.animations.ShieldRingAnimation;
import dev.xpolion.xpotriad.runtime.RuntimeState;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Reduces incoming damage by 40% for exactly 100 ticks.
 *
 * Lifecycle: start -> intercept damage each event -> 100 ticks -> cleanup
 * (unregisters listener, stops the rotating r0.7 waist-height ring).
 * Early stop: target becomes invalid.
 */
public final class ShieldState implements RuntimeState, Listener {

    private final double damageMultiplier;

    private final Player target;
    private final ParticleHandle ringHandle;

    private int ticksRemaining;
    private volatile boolean finished = false;

    public ShieldState(Player target, JavaPlugin plugin, ParticleSystem particleSystem,
                       int durationTicks, double damageMultiplier) {
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
        this.damageMultiplier = damageMultiplier;
        this.ticksRemaining = durationTicks;

        double ringDurationSeconds = durationTicks / 20.0;

        this.ringHandle = particleSystem.playPersistent(
                new ShieldRingAnimation(),
                target,
                ringDurationSeconds
        );

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (finished) {
            return;
        }

        if (event.getEntity().equals(target)) {
            event.setDamage(event.getDamage() * damageMultiplier);
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
        HandlerList.unregisterAll(this);

        if (ringHandle != null && ringHandle.isActive()) {
            ringHandle.stop();
        }
    }
}