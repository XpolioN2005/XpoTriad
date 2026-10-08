package dev.xpolion.xpotriad.runtime.states;

import dev.xpolion.xpotriad.particle.ParticleSystem;
import dev.xpolion.xpotriad.runtime.RuntimeState;
import dev.xpolion.xpotriad.targeting.TargetResolver;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Simulated fast projectile for Line Snipe.
 *
 * Travels 1.5 blocks/tick (30 blocks over its 20-tick lifetime) along the
 * cast direction, stopping at the first valid entity (source excluded,
 * multiplayer safe) or a solid block. The first target takes 8 damage.
 *
 * Lifecycle: start -> advance + thin trail each tick -> impact/expiry ->
 * cleanup. No listeners or particle handles are held; the trail stops
 * automatically once finished.
 */
public final class LineSnipeState implements RuntimeState {

    private static final double MAX_DISTANCE = 30.0;
    private static final int LIFETIME_TICKS = 20;
    private static final double SPEED = MAX_DISTANCE / LIFETIME_TICKS;
    private static final double DAMAGE = 8.0;
    private static final double TRAIL_THRESHOLD = 1.0E-6;

    private final Player source;
    private final World world;
    private final Vector direction;
    private final ParticleSystem particleSystem;

    private Location position;
    private double travelled = 0.0;
    private int ticksElapsed = 0;
    private volatile boolean finished = false;

    public LineSnipeState(
            Player source,
            Location origin,
            Vector direction,
            JavaPlugin plugin,
            ParticleSystem particleSystem
    ) {
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null");
        }
        if (origin == null || origin.getWorld() == null) {
            throw new IllegalArgumentException("Origin cannot be null");
        }
        if (direction == null) {
            throw new IllegalArgumentException("Direction cannot be null");
        }
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        if (particleSystem == null) {
            throw new IllegalArgumentException("ParticleSystem cannot be null");
        }

        this.source = source;
        this.world = origin.getWorld();
        this.direction = direction.clone().normalize();
        this.position = origin.clone();
        this.particleSystem = particleSystem;
    }

    @Override
    public void tick() {
        if (finished) {
            return;
        }

        if (!source.isValid() || source.isDead()) {
            stop();
            return;
        }

        ticksElapsed++;

        if (ticksElapsed > LIFETIME_TICKS) {
            stop();
            return;
        }

        double step = Math.min(SPEED, MAX_DISTANCE - travelled);

        if (step <= TRAIL_THRESHOLD) {
            stop();
            return;
        }

        // Shorten the segment when a solid block is in the way.
        double segmentLength = step;

        RayTraceResult blockHit = world.rayTraceBlocks(
                position,
                direction,
                step,
                FluidCollisionMode.NEVER,
                true
        );

        if (blockHit != null && blockHit.getHitPosition() != null) {
            double blockDistance = blockHit.getHitPosition().distance(position.toVector());
            segmentLength = Math.min(step, Math.max(blockDistance, 0.0));
        }

        // First living entity along the segment — caster excluded.
        List<LivingEntity> hits = TargetResolver.raycast(
                position,
                direction,
                segmentLength,
                source
        );

        Location segmentStart = position.clone();
        Location segmentEnd = position.clone().add(direction.clone().multiply(segmentLength));

        // Thin, fast trail this tick.
        Vector trailDirection = direction.clone();
        double trailLength = segmentLength;

        particleSystem.play(animationContext -> {
            Location origin = animationContext.getOrigin();

            if (origin.getWorld() == null) {
                return;
            }

            for (double d = 0.0; d <= trailLength; d += 0.75) {
                origin.getWorld().spawnParticle(
                        Particle.ELECTRIC_SPARK,
                        origin.clone().add(trailDirection.clone().multiply(d)),
                        1,
                        0.02, 0.02, 0.02,
                        0.0
                );
            }
        }, segmentStart);

        if (!hits.isEmpty()) {
            LivingEntity first = hits.get(0);
            first.damage(DAMAGE);

            // Small sharp impact burst on the hit.
            particleSystem.play(animationContext -> {
                Location origin = animationContext.getOrigin();

                if (origin.getWorld() != null) {
                    origin.getWorld().spawnParticle(
                            Particle.CRIT,
                            origin,
                            16,
                            0.3, 0.3, 0.3,
                            0.3
                    );
                }
            }, first.getLocation().add(0, 1.0, 0));

            position = segmentEnd;
            stop();
            return;
        }

        travelled += segmentLength;
        position = segmentEnd;

        boolean hitWall = segmentLength < step - TRAIL_THRESHOLD;

        if (travelled >= MAX_DISTANCE - TRAIL_THRESHOLD || hitWall) {
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
    }
}