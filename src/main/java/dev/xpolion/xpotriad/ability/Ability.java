package dev.xpolion.xpotriad.ability;

import dev.xpolion.xpotriad.fragment.Fragment;

import java.util.EnumMap;
import java.util.Map;

public final class Ability {

    public static final long BASE_COOLDOWN_TICKS = 20L;
    public static final long MIN_COOLDOWN_TICKS = 0L;
    public static final long MAX_COOLDOWN_TICKS = 300L;

    public enum Stage {
        PRE_CAST,
        CAST,
        POST_CAST
    }

    public enum WeaponType {
        MELEE,
        RANGED
    }

    private final WeaponType weaponType;
    private final Map<Stage, Fragment> fragments = new EnumMap<>(Stage.class);

    public Ability(WeaponType weaponType) {
        this.weaponType = weaponType;
    }

    public void setFragment(Stage stage, Fragment fragment) {
        fragments.put(stage, fragment);
    }

    public Fragment getFragment(Stage stage) {
        return fragments.get(stage);
    }

    public WeaponType getWeaponType() {
        return weaponType;
    }

    /**
     * Calculates the cooldown for this ability based on base cooldown and Fragment modifiers.
     * Clamped between MIN_COOLDOWN_TICKS (0) and MAX_COOLDOWN_TICKS (300).
     */
    public long calculateCooldown() {
        long cooldown = BASE_COOLDOWN_TICKS;
        for (Stage stage : Stage.values()) {
            Fragment fragment = fragments.get(stage);
            if (fragment != null) {
                cooldown += fragment.getCooldownModifier();
            }
        }
        return Math.clamp(cooldown, MIN_COOLDOWN_TICKS, MAX_COOLDOWN_TICKS);
    }
}
