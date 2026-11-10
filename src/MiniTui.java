import java.io.*;
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
            this(x, y, z, text, "\033[1;36m");
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
            return colorCode + line + "\033[0m";
        }
    }

    // A border made from several cursor-positioned fragments. The interior is
    // left untouched so other widgets can be drawn there. Width is constrained,
    // but very small terminals can still be too narrow for the minimum width.
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

        @Override
        public String render(boolean focused, boolean editing, int termWidth) {
            int maxW = Math.max(6, termWidth - x - 1);
            int w = Math.min(Math.max(6, width), maxW);
            String t = title.length() > w - 6 ? title.substring(0, Math.max(0, w - 6)) : title;

            StringBuilder sb = new StringBuilder();
            sb.append(colorCode);

            // Top border
            String top = "+- " + t + " " + "-".repeat(Math.max(0, w - t.length() - 5)) + "+";
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
            sb.append(String.format("\033[%d;%dH%s\033[0m", y + height, x + 1, bot));
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
            // Unlike Label, this widget does not use termWidth to clip its output.
            String displayVal = value.length() > width
                    ? value.substring(value.length() - width)
                    : String.format("%-" + width + "s", value);

            if (editing) {
                return " " + label + ": \033[43;30m[" + displayVal + "]\033[0m ";
            } else if (focused) {
                return "\033[7;36m " + label + ": [" + displayVal + "] \033[0m";
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
        public String render(boolean focused, boolean editing, int termWidth) {
            if (focused) {
                return "\033[7;32m < " + text + " > \033[0m";
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
    private volatile int termWidth = 80;
    private volatile int termHeight = 24;
    private volatile String statusText = "Arrows: Move | Enter: Edit/Click | [ / ]: Switch Screen | Ctrl+C: Quit";
    // Only the UI loop accesses this cache. Identical frames are not printed
    // again, although callbacks and frame construction still run each tick.
    private String lastFrame = "";

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

    public int getTerminalWidth() {
        return termWidth;
    }

    public int getTerminalHeight() {
        return termHeight;
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

    // The loop order is: consume queued keys, refresh terminal size, run tick
    // callbacks, render, sleep. Callbacks are synchronous: a slow callback delays
    // all later input handling and drawing. The 50 ms sleep targets roughly
    // 20 updates per second, with processing time added on top.
    public void start() {
        running = true;
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        // Enter the terminal's alternate screen buffer and hide the cursor.
        // The normal shell screen is restored in the finally block.
        System.out.print("\033[?1049h\033[?25l");
        System.out.flush();

        // Platform-specific input becomes common names such as ENTER and UP.
        // Input exceptions are currently swallowed: a failed reader can leave
        // the screen running without working controls. This is existing behavior.
        Thread inputThread = new Thread(() -> {
            try {
                if (isWindows) readWindowsKeys();
                else readUnixKeys();
            } catch (Exception ignored) {
            }
        }, "TUI-Input");
        // A daemon thread alone cannot keep the JVM alive after the main thread
        // finishes. stop() changes a flag; it does not unblock a pending input read.
        inputThread.setDaemon(true);
        inputThread.start();

        try {
            long tickCount = 0;
            while (running) {
                String key;
                // poll() returns null immediately when the queue is empty.
                // All actions in this batch run before the next render.
                while ((key = keyQueue.poll()) != null) {
                    handleKey(key);
                }

                // Unix resizing is checked every fifth tick, including tick zero.
                // Windows size updates arrive through its input reader instead.
                if (!isWindows && tickCount % 5 == 0) {
                    updateUnixTermSize();
                }
                for (Runnable tick : tickListeners) {
                    tick.run();
                }
                renderScreen();
                tickCount++;
                Thread.sleep(50);
            }
        } catch (InterruptedException ignored) {
        } finally {
            running = false;
            // Restore usable terminal input on a normal exit or an exception
            // leaving this loop. "stty sane" restores defaults, not an exact
            // snapshot of the user's previous settings. Forced process termination
            // or an OS signal does not guarantee this finally block will run.
            if (!isWindows) {
                try {
                    execSh("stty sane < /dev/tty");
                } catch (Exception ignored) {
                }
            }
            System.out.print("\033[?25h\033[?1049l");
            System.out.flush();
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
        StringBuilder sb = new StringBuilder();
        // Hide cursor, move to home, clear screen. These are only sent when the
        // completed frame differs from lastFrame; building the string does no I/O.
        sb.append("\033[?25l\033[H\033[2J");

        // Top border changes style depending on which UI screen (Z-layer) is active
        String borderChar = (currentZ == 0) ? "-" : "~";
        String borderColor = (currentZ == 0) ? "\033[36m" : "\033[33m";
        String topBorder = borderColor + "+" + borderChar.repeat(Math.max(0, termWidth - 2)) + "+\033[0m";
        sb.append("\033[1;1H").append(topBorder);

        for (Element el : elements) {
            if (el.z != currentZ || !el.visible) continue;
            boolean isFocused = (el == focusedElement);
            boolean isEditing = isFocused && editing;
            // Find the nearest visible widget anchored to the right on the same
            // row. Pass that boundary to render(). This is a clipping hint, not a
            // full layout engine: Button/Input do not honor it, and multi-row
            // overlap is not detected.
            int renderWidth = termWidth;
            for (Element other : elements) {
                if (other.z == currentZ && other.visible && other.y == el.y
                        && other.x > el.x && other.x < renderWidth) {
                    renderWidth = other.x;
                }
            }
            String out = el.render(isFocused, isEditing, renderWidth);
            sb.append(String.format("\033[%d;%dH%s", el.y + 1, el.x + 1, out));
        }

        // Clip the footer text against the known terminal width. The minimum
        // row and width assume a reasonably sized terminal; very small windows
        // can still wrap or scroll. Layer labels are inherited from the old demo.
        int footerRow = Math.max(19, termHeight - 1);
        int maxFooterWidth = Math.max(10, termWidth - 2);
        String zTag = (currentZ == 0) ? " [MAIN UI] " : " [SHOP UI] ";
        String rawFooter = " " + statusText;
        if (rawFooter.length() + zTag.length() < maxFooterWidth) {
            rawFooter = rawFooter + " ".repeat(maxFooterWidth - rawFooter.length() - zTag.length()) + zTag;
        } else if (rawFooter.length() > maxFooterWidth) {
            rawFooter = rawFooter.substring(0, maxFooterWidth);
        }
        sb.append(String.format("\033[%d;1H\033[7m%s\033[0m", footerRow, rawFooter));

        // While editing, show the real terminal cursor at the end of the visible
        // text. The +5 accounts for the input's prefix and ANSI's one-based column.
        // This assumes characters occupy one terminal column.
        if (editing && focusedElement instanceof Input input) {
            int visibleLen = Math.min(input.value.length(), input.width);
            int cursorX = input.x + input.label.length() + 5 + visibleLen;
            int cursorY = input.y + 1;
            sb.append(String.format("\033[%d;%dH\033[?25h", cursorY, cursorX));
        }

        String frame = sb.toString();
        if (!frame.equals(lastFrame)) {
            System.out.print(frame);
            System.out.flush();
            lastFrame = frame;
        }
    }

    // UNIX INPUT
    // The reader blocks on /dev/tty in the background thread. It is the UI loop,
    // not this read operation, that stays responsive while waiting for a key.
    private void readUnixKeys() throws Exception {
        // Disable automatic echo and line buffering so keys arrive immediately.
        // This is not full raw mode: signal handling remains enabled, so Ctrl+C
        // may be delivered as an OS interrupt rather than byte 3 below.
        // /dev/tty requires a controlling terminal; ordinary redirected stdin
        // alone is not enough for this reader.
        execSh("stty -echo -icanon min 1 time 0 < /dev/tty");
        try (InputStream in = new FileInputStream("/dev/tty")) {
            while (running) {
                int c = in.read();
                if (c == -1) break;
                if (c == 3) keyQueue.offer("CTRL_C");
                else if (c == 10 || c == 13) keyQueue.offer("ENTER");
                else if (c == 127 || c == 8) keyQueue.offer("BACKSPACE");
                // ESC can mean the Escape key or the start of an arrow/page
                // sequence. A short delay gives remaining bytes time to arrive.
                // This simple timing-based parser does not cover every terminal.
                else if (c == 27) {
                    Thread.sleep(15);
                    if (in.available() > 0) {
                        int next1 = in.read();
                        if (next1 == '[' && in.available() > 0) {
                            int next2 = in.read();
                            // Common sequences are ESC [ A/B/C/D for arrows
                            // and ESC [ 5~/6~ for Page Up/Down. The page cases
                            // consume one more byte, assuming it is the '~'.
                            switch (next2) {
                                case 'A' -> keyQueue.offer("UP");
                                case 'B' -> keyQueue.offer("DOWN");
                                case 'C' -> keyQueue.offer("RIGHT");
                                case 'D' -> keyQueue.offer("LEFT");
                                case '5' -> {
                                    in.read();
                                    keyQueue.offer("PGUP");
                                }
                                case '6' -> {
                                    in.read();
                                    keyQueue.offer("PGDN");
                                }
                            }
                        }
                    } else {
                        keyQueue.offer("ESC");
                    }
                } else {
                    // This casts one byte to a character; it does not decode
                    // multibyte UTF-8. Combined with handleKey's ASCII restriction,
                    // this is why accented keyboard input needs future work.
                    keyQueue.offer(String.valueOf((char) c));
                }
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
                termHeight = Integer.parseInt(parts[0]);
                termWidth = Integer.parseInt(parts[1]);
            }
        } catch (Exception ignored) {
        }
    }

    // Run an internal shell command and capture stdout. Current callers pass
    // fixed commands, never player input. This helper waits for completion and
    // does not inspect the exit code or capture stderr.
    private String execSh(String cmd) throws Exception {
        Process p = new ProcessBuilder("sh", "-c", cmd).start();
        String out = new String(p.getInputStream().readAllBytes());
        p.waitFor();
        return out;
    }

    // WINDOWS INPUT
    // A PowerShell child process reads console keys and sends line-based messages
    // to Java. This avoids trying to decode Windows console events as Unix bytes.
    private void readWindowsKeys() throws Exception {
        // Protocol: SIZE,width,height while idle; KEY,width,height,name,char
        // after a key press. The idle sleep avoids a busy loop in PowerShell.
        String psCmd = "while($true) { " +
                "$w = $Host.UI.RawUI.WindowSize.Width; $h = $Host.UI.RawUI.WindowSize.Height; " +
                "if ([Console]::KeyAvailable) { " +
                "  $k = [Console]::ReadKey($true); " +
                "  [Console]::WriteLine(\"KEY,$w,$h,$($k.Key),$($k.KeyChar)\"); " +
                "} else { " +
                "  [Console]::WriteLine(\"SIZE,$w,$h\"); " +
                "  Start-Sleep -Milliseconds 50; " +
                "} }";
        // Inherit console input so PowerShell can read keys. Its stdout remains
        // a pipe, consumed below by the Java input thread.
        Process p = new ProcessBuilder("powershell", "-NoProfile", "-Command", psCmd)
                .redirectInput(ProcessBuilder.Redirect.INHERIT)
                .start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while (running && (line = reader.readLine()) != null) {
                if (line.startsWith("SIZE,")) {
                    String[] parts = line.split(",");
                    if (parts.length >= 3) {
                        termWidth = Integer.parseInt(parts[1]);
                        termHeight = Integer.parseInt(parts[2]);
                    }
                } else if (line.startsWith("KEY,")) {
                    // Limit splitting so a typed comma stays in the last field.
                    // Width and height are refreshed on key events as well.
                    String[] parts = line.split(",", 5);
                    if (parts.length >= 5) {
                        termWidth = Integer.parseInt(parts[1]);
                        termHeight = Integer.parseInt(parts[2]);
                        String keyName = parts[3];
                        String keyChar = parts[4];

                        // Translate platform names into the same vocabulary as
                        // the Unix reader. Modifiers and Ctrl+C have no dedicated
                        // mapping in this protocol; handling depends on the host.
                        switch (keyName) {
                            case "UpArrow"    -> keyQueue.offer("UP");
                            case "DownArrow"  -> keyQueue.offer("DOWN");
                            case "LeftArrow"  -> keyQueue.offer("LEFT");
                            case "RightArrow" -> keyQueue.offer("RIGHT");
                            case "Enter"      -> keyQueue.offer("ENTER");
                            case "Escape"     -> keyQueue.offer("ESC");
                            case "Backspace"  -> keyQueue.offer("BACKSPACE");
                            case "PageUp"     -> keyQueue.offer("PGUP");
                            case "PageDown"   -> keyQueue.offer("PGDN");
                            default -> {
                                if (!keyChar.isEmpty()) keyQueue.offer(keyChar);
                            }
                        }
                    }
                }
            }
        } finally {
            // Once the reader leaves its loop, terminate the child process too.
            // Changing running alone does not directly interrupt readLine().
            p.destroyForcibly();
        }
    }
}
