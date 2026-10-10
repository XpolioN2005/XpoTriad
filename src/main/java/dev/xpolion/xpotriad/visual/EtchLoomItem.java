package dev.xpolion.xpotriad.visual;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * The craftable EtchLoom item.
 *
 * <p>Right-clicking it opens the EtchLoom GUI from anywhere. It is a
 * plain {@link Material#BOOK} (not written/writable) so vanilla does
 * nothing on right-click and the interaction is fully owned by
 * {@link EtchLoomItemListener}.
 *
 * <p>PDC keys (shared namespace with {@code FragmentItem} /
 * {@code AbilityItem}):
 *   xpotriad:item_type = "etch_loom"
 */
public final class EtchLoomItem {

    private static final String ITEM_TYPE = "etch_loom";

    private static NamespacedKey itemTypeKey;

    private EtchLoomItem() {
    }

    public static void initialize(JavaPlugin plugin) {
        itemTypeKey = new NamespacedKey(plugin, "item_type");
    }

    /**
     * Builds a fresh EtchLoom item. One per craft; never breaks.
     */
    public static ItemStack create() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            throw new IllegalStateException("BOOK does not support item meta");
        }

        meta.displayName(Component.text("EtchLoom", NamedTextColor.LIGHT_PURPLE));
        meta.lore(List.of(
                Component.text("Right-click to open the EtchLoom.", NamedTextColor.GRAY)
        ));
        meta.setEnchantmentGlintOverride(true);
        meta.setMaxStackSize(1);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(itemTypeKey, PersistentDataType.STRING, ITEM_TYPE);

        item.setItemMeta(meta);

        return item;
    }

    /**
     * True if {@code item} is an EtchLoom item (identified by PDC).
     */
    public static boolean isEtchLoomItem(ItemStack item) {
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
}
