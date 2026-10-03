package dev.xpolion.xpotriad.visual;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class ChestGui implements Listener {

    public static final int COLUMNS = 9;
    public static final int ROWS = 5;
    public static final int SIZE = COLUMNS * ROWS;

    private static final Material DEFAULT_DECORATION = Material.BLACK_STAINED_GLASS_PANE;

    private final Plugin plugin;
    private final Function<ItemStack, Object> itemResolver;

    public ChestGui(
            Plugin plugin,
            Function<ItemStack, Object> itemResolver
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.itemResolver = Objects.requireNonNull(itemResolver, "itemResolver");

        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public ChestGui(Plugin plugin) {
        this(plugin, item -> item);
    }

    public Session create(Player player, Component title) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(title, "title");

        return new Session(player, title);
    }

    private static int slot(int x, int y) {
        if (x < 1 || x > COLUMNS) {
            throw new IllegalArgumentException("x must be between 1 and 9");
        }

        if (y < 1 || y > ROWS) {
            throw new IllegalArgumentException("y must be between 1 and 5");
        }

        return (y - 1) * COLUMNS + (x - 1);
    }

    private static ItemStack createDefaultDecoration() {
        return new ItemStack(DEFAULT_DECORATION);
    }

    private static ItemStack copy(ItemStack item) {
        return item == null ? null : item.clone();
    }

    private boolean accepts(Input input, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        if (input.acceptedClass() == null) {
            return true;
        }

        Object resolved;

        try {
            resolved = itemResolver.apply(item);
        } catch (RuntimeException ignored) {
            return false;
        }

        return resolved != null && input.acceptedClass().isInstance(resolved);
    }

    private void returnItem(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);

        for (ItemStack remaining : overflow.values()) {
            player.getWorld().dropItemNaturally(
                    player.getLocation(),
                    remaining
            );
        }
    }

    public enum State {
        OPEN,
        PROCESSING,
        CLOSED
    }

    @FunctionalInterface
    public interface ButtonAction {

        /**
         * @return true  = consume inputs and close GUI
         *         false = keep inputs and keep GUI open
         */
        boolean execute(Player player, Session session);
    }

    public final class Session implements InventoryHolder {

        private final Player player;
        private final Inventory inventory;

        private final Map<String, Input> inputs = new LinkedHashMap<>();
        private final Map<Integer, Input> inputsBySlot = new LinkedHashMap<>();
        private final Map<Integer, Button> buttons = new LinkedHashMap<>();

        private State state = State.OPEN;

        private Session(Player player, Component title) {
            this.player = player;

            this.inventory = Bukkit.createInventory(
                    this,
                    SIZE,
                    title
            );

            fillDefaultDecoration();
        }

        private void fillDefaultDecoration() {
            ItemStack pane = createDefaultDecoration();

            for (int i = 0; i < SIZE; i++) {
                inventory.setItem(i, pane.clone());
            }
        }

        public Session input(String id, int x, int y) {
            return input(id, x, y, null);
        }

        public Session input(
                String id,
                int x,
                int y,
                Class<?> acceptedClass
        ) {
            Objects.requireNonNull(id, "id");

            if (inputs.containsKey(id)) {
                throw new IllegalArgumentException(
                        "Input already exists: " + id
                );
            }

            int slot = slot(x, y);

            ensureSlotAvailable(slot);

            Input input = new Input(
                    id,
                    slot,
                    acceptedClass
            );

            inputs.put(id, input);
            inputsBySlot.put(slot, input);

            // Inputs are intentionally empty.
            inventory.setItem(slot, null);

            return this;
        }

        public Session button(
                int x,
                int y,
                ItemStack visual,
                ButtonAction action
        ) {
            Objects.requireNonNull(visual, "visual");
            Objects.requireNonNull(action, "action");

            int slot = slot(x, y);

            ensureSlotAvailable(slot);

            buttons.put(
                    slot,
                    new Button(slot, visual.clone(), action)
            );

            inventory.setItem(slot, visual.clone());

            return this;
        }

        public Session button(
                int x,
                int y,
                ItemStack visual,
                String command
        ) {
            Objects.requireNonNull(command, "command");

            return button(
                    x,
                    y,
                    visual,
                    (player, session) -> {
                        String commandToRun = command;

                        if (commandToRun.startsWith("/")) {
                            commandToRun = commandToRun.substring(1);
                        }

                        return player.performCommand(commandToRun);
                    }
            );
        }

        public Session info(
                int x,
                int y,
                ItemStack visual
        ) {
            Objects.requireNonNull(visual, "visual");

            int slot = slot(x, y);

            ensureSlotAvailable(slot);

            inventory.setItem(slot, visual.clone());

            return this;
        }

        public Session decoration(
                int x,
                int y
        ) {
            return decoration(
                    x,
                    y,
                    createDefaultDecoration()
            );
        }

        public Session decoration(
                int x,
                int y,
                ItemStack visual
        ) {
            Objects.requireNonNull(visual, "visual");

            int slot = slot(x, y);

            ensureSlotAvailable(slot);

            inventory.setItem(slot, visual.clone());

            return this;
        }

        private void ensureSlotAvailable(int slot) {
            if (inputsBySlot.containsKey(slot)) {
                throw new IllegalArgumentException(
                        "Slot is already an input: " + slot
                );
            }

            if (buttons.containsKey(slot)) {
                throw new IllegalArgumentException(
                        "Slot is already a button: " + slot
                );
            }
        }

        public Input getInput(String id) {
            return inputs.get(id);
        }

        public boolean hasInput(String id) {
            return inputs.containsKey(id);
        }

        public Map<String, Input> getInputs() {
            return Map.copyOf(inputs);
        }

        public Player getPlayer() {
            return player;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public State getState() {
            return state;
        }

        public void open() {
            if (state == State.CLOSED) {
                throw new IllegalStateException(
                        "GUI session is already closed"
                );
            }

            player.openInventory(inventory);
        }

        public void close() {
            if (state == State.CLOSED) {
                return;
            }

            state = State.CLOSED;

            returnInputs();

            if (player.getOpenInventory().getTopInventory() == inventory) {
                player.closeInventory();
            }
        }

        private void processButton(Button button) {
            if (state != State.OPEN) {
                return;
            }

            state = State.PROCESSING;

            boolean consume;

            try {
                consume = button.action().execute(player, this);
            } catch (RuntimeException exception) {
                exception.printStackTrace();
                state = State.OPEN;
                return;
            }

            if (!consume) {
                state = State.OPEN;
                return;
            }

            /*
             * Clear the inputs BEFORE closing.
             *
             * This is important because InventoryCloseEvent normally
             * returns remaining input items. A successful action has
             * already consumed them, so there must be nothing left
             * for the close handler to return.
             */
            clearInputs();

            state = State.CLOSED;

            if (player.getOpenInventory().getTopInventory() == inventory) {
                player.closeInventory();
            }
        }

        private void clearInputs() {
            for (Input input : inputs.values()) {
                inventory.setItem(input.slot(), null);
            }
        }

        private void returnInputs() {
            for (Input input : inputs.values()) {
                ItemStack item = inventory.getItem(input.slot());

                if (item == null || item.getType().isAir()) {
                    continue;
                }

                inventory.setItem(input.slot(), null);
                returnItem(player, item);
            }
        }

        private boolean isTopInventory(InventoryClickEvent event) {
            return event.getRawSlot() >= 0
                    && event.getRawSlot() < inventory.getSize();
        }

        private boolean isOurInventory(Inventory inventory) {
            return inventory == this.inventory;
        }

        private boolean isProtectedSlot(int slot) {
            return !inputsBySlot.containsKey(slot);
        }

        private boolean isInputSlot(int slot) {
            return inputsBySlot.containsKey(slot);
        }

        private boolean isValidInputClick(InventoryClickEvent event) {
            if (!isTopInventory(event)) {
                return false;
            }

            return isInputSlot(event.getRawSlot());
        }

        private void handleBottomShiftClick(InventoryClickEvent event) {
            ItemStack source = event.getCurrentItem();

            if (source == null || source.getType().isAir()) {
                return;
            }

            ItemStack remaining = source.clone();

            boolean moved = insertIntoInputs(remaining);

            if (!moved) {
                return;
            }

            event.setCurrentItem(
                    remaining.getAmount() <= 0
                            ? null
                            : remaining
            );
        }

        private boolean insertIntoInputs(ItemStack remaining) {
            boolean moved = false;

            /*
             * First merge into compatible existing input stacks.
             */
            for (Input input : inputs.values()) {
                if (remaining.getAmount() <= 0) {
                    break;
                }

                if (!accepts(input, remaining)) {
                    continue;
                }

                ItemStack current = inventory.getItem(input.slot());

                if (current == null || current.getType().isAir()) {
                    continue;
                }

                if (!current.isSimilar(remaining)) {
                    continue;
                }

                int max = Math.min(
                        current.getMaxStackSize(),
                        inventory.getMaxStackSize()
                );

                int available = max - current.getAmount();

                if (available <= 0) {
                    continue;
                }

                int amount = Math.min(
                        available,
                        remaining.getAmount()
                );

                current.setAmount(current.getAmount() + amount);
                remaining.setAmount(remaining.getAmount() - amount);

                inventory.setItem(input.slot(), current);

                moved = true;
            }

            /*
             * Then use empty input slots.
             */
            for (Input input : inputs.values()) {
                if (remaining.getAmount() <= 0) {
                    break;
                }

                if (!accepts(input, remaining)) {
                    continue;
                }

                ItemStack current = inventory.getItem(input.slot());

                if (current != null && !current.getType().isAir()) {
                    continue;
                }

                int amount = Math.min(
                        remaining.getAmount(),
                        remaining.getMaxStackSize()
                );

                ItemStack inserted = remaining.clone();
                inserted.setAmount(amount);

                remaining.setAmount(
                        remaining.getAmount() - amount
                );

                inventory.setItem(input.slot(), inserted);

                moved = true;
            }

            return moved;
        }

        private boolean isAllowedDrag(InventoryDragEvent event) {
            ItemStack cursor = event.getOldCursor();

            if (cursor == null || cursor.getType().isAir()) {
                return false;
            }

            boolean touchedTop = false;

            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < inventory.getSize()) {
                    touchedTop = true;

                    if (!isInputSlot(rawSlot)) {
                        return false;
                    }

                    Input input = inputsBySlot.get(rawSlot);

                    if (!accepts(input, cursor)) {
                        return false;
                    }
                }
            }

            /*
             * Dragging entirely inside the player's inventory is normal
             * Minecraft behavior and does not need intervention.
             */
            return !touchedTop || true;
        }
    }

    public record Input(
            String id,
            int slot,
            Class<?> acceptedClass
    ) {
    }

    public record Button(
            int slot,
            ItemStack visual,
            ButtonAction action
    ) {
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();

        if (!(top.getHolder() instanceof Session session)) {
            return;
        }

        if (session.getState() == State.CLOSED) {
            event.setCancelled(true);
            return;
        }

        /*
         * Number-key hotbar swaps can bypass ordinary slot handling.
         * Protected GUI slots must never participate in them.
         */
        if (event.getClick() == ClickType.NUMBER_KEY) {
            if (session.isTopInventory(event)
                    || event.getHotbarButton() >= 0) {
                event.setCancelled(true);
            }

            return;
        }

        /*
         * Offhand swap.
         */
        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            event.setCancelled(true);
            return;
        }

        /*
         * Double-click collection can attempt to collect matching
         * items from the entire open inventory.
         */
        if (event.getClick() == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return;
        }

        /*
         * Dropping items while the GUI is open is blocked from the
         * GUI interaction path. Input items can only leave through
         * normal pickup or closing the GUI.
         */
        if (event.getClick() == ClickType.DROP
                || event.getClick() == ClickType.CONTROL_DROP) {
            event.setCancelled(true);
            return;
        }

        /*
         * Creative mode has inventory manipulation actions that do
         * not correspond cleanly to normal survival clicks.
         */
        if (event.getClick() == ClickType.CREATIVE) {
            event.setCancelled(true);
            return;
        }

        if (session.isTopInventory(event)) {
            int slot = event.getRawSlot();

            /*
             * Button.
             */
            Button button = session.buttons.get(slot);

            if (button != null) {
                event.setCancelled(true);

                if (event.getAction() == InventoryAction.NOTHING) {
                    return;
                }

                session.processButton(button);
                return;
            }

            /*
             * Input.
             */
            if (session.isInputSlot(slot)) {
                Input input = session.inputsBySlot.get(slot);

                /*
                 * Normal left/right interaction with an input is
                 * delegated to vanilla Minecraft.
                 *
                 * The accepted-class check is still enforced by
                 * cancelling attempts to place invalid items.
                 */
                if (event.getCursor() != null
                        && !event.getCursor().getType().isAir()
                        && !accepts(input, event.getCursor())
                        && event.getAction() != InventoryAction.PICKUP_ALL
                        && event.getAction() != InventoryAction.PICKUP_HALF
                        && event.getAction() != InventoryAction.PICKUP_ONE
                        && event.getAction() != InventoryAction.PICKUP_SOME) {

                    event.setCancelled(true);
                }

                return;
            }

            /*
             * Everything else in the top inventory is decoration/info.
             */
            event.setCancelled(true);
            return;
        }

        /*
         * Shift-clicking from the player's inventory into the GUI.
         */
        if (event.isShiftClick()) {
            event.setCancelled(true);

            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                session.handleBottomShiftClick(event);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();

        if (!(top.getHolder() instanceof Session session)) {
            return;
        }

        if (session.getState() == State.CLOSED) {
            event.setCancelled(true);
            return;
        }

        /*
         * A drag touching the GUI is allowed only when every affected
         * GUI slot is an input and the dragged item is valid for that
         * input.
         */
        if (!session.isAllowedDrag(event)) {
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < session.inventory.getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onInventoryClose(InventoryCloseEvent event) {
        Inventory top = event.getInventory();

        if (!(top.getHolder() instanceof Session session)) {
            return;
        }

        if (session.state == State.CLOSED) {
            return;
        }

        session.state = State.CLOSED;
        session.returnInputs();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        Inventory top = player.getOpenInventory().getTopInventory();

        if (!(top.getHolder() instanceof Session session)) {
            return;
        }

        if (session.state == State.CLOSED) {
            return;
        }

        session.state = State.CLOSED;
        session.returnInputs();
    }
}
