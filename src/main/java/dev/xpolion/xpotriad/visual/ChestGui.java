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
import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class ChestGui implements Listener {

    public static final int COLUMNS = 9;
    public static final int ROWS = 5;
    public static final int SIZE = COLUMNS * ROWS;

    private static final Material DEFAULT_DECORATION =
            Material.BLACK_STAINED_GLASS_PANE;

    private final Plugin plugin;
    private final Function<ItemStack, Object> itemResolver;

    public ChestGui(
            Plugin plugin,
            Function<ItemStack, Object> itemResolver
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.itemResolver =
                Objects.requireNonNull(itemResolver, "itemResolver");

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

    private boolean accepts(Input input, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        if (input.filter() != null && !input.filter().test(item)) {
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

        return resolved != null
                && input.acceptedClass().isInstance(resolved);
    }

    private void returnItem(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        Map<Integer, ItemStack> overflow =
                player.getInventory().addItem(item);

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
        private final Map<Integer, Input> inputsBySlot =
                new LinkedHashMap<>();
        private final Map<Integer, Button> buttons =
                new LinkedHashMap<>();

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
            return input(id, x, y, (Class<?>) null);
        }

        public Session input(
                String id,
                int x,
                int y,
                Class<?> acceptedClass
        ) {
            return input(id, x, y, acceptedClass, null);
        }

        public Session input(
                String id,
                int x,
                int y,
                Predicate<ItemStack> filter
        ) {
            return input(id, x, y, null, filter);
        }

        public Session input(
                String id,
                int x,
                int y,
                Predicate<ItemStack> filter,
                BiConsumer<Session, ItemStack> onChange
        ) {
            return input(
                    id,
                    x,
                    y,
                    null,
                    filter,
                    onChange
            );
        }

        public Session input(
                String id,
                int x,
                int y,
                Class<?> acceptedClass,
                Predicate<ItemStack> filter
        ) {
            return input(
                    id,
                    x,
                    y,
                    acceptedClass,
                    filter,
                    null
            );
        }

        public Session input(
                String id,
                int x,
                int y,
                Class<?> acceptedClass,
                Predicate<ItemStack> filter,
                BiConsumer<Session, ItemStack> onChange
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
                    acceptedClass,
                    filter,
                    onChange
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
                            commandToRun =
                                    commandToRun.substring(1);
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

        public ItemStack getItem(String id) {
            Input input = inputs.get(id);

            return input != null
                    ? inventory.getItem(input.slot())
                    : null;
        }

        public void setItem(String id, ItemStack item) {
            Input input = inputs.get(id);

            if (input == null) {
                throw new IllegalArgumentException(
                        "Unknown input: " + id
                );
            }

            if (item != null && !item.getType().isAir()
                    && !accepts(input, item)) {
                throw new IllegalArgumentException(
                        "Item is not accepted by input: " + id
                );
            }

            inventory.setItem(
                    input.slot(),
                    item == null ? null : item.clone()
            );
        }

        public void returnItem(ItemStack item) {
            ChestGui.this.returnItem(player, item);
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

            if (player.getOpenInventory().getTopInventory()
                    == inventory) {
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

            clearInputs();

            state = State.CLOSED;

            if (player.getOpenInventory().getTopInventory()
                    == inventory) {
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
                ItemStack item =
                        inventory.getItem(input.slot());

                if (item == null || item.getType().isAir()) {
                    continue;
                }

                inventory.setItem(input.slot(), null);
                ChestGui.this.returnItem(player, item);
            }
        }

        private void notifyInputChanged(Input input) {
            if (input.onChange() == null) {
                return;
            }

            ItemStack item = inventory.getItem(input.slot());

            input.onChange().accept(
                    this,
                    item == null ? null : item.clone()
            );
        }

        private void notifyInputChangedNextTick(Input input) {
            if (input.onChange() == null) {
                return;
            }

            Bukkit.getScheduler().runTask(
                    plugin,
                    () -> {
                        if (state != State.OPEN) {
                            return;
                        }

                        notifyInputChanged(input);
                    }
            );
        }

        private void notifyAllChangedInputsNextTick(
                Iterable<Integer> slots
        ) {
            for (int rawSlot : slots) {
                if (rawSlot < 0
                        || rawSlot >= inventory.getSize()) {
                    continue;
                }

                Input input = inputsBySlot.get(rawSlot);

                if (input != null) {
                    notifyInputChangedNextTick(input);
                }
            }
        }

        private boolean isTopInventory(
                InventoryClickEvent event
        ) {
            return event.getRawSlot() >= 0
                    && event.getRawSlot() < inventory.getSize();
        }

        private boolean isInputSlot(int slot) {
            return inputsBySlot.containsKey(slot);
        }

        private void handleBottomShiftClick(
                InventoryClickEvent event
        ) {
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

        private boolean insertIntoInputs(
                ItemStack remaining
        ) {
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

                ItemStack current =
                        inventory.getItem(input.slot());

                if (current == null
                        || current.getType().isAir()) {
                    continue;
                }

                if (!current.isSimilar(remaining)) {
                    continue;
                }

                int max = Math.min(
                        current.getMaxStackSize(),
                        inventory.getMaxStackSize()
                );

                int available =
                        max - current.getAmount();

                if (available <= 0) {
                    continue;
                }

                int amount = Math.min(
                        available,
                        remaining.getAmount()
                );

                current.setAmount(
                        current.getAmount() + amount
                );

                remaining.setAmount(
                        remaining.getAmount() - amount
                );

                inventory.setItem(
                        input.slot(),
                        current
                );

                notifyInputChanged(input);

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

                ItemStack current =
                        inventory.getItem(input.slot());

                if (current != null
                        && !current.getType().isAir()) {
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

                inventory.setItem(
                        input.slot(),
                        inserted
                );

                notifyInputChanged(input);

                moved = true;
            }

            return moved;
        }

        private boolean isAllowedDrag(
                InventoryDragEvent event
        ) {
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

                    Input input =
                            inputsBySlot.get(rawSlot);

                    if (!accepts(input, cursor)) {
                        return false;
                    }
                }
            }

            return !touchedTop || true;
        }
    }

    public record Input(
            String id,
            int slot,
            Class<?> acceptedClass,
            Predicate<ItemStack> filter,
            BiConsumer<Session, ItemStack> onChange
    ) {
        public Input(
                String id,
                int slot,
                Class<?> acceptedClass
        ) {
            this(
                    id,
                    slot,
                    acceptedClass,
                    null,
                    null
            );
        }

        public Input(
                String id,
                int slot,
                Class<?> acceptedClass,
                Predicate<ItemStack> filter
        ) {
            this(
                    id,
                    slot,
                    acceptedClass,
                    filter,
                    null
            );
        }
    }

    public record Button(
            int slot,
            ItemStack visual,
            ButtonAction action
    ) {
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    private void onInventoryClick(InventoryClickEvent event) {
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
         * Number-key hotbar swaps.
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
         * Double-click collection.
         */
        if (event.getClick() == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return;
        }

        /*
         * Item dropping.
         */
        if (event.getClick() == ClickType.DROP
                || event.getClick() == ClickType.CONTROL_DROP) {
            event.setCancelled(true);
            return;
        }

        /*
         * Creative inventory manipulation.
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
                session.processButton(button);
                return;
            }

            /*
             * Input.
             */
            if (session.isInputSlot(slot)) {
                Input input =
                        session.inputsBySlot.get(slot);

                if (event.getCursor() != null
                        && !event.getCursor().getType().isAir()
                        && !accepts(
                                input,
                                event.getCursor()
                        )
                        && event.getAction()
                        != InventoryAction.PICKUP_ALL
                        && event.getAction()
                        != InventoryAction.PICKUP_HALF
                        && event.getAction()
                        != InventoryAction.PICKUP_ONE
                        && event.getAction()
                        != InventoryAction.PICKUP_SOME) {

                    event.setCancelled(true);
                    return;
                }

                /*
                 * Vanilla will modify the inventory after this
                 * event. Check the changed input on the next tick.
                 */
                session.notifyInputChangedNextTick(input);
                return;
            }

            /*
             * Everything else in the top inventory is protected.
             */
            event.setCancelled(true);
            return;
        }

        /*
         * Shift-clicking from the player's inventory.
         */
        if (event.isShiftClick()) {
            event.setCancelled(true);

            if (event.getAction()
                    == InventoryAction.MOVE_TO_OTHER_INVENTORY) {

                session.handleBottomShiftClick(event);
            }
        }
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
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

        if (!session.isAllowedDrag(event)) {
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < session.inventory.getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        /*
         * Vanilla applies the drag after this event.
         */
        session.notifyAllChangedInputsNextTick(
                event.getRawSlots()
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onInventoryClose(
            InventoryCloseEvent event
    ) {
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

        Inventory top =
                player.getOpenInventory().getTopInventory();

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
