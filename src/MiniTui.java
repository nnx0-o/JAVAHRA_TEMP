import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * A small terminal UI: widgets describe what to draw, and this class handles
 * keyboard input, focus, callbacks, and ANSI terminal output.
 *
 * Reading order: Element and its widgets, engine state, start(), handleKey(),
 * moveFocus(), renderScreen(), then the platform-specific input readers.
 *
 * Main builds the widgets and supplies game actions as callbacks. start() runs
 * on its calling thread and blocks until stopped. A separate daemon thread reads
 * keys into a queue; game callbacks run on the calling thread when it drains that
 * queue. The input thread does not directly execute game actions.
 */
public class MiniTui {

    /**
     * Composable ANSI colors and text styles. Values are strings, so they work
     * with the existing Title and Box color arguments and with custom elements.
     * For example: Color.combine(Color.BOLD, Color.WHITE, Color.BG_BLUE).
     * Effects and extended colors depend on the terminal's capabilities.
     */
    public static final class Color {
        private Color() {}

        public static final String RESET = "\033[0m";

        // Foreground colors, including the terminal's configured default.
        public static final String BLACK = "\033[30m";
        public static final String RED = "\033[31m";
        public static final String GREEN = "\033[32m";
        public static final String YELLOW = "\033[33m";
        public static final String BLUE = "\033[34m";
        public static final String MAGENTA = "\033[35m";
        public static final String CYAN = "\033[36m";
        public static final String WHITE = "\033[37m";
        public static final String BRIGHT_BLACK = "\033[90m";
        public static final String BRIGHT_RED = "\033[91m";
        public static final String BRIGHT_GREEN = "\033[92m";
        public static final String BRIGHT_YELLOW = "\033[93m";
        public static final String BRIGHT_BLUE = "\033[94m";
        public static final String BRIGHT_MAGENTA = "\033[95m";
        public static final String BRIGHT_CYAN = "\033[96m";
        public static final String BRIGHT_WHITE = "\033[97m";
        public static final String DEFAULT = "\033[39m";

        // Background colors can be combined with any foreground and styles.
        public static final String BG_BLACK = "\033[40m";
        public static final String BG_RED = "\033[41m";
        public static final String BG_GREEN = "\033[42m";
        public static final String BG_YELLOW = "\033[43m";
        public static final String BG_BLUE = "\033[44m";
        public static final String BG_MAGENTA = "\033[45m";
        public static final String BG_CYAN = "\033[46m";
        public static final String BG_WHITE = "\033[47m";
        public static final String BG_BRIGHT_BLACK = "\033[100m";
        public static final String BG_BRIGHT_RED = "\033[101m";
        public static final String BG_BRIGHT_GREEN = "\033[102m";
        public static final String BG_BRIGHT_YELLOW = "\033[103m";
        public static final String BG_BRIGHT_BLUE = "\033[104m";
        public static final String BG_BRIGHT_MAGENTA = "\033[105m";
        public static final String BG_BRIGHT_CYAN = "\033[106m";
        public static final String BG_BRIGHT_WHITE = "\033[107m";
        public static final String BG_DEFAULT = "\033[49m";

        public static final String BOLD = "\033[1m";
        public static final String DIM = "\033[2m";
        public static final String ITALIC = "\033[3m";
        public static final String UNDERLINE = "\033[4m";
        public static final String BLINK = "\033[5m";
        public static final String RAPID_BLINK = "\033[6m";
        public static final String REVERSE = "\033[7m";
        public static final String HIDDEN = "\033[8m";
        public static final String STRIKETHROUGH = "\033[9m";
        public static final String DOUBLE_UNDERLINE = "\033[21m";
        public static final String OVERLINE = "\033[53m";

        /**
         * Combines codes in order without resetting between them. Any number of
         * styles can accompany a foreground/background pair; for conflicting
         * codes the terminal applies the last one. No codes returns an empty string.
         */
        public static String combine(String... codes) {
            StringBuilder result = new StringBuilder();
            for (String code : codes) result.append(Objects.requireNonNull(code, "code"));
            return result.toString();
        }

        /** Wraps text with the supplied codes and resets all styling afterwards. */
        public static String apply(String text, String... codes) {
            return combine(codes) + Objects.requireNonNull(text, "text") + RESET;
        }

        /** Selects a foreground from the terminal's 256-color palette (0-255). */
        public static String foreground(int index) {
            return sgr(38, 5, channel(index));
        }

        /** Selects a background from the terminal's 256-color palette (0-255). */
        public static String background(int index) {
            return sgr(48, 5, channel(index));
        }

        /** Selects a true-color foreground; each RGB component must be 0-255. */
        public static String foreground(int red, int green, int blue) {
            return sgr(38, 2, channel(red), channel(green), channel(blue));
        }

        /** Selects a true-color background; each RGB component must be 0-255. */
        public static String background(int red, int green, int blue) {
            return sgr(48, 2, channel(red), channel(green), channel(blue));
        }

        /**
         * Builds any numeric SGR sequence, including styles without named
         * constants. For example, sgr(22) turns off bold/dim. No parameters resets.
         */
        public static String sgr(int... parameters) {
            if (parameters.length == 0) return RESET;
            StringJoiner result = new StringJoiner(";", "\033[", "m");
            for (int parameter : parameters) {
                if (parameter < 0) throw new IllegalArgumentException("SGR parameters must be non-negative");
                result.add(Integer.toString(parameter));
            }
            return result.toString();
        }

        private static int channel(int value) {
            if (value < 0 || value > 255) {
                throw new IllegalArgumentException("Color index or RGB component must be between 0 and 255: " + value);
            }
            return value;
        }
    }

    // WIDGETS
    // Nested static classes can be created as new MiniTui.Button(...), without
    // first needing an enclosing MiniTui instance. add() registers them later.
    // Common contract for every widget. x and y are zero-based column/row
    // positions; z selects a screen, not overlapping depth within one screen.
    // Only visible widgets on currentZ are rendered. focusable controls whether
    // keyboard navigation can select the widget.
    public static abstract class Element {
        // volatile makes individual updates visible across threads. It does not
        // make several field changes one atomic operation; normal game updates
        // should stay in callbacks on the UI loop thread.
        public volatile int x, y, z;
        public volatile boolean focusable;
        public volatile boolean visible = true;

        public Element(int x, int y, int z, boolean focusable) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.focusable = focusable;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        public boolean isFocusable() {
            return focusable;
        }

        public boolean isVisible() {
            return visible;
        }

        /**
         * Returns the full stored text without rendering decorations or clipping.
         * Inputs return their value; boxes return their title. Custom elements
         * with no text return an empty string unless they override this method.
         */
        public String getText() {
            return "";
        }

        // Return display text and optional ANSI escape sequences; do not print
        // here. renderScreen() combines all widget output into a single frame.
        // termWidth may be shortened to the next widget's x position.
        public abstract String render(boolean focused, boolean editing, int termWidth);
    }

    // A non-interactive heading padded with '=' characters and an ANSI color.
    // The short constructor delegates to the full one with a default color.
    public static class Title extends Element {
        private volatile String text;
        private volatile String colorCode;

        public Title(int x, int y, int z, String text) {
            this(x, y, z, text, Color.combine(Color.BOLD, Color.CYAN));
        }

        public Title(int x, int y, int z, String text, String ansiColor) {
            super(x, y, z, false);
            this.text = text;
            this.colorCode = ansiColor;
        }

        public void setText(String text) {
            this.text = text;
        }

        @Override
        public String getText() {
            return text;
        }

        public String getColorCode() {
            return colorCode;
        }

        @Override
        public String render(boolean focused, boolean editing, int termWidth) {
            // Reserve room for padding, trim the title, then build a centered bar.
            // These lengths count Java characters, not terminal display columns.
            int maxAllowed = Math.max(4, termWidth - x - 2);
            String cleanText = text.length() > maxAllowed - 4 ? text.substring(0, maxAllowed - 4) : text;
            int barWidth = Math.max(cleanText.length() + 2, maxAllowed);
            int padLen = Math.max(0, (barWidth - cleanText.length() - 2) / 2);
            String pad = "=".repeat(padLen);
            String line = pad + " " + cleanText + " " + pad;
            while (line.length() < barWidth) line += "=";
            return colorCode + line + Color.RESET;
        }
    }

    // A border made from several cursor-positioned fragments. The interior is
    // left untouched so other widgets can be drawn there.
    public static class Box extends Element {
        public volatile int width, height;
        public volatile String title;
        public volatile String colorCode;

        public Box(int x, int y, int z, int width, int height, String title, String colorCode) {
            super(x, y, z, false);
            this.width = width;
            this.height = height;
            this.title = title;
            this.colorCode = colorCode;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public String getTitle() {
            return title;
        }

        @Override
        public String getText() {
            return getTitle();
        }

        public String getColorCode() {
            return colorCode;
        }

        @Override
        public String render(boolean focused, boolean editing, int termWidth) {
            int maxW = Math.max(0, termWidth - x);
            int w = Math.min(Math.max(2, width), maxW);
            if (w < 2 || height < 2) return "";
            String t = title.length() > w - 6 ? title.substring(0, Math.max(0, w - 6)) : title;

            StringBuilder sb = new StringBuilder();
            sb.append(colorCode);

            // Top border
            String top = w < 6 ? "+" + "-".repeat(w - 2) + "+"
                    : "+- " + t + " " + "-".repeat(Math.max(0, w - t.length() - 5)) + "+";
            // ESC[row;columnH moves the terminal cursor. ANSI coordinates start
            // at 1, which is why the widget's zero-based positions get +1.
            sb.append(String.format("\033[%d;%dH%s", y + 1, x + 1, top));

            // Left & right walls only (hollow inside so inner elements stay intact)
            for (int i = 1; i < height - 1; i++) {
                sb.append(String.format("\033[%d;%dH|", y + 1 + i, x + 1));
                sb.append(String.format("\033[%d;%dH|", y + 1 + i, x + w));
            }

            // Bottom border
            String bot = "+" + "-".repeat(Math.max(0, w - 2)) + "+";
            sb.append(String.format("\033[%d;%dH%s", y + height, x + 1, bot)).append(Color.RESET);
            return sb.toString();
        }
    }

    // Plain, non-focusable text. Main keeps references to labels and calls
    // setText() when player statistics or messages need to change.
    public static class Label extends Element {
        private volatile String text;

        public Label(int x, int y, int z, String text) {
            super(x, y, z, false);
            this.text = text;
        }

        public void setText(String text) {
            this.text = text;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public String render(boolean focused, boolean editing, int termWidth) {
            int maxLen = Math.max(1, termWidth - x - 2);
            return text.length() > maxLen ? text.substring(0, maxLen) : text;
        }
    }

    // A text field has two separate states: focused (selected by navigation)
    // and editing (typed characters change its value). Enter toggles editing.
    // Consumer<String> is a callback taking the current text as its argument.
    public static class Input extends Element {
        public volatile String label;
        public volatile String value;
        public volatile int width;
        public Consumer<String> onChange;
        public Consumer<String> onSubmit;

        public Input(int x, int y, int z, String label, String defaultValue, int width) {
            super(x, y, z, true);
            this.label = label;
            this.value = defaultValue;
            this.width = width;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String getText() {
            return getValue();
        }

        public String getLabel() {
            return label;
        }

        public int getWidth() {
            return width;
        }

        public Consumer<String> getOnChange() {
            return onChange;
        }

        public Consumer<String> getOnSubmit() {
            return onSubmit;
        }

        // Programmatic assignment does not invoke onChange. The keyboard handler
        // explicitly calls onChange after user edits and onSubmit after Enter.
        // Returning this from the registration methods allows chained calls.
        public void setValue(String val) {
            this.value = val;
        }

        public Input onChange(Consumer<String> cb) {
            this.onChange = cb;
            return this;
        }

        public Input onSubmit(Consumer<String> cb) {
            this.onSubmit = cb;
            return this;
        }

        @Override
        public String render(boolean focused, boolean editing, int termWidth) {
            // width limits the visible portion, not the stored input length.
            // Long values show their last characters so the typing end stays visible.
            // The renderer also clips the label/decorations to the viewport.
            int fieldWidth = Math.max(1, Math.min(width, termWidth - x - label.length() - 7));
            String displayVal = value.length() > fieldWidth
                    ? value.substring(value.length() - fieldWidth)
                    : String.format("%-" + fieldWidth + "s", value);

            if (editing) {
                return " " + label + ": " + Color.apply("[" + displayVal + "]", Color.BG_YELLOW, Color.BLACK) + " ";
            } else if (focused) {
                return Color.apply(" " + label + ": [" + displayVal + "] ", Color.REVERSE, Color.CYAN);
            } else {
                return " " + label + ": [" + displayVal + "] ";
            }
        }
    }

    // Runnable is a callback with no arguments and no return value. The button
    // stores it; handleKey() calls action.run() when Enter activates this button.
    // Rendering only changes the visual style and never triggers the action.
    public static class Button extends Element {
        public volatile String text;
        public Runnable action;

        public Button(int x, int y, int z, String text, Runnable action) {
            super(x, y, z, true);
            this.text = text;
            this.action = action;
        }

        public void setText(String text) {
            this.text = text;
        }

        @Override
        public String getText() {
            return text;
        }

        public Runnable getAction() {
            return action;
        }

        @Override
        public String render(boolean focused, boolean editing, int termWidth) {
            if (focused) {
                return Color.apply(" < " + text + " > ", Color.REVERSE, Color.GREEN);
            }
            return " [ " + text + " ] ";
        }
    }

    // ENGINE STATE
    // CopyOnWriteArrayList lets iteration continue safely if a list changes,
    // at the cost of copying its array on modification. This suits lists that
    // are populated during setup and read much more often than they change.
    private final List<Element> elements = new CopyOnWriteArrayList<>();
    // Thread-safe handoff: the input thread offers normalized key names, and
    // the UI loop polls them without waiting. Reading the physical input can block
    // without blocking screen updates.
    private final BlockingQueue<String> keyQueue = new LinkedBlockingQueue<>();
    private final List<Runnable> tickListeners = new CopyOnWriteArrayList<>();

    // Focus is the selected widget; editing applies only to an Input widget.
    // Terminal size starts at 80x24 until a platform reader obtains real values.
    private volatile Element focusedElement = null;
    private volatile boolean editing = false;
    private volatile boolean running = false;
    private volatile int currentZ = 0;
    // Publish width, height and the Windows scroll-buffer origin together.
    // A new snapshot also invalidates the frame after a shrink/grow round trip.
    private record TerminalGeometry(int width, int height, int left, int top) {}
    private volatile TerminalGeometry terminal = new TerminalGeometry(80, 24, 0, 0);
    private volatile String statusText = "Arrows: Move | Enter: Edit/Click | [ / ]: Switch Screen | Ctrl+C: Quit";
    // Only the UI loop accesses this cache. Identical frames are not printed
    // again, although callbacks and frame construction still run each tick.
    private String lastFrame = "";
    private TerminalGeometry lastRenderedGeometry;
    private volatile Process windowsProcess;
    private volatile int windowsOutputMode = -1;
    private volatile int windowsInputMode = -1;
    private volatile Exception inputFailure;
    private String unixTerminalMode;
    private boolean terminalRestored;

    // Preserve the concrete widget type in the return value. For example,
    // Label stats = tui.add(new Label(...)) needs no cast from Element.
    // The first eligible widget becomes the initial keyboard selection.
    public <T extends Element> T add(T el) {
        elements.add(el);
        if (focusedElement == null && el.focusable && el.z == currentZ && el.visible) {
            focusedElement = el;
        }
        return el;
    }

    // Register work to run before each render. Main uses this for refresh().
    // This is UI refresh timing, not a game turn: turns advance only in game actions.
    public void onTick(Runnable callback) {
        tickListeners.add(callback);
    }

    public void setStatus(String status) {
        this.statusText = status;
    }

    public String getStatus() {
        return statusText;
    }

    /** Returns an unmodifiable snapshot; the elements themselves remain mutable. */
    public List<Element> getElements() {
        return List.copyOf(elements);
    }

    /** Returns the currently selected element, or null if none is selected. */
    public Element getFocusedElement() {
        return focusedElement;
    }

    public boolean isEditing() {
        return editing;
    }

    public boolean isRunning() {
        return running;
    }

    public int getTerminalWidth() {
        return terminal.width();
    }

    public int getTerminalHeight() {
        return terminal.height();
    }

    // Switching screens cancels editing and selects an eligible widget again.
    // Calling this with the current layer also resets focus after hiding a group.
    public void setLayer(int z) {
        this.currentZ = z;
        this.editing = false;
        this.focusedElement = null;
        ensureFocusOnCurrentLayer();
    }

    public int getLayer() {
        return currentZ;
    }

    public void stop() {
        this.running = false;
    }

    // Input and geometry are monitored off the UI thread, including while idle.
    // Callbacks still run synchronously on the calling thread, as before.
    public void start() {
        if (running) throw new IllegalStateException("MiniTui is already running");
        running = true;
        boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");
        terminalRestored = false;
        inputFailure = null;
        lastFrame = "";
        lastRenderedGeometry = null;
        keyQueue.clear();
        CountDownLatch ready = new CountDownLatch(1);
        Thread inputThread = new Thread(() -> {
            try {
                if (isWindows) readWindowsKeys(ready);
                else readUnixKeys();
            } catch (Exception failure) {
                if (running) {
                    inputFailure = failure;
                    running = false;
                }
            } finally {
                ready.countDown();
            }
        }, "TUI-Input");
        inputThread.setDaemon(true);
        Thread shutdownHook = new Thread(() -> {
            running = false;
            restoreTerminal(isWindows);
        }, "TUI-Restore");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        try {
            if (!isWindows) {
                unixTerminalMode = execSh("stty -g < /dev/tty").trim();
                // Read Ctrl+C as input so the normal cleanup path always runs.
                execSh("stty -echo -icanon -isig min 1 time 0 < /dev/tty");
                updateUnixTermSize();
            }
            inputThread.start();
            // Windows must enable VT output before the first escape is printed.
            if (isWindows && !ready.await(5, TimeUnit.SECONDS)) {
                throw new IOException("Timed out opening the Windows console");
            }
            if (inputFailure != null) throw inputFailure;
            System.out.print("\033[?1049h\033[?25l");
            System.out.flush();
            while (running) {
                String key;
                while ((key = keyQueue.poll()) != null) {
                    handleKey(key);
                }
                for (Runnable tick : tickListeners) {
                    tick.run();
                }
                renderScreen();
                Thread.sleep(50);
            }
            if (inputFailure != null) throw inputFailure;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("MiniTui needs a working interactive terminal", failure);
        } finally {
            boolean interrupted = Thread.interrupted();
            running = false;
            inputThread.interrupt();
            restoreTerminal(isWindows);
            interrupted |= Thread.interrupted();
            try {
                inputThread.join(1000);
            } catch (InterruptedException interruption) {
                interrupted = true;
            }
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException shuttingDown) {
                // The hook has already started and shares the cleanup guard.
            }
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private synchronized void restoreTerminal(boolean isWindows) {
        if (terminalRestored) return;
        terminalRestored = true;
        boolean interrupted = Thread.interrupted();
        try {
            Process process = windowsProcess;
            if (process != null) {
                process.destroyForcibly();
                process.waitFor(1, TimeUnit.SECONDS);
                windowsProcess = null;
            }
            System.out.print("\033[0m\033[?25h\033[?1049l");
            System.out.flush();
            if (isWindows && windowsOutputMode >= 0 && windowsInputMode >= 0) {
                runCommand(windowsCommand(WINDOWS_CONSOLE_API + "\n" +
                        "$o = [TuiConsole]::Open('CONOUT$'); $i = [TuiConsole]::Open('CONIN$'); " +
                        "[TuiConsole]::SetConsoleMode($o, " + windowsOutputMode + ") | Out-Null; " +
                        "[TuiConsole]::SetConsoleMode($i, " + windowsInputMode + ") | Out-Null; " +
                        "[TuiConsole]::CloseHandle($o) | Out-Null; " +
                        "[TuiConsole]::CloseHandle($i) | Out-Null;"), 5000);
            } else if (!isWindows && unixTerminalMode != null) {
                execSh("stty '" + unixTerminalMode.replace("'", "'\\''") + "' < /dev/tty");
            }
        } catch (InterruptedException interruption) {
            interrupted = true;
        } catch (IOException ignored) {
            // The console may have been closed by the user during shutdown.
        } finally {
            unixTerminalMode = null;
            windowsOutputMode = windowsInputMode = -1;
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    // This method interprets normalized input, independently of the OS reader.
    // In editing mode, keys modify text; otherwise they navigate or activate UI.
    private void handleKey(String key) {
        if (key.equals("CTRL_C")) {
            running = false;
            return;
        }

        if (editing && focusedElement instanceof Input input) {
            if (key.equals("ENTER") || key.equals("ESC")) {
                editing = false;
                // Escape exits editing without submission, but keeps typed text;
                // it does not restore the field's value from before editing.
                if (key.equals("ENTER") && input.onSubmit != null) {
                    input.onSubmit.accept(input.value);
                }
            } else if (key.equals("BACKSPACE")) {
                if (!input.value.isEmpty()) {
                    input.value = input.value.substring(0, input.value.length() - 1);
                    if (input.onChange != null) input.onChange.accept(input.value);
                }
            // Current limitation: only printable ASCII is accepted for typing.
            // Czech text supplied by the program can still be rendered, but users
            // cannot type accented characters through this branch.
            } else if (key.length() == 1 && key.charAt(0) >= 32 && key.charAt(0) <= 126) {
                input.value += key;
                if (input.onChange != null) input.onChange.accept(input.value);
            }
            return;
        }

        // Arrow keys choose another widget geometrically. Brackets/Page keys
        // switch to an adjacent layer only when it contains a visible element.
        switch (key) {
            case "UP"    -> moveFocus(0, -1);
            case "DOWN"  -> moveFocus(0, 1);
            case "LEFT"  -> moveFocus(-1, 0);
            case "RIGHT" -> moveFocus(1, 0);
            case "[", "PGUP" -> {
                if (hasElementsAtZ(currentZ - 1)) setLayer(currentZ - 1);
            }
            case "]", "PGDN" -> {
                if (hasElementsAtZ(currentZ + 1)) setLayer(currentZ + 1);
            }
            case "ENTER" -> {
                if (focusedElement instanceof Input) {
                    editing = true;
                } else if (focusedElement instanceof Button btn && btn.action != null) {
                    btn.action.run();
                }
            }
        }
    }

    // A layer may contain only labels: this checks visibility, not focusability.
    private boolean hasElementsAtZ(int z) {
        return elements.stream().anyMatch(e -> e.z == z && e.visible);
    }

    // Keep valid focus. If a screen change or visibility change invalidates it,
    // fall back to the first eligible widget in registration order, or null.
    private void ensureFocusOnCurrentLayer() {
        if (focusedElement != null && focusedElement.z == currentZ
                && focusedElement.focusable && focusedElement.visible) return;
        focusedElement = elements.stream()
                .filter(e -> e.z == currentZ && e.focusable && e.visible)
                .findFirst()
                .orElse(null);
    }

    // dx/dy identify one cardinal direction, such as (0, 1) for down.
    // Navigation uses widget anchor positions, not widget sizes or tab order.
    private void moveFocus(int dx, int dy) {
        ensureFocusOnCurrentLayer();
        if (focusedElement == null) return;

        Element best = null;
        double bestScore = Double.MAX_VALUE;

        for (Element el : elements) {
            if (!el.focusable || !el.visible || el == focusedElement || el.z != currentZ) continue;

            int relX = el.x - focusedElement.x;
            int relY = el.y - focusedElement.y;

            // Reject candidates outside the requested direction. For example,
            // moving right requires a strictly positive horizontal offset.
            if (dx != 0 && Integer.signum(relX) != dx) continue;
            if (dy != 0 && Integer.signum(relY) != dy) continue;

            // Prefer nearby widgets in the requested direction, with a penalty
            // for sideways deviation. These weights are a layout heuristic;
            // equal scores keep the earlier widget in registration order.
            double primaryDist = (dx != 0) ? Math.abs(relX) : Math.abs(relY);
            double secondaryDist = (dx != 0) ? Math.abs(relY) * 6.0 : Math.abs(relX) * 2.5;
            double score = primaryDist + secondaryDist;

            if (score < bestScore) {
                bestScore = score;
                best = el;
            }
        }

        if (best != null) focusedElement = best;
    }

    // Build the entire screen in memory, then print once if it has changed.
    // This redraws a whole frame rather than updating individual changed cells.
    private void renderScreen() {
        ensureFocusOnCurrentLayer();
        TerminalGeometry geometry = terminal;
        int termWidth = geometry.width();
        int termHeight = geometry.height();
        int footerRow = termHeight - 1;
        StringBuilder sb = new StringBuilder();
        // Hide cursor, move to home, clear screen. These are only sent when the
        // completed frame differs from lastFrame; building the string does no I/O.
        sb.append("\033[?25l\033[H\033[2J");

        // Top border changes style depending on which UI screen (Z-layer) is active
        String borderChar = (currentZ == 0) ? "-" : "~";
        String borderColor = (currentZ == 0) ? Color.CYAN : Color.YELLOW;
        String topBorder = borderColor + (termWidth == 1 ? "+"
                : "+" + borderChar.repeat(termWidth - 2) + "+") + Color.RESET;
        if (footerRow > 0) appendClipped(sb, topBorder, 0, 0, termWidth, footerRow);

        for (Element el : elements) {
            if (el.z != currentZ || !el.visible || el.x >= termWidth || el.y >= footerRow) continue;
            boolean isFocused = (el == focusedElement);
            boolean isEditing = isFocused && editing;
            // Find the nearest visible widget anchored to the right on the same
            // row. The same boundary is enforced for every widget's output,
            // including custom elements and Box's absolute cursor positions.
            int renderWidth = renderBoundary(el, termWidth);
            String out = el.render(isFocused, isEditing, renderWidth);
            appendClipped(sb, out, el.x, el.y, renderWidth, footerRow);
        }

        // Never write the bottom-right cell: terminals with automatic wrapping
        // could scroll the entire frame when that cell is filled.
        int maxFooterWidth = Math.max(0, termWidth - 1);
        String zTag = (currentZ == 0) ? " [MAIN UI] " : " [SHOP UI] ";
        String rawFooter = " " + statusText;
        if (rawFooter.length() + zTag.length() < maxFooterWidth) {
            rawFooter = rawFooter + " ".repeat(maxFooterWidth - rawFooter.length() - zTag.length()) + zTag;
        }
        appendClipped(sb, Color.apply(rawFooter, Color.REVERSE), 0, footerRow,
                maxFooterWidth, termHeight);

        // While editing, show the real terminal cursor at the end of the visible
        // text. The +4 accounts for the input's prefix in zero-based columns.
        // This assumes characters occupy one terminal column.
        if (editing && focusedElement instanceof Input input) {
            int right = renderBoundary(input, termWidth);
            int fieldWidth = Math.max(1, Math.min(input.width, right - input.x - input.label.length() - 7));
            int visibleLen = Math.min(input.value.length(), fieldWidth);
            int cursorX = input.x + input.label.length() + 4 + visibleLen;
            int cursorY = input.y;
            if (cursorX >= 0 && cursorX < right && cursorY >= 0 && cursorY < footerRow) {
                sb.append(String.format("\033[%d;%dH\033[?25h", cursorY + 1, cursorX + 1));
            }
        }

        String frame = sb.toString();
        if (geometry != lastRenderedGeometry || !frame.equals(lastFrame)) {
            System.out.print(frame);
            System.out.flush();
            lastFrame = frame;
            lastRenderedGeometry = geometry;
        }
    }

    private int renderBoundary(Element element, int width) {
        for (Element other : elements) {
            if (other.z == currentZ && other.visible && other.y == element.y
                    && other.x > element.x && other.x < width) width = other.x;
        }
        return width;
    }

    // Interpret cursor movement rather than passing it through unchecked: CUP
    // below the viewport is clamped by terminals and can overwrite the footer.
    // Keep SGR styling, clip text in display columns, and discard other controls.
    private static void appendClipped(StringBuilder frame, String text, int x, int y,
                                      int right, int bottom) {
        boolean positioned = false;
        for (int index = 0; index < text.length();) {
            int codePoint = text.codePointAt(index);
            index += Character.charCount(codePoint);
            if (codePoint == 27) {
                if (index >= text.length() || text.charAt(index++) != '[') continue;
                int start = index;
                while (index < text.length() && (text.charAt(index) < '@' || text.charAt(index) > '~')) index++;
                if (index == text.length()) break;
                char command = text.charAt(index++);
                String args = text.substring(start, index - 1);
                if (command == 'm' && args.matches("[0-9;:]*")) {
                    frame.append("\033[").append(args).append('m');
                } else if (args.matches("[0-9;]*")) {
                    String[] parts = args.split(";", -1);
                    int first = ansiParameter(parts, 0);
                    switch (command) {
                        case 'H', 'f' -> { y = first - 1; x = ansiParameter(parts, 1) - 1; }
                        case 'A' -> y -= first;
                        case 'B' -> y += first;
                        case 'C' -> x += first;
                        case 'D' -> x -= first;
                        case 'G' -> x = first - 1;
                        case 'd' -> y = first - 1;
                        default -> { }
                    }
                    positioned = false;
                }
                continue;
            }
            if (codePoint == '\n') { y++; x = 0; positioned = false; continue; }
            if (codePoint == '\r') { x = 0; positioned = false; continue; }
            if (codePoint == '\t') { x += 8 - Math.floorMod(x, 8); positioned = false; continue; }
            if (Character.isISOControl(codePoint)) continue;
            int columns = displayColumns(codePoint);
            if (x >= 0 && x + columns <= right && y >= 0 && y < bottom
                    && (columns > 0 || positioned)) {
                if (!positioned) frame.append(String.format("\033[%d;%dH", y + 1, x + 1));
                frame.appendCodePoint(codePoint);
                positioned = true;
            } else {
                positioned = false;
            }
            x += columns;
        }
        frame.append(Color.RESET);
    }

    private static int ansiParameter(String[] parts, int index) {
        if (index >= parts.length || parts[index].isEmpty()) return 1;
        try {
            return Math.max(1, Math.min(1_000_000, Integer.parseInt(parts[index])));
        } catch (NumberFormatException invalid) {
            return 1;
        }
    }

    private static int displayColumns(int codePoint) {
        int type = Character.getType(codePoint);
        if (type == Character.NON_SPACING_MARK || type == Character.ENCLOSING_MARK
                || type == Character.FORMAT) return 0;
        // Common wide CJK characters and emoji occupy two terminal cells.
        if (codePoint >= 0x1100 && (codePoint <= 0x115f || codePoint == 0x2329 || codePoint == 0x232a
                || codePoint >= 0x2e80 && codePoint <= 0xa4cf && codePoint != 0x303f
                || codePoint >= 0xac00 && codePoint <= 0xd7a3 || codePoint >= 0xf900 && codePoint <= 0xfaff
                || codePoint >= 0xfe10 && codePoint <= 0xfe19 || codePoint >= 0xfe30 && codePoint <= 0xfe6f
                || codePoint >= 0xff00 && codePoint <= 0xff60 || codePoint >= 0xffe0 && codePoint <= 0xffe6
                || codePoint >= 0x1f300 && codePoint <= 0x1faff || codePoint >= 0x20000 && codePoint <= 0x3fffd)) return 2;
        return 1;
    }

    // UNIX INPUT (Linux, macOS, BSD). Polling /dev/tty lets stop() release the
    // reader without waiting for a key and needs no internal JDK signal APIs.
    private void readUnixKeys() throws Exception {
        UnixKeyDecoder decoder = new UnixKeyDecoder(keyQueue::offer);
        long nextSizeCheck = System.nanoTime();
        try (InputStream in = new FileInputStream("/dev/tty")) {
            while (running) {
                for (int count = 0; count < 256 && running && in.available() > 0; count++) {
                    int c = in.read();
                    if (c < 0) throw new EOFException("Terminal input closed");
                    decoder.accept(c, System.nanoTime());
                }
                long now = System.nanoTime();
                decoder.expire(now);
                if (now >= nextSizeCheck) {
                    updateUnixTermSize();
                    nextSizeCheck = now + TimeUnit.MILLISECONDS.toNanos(250);
                }
                Thread.sleep(10);
            }
        }
    }

    // Retain incomplete CSI/SS3 sequences across reads, including modified
    // arrows. Unknown sequences are consumed as a unit, never typed into Input.
    private static final class UnixKeyDecoder {
        private final Consumer<String> keys;
        private final StringBuilder escape = new StringBuilder();
        private long deadline;

        UnixKeyDecoder(Consumer<String> keys) { this.keys = keys; }

        void accept(int c, long now) {
            if (escape.isEmpty()) {
                switch (c) {
                    case 27 -> escape.append((char) c);
                    case 3 -> keys.accept("CTRL_C");
                    case 10, 13 -> keys.accept("ENTER");
                    case 8, 127 -> keys.accept("BACKSPACE");
                    default -> keys.accept(String.valueOf((char) c));
                }
            } else if (escape.length() == 1) {
                if (c == '[' || c == 'O') escape.append((char) c);
                else {
                    escape.setLength(0);
                    keys.accept("ESC");
                    accept(c, now);
                }
            } else {
                escape.append((char) c);
                if (c >= '@' && c <= '~') {
                    String key = switch (c) {
                        case 'A' -> "UP";
                        case 'B' -> "DOWN";
                        case 'C' -> "RIGHT";
                        case 'D' -> "LEFT";
                        case '~' -> switch (escape.substring(2, escape.length() - 1).split(";", 2)[0]) {
                            case "5" -> "PGUP";
                            case "6" -> "PGDN";
                            default -> null;
                        };
                        default -> null;
                    };
                    if (key != null) keys.accept(key);
                    escape.setLength(0);
                } else if (escape.length() >= 64) escape.setLength(0);
            }
            deadline = now + TimeUnit.MILLISECONDS.toNanos(75);
        }

        void expire(long now) {
            if (!escape.isEmpty() && now >= deadline) {
                if (escape.length() == 1) keys.accept("ESC");
                escape.setLength(0);
            }
        }
    }

    // stty returns "rows columns", so height is read before width.
    // On failure, keep the last known dimensions (or startup defaults).
    private void updateUnixTermSize() {
        try {
            String out = execSh("stty size < /dev/tty").trim();
            String[] parts = out.split("\\s+");
            if (parts.length == 2) {
                updateTerminalGeometry(Integer.parseInt(parts[1]), Integer.parseInt(parts[0]), 0, 0);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (IOException | NumberFormatException ignored) {
        }
    }

    private synchronized void updateTerminalGeometry(int width, int height, int left, int top) {
        // Minimized consoles can briefly report zero dimensions. Keep the last
        // usable snapshot rather than producing negative coordinates/repeats.
        if (width <= 0 || height <= 0 || left < 0 || top < 0) return;
        TerminalGeometry next = new TerminalGeometry(width, height, left, top);
        if (!next.equals(terminal)) terminal = next;
    }

    private static String execSh(String cmd) throws IOException, InterruptedException {
        return runCommand(new ProcessBuilder("sh", "-c", cmd), 1000);
    }

    private static String runCommand(ProcessBuilder builder, long timeoutMillis) throws IOException, InterruptedException {
        Process process = builder.redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) throw new IOException("Terminal command timed out");
            if (process.exitValue() != 0) throw new IOException("Terminal command failed: " + builder.command().get(0));
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            process.destroyForcibly();
        }
    }

    // WINDOWS INPUT. Query CONOUT$ directly: PowerShell's stdout is a pipe, so
    // its host/default buffer dimensions need not describe the visible window.
    // The same bridge works with the classic console and Windows Terminal.
    private static final String WINDOWS_CONSOLE_API = """
            $ErrorActionPreference = 'Stop'
            Add-Type -TypeDefinition @'
            using System;
            using System.Runtime.InteropServices;
            public static class TuiConsole {
                [StructLayout(LayoutKind.Sequential)] public struct Coord { public short X, Y; }
                [StructLayout(LayoutKind.Sequential)] public struct Rect { public short Left, Top, Right, Bottom; }
                [StructLayout(LayoutKind.Sequential)] public struct Info {
                    public Coord Size, Cursor; public ushort Attributes; public Rect Window; public Coord Maximum;
                }
                [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
                public static extern IntPtr CreateFile(string name, uint access, uint share, IntPtr security,
                    uint creation, uint flags, IntPtr template);
                [DllImport("kernel32.dll")] public static extern bool GetConsoleScreenBufferInfo(IntPtr h, out Info info);
                [DllImport("kernel32.dll")] public static extern bool GetConsoleMode(IntPtr h, out uint mode);
                [DllImport("kernel32.dll")] public static extern bool SetConsoleMode(IntPtr h, uint mode);
                [DllImport("kernel32.dll")] public static extern bool CloseHandle(IntPtr h);
                public static IntPtr Open(string name) {
                    return CreateFile(name, 0xC0000000u, 3, IntPtr.Zero, 3, 0, IntPtr.Zero);
                }
            }
            '@
            """;

    private static ProcessBuilder windowsCommand(String script) {
        String encoded = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
        return new ProcessBuilder("powershell.exe", "-NoLogo", "-NoProfile", "-EncodedCommand", encoded);
    }

    private void readWindowsKeys(CountDownLatch ready) throws Exception {
        String script = WINDOWS_CONSOLE_API + """
                $o = [TuiConsole]::Open('CONOUT$'); $i = [TuiConsole]::Open('CONIN$')
                [uint32]$om = 0; [uint32]$im = 0
                if (![TuiConsole]::GetConsoleMode($o, [ref]$om) -or
                    ![TuiConsole]::GetConsoleMode($i, [ref]$im)) { throw 'No interactive console' }
                [Console]::WriteLine("MODE,$om,$im")
                try {
                    if (![TuiConsole]::SetConsoleMode($o, ($om -bor 4))) { throw 'VT output unavailable' }
                    [Console]::TreatControlCAsInput = $true
                    $last = ''
                    while ($true) {
                        $info = New-Object TuiConsole+Info
                        $view = [TuiConsole]::Open('CONOUT$')
                        $valid = [TuiConsole]::GetConsoleScreenBufferInfo($view, [ref]$info)
                        [TuiConsole]::CloseHandle($view) | Out-Null
                        if (!$valid) { throw 'Console closed' }
                        $w = $info.Window.Right - $info.Window.Left + 1
                        $h = $info.Window.Bottom - $info.Window.Top + 1
                        $size = "SIZE,$w,$h,$($info.Window.Left),$($info.Window.Top)"
                        if ($size -ne $last) { [Console]::WriteLine($size); $last = $size }
                        if ([Console]::KeyAvailable) {
                            $k = [Console]::ReadKey($true)
                            [Console]::WriteLine("KEY,$($k.Key),$([int]$k.KeyChar)")
                        } else { Start-Sleep -Milliseconds 20 }
                    }
                } finally {
                    [TuiConsole]::SetConsoleMode($o, $om) | Out-Null
                    [TuiConsole]::SetConsoleMode($i, $im) | Out-Null
                    [TuiConsole]::CloseHandle($o) | Out-Null
                    [TuiConsole]::CloseHandle($i) | Out-Null
                }
                """;
        Process process;
        synchronized (this) {
            if (!running) return;
            process = windowsCommand(script).redirectInput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();
            windowsProcess = process;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while (running && (line = reader.readLine()) != null) {
                handleWindowsMessage(line);
                if (line.startsWith("SIZE,")) ready.countDown();
            }
            if (running) throw new IOException("Windows console reader exited");
        } finally {
            process.destroyForcibly();
        }
    }

    // Numeric character codes keep commas, Enter, and Ctrl+C out of the line
    // protocol itself. Malformed/temporary geometry is ignored safely.
    private void handleWindowsMessage(String line) {
        String[] parts = line.split(",", -1);
        try {
            if (parts.length == 3 && parts[0].equals("MODE")) {
                windowsOutputMode = Integer.parseInt(parts[1]);
                windowsInputMode = Integer.parseInt(parts[2]);
            } else if (parts.length == 5 && parts[0].equals("SIZE")) {
                updateTerminalGeometry(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                        Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
            } else if (parts.length == 3 && parts[0].equals("KEY")) {
                int character = Integer.parseInt(parts[2]);
                String key = character == 3 ? "CTRL_C" : switch (parts[1]) {
                    case "UpArrow" -> "UP";
                    case "DownArrow" -> "DOWN";
                    case "LeftArrow" -> "LEFT";
                    case "RightArrow" -> "RIGHT";
                    case "Enter" -> "ENTER";
                    case "Escape" -> "ESC";
                    case "Backspace" -> "BACKSPACE";
                    case "PageUp" -> "PGUP";
                    case "PageDown" -> "PGDN";
                    default -> character > 0 && Character.isValidCodePoint(character)
                            ? new String(Character.toChars(character)) : null;
                };
                if (key != null) keyQueue.offer(key);
            }
        } catch (NumberFormatException ignored) {
        }
    }
}
