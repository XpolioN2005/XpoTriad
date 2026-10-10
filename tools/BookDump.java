import dev.xpolion.xpotriad.fragment.Fragment;
import dev.xpolion.xpotriad.visual.BookPalette;
import dev.xpolion.xpotriad.visual.TutorialBook;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Tutorial book layout checker.
 *
 * <p>Renders every page with the real {@link BookPalette} metrics, applies
 * the vanilla 114px word wrap, and reports any page that overflows the
 * 14-line page height or the 114px page width. Run this after editing
 * {@code TutorialContent} so layout regressions surface here instead of
 * in game.
 *
 * <p>Lives outside {@code src/main/java} on purpose: it reaches into
 * private page builders by reflection and must not ship in the plugin jar.
 *
 * <pre>
 *   javac -nowarn -cp "&lt;classpath&gt;;build/classes" -d tools/out tools/BookDump.java
 *   java -cp "&lt;classpath&gt;;build/classes;tools/out" BookDump
 * </pre>
 *
 * <p>Where {@code &lt;classpath&gt;} is every jar in {@code lib/} joined by {@code ;}.
 */
public class BookDump {

    /** Minecraft book page height, in text lines. */
    private static final int MAX_LINES = 14;

    /** Number of pages that must fit; a written book allows up to 100. */
    private static final int MAX_PAGES = 100;

    public static void main(String[] args) throws Exception {
        Class<?> book = TutorialBook.class;

        List<Component> pages = new ArrayList<>();
        pages.add(invoke(book, "coverPage"));
        pages.add(invoke(book, "sectionPage",
                content("ABILITIES_HEADING"), contentList("ABILITIES_LINES")));
        pages.add(invoke(book, "sectionPage",
                content("FRAGMENTS_INFO_HEADING"), contentList("FRAGMENTS_INFO_LINES")));
        pages.add(invoke(book, "sectionPage",
                content("FRAGMENTS_USE_HEADING"), contentList("FRAGMENTS_USE_LINES")));
        pages.add(invoke(book, "sectionPage",
                content("ETCHLOOM_ITEM_HEADING"), contentList("ETCHLOOM_ITEM_LINES")));
        pages.add(invoke(book, "rarityPage"));
        pages.add(invoke(book, "sectionPage",
                content("QUICKREF_HEADING"), contentList("QUICKREF_LINES")));

        String[] names = {"cover", "abilities", "fragments", "using", "loom item",
                "rarity", "quickref"};

        System.out.println("== fixed pages ==");

        int failures = 0;

        for (int i = 0; i < pages.size(); i++) {
            failures += report(names[i], pages.get(i));
        }

        // Fragment pages cannot be instantiated without a live server
        // (Material needs the registry), so simulate the worst case.
        System.out.println();
        System.out.println("== worst-case fragment page ==");
        failures += reportWorstFragment();

        System.out.println();
        System.out.println("== font metric spot checks ==");
        for (String s : new String[] {
                "Abilities", "Quick Reference", "COMMON", "Mark Fragment",
                "Mass Freeze Fragment", "Cooldown" }) {
            int width = BookPalette.pixelWidth(s);

            System.out.printf("  %-22s %3dpx  pad=%d%n",
                    '"' + s + '"', width, BookPalette.centerPadding(width));
        }

        System.out.println();
        System.out.println("== rarity palette (must all be dark) ==");
        failures += auditPalette();

        System.out.println();
        System.out.printf("pages so far=%d (limit %d)%n", pages.size(), MAX_PAGES);
        System.out.printf("total fragments=29 -> book would be %d pages%n",
                pages.size() + 29);
        System.out.println(failures == 0
                ? "RESULT: all checks passed"
                : "RESULT: " + failures + " FAILURE(S)");
    }

    private static String content(String field) throws Exception {
        return (String) field(field, String.class);
    }

    @SuppressWarnings("unchecked")
    private static List<String> contentList(String field) throws Exception {
        return (List<String>) field(field, List.class);
    }

    private static Object field(String name, Class<?> type) throws Exception {
        var f = Class.forName("dev.xpolion.xpotriad.visual.TutorialContent")
                .getDeclaredField(name);
        f.setAccessible(true);

        return type == List.class ? f.get(null) : f.get(null);
    }

    private static Component invoke(Class<?> book, String name, Object... args)
            throws Exception {
        Method m = book.getDeclaredMethod(name, types(args));
        m.setAccessible(true);

        try {
            return (Component) m.invoke(null, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw cause instanceof Exception ex ? ex : new RuntimeException(cause);
        }
    }

    private static Class<?>[] types(Object... args) {
        // Use the declared parameter type, not the runtime class, so
        // List.of()'s ImmutableCollections impl still matches List.class.
        Class<?>[] types = new Class<?>[args.length];

        for (int i = 0; i < args.length; i++) {
            types[i] = args[i] instanceof List ? List.class : args[i].getClass();
        }

        return types;
    }

    /**
     * Simulates the vanilla 114px word wrap so wrapped line counts match
     * what the game actually renders.
     *
     * <p>Two bugs used to live here, both of which made pages look
     * shorter than they really were - and one of them is what let the
     * "Fragments" page report a clean pass while clipping in game:
     *
     * <ol>
     *   <li>{@code split(" ")} turned a leading indent into empty tokens,
     *       so the indent was dropped and the line measured too narrow.</li>
     *   <li>An empty line produced no output at all, so blank separator
     *       lines were never counted - even though the game renders them.</li>
     * </ol>
     */
    private static List<String> wrap(String line) {
        List<String> out = new ArrayList<>();

        // A blank line is still a rendered line.
        if (line.isEmpty()) {
            out.add("");
            return out;
        }

        StringBuilder cur = new StringBuilder();
        int i = 0;

        while (i < line.length()) {
            if (line.charAt(i) == ' ') {
                // Keep the whole whitespace run together so leading
                // indents survive and are measured at their real width.
                int start = i;
                while (i < line.length() && line.charAt(i) == ' ') {
                    i++;
                }
                String spaces = line.substring(start, i);

                if (BookPalette.pixelWidth(cur + spaces) > BookPalette.PAGE_TEXT_WIDTH
                        && cur.length() > 0) {
                    out.add(trimTrailingSpaces(cur.toString()));
                    cur.setLength(0);
                } else {
                    cur.append(spaces);
                }
                continue;
            }

            int start = i;
            while (i < line.length() && line.charAt(i) != ' ') {
                i++;
            }
            String word = line.substring(start, i);
            String trial = cur + word;

            if (BookPalette.pixelWidth(trial) > BookPalette.PAGE_TEXT_WIDTH
                    && cur.length() > 0) {
                out.add(trimTrailingSpaces(cur.toString()));
                cur.setLength(0);
                cur.append(word);
            } else {
                cur.append(word);
            }
        }

        if (cur.length() > 0) {
            out.add(cur.toString());
        }

        return out;
    }

    private static String trimTrailingSpaces(String s) {
        int end = s.length();

        while (end > 0 && s.charAt(end - 1) == ' ') {
            end--;
        }

        return s.substring(0, end);
    }

    private static int report(String label, Component page) {
        List<String> wrapped = new ArrayList<>();

        for (String line : flatten(page).split("\n", -1)) {
            wrapped.addAll(wrap(line));
        }

        int widest = 0;
        int failures = 0;

        for (String line : wrapped) {
            int px = BookPalette.pixelWidth(line);
            widest = Math.max(widest, px);

            if (px > BookPalette.PAGE_TEXT_WIDTH) {
                System.out.printf("   !! line is %dpx (max %d): |%s%n",
                        px, BookPalette.PAGE_TEXT_WIDTH, line);
                failures++;
            }
        }

        if (wrapped.size() > MAX_LINES) {
            System.out.printf("   !! %d lines (max %d) - tail will be clipped%n",
                    wrapped.size(), MAX_LINES);
            failures++;
        }

        System.out.printf("%-10s lines=%2d  widest=%3dpx  %s%n",
                label, wrapped.size(), widest, failures == 0 ? "ok" : "FAIL");

        for (String line : wrapped) {
            System.out.println("    |" + line);
        }

        return failures;
    }

    /**
     * Longest real fragment name + longest real lore, plus the four stat
     * rows, since Fragment instances cannot be built off-server.
     */
    private static int reportWorstFragment() {
        String name = "Mass Freeze Fragment";
        String[] rows = {
                name,
                "-".repeat(BookPalette.pixelWidth(name) / 6 + 1),
                "",
                "Freezes enemies in an area.",
                "The next damage dealt to the target is multiplied by 1.5x.",
                "",
                "Rarity    COMMON",
                "Type      close range",
                "Execution 0.50s",
                "Cooldown +1.50s"
        };

        List<String> wrapped = new ArrayList<>();

        for (String row : rows) {
            wrapped.addAll(wrap(row));
        }

        System.out.printf("worst-case fragment page: lines=%d (max %d) %s%n",
                wrapped.size(), MAX_LINES,
                wrapped.size() > MAX_LINES ? "FAIL" : "ok");

        for (String line : wrapped) {
            System.out.println("    |" + line);
        }

        return wrapped.size() > MAX_LINES ? 1 : 0;
    }

    /**
     * Fails if any rarity colour is one of the bright values that are
     * unreadable on the cream page.
     */
    private static int auditPalette() {
        int failures = 0;

        for (Fragment.Rarity rarity : Fragment.Rarity.values()) {
            var color = BookPalette.rarityColor(rarity);
            int hex = color.value();

            // Rough luminance: bright colours read poorly on parchment.
            int r = (hex >> 16) & 0xFF;
            int g = (hex >> 8) & 0xFF;
            int b = hex & 0xFF;
            int lum = (r + g + b) / 3;

            boolean readable = lum < 0xC0;

            System.out.printf("  %-10s #%06X  lum=%3d  %s%n",
                    rarity, hex, lum, readable ? "readable" : "TOO BRIGHT");

            if (!readable) {
                failures++;
            }
        }

        return failures;
    }

    /** Strips markup and flattens a component tree to plain text. */
    private static String flatten(Component c) {
        StringBuilder sb = new StringBuilder();
        collect(c, sb);
        return sb.toString();
    }

    private static void collect(Component c, StringBuilder sb) {
        if (c instanceof TextComponent t) {
            sb.append(t.content());
        }
        for (Component child : c.children()) {
            collect(child, sb);
        }
    }
}
