package dev.xpolion.xpotriad.fragment;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.loot.LootTables;

import dev.xpolion.xpotriad.fragment.fragments.ExplosionFragment;
import dev.xpolion.xpotriad.fragment.fragments.InvisibilityFragment;
import dev.xpolion.xpotriad.fragment.fragments.MarkFragment;
import dev.xpolion.xpotriad.fragment.fragments.SpeedFragment;

public final class FragmentRegistry {

    private static final Map<String, Fragment> FRAGMENTS = new HashMap<>();

    private static final Map<LootTables, Map<Fragment.Rarity, Integer>> RARITY_LOOT =
            new HashMap<>();

    private static final Map<LootTables, Map<String, Integer>> FRAGMENT_LOOT =
            new HashMap<>();

    static {
        register(new InvisibilityFragment());

        register(new SpeedFragment())
                .loot(LootTables.ANCIENT_CITY, 10);

        register(new ExplosionFragment())
                .loot(LootTables.ANCIENT_CITY, 5);

        register(new MarkFragment())
                .loot(LootTables.ANCIENT_CITY, 2)
                .loot(LootTables.ANCIENT_CITY_ICE_BOX, 5);

        rarity(LootTables.ANCIENT_CITY, Fragment.Rarity.COMMON, 60);
        rarity(LootTables.ANCIENT_CITY, Fragment.Rarity.UNCOMMON, 25);
        rarity(LootTables.ANCIENT_CITY, Fragment.Rarity.RARE, 10);
        rarity(LootTables.ANCIENT_CITY, Fragment.Rarity.EPIC, 4);
        rarity(LootTables.ANCIENT_CITY, Fragment.Rarity.LEGENDARY, 1);
    }

    private FragmentRegistry() {
    }

    private static FragmentEntry register(Fragment fragment) {
        FRAGMENTS.put(fragment.getId(), fragment);
        return new FragmentEntry(fragment);
    }

    private static void rarity(
            LootTables lootTable,
            Fragment.Rarity rarity,
            int weight
    ) {
        if (weight <= 0) {
            throw new IllegalArgumentException("Rarity weight must be positive");
        }

        RARITY_LOOT
                .computeIfAbsent(lootTable, key -> new HashMap<>())
                .put(rarity, weight);
    }

    public static Fragment get(String id) {
        return FRAGMENTS.get(id);
    }

    public static Fragment rollLoot(LootTables lootTable) {
        Fragment.Rarity rarity = rollRarity(lootTable);

        if (rarity == null) {
            return null;
        }

        return rollFragment(lootTable, rarity);
    }

    private static Fragment.Rarity rollRarity(LootTables lootTable) {
        Map<Fragment.Rarity, Integer> entries = RARITY_LOOT.get(lootTable);

        if (entries == null || entries.isEmpty()) {
            return null;
        }

        int totalWeight = 0;

        for (int weight : entries.values()) {
            totalWeight += weight;
        }

        int roll = ThreadLocalRandom.current().nextInt(totalWeight);

        for (Map.Entry<Fragment.Rarity, Integer> entry : entries.entrySet()) {
            roll -= entry.getValue();

            if (roll < 0) {
                return entry.getKey();
            }
        }

        return null;
    }

    private static Fragment rollFragment(
            LootTables lootTable,
            Fragment.Rarity rarity
    ) {
        Map<String, Integer> entries = FRAGMENT_LOOT.get(lootTable);

        if (entries == null || entries.isEmpty()) {
            return null;
        }

        int totalWeight = 0;

        for (Map.Entry<String, Integer> entry : entries.entrySet()) {
            Fragment fragment = FRAGMENTS.get(entry.getKey());

            if (fragment != null && fragment.getRarity() == rarity) {
                totalWeight += entry.getValue();
            }
        }

        if (totalWeight <= 0) {
            return null;
        }

        int roll = ThreadLocalRandom.current().nextInt(totalWeight);

        for (Map.Entry<String, Integer> entry : entries.entrySet()) {
            Fragment fragment = FRAGMENTS.get(entry.getKey());

            if (fragment == null || fragment.getRarity() != rarity) {
                continue;
            }

            roll -= entry.getValue();

            if (roll < 0) {
                return fragment;
            }
        }

        return null;
    }

    private static final class FragmentEntry {

        private final Fragment fragment;

        private FragmentEntry(Fragment fragment) {
            this.fragment = fragment;
        }

        private FragmentEntry loot(
                LootTables lootTable,
                int weight
        ) {
            if (weight <= 0) {
                throw new IllegalArgumentException(
                        "Fragment loot weight must be positive"
                );
            }

            FRAGMENT_LOOT
                    .computeIfAbsent(lootTable, key -> new HashMap<>())
                    .put(fragment.getId(), weight);

            return this;
        }
    }
}