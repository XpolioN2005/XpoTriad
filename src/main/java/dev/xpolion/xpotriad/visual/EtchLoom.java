package dev.xpolion.xpotriad.visual;

import dev.xpolion.xpotriad.ability.Ability;
import dev.xpolion.xpotriad.ability.AbilityItem;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.FragmentItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * EtchLoom GUI for engraving and deconstructing abilities.
 */
public final class EtchLoom {

    public static final Set<Material> ALLOWED_TARGETS = Set.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.COPPER_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,

            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.COPPER_PICKAXE, Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,

            Material.WOODEN_AXE, Material.STONE_AXE, Material.COPPER_AXE, Material.IRON_AXE,
            Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,

            Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.COPPER_SHOVEL, Material.IRON_SHOVEL,
            Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL,

            Material.WOODEN_HOE, Material.STONE_HOE, Material.COPPER_HOE, Material.IRON_HOE,
            Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE,

            Material.WOODEN_SPEAR, Material.STONE_SPEAR, Material.COPPER_SPEAR, Material.IRON_SPEAR,
            Material.GOLDEN_SPEAR, Material.DIAMOND_SPEAR, Material.NETHERITE_SPEAR,

            Material.MACE,

            Material.BREEZE_ROD, Material.BLAZE_ROD, Material.STICK
    );

    private final ChestGui chestGui;
    private final String tutorialCommand;

    public EtchLoom(Plugin plugin, String tutorialCommand) {
        Objects.requireNonNull(plugin, "plugin");
        this.tutorialCommand = Objects.requireNonNull(tutorialCommand, "tutorialCommand");

        this.chestGui = new ChestGui(plugin, item -> {
            Fragment fragment = FragmentItem.getFragment(item);
            if (fragment != null) {
                return fragment;
            }

            return item;
        });
    }

    public EtchLoom(Plugin plugin) {
        this(plugin, "XpoTriad");
    }

    public void open(Player player) {
        Objects.requireNonNull(player, "player");

        ChestGui.Session session = chestGui.create(
                player,
                Component.text("EtchLoom")
        );

        // Info / description row (row 3)
        session.info(2, 3, createInfoPane(
                Material.CYAN_STAINED_GLASS_PANE,
                Component.text("Target", NamedTextColor.AQUA),
                List.of(
                        Component.text("The item that will receive the Ability.", NamedTextColor.GRAY),
                        Component.text("Only supported weapons/tools can be engraved.", NamedTextColor.GRAY),
                        Component.text("Existing item data is preserved.", NamedTextColor.GRAY)
                )
        ));

        session.info(4, 3, createInfoPane(
                Material.PURPLE_STAINED_GLASS_PANE,
                Component.text("Pre-Cast Fragment", NamedTextColor.LIGHT_PURPLE),
                List.of(
                        Component.text("The Fragment placed below will execute", NamedTextColor.GRAY),
                        Component.text("during the PRE_CAST stage.", NamedTextColor.GRAY)
                )
        ));

        session.info(5, 3, createInfoPane(
                Material.MAGENTA_STAINED_GLASS_PANE,
                Component.text("Cast Fragment", NamedTextColor.LIGHT_PURPLE),
                List.of(
                        Component.text("The Fragment placed below will execute", NamedTextColor.GRAY),
                        Component.text("during the CAST stage.", NamedTextColor.GRAY)
                )
        ));

        session.info(6, 3, createInfoPane(
                Material.RED_STAINED_GLASS_PANE,
                Component.text("Post-Cast Fragment", NamedTextColor.LIGHT_PURPLE),
                List.of(
                        Component.text("The Fragment placed below will execute", NamedTextColor.GRAY),
                        Component.text("during the POST_CAST stage.", NamedTextColor.GRAY)
                )
        ));

        // Input row (row 4)
        session.input(
                "target",
                2,
                4,
                item -> item != null
                        && (
                        ALLOWED_TARGETS.contains(item.getType())
                                || AbilityItem.isAbilityItem(item)
                ),
                this::onTargetChanged
        );

        session.input("pre_cast", 4, 4, Fragment.class);
        session.input("cast", 5, 4, Fragment.class);
        session.input("post_cast", 6, 4, Fragment.class);

        // Engrave button
        session.button(
                8,
                4,
                createNamedItem(
                        Material.LIME_STAINED_GLASS_PANE,
                        Component.text("Engrave", NamedTextColor.GREEN)
                ),
                this::onEngrave
        );

        // Tutorial button
        session.button(
                9,
                6,
                createNamedItem(
                        Material.WRITTEN_BOOK,
                        Component.text("Tutorial", NamedTextColor.GOLD)
                ),
                tutorialCommand
        );

        session.open();
    }

    /**
     * Handles changes to the target input.
     *
     * If an engraved ability item is inserted, the ability is read,
     * the item is deconstructed back into its normal item form,
     * and its fragments are placed into the appropriate input slots.
     */
    private void onTargetChanged(
            ChestGui.Session session,
            ItemStack item
    ) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        if (!AbilityItem.isAbilityItem(item)) {
            return;
        }

        Ability ability = AbilityItem.read(item);

        if (ability == null) {
            return;
        }

        /*
         * Do not deconstruct an ability if the fragment slots are already
         * occupied. This prevents silently replacing or losing fragments.
         */
        if (hasItem(session, "pre_cast")
                || hasItem(session, "cast")
                || hasItem(session, "post_cast")) {
            return;
        }

        ItemStack normalItem = AbilityItem.deconstruct(item);

        if (normalItem == null) {
            return;
        }

        // Restore the normal item in the target slot.
        session.setItem("target", normalItem);

        Fragment pre = ability.getFragment(Ability.Stage.PRE_CAST);
        Fragment cast = ability.getFragment(Ability.Stage.CAST);
        Fragment post = ability.getFragment(Ability.Stage.POST_CAST);

        if (pre != null) {
            session.setItem(
                    "pre_cast",
                    FragmentItem.create(pre)
            );
        }

        if (cast != null) {
            session.setItem(
                    "cast",
                    FragmentItem.create(cast)
            );
        }

        if (post != null) {
            session.setItem(
                    "post_cast",
                    FragmentItem.create(post)
            );
        }
    }

    private boolean hasItem(
            ChestGui.Session session,
            String id
    ) {
        ItemStack item = session.getItem(id);

        return item != null
                && !item.getType().isAir();
    }

    private boolean onEngrave(
            Player player,
            ChestGui.Session session
    ) {
        ItemStack targetItem = session.getItem("target");

        if (targetItem == null || targetItem.getType().isAir()) {
            return false;
        }

        if (!ALLOWED_TARGETS.contains(targetItem.getType())) {
            return false;
        }

        ItemStack preItem = session.getItem("pre_cast");
        ItemStack castItem = session.getItem("cast");
        ItemStack postItem = session.getItem("post_cast");

        Fragment preFragment =
                preItem != null && !preItem.getType().isAir()
                        ? FragmentItem.getFragment(preItem)
                        : null;

        Fragment castFragment =
                castItem != null && !castItem.getType().isAir()
                        ? FragmentItem.getFragment(castItem)
                        : null;

        Fragment postFragment =
                postItem != null && !postItem.getType().isAir()
                        ? FragmentItem.getFragment(postItem)
                        : null;

        // If an item was placed but failed to resolve, fail.
        if (preItem != null
                && !preItem.getType().isAir()
                && preFragment == null) {
            return false;
        }

        if (castItem != null
                && !castItem.getType().isAir()
                && castFragment == null) {
            return false;
        }

        if (postItem != null
                && !postItem.getType().isAir()
                && postFragment == null) {
            return false;
        }

        // Must have at least one fragment.
        if (preFragment == null
                && castFragment == null
                && postFragment == null) {
            return false;
        }

        Ability ability = new Ability();

        if (preFragment != null) {
            ability.setFragment(
                    Ability.Stage.PRE_CAST,
                    preFragment
            );
        }

        if (castFragment != null) {
            ability.setFragment(
                    Ability.Stage.CAST,
                    castFragment
            );
        }

        if (postFragment != null) {
            ability.setFragment(
                    Ability.Stage.POST_CAST,
                    postFragment
            );
        }

        // Consume 1 target item.
        ItemStack engravedTarget = targetItem.clone();
        engravedTarget.setAmount(1);

        if (targetItem.getAmount() > 1) {
            ItemStack extraTarget = targetItem.clone();
            extraTarget.setAmount(targetItem.getAmount() - 1);
            session.returnItem(extraTarget);
        }

        // Consume 1 of each fragment stack.
        if (preItem != null && preItem.getAmount() > 1) {
            ItemStack extra = preItem.clone();
            extra.setAmount(preItem.getAmount() - 1);
            session.returnItem(extra);
        }

        if (castItem != null && castItem.getAmount() > 1) {
            ItemStack extra = castItem.clone();
            extra.setAmount(castItem.getAmount() - 1);
            session.returnItem(extra);
        }

        if (postItem != null && postItem.getAmount() > 1) {
            ItemStack extra = postItem.clone();
            extra.setAmount(postItem.getAmount() - 1);
            session.returnItem(extra);
        }

        AbilityItem.engrave(engravedTarget, ability);
        session.returnItem(engravedTarget);

        return true;
    }

    private static ItemStack createInfoPane(
            Material material,
            Component name,
            List<Component> lore
    ) {
        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(name);
            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private static ItemStack createNamedItem(
            Material material,
            Component name
    ) {
        ItemStack item = new ItemStack(material);

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(name);
            item.setItemMeta(meta);
        }

        return item;
    }
}