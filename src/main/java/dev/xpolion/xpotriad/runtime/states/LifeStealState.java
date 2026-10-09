package dev.xpolion.xpotriad.runtime.states;

import dev.xpolion.xpotriad.particle.ParticleSystem;
import dev.xpolion.xpotriad.runtime.RuntimeState;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

/**
 * Heals the caster for 25% of qualifying damage the caster deals,
 * for exactly 100 ticks.
 *
 * Healing is clamped to maximum health. Lifecycle: start -> listen for
 * outgoing damage -> heal on each qualifying hit -> 100 ticks -> cleanup
 * (unregisters listener, small red burst at the caster).
 * Early stop: caster becomes invalid.
 */
public final class LifeStealState implements RuntimeState, Listener {

    private final double healRatio;

    private static final Particle.DustOptions DUST_RED =
            new Particle.DustOptions(Color.RED, 1.1f);

    private final Player target;
    private final ParticleSystem particleSystem;

    private int ticksRemaining;
    private volatile boolean finished = false;

    public LifeStealState(Player target, JavaPlugin plugin, ParticleSystem particleSystem,
                          int durationTicks, double healRatio) {
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
        this.healRatio = healRatio;
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

        // Qualifying damage: dealt BY the caster.
        if (!event.getDamager().equals(target)) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }

        if (victim.equals(target)) {
            return;
        }

        double healAmount = event.getDamage() * healRatio;

        if (healAmount <= 0.0) {
            return;
        }

        // Clamped to maximum health.
        var maxHealthAttribute = target.getAttribute(Attribute.MAX_HEALTH);
        double maxHealth = maxHealthAttribute != null ? maxHealthAttribute.getValue() : target.getHealth();
        target.setHealth(Math.min(target.getHealth() + healAmount, maxHealth));

        // Red particles travel from the victim toward the caster.
        Location from = victim.getLocation().add(0, 1.0, 0);
        Location to = target.getLocation().add(0, 1.0, 0);

        if (from.getWorld() == null || !from.getWorld().equals(to.getWorld())) {
            return;
        }

        Vector travel = to.toVector().subtract(from.toVector());

        particleSystem.play(animationContext -> {
            Location origin = animationContext.getOrigin();

            if (origin.getWorld() == null) {
                return;
            }

            for (double d = 0.0; d <= travel.length(); d += 0.4) {
                origin.getWorld().spawnParticle(
                        Particle.DUST,
                        origin.clone().add(travel.clone().multiply(d / Math.max(travel.length(), 1.0E-6))),
                        1,
                        0, 0, 0,
                        0,
                        DUST_RED
                );
            }
        }, from);
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

        // Finish with a small red burst around the caster.
        particleSystem.play(animationContext -> {
            Location origin = animationContext.getOrigin();

            if (origin.getWorld() != null) {
                origin.getWorld().spawnParticle(
                        Particle.DUST,
                        origin.clone().add(0, 1.0, 0),
                        16,
                        0.5, 0.8, 0.5,
                        0.0,
                        DUST_RED
                );
            }
        }, target.getLocation());
    }
}