package dev.xpolion.xpotriad.particle;

import org.bukkit.Location;

public final class ParticleContext {

    private final Location origin;
    private final double elapsedSeconds;
    private final double deltaSeconds;

    public ParticleContext(Location origin, double elapsedSeconds, double deltaSeconds) {
        if (origin == null) {
            throw new IllegalArgumentException("Location origin cannot be null");
        }
        this.origin = origin.clone();
        this.elapsedSeconds = elapsedSeconds;
        this.deltaSeconds = deltaSeconds;
    }

    public Location getOrigin() {
        return origin.clone();
    }

    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    public double getDeltaSeconds() {
        return deltaSeconds;
    }
}
