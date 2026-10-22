import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class MiniTui {

    // --- UI WIDGETS ---
    public static abstract class Element {
        public volatile int x, y, z;
        public volatile boolean focusable;
        public volatile boolean visible = true;

        public Element(int x, int y, int z, boolean focusable) {
            this.x = x; this.y = y; this.z = z;
            this.focusable = focusable;
        }
        public abstract String render(boolean focused, boolean editing, int termWidth);
    }

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
        public void setText(String text) { this.text = text; }

        @Override
        public String render(boolean focused, boolean editing, int termWidth) {
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

    // Hollow ASCII Box that clamps to terminal width and never overwrites inner widgets
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

    public static class Label extends Element {
        private volatile String text;
        public Label(int x, int y, int z, String text) {
            super(x, y, z, false);
            this.text = text;
        }
        public void setText(String text) { this.text = text; }
        public String getText() { return text; }

        @Override
        public String render(boolean focused, boolean editing, int termWidth) {
            int maxLen = Math.max(1, termWidth - x - 2);
            return text.length() > maxLen ? text.substring(0, maxLen) : text;
        }
    }

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

        public String getValue() { return value; }
        public void setValue(String val) { this.value = val; }
        public Input onChange(Consumer<String> cb) { this.onChange = cb; return this; }
        public Input onSubmit(Consumer<String> cb) { this.onSubmit = cb; return this; }

        @Override
        public String render(boolean focused, boolean editing, int termWidth) {
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

    public static class Button extends Element {
        public volatile String text;
        public Runnable action;

        public Button(int x, int y, int z, String text, Runnable action) {
            super(x, y, z, true);
            this.text = text;
            this.action = action;
        }

        public void setText(String text) { this.text = text; }

        @Override
        public String render(boolean focused, boolean editing, int termWidth) {
            if (focused) {
                return "\033[7;32m < " + text + " > \033[0m";
            }
            return " [ " + text + " ] ";
        }
    }

    // --- ENGINE STATE ---
    private final List<Element> elements = new CopyOnWriteArrayList<>();
    private final BlockingQueue<String> keyQueue = new LinkedBlockingQueue<>();
    private final List<Runnable> tickListeners = new CopyOnWriteArrayList<>();

    private volatile Element focusedElement = null;
    private volatile boolean editing = false;
    private volatile boolean running = false;
    private volatile int currentZ = 0;
    private volatile int termWidth = 80;
    private volatile int termHeight = 24;
    private volatile String statusText = "Arrows: Move | Enter: Edit/Click | [ / ]: Switch Screen | Ctrl+C: Quit";
    private String lastFrame = "";

    public <T extends Element> T add(T el) {
        elements.add(el);
        if (focusedElement == null && el.focusable && el.z == currentZ && el.visible) {
            focusedElement = el;
        }
        return el;
    }

    public void onTick(Runnable callback) { tickListeners.add(callback); }
    public void setStatus(String status) { this.statusText = status; }
    public int getTerminalWidth() { return termWidth; }
    public int getTerminalHeight() { return termHeight; }

    public void setLayer(int z) {
        this.currentZ = z;
        this.editing = false;
        this.focusedElement = null;
        ensureFocusOnCurrentLayer();
    }
    public int getLayer() { return currentZ; }
    public void stop() { this.running = false; }

    public void start() {
        running = true;
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        System.out.print("\033[?1049h\033[?25l");
        System.out.flush();

        Thread inputThread = new Thread(() -> {
            try {
                if (isWindows) readWindowsKeys();
                else readUnixKeys();
            } catch (Exception ignored) {}
        }, "TUI-Input");
        inputThread.setDaemon(true);
        inputThread.start();

        try {
            long tickCount = 0;
            while (running) {
                String key;
                while ((key = keyQueue.poll()) != null) {
                    handleKey(key);
                }
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
            if (!isWindows) {
                try { execSh("stty sane < /dev/tty"); } catch (Exception ignored) {}
            }
            System.out.print("\033[?25h\033[?1049l");
            System.out.flush();
        }
    }

    private void handleKey(String key) {
        if (key.equals("CTRL_C")) {
            running = false;
            return;
        }

        if (editing && focusedElement instanceof Input input) {
            if (key.equals("ENTER") || key.equals("ESC")) {
                editing = false;
                if (key.equals("ENTER") && input.onSubmit != null) {
                    input.onSubmit.accept(input.value);
                }
            } else if (key.equals("BACKSPACE")) {
                if (!input.value.isEmpty()) {
                    input.value = input.value.substring(0, input.value.length() - 1);
                    if (input.onChange != null) input.onChange.accept(input.value);
                }
            } else if (key.length() == 1 && key.charAt(0) >= 32 && key.charAt(0) <= 126) {
                input.value += key;
                if (input.onChange != null) input.onChange.accept(input.value);
            }
            return;
        }

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

    private boolean hasElementsAtZ(int z) {
        return elements.stream().anyMatch(e -> e.z == z && e.visible);
    }

    private void ensureFocusOnCurrentLayer() {
        if (focusedElement != null && focusedElement.z == currentZ && focusedElement.focusable && focusedElement.visible) return;
        focusedElement = elements.stream()
                .filter(e -> e.z == currentZ && e.focusable && e.visible)
                .findFirst()
                .orElse(null);
    }

    private void moveFocus(int dx, int dy) {
        ensureFocusOnCurrentLayer();
        if (focusedElement == null) return;

        Element best = null;
        double bestScore = Double.MAX_VALUE;

        for (Element el : elements) {
            if (!el.focusable || !el.visible || el == focusedElement || el.z != currentZ) continue;

            int relX = el.x - focusedElement.x;
            int relY = el.y - focusedElement.y;

            if (dx != 0 && Integer.signum(relX) != dx) continue;
            if (dy != 0 && Integer.signum(relY) != dy) continue;

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

    private void renderScreen() {
        ensureFocusOnCurrentLayer();
        StringBuilder sb = new StringBuilder();
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

        // Safe footer bar that truncates to termWidth so it never wraps/scrolls
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

        // FIXED CURSOR POSITION:
        // The cursor belongs immediately after the visible text, before the closing ']'.
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

    // --- NON-BLOCKING UNIX INPUT READER ---
    private void readUnixKeys() throws Exception {
        execSh("stty -echo -icanon min 1 time 0 < /dev/tty");
        try (InputStream in = new FileInputStream("/dev/tty")) {
            while (running) {
                int c = in.read();
                if (c == -1) break;
                if (c == 3) keyQueue.offer("CTRL_C");
                else if (c == 10 || c == 13) keyQueue.offer("ENTER");
                else if (c == 127 || c == 8) keyQueue.offer("BACKSPACE");
                else if (c == 27) {
                    Thread.sleep(15);
                    if (in.available() > 0) {
                        int next1 = in.read();
                        if (next1 == '[' && in.available() > 0) {
                            int next2 = in.read();
                            switch (next2) {
                                case 'A' -> keyQueue.offer("UP");
                                case 'B' -> keyQueue.offer("DOWN");
                                case 'C' -> keyQueue.offer("RIGHT");
                                case 'D' -> keyQueue.offer("LEFT");
                                case '5' -> { in.read(); keyQueue.offer("PGUP"); }
                                case '6' -> { in.read(); keyQueue.offer("PGDN"); }
                            }
                        }
                    } else {
                        keyQueue.offer("ESC");
                    }
                } else {
                    keyQueue.offer(String.valueOf((char) c));
                }
            }
        }
    }

    private void updateUnixTermSize() {
        try {
            String out = execSh("stty size < /dev/tty").trim();
            String[] parts = out.split("\\s+");
            if (parts.length == 2) {
                termHeight = Integer.parseInt(parts[0]);
                termWidth = Integer.parseInt(parts[1]);
            }
        } catch (Exception ignored) {}
    }

    private String execSh(String cmd) throws Exception {
        Process p = new ProcessBuilder("sh", "-c", cmd).start();
        String out = new String(p.getInputStream().readAllBytes());
        p.waitFor();
        return out;
    }

    // --- NON-BLOCKING WINDOWS INPUT & RESIZE READER ---
    private void readWindowsKeys() throws Exception {
        String psCmd = "while($true) { " +
                "$w = $Host.UI.RawUI.WindowSize.Width; $h = $Host.UI.RawUI.WindowSize.Height; " +
                "if ([Console]::KeyAvailable) { " +
                "  $k = [Console]::ReadKey($true); " +
                "  [Console]::WriteLine(\"KEY,$w,$h,$($k.Key),$($k.KeyChar)\"); " +
                "} else { " +
                "  [Console]::WriteLine(\"SIZE,$w,$h\"); " +
                "  Start-Sleep -Milliseconds 50; " +
                "} }";
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
                    String[] parts = line.split(",", 5);
                    if (parts.length >= 5) {
                        termWidth = Integer.parseInt(parts[1]);
                        termHeight = Integer.parseInt(parts[2]);
                        String keyName = parts[3];
                        String keyChar = parts[4];

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
            p.destroyForcibly();
        }
    }
}