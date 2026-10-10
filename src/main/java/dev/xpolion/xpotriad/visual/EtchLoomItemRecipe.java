package dev.xpolion.xpotriad.visual;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Registers the EtchLoom crafting recipe.
 *
 * <pre>
 *   _X_
 *   XBX
 *   _X_
 * </pre>
 *
 * <p>{@code X} = amethyst shard (x4), {@code B} = book (x1). The recipe
 * always consumes four shards, so there is no duplication exploit even
 * though the output is a book and the centre ingredient is a book.
 */
public final class EtchLoomItemRecipe {

    private static final char SHARD = 'X';
    private static final char BOOK = 'B';

    private static NamespacedKey key;

    private EtchLoomItemRecipe() {
    }

    /**
     * (Re)registers the recipe. {@link Bukkit#removeRecipe} runs first so
     * a {@code /reload} cannot crash on a duplicate key.
     */
    public static void register(JavaPlugin plugin) {
        key = new NamespacedKey(plugin, "etch_loom");

        Bukkit.removeRecipe(key);

        ShapedRecipe recipe = new ShapedRecipe(key, EtchLoomItem.create());
        recipe.shape(" X ", "XBX", " X ");
        recipe.setIngredient(SHARD, new RecipeChoice.MaterialChoice(Material.AMETHYST_SHARD));
        recipe.setIngredient(BOOK, new RecipeChoice.MaterialChoice(Material.BOOK));

        Bukkit.addRecipe(recipe);
    }

    /**
     * The recipe key, or {@code null} before {@link #register} ran.
     */
    public static NamespacedKey key() {
        return key;
    }
}
