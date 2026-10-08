package dev.xpolion.xpotriad.runtime.states;

import dev.xpolion.xpotriad.particle.ParticleHandle;
import dev.xpolion.xpotriad.particle.ParticleSystem;
import dev.xpolion.xpotriad.particle.animations.SoulTetherAnimation;
import dev.xpolion.xpotriad.runtime.RuntimeState;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
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
 * Links caster and target: 30% of qualifying damage transfers between
 * them for exactly 100 ticks.
 *
 * Lifecycle: start -> listen to damage on either end -> transfer one hop
 * (reentrancy guard prevents ping-pong) -> 100 ticks -> cleanup
 * (unregisters listener, stops the tether particle handle).
 * Early stop: either entity becomes invalid.
 */
public final class SoulLinkState implements RuntimeState, Listener {

    private static final int DURATION_TICKS = 100;
    private static final double TRANSFER_RATIO = 0.3;

    /** Shared synchronous guard — transferred damage never re-transfers. */
    private static volatile boolean transferring = false;

    private static final Particle.DustOptions DUST_SOUL =
            new Particle.DustOptions(Color.fromRGB(120, 220, 255), 1.0f);

    private final Player source;
    private final LivingEntity target;
    private final ParticleSystem particleSystem;
    private final ParticleHandle tetherHandle;

    private int ticksRemaining = DURATION_TICKS;
    private volatile boolean finished = false;

    public SoulLinkState(Player source, LivingEntity target, JavaPlugin plugin, ParticleSystem particleSystem) {
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null");
        }
        if (target == null) {
            throw new IllegalArgumentException("Target cannot be null");
        }
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        if (particleSystem == null) {
            throw new IllegalArgumentException("ParticleSystem cannot be null");
        }

        this.source = source;
        this.target = target;
        this.particleSystem = particleSystem;

        this.tetherHandle = particleSystem.playPersistent(
                new SoulTetherAnimation(target),
                source,
                DURATION_TICKS / 20.0
        );

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void tick() {
        if (finished) {
            return;
        }

        boolean sourceInvalid = !source.isValid() || source.isDead();
        boolean targetInvalid = !target.isValid() || target.isDead();

        if (sourceInvalid || targetInvalid) {
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
        if (finished || transferring) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }

        LivingEntity receiver;

        if (victim.equals(source)) {
            receiver = target;
        } else if (victim.equals(target)) {
            receiver = source;
        } else {
            return;
        }

        if (!receiver.isValid() || receiver.isDead()) {
            return;
        }

        double transfer = event.getDamage() * TRANSFER_RATIO;

        if (transfer <= 0.0) {
            return;
        }

        transferring = true;

        try {
            receiver.damage(transfer);
        } finally {
            transferring = false;
        }

        // Particles travel along the tether from victim to receiver.
        Location from = victim.getLocation().add(0, 1.0, 0);
        Vector travel = receiver.getLocation().add(0, 1.0, 0).toVector()
                .subtract(from.toVector());

        particleSystem.play(animationContext -> {
            Location origin = animationContext.getOrigin();

            if (origin.getWorld() == null || travel.lengthSquared() < 1.0E-6) {
                return;
            }

            for (double d = 0.0; d <= travel.length(); d += 0.35) {
                origin.getWorld().spawnParticle(
                        Particle.DUST,
                        origin.clone().add(travel.clone().multiply(d / travel.length())),
                        1,
                        0, 0, 0,
                        0,
                        DUST_SOUL
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

        if (tetherHandle != null && tetherHandle.isActive()) {
            tetherHandle.stop();
        }
    }
}