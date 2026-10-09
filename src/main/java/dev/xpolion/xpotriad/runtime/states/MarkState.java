package dev.xpolion.xpotriad.runtime.states;

import dev.xpolion.xpotriad.particle.ParticleHandle;
import dev.xpolion.xpotriad.particle.ParticleSystem;
import dev.xpolion.xpotriad.particle.animations.MarkAnimation;
import dev.xpolion.xpotriad.particle.animations.MarkBurstAnimation;
import dev.xpolion.xpotriad.runtime.RuntimeState;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class MarkState implements RuntimeState, Listener {

    private static final double TICK_DELTA_SECONDS = 0.05;

    private final LivingEntity target;
    private final ParticleSystem particleSystem;
    private final ParticleHandle particleHandle;

    private final double durationSeconds;
    private final double damageMultiplier;

    private double remainingSeconds;
    private volatile boolean finished = false;

    public MarkState(LivingEntity target, JavaPlugin plugin, ParticleSystem particleSystem,
                     double durationSeconds, double damageMultiplier) {
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
        this.durationSeconds = durationSeconds;
        this.damageMultiplier = damageMultiplier;
        this.remainingSeconds = durationSeconds;

        this.particleHandle = particleSystem.playPersistent(
                new MarkAnimation(),
                target,
                durationSeconds
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

        remainingSeconds -= TICK_DELTA_SECONDS;
        if (remainingSeconds <= 0) {
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
            particleSystem.play(new MarkBurstAnimation(), target.getLocation());
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
        HandlerList.unregisterAll(this);
        if (particleHandle != null && particleHandle.isActive()) {
            particleHandle.stop();
        }
    }
}
