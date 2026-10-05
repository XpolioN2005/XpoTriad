package dev.xpolion.xpotriad.particle;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ParticleSystem {

    private static final double TICK_DELTA_SECONDS = 0.05;

    private final JavaPlugin plugin;
    private final List<PersistentParticle> activeParticles = new CopyOnWriteArrayList<>();
    private BukkitTask task;

    public ParticleSystem(JavaPlugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        this.plugin = plugin;
    }

    public void start() {
        if (task != null) {
            return;
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void play(ParticleAnimation animation, Location origin) {
        if (animation == null || origin == null) {
            return;
        }
        try {
            ParticleContext context = new ParticleContext(origin, 0.0, 0.0);
            animation.render(context);
        } catch (Exception e) {
            plugin.getLogger().severe("Error executing one-shot particle animation: " + e.getMessage());
        }
    }

    public void play(ParticleAnimation animation, Entity entity) {
        if (animation == null || entity == null || !entity.isValid()) {
            return;
        }
        play(animation, entity.getLocation());
    }

    public ParticleHandle playPersistent(ParticleAnimation animation, Location location, double durationSeconds) {
        return playPersistent(animation, ParticleAttachment.WORLD, location, null, durationSeconds);
    }

    public ParticleHandle playPersistent(ParticleAnimation animation, Entity entity, double durationSeconds) {
        return playPersistent(animation, ParticleAttachment.ENTITY, null, entity, durationSeconds);
    }

    public ParticleHandle playPersistent(
            ParticleAnimation animation,
            ParticleAttachment attachment,
            Location location,
            Entity entity,
            double durationSeconds
    ) {
        if (animation == null || attachment == null) {
            throw new IllegalArgumentException("Animation and attachment cannot be null");
        }
        if (attachment == ParticleAttachment.WORLD && location == null) {
            throw new IllegalArgumentException("Location required for WORLD attachment");
        }
        if (attachment == ParticleAttachment.ENTITY && (entity == null || !entity.isValid())) {
            throw new IllegalArgumentException("Valid Entity required for ENTITY attachment");
        }

        PersistentParticle particle = new PersistentParticle(
                animation,
                attachment,
                location,
                entity,
                durationSeconds
        );

        activeParticles.add(particle);
        return particle;
    }

    private void tick() {
        for (PersistentParticle particle : activeParticles) {
            if (!particle.active) {
                activeParticles.remove(particle);
                continue;
            }

            particle.elapsedSeconds += TICK_DELTA_SECONDS;

            Location origin = particle.resolveLocation();
            if (origin == null) {
                particle.stop();
                continue;
            }

            try {
                ParticleContext context = new ParticleContext(origin, particle.elapsedSeconds, TICK_DELTA_SECONDS);
                particle.animation.render(context);
            } catch (Exception e) {
                plugin.getLogger().severe("Error rendering persistent particle animation: " + e.getMessage());
                particle.stop();
                continue;
            }

            if (particle.elapsedSeconds >= particle.durationSeconds) {
                particle.stop();
            }
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (PersistentParticle particle : activeParticles) {
            particle.stopInternal();
        }
        activeParticles.clear();
    }

    private class PersistentParticle implements ParticleHandle {
        private final ParticleAnimation animation;
        private final ParticleAttachment attachment;
        private final Location worldLocation;
        private final Entity entity;
        private final double durationSeconds;
        private double elapsedSeconds = 0.0;
        private volatile boolean active = true;

        private PersistentParticle(
                ParticleAnimation animation,
                ParticleAttachment attachment,
                Location location,
                Entity entity,
                double durationSeconds
        ) {
            this.animation = animation;
            this.attachment = attachment;
            this.worldLocation = location != null ? location.clone() : null;
            this.entity = entity;
            this.durationSeconds = durationSeconds;
        }

        @Override
        public void stop() {
            if (!active) {
                return;
            }
            active = false;
            activeParticles.remove(this);
        }

        private void stopInternal() {
            active = false;
        }

        @Override
        public boolean isActive() {
            if (!active) {
                return false;
            }
            if (elapsedSeconds >= durationSeconds) {
                return false;
            }
            if (attachment == ParticleAttachment.ENTITY) {
                return entity != null && entity.isValid() && !entity.isDead();
            }
            return true;
        }

        private Location resolveLocation() {
            if (attachment == ParticleAttachment.ENTITY) {
                if (entity == null || !entity.isValid() || entity.isDead()) {
                    return null;
                }
                return entity.getLocation();
            } else {
                return worldLocation != null ? worldLocation.clone() : null;
            }
        }
    }
}
