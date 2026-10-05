package dev.xpolion.xpotriad.ability;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles ability activation.
 *
 * Right-click -> ability item in the main hand.
 * Left-click  -> ability item in the off hand.
 *
 * Each click is its own event, so both hands can fire independently,
 * but a short global cast lock keeps their ordering deterministic.
 * Cooldown is item-specific and checked/written on the physical ItemStack PDC.
 * The vanilla interaction is only cancelled when an ability actually executes.
 */
public final class AbilityListener implements Listener {

    /** Global per-player delay between casts (3 ticks = 150ms). */
    private static final long CAST_LOCK_TICKS = 3;

    private final AbilityEngine abilityEngine;

    /** Server tick of each player's last successful cast. */
    private final Map<UUID, Integer> lastCastTick = new HashMap<>();

    public AbilityListener(AbilityEngine abilityEngine) {
        this.abilityEngine = abilityEngine;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Enforce main hand only: right-click fires HAND then OFF_HAND, so ignoring
        // OFF_HAND avoids duplicate activation. Left-click only ever fires for HAND.
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        PlayerInventory inv = player.getInventory();

        // Right-click -> main hand item, left-click -> off hand item
        ItemStack item = switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> inv.getItemInMainHand();
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> inv.getItemInOffHand();
            default -> null;
        };

        // Only cancel the vanilla interaction if the ability actually executed
        if (item != null && tryActivate(player, item)) {
            event.setCancelled(true);
        }
    }

    /**
     * @return true if the ability executed (and the vanilla interaction should be cancelled)
     */
    private boolean tryActivate(Player player, ItemStack item) {
        if (!AbilityItem.isAbilityItem(item)) {
            return false;
        }

        Ability ability = AbilityItem.read(item);
        if (ability == null) {
            return false;
        }

        // Item-specific cooldown: notify, but let the vanilla interaction go through
        if (AbilityItem.isOnCooldown(item)) {
            long remainingTicks = AbilityItem.getRemainingCooldownTicks(item);
            double remainingSeconds = remainingTicks / 20.0;
            ((Audience) player).sendMessage(Component.text(
                    String.format("Ability is on cooldown! (%.1fs remaining)", remainingSeconds),
                    NamedTextColor.RED
            ));
            return false;
        }

        // Global cast lock: silent, so rapid double-clicks don't spam messages
        int now = Bukkit.getCurrentTick();
        Integer last = lastCastTick.get(player.getUniqueId());
        if (last != null && now - last < CAST_LOCK_TICKS) {
            return false;
        }

        // Record the cast only on success so blocked clicks don't extend the lock
        lastCastTick.put(player.getUniqueId(), now);

        // Calculate and apply item cooldown directly to the held ItemStack
        long cooldownTicks = ability.calculateCooldown();
        AbilityItem.applyCooldown(item, cooldownTicks);

        abilityEngine.execute(new AbilityContext(player, ability));
        return true;
    }

    /** Clean up so the map doesn't leak entries for offline players. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastCastTick.remove(event.getPlayer().getUniqueId());
    }
}