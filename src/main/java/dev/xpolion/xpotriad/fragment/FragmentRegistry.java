package dev.xpolion.xpotriad.fragment;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import dev.xpolion.xpotriad.fragment.fragments.ExplosionFragment;
import dev.xpolion.xpotriad.fragment.fragments.HealFragment;
import dev.xpolion.xpotriad.fragment.fragments.InvisibilityFragment;
import dev.xpolion.xpotriad.fragment.fragments.MarkFragment;
import dev.xpolion.xpotriad.fragment.fragments.SpeedFragment;

/**
 * Owns fragment definitions and ALL fragment loot logic.
 *
 * Loot flow (single entry point):
 *   resolve source config -> spawn chance -> rarity roll -> uniform fragment
 *
 * Source configuration:
 *   - Keys ending with '/' or '_' are prefix rules (match by startsWith).
 *   - All other keys are exact rules (match by equals).
 *   - Exact rules take priority over prefix rules.
 *   - Every "chests/..." loot-table key is eligible by default.
 *   - All other sources (entities, ...) are opt-in: they require at
 *     least one exact or prefix rule to exist.
 *
 * Defaults:
 *   chance   = 0.15
 *   rarities = COMMON 60 / UNCOMMON 25 / RARE 10 / EPIC 4 / LEGENDARY 1
 *
 * Within a selected rarity, fragments are picked uniformly (no weights).
 */
public final class FragmentRegistry {

    private static final double DEFAULT_CHANCE = 0.15;

    /** Maximum fragments that can drop from a single loot interaction. */
    public static final int MAX_FRAGMENTS_PER_INTERACTION = 3;

    private static final String CHEST_PREFIX = "chests/";

    private static final Map<Fragment.Rarity, Integer> DEFAULT_RARITIES = Map.of(
            Fragment.Rarity.COMMON, 60,
            Fragment.Rarity.UNCOMMON, 25,
            Fragment.Rarity.RARE, 10,
            Fragment.Rarity.EPIC, 4,
            Fragment.Rarity.LEGENDARY, 1
    );

    private static final Map<String, Fragment> FRAGMENTS = new HashMap<>();

    /** Exact-key source rules (key does not end with '/' or '_'). */
    private static final Map<String, SourceRule> EXACT_RULES = new HashMap<>();

    /** Prefix source rules (key ends with '/' or '_'), matched by startsWith. */
    private static final Map<String, SourceRule> PREFIX_RULES = new HashMap<>();

    static {
        register(new InvisibilityFragment());
        register(new SpeedFragment());
        register(new HealFragment());
        register(new ExplosionFragment());
        register(new MarkFragment());

        // --- Exact loot-table rules (real Paper 26.3 keys) ---

        rarities("chests/ancient_city", Map.of(
                Fragment.Rarity.COMMON, 45,
                Fragment.Rarity.UNCOMMON, 30,
                Fragment.Rarity.RARE, 15,
                Fragment.Rarity.EPIC, 7,
                Fragment.Rarity.LEGENDARY, 3
        ));
        chance("chests/ancient_city", 0.15);

        rarities("chests/woodland_mansion", Map.of(
                Fragment.Rarity.COMMON, 50,
                Fragment.Rarity.UNCOMMON, 25,
                Fragment.Rarity.RARE, 15,
                Fragment.Rarity.EPIC, 7,
                Fragment.Rarity.LEGENDARY, 3
        ));

        rarities("chests/nether_bridge", Map.of(
                Fragment.Rarity.COMMON, 55,
                Fragment.Rarity.UNCOMMON, 25,
                Fragment.Rarity.RARE, 12,
                Fragment.Rarity.EPIC, 5,
                Fragment.Rarity.LEGENDARY, 3
        ));

        // --- Prefix rules: one line covers a whole key group ---
        // Vanilla bastion keys are "chests/bastion_*" (underscore), so the
        // prefix rule uses '_' to match them all.
        rarities("chests/bastion_", Map.of(
                Fragment.Rarity.COMMON, 45,
                Fragment.Rarity.UNCOMMON, 25,
                Fragment.Rarity.RARE, 15,
                Fragment.Rarity.EPIC, 10,
                Fragment.Rarity.LEGENDARY, 5
        ));

        rarities("chests/trial_chambers/", Map.of(
                Fragment.Rarity.COMMON, 50,
                Fragment.Rarity.UNCOMMON, 25,
                Fragment.Rarity.RARE, 15,
                Fragment.Rarity.EPIC, 7,
                Fragment.Rarity.LEGENDARY, 3
        ));

        rarities("chests/village/", Map.of(
                Fragment.Rarity.COMMON, 70,
                Fragment.Rarity.UNCOMMON, 20,
                Fragment.Rarity.RARE, 7,
                Fragment.Rarity.EPIC, 2,
                Fragment.Rarity.LEGENDARY, 1
        ));

        // --- Entity sources are opt-in; without a rule they never drop ---
        // Example (commented out):
        // chance("entities/", 0.01);
    }

    private FragmentRegistry() {
    }

    public static Fragment get(String id) {
        return FRAGMENTS.get(id);
    }

    // ------------------------------------------------------------------
    // Loot entry point: ALL loot logic lives here
    // ------------------------------------------------------------------

    /**
     * Rolls fragment loot for the given source.
     *
     * @return a fragment to drop, or null when the source is not eligible,
     *         the chance roll fails, or no fragment matches the rolled rarity.
     */
    public static Fragment roll(LootSource source) {
        SourceRule exact = EXACT_RULES.get(source.key());
        SourceRule prefix = matchPrefix(source.key());

        if (exact == null && prefix == null && !source.key().startsWith(CHEST_PREFIX)) {
            return null; // not eligible (opt-in source without rules)
        }

        double chance = resolveChance(exact, prefix);

        if (chance <= 0.0 || ThreadLocalRandom.current().nextDouble() >= chance) {
            return null;
        }

        Fragment.Rarity rarity = rollRarity(resolveRarities(exact, prefix));

        if (rarity == null) {
            return null;
        }

        return rollFragment(rarity);
    }

    /**
     * Rolls up to {@code max} fragments for the given source, stopping at
     * the first failed roll (chance miss, ineligible source, or empty
     * rarity pool). Duplicates are allowed.
     */
    public static List<Fragment> roll(LootSource source, int max) {
        if (max < 0) {
            throw new IllegalArgumentException("Max cannot be negative");
        }

        List<Fragment> drops = new ArrayList<>();

        for (int i = 0; i < max; i++) {
            Fragment fragment = roll(source);

            if (fragment == null) {
                break;
            }

            drops.add(fragment);
        }

        return drops;
    }

    private static Fragment.Rarity rollRarity(Map<Fragment.Rarity, Integer> rarities) {
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

    // ------------------------------------------------------------------
    // Source configuration (compact rules API)
    // ------------------------------------------------------------------

    /**
     * Sets the spawn chance for a source key.
     *
     * Key ends with '/' or '_'  -> prefix rule (matches key group).
     * Otherwise                 -> exact rule.
     */
    private static void chance(String key, double chance) {
        if (chance < 0.0 || chance > 1.0) {
            throw new IllegalArgumentException("Chance must be between 0.0 and 1.0");
        }

        ruleFor(key).chance = chance;
    }

    /**
     * Sets rarity weights for a source key.
     * All weights must be positive.
     */
    private static void rarities(String key, Map<Fragment.Rarity, Integer> rarities) {
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
                throw new IllegalArgumentException("Rarity weight must be positive");
            }

            validated.put(entry.getKey(), entry.getValue());
        }

        ruleFor(key).rarities = validated;
    }

    private static SourceRule ruleFor(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Source key cannot be null or blank");
        }

        if (isPrefixKey(key)) {
            return PREFIX_RULES.computeIfAbsent(key, k -> new SourceRule());
        }

        return EXACT_RULES.computeIfAbsent(key, k -> new SourceRule());
    }

    private static boolean isPrefixKey(String key) {
        return key.endsWith("/") || key.endsWith("_");
    }

    // ------------------------------------------------------------------
    // Rule resolution (exact beats prefix; longest prefix wins)
    // ------------------------------------------------------------------

    private static SourceRule matchPrefix(String key) {
        SourceRule best = null;
        int bestLength = -1;

        for (Map.Entry<String, SourceRule> entry : PREFIX_RULES.entrySet()) {
            if (key.startsWith(entry.getKey())
                    && entry.getKey().length() > bestLength) {
                best = entry.getValue();
                bestLength = entry.getKey().length();
            }
        }

        return best;
    }

    private static double resolveChance(SourceRule exact, SourceRule prefix) {
        if (exact != null && exact.chance != null) {
            return exact.chance;
        }

        if (prefix != null && prefix.chance != null) {
            return prefix.chance;
        }

        return DEFAULT_CHANCE;
    }

    private static Map<Fragment.Rarity, Integer> resolveRarities(
            SourceRule exact,
            SourceRule prefix
    ) {
        if (exact != null && exact.rarities != null) {
            return exact.rarities;
        }

        if (prefix != null && prefix.rarities != null) {
            return prefix.rarities;
        }

        return DEFAULT_RARITIES;
    }

    private static final class SourceRule {

        private Double chance;
        private Map<Fragment.Rarity, Integer> rarities;
    }
}