package dev.xpolion.xpotriad.runtime.states;

import dev.xpolion.xpotriad.particle.ParticleHandle;
import dev.xpolion.xpotriad.particle.ParticleSystem;
import dev.xpolion.xpotriad.particle.animations.MeteorTrailAnimation;
import dev.xpolion.xpotriad.runtime.RuntimeState;
import dev.xpolion.xpotriad.targeting.TargetResolver;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Schedules a meteor barrage over a static zone centered where the
 * caster stood at cast time.
 *
 * Every stagger-ticks one fireball is spawned at a random point inside
 * zone-radius, spawn-height above the cast plane, falling straight down
 * at fall-speed. When a fireball dies (block/entity hit or expiry) the
 * meteor deals aoe-radius damage (caster excluded) with an expanding
 * burst — no terrain destruction.
 *
 * Lifecycle: start -> stagger-spawn meteors -> all landed (or the
 * lifetime-ticks failsafe) -> cleanup (stops trail handles).
 * No listeners held.
 */
public final class MeteorState implements RuntimeState {

    private static final Particle.DustOptions DUST_EMBER =
            new Particle.DustOptions(Color.fromRGB(255, 130, 30), 1.4f);

    private final Player source;
    private final Location zoneCenter;

    private final double zoneRadius;
    private final int count;
    private final int staggerTicks;
    private final int spawnHeight;
    private final double fallSpeed;
    private final int lifetimeTicks;
    private final double aoeRadius;
    private final double impactDamage;

    private final ParticleSystem particleSystem;
    private final List<Meteor> inFlight = new ArrayList<>();

    private int spawned = 0;
    private int staggerCooldown = 0;
    private int ticksRemaining;
    private volatile boolean finished = false;

    public MeteorState(
            Player source,
            Location zoneCenter,
            JavaPlugin plugin,
            ParticleSystem particleSystem,
            double zoneRadius,
            int count,
            int staggerTicks,
            int spawnHeight,
            double fallSpeed,
            int lifetimeTicks,
            double aoeRadius,
            double impactDamage
    ) {
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null");
        }
        if (zoneCenter == null || zoneCenter.getWorld() == null) {
            throw new IllegalArgumentException("Zone center cannot be null");
        }
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        if (particleSystem == null) {
            throw new IllegalArgumentException("ParticleSystem cannot be null");
        }

        this.source = source;
        this.zoneCenter = zoneCenter.clone();
        this.particleSystem = particleSystem;
        this.zoneRadius = zoneRadius;
        this.count = Math.max(1, count);
        this.staggerTicks = Math.max(1, staggerTicks);
        this.spawnHeight = spawnHeight;
        this.fallSpeed = Math.max(0.1, Math.abs(fallSpeed));
        this.lifetimeTicks = lifetimeTicks;
        this.aoeRadius = aoeRadius;
        this.impactDamage = impactDamage;
        this.ticksRemaining = lifetimeTicks;
    }

    @Override
    public void tick() {
        if (finished) {
            return;
        }

        // Staggered spawning.
        if (spawned < count) {
            if (staggerCooldown <= 0) {
                spawnMeteor();
                spawned++;
                staggerCooldown = staggerTicks;
            } else {
                staggerCooldown--;
            }
        }

        // Impact / expiry tracking.
        Iterator<Meteor> iterator = inFlight.iterator();

        while (iterator.hasNext()) {
            Meteor meteor = iterator.next();

            if (!meteor.fireball.isValid() || meteor.fireball.isDead()) {
                impact(meteor.lastLocation);
                stopTrail(meteor);
                iterator.remove();
                continue;
            }

            meteor.lastLocation = meteor.fireball.getLocation().clone();
        }

        ticksRemaining--;

        if (ticksRemaining <= 0) {
            // Failsafe: force-land whatever is still falling.
            for (Meteor meteor : inFlight) {
                meteor.fireball.remove();
                impact(meteor.lastLocation);
                stopTrail(meteor);
            }

            inFlight.clear();
            stop();
            return;
        }

        // Barrage complete once everything has landed.
        if (spawned >= count && inFlight.isEmpty()) {
            stop();
        }
    }

    private void spawnMeteor() {
        World world = zoneCenter.getWorld();

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble(Math.PI * 2.0);
        double radial = zoneRadius * Math.sqrt(random.nextDouble());

        double x = zoneCenter.getX() + Math.cos(angle) * radial;
        double z = zoneCenter.getZ() + Math.sin(angle) * radial;

        Location spawnLoc = new Location(
                world, x, zoneCenter.getY() + spawnHeight, z);

        Vector fallVelocity = new Vector(0.0, -fallSpeed, 0.0);

        LargeFireball fireball = world.spawn(spawnLoc, LargeFireball.class, meteor -> {
            meteor.setShooter(source);
            meteor.setDirection(fallVelocity);
            meteor.setVelocity(fallVelocity.clone());
            meteor.setYield(0.0f);          // no terrain destruction
            meteor.setIsIncendiary(false);  // no fire spread
        });

        ParticleHandle trail = particleSystem.playPersistent(
                new MeteorTrailAnimation(),
                fireball,
                ticksRemaining / 20.0
        );

        inFlight.add(new Meteor(fireball, trail, spawnLoc.clone()));
    }

    private void impact(Location at) {
        if (at == null || at.getWorld() == null) {
            return;
        }

        // AOE damage — caster excluded.
        List<LivingEntity> affected = TargetResolver.entitiesNear(
                at,
                aoeRadius,
                source
        );

        for (LivingEntity entity : affected) {
            entity.damage(impactDamage);
        }

        // Expanding fire/smoke/debris burst.
        Location burstAt = at.clone();

        particleSystem.play(animationContext -> {
            Location origin = animationContext.getOrigin();

            if (origin.getWorld() == null) {
                return;
            }

            origin.getWorld().spawnParticle(
                    Particle.FLAME,
                    origin,
                    50,
                    1.2, 1.2, 1.2,
                    0.25
            );

            origin.getWorld().spawnParticle(
                    Particle.LARGE_SMOKE,
                    origin,
                    30,
                    1.4, 1.2, 1.4,
                    0.05
            );

            origin.getWorld().spawnParticle(
                    Particle.DUST,
                    origin,
                    35,
                    1.6, 1.0, 1.6,
                    0.0,
                    DUST_EMBER
            );
        }, burstAt);
    }

    private void stopTrail(Meteor meteor) {
        if (meteor.trailHandle != null && meteor.trailHandle.isActive()) {
            meteor.trailHandle.stop();
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

        for (Meteor meteor : inFlight) {
            stopTrail(meteor);
        }

        inFlight.clear();
    }

    /** One in-flight meteor with its trail and last known position. */
    private static final class Meteor {

        private final LargeFireball fireball;
        private final ParticleHandle trailHandle;
        private Location lastLocation;

        private Meteor(LargeFireball fireball, ParticleHandle trailHandle,
                       Location lastLocation) {
            this.fireball = fireball;
            this.trailHandle = trailHandle;
            this.lastLocation = lastLocation;
        }
    }
}