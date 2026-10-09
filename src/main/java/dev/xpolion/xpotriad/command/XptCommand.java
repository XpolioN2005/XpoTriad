package dev.xpolion.xpotriad.command;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.FragmentItem;
import dev.xpolion.xpotriad.fragment.FragmentRegistry;
import dev.xpolion.xpotriad.visual.EtchLoom;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Unified /xpt dispatcher replacing the old xptest / xpbind commands.
 *
 * <pre>
 *   /xpt reload   — re-read config.yml (permission: xpotriad.admin)
 *   /xpt test     — chest(s) with every registered fragment
 *   /xpt bind     — open the EtchLoom GUI
 *   /xpt help     — usage overview
 * </pre>
 */
public final class XptCommand implements TabExecutor {

    private static final String PERMISSION_RELOAD = "xpotriad.admin";

    private static final List<String> SUBCOMMANDS = List.of("reload", "test", "bind", "help");

    private final Main plugin;
    private final EtchLoom etchLoom;

    public XptCommand(Main plugin, EtchLoom etchLoom) {
        this.plugin = plugin;
        this.etchLoom = etchLoom;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> handleReload(sender);
            case "test" -> handleTest(sender);
            case "bind" -> handleBind(sender);
            case "help" -> sendHelp(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    /**
     * Adventure messaging route — CommandSender's legacy Spigot hierarchy
     * references classes absent from lib/, so always send via Audience.
     */
    private static void message(CommandSender sender, Component component) {
        Audience audience = sender;
        audience.sendMessage(component);
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission(PERMISSION_RELOAD)) {
            message(sender, Component.text(
                    "You need the " + PERMISSION_RELOAD + " permission.",
                    NamedTextColor.RED
            ));
            return;
        }

        plugin.reloadBalanceConfig();
        message(sender, Component.text("[XpoTriad] config.yml reloaded.", NamedTextColor.GREEN));
    }

    private void handleTest(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            message(sender, Component.text("Players only.", NamedTextColor.RED));
            return;
        }

        // Chest(s) full of every registered fragment — nothing else is given.
        // A single chest holds 27 slots; additional chests cover any overflow.
        List<ItemStack> fragmentItems = new ArrayList<>();

        for (Fragment fragment : FragmentRegistry.all()) {
            fragmentItems.add(FragmentItem.create(fragment));
        }

        int chestCount = (fragmentItems.size() + 26) / 27;

        for (int chestIndex = 0; chestIndex < chestCount; chestIndex++) {
            ItemStack chestItem = new ItemStack(Material.CHEST);
            BlockStateMeta meta = (BlockStateMeta) chestItem.getItemMeta();

            if (meta != null) {
                Chest chest = (Chest) meta.getBlockState();

                int from = chestIndex * 27;
                int to = Math.min(from + 27, fragmentItems.size());

                for (int i = from; i < to; i++) {
                    chest.getInventory().addItem(fragmentItems.get(i));
                }

                meta.setBlockState(chest);
                chestItem.setItemMeta(meta);
            }

            player.getInventory().addItem(chestItem);
        }
    }

    private void handleBind(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            message(sender, Component.text("Players only.", NamedTextColor.RED));
            return;
        }

        etchLoom.open(player);
    }

    private void sendHelp(CommandSender sender) {
        message(sender, Component.text("=== XpoTriad ===", NamedTextColor.GOLD));
        message(sender, Component.text("/xpt reload", NamedTextColor.YELLOW)
                .append(Component.text(" — reload config.yml (admin)", NamedTextColor.GRAY)));
        message(sender, Component.text("/xpt test", NamedTextColor.YELLOW)
                .append(Component.text(" — chest(s) with all fragments", NamedTextColor.GRAY)));
        message(sender, Component.text("/xpt bind", NamedTextColor.YELLOW)
                .append(Component.text(" — open the EtchLoom GUI", NamedTextColor.GRAY)));
        message(sender, Component.text("/xpt help", NamedTextColor.YELLOW)
                .append(Component.text(" — this overview", NamedTextColor.GRAY)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> matches = new ArrayList<>();

            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(prefix) && (!sub.equals("reload") || sender.hasPermission(PERMISSION_RELOAD))) {
                    matches.add(sub);
                }
            }

            return matches;
        }

        return List.of();
    }
}