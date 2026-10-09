package dev.xpolion.xpotriad.ability;

import dev.xpolion.xpotriad.config.BalanceConfig;
import dev.xpolion.xpotriad.fragment.Fragment;

import java.util.EnumMap;
import java.util.Map;

public final class Ability {

    /** Fallbacks used only if the balance config is not yet loaded. */
    public static final long BASE_COOLDOWN_TICKS = 20L;
    public static final long MIN_COOLDOWN_TICKS = 0L;
    public static final long MAX_COOLDOWN_TICKS = 300L;

    public enum Stage {
        PRE_CAST,
        CAST,
        POST_CAST
    }

    private final Map<Stage, Fragment> fragments = new EnumMap<>(Stage.class);

    public Ability() {
    }

    public void setFragment(Stage stage, Fragment fragment) {
        fragments.put(stage, fragment);
    }

    public Fragment getFragment(Stage stage) {
        return fragments.get(stage);
    }

    /**
     * Calculates the cooldown for this ability based on base cooldown and Fragment modifiers.
     * Base/min/max come from the balance config (falling back to the compiled
     * defaults if it is not yet loaded).
     */
    public long calculateCooldown() {
        BalanceConfig cfg = BalanceConfig.get();

        long base = cfg.baseCooldownTicks();
        long min = cfg.minCooldownTicks();
        long max = cfg.maxCooldownTicks();

        long cooldown = base;
        for (Stage stage : Stage.values()) {
            Fragment fragment = fragments.get(stage);
            if (fragment != null) {
                cooldown += fragment.getCooldownModifier();
            }
        }
        return Math.clamp(cooldown, min, max);
    }
}
