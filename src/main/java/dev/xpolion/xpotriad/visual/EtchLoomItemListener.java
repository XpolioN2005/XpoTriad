package dev.xpolion.xpotriad.visual;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import static org.bukkit.event.block.Action.RIGHT_CLICK_AIR;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

/**
 * Handles the craftable EtchLoom item.
 *
 * <p>Right-click opens the EtchLoom GUI. Detection uses
 * {@link PlayerInteractEvent#getItem()} so it is scoped per-hand and a
 * single physical click (which fires a HAND event then an OFF_HAND event)
 * cannot double-open the GUI. The interaction is cancelled so that, for
 * example, right-clicking a chest with the key does not also open the
 * chest.
 *
 * <p>On join the recipe is discovered for every player so it shows up in
 * the recipe book without having to be found first.
 */
public final class EtchLoomItemListener implements Listener {

    private final EtchLoom etchLoom;

    public EtchLoomItemListener(EtchLoom etchLoom) {
        this.etchLoom = etchLoom;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();

        if (action != RIGHT_CLICK_AIR && action != RIGHT_CLICK_BLOCK) {
            return;
        }

        // Respect protection plugins that denied item use.
        if (event.useItemInHand() == Event.Result.DENY) {
            return;
        }

        if (!EtchLoomItem.isEtchLoomItem(event.getItem())) {
            return;
        }

        Player player = event.getPlayer();
        etchLoom.open(player);

        // Stop vanilla interaction (e.g. opening a chest) alongside the GUI.
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        NamespacedKey recipeKey = EtchLoomItemRecipe.key();

        if (recipeKey != null && !player.hasDiscoveredRecipe(recipeKey)) {
            player.discoverRecipe(recipeKey);
        }
    }
}
