package dev.xpolion.xpotriad.ability;

import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.FragmentRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages the engraving, reading, and cooldown state of Ability definitions
 * on ItemStacks using PDC.
 *
 * PDC keys:
 *   xpotriad:ability          - presence marker
 *   xpotriad:item_type        - "ability"
 *   xpotriad:ability_id       - unique ability identifier
 *   xpotriad:pre_cast_fragment
 *   xpotriad:cast_fragment
 *   xpotriad:post_cast_fragment
 *   xpotriad:cooldown_until   - timestamp (epoch ms) until which this item is on cooldown
 *   xpotriad:executing        - true while the ability sequence is in flight
 *   xpotriad:executing_cast_at - epoch ms when the executing flag was written
 */
public final class AbilityItem {

    private static final String ABILITY_MARKER = "ability";

    /**
     * Failsafe window for the executing flag. A real sequence lasts a few
     * seconds at most (stage execution times + buffers), so a flag older
     * than 500 ticks means the completion hook never ran (crash or thrown
     * effect) and the flag must be discarded.
     */
    private static final long EXECUTING_FAILSAFE_TICKS = 500L;

    private static JavaPlugin plugin;

    private static NamespacedKey abilityKey;
    private static NamespacedKey itemTypeKey;
    private static NamespacedKey abilityIdKey;
    private static NamespacedKey cooldownUntilKey;
    private static NamespacedKey executingKey;
    private static NamespacedKey executingCastAtKey;

    private static final NamespacedKey[] fragmentKeys =
            new NamespacedKey[Ability.Stage.values().length];

    private AbilityItem() {
    }

    public static void initialize(JavaPlugin plugin) {
        AbilityItem.plugin = plugin;

        abilityKey         = new NamespacedKey(plugin, "ability");
        itemTypeKey        = new NamespacedKey(plugin, "item_type");
        abilityIdKey       = new NamespacedKey(plugin, "ability_id");
        cooldownUntilKey   = new NamespacedKey(plugin, "cooldown_until");
        executingKey       = new NamespacedKey(plugin, "executing");
        executingCastAtKey = new NamespacedKey(plugin, "executing_cast_at");

        fragmentKeys[Ability.Stage.PRE_CAST.ordinal()] =
                new NamespacedKey(plugin, "pre_cast_fragment");

        fragmentKeys[Ability.Stage.CAST.ordinal()] =
                new NamespacedKey(plugin, "cast_fragment");

        fragmentKeys[Ability.Stage.POST_CAST.ordinal()] =
                new NamespacedKey(plugin, "post_cast_fragment");
    }

    public static void engrave(ItemStack item, Ability ability) {
        if (item == null || item.getType() == Material.AIR) {
            throw new IllegalArgumentException("Cannot engrave an empty item");
        }

        if (ability == null) {
            throw new IllegalArgumentException("Ability cannot be null");
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            throw new IllegalArgumentException(
                    "Item does not support item meta"
            );
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // -----------------------------------------------------------------
        // Ability identity
        // -----------------------------------------------------------------

        pdc.set(
                abilityKey,
                PersistentDataType.STRING,
                ABILITY_MARKER
        );

        pdc.set(
                itemTypeKey,
                PersistentDataType.STRING,
                ABILITY_MARKER
        );

        pdc.set(
                abilityIdKey,
                PersistentDataType.STRING,
                UUID.randomUUID().toString()
        );

        // -----------------------------------------------------------------
        // Fragments
        // -----------------------------------------------------------------

        for (Ability.Stage stage : Ability.Stage.values()) {
            int index = stage.ordinal();
            Fragment fragment = ability.getFragment(stage);

            if (fragment != null) {
                pdc.set(
                        fragmentKeys[index],
                        PersistentDataType.STRING,
                        fragment.getId()
                );
            } else {
                // Remove an old fragment when the new ability leaves
                // this stage empty.
                pdc.remove(fragmentKeys[index]);
            }
        }

        // A newly engraved ability starts without an active cooldown
        // and is never mid-execution.
        pdc.remove(cooldownUntilKey);
        pdc.remove(executingKey);
        pdc.remove(executingCastAtKey);

        // -----------------------------------------------------------------
        // Ability lore
        // -----------------------------------------------------------------
        //
        // This intentionally replaces the item's lore completely.
        // The item itself still retains all other ItemMeta:
        // name, enchantments, attributes, damage/durability, etc.
        //
        meta.lore(createLore(ability));
        meta.setEnchantmentGlintOverride(true);

        item.setItemMeta(meta);
    }

    public static Ability read(ItemStack item) {
        if (!isAbilityItem(item)) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return null;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        Ability ability = new Ability();

        for (Ability.Stage stage : Ability.Stage.values()) {
            int index = stage.ordinal();

            String fragmentId =
                    pdc.get(fragmentKeys[index], PersistentDataType.STRING);

            if (fragmentId != null) {
                Fragment fragment = FragmentRegistry.get(fragmentId);

                if (fragment != null) {
                    ability.setFragment(stage, fragment);
                }

                // Unknown fragment IDs are ignored gracefully.
            }
        }

        return ability;
    }

    public static boolean isAbilityItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String marker =
                pdc.get(abilityKey, PersistentDataType.STRING);

        if (ABILITY_MARKER.equals(marker)) {
            return true;
        }

        String itemType =
                pdc.get(itemTypeKey, PersistentDataType.STRING);

        return ABILITY_MARKER.equals(itemType);
    }

    // ------------------------------------------------------------------
    // Ability identity (used to re-resolve a stack for deferred cooldown)
    // ------------------------------------------------------------------

    /** Returns the ability id PDC value, or null if the item is not an ability. */
    public static String readAbilityId(ItemStack item) {
        if (!isAbilityItem(item)) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return null;
        }

        return meta.getPersistentDataContainer().get(
                abilityIdKey,
                PersistentDataType.STRING
        );
    }

    /**
     * Finds the live inventory stack carrying the given ability id.
     *
     * <p>Checks main hand, off hand, then the full inventory (storage +
     * armor + extra). Returns the live reference so a cooldown write persists;
     * returns null when no matching stack is currently held.
     */
    public static ItemStack findByAbilityId(Player player, String abilityId) {
        if (player == null || abilityId == null) {
            return null;
        }

        PlayerInventory inventory = player.getInventory();

        ItemStack mainHand = inventory.getItemInMainHand();
        if (abilityId.equals(readAbilityId(mainHand))) {
            return mainHand;
        }

        ItemStack offHand = inventory.getItemInOffHand();
        if (abilityId.equals(readAbilityId(offHand))) {
            return offHand;
        }

        for (ItemStack stack : inventory.getContents()) {
            if (stack != null && abilityId.equals(readAbilityId(stack))) {
                return stack;
            }
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // Cooldown management
    // -------------------------------------------------------------------------

    public static boolean isOnCooldown(ItemStack item) {
        return getRemainingCooldownTicks(item) > 0;
    }

    public static long getRemainingCooldownTicks(ItemStack item) {
        if (!isAbilityItem(item)) {
            return 0L;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return 0L;
        }

        Long until = meta.getPersistentDataContainer().get(
                cooldownUntilKey,
                PersistentDataType.LONG
        );

        if (until == null) {
            return 0L;
        }

        long remainingMillis = until - System.currentTimeMillis();

        if (remainingMillis <= 0) {
            return 0L;
        }

        return (remainingMillis + 49) / 50;
    }

    public static void applyCooldown(ItemStack item, long cooldownTicks) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return;
        }

        long until =
                System.currentTimeMillis() + (cooldownTicks * 50L);

        meta.getPersistentDataContainer().set(
                cooldownUntilKey,
                PersistentDataType.LONG,
                until
        );

        item.setItemMeta(meta);
    }

    // -------------------------------------------------------------------------
    // Executing flag (blocks clicks until the deferred cooldown is written)
    // -------------------------------------------------------------------------

    /** Result of an executing-flag lookup. */
    public enum ExecutingState {
        /** No flag present. */
        NOT_EXECUTING,
        /** Flag present and within the failsafe window. */
        EXECUTING,
        /** Flag present but expired - dead sequence, caller may proceed. */
        STALE
    }

    /**
     * Flags the item as mid-sequence. Written before the engine starts so a
     * click arriving before the completion hook cannot fire the ability again.
     */
    public static void markExecuting(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        pdc.set(executingKey, PersistentDataType.BOOLEAN, Boolean.TRUE);
        pdc.set(executingCastAtKey, PersistentDataType.LONG, System.currentTimeMillis());

        item.setItemMeta(meta);
    }

    /**
     * Fresh flag -&gt; EXECUTING (block the click). Expired or timestamp-less
     * flag -&gt; STALE: both keys are removed here and a warning is logged, so
     * the caller falls through and a dead sequence never bricks the item.
     */
    public static ExecutingState executingState(ItemStack item) {
        if (!isAbilityItem(item)) {
            return ExecutingState.NOT_EXECUTING;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return ExecutingState.NOT_EXECUTING;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        Boolean flag = pdc.get(executingKey, PersistentDataType.BOOLEAN);

        if (flag == null || !flag) {
            return ExecutingState.NOT_EXECUTING;
        }

        Long castAt = pdc.get(executingCastAtKey, PersistentDataType.LONG);

        if (castAt != null
                && System.currentTimeMillis() - castAt <= EXECUTING_FAILSAFE_TICKS * 50L) {
            return ExecutingState.EXECUTING;
        }

        clearExecuting(item);

        if (plugin != null) {
            plugin.getLogger().warning(
                    "[XpoTriad] Cleared stale executing flag - the previous "
                            + "ability sequence never completed."
            );
        }

        return ExecutingState.STALE;
    }

    /** Releases the executing flag (completion hook, engrave, deconstruct). */
    public static void clearExecuting(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        pdc.remove(executingKey);
        pdc.remove(executingCastAtKey);

        item.setItemMeta(meta);
    }

    // -------------------------------------------------------------------------
    // Lore
    // -------------------------------------------------------------------------

    private static List<Component> createLore(Ability ability) {
        List<Component> lore = new ArrayList<>();

        lore.add(Component.text("Ability", NamedTextColor.DARK_PURPLE));

        addStageLore(lore, "Pre-Cast",  ability.getFragment(Ability.Stage.PRE_CAST));
        addStageLore(lore, "Cast",      ability.getFragment(Ability.Stage.CAST));
        addStageLore(lore, "Post-Cast", ability.getFragment(Ability.Stage.POST_CAST));

        long cooldownTicks = ability.calculateCooldown();
        double cooldownSeconds = cooldownTicks / 20.0;

        lore.add(
                Component.text("Cooldown: ", NamedTextColor.GRAY)
                        .append(
                                Component.text(
                                        formatCooldown(cooldownSeconds),
                                        NamedTextColor.WHITE
                                )
                        )
        );

        return lore;
    }

    /**
     * Adds two lore lines per occupied stage:
     *
     *   Pre-Cast: <Fragment Name> [<RARITY>]
     *   Execution Time: <time>
     *
     * If the stage is empty, adds the existing "Empty" single line only.
     */
    private static void addStageLore(
            List<Component> lore,
            String displayName,
            Fragment fragment
    ) {
        if (fragment == null) {
            lore.add(
                    Component.text(displayName + ": ", NamedTextColor.LIGHT_PURPLE)
                            .append(Component.text("Empty", NamedTextColor.GRAY))
            );
            return;
        }

        // Stage label + fragment name (white) + rarity tag in rarity color
        lore.add(
                Component.text(displayName + ": ", NamedTextColor.LIGHT_PURPLE)
                        .append(
                                Component.text(
                                        fragment.getName() + " ",
                                        NamedTextColor.WHITE
                                )
                        )
                        .append(
                                Component.text(
                                        "[" + fragment.getRarity().name() + "]",
                                        fragment.getRarity().getColor()
                                )
                        )
        );

        // Execution time on the next line, indented
        double execSeconds = fragment.getExecutionTime() / 20.0;
        lore.add(
                Component.text("Execution Time: ", NamedTextColor.GRAY)
                        .append(
                                Component.text(
                                        formatCooldown(execSeconds),
                                        NamedTextColor.WHITE
                                )
                        )
        );
    }

    private static String formatCooldown(double seconds) {
        if (seconds == Math.floor(seconds)) {
            return String.format("%.0fs", seconds);
        }

        return String.format("%.2fs", seconds);
    }

    public static ItemStack deconstruct(ItemStack item) {
        if (!isAbilityItem(item)) {
            return null;
        }

        ItemStack result = item.clone();
        ItemMeta meta = result.getItemMeta();

        if (meta == null) {
            return null;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        pdc.remove(abilityKey);
        pdc.remove(itemTypeKey);
        pdc.remove(abilityIdKey);
        pdc.remove(cooldownUntilKey);
        pdc.remove(executingKey);
        pdc.remove(executingCastAtKey);

        for (NamespacedKey fragmentKey : fragmentKeys) {
            pdc.remove(fragmentKey);
        }

        meta.lore(null);
        meta.setEnchantmentGlintOverride(false);

        result.setItemMeta(meta);

        return result;
    }
}
