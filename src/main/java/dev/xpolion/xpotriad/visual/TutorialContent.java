package dev.xpolion.xpotriad.visual;

import java.util.List;

/**
 * All player-facing text of the tutorial book lives here.
 *
 * <p>EDIT THIS FILE to change the book - {@link TutorialBook} only handles
 * layout and never hardcodes prose.
 *
 * <p>Write whole sentences on one line and let Minecraft word-wrap them;
 * do not hand-break lines mid-clause. A page fits roughly 14 wrapped
 * lines, so keep each page to about six sentences of body text.
 *
 * <p>COLOURS: a book page is light cream with dark text, the inverse of
 * chat and item lore. Bright colours ({@code white}, {@code gray},
 * {@code gold}, {@code light_purple}, {@code aqua}, {@code green}) are
 * unreadable on parchment, so content defaults to {@code dark_gray} and
 * headings to {@code black}. See {@link BookPalette} for the contrast
 * table. Rarity colours come from
 * {@link BookPalette#rarityColor} - never from the items, whose colours
 * are tuned for dark tooltips.
 *
 * <p>Inline markup supported in every line:
 * <table>
 *   <caption>Markup tags</caption>
 *   <tr><th>tag</th><th>effect</th></tr>
 *   <tr><td>{@code <dark_gray>}</td><td>colour with any NamedTextColor name</td></tr>
 *   <tr><td>{@code </dark_gray>}</td><td>close a colour, back to dark_gray</td></tr>
 *   <tr><td>{@code <b>} / {@code </b>}</td><td>bold on / off</td></tr>
 *   <tr><td>{@code <i>} / {@code </i>}</td><td>italic on / off</td></tr>
 *   <tr><td>{@code </>}</td><td>reset colour and decorations</td></tr>
 * </table>
 * Colour names are lowercase with underscores: {@code dark_gray} works,
 * {@code darkGray} does not. Unrecognised tags are dropped silently, so a
 * typo never prints raw markup on a page.
 *
 * <p>Fragment pages are generated from {@code FragmentRegistry.all()};
 * only their labels are defined here.
 */
public final class TutorialContent {

    private TutorialContent() {
    }

    // ------------------------------------------------------------------
    // Page 1 - cover (title + subtitle)
    // ------------------------------------------------------------------

    public static final String TITLE = "XpoTriad";

    public static final String SUBTITLE = "An Ability and Fragment Handbook";

    /** Shown under the subtitle. */
    public static final String COVER_TAGLINE = "Etch a fragment. Fire the weapon.";

    /** Last line of the cover. */
    public static final String COVER_HOWTO = "";

    // ------------------------------------------------------------------
    // Page 2 - what is an Ability
    // ------------------------------------------------------------------

    public static final String ABILITIES_HEADING = "Abilities";

    public static final List<String> ABILITIES_LINES = List.of(
            "An ability is a weapon with three stages:",
            "<dark_purple>PRE_CAST</>, <dark_purple>CAST</> and",
            "<dark_purple>POST_CAST</>. Right-click to fire it.",
            "",
            "Cooldown starts at 1.0s, shifts with every",
            "fragment, and clamps between 0 and 15s."
    );

    // ------------------------------------------------------------------
    // Page 3 - what is a Fragment
    // ------------------------------------------------------------------

    public static final String FRAGMENTS_INFO_HEADING = "Fragments";

    public static final List<String> FRAGMENTS_INFO_LINES = List.of(
            "A fragment is one",
            "gameplay unit.",
            "",
            "It holds an effect,",
            "a timing cost, and",
            "a type.",
            "",
            "Fragments slot into a",
            "weapon to give it an",
            "ability."
    );

    // ------------------------------------------------------------------
    // Page 4 - how to use a Fragment
    // ------------------------------------------------------------------

    public static final String FRAGMENTS_USE_HEADING = "Using Fragments";

    public static final List<String> FRAGMENTS_USE_LINES = List.of(
            "<dark_blue>1.</> Find one as loot.",
            "<dark_blue>2.</> Open the EtchLoom",
            "   with a crafted",
            "   EtchLoom item.",
            "<dark_blue>3.</> Slot a weapon and",
            "   your fragments in.",
            "<dark_blue>4.</> Engrave, then fire",
            "   with a right-click."
    );

    // ------------------------------------------------------------------
    // Page 5 - the craftable EtchLoom item
    // ------------------------------------------------------------------

    public static final String ETCHLOOM_ITEM_HEADING = "EtchLoom Item";

    public static final List<String> ETCHLOOM_ITEM_LINES = List.of(
            "A portable loom.",
            "",
            "Right-click it to open",
            "the EtchLoom.",
            "",
            "Craft it from four",
            "amethyst shards and",
            "one book. It never",
            "breaks."
    );

    // ------------------------------------------------------------------
    // Page 6 - rarity key (tier rows are generated from the enum)
    // ------------------------------------------------------------------

    public static final String RARITY_HEADING = "Rarity";

    /**
     * One short note per {@code Fragment.Rarity} value, in declaration
     * order: COMMON, UNCOMMON, RARE, EPIC, LEGENDARY. Tier names are
     * column-aligned automatically, so any length is fine.
     */
    public static final String[] RARITY_NOTES = {
            "often",
            "steady",
            "good",
            "wild",
            "rarest"
    };

    public static final List<String> RARITY_LINES = List.of(
            "",
            "Rarity sets how hard",
            "a fragment is to find."
    );

    // ------------------------------------------------------------------
    // Fragment pages - labels only. Names, lore and rarity come from the
    // Fragment instances themselves (FragmentRegistry.all()).
    // ------------------------------------------------------------------

    public static final String FRAGMENT_RARITY_LABEL = "Rarity ";
    public static final String FRAGMENT_TYPE_LABEL = "Type ";
    public static final String FRAGMENT_EXEC_LABEL = "Execution ";
    public static final String FRAGMENT_COOLDOWN_LABEL = "Cooldown ";
    public static final String FRAGMENT_TYPE_MELEE = "close range ";
    public static final String FRAGMENT_TYPE_RANGED = "ranged ";

    // ------------------------------------------------------------------
    // Last page - quick reference (edit here when commands change)
    // ------------------------------------------------------------------

    public static final String QUICKREF_HEADING = "Quick Reference";

    public static final List<String> QUICKREF_LINES = List.of(
            "A crafted EtchLoom",
            "opens the loom.",
            "",
            "<dark_blue>/xpt book</> reopens",
            "this book.",
            "",
            "<dark_blue>/xpt test</> gives a",
            "chest of fragments.",
            "",
            "<dark_blue>/xpt help</> lists all",
            "commands."
    );
}
