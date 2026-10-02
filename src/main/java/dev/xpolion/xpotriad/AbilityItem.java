package dev.xpolion.xpotriad;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class AbilityItem {

    private static final String ABILITY_MARKER = "ability";

    private static NamespacedKey abilityKey;
    private static NamespacedKey weaponTypeKey;

    private static final NamespacedKey[] fragmentKeys = new NamespacedKey[Ability.Stage.values().length];
    private static final NamespacedKey[] delayKeys = new NamespacedKey[Ability.Stage.values().length];

    private AbilityItem() {
    }

    public static void initialize(JavaPlugin plugin) {
        abilityKey = new NamespacedKey(plugin, "ability");
        weaponTypeKey = new NamespacedKey(plugin, "weapon_type");

        fragmentKeys[Ability.Stage.PRE_CAST.ordinal()] =
                new NamespacedKey(plugin, "pre_cast_fragment");
        delayKeys[Ability.Stage.PRE_CAST.ordinal()] =
                new NamespacedKey(plugin, "pre_cast_delay");

        fragmentKeys[Ability.Stage.CAST.ordinal()] =
                new NamespacedKey(plugin, "cast_fragment");
        delayKeys[Ability.Stage.CAST.ordinal()] =
                new NamespacedKey(plugin, "cast_delay");

        fragmentKeys[Ability.Stage.POST_CAST.ordinal()] =
                new NamespacedKey(plugin, "post_cast_fragment");
        delayKeys[Ability.Stage.POST_CAST.ordinal()] =
                new NamespacedKey(plugin, "post_cast_delay");
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
            throw new IllegalArgumentException("Item does not support item meta");
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        pdc.set(
                abilityKey,
                PersistentDataType.STRING,
                ABILITY_MARKER
        );

        pdc.set(
                weaponTypeKey,
                PersistentDataType.STRING,
                ability.getWeaponType().name()
        );

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
                pdc.remove(fragmentKeys[index]);
            }

            pdc.set(
                    delayKeys[index],
                    PersistentDataType.LONG,
                    ability.getDelay(stage)
            );
        }

        meta.setLore(createLore(ability));
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

        String weaponTypeName = pdc.get(
                weaponTypeKey,
                PersistentDataType.STRING
        );

        if (weaponTypeName == null) {
            return null;
        }

        Ability.WeaponType weaponType;

        try {
            weaponType = Ability.WeaponType.valueOf(weaponTypeName);
        } catch (IllegalArgumentException exception) {
            return null;
        }

        Ability ability = new Ability(weaponType);

        for (Ability.Stage stage : Ability.Stage.values()) {
            int index = stage.ordinal();

            String fragmentId = pdc.get(
                    fragmentKeys[index],
                    PersistentDataType.STRING
            );

            Long delay = pdc.get(
                    delayKeys[index],
                    PersistentDataType.LONG
            );

            if (fragmentId != null) {
                Fragment fragment = FragmentRegistry.get(fragmentId);

                if (fragment == null) {
                    return null;
                }

                ability.setFragment(stage, fragment);
            }

            if (delay != null) {
                ability.setDelay(stage, delay);
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

        String marker = meta.getPersistentDataContainer().get(
                abilityKey,
                PersistentDataType.STRING
        );

        return ABILITY_MARKER.equals(marker);
    }

    private static List<String> createLore(Ability ability) {
        List<String> lore = new ArrayList<>();

        lore.add(ChatColor.DARK_PURPLE + "Ability");
        lore.add("");

        addStageLore(
                lore,
                "Pre-Cast",
                ability,
                Ability.Stage.PRE_CAST
        );

        addStageLore(
                lore,
                "Cast",
                ability,
                Ability.Stage.CAST
        );

        addStageLore(
                lore,
                "Post-Cast",
                ability,
                Ability.Stage.POST_CAST
        );

        return lore;
    }

    private static void addStageLore(
            List<String> lore,
            String displayName,
            Ability ability,
            Ability.Stage stage
    ) {
        Fragment fragment = ability.getFragment(stage);

        lore.add(ChatColor.LIGHT_PURPLE + displayName);

        if (fragment == null) {
            lore.add(ChatColor.GRAY + "  Empty");
        } else {
            lore.add(
                    ChatColor.WHITE
                            + "  "
                            + ChatColor.stripColor(fragment.getDisplayName())
            );

            lore.add(
                    ChatColor.GRAY
                            + "  Delay: "
                            + formatDelay(ability.getDelay(stage))
            );
        }

        lore.add("");
    }

    private static String formatDelay(long ticks) {
        double seconds = ticks / 20.0;

        if (seconds == Math.floor(seconds)) {
            return String.format("%.0fs", seconds);
        }

        return String.format("%.1fs", seconds);
    }
}