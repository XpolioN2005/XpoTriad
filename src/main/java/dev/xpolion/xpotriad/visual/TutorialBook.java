package dev.xpolion.xpotriad.visual;

import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.fragment.FragmentRegistry;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Builds the tutorial book from {@link TutorialContent} and the live
 * fragment registry.
 *
 * <p>Pages:
 * <ol>
 *   <li>cover (title + subtitle)</li>
 *   <li>what is an Ability</li>
 *   <li>what is a Fragment</li>
 *   <li>how to use a Fragment</li>
 *   <li>the craftable EtchLoom item</li>
 *   <li>rarity key</li>
 *   <li>..<i>n</i> one page per fragment (sorted by rarity, then name)</li>
 *   <li>last page quick reference</li>
 * </ol>
 *
 * <p>The book is opened in place with {@link #open(Player)} - the player
 * never receives an item.
 *
 * <p>All colours come from {@link BookPalette}, which exists because a
 * book page is light parchment: the item rarity colours are tuned for
 * dark tooltips and would be unreadable here.
 */
public final class TutorialBook {

    /** Shortest separator rule, so tiny headings still get a visible line. */
    private static final int MIN_RULE_LENGTH = 11;

    private TutorialBook() {
    }

    /** Opens the book UI for the player. No item is added to the inventory. */
    public static void open(Player player) {
        player.openBook(create());
    }

    /** Builds the full book item. */
    public static ItemStack create() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();

        if (meta == null) {
            throw new IllegalStateException("WRITTEN_BOOK does not support BookMeta");
        }

        meta.title(Component.text(
                TutorialContent.TITLE,
                BookPalette.ACCENT,
                TextDecoration.BOLD
        ));
        meta.author(Component.text("XpoTriad", BookPalette.MUTED));

        List<Component> pages = new ArrayList<>();

        pages.add(coverPage());
        pages.add(sectionPage(TutorialContent.ABILITIES_HEADING, TutorialContent.ABILITIES_LINES));
        pages.add(sectionPage(TutorialContent.FRAGMENTS_INFO_HEADING, TutorialContent.FRAGMENTS_INFO_LINES));
        pages.add(sectionPage(TutorialContent.FRAGMENTS_USE_HEADING, TutorialContent.FRAGMENTS_USE_LINES));
        pages.add(sectionPage(TutorialContent.ETCHLOOM_ITEM_HEADING, TutorialContent.ETCHLOOM_ITEM_LINES));
        pages.add(rarityPage());

        for (Fragment fragment : sortedFragments()) {
            pages.add(fragmentPage(fragment));
        }

        pages.add(sectionPage(TutorialContent.QUICKREF_HEADING, TutorialContent.QUICKREF_LINES));

        meta.pages(pages);
        book.setItemMeta(meta);

        return book;
    }

    // ------------------------------------------------------------------
    // Pages
    // ------------------------------------------------------------------

    private static Component coverPage() {
        List<Component> lines = new ArrayList<>();

        lines.add(heading(TutorialContent.TITLE, BookPalette.ACCENT));
        lines.add(blank());
        lines.add(centered(
                Component.text(TutorialContent.SUBTITLE, BookPalette.MUTED)
                        .decorate(TextDecoration.ITALIC)
        ));

        // Optional lines: an empty constant in TutorialContent removes the
        // line entirely instead of leaving a stray gap on the page.
        if (!TutorialContent.COVER_TAGLINE.isBlank()) {
            lines.add(blank());
            lines.add(centered(Component.text(
                    TutorialContent.COVER_TAGLINE,
                    BookPalette.BODY
            )));
        }

        if (!TutorialContent.COVER_HOWTO.isBlank()) {
            lines.add(blank());
            lines.add(blank());
            lines.add(centered(Component.text(
                    TutorialContent.COVER_HOWTO,
                    BookPalette.MUTED
            )));
        }

        return join(lines);
    }

    /** Heading + prose page (abilities, fragments, quick reference). */
    private static Component sectionPage(String heading, List<String> body) {
        List<Component> lines = new ArrayList<>();

        lines.add(heading(heading, BookPalette.HEADING));
        lines.add(rule(heading, BookPalette.RULE));
        lines.add(blank());

        for (String line : body) {
            lines.add(parse(line));
        }

        return join(lines);
    }

    /**
     * Rarity legend. Tier rows are generated from the enum so the colours
     * can never disagree with {@link BookPalette#rarityColor}, and tier
     * names are column-aligned by pixel width.
     */
    private static Component rarityPage() {
        List<Component> lines = new ArrayList<>();

        String heading = TutorialContent.RARITY_HEADING;
        lines.add(heading(heading, BookPalette.HEADING));
        lines.add(rule(heading, BookPalette.RULE));
        lines.add(blank());

        Fragment.Rarity[] rarities = Fragment.Rarity.values();

        // Column offset = width of the longest tier name, plus a space.
        int column = 0;

        for (Fragment.Rarity rarity : rarities) {
            column = Math.max(column, BookPalette.pixelWidth(rarity.name()));
        }

        column += BookPalette.SPACE_WIDTH;

        for (int i = 0; i < rarities.length; i++) {
            Fragment.Rarity rarity = rarities[i];
            String note = i < TutorialContent.RARITY_NOTES.length
                    ? TutorialContent.RARITY_NOTES[i]
                    : "";

            lines.add(
                    Component.text(
                            BookPalette.padToColumn(rarity.name(), column),
                            BookPalette.rarityColor(rarity)
                    ).append(Component.text(note, BookPalette.BODY))
            );
        }

        for (String line : TutorialContent.RARITY_LINES) {
            lines.add(parse(line));
        }

        return join(lines);
    }

    /**
     * One page per fragment: name in the book rarity colour, the
     * fragment's own lore, then classification/timing rows using the same
     * labels and tick formatting as the physical FragmentItem lore.
     */
    private static Component fragmentPage(Fragment fragment) {
        List<Component> lines = new ArrayList<>();

        String name = fragment.getName();
        TextColor rarityColor = BookPalette.rarityColor(fragment.getRarity());

        lines.add(Component.text(name, rarityColor).decorate(TextDecoration.BOLD));
        lines.add(rule(name, rarityColor));
        lines.add(blank());

        for (Component loreLine : fragment.getLore()) {
            lines.add(recolor(loreLine, BookPalette.BODY));
        }

        lines.add(blank());

        lines.add(statRow(
                TutorialContent.FRAGMENT_RARITY_LABEL,
                Component.text(
                        fragment.getRarity().name(),
                        rarityColor
                )
        ));
        lines.add(statRow(
                TutorialContent.FRAGMENT_TYPE_LABEL,
                Component.text(
                        fragment.getType() == Fragment.Type.MELEE
                                ? TutorialContent.FRAGMENT_TYPE_MELEE
                                : TutorialContent.FRAGMENT_TYPE_RANGED,
                        BookPalette.BODY
                )
        ));
        lines.add(statRow(
                TutorialContent.FRAGMENT_EXEC_LABEL,
                Component.text(
                        formatTicks(fragment.getExecutionTime()),
                        BookPalette.BODY
                )
        ));
        lines.add(statRow(
                TutorialContent.FRAGMENT_COOLDOWN_LABEL,
                Component.text(
                        formatModifier(fragment.getCooldownModifier()),
                        BookPalette.BODY
                )
        ));

        return join(lines);
    }

    // ------------------------------------------------------------------
    // Registry access
    // ------------------------------------------------------------------

    /**
     * FragmentRegistry.all() is backed by a HashMap, so registration
     * order is not guaranteed. Sort by rarity tier, then by name, so page
     * order is stable across restarts.
     */
    private static List<Fragment> sortedFragments() {
        List<Fragment> sorted = new ArrayList<>(FragmentRegistry.all());

        sorted.sort(
                Comparator.comparingInt((Fragment f) -> f.getRarity().ordinal())
                        .thenComparing(Fragment::getName)
        );

        return sorted;
    }

    // ------------------------------------------------------------------
    // Layout helpers
    //
    // BookMeta has no alignment API and WrittenBookContentComponent has
    // no alignment field, so centring is done with leading spaces
    // measured through BookPalette's real glyph advances.
    // ------------------------------------------------------------------

    /** Pixel offset the fragment stat labels are column-aligned to. */
    private static final int LABEL_COLUMN =
            BookPalette.pixelWidth("Cooldown") + BookPalette.SPACE_WIDTH;

    /** A bold, centred heading. */
    private static Component heading(String text, TextColor color) {
        return centered(Component.text(text, color).decorate(TextDecoration.BOLD));
    }

    /** A separator rule the same width as {@code text}, also centred. */
    private static Component rule(String text, TextColor color) {
        return centered(Component.text(
                BookPalette.ruleFor(text, MIN_RULE_LENGTH),
                color
        ));
    }

    /** A label line: dark label, coloured value (mirrors FragmentItem lore). */
    private static Component statRow(String labelText, Component value) {
        return Component.text(
                BookPalette.padToColumn(labelText, LABEL_COLUMN),
                BookPalette.MUTED
        ).append(value);
    }

    private static Component blank() {
        return Component.empty();
    }

    /**
     * Centres a component by prepending the right number of spaces.
     *
     * <p>Lines too wide to centre are left aligned deliberately rather
     * than silently given a negative pad.
     */
    private static Component centered(Component line) {
        int pad = BookPalette.centerPadding(flattenWidth(line));

        if (pad <= 0) {
            return line;
        }

        return Component.text(" ".repeat(pad)).append(line);
    }

    /** Plain-text pixel width of a component tree. */
    private static int flattenWidth(Component component) {
        int width = 0;

        if (component instanceof TextComponent text) {
            width += BookPalette.pixelWidth(text.content());
        }

        for (Component child : component.children()) {
            width += flattenWidth(child);
        }

        return width;
    }

    /** Joins page lines with newlines. */
    private static Component join(List<Component> lines) {
        Component result = Component.empty();

        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                result = result.append(Component.newline());
            }
            result = result.append(lines.get(i));
        }

        return result;
    }

    /**
     * Re-applies {@code color} to every leaf of a component tree,
     * preserving bold/italic/underline. Item lore is coloured for dark
     * tooltips, so it has to be recoloured for the book page.
     */
    private static Component recolor(Component component, TextColor color) {
        if (component instanceof TextComponent text) {
            // TextComponent.Builder only overloads content(String), so
            // recolour through Component.color() instead.
            Component recolored = Component.text(text.content())
                    .color(color)
                    .decorations(text.decorations());

            if (text.hasDecoration(TextDecoration.BOLD)) {
                recolored = recolored.decorate(TextDecoration.BOLD);
            }
            if (text.hasDecoration(TextDecoration.ITALIC)) {
                recolored = recolored.decorate(TextDecoration.ITALIC);
            }
            if (text.hasDecoration(TextDecoration.UNDERLINED)) {
                recolored = recolored.decorate(TextDecoration.UNDERLINED);
            }

            for (Component child : text.children()) {
                recolored = recolored.append(recolor(child, color));
            }

            return recolored;
        }

        Component rebuilt = component.color(color);

        for (Component child : component.children()) {
            rebuilt = rebuilt.append(recolor(child, color));
        }

        return rebuilt;
    }

    // ------------------------------------------------------------------
    // Tick formatting - kept identical to FragmentItem so the book and the
    // physical fragment item always show the same numbers.
    // ------------------------------------------------------------------

    private static String formatTicks(long ticks) {
        double seconds = ticks / 20.0;

        if (seconds == Math.floor(seconds)) {
            return String.format(Locale.ROOT, "%.0fs", seconds);
        }

        return String.format(Locale.ROOT, "%.2fs", seconds);
    }

    private static String formatModifier(long ticks) {
        double seconds = ticks / 20.0;

        if (ticks > 0) {
            if (seconds == Math.floor(seconds)) {
                return String.format(Locale.ROOT, "+%.0fs", seconds);
            }
            return String.format(Locale.ROOT, "+%.2fs", seconds);
        }

        if (seconds == Math.floor(seconds)) {
            return String.format(Locale.ROOT, "%.0fs", seconds);
        }

        return String.format(Locale.ROOT, "%.2fs", seconds);
    }

    // ------------------------------------------------------------------
    // Inline markup parser
    // ------------------------------------------------------------------

    /**
     * Parses one content line into a Component.
     *
     * <p>Supported tags (see {@link TutorialContent}): named colour tags,
     * their {@code </colour>} counterparts, {@code <b>}/{@code </b>},
     * {@code <i>}/{@code </i>} and a bare {@code </>} reset.
     *
     * <p>Unrecognised tags are dropped rather than rendered literally.
     * Printing the raw tag was handy while writing the content file, but
     * it ships as visible junk on a real page, so silence wins here.
     */
    private static Component parse(String raw) {
        TextComponent.Builder builder = Component.text();

        TextColor currentColor = BookPalette.BODY;
        boolean bold = false;
        boolean italic = false;

        StringBuilder buffer = new StringBuilder();
        int index = 0;

        while (index < raw.length()) {
            char ch = raw.charAt(index);

            if (ch == '<') {
                int end = raw.indexOf('>', index);

                if (end != -1) {
                    String tag = raw.substring(index + 1, end)
                            .toLowerCase(Locale.ROOT).trim();

                    // Flush the text accumulated before the tag.
                    if (!buffer.isEmpty()) {
                        builder.append(styled(buffer.toString(), currentColor, bold, italic));
                        buffer.setLength(0);
                    }

                    switch (tag) {
                        case "b" -> bold = true;
                        case "/b" -> bold = false;
                        case "i" -> italic = true;
                        case "/i" -> italic = false;
                        case "/", "reset" -> {
                            currentColor = BookPalette.BODY;
                            bold = false;
                            italic = false;
                        }
                        default -> {
                            if (tag.startsWith("/")) {
                                // Closing colour tag, e.g. </dark_gray>.
                                currentColor = BookPalette.BODY;
                            } else {
                                TextColor named = BookPalette.color(tag, null);

                                if (named != null) {
                                    currentColor = named;
                                }
                                // Unknown tag: dropped on purpose.
                            }
                        }
                    }

                    index = end + 1;
                    continue;
                }
            }

            buffer.append(ch);
            index++;
        }

        if (!buffer.isEmpty()) {
            builder.append(styled(buffer.toString(), currentColor, bold, italic));
        }

        return builder.build();
    }

    private static Component styled(String text, TextColor color, boolean bold, boolean italic) {
        Component component = Component.text(text, color);

        if (bold) {
            component = component.decorate(TextDecoration.BOLD);
        }
        if (italic) {
            component = component.decorate(TextDecoration.ITALIC);
        }

        return component;
    }
}

