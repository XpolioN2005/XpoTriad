package dev.xpolion.xpotriad.fragment;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.loot.LootTables;

import dev.xpolion.xpotriad.fragment.fragments.ExplosionFragment;
import dev.xpolion.xpotriad.fragment.fragments.HealFragment;
import dev.xpolion.xpotriad.fragment.fragments.InvisibilityFragment;
import dev.xpolion.xpotriad.fragment.fragments.MarkFragment;
import dev.xpolion.xpotriad.fragment.fragments.SpeedFragment;

public final class FragmentRegistry {

    private static final Map<String, Fragment> FRAGMENTS = new HashMap<>();

    private static final Map<Fragment.Rarity, Integer> DEFAULT_RARITIES = Map.of(
            Fragment.Rarity.COMMON, 60,
            Fragment.Rarity.UNCOMMON, 25,
            Fragment.Rarity.RARE, 10,
            Fragment.Rarity.EPIC, 4,
            Fragment.Rarity.LEGENDARY, 1
    );

    private static final Map<LootTables, Map<Fragment.Rarity, Integer>> LOOT_RARITIES =
            new HashMap<>();

    static {
        register(new InvisibilityFragment());
        register(new SpeedFragment());
        register(new HealFragment());
        register(new ExplosionFragment());
        register(new MarkFragment());

        rarities(LootTables.ANCIENT_CITY, Map.of(
                Fragment.Rarity.COMMON, 45,
                Fragment.Rarity.UNCOMMON, 30,
                Fragment.Rarity.RARE, 15,
                Fragment.Rarity.EPIC, 7,
                Fragment.Rarity.LEGENDARY, 3
        ));
    }

    private FragmentRegistry() {
    }

    public static Fragment get(String id) {
        return FRAGMENTS.get(id);
    }

    public static Fragment rollLoot(LootTables lootTable) {
        Fragment.Rarity rarity = rollRarity(lootTable);

        if (rarity == null) {
            return null;
        }

        return rollFragment(rarity);
    }

    private static Fragment.Rarity rollRarity(LootTables lootTable) {
        Map<Fragment.Rarity, Integer> rarities =
                LOOT_RARITIES.getOrDefault(lootTable, DEFAULT_RARITIES);

        int totalWeight = 0;

        for (int weight : rarities.values()) {
            totalWeight += weight;
        }

        if (totalWeight <= 0) {
            return null;
        }

        int roll = ThreadLocalRandom.current().nextInt(totalWeight);

        for (Map.Entry<Fragment.Rarity, Integer> entry : rarities.entrySet()) {
            roll -= entry.getValue();

            if (roll < 0) {
                return entry.getKey();
            }
        }

        return null;
    }

    private static Fragment rollFragment(Fragment.Rarity rarity) {
        List<Fragment> pool = new ArrayList<>();

        for (Fragment fragment : FRAGMENTS.values()) {
            if (fragment.getRarity() == rarity) {
                pool.add(fragment);
            }
        }

        if (pool.isEmpty()) {
            return null;
        }

        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private static void register(Fragment fragment) {
        if (FRAGMENTS.put(fragment.getId(), fragment) != null) {
            throw new IllegalArgumentException(
                    "Duplicate fragment id: " + fragment.getId()
            );
        }
    }

    private static void rarities(
            LootTables lootTable,
            Map<Fragment.Rarity, Integer> rarities
    ) {
        if (rarities.isEmpty()) {
            throw new IllegalArgumentException("Rarity map cannot be empty");
        }

        Map<Fragment.Rarity, Integer> validated =
                new EnumMap<>(Fragment.Rarity.class);

        for (Map.Entry<Fragment.Rarity, Integer> entry : rarities.entrySet()) {
            if (entry.getKey() == null) {
                throw new IllegalArgumentException("Rarity cannot be null");
            }

            if (entry.getValue() == null || entry.getValue() <= 0) {
                throw new IllegalArgumentException(
                        "Rarity weight must be positive"
                );
            }

            validated.put(entry.getKey(), entry.getValue());
        }

        LOOT_RARITIES.put(lootTable, validated);
    }
}