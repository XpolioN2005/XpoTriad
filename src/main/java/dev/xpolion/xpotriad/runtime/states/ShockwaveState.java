package dev.xpolion.xpotriad.runtime.states;

import dev.xpolion.xpotriad.particle.ParticleHandle;
import dev.xpolion.xpotriad.particle.ParticleSystem;
import dev.xpolion.xpotriad.particle.animations.SlamTrailAnimation;
import dev.xpolion.xpotriad.particle.animations.ShockwaveRingAnimation;
import dev.xpolion.xpotriad.runtime.RuntimeState;
import dev.xpolion.xpotriad.targeting.TargetResolver;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Slam-to-ground shockwave. On construction (main thread, from the effect)
 * the state scans straight down for the first solid block, teleports the
 * caster to its top, and starts a descending slam trail. After {@code slamTicks}
 * it erupts: knockback + damage to every entity within the fall-scaled radius
 * and a growing shockwave ring.
 *
 * Gate: if the drop from the captured start height to the ground is below
 * {@code minFall}, or no ground is found within {@code scanDistance}, the
 * state finishes immediately (no teleport, no effect) — this blocks accidental
 * self-slams while already standing on the ground.
 *
 * Lifecycle: slam (constructor) -> trail plays slamTicks -> impact -> ring
 * plays ringTicks -> finish. A failsafe tick cap guards a vanished caster.
 */
public final class ShockwaveState implements RuntimeState {

    private final Player source;
    private final double startY;
    private final JavaPlugin plugin;
    private final ParticleSystem particleSystem;

    private final double radiusPerBlock;
    private final double maxRadius;
    private final double minFall;
    private final double scanDistance;
    private final double damage;
    private final double knockbackStrength;
    private final int slamTicks;
    private final int ringTicks;
    private final int failsafeTicks;

    private ParticleHandle trailHandle;
    private Location landing;
    private double radius;
    private boolean impacted = false;
    private int elapsedTicks = 0;
    private volatile boolean finished = false;

    public ShockwaveState(
            Player source,
            double startY,
            JavaPlugin plugin,
            ParticleSystem particleSystem,
            double radiusPerBlock,
            double maxRadius,
            double minFall,
            double scanDistance,
            double damage,
            double knockbackStrength,
            int slamTicks,
            int ringTicks,
            int failsafeTicks
    ) {
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null");
        }
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        if (particleSystem == null) {
            throw new IllegalArgumentException("ParticleSystem cannot be null");
        }

        this.source = source;
        this.startY = startY;
        this.plugin = plugin;
        this.particleSystem = particleSystem;
        this.radiusPerBlock = radiusPerBlock;
        this.maxRadius = maxRadius;
        this.minFall = minFall;
        this.scanDistance = Math.max(1.0, scanDistance);
        this.damage = damage;
        this.knockbackStrength = knockbackStrength;
        this.slamTicks = Math.max(1, slamTicks);
        this.ringTicks = Math.max(1, ringTicks);
        this.failsafeTicks = Math.max(this.slamTicks + this.ringTicks, failsafeTicks);

        slam();
    }

    /** Ground scan + teleport + trail. May set {@link #finished} and bail. */
    private void slam() {
        if (!source.isOnline()) {
            finished = true;
            return;
        }

        World world = source.getWorld();
        Location from = source.getLocation().clone();
        Vector down = new Vector(0.0, -1.0, 0.0);

        RayTraceResult hit = world.rayTraceBlocks(
                from,
                down,
                scanDistance,
                FluidCollisionMode.NEVER,
                true
        );

        if (hit == null || hit.getHitBlock() == null) {
            finished = true; // void / gap deeper than the scan: no slam
            return;
        }

        double groundY = hit.getHitBlock().getY() + 1.0;
        double fall = startY - groundY;

        if (fall < minFall) {
            finished = true; // already near the ground: block self-slam
            return;
        }

        radius = Math.min(maxRadius, fall * radiusPerBlock);

        Location landingLoc = new Location(
                world, from.getX(), groundY, from.getZ(), from.getYaw(), from.getPitch()
        );
        this.landing = landingLoc.clone();

        source.teleport(landingLoc);
        source.setFallDistance(0.0f);

        trailHandle = particleSystem.playPersistent(
                new SlamTrailAnimation(startY, groundY, slamTicks),
                landingLoc.clone(),
                slamTicks / 20.0
        );
    }

    @Override
    public void tick() {
        if (finished) {
            return;
        }

        if (!source.isOnline()) {
            stop();
            return;
        }

        elapsedTicks++;

        if (!impacted && elapsedTicks >= slamTicks) {
            impact();
        }

        if (elapsedTicks >= slamTicks + ringTicks || elapsedTicks >= failsafeTicks) {
            stop();
        }
    }

    private void impact() {
        impacted = true;

        if (landing == null || landing.getWorld() == null) {
            return;
        }

        List<LivingEntity> targets =
                TargetResolver.entitiesNear(landing, radius, source);

        for (LivingEntity entity : targets) {
            Vector away = entity.getLocation().toVector().subtract(landing.toVector());
            if (away.lengthSquared() < 1.0E-6) {
                away = new Vector(1.0, 0.0, 0.0);
            }
            away.setY(Math.abs(away.getY()) + 0.35);
            away.normalize().multiply(knockbackStrength);
            entity.setVelocity(entity.getVelocity().clone().add(away));

            entity.damage(damage);
        }

        particleSystem.playPersistent(
                new ShockwaveRingAnimation(radius, ringTicks),
                landing.clone(),
                ringTicks / 20.0
        );
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
