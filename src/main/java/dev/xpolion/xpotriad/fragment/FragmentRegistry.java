package dev.xpolion.xpotriad.fragment;

import java.util.HashMap;
import java.util.Map;

import dev.xpolion.xpotriad.fragment.fragments.ExplosionFragment;
import dev.xpolion.xpotriad.fragment.fragments.InvisibilityFragment;
import dev.xpolion.xpotriad.fragment.fragments.MarkFragment;
import dev.xpolion.xpotriad.fragment.fragments.SpeedFragment;

/**
 * Static registry of all registered Fragment definitions.
 * Fragment IDs map to concrete Fragment instances.
 */
public final class FragmentRegistry {

    private static final Map<String, Fragment> FRAGMENTS = new HashMap<>();

    static {
        register(new InvisibilityFragment());
        register(new SpeedFragment());
        register(new ExplosionFragment());
        register(new MarkFragment());
    }

    private FragmentRegistry() {
    }

    private static void register(Fragment fragment) {
        FRAGMENTS.put(fragment.getId(), fragment);
    }

    public static Fragment get(String id) {
        return FRAGMENTS.get(id);
    }
}
