package dev.xpolion.xpotriad.runtime.states;

import dev.xpolion.xpotriad.particle.ParticleHandle;
import dev.xpolion.xpotriad.particle.ParticleSystem;
import dev.xpolion.xpotriad.particle.animations.CheatDeathSphereAnimation;
import dev.xpolion.xpotriad.runtime.RuntimeState;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Complete incoming damage immunity for exactly 40 ticks.
 *
 * Lifecycle: start -> cancel every damage event on the caster ->
 * 40 ticks -> cleanup (unregisters listener, stops the golden sphere).
 * Early stop: caster becomes invalid.
 */
public final class CheatDeathState implements RuntimeState, Listener {

    private static final int DURATION_TICKS = 40;
    private static final double AURA_DURATION_SECONDS = DURATION_TICKS / 20.0;

    private final Player target;
    private final ParticleHandle auraHandle;

    private int ticksRemaining = DURATION_TICKS;
    private volatile boolean finished = false;

    public CheatDeathState(Player target, JavaPlugin plugin, ParticleSystem particleSystem) {
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

        this.auraHandle = particleSystem.playPersistent(
                new CheatDeathSphereAnimation(),
                target,
                AURA_DURATION_SECONDS
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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (finished) {
            return;
        }

        if (event.getEntity().equals(target)) {
            event.setCancelled(true);
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

        if (auraHandle != null && auraHandle.isActive()) {
            auraHandle.stop();
        }
    }
}