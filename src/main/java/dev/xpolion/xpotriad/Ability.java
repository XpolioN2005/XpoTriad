package dev.xpolion.xpotriad;

import java.util.EnumMap;
import java.util.Map;

public final class Ability {

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
    private final Map<Stage, Long> delays = new EnumMap<>(Stage.class);

    public Ability(WeaponType weaponType) {
        this.weaponType = weaponType;

        for (Stage stage : Stage.values()) {
            delays.put(stage, 0L);
        }
    }

    public void setFragment(Stage stage, Fragment fragment) {
        fragments.put(stage, fragment);
    }

    public Fragment getFragment(Stage stage) {
        return fragments.get(stage);
    }

    public void setDelay(Stage stage, long ticks) {
        if (ticks < 0) {
            throw new IllegalArgumentException("Delay cannot be negative");
        }

        delays.put(stage, ticks);
    }

    public long getDelay(Stage stage) {
        return delays.getOrDefault(stage, 0L);
    }

    public WeaponType getWeaponType() {
        return weaponType;
    }
}