package dev.xpolion.xpotriad;

import dev.xpolion.xpotriad.fragment.ExplosionFragment;
import dev.xpolion.xpotriad.fragment.InvisibilityFragment;
import dev.xpolion.xpotriad.fragment.SpeedFragment;

import java.util.HashMap;
import java.util.Map;

public final class FragmentRegistry {

    private static final Map<String, Fragment> FRAGMENTS = new HashMap<>();

    static {
        register(new InvisibilityFragment());
        register(new SpeedFragment());
        register(new ExplosionFragment());
    }

    private static void register(Fragment fragment) {
        FRAGMENTS.put(fragment.getId(), fragment);
    }

    public static Fragment get(String id) {
        return FRAGMENTS.get(id);
    }

    private FragmentRegistry() {
    }
}