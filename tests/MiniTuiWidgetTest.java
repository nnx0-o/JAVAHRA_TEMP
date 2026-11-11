import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MiniTuiWidgetTest {

    public static void main(String[] args) throws Exception {
        MiniTui tui = new MiniTui();
        tui.setTheme(MiniTui.Color.YELLOW);
        AtomicInteger actions = new AtomicInteger();
        MiniTui.Button button = tui.add(new MiniTui.Button(2, 2, 0, "Akce", actions::incrementAndGet));
        check(button.render(true, false, 80).contains(MiniTui.Color.YELLOW), "button adopts theme");
        Method key = MiniTui.class.getDeclaredMethod("handleKey", String.class);
        key.setAccessible(true);
        button.enabled = false;
        key.invoke(tui, "ENTER");
        check(actions.get() == 0, "disabled button cannot activate");
        button.enabled = true;
        key.invoke(tui, "ENTER");
        check(actions.get() == 1, "enabled button activates");
        AtomicInteger help = new AtomicInteger();
        tui.onKey("h", help::incrementAndGet);
        key.invoke(tui, "h");
        check(help.get() == 1, "global help callback");
        MiniTui.Input input = tui.add(new MiniTui.Input(2, 4, 0, "Jméno", "", 12));
        tui.focus(input);
        key.invoke(tui, "ENTER");
        key.invoke(tui, "h");
        check(input.getValue().equals("h") && help.get() == 1, "editing does not trigger shortcut");
        tui.onKey("F1", help::incrementAndGet);
        key.invoke(tui, "F1");
        check(help.get() == 2 && input.getValue().equals("h"), "F1 remains available during name editing");
        key.invoke(tui, "ESC");
        tui.focus(button);
        tui.focus(new MiniTui.Button(0, 0, 0, "Cizí", () -> {}));
        check(tui.getFocusedElement() == button, "foreign focus ignored");

        MiniTui.TableRow row = new MiniTui.TableRow(2, 6, 0, "Nabídka",
                List.of("Dlouhý meč", "5", "0"), List.of(7, 3, 2),
                List.of(MiniTui.Color.WHITE, MiniTui.Color.YELLOW, MiniTui.Color.RED), () -> {});
        String rendered = row.render(true, false, 80);
        check(strip(rendered).equals(" > Dlouhý  5   0 "), "columns padded and clipped by visible width");
        check(rendered.indexOf(MiniTui.Color.RESET) < rendered.indexOf("Dlouhý"), "only selector highlighted");
        MiniTui.Label label = new MiniTui.Label(0, 8, 0, MiniTui.Color.RED + "abcdef" + MiniTui.Color.RESET);
        check(strip(label.render(false, false, 5)).equals("abcd"), "ansi styles do not consume label columns");
        MiniTui.Label unicode = new MiniTui.Label(0, 9, 0, "界abc");
        check(strip(unicode.render(false, false, 5)).equals("界ab"), "wide characters clipped to terminal cells");
        MiniTui.Box box = tui.add(new MiniTui.Box(1, 10, 0, 60, 5, "Rám", MiniTui.Color.WHITE));
        tui.add(new MiniTui.Label(20, 10, 0, "Vnitřek"));
        Method boundary = MiniTui.class.getDeclaredMethod("renderBoundary", MiniTui.Element.class, int.class);
        boundary.setAccessible(true);
        check((int) boundary.invoke(tui, box, 80) == 80, "interior labels cannot clip box borders");
        System.out.println("MiniTuiWidgetTest: OK");
    }

    private static String strip(String text) {
        return text.replaceAll("\\u001B\\[[0-9;:]*m", "");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
