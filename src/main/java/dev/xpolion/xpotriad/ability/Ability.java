package dev.xpolion.xpotriad.ability;

import dev.xpolion.xpotriad.fragment.Fragment;

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
}
