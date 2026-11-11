import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TerminalInputTest {

    public static void main(String[] args) throws Exception {
        List<String> keys = new ArrayList<>();
        Class<?> decoderClass = Class.forName("MiniTui$UnixKeyDecoder");
        Constructor<?> constructor = decoderClass.getDeclaredConstructor(Consumer.class);
        constructor.setAccessible(true);
        Object decoder = constructor.newInstance((Consumer<String>) keys::add);
        Method accept = decoderClass.getDeclaredMethod("accept", int.class, long.class);
        accept.setAccessible(true);
        for (byte value : "Žluťoučký".getBytes(StandardCharsets.UTF_8)) accept.invoke(decoder, value & 255, System.nanoTime());
        check(String.join("", keys).equals("Žluťoučký"), "UTF-8 input survives byte boundaries");
        keys.clear();
        for (byte value : "\033[A\033[B\033[1;5C".getBytes(StandardCharsets.UTF_8)) accept.invoke(decoder, value & 255, System.nanoTime());
        check(keys.equals(List.of("UP", "DOWN", "RIGHT")), "arrow decoding preserved");
        MiniTui tui = new MiniTui();
        MiniTui.Input input = tui.add(new MiniTui.Input(1, 1, 0, "Jméno", "", 20));
        Method handleKey = MiniTui.class.getDeclaredMethod("handleKey", String.class);
        handleKey.setAccessible(true);
        handleKey.invoke(tui, "ENTER");
        handleKey.invoke(tui, "Ž");
        handleKey.invoke(tui, "á");
        handleKey.invoke(tui, "😀");
        handleKey.invoke(tui, "BACKSPACE");
        check(input.getValue().equals("Žá"), "unicode edits remove whole codepoints");
        handleKey.invoke(tui, "\n");
        check(input.getValue().equals("Žá"), "control input rejected");
        tui.clear();
        check(tui.getElements().isEmpty() && !tui.isEditing() && tui.getFocusedElement() == null, "clear resets controls");
        System.out.println("TerminalInputTest: OK");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
