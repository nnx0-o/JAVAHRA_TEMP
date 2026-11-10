import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/** Dependency-free regression tests; run with assertions implemented below. */
public class MiniTuiTerminalTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Object call(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void geometry(MiniTui tui, int width, int height, int left, int top) throws Exception {
        call(tui, "updateTerminalGeometry", new Class<?>[]{int.class, int.class, int.class, int.class}, width, height, left, top);
    }

    private static String frame(MiniTui tui) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream original = System.out;
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            call(tui, "renderScreen", new Class<?>[0]);
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    // Replay emitted output independently of MiniTui's clipping implementation.
    // Any out-of-range CUP, overflowing write, or bottom-right write fails.
    private static void assertFits(String output, int width, int height) {
        int x = 0, y = 0;
        Matcher escapes = Pattern.compile("\\x1b\\[([?0-9;:]*)([@-~])").matcher(output);
        for (int index = 0; index < output.length();) {
            if (output.charAt(index) == 27) {
                escapes.region(index, output.length());
                check(escapes.lookingAt(), "Invalid ANSI output");
                if (escapes.group(2).equals("H")) {
                    String[] coordinates = escapes.group(1).split(";");
                    y = coordinates[0].isEmpty() ? 0 : Integer.parseInt(coordinates[0]) - 1;
                    x = coordinates.length == 1 ? 0 : Integer.parseInt(coordinates[1]) - 1;
                    check(x >= 0 && x < width && y >= 0 && y < height, "Cursor outside " + width + "x" + height);
                }
                index = escapes.end();
            } else {
                int cp = output.codePointAt(index);
                int columns = cp == 0x754c ? 2 : Character.getType(cp) == Character.NON_SPACING_MARK ? 0 : 1;
                check(x >= 0 && x + columns <= width && y >= 0 && y < height, "Text overflow in " + width + "x" + height);
                check(columns == 0 || y != height - 1 || x + columns < width, "Bottom-right write can scroll");
                x += columns;
                index += Character.charCount(cp);
            }
        }
    }

    private static void renderingAndGeometry() throws Exception {
        MiniTui tui = new MiniTui();
        tui.add(new MiniTui.Title(2, 1, 0, "A title longer than a small terminal"));
        MiniTui.Input input = tui.add(new MiniTui.Input(2, 3, 0, "Name", "unchanged", 100));
        tui.add(new MiniTui.Button(2, 5, 0, "A long button that must never wrap", () -> {}));
        tui.add(new MiniTui.Box(0, 7, 0, 90, 30, "Clipped border", "\033[36m"));
        tui.add(new MiniTui.Label(-2, 2, 0, "negative origin"));
        tui.add(new MiniTui.Label(0, 4, 0, "e\u0301\u754c\u754c\u754c\nnext row"));
        tui.add(new MiniTui.Element(0, 6, 0, false) {
            public String render(boolean focused, boolean editing, int width) {
                return "\033[999;999Hmust be invisible\033[2;1Hvisible";
            }
        });
        set(tui, "editing", true);
        Object focus = field(tui, "focusedElement");
        for (int width : new int[]{1, 2, 3, 5, 8, 20, 80}) {
            for (int height : new int[]{1, 2, 3, 5, 24}) {
                geometry(tui, width, height, 0, 0);
                assertFits(frame(tui), width, height);
                check(input.value.equals("unchanged") && field(tui, "focusedElement") == focus
                        && (boolean) field(tui, "editing"), "Resize changed editing/focus state");
            }
        }
        check(frame(tui).isEmpty(), "Stable frames should stay cached");
        geometry(tui, 80, 24, 0, 12);
        check(!frame(tui).isEmpty(), "Moving the Windows viewport must redraw an identical frame");
        geometry(tui, 80, 24, 0, 12);
        check(frame(tui).isEmpty(), "Unchanged geometry should not redraw");
        geometry(tui, 12, 4, 0, 0);
        geometry(tui, 80, 24, 0, 12);
        check(!frame(tui).isEmpty(), "A shrink/grow between frames must invalidate the cache");
        geometry(tui, 0, 0, 0, 0);
        geometry(tui, -1, 4, 0, 0);
        check(tui.getTerminalWidth() == 80 && tui.getTerminalHeight() == 24, "Invalid dimensions were accepted");
        input.x = 100;
        input.y = 100;
        assertFits(frame(tui), 80, 24);
        input.x = 2;
        input.y = 3;
        check(!frame(tui).isEmpty(), "Moving a widget back into view must redraw it");
        PlayerPurchaseTest.main(new String[0]);
    }

    private static void movementKeys() throws Exception {
        List<String> keys = new ArrayList<>();
        Class<?> decoderType = Class.forName("MiniTui$UnixKeyDecoder");
        Constructor<?> constructor = decoderType.getDeclaredConstructor(java.util.function.Consumer.class);
        constructor.setAccessible(true);
        Object decoder = constructor.newInstance((java.util.function.Consumer<String>) keys::add);
        long now = 0;
        for (String sequence : List.of("\033[A", "\033OB", "\033[1;5C", "\033[1;2D", "\033[5~", "\033[6;2~")) {
            for (char c : sequence.toCharArray()) {
                call(decoder, "accept", new Class<?>[]{int.class, long.class}, (int) c, now);
                now += TimeUnit.MILLISECONDS.toNanos(20);
                call(decoder, "expire", new Class<?>[]{long.class}, now);
            }
        }
        check(keys.equals(List.of("UP", "DOWN", "RIGHT", "LEFT", "PGUP", "PGDN")), "Fragmented CSI/SS3 keys: " + keys);
        keys.clear();
        call(decoder, "accept", new Class<?>[]{int.class, long.class}, 27, now);
        call(decoder, "expire", new Class<?>[]{long.class}, now + TimeUnit.MILLISECONDS.toNanos(100));
        check(keys.equals(List.of("ESC")), "Standalone Escape did not expire");
        keys.clear();
        for (char c : "\033[999~x".toCharArray()) call(decoder, "accept", new Class<?>[]{int.class, long.class}, (int) c, now);
        check(keys.equals(List.of("x")), "Unknown escape sequence leaked into input");

        MiniTui tui = new MiniTui();
        for (String line : List.of("SIZE,120,40,5,12", "SIZE,0,0,0,0", "SIZE,bad,24,0,0",
                "KEY,UpArrow,0", "KEY,DownArrow,0", "KEY,LeftArrow,0", "KEY,RightArrow,0",
                "KEY,PageUp,0", "KEY,PageDown,0", "KEY,OemComma,44", "KEY,Enter,13", "KEY,C,3")) {
            call(tui, "handleWindowsMessage", new Class<?>[]{String.class}, line);
        }
        check(tui.getTerminalWidth() == 120 && tui.getTerminalHeight() == 40, "Windows visible geometry lost");
        List<?> windowsKeys = new ArrayList<>((Queue<?>) field(tui, "keyQueue"));
        check(windowsKeys.equals(List.of("UP", "DOWN", "LEFT", "RIGHT", "PGUP", "PGDN", ",", "ENTER", "CTRL_C")),
                "Windows key protocol: " + windowsKeys);
    }

    private static String stty(String arguments) throws Exception {
        Process process = new ProcessBuilder("sh", "-c", "stty " + arguments + " < /dev/tty").start();
        check(process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() == 0, "stty failed");
        return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
    }

    private static void ptyChild() throws Exception {
        String originalMode = stty("-g");
        MiniTui tui = new MiniTui();
        boolean[] activated = {false};
        tui.add(new MiniTui.Button(2, 1, 0, "First", () -> {}));
        tui.add(new MiniTui.Button(2, 3, 0, "Second", () -> activated[0] = true));
        tui.add(new MiniTui.Box(0, 6, 0, 80, 30, "Oversized", "\033[36m"));
        int[][] sizes = {{12, 5}, {1, 1}, {80, 24}};
        int[] stage = {-1};
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        tui.onTick(() -> {
            try {
                check(System.nanoTime() < deadline, "Live resize/key handling timed out");
                if (stage[0] == -1) {
                    System.err.println("PTY_READY");
                    stage[0] = 0;
                    stty("cols 12 rows 5");
                } else if (stage[0] < sizes.length && tui.getTerminalWidth() == sizes[stage[0]][0]
                        && tui.getTerminalHeight() == sizes[stage[0]][1]) {
                    stage[0]++;
                    if (stage[0] < sizes.length) stty("cols " + sizes[stage[0]][0] + " rows " + sizes[stage[0]][1]);
                } else if (stage[0] == sizes.length && activated[0]) tui.stop();
            } catch (Exception failure) {
                throw new RuntimeException(failure);
            }
        });
        tui.start();
        check(stty("-g").equals(originalMode), "Terminal mode was not restored exactly");
        tui.start();
        check(stty("-g").equals(originalMode), "Restart did not restore terminal mode");
        MiniTui failing = new MiniTui();
        RuntimeException callbackFailure = new RuntimeException("Expected callback failure");
        failing.onTick(() -> { throw callbackFailure; });
        try {
            failing.start();
            throw new AssertionError("Callback failure did not propagate");
        } catch (RuntimeException failure) {
            check(failure == callbackFailure, "Callback failure was replaced");
        }
        check(stty("-g").equals(originalMode), "Exception did not restore terminal mode");
        MiniTui interrupted = new MiniTui();
        interrupted.onTick(() -> Thread.currentThread().interrupt());
        interrupted.start();
        check(Thread.interrupted(), "Interrupted status was not preserved");
        check(stty("-g").equals(originalMode), "Interruption did not restore terminal mode");
        check(Thread.getAllStackTraces().keySet().stream().noneMatch(t -> t.getName().equals("TUI-Input") && t.isAlive()),
                "Input thread leaked after stop");
        System.out.println("PTY_OK");
    }

    private static void liveUnixTerminal() throws Exception {
        if (!System.getProperty("os.name").equals("Linux")) return;
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("java.class.path");
        String command = "stty cols 80 rows 24; exec '" + java.replace("'", "'\\''") + "' -cp '"
                + classpath.replace("'", "'\\''") + "' MiniTuiTerminalTest --pty-child";
        Process process = new ProcessBuilder("script", "-qefc", command, "/dev/null").redirectErrorStream(true).start();
        StringBuffer transcript = new StringBuffer();
        CountDownLatch ready = new CountDownLatch(1);
        Thread output = new Thread(() -> {
            try (Reader reader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)) {
                char[] buffer = new char[4096];
                int count;
                while ((count = reader.read(buffer)) >= 0) {
                    transcript.append(buffer, 0, count);
                    if (transcript.indexOf("PTY_READY") >= 0) ready.countDown();
                }
            } catch (IOException ignored) { }
        });
        output.start();
        try {
            check(ready.await(5, TimeUnit.SECONDS), "PTY did not become ready: " + transcript);
            for (byte b : new byte[]{27, '[', 'B', 13}) {
                process.getOutputStream().write(b);
                process.getOutputStream().flush();
                Thread.sleep(20);
            }
            check(process.waitFor(12, TimeUnit.SECONDS), "PTY process did not stop");
            output.join(1000);
            check(process.exitValue() == 0 && transcript.indexOf("PTY_OK") >= 0, "Live terminal failed: " + transcript);
        } finally {
            process.destroyForcibly();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--pty-child")) { ptyChild(); return; }
        Path result = Path.of("build", "mini-tui-test-results.txt");
        Files.createDirectories(result.getParent());
        try {
            renderingAndGeometry();
            movementKeys();
            liveUnixTerminal();
            Files.writeString(result, "PASS: viewport clipping, resize/movement invalidation, focus/edit preservation, Unix/Windows key decoding, Linux PTY resize/stop/restart/exception/interruption and exact mode restoration.\n");
            System.out.println("MiniTuiTerminalTest: OK");
        } catch (Exception | AssertionError failure) {
            StringWriter details = new StringWriter();
            failure.printStackTrace(new PrintWriter(details));
            Files.writeString(result, details.toString());
            throw failure;
        }
    }
}
