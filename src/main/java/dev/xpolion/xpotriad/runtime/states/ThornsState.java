package dev.xpolion.xpotriad.runtime.states;

import dev.xpolion.xpotriad.particle.ParticleSystem;
import dev.xpolion.xpotriad.runtime.RuntimeState;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Reflects 30% of qualifying melee damage back to the attacker for
 * exactly 100 ticks.
 *
 * Lifecycle: start -> listen for incoming melee damage -> retaliate ->
 * 100 ticks -> cleanup (unregisters listener).
 * Early stop: target becomes invalid.
 *
 * Reentrancy: the shared {@link #reflecting} guard spans ALL Thorns states
 * so reflected damage can never chain into another reflect loop.
 */
public final class ThornsState implements RuntimeState, Listener {

    private static final int DURATION_TICKS = 100;
    private static final double REFLECT_RATIO = 0.3;

    /** Shared synchronous guard — Bukkit events run on one thread. */
    private static volatile boolean reflecting = false;

    private static final Particle.DustOptions DUST_SHARP =
            new Particle.DustOptions(Color.RED, 1.2f);
    private static final Particle.DustOptions DUST_DEFENSE =
            new Particle.DustOptions(Color.fromRGB(120, 255, 140), 0.8f);

    private final Player target;
    private final ParticleSystem particleSystem;

    private int ticksRemaining = DURATION_TICKS;
    private volatile boolean finished = false;

    public ThornsState(Player target, JavaPlugin plugin, ParticleSystem particleSystem) {
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
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (finished || reflecting) {
            return;
        }

        if (!event.getEntity().equals(target)) {
            return;
        }

        if (!(event.getDamager() instanceof LivingEntity attacker)) {
            return;
        }

        // Qualifying melee damage only — projectiles never reflect here.
        if (attacker instanceof Projectile) {
            return;
        }

        if (attacker.equals(target)) {
            return;
        }

        double reflectAmount = event.getDamage() * REFLECT_RATIO;

        if (reflectAmount <= 0.0) {
            return;
        }

        reflecting = true;

        try {
            attacker.damage(reflectAmount);
        } finally {
            reflecting = false;
        }

        // Sharp burst around the attacker, smaller defensive burst at target.
        particleSystem.play(animationContext -> {
            var origin = animationContext.getOrigin();

            if (origin.getWorld() != null) {
                origin.getWorld().spawnParticle(
                        Particle.DUST,
                        origin.clone().add(0, 1.0, 0),
                        14,
                        0.4, 0.6, 0.4,
                        0.0,
                        DUST_SHARP
                );
            }
        }, attacker.getLocation());

        particleSystem.play(animationContext -> {
            var origin = animationContext.getOrigin();

            if (origin.getWorld() != null) {
                origin.getWorld().spawnParticle(
                        Particle.DUST,
                        origin.clone().add(0, 1.0, 0),
                        7,
                        0.3, 0.3, 0.3,
                        0.0,
                        DUST_DEFENSE
                );
            }
        }, target.getLocation());
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
    }
}