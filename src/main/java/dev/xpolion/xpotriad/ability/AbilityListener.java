package dev.xpolion.xpotriad.ability;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.audience.Audience;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Locale;

import static org.bukkit.event.block.Action.RIGHT_CLICK_AIR;
import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

/**
 * Handles ability activation.
 *
 * <p>Right-click attempts to activate an ability.
 *
 * <p>Priority:
 * <ol>
 *   <li>Off-hand</li>
 *   <li>Main-hand</li>
 * </ol>
 *
 * <p>If the off-hand ability is on cooldown, the main-hand ability still
 * gets a chance to activate. The vanilla interaction is only cancelled when
 * an ability actually executes.
 *
 * <p>An activation stays EXECUTING until the completion hook writes the
 * deferred cooldown, so a second click during the sequence never fires
 * again. An executing flag older than 500 ticks is a dead sequence: it is
 * discarded and the click proceeds normally.
 *
 * <p>Action bar feedback:
 * <pre>
 *   &lt; 2.0s | Fired &gt;
 *   &lt; Fired | 1.5s &gt;
 *   &lt; Fired | Executing &gt;
 *   &lt; Fired | Ready &gt;
 * </pre>
 */
public final class AbilityListener implements Listener {

    private final AbilityEngine abilityEngine;

    public AbilityListener(AbilityEngine abilityEngine) {
        this.abilityEngine = abilityEngine;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();

        // Abilities are activated exclusively with right-click.
        if (action != RIGHT_CLICK_AIR && action != RIGHT_CLICK_BLOCK) {
            return;
        }

        // Respect protection plugins that denied item use.
        if (event.useItemInHand() == Event.Result.DENY) {
            return;
        }

        EquipmentSlot eventHand = event.getHand();
        if (eventHand == null) {
            return;
        }

        Player player = event.getPlayer();
        PlayerInventory inventory = player.getInventory();

        ItemStack mainHand = inventory.getItemInMainHand();
        ItemStack offHand = inventory.getItemInOffHand();

        if (!shouldProcess(eventHand, action, mainHand)) {
            return;
        }

        /*
         * Priority: OFF_HAND -> MAIN_HAND.
         *
         * A cooldown does not consume priority. If the off-hand ability is
         * on cooldown, the main-hand ability can still fire.
         */
        ActivationResult offHandResult = tryActivate(player, offHand);

        if (offHandResult.isFired()) {
            event.setCancelled(true);
            sendFeedback(player, offHandResult, getState(mainHand));
            return;
        }

        ActivationResult mainHandResult = tryActivate(player, mainHand);

        if (mainHandResult.isFired()) {
            event.setCancelled(true);
            sendFeedback(player, offHandResult, mainHandResult);
            return;
        }

        // Nothing fired: show cooldowns and leave vanilla interaction intact.
        sendFeedback(player, offHandResult, mainHandResult);
    }

    /**
     * Decides whether this interact event should be processed.
     *
     * <p>A single physical right-click can produce a HAND event followed by
     * an OFF_HAND event. Processing both would risk a double activation, so
     * the HAND event is the canonical one.
     *
     * <p>The exception: right-clicking air with an empty main hand produces
     * only an OFF_HAND event, so that one must be handled.
     */
    private boolean shouldProcess(
            EquipmentSlot eventHand,
            Action action,
            ItemStack mainHand
    ) {
        if (eventHand == EquipmentSlot.HAND) {
            return true;
        }

        return eventHand == EquipmentSlot.OFF_HAND
                && action == RIGHT_CLICK_AIR
                && mainHand.isEmpty();
    }

    /**
     * Attempts to activate the ability on the given item.
     *
     * <p>The cooldown is deferred: it is applied only after every fragment in
     * the ability has executed (via the engine's completion hook), so the
     * countdown does not start until the full sequence finishes. The stack is
     * re-resolved by its ability id at completion, so moving/ swapping the item
     * mid-sequence still lands the cooldown on the correct slot.
     *
     * @return the resulting state: EMPTY, READY, EXECUTING, COOLDOWN or FIRED
     */
    private ActivationResult tryActivate(Player player, ItemStack item) {
        ActivationResult state = getState(item);

        if (state.state() != State.READY) {
            return state;
        }

        Ability ability = AbilityItem.read(item);

        if (ability == null) {
            return ActivationResult.empty();
        }

        long cooldownTicks = ability.calculateCooldown();
        String abilityId = AbilityItem.readAbilityId(item);

        // Flag the item BEFORE the sequence starts: any click arriving while
        // the sequence is running sees EXECUTING and cannot fire again.
        AbilityItem.markExecuting(item);

        Runnable applyDeferredCooldown = () -> {
            try {
                if (!player.isOnline()) {
                    return; // Offline at completion — nothing to write.
                }
                ItemStack current = AbilityItem.findByAbilityId(player, abilityId);
                if (current != null) {
                    AbilityItem.applyCooldown(current, cooldownTicks);
                    player.updateInventory();
                }
            } finally {
                // Always release the executing flag. Re-resolve the stack the
                // same way the cooldown does; if it is gone, the 500-tick
                // failsafe clears the flag later.
                ItemStack flagged = player.isOnline()
                        ? AbilityItem.findByAbilityId(player, abilityId)
                        : item;
                if (flagged != null) {
                    AbilityItem.clearExecuting(flagged);
                }
            }
        };

        abilityEngine.execute(new AbilityContext(player, ability), applyDeferredCooldown);

        return ActivationResult.fired();
    }

    /**
     * Reads the current state of an ability item without activating it.
     *
     * <p>Priority: executing flag, then cooldown, then READY. A stale
     * executing flag (its sequence died before the completion hook ran) was
     * already discarded by {@link AbilityItem#executingState}, so this falls
     * through and the click starts a fresh sequence.</p>
     */
    private ActivationResult getState(ItemStack item) {
        if (!AbilityItem.isAbilityItem(item)) {
            return ActivationResult.empty();
        }

        if (AbilityItem.read(item) == null) {
            return ActivationResult.empty();
        }

        if (AbilityItem.executingState(item) == AbilityItem.ExecutingState.EXECUTING) {
            return ActivationResult.executing();
        }

        if (AbilityItem.isOnCooldown(item)) {
            return ActivationResult.cooldown(
                    AbilityItem.getRemainingCooldownTicks(item)
            );
        }

        return ActivationResult.ready();
    }

    /**
     * Builds the compact action bar.
     *
     * <p>Off-hand is represented by "&lt;", main-hand by "&gt;".
     * Only hands containing an ability item are rendered.
     */
    private void sendFeedback(
            Player player,
            ActivationResult offHand,
            ActivationResult mainHand
    ) {
        boolean hasOffHand = offHand.present();
        boolean hasMainHand = mainHand.present();

        if (!hasOffHand && !hasMainHand) {
            return;
        }

        Component message = Component.empty();

        if (hasOffHand) {
            message = message
                    .append(Component.text("< ", NamedTextColor.GRAY))
                    .append(offHand.toComponent());
        }

        if (hasOffHand && hasMainHand) {
            message = message.append(
                    Component.text(" | ", NamedTextColor.GRAY)
            );
        }

        if (hasMainHand) {
            message = message
                    .append(mainHand.toComponent())
                    .append(Component.text(" >", NamedTextColor.GRAY));
        }
        
        Audience audience = player;
        audience.sendActionBar(message);
    }

    private record ActivationResult(
            State state,
            long remainingTicks
    ) {

        static ActivationResult empty() {
            return new ActivationResult(State.EMPTY, 0);
        }

        static ActivationResult ready() {
            return new ActivationResult(State.READY, 0);
        }

        static ActivationResult fired() {
            return new ActivationResult(State.FIRED, 0);
        }

        static ActivationResult executing() {
            return new ActivationResult(State.EXECUTING, 0);
        }

        static ActivationResult cooldown(long remainingTicks) {
            return new ActivationResult(State.COOLDOWN, remainingTicks);
        }

        boolean isFired() {
            return state == State.FIRED;
        }

        boolean present() {
            return state != State.EMPTY;
        }

        Component toComponent() {
            return switch (state) {
                case FIRED ->
                        Component.text("Fired", NamedTextColor.GREEN);

                case COOLDOWN ->
                        Component.text(
                                String.format(
                                        Locale.ROOT,
                                        "%.1fs",
                                        remainingTicks / 20.0
                                ),
                                NamedTextColor.RED
                        );

                case EXECUTING ->
                        Component.text("Executing", NamedTextColor.YELLOW);

                case READY ->
                        Component.text("Ready", NamedTextColor.YELLOW);

                case EMPTY ->
                        Component.empty();
            };
        }
    }

    private enum State {
        EMPTY,
        READY,
        EXECUTING,
        COOLDOWN,
        FIRED
    }
}