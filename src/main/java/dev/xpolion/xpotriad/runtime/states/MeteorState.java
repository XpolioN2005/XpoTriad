package dev.xpolion.xpotriad.runtime.states;

import dev.xpolion.xpotriad.particle.ParticleHandle;
import dev.xpolion.xpotriad.particle.ParticleSystem;
import dev.xpolion.xpotriad.particle.animations.MeteorTrailAnimation;
import dev.xpolion.xpotriad.runtime.RuntimeState;
import dev.xpolion.xpotriad.targeting.TargetResolver;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Tracks the meteor fireball until impact or expiry (40 ticks).
 *
 * On impact: entities within 4 blocks (caster excluded — multiplayer
 * safe) take 20 damage, followed by a large expanding burst.
 * Trail particles ride the fireball entity and stop on impact/expiry.
 *
 * Lifecycle: start -> follow projectile -> impact/40 ticks -> cleanup
 * (stops trail handle). No listeners held.
 */
public final class MeteorState implements RuntimeState {

    private static final Particle.DustOptions DUST_EMBER =
            new Particle.DustOptions(Color.fromRGB(255, 130, 30), 1.4f);

    private final double aoeRadius;
    private final double impactDamage;

    private final Player source;
    private final LargeFireball fireball;
    private final ParticleSystem particleSystem;
    private final ParticleHandle trailHandle;

    private Location lastLocation;
    private int ticksRemaining;
    private volatile boolean finished = false;

    public MeteorState(
            Player source,
            LargeFireball fireball,
            JavaPlugin plugin,
            ParticleSystem particleSystem,
            int lifetimeTicks,
            double aoeRadius,
            double impactDamage
    ) {
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null");
        }
        if (fireball == null) {
            throw new IllegalArgumentException("Fireball cannot be null");
        }
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        if (particleSystem == null) {
            throw new IllegalArgumentException("ParticleSystem cannot be null");
        }

        this.source = source;
        this.fireball = fireball;
        this.particleSystem = particleSystem;
        this.lastLocation = fireball.getLocation().clone();
        this.ticksRemaining = lifetimeTicks;
        this.aoeRadius = aoeRadius;
        this.impactDamage = impactDamage;

        this.trailHandle = particleSystem.playPersistent(
                new MeteorTrailAnimation(),
                fireball,
                lifetimeTicks / 20.0
        );
    }

    @Override
    public void tick() {
        if (finished) {
            return;
        }

        if (!fireball.isValid() || fireball.isDead()) {
            impact();
            stop();
            return;
        }

        lastLocation = fireball.getLocation().clone();

        ticksRemaining--;

        if (ticksRemaining <= 0) {
            fireball.remove();
            impact();
            stop();
        }
    }

    private void impact() {
        if (lastLocation == null || lastLocation.getWorld() == null) {
            return;
        }

        // AOE damage — caster excluded.
        List<LivingEntity> affected = TargetResolver.entitiesNear(
                lastLocation,
                aoeRadius,
                source
        );

        for (LivingEntity entity : affected) {
            entity.damage(impactDamage);
        }

        // Large expanding fire/smoke/debris burst.
        Location at = lastLocation.clone();

        particleSystem.play(animationContext -> {
            Location origin = animationContext.getOrigin();

            if (origin.getWorld() == null) {
                return;
            }

            origin.getWorld().spawnParticle(
                    Particle.FLAME,
                    origin,
                    70,
                    1.5, 1.5, 1.5,
                    0.25
            );

            origin.getWorld().spawnParticle(
                    Particle.LARGE_SMOKE,
                    origin,
                    45,
                    1.8, 1.5, 1.8,
                    0.05
            );

            origin.getWorld().spawnParticle(
                    Particle.DUST,
                    origin,
                    50,
                    2.0, 1.2, 2.0,
                    0.0,
                    DUST_EMBER
            );
        }, at);
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

        if (trailHandle != null && trailHandle.isActive()) {
            trailHandle.stop();
        }
    }
}