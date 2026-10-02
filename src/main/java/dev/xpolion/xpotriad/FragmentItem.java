package dev.xpolion.xpotriad;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class FragmentItem {

    private static final String ITEM_TYPE = "fragment";

    private static NamespacedKey itemTypeKey;
    private static NamespacedKey fragmentIdKey;

    private FragmentItem() {
    }

    public static void initialize(JavaPlugin plugin) {
        itemTypeKey = new NamespacedKey(plugin, "item_type");
        fragmentIdKey = new NamespacedKey(plugin, "fragment_id");
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

        if (!fragment.getLore().isEmpty()) {
            meta.setLore(fragment.getLore());
        }

        if (fragment.hasGlint()) {
            meta.setEnchantmentGlintOverride(true);
        }

        PersistentDataContainer pdc =
                meta.getPersistentDataContainer();

        pdc.set(
                itemTypeKey,
                PersistentDataType.STRING,
                ITEM_TYPE
        );

        pdc.set(
                fragmentIdKey,
                PersistentDataType.STRING,
                fragment.getId()
        );

        item.setItemMeta(meta);

        return item;
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