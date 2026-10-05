package dev.xpolion.xpotriad.fragment;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages physical Fragment item creation and identification.
 *
 * PDC keys:
 *   xpotriad:item_type   = "fragment"
 *   xpotriad:fragment_id = <fragment id>
 *
 * Lore structure produced by create():
 *
 *   <Fragment Name>
 *
 *   <existing Fragment description lines>
 *
 *   Rarity: <RARITY>
 *   Type: <MELEE|RANGED>
 *
 *   Execution Time: <time>
 *   Cooldown Modifier: <modifier>
 */
public final class FragmentItem {

    private static final String ITEM_TYPE = "fragment";

    private static NamespacedKey itemTypeKey;
    private static NamespacedKey fragmentIdKey;

    private FragmentItem() {
    }

    public static void initialize(JavaPlugin plugin) {
        itemTypeKey    = new NamespacedKey(plugin, "item_type");
        fragmentIdKey  = new NamespacedKey(plugin, "fragment_id");
    }

    public static ItemStack create(Fragment fragment) {
        if (fragment == null) {
            throw new IllegalArgumentException("Fragment cannot be null");
        }

        ItemStack item = new ItemStack(fragment.getMaterial());
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            throw new IllegalArgumentException(
                    "Fragment material does not support item meta"
            );
        }

        meta.setDisplayName(fragment.getDisplayName());
        meta.setLore(buildLore(fragment));

        if (fragment.hasGlint()) {
            meta.setEnchantmentGlintOverride(true);
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        pdc.set(itemTypeKey,   PersistentDataType.STRING, ITEM_TYPE);
        pdc.set(fragmentIdKey, PersistentDataType.STRING, fragment.getId());

        item.setItemMeta(meta);

        return item;
    }

    /**
     * Builds the structured lore for a Fragment item.
     *
     * Groups:
     *   Identity     – existing description lines (from Fragment.getLore())
     *   Classification – Rarity, Type
     *   Timing       – Execution Time, Cooldown Modifier
     */
    private static List<String> buildLore(Fragment fragment) {
        List<String> lore = new ArrayList<>();

        // --- Identity: existing description lines ---
        if (!fragment.getLore().isEmpty()) {
            for (String line : fragment.getLore()) {
                lore.add(line);
            }
            lore.add(""); // blank separator
        }

        // --- Classification ---
        lore.add(ChatColor.GRAY + "Rarity: " + fragment.getRarity().getColor() + fragment.getRarity().name());
        lore.add(ChatColor.GRAY + "Type: " + ChatColor.WHITE + fragment.getType().name());

        lore.add(""); // blank separator

        // --- Timing ---
        lore.add(ChatColor.GRAY + "Execution Time: " + ChatColor.WHITE + formatTicks(fragment.getExecutionTime()));
        lore.add(ChatColor.GRAY + "Cooldown Modifier: " + ChatColor.WHITE + formatModifier(fragment.getCooldownModifier()));

        return lore;
    }

    /**
     * Formats a tick count as seconds (e.g. "10t" or "0.5s").
     * Consistent with the Fragment API: values are in ticks.
     */
    private static String formatTicks(long ticks) {
        double seconds = ticks / 20.0;
        if (seconds == Math.floor(seconds)) {
            return String.format("%.0fs", seconds);
        }
        return String.format("%.2fs", seconds);
    }

    /**
     * Formats a cooldown modifier tick value, showing sign explicitly.
     * e.g. +40 ticks → "+2s", 0 ticks → "0s", -10 ticks → "-0.5s"
     */
    private static String formatModifier(long ticks) {
        double seconds = ticks / 20.0;
        if (ticks > 0) {
            if (seconds == Math.floor(seconds)) {
                return String.format("+%.0fs", seconds);
            }
            return String.format("+%.2fs", seconds);
        }
        if (seconds == Math.floor(seconds)) {
            return String.format("%.0fs", seconds);
        }
        return String.format("%.2fs", seconds);
    }

    public static boolean isFragmentItem(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return false;
        }

        String itemType = meta.getPersistentDataContainer().get(
                itemTypeKey,
                PersistentDataType.STRING
        );

        return ITEM_TYPE.equals(itemType);
    }

    public static String getFragmentId(ItemStack item) {
        if (!isFragmentItem(item)) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return null;
        }

        return meta.getPersistentDataContainer().get(
                fragmentIdKey,
                PersistentDataType.STRING
        );
    }

    public static Fragment getFragment(ItemStack item) {
        String fragmentId = getFragmentId(item);

        if (fragmentId == null) {
            return null;
        }

        return FragmentRegistry.get(fragmentId);
    }
}
