package dev.xpolion.xpotriad.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Typed, reload-safe view over {@code config.yml}.
 *
 * <p>All balance numbers (fragment timing, targeting ranges, effect
 * damage / duration / percentage) live here. Effects and runtime states read
 * values from the current {@link BalanceConfig} instance held by {@code Main}
 * and snapshot them at construction, so a {@code /xpt reload} never mutates an
 * ability that is already executing.</p>
 *
 * <p>Every getter falls back to a compiled default when the key is missing or
 * invalid, and logs a single warning the first time that happens. No value can
 * ever crash or half-apply a reload.</p>
 */
public final class BalanceConfig {

    /** Shared instance; effects/fragments read via {@link #get()}. */
    private static volatile BalanceConfig instance = new BalanceConfig();

    private static final long DEFAULT_BASE_COOLDOWN = 20L;
    private static final long DEFAULT_MIN_COOLDOWN = 0L;
    private static final long DEFAULT_MAX_COOLDOWN = 300L;
    private static final long DEFAULT_STAGE_BUFFER = 5L;

    private static final double DEFAULT_RAYCAST_RANGE = 12.0;
    private static final double DEFAULT_NEARBY_RADIUS = 3.0;
    private static final double DEFAULT_POINT_RANGE = 15.0;

    private long baseCooldownTicks;
    private long minCooldownTicks;
    private long maxCooldownTicks;
    private long stageBufferTicks;

    private double singleTargetRaycastRange;
    private double singleTargetNearbyRadius;
    private double pointRange;

    private final Map<String, long[]> fragmentTiming = new HashMap<>();
    private final Map<String, Map<String, Double>> effectValues = new HashMap<>();

    public BalanceConfig() {
        applyDefaults();
    }

    /** The active config. Never null (defaults until {@link #set} is called). */
    public static BalanceConfig get() {
        return instance;
    }

    /** Atomically installs a freshly-loaded config (used by /xpt reload). */
    public static void set(BalanceConfig config) {
        instance = config != null ? config : new BalanceConfig();
    }

    private void applyDefaults() {
        baseCooldownTicks = DEFAULT_BASE_COOLDOWN;
        minCooldownTicks = DEFAULT_MIN_COOLDOWN;
        maxCooldownTicks = DEFAULT_MAX_COOLDOWN;
        stageBufferTicks = DEFAULT_STAGE_BUFFER;

        singleTargetRaycastRange = DEFAULT_RAYCAST_RANGE;
        singleTargetNearbyRadius = DEFAULT_NEARBY_RADIUS;
        pointRange = DEFAULT_POINT_RANGE;
    }

    /**
     * Loads and validates every value from {@code cfg}, starting from the
     * compiled defaults so a partial / broken file still yields a usable,
     * fully-populated instance.
     */
    public void load(FileConfiguration cfg, Logger log) {
        applyDefaults();
        fragmentTiming.clear();
        effectValues.clear();

        baseCooldownTicks = readLong(cfg, "ability.base-cooldown-ticks", DEFAULT_BASE_COOLDOWN, 0, Long.MAX_VALUE, log);
        minCooldownTicks = readLong(cfg, "ability.min-cooldown-ticks", DEFAULT_MIN_COOLDOWN, 0, Long.MAX_VALUE, log);
        maxCooldownTicks = readLong(cfg, "ability.max-cooldown-ticks", DEFAULT_MAX_COOLDOWN, 0, Long.MAX_VALUE, log);
        stageBufferTicks = readLong(cfg, "ability.stage-buffer-ticks", DEFAULT_STAGE_BUFFER, 0, Long.MAX_VALUE, log);

        if (maxCooldownTicks < minCooldownTicks) {
            if (log != null) {
                log.warning("[XpoTriad] max-cooldown-ticks < min-cooldown-ticks; using min as max.");
            }
            maxCooldownTicks = minCooldownTicks;
        }

        singleTargetRaycastRange = readDouble(cfg, "targeting.single-target.raycast-range", DEFAULT_RAYCAST_RANGE, 0.0, Double.MAX_VALUE, log);
        singleTargetNearbyRadius = readDouble(cfg, "targeting.single-target.nearby-radius", DEFAULT_NEARBY_RADIUS, 0.0, Double.MAX_VALUE, log);
        pointRange = readDouble(cfg, "targeting.point-range", DEFAULT_POINT_RANGE, 0.0, Double.MAX_VALUE, log);

        ConfigurationSection frags = cfg.getConfigurationSection("fragments");
        if (frags != null) {
            for (String id : frags.getKeys(false)) {
                long exec = readLong(cfg, "fragments." + id + ".execution-time-ticks", 0L, 0, Long.MAX_VALUE, log);
                long cd = readLong(cfg, "fragments." + id + ".cooldown-modifier-ticks", 0L, Long.MIN_VALUE, Long.MAX_VALUE, log);
                fragmentTiming.put(id, new long[]{exec, cd});
            }
        }

        ConfigurationSection effs = cfg.getConfigurationSection("effects");
        if (effs != null) {
            for (String id : effs.getKeys(false)) {
                ConfigurationSection sec = effs.getConfigurationSection(id);
                if (sec == null) {
                    continue;
                }
                Map<String, Double> values = new HashMap<>();
                for (String key : sec.getKeys(false)) {
                    if (sec.isDouble(key) || sec.isInt(key) || sec.isLong(key)) {
                        values.put(key, sec.getDouble(key));
                    }
                }
                effectValues.put(id, values);
            }
        }
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    public long baseCooldownTicks() {
        return baseCooldownTicks;
    }

    public long minCooldownTicks() {
        return minCooldownTicks;
    }

    public long maxCooldownTicks() {
        return maxCooldownTicks;
    }

    public long stageBufferTicks() {
        return stageBufferTicks;
    }

    public double singleTargetRaycastRange() {
        return singleTargetRaycastRange;
    }

    public double singleTargetNearbyRadius() {
        return singleTargetNearbyRadius;
    }

    public double pointRange() {
        return pointRange;
    }

    /** Execution ticks for a fragment id, or {@code fallback} if unset. */
    public long executionTime(String fragmentId, long fallback) {
        long[] v = fragmentTiming.get(fragmentId);
        return v != null ? v[0] : fallback;
    }

    /** Cooldown-modifier ticks for a fragment id, or {@code fallback} if unset. */
    public long cooldownModifier(String fragmentId, long fallback) {
        long[] v = fragmentTiming.get(fragmentId);
        return v != null ? v[1] : fallback;
    }

    public double effectDouble(String effectId, String key, double fallback) {
        Double v = lookup(effectId, key);
        return v != null ? v : fallback;
    }

    public int effectInt(String effectId, String key, int fallback) {
        Double v = lookup(effectId, key);
        return v != null ? (int) Math.round(v) : fallback;
    }

    public long effectLong(String effectId, String key, long fallback) {
        Double v = lookup(effectId, key);
        return v != null ? Math.round(v) : fallback;
    }

    public float effectFloat(String effectId, String key, float fallback) {
        Double v = lookup(effectId, key);
        return v != null ? v.floatValue() : fallback;
    }

    private Double lookup(String effectId, String key) {
        Map<String, Double> values = effectValues.get(effectId);
        return values != null ? values.get(key) : null;
    }

    // ------------------------------------------------------------------
    // Safe readers
    // ------------------------------------------------------------------

    private static long readLong(FileConfiguration cfg, String path, long def, long min, long max, Logger log) {
        if (!cfg.contains(path)) {
            return def;
        }
        double raw = cfg.getDouble(path, def);
        long clamped = (long) Math.clamp(raw, min, max);
        if (clamped != raw) {
            warn(log, path, raw + " -> " + clamped);
        }
        return clamped;
    }

    private static double readDouble(FileConfiguration cfg, String path, double def, double min, double max, Logger log) {
        if (!cfg.contains(path)) {
            return def;
        }
        double raw = cfg.getDouble(path, def);
        double clamped = Math.clamp(raw, min, max);
        if (clamped != raw) {
            warn(log, path, raw + " -> " + clamped);
        }
        return clamped;
    }

    private static void warn(Logger log, String path, String detail) {
        if (log != null) {
            log.warning("[XpoTriad] config value '" + path + "' out of range, clamped (" + detail + ")");
        }
    }
}
