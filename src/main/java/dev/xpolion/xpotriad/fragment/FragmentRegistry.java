package dev.xpolion.xpotriad.fragment;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import dev.xpolion.xpotriad.fragment.fragments.BarrierFragment;
import dev.xpolion.xpotriad.fragment.fragments.BlinkFragment;
import dev.xpolion.xpotriad.fragment.fragments.CheatDeathFragment;
import dev.xpolion.xpotriad.fragment.fragments.CleanseFragment;
import dev.xpolion.xpotriad.fragment.fragments.DelayFragment;
import dev.xpolion.xpotriad.fragment.fragments.ExplosionFragment;
import dev.xpolion.xpotriad.fragment.fragments.ExecutionFragment;
import dev.xpolion.xpotriad.fragment.fragments.GravityPullFragment;
import dev.xpolion.xpotriad.fragment.fragments.HealFragment;
import dev.xpolion.xpotriad.fragment.fragments.HoundCallFragment;
import dev.xpolion.xpotriad.fragment.fragments.InvisibilityFragment;
import dev.xpolion.xpotriad.fragment.fragments.KnockbackFragment;
import dev.xpolion.xpotriad.fragment.fragments.LeapFragment;
import dev.xpolion.xpotriad.fragment.fragments.LevitationFragment;
import dev.xpolion.xpotriad.fragment.fragments.LifeStealFragment;
import dev.xpolion.xpotriad.fragment.fragments.LineSnipeFragment;
import dev.xpolion.xpotriad.fragment.fragments.MarkFragment;
import dev.xpolion.xpotriad.fragment.fragments.MassFreezeFragment;
import dev.xpolion.xpotriad.fragment.fragments.MeteorFragment;
import dev.xpolion.xpotriad.fragment.fragments.ReflectFragment;
import dev.xpolion.xpotriad.fragment.fragments.RepeatFragment;
import dev.xpolion.xpotriad.fragment.fragments.RewindFragment;
import dev.xpolion.xpotriad.fragment.fragments.ShieldFragment;
import dev.xpolion.xpotriad.fragment.fragments.SlowFallingFragment;
import dev.xpolion.xpotriad.fragment.fragments.SmokeBombFragment;
import dev.xpolion.xpotriad.fragment.fragments.SoulLinkFragment;
import dev.xpolion.xpotriad.fragment.fragments.SpeedFragment;
import dev.xpolion.xpotriad.fragment.fragments.SwapFragment;
import dev.xpolion.xpotriad.fragment.fragments.ThornsFragment;
import dev.xpolion.xpotriad.fragment.fragments.WeakeningFragment;

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
 *   rarities = COMMON 61 / UNCOMMON 25 / RARE 10 / EPIC 4
 *              (LEGENDARY omitted = weight 0: default structures cannot
 *              drop Legendary. Zero weight = omit the rarity key.)
 *   maxDrops = MAX_FRAGMENTS_PER_INTERACTION (3)
 *
 * Within a selected rarity, fragments are picked uniformly (no weights).
 *
 * Per-source rule: maxDrops(key, n) caps fragments per loot interaction
 * (used by boss sources to drop exactly one).
 */
public final class FragmentRegistry {

    private static final double DEFAULT_CHANCE = 0.15;

    /** Maximum fragments that can drop from a single loot interaction. */
    public static final int MAX_FRAGMENTS_PER_INTERACTION = 3;

    private static final String CHEST_PREFIX = "chests/";

    private static final Map<Fragment.Rarity, Integer> DEFAULT_RARITIES = Map.of(
            Fragment.Rarity.COMMON, 61,
            Fragment.Rarity.UNCOMMON, 25,
            Fragment.Rarity.RARE, 10,
            Fragment.Rarity.EPIC, 4
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

        // --- COMMON ---
        register(new WeakeningFragment());
        register(new KnockbackFragment());
        register(new LeapFragment());
        register(new ShieldFragment());
        register(new SmokeBombFragment());
        register(new SlowFallingFragment());
        register(new DelayFragment());

        // --- UNCOMMON ---
        register(new CleanseFragment());
        register(new LevitationFragment());
        register(new ThornsFragment());

        // --- RARE ---
        register(new GravityPullFragment());
        register(new BlinkFragment());
        register(new LineSnipeFragment());
        register(new BarrierFragment());
        register(new ExecutionFragment());

        // --- EPIC ---
        register(new HoundCallFragment());
        register(new ReflectFragment());
        register(new LifeStealFragment());
        register(new SwapFragment());
        register(new SoulLinkFragment());

        // --- LEGENDARY ---
        register(new RewindFragment());
        register(new MassFreezeFragment());
        register(new MeteorFragment());
        register(new CheatDeathFragment());
        register(new RepeatFragment());

        // ==================================================================
        // STRUCTURE LOOT — all vanilla "chests/..." tables are eligible by
        // default: chance = 15% (DEFAULT_CHANCE), rarities = 61/25/10/4
        // (DEFAULT_RARITIES, no Legendary). Rows below override that.
        // ==================================================================

        // Village — 15% | 55/30/10/4/1  (prefix covers all 16 chests/village/*)
        chance("chests/village/", 0.15);
        rarities("chests/village/", Map.of(
                Fragment.Rarity.COMMON, 55,
                Fragment.Rarity.UNCOMMON, 30,
                Fragment.Rarity.RARE, 10,
                Fragment.Rarity.EPIC, 4,
                Fragment.Rarity.LEGENDARY, 1
        ));

        // Nether Fortress — 20% | 45/30/15/8/2
        chance("chests/nether_bridge", 0.20);
        rarities("chests/nether_bridge", Map.of(
                Fragment.Rarity.COMMON, 45,
                Fragment.Rarity.UNCOMMON, 30,
                Fragment.Rarity.RARE, 15,
                Fragment.Rarity.EPIC, 8,
                Fragment.Rarity.LEGENDARY, 2
        ));

        // Woodland Mansion — 25% | 30/25/20/22/3
        chance("chests/woodland_mansion", 0.25);
        rarities("chests/woodland_mansion", Map.of(
                Fragment.Rarity.COMMON, 30,
                Fragment.Rarity.UNCOMMON, 25,
                Fragment.Rarity.RARE, 20,
                Fragment.Rarity.EPIC, 22,
                Fragment.Rarity.LEGENDARY, 3
        ));

        // Trial Chambers — 25% | 30/25/20/22/3  (prefix covers all 21 keys)
        chance("chests/trial_chambers/", 0.25);
        rarities("chests/trial_chambers/", Map.of(
                Fragment.Rarity.COMMON, 30,
                Fragment.Rarity.UNCOMMON, 25,
                Fragment.Rarity.RARE, 20,
                Fragment.Rarity.EPIC, 22,
                Fragment.Rarity.LEGENDARY, 3
        ));

        // Bastion — 30% | 30/25/25/15/5  (prefix: vanilla keys are bastion_*)
        chance("chests/bastion_", 0.30);
        rarities("chests/bastion_", Map.of(
                Fragment.Rarity.COMMON, 30,
                Fragment.Rarity.UNCOMMON, 25,
                Fragment.Rarity.RARE, 25,
                Fragment.Rarity.EPIC, 15,
                Fragment.Rarity.LEGENDARY, 5
        ));

        // End City — 35% | 20/20/33/20/7
        chance("chests/end_city_treasure", 0.35);
        rarities("chests/end_city_treasure", Map.of(
                Fragment.Rarity.COMMON, 20,
                Fragment.Rarity.UNCOMMON, 20,
                Fragment.Rarity.RARE, 33,
                Fragment.Rarity.EPIC, 20,
                Fragment.Rarity.LEGENDARY, 7
        ));

        // Ancient City — 40% | 15/15/30/30/10  (ice box is part of the structure)
        chance("chests/ancient_city", 0.40);
        rarities("chests/ancient_city", Map.of(
                Fragment.Rarity.COMMON, 15,
                Fragment.Rarity.UNCOMMON, 15,
                Fragment.Rarity.RARE, 30,
                Fragment.Rarity.EPIC, 30,
                Fragment.Rarity.LEGENDARY, 10
        ));
        chance("chests/ancient_city_ice_box", 0.40);
        rarities("chests/ancient_city_ice_box", Map.of(
                Fragment.Rarity.COMMON, 15,
                Fragment.Rarity.UNCOMMON, 15,
                Fragment.Rarity.RARE, 30,
                Fragment.Rarity.EPIC, 30,
                Fragment.Rarity.LEGENDARY, 10
        ));

        // ==================================================================
        // MOB LOOT — opt-in: every listed rule makes that entity eligible,
        // all unlisted mobs (incl. Ghast, Piglin, Zombified Piglin) stay out.
        // ==================================================================

        // Bosses — 100%, exactly one Legendary (Rule 6)
        chance("entities/warden", 1.0);
        rarities("entities/warden", Map.of(Fragment.Rarity.LEGENDARY, 100));
        maxDrops("entities/warden", 1);

        chance("entities/ender_dragon", 1.0);
        rarities("entities/ender_dragon", Map.of(Fragment.Rarity.LEGENDARY, 100));
        maxDrops("entities/ender_dragon", 1);

        chance("entities/wither", 1.0);
        rarities("entities/wither", Map.of(Fragment.Rarity.LEGENDARY, 100));
        maxDrops("entities/wither", 1);

        // Rare mobs
        chance("entities/ravager", 0.35);
        rarities("entities/ravager", Map.of(
                Fragment.Rarity.COMMON, 10,
                Fragment.Rarity.UNCOMMON, 20,
                Fragment.Rarity.RARE, 25,
                Fragment.Rarity.EPIC, 40,
                Fragment.Rarity.LEGENDARY, 5
        ));

        chance("entities/evoker", 0.35);
        rarities("entities/evoker", Map.of(
                Fragment.Rarity.COMMON, 10,
                Fragment.Rarity.UNCOMMON, 20,
                Fragment.Rarity.RARE, 25,
                Fragment.Rarity.EPIC, 40,
                Fragment.Rarity.LEGENDARY, 5
        ));

        chance("entities/elder_guardian", 0.25);
        rarities("entities/elder_guardian", Map.of(
                Fragment.Rarity.COMMON, 5,
                Fragment.Rarity.UNCOMMON, 20,
                Fragment.Rarity.RARE, 45,
                Fragment.Rarity.EPIC, 25,
                Fragment.Rarity.LEGENDARY, 5
        ));

        chance("entities/piglin_brute", 0.20);
        rarities("entities/piglin_brute", Map.of(
                Fragment.Rarity.COMMON, 5,
                Fragment.Rarity.UNCOMMON, 25,
                Fragment.Rarity.RARE, 50,
                Fragment.Rarity.EPIC, 18,
                Fragment.Rarity.LEGENDARY, 2
        ));

        chance("entities/shulker", 0.15);
        rarities("entities/shulker", Map.of(
                Fragment.Rarity.COMMON, 5,
                Fragment.Rarity.UNCOMMON, 25,
                Fragment.Rarity.RARE, 50,
                Fragment.Rarity.EPIC, 18,
                Fragment.Rarity.LEGENDARY, 2
        ));

        chance("entities/blaze", 0.12);
        rarities("entities/blaze", Map.of(
                Fragment.Rarity.COMMON, 5,
                Fragment.Rarity.UNCOMMON, 30,
                Fragment.Rarity.RARE, 45,
                Fragment.Rarity.EPIC, 18,
                Fragment.Rarity.LEGENDARY, 2
        ));

        chance("entities/wither_skeleton", 0.15);
        rarities("entities/wither_skeleton", Map.of(
                Fragment.Rarity.COMMON, 5,
                Fragment.Rarity.UNCOMMON, 30,
                Fragment.Rarity.RARE, 45,
                Fragment.Rarity.EPIC, 18,
                Fragment.Rarity.LEGENDARY, 2
        ));

        chance("entities/pillager", 0.08);
        rarities("entities/pillager", Map.of(
                Fragment.Rarity.COMMON, 5,
                Fragment.Rarity.UNCOMMON, 50,
                Fragment.Rarity.RARE, 35,
                Fragment.Rarity.EPIC, 9,
                Fragment.Rarity.LEGENDARY, 1
        ));

        chance("entities/vindicator", 0.10);
        rarities("entities/vindicator", Map.of(
                Fragment.Rarity.COMMON, 5,
                Fragment.Rarity.UNCOMMON, 40,
                Fragment.Rarity.RARE, 40,
                Fragment.Rarity.EPIC, 14,
                Fragment.Rarity.LEGENDARY, 1
        ));

        // Common hostiles — 0.5% | 70/25/5 (Rule 9; no Epic/Legendary)
        chance("entities/zombie", 0.005);
        rarities("entities/zombie", Map.of(
                Fragment.Rarity.COMMON, 70,
                Fragment.Rarity.UNCOMMON, 25,
                Fragment.Rarity.RARE, 5
        ));

        chance("entities/skeleton", 0.005);
        rarities("entities/skeleton", Map.of(
                Fragment.Rarity.COMMON, 70,
                Fragment.Rarity.UNCOMMON, 25,
                Fragment.Rarity.RARE, 5
        ));

        chance("entities/creeper", 0.005);
        rarities("entities/creeper", Map.of(
                Fragment.Rarity.COMMON, 70,
                Fragment.Rarity.UNCOMMON, 25,
                Fragment.Rarity.RARE, 5
        ));
    }

    private FragmentRegistry() {
    }

    public static Fragment get(String id) {
        return FRAGMENTS.get(id);
    }

    /**
     * Unmodifiable view of every registered fragment, in registration order
     * is not guaranteed (backed by a HashMap).
     */
    public static java.util.Collection<Fragment> all() {
        return java.util.Collections.unmodifiableCollection(FRAGMENTS.values());
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

        return rollOnce(source, exact, prefix);
    }

    /**
     * Rolls up to {@code max} fragments for the given source, stopping at
     * the first failed roll (chance miss, ineligible source, or empty
     * rarity pool). A per-source maxDrops rule can lower (not raise) the
     * cap. Duplicates are allowed.
     */
    public static List<Fragment> roll(LootSource source, int max) {
        if (max < 0) {
            throw new IllegalArgumentException("Max cannot be negative");
        }

        SourceRule exact = EXACT_RULES.get(source.key());
        SourceRule prefix = matchPrefix(source.key());

        int limit = Math.min(max, resolveMaxDrops(exact, prefix));

        List<Fragment> drops = new ArrayList<>();

        for (int i = 0; i < limit; i++) {
            Fragment fragment = rollOnce(source, exact, prefix);

            if (fragment == null) {
                break;
            }

            drops.add(fragment);
        }

        return drops;
    }

    private static Fragment rollOnce(
            LootSource source,
            SourceRule exact,
            SourceRule prefix
    ) {
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

    /**
     * Caps how many fragments can drop from one interaction for a source key.
     * Key ends with '/' or '_'  -> prefix rule, otherwise exact rule.
     */
    private static void maxDrops(String key, int max) {
        if (max < 1) {
            throw new IllegalArgumentException("Max drops must be at least 1");
        }

        ruleFor(key).maxDrops = max;
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

    private static int resolveMaxDrops(SourceRule exact, SourceRule prefix) {
        if (exact != null && exact.maxDrops != null) {
            return exact.maxDrops;
        }

        if (prefix != null && prefix.maxDrops != null) {
            return prefix.maxDrops;
        }

        return MAX_FRAGMENTS_PER_INTERACTION;
    }

    private static final class SourceRule {

        private Double chance;
        private Map<Fragment.Rarity, Integer> rarities;
        private Integer maxDrops;
    }
}