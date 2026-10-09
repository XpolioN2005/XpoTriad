package dev.xpolion.xpotriad.runtime.states;

import dev.xpolion.xpotriad.particle.ParticleSystem;
import dev.xpolion.xpotriad.runtime.RuntimeState;

import org.bukkit.Color;
import org.bukkit.Location;
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
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Reflects incoming PROJECTILES back at their shooter for exactly 80 ticks.
 *
 * Projectiles only: melee attacks are never reflected, so this can never
 * behave like Cheat Death. Reflected projectiles are tracked by UUID and
 * never re-trigger the handler (no recursion).
 *
 * Lifecycle: start -> cancel + redirect projectiles -> 80 ticks -> cleanup
 * (unregisters listener, drops UUID tracking).
 * Early stop: target becomes invalid.
 */
public final class ReflectState implements RuntimeState, Listener {

    private static final Particle.DustOptions DUST_GOLD =
            new Particle.DustOptions(Color.fromRGB(255, 215, 0), 1.2f);

    private final Player target;
    private final ParticleSystem particleSystem;
    private final Set<UUID> reflectedProjectiles = new HashSet<>();

    private int ticksRemaining;
    private volatile boolean finished = false;

    public ReflectState(Player target, JavaPlugin plugin, ParticleSystem particleSystem,
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
        this.particleSystem = particleSystem;
        this.ticksRemaining = durationTicks;

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
        if (finished) {
            return;
        }

        if (!event.getEntity().equals(target)) {
            return;
        }

        // Projectiles only — melee passes through untouched.
        if (!(event.getDamager() instanceof Projectile projectile)) {
            return;
        }

        if (reflectedProjectiles.contains(projectile.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        reflectedProjectiles.add(projectile.getUniqueId());

        Vector velocity = projectile.getVelocity();
        ProjectileSource shooter = projectile.getShooter();
        Vector reflected;

        if (shooter instanceof LivingEntity living
                && living.isValid()
                && !living.isDead()
                && !living.equals(target)) {
            // Send it back toward whoever fired it.
            reflected = living.getLocation().add(0, 1.0, 0).toVector()
                    .subtract(projectile.getLocation().toVector());

            if (reflected.lengthSquared() < 1.0E-6) {
                reflected = velocity.clone().multiply(-1.0);
            } else {
                reflected.normalize().multiply(Math.max(velocity.length(), 1.0));
            }
        } else {
            reflected = velocity.clone().multiply(-1.0);
        }

        projectile.setVelocity(reflected);

        // Brief defensive flash around the caster.
        particleSystem.play(animationContext -> {
            Location origin = animationContext.getOrigin();

            if (origin.getWorld() != null) {
                origin.getWorld().spawnParticle(
                        Particle.FLASH,
                        origin.clone().add(0, 1.0, 0),
                        1,
                        0, 0, 0,
                        0
                );
            }
        }, target.getLocation());

        // Short particle trail along the reflected direction.
        Vector trailDirection = reflected.clone().normalize();
        Location trailOrigin = projectile.getLocation().clone();

        particleSystem.play(animationContext -> {
            Location origin = animationContext.getOrigin();

            if (origin.getWorld() == null) {
                return;
            }

            for (double d = 0.0; d <= 3.0; d += 0.5) {
                origin.getWorld().spawnParticle(
                        Particle.DUST,
                        origin.clone().add(trailDirection.clone().multiply(d)),
                        1,
                        0, 0, 0,
                        0,
                        DUST_GOLD
                );
            }
        }, trailOrigin);
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
        reflectedProjectiles.clear();
        HandlerList.unregisterAll(this);
    }
}