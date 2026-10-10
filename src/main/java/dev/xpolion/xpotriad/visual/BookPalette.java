package dev.xpolion.xpotriad.visual;

import dev.xpolion.xpotriad.fragment.Fragment.Rarity;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.Locale;

/**
 * Book-specific colours and font metrics.
 *
 * <p>A book page is light cream parchment with black text - the exact
 * inverse of chat, item lore and the action bar. Every bright colour that
 * reads well on a dark background ({@code white}, {@code gray},
 * {@code light_purple}, {@code aqua}, {@code green}, {@code gold}) is
 * low contrast or invisible on parchment, so the book deliberately does
 * <b>not</b> reuse {@code Rarity.getColor()} and keeps its own palette.
 *
 * <p>Contrast on the cream page, rounded:
 * <table>
 *   <caption>Relative luminance contrast</caption>
 *   <tr><th>colour</th><th>hex</th><th>contrast</th><th>usable?</th></tr>
 *   <tr><td>{@code black}</td><td>#000000</td><td>~20:1</td><td>yes</td></tr>
 *   <tr><td>{@code dark_gray}</td><td>#555555</td><td>~7:1</td><td>yes</td></tr>
 *   <tr><td>{@code dark_red}</td><td>#AA0000</td><td>~6:1</td><td>yes</td></tr>
 *   <tr><td>{@code dark_blue}</td><td>#0000AA</td><td>~12:1</td><td>yes</td></tr>
 *   <tr><td>{@code dark_purple}</td><td>#AA00AA</td><td>~4.5:1</td><td>yes</td></tr>
 *   <tr><td>{@code dark_green}</td><td>#00AA00</td><td>~3.5:1</td><td>large text</td></tr>
 *   <tr><td>{@code gold}</td><td>#FFAA00</td><td>~1.9:1</td><td>no</td></tr>
 *   <tr><td>{@code gray}</td><td>#AAAAAA</td><td>~2:1</td><td>no</td></tr>
 *   <tr><td>{@code white}</td><td>#FFFFFF</td><td>~1:1</td><td>no - it is the page</td></tr>
 * </table>
 */
public final class BookPalette {

    /** Body prose. {@code gray} (#AAAAAA) is unreadable on parchment. */
    public static final TextColor BODY = NamedTextColor.DARK_GRAY;

    /** Page headings. */
    public static final TextColor HEADING = NamedTextColor.BLACK;

    /** A quieter note than {@link #BODY}, for secondary hints. */
    public static final TextColor MUTED = NamedTextColor.DARK_GRAY;

    /** Stage names such as PRE_CAST / CAST / POST_CAST. */
    public static final TextColor STAGE = NamedTextColor.DARK_PURPLE;

    /** Command names such as /xpt book. */
    public static final TextColor COMMAND = NamedTextColor.DARK_BLUE;

    /** The separator rule drawn under a heading. */
    public static final TextColor RULE = NamedTextColor.DARK_GRAY;

    /** Cover accents. */
    public static final TextColor ACCENT = NamedTextColor.BLACK;
    public static final TextColor ACCENT_SECONDARY = NamedTextColor.DARK_GRAY;

    private BookPalette() {
    }

    /**
     * Maps a rarity tier to a colour that survives on parchment.
     *
     * <p>Each tier keeps a distinct hue so the legend still reads as a
     * progression: COMMON &rarr; gray, UNCOMMON &rarr; green,
     * RARE &rarr; blue, EPIC &rarr; purple, LEGENDARY &rarr; red.
     *
     * @param rarity the item rarity tier
     * @return a dark, parchment-legible colour
     */
    public static TextColor rarityColor(Rarity rarity) {
        if (rarity == null) {
            return BODY;
        }

        return switch (rarity) {
            case COMMON -> NamedTextColor.DARK_GRAY;
            case UNCOMMON -> NamedTextColor.DARK_GREEN;
            case RARE -> NamedTextColor.DARK_BLUE;
            case EPIC -> NamedTextColor.DARK_PURPLE;
            case LEGENDARY -> NamedTextColor.DARK_RED;
        };
    }

    // ------------------------------------------------------------------
    // Font metrics
    //
    // BookMeta exposes no alignment API, and WrittenBookContentComponent
    // has no alignment field, so leading spaces are the only way to
    // centre or column-align text. Doing that accurately requires the
    // real glyph advances: the font is proportional, so a flat
    // 6px-per-glyph guess makes each line drift onto a different axis.
    // ------------------------------------------------------------------

    /** Width in pixels of the book page text area. */
    public static final int PAGE_TEXT_WIDTH = 114;

    /** Advance width of a space in the default font. */
    public static final int SPACE_WIDTH = 4;

    /** Advance width of a hyphen, used for separator rules. */
    public static final int RULE_CHAR_WIDTH = 6;

    /** Fallback advance for any character outside the table. */
    private static final int DEFAULT_ADVANCE = 6;

    /**
     * Advance widths of the default Minecraft font for ASCII, indexed by
     * character code. Derived from the vanilla {@code ascii.png} glyph
     * provider: most glyphs are 6px, but {@code i}/{@code l} and the
     * punctuation are much narrower.
     */
    private static final byte[] ADVANCE = buildAdvanceTable();

    private static byte[] buildAdvanceTable() {
        byte[] table = new byte[128];

        for (int i = 0; i < table.length; i++) {
            table[i] = (byte) DEFAULT_ADVANCE;
        }

        set(table, ' ', 4);
        set(table, '!', 2);
        set(table, '"', 5);
        set(table, '\'', 3);
        set(table, '(', 5);
        set(table, ')', 5);
        set(table, '*', 5);
        set(table, ',', 2);
        set(table, '.', 2);
        set(table, ':', 2);
        set(table, ';', 2);
        set(table, '<', 5);
        set(table, '>', 5);
        set(table, '[', 4);
        set(table, ']', 4);
        set(table, '^', 5);
        set(table, '`', 3);
        set(table, '{', 5);
        set(table, '}', 5);
        set(table, '~', 7);
        set(table, '|', 2);
        set(table, 'f', 5);
        set(table, 'i', 2);
        set(table, 'k', 5);
        set(table, 'l', 3);
        set(table, 'r', 5);
        set(table, 't', 4);
        set(table, 'I', 4);
        set(table, '@', 7);
        set(table, '-', 6);

        return table;
    }

    private static void set(byte[] table, char c, int width) {
        if (c < table.length) {
            table[c] = (byte) width;
        }
    }

    /** Advance width of a single character. */
    public static int advance(char c) {
        return c < ADVANCE.length ? ADVANCE[c] : DEFAULT_ADVANCE;
    }

    /**
     * Pixel width of a string in the default font.
     *
     * @param text text to measure
     * @return advance width in pixels
     */
    public static int pixelWidth(String text) {
        int width = 0;

        for (int i = 0; i < text.length(); i++) {
            width += advance(text.charAt(i));
        }

        return width;
    }

    /**
     * Leading spaces needed to optically centre text {@code width}px wide.
     *
     * @param width measured width of the text
     * @return number of spaces to prepend, never negative
     */
    public static int centerPadding(int width) {
        int remaining = PAGE_TEXT_WIDTH - width;

        return remaining <= 0 ? 0 : remaining / (2 * SPACE_WIDTH);
    }

    /**
     * A separator rule matching the width of {@code text}, so the rule
     * under a heading lines up with the heading itself.
     *
     * @param text the heading the rule underlines
     * @param minLength floor, so very short headings still get a visible rule
     * @return a string of hyphens
     */
    public static String ruleFor(String text, int minLength) {
        int target = Math.max(minLength, pixelWidth(text) / RULE_CHAR_WIDTH + 1);

        return "-".repeat(target);
    }

    /**
     * Appends spaces until {@code text} ends at or just past {@code column}.
     * Used to column-align the rarity legend.
     *
     * @param text text to pad
     * @param column target pixel offset
     * @return the padded text
     */
    public static String padToColumn(String text, int column) {
        while (pixelWidth(text) + SPACE_WIDTH <= column) {
            text = text + ' ';
        }

        return text;
    }

    /**
     * Resolves a colour name from content, falling back to {@code fallback}.
     *
     * <p>{@code NamedTextColor.NAMES} is keyed by lowercase names using
     * underscores, so {@code dark_gray} resolves but {@code darkGray} does
     * not.
     *
     * @param name colour name such as {@code dark_gray}
     * @param fallback colour used when {@code name} is unknown
     * @return the resolved colour
     */
    public static TextColor color(String name, TextColor fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }

        NamedTextColor named = NamedTextColor.NAMES.value(
                name.toLowerCase(Locale.ROOT)
        );

        return named != null ? named : fallback;
    }
}
