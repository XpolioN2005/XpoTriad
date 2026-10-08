package dev.xpolion.xpotriad.ability;

import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.FragmentRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
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
 */
public final class AbilityItem {

    private static final String ABILITY_MARKER = "ability";

    private static NamespacedKey abilityKey;
    private static NamespacedKey itemTypeKey;
    private static NamespacedKey abilityIdKey;
    private static NamespacedKey cooldownUntilKey;

    private static final NamespacedKey[] fragmentKeys =
            new NamespacedKey[Ability.Stage.values().length];

    private AbilityItem() {
    }

    public static void initialize(JavaPlugin plugin) {
        abilityKey       = new NamespacedKey(plugin, "ability");
        itemTypeKey      = new NamespacedKey(plugin, "item_type");
        abilityIdKey     = new NamespacedKey(plugin, "ability_id");
        cooldownUntilKey = new NamespacedKey(plugin, "cooldown_until");

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

        // A newly engraved ability starts without an active cooldown.
        pdc.remove(cooldownUntilKey);

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

        for (NamespacedKey fragmentKey : fragmentKeys) {
            pdc.remove(fragmentKey);
        }

        meta.lore(null);
        meta.setEnchantmentGlintOverride(false);

        result.setItemMeta(meta);

        return result;
    }
}
