package dev.xpolion.xpotriad.command;

import dev.xpolion.xpotriad.Main;
import dev.xpolion.xpotriad.visual.TutorialBook;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Unified /xpt dispatcher replacing the old xptest / xpbind commands.
 *
 * <pre>
 *   /xpt reload   — re-read config.yml (permission: xpotriad.admin)
 *   /xpt book     — open the tutorial book (no item is given)
 *   /xpt help     — usage overview
 * </pre>
 */
public final class XptCommand implements TabExecutor {

    private static final String PERMISSION_RELOAD = "xpotriad.admin";

    private static final List<String> SUBCOMMANDS = List.of("reload", "book", "help");

    private final Main plugin;

    public XptCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> handleReload(sender);
            case "book" -> handleBook(sender);
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

    /**
     * Opens the tutorial book in place - the player receives no item.
     */
    private void handleBook(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            message(sender, Component.text("Players only.", NamedTextColor.RED));
            return;
        }

        TutorialBook.open(player);
    }

    private void sendHelp(CommandSender sender) {
        message(sender, Component.text("=== XpoTriad ===", NamedTextColor.GOLD));
        message(sender, Component.text("/xpt reload", NamedTextColor.YELLOW)
                .append(Component.text(" — reload config.yml (admin)", NamedTextColor.GRAY)));
        message(sender, Component.text("/xpt book", NamedTextColor.YELLOW)
                .append(Component.text(" — open the tutorial book", NamedTextColor.GRAY)));
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