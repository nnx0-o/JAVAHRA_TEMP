import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

// tady se cte klavesnice a kresli ui
// nejdriv prvky pak loop a terminal
// co buttony delaji posila main
// input jen posila klavesy hlavni loop je zpracuje
public class MiniTui {

    // barvy a ansi styly

    // styly jsou jen stringy muzeme je poskladat za sebe
    // zalezi co terminal umi
    public static final class Color {
        private Color() {}

        public static final String RESET = "\033[0m";

        // barvy textu
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

        // pozadi jde kombinovat s textem a styly
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

        // jen spojime kody za sebe
        // posledni kod ma prednost
        public static String combine(String... codes) {
            StringBuilder result = new StringBuilder();
            for (String code : codes) result.append(Objects.requireNonNull(code, "code"));
            return result.toString();
        }

        // styl pred text a reset za nej
        public static String apply(String text, String... codes) {
            return combine(codes) + Objects.requireNonNull(text, "text") + RESET;
        }

        // barva textu z palety
        public static String foreground(int index) {
            return sgr(38, 5, channel(index));
        }

        // barva pozadi z palety
        public static String background(int index) {
            return sgr(48, 5, channel(index));
        }

        // text podle rgb
        public static String foreground(int red, int green, int blue) {
            return sgr(38, 2, channel(red), channel(green), channel(blue));
        }

        // pozadi podle rgb
        public static String background(int red, int green, int blue) {
            return sgr(48, 2, channel(red), channel(green), channel(blue));
        }

        // styl muzeme zadat i primo cisly
        // bez parametru je to reset
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

    // ui prvky

    // prvky muzeme vytvorit predem a pridat az pak
    // x a y od nuly z je screen
    // kreslime viditelne ovladame jen vybiratelne
    public static abstract class Element {
        // zmenu musi videt i ostatni thready
        // cely update tim atomicky neni akce porad patri do hlavniho loopu
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

        // puvodni text bez vseho kolem
        // u inputu hodnota u ramecku nadpis
        // vlastni prvek si to muze prepsat
        public String getText() {
            return "";
        }

        // tady jen pripravime text
        // vytiskne se to az cele najednou
        // soused vpravo muze sirku zkratit
        public abstract String render(boolean focused, boolean editing, int termWidth);
    }

    // nadpis

    // nadpis se nevybira
    // bez barvy vezmeme default
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
            // nejdriv misto na okraje pak text
            // tady pocitame java znaky ne bunky terminalu
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

    // ramecek

    // kazdy kus ramecku posleme na jeho souradnice
    // vnitrek zustane pro ostatni prvky
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

            // horni hrana
            String top = w < 6 ? "+" + "-".repeat(w - 2) + "+"
                    : "+- " + t + " " + "-".repeat(Math.max(0, w - t.length() - 5)) + "+";
            // nejdriv kurzor na spravne misto
            // terminal zacina od jednicky
            sb.append(String.format("\033[%d;%dH%s", y + 1, x + 1, top));

            // jen boky vnitrek neprepisujeme
            for (int i = 1; i < height - 1; i++) {
                sb.append(String.format("\033[%d;%dH|", y + 1 + i, x + 1));
                sb.append(String.format("\033[%d;%dH|", y + 1 + i, x + w));
            }

            // dolni hrana
            String bot = "+" + "-".repeat(Math.max(0, w - 2)) + "+";
            sb.append(String.format("\033[%d;%dH%s", y + height, x + 1, bot)).append(Color.RESET);
            return sb.toString();
        }
    }

    // label

    // jen text neni na co klikat
    // main sem doplni aktualni text
    public static class Label extends Element {
        private volatile String text;
        public volatile String colorCode = Color.DEFAULT;

        public Label(int x, int y, int z, String text) {
            super(x, y, z, false);
            this.text = text;
        }

        public Label(int x, int y, int z, String text, String colorCode) {
            this(x, y, z, text);
            this.colorCode = colorCode;
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
            return colorCode + fitCell(text, Math.max(0, termWidth - x - 1), false) + Color.RESET;
        }
    }

    // input

    // vybrat input a psat do nej jsou dva rezimy
    // enter prepne psani callback dostane text
    public static class Input extends Element {
        public volatile String label;
        public volatile String value;
        public volatile int width;
        public Consumer<String> onChange;
        public Consumer<String> onSubmit;
        public volatile String colorCode = Color.CYAN;

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

        // kdyz to nastavime primo callback nevolame
        // zmena hned submit az enterem
        // vracime sebe at jde chainovat
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
            // sirka je jen na zobrazeni text muze byt delsi
            // u dlouheho textu ukazeme konec zbytek orizne render
            int fieldWidth = Math.max(1, Math.min(width, termWidth - x - label.length() - 7));
            String displayVal = value.length() > fieldWidth
                    ? value.substring(value.length() - fieldWidth)
                    : String.format("%-" + fieldWidth + "s", value);

            if (editing) {
                return " " + label + ": " + Color.apply("[" + displayVal + "]", Color.REVERSE, colorCode) + " ";
            } else if (focused) {
                return Color.apply(" " + label + ": [" + displayVal + "] ", Color.REVERSE, colorCode);
            } else {
                return " " + label + ": [" + displayVal + "] ";
            }
        }
    }

    // button

    // button jen zavola svoji akci
    // akce az po enteru pri kresleni ne
    public static class Button extends Element {
        public volatile String text;
        public Runnable action;
        public volatile String colorCode;
        public volatile boolean enabled = true;
        public volatile boolean primary = false;

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
            String accent = colorCode == null ? Color.WHITE : colorCode;
            String style = enabled ? accent : Color.BRIGHT_BLACK;
            String marker = focused ? " > " : "   ";
            return Color.apply(marker, focused ? Color.REVERSE : "", style)
                    + Color.apply((primary ? "[ " : "") + text + (primary ? " ]" : ""),
                    primary ? Color.BOLD : "", style);
        }
    }

    // radek tabulky

    // zvyraznime jen ukazatel barvy sloupcu nechame
    public static class TableRow extends Button {
        private final List<String> cells;
        private final List<Integer> widths;
        private final List<String> colors;
        public volatile String backgroundCode = "";

        public TableRow(int x, int y, int z, String name, List<String> cells,
                        List<Integer> widths, List<String> colors, Runnable action) {
            super(x, y, z, name, action);
            if (cells.size() != widths.size() || cells.size() != colors.size()) {
                throw new IllegalArgumentException("Pocet sloupcu neodpovida jejich nastaveni");
            }
            this.cells = List.copyOf(cells);
            this.widths = List.copyOf(widths);
            this.colors = List.copyOf(colors);
        }

        @Override
        public String render(boolean focused, boolean editing, int termWidth) {
            String accent = colorCode == null ? Color.WHITE : colorCode;
            StringBuilder result = new StringBuilder(Color.apply(focused ? " > " : "   ",
                    backgroundCode, focused ? Color.REVERSE : "", enabled ? accent : Color.BRIGHT_BLACK));
            int available = Math.max(0, termWidth - x - 3);
            for (int index = 0; index < cells.size() && available > 0; index++) {
                int size = Math.min(Math.max(0, widths.get(index)), available);
                // po resetu vratime pozadi at je souvisle
                result.append(backgroundCode).append(colors.get(index))
                        .append(fitCell(cells.get(index), size, true)).append(Color.RESET);
                available -= size;
                if (available > 0 && index + 1 < cells.size()) {
                    result.append(backgroundCode).append(' ').append(Color.RESET);
                    available--;
                }
            }
            return result.toString();
        }
    }

    // text do jedne bunky

    // pocitame viditelne bunky z ansi bereme jen styly
    private static String fitCell(String text, int width, boolean pad) {
        StringBuilder result = new StringBuilder();
        int columns = 0;
        for (int index = 0; index < text.length();) {
            int point = text.codePointAt(index);
            if (point == 27) {
                int end = index + 1;
                if (end < text.length() && text.charAt(end) == '[') {
                    end++;
                    while (end < text.length() && (text.charAt(end) < '@' || text.charAt(end) > '~')) end++;
                    if (end < text.length()) {
                        String sequence = text.substring(index, end + 1);
                        if (sequence.matches("\\u001B\\[[0-9;:]*m")) result.append(sequence);
                        index = end + 1;
                        continue;
                    }
                }
                index++;
                continue;
            }
            index += Character.charCount(point);
            if (Character.isISOControl(point)) continue;
            int size = displayColumns(point);
            if (columns + size > width) break;
            result.appendCodePoint(point);
            columns += size;
        }
        if (pad) result.append(" ".repeat(Math.max(0, width - columns)));
        return result.toString();
    }

    // co si loop drzi

    // pri zmene vznikne kopie loop muze jet dal
    // cteme casto pridavame malo
    private final List<Element> elements = new CopyOnWriteArrayList<>();

    // sem chodi klavesy z input threadu
    // loop si je vyzvedne bez cekani
    private final BlockingQueue<String> keyQueue = new LinkedBlockingQueue<>();
    private final Map<String, Runnable> keyCallbacks = new ConcurrentHashMap<>();
    private volatile String theme = Color.WHITE;
    private final List<Runnable> tickListeners = new CopyOnWriteArrayList<>();

    // co je vybrane a jestli se zrovna pise
    // zatim 80 na 24 pak zjistime skutecnou velikost
    private volatile Element focusedElement = null;
    private volatile boolean editing = false;
    private volatile boolean running = false;
    private volatile int currentZ = 0;

    // rozmery a posun menime spolu
    // i rychle zmenseni a zvetseni musi vyvolat refresh
    private record TerminalGeometry(int width, int height, int left, int top) {}
    private volatile TerminalGeometry terminal = new TerminalGeometry(80, 24, 0, 0);
    private volatile String statusText = "Šipky: výběr | Enter: potvrdit | [ / ]: obrazovka | Ctrl+C: konec";

    // cache si resi jen hlavni loop
    // kdyz se obraz nezmenil netiskneme ho znovu
    private String lastFrame = "";
    private TerminalGeometry lastRenderedGeometry;
    private volatile Process windowsProcess;
    private volatile int windowsOutputMode = -1;
    private volatile int windowsInputMode = -1;
    private volatile Exception inputFailure;
    private String unixTerminalMode;
    private boolean terminalRestored;
    private volatile boolean terminalSizeReported;

    // pridat a smazat prvky

    // vratime pridany prvek se stejnym typem
    // prvni co jde vybrat rovnou vybereme
    public <T extends Element> T add(T el) {
        if (el instanceof Button button && button.colorCode == null) button.colorCode = theme;
        if (el instanceof Input input) input.colorCode = theme;
        elements.add(el);
        if (focusedElement == null && el.focusable && el.z == currentZ && el.visible) {
            focusedElement = el;
        }
        return el;
    }

    public void clear() {
        elements.clear(); // stare prvky pryc
        focusedElement = null;
        editing = false;
    }

    // callbacky barvy a klavesy

    // tohle zavolame pred kreslenim
    // refresh neni dalsi herni tah
    public void onTick(Runnable callback) {
        tickListeners.add(callback);
    }

    public void setTheme(String accent) {
        theme = Objects.requireNonNull(accent);
        for (Element element : elements) {
            if (element instanceof Button button) button.colorCode = theme;
            if (element instanceof Input input) input.colorCode = theme;
        }
    }

    public String getTheme() {
        return theme;
    }

    public void onKey(String key, Runnable callback) {
        if (callback == null) keyCallbacks.remove(key);
        else keyCallbacks.put(key, callback);
    }

    // focus a paticka

    public void focus(Element element) {
        if (element != null && elements.contains(element) && element.focusable
                && element.visible && element.z == currentZ) focusedElement = element;
    }

    public void setStatus(String status) {
        this.statusText = status;
    }

    public String getStatus() {
        return statusText;
    }

    // co je v ui a jak je velky terminal

    // kopie seznamu prvky uvnitr jsou porad stejne
    public List<Element> getElements() {
        return List.copyOf(elements);
    }

    // co je vybrane pripadne nic
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

    // prepinani screenu

    // novy screen ukonci psani a dostane novy focus
    // i kdyz nastavime stejny screen
    public void setLayer(int z) {
        this.currentZ = z;
        this.editing = false;
        this.focusedElement = null;
        ensureFocusOnCurrentLayer();
    }

    public int getLayer() {
        return currentZ;
    }

    // spustit a ukoncit ui

    public void stop() {
        this.running = false;
    }

    // input a rozmery hlidame i kdyz loop zrovna nic nedela
    // callbacky zustavaji v hlavnim loopu
    public void start() {
        if (running) throw new IllegalStateException("MiniTui už běží");
        running = true;
        boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");
        terminalRestored = false;
        terminalSizeReported = false;
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
                // ctrl c vezmeme jako klavesu a pak normalne uklidime
                execSh("stty -echo -icanon -isig min 1 time 0 < /dev/tty");
                updateUnixTermSize();
            }
            inputThread.start();
            // na windows nejdriv zapneme escape sekvence
            if (isWindows && !ready.await(5, TimeUnit.SECONDS)) {
                throw new IOException("Vypršel čas na otevření konzole Windows");
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
            throw new IllegalStateException("MiniTui potřebuje funkční interaktivní terminál", failure);
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
                // ukonceni uz bezi uklid se udela jen jednou
            }
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    // vratime terminal do puvodniho stavu

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
            // konzole uz mezitim mohla byt zavrena
        } finally {
            unixTerminalMode = null;
            windowsOutputMode = windowsInputMode = -1;
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    // co s prislymi klavesami

    // tady uz jsou klavesy stejne na obou systemech
    // bud piseme nebo ovladame menu
    private void handleKey(String key) {
        // rozmer muze prijit i jako pozice kurzoru v rohu
        // tohle odpovida terminal neni to psany text
        if (key.startsWith("\033[8;") && key.endsWith("t")
                || key.startsWith("\033[") && key.endsWith("R")) {
            try {
                String[] size = key.substring(key.endsWith("R") ? 2 : 4, key.length() - 1).split(";", -1);
                if (size.length == 2) {
                    int height = Integer.parseInt(size[0]);
                    int width = Integer.parseInt(size[1]);
                    if (width > 0 && height > 0 && width <= 10000 && height <= 10000) {
                        terminalSizeReported = true;
                        updateTerminalGeometry(width, height, 0, 0);
                    }
                }
            } catch (NumberFormatException ignored) {
            }
            return;
        }
        if (key.equals("CTRL_C")) {
            running = false;
            return;
        }

        Runnable helpShortcut = key.equals("F1") ? keyCallbacks.get(key) : null;
        if (helpShortcut != null) {
            helpShortcut.run(); // napovedu pustime i behem psani
            return;
        }

        if (editing && focusedElement instanceof Input input) {
            if (key.equals("ENTER") || key.equals("ESC")) {
                editing = false;
                // escape prestane psat rozepsany text necha
                if (key.equals("ENTER") && input.onSubmit != null) {
                    input.onSubmit.accept(input.value);
                }
            } else if (key.equals("BACKSPACE")) {
                if (!input.value.isEmpty()) {
                    input.value = input.value.substring(0, input.value.offsetByCodePoints(input.value.length(), -1));
                    if (input.onChange != null) input.onChange.accept(input.value);
                }
            // normalni text i s diakritikou
            // ridici znaky vynechame
            } else if (key.codePointCount(0, key.length()) == 1
                    && !Character.isISOControl(key.codePointAt(0))) {
                input.value += key;
                if (input.onChange != null) input.onChange.accept(input.value);
            }
            return;
        }

        Runnable shortcut = keyCallbacks.get(key);
        if (shortcut != null) {
            shortcut.run();
            return;
        }

        // sipky jdou podle polohy na screenu
        // na sousedni screen jen kdyz tam neco je
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
                } else if (focusedElement instanceof Button btn && btn.enabled && btn.action != null) {
                    btn.action.run();
                }
            }
        }
    }

    // kam posunout focus

    // na screenu muzou byt jen labely i to se pocita
    private boolean hasElementsAtZ(int z) {
        return elements.stream().anyMatch(e -> e.z == z && e.visible);
    }

    // kdyz focus prestal platit najdeme novy
    // kdyz neni co vybrat nechame nic
    private void ensureFocusOnCurrentLayer() {
        if (focusedElement != null && focusedElement.z == currentZ
                && focusedElement.focusable && focusedElement.visible) return;
        focusedElement = elements.stream()
                .filter(e -> e.z == currentZ && e.focusable && e.visible)
                .findFirst()
                .orElse(null);
    }

    // jedna ze ctyr sipek
    // bereme pozici prvku ne jeho velikost
    private void moveFocus(int dx, int dy) {
        ensureFocusOnCurrentLayer();
        if (focusedElement == null) return;

        Element best = null;
        double bestScore = Double.MAX_VALUE;

        for (Element el : elements) {
            if (!el.focusable || !el.visible || el == focusedElement || el.z != currentZ) continue;

            int relX = el.x - focusedElement.x;
            int relY = el.y - focusedElement.y;

            // co je jinym smerem preskocime
            if (dx != 0 && Integer.signum(relX) != dx) continue;
            if (dy != 0 && Integer.signum(relY) != dy) continue;

            // nejdriv co je blizko v tom smeru
            // cim vic bokem tim hur pri shode vyhraje prvni
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

    // slozime obraz a pripadne vytiskneme

    // nejdriv slozit cele pak vytisknout
    private void renderScreen() {
        ensureFocusOnCurrentLayer();
        TerminalGeometry geometry = terminal;
        int termWidth = geometry.width();
        int termHeight = geometry.height();
        int footerRow = termHeight - 1;
        StringBuilder sb = new StringBuilder();
        // schovat kurzor nahoru a smazat
        // odesleme az kdyz je opravdu co menit
        sb.append("\033[?25l\033[H\033[2J");

        // okraj podle aktualniho screenu
        String borderChar = (currentZ == 0) ? "-" : "~";
        String borderColor = theme;
        String topBorder = borderColor + (termWidth == 1 ? "+"
                : "+" + borderChar.repeat(termWidth - 2) + "+") + Color.RESET;
        if (footerRow > 0) appendClipped(sb, topBorder, 0, 0, termWidth, footerRow);

        for (Element el : elements) {
            if (el.z != currentZ || !el.visible || el.x >= termWidth || el.y >= footerRow) continue;
            boolean isFocused = (el == focusedElement);
            boolean isEditing = isFocused && editing;
            // vpravo uz muze zacinat dalsi prvek
            // plati i pro vlastni prvky a ramecky
            int renderWidth = renderBoundary(el, termWidth);
            String out = el.render(isFocused, isEditing, renderWidth);
            appendClipped(sb, out, el.x, el.y, renderWidth, footerRow);
        }

        // pravy dolni roh nechame prazdny jinak terminal muze odscrollovat
        int maxFooterWidth = Math.max(0, termWidth - 1);
        String rawFooter = " " + statusText;
        appendClipped(sb, Color.apply(rawFooter, Color.REVERSE, theme), 0, footerRow,
                maxFooterWidth, termHeight);

        // pri psani vratime kurzor na konec textu
        // ctyri znaky zabira ramecek inputu
        // tady bereme jeden znak jako jeden sloupec
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

    // kam az muze prvek kreslit

    private int renderBoundary(Element element, int width) {
        if (element instanceof Box) return width;
        for (Element other : elements) {
            if (other.z == currentZ && other.visible && other.y == element.y
                    && other.x > element.x && other.x < width) width = other.x;
        }
        return width;
    }

    // kurzor posouvame sami at neprepiseme paticku
    // styly nechame text orizneme ostatni prikazy zahodime
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
                        case 'H', 'f' -> {
                            y = first - 1;
                            x = ansiParameter(parts, 1) - 1;
                        }
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
            if (codePoint == '\n') {
                y++;
                x = 0;
                positioned = false;
                continue;
            }
            if (codePoint == '\r') {
                x = 0;
                positioned = false;
                continue;
            }
            if (codePoint == '\t') {
                x += 8 - Math.floorMod(x, 8);
                positioned = false;
                continue;
            }
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

    // ansi parametry a sirka znaku

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
        // siroke znaky a emoji vetsinou zaberou dve bunky
        if (codePoint >= 0x1100 && (codePoint <= 0x115f || codePoint == 0x2329 || codePoint == 0x232a
                || codePoint >= 0x2e80 && codePoint <= 0xa4cf && codePoint != 0x303f
                || codePoint >= 0xac00 && codePoint <= 0xd7a3 || codePoint >= 0xf900 && codePoint <= 0xfaff
                || codePoint >= 0xfe10 && codePoint <= 0xfe19 || codePoint >= 0xfe30 && codePoint <= 0xfe6f
                || codePoint >= 0xff00 && codePoint <= 0xff60 || codePoint >= 0xffe0 && codePoint <= 0xffe6
                || codePoint >= 0x1f300 && codePoint <= 0x1faff || codePoint >= 0x20000 && codePoint <= 0x3fffd)) return 2;
        return 1;
    }

    // input na unixu

    // na ukonceni nechceme cekat az nekdo zmackne klavesu
    private void readUnixKeys() throws Exception {
        UnixKeyDecoder decoder = new UnixKeyDecoder(keyQueue::offer);
        long nextSizeCheck = System.nanoTime();
        try (InputStream in = new FileInputStream("/dev/tty")) {
            while (running) {
                for (int count = 0; count < 256 && running && in.available() > 0; count++) {
                    int c = in.read();
                    if (c < 0) throw new EOFException("Vstup terminálu byl uzavřen");
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

    // zbytek sekvence muze prijit az priste
    // neznamou sekvenci zahodime celou
    private static final class UnixKeyDecoder {
        private final Consumer<String> keys;
        private final StringBuilder escape = new StringBuilder();
        private final ByteArrayOutputStream utf8 = new ByteArrayOutputStream();
        private int remainingBytes;
        private long deadline;

        UnixKeyDecoder(Consumer<String> keys) {
            this.keys = keys;
        }

        void accept(int c, long now) {
            if (escape.isEmpty() && (remainingBytes > 0 || c >= 128)) {
                if (remainingBytes > 0 && (c & 0xc0) == 0x80) {
                    utf8.write(c); // znak muze dorazit po kouskach
                    if (--remainingBytes == 0) {
                        keys.accept(utf8.toString(StandardCharsets.UTF_8));
                        utf8.reset();
                    }
                    return;
                }
                utf8.reset();
                remainingBytes = c >= 0xc2 && c <= 0xdf ? 1
                        : c >= 0xe0 && c <= 0xef ? 2 : c >= 0xf0 && c <= 0xf4 ? 3 : 0;
                if (remainingBytes > 0) utf8.write(c);
                if (c >= 128) return;
            }
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
                        case 'P' -> "F1";
                        case 'R' -> escape.toString().startsWith("\033[") ? escape.toString() : null;
                        case 't' -> escape.toString().startsWith("\033[8;") ? escape.toString() : null;
                        case '~' -> switch (escape.substring(2, escape.length() - 1).split(";", 2)[0]) {
                            case "11" -> "F1";
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

    // zjistime rozmery terminalu

    // stty jako zaloha pak se zeptame terminalu
    // ve vscode treba projde dotaz na kurzor i kdyz velikost ne
    private void updateUnixTermSize() {
        try {
            if (!terminalSizeReported) {
                String out = execSh("stty size < /dev/tty").trim();
                String[] parts = out.split("\\s+");
                if (parts.length == 2) {
                    updateTerminalGeometry(Integer.parseInt(parts[1]), Integer.parseInt(parts[0]), 0, 0);
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (IOException | NumberFormatException ignored) {
        }
        // po mereni vratime kurzor kde byl
        System.out.print("\033[18t\0337\033[9999;9999H\033[6n\0338");
        System.out.flush();
    }

    private synchronized void updateTerminalGeometry(int width, int height, int left, int top) {
        // minimalizovane okno nekdy vrati nuly
        // tak si zatim nechame posledni rozmery
        if (width <= 0 || height <= 0 || left < 0 || top < 0) return;
        TerminalGeometry next = new TerminalGeometry(width, height, left, top);
        if (!next.equals(terminal)) terminal = next;
    }

    // pomocne prikazy s timeoutem

    private static String execSh(String cmd) throws IOException, InterruptedException {
        return runCommand(new ProcessBuilder("sh", "-c", cmd), 1000);
    }

    private static String runCommand(ProcessBuilder builder, long timeoutMillis) throws IOException, InterruptedException {
        Process process = builder.redirectError(ProcessBuilder.Redirect.DISCARD).start();
        try {
            if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) throw new IOException("Vypršel čas na příkaz terminálu");
            if (process.exitValue() != 0) throw new IOException("Příkaz terminálu selhal: " + builder.command().get(0));
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            process.destroyForcibly();
        }
    }

    // input na windows

    // powershell vystup je pipe rozmery musime vzit z konzole
    // funguje v obycejne konzoli i windows terminalu
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
            if (running) throw new IOException("Čtení konzole Windows skončilo");
        } finally {
            process.destroyForcibly();
        }
    }

    // znaky posilame jako cisla at se nepletou s oddelovaci
    // neplatne rozmery proste preskocime
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
                    case "F1" -> "F1";
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
