package dev.xpolion.xpotriad.ability;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Handles ability activation.
 *
 * ALL abilities (MELEE and RANGED) activate exclusively via right-click.
 * Cooldown is item-specific and checked/written on the physical ItemStack PDC.
 */
public final class AbilityListener implements Listener {

    private final AbilityEngine abilityEngine;

    public AbilityListener(AbilityEngine abilityEngine) {
        this.abilityEngine = abilityEngine;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Enforce main hand only to avoid duplicate activation from off-hand
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!AbilityItem.isAbilityItem(item)) {
            return;
        }

        Ability ability = AbilityItem.read(item);
        if (ability == null) {
            return;
        }

        event.setCancelled(true);

        // Check item-specific cooldown
        if (AbilityItem.isOnCooldown(item)) {
            long remainingTicks = AbilityItem.getRemainingCooldownTicks(item);
            double remainingSeconds = remainingTicks / 20.0;
            ((Audience) player).sendMessage(Component.text(
                    String.format("Ability is on cooldown! (%.1fs remaining)", remainingSeconds),
                    NamedTextColor.RED
            ));
            return;
        }

        // Calculate and apply item cooldown directly to the held ItemStack
        long cooldownTicks = ability.calculateCooldown();
        AbilityItem.applyCooldown(item, cooldownTicks);

        AbilityContext context = new AbilityContext(player, ability);
        abilityEngine.execute(context);
    }
}
