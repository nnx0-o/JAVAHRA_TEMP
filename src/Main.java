import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

public class Main {

    // ------------------------------------ typy obrazovek a ulozeny navratovy pohled ------------------------------------

    private enum Screen {
        WELCOME, NAMES, HOME, DETAIL, HELP, HISTORY, PLAYER
    }

    // Uklada pouze navigaci. Samotny zapas zustava v objektu Game.
    private record View(Screen screen, int page, boolean inventory, Item item,
                        boolean offer, int player, String focusText, int focusX, int focusY) {}

    // ------------------------------------ barvy rozhrani ------------------------------------

    private static final String WHITE = MiniTui.Color.WHITE;
    private static final String MUTED = MiniTui.Color.BRIGHT_BLACK;
    private static final String GOLD = MiniTui.Color.YELLOW;
    private static final String RED = MiniTui.Color.BRIGHT_RED;
    private static final String ORANGE = MiniTui.Color.foreground(255, 170, 80);
    private static final String MINT = MiniTui.Color.foreground(112, 235, 180);

    // ------------------------------------ herni model a pomocne objekty rozhrani ------------------------------------

    private final MiniTui tui = new MiniTui();
    private final Deque<View> views = new ArrayDeque<>();
    private final GameText text = new GameText();
    private Game game;

    // ------------------------------------ otevreny pohled, vybrane predmety a strankovani ------------------------------------

    private Screen screen = Screen.WELCOME;
    private int page;
    private boolean inventoryView;
    private Item selectedItem;
    private boolean shopItem;
    private int selectedPlayer;
    private int selectedPlan = -1;
    private int planPage;
    private int helpSlice;

    // ------------------------------------ obnoveni vyberu po prekresleni ------------------------------------

    private Screen lastDrawnScreen;
    private int lastDrawnActor = -1;
    private Game.Phase lastDrawnPhase;
    private String requestedFocus;
    private int requestedX;
    private int requestedY;

    // ------------------------------------ propojeni radku vybavy s predmety ------------------------------------

    private final Map<MiniTui.Element, Item> toolkitRows = new HashMap<>();

    // ------------------------------------ rozmery terminalu a panelu ------------------------------------

    private int width;
    private int height;
    private int leftWidth;
    private int rightX;
    private int rightWidth;
    private final int panelGap = Math.max(2, Integer.getInteger("duel.panelGap", 3));

    // ------------------------------------ zprava a rozepsana jmena hracu ------------------------------------

    private String message = "";
    private String firstValue = "Hráč 1";
    private String secondValue = "Hráč 2";
    private MiniTui.Input firstName;
    private MiniTui.Input secondName;

    // ------------------------------------ inicializace rozhrani a klavesovych zkratek ------------------------------------

    public Main() {
        tui.onKey("F1", () -> open(Screen.HELP));
        tui.onKey("h", () -> open(Screen.HELP));
        tui.onKey("l", () -> {
            if (game != null) open(Screen.HISTORY);
        });
        tui.onKey("ESC", this::back);
        tui.onKey("i", () -> {
            if (screen == Screen.HOME && toolkitRows.containsKey(tui.getFocusedElement()))
                inspect(toolkitRows.get(tui.getFocusedElement()), false);
        });
        // Konstruktor pouze sestavi prvni pohled; smycku spousti main().
        draw();

        // Zmena rozmeru prestavi prvky; neposouva tah ani nemeni herni stav.
        tui.onTick(() -> {
            if (width != tui.getTerminalWidth() || height != tui.getTerminalHeight()) draw();
        });
    }

    // ------------------------------------ barva aktivniho hrace a obrazovky ------------------------------------

    private String playerColor(int index) {
        return index == 0 ? ORANGE : MINT;
    }

    private String accent() {
        return game != null && (game.getPhase() == Game.Phase.SHOP || game.getPhase() == Game.Phase.COMBAT)
                ? playerColor(game.getCurrentIndex()) : WHITE;
    }

    // ------------------------------------ vypocet prostoru pro hlavni obsah ------------------------------------

    private int contentY() {
        return game == null ? 5 : 12;
    }

    private int contentHeight() {
        return height - contentY() - 5;
    }

    // ------------------------------------ vytvareni popisku, ramecku a tlacitek ------------------------------------

    private void label(int x, int y, String value, String color) {
        tui.add(new MiniTui.Label(x, y, 0, value, color));
    }

    private void box(int x, int y, int w, int h, String title, String color) {
        tui.add(new MiniTui.Box(x, y, 0, w, h, title, color));
    }

    // Runnable je akce pro pozdejsi Enter; pri vytvareni tlacitka se nespousti.
    private MiniTui.Button button(int x, int y, String value, Runnable action) {
        return tui.add(new MiniTui.Button(x, y, 0, value, action));
    }

    private void primary(int x, int y, String value, Runnable action) {
        MiniTui.Button button = button(x, y, value, action);
        button.primary = true;
    }

    // ------------------------------------ zkracovani textu a pocet stran ------------------------------------

    private String fit(String value, int columns) {
        if (columns < 1) return "";
        return value.length() <= columns ? value : value.substring(0, Math.max(0, columns - 1)) + "…";
    }

    private int pages(int count, int size) {
        return Math.max(1, (count + size - 1) / size);
    }

    // ------------------------------------ otevreni pohledu, navrat a zachovani jmen ------------------------------------

    private void open(Screen next) {
        if (screen == next) return;
        saveNames();
        MiniTui.Element focus = tui.getFocusedElement();
        // Zasobnik dovoli navrat i z napovedy otevrene nad detailem predmetu.
        views.push(new View(screen, page, inventoryView, selectedItem, shopItem, selectedPlayer,
                focus == null ? "" : focus.getText(), focus == null ? -1 : focus.x, focus == null ? -1 : focus.y));
        screen = next;
        page = 0;
        helpSlice = 0;
        draw();
    }

    private void back() {
        if (views.isEmpty()) return;
        View view = views.pop();
        screen = view.screen();
        page = view.page();
        inventoryView = view.inventory();
        selectedItem = view.item();
        shopItem = view.offer();
        selectedPlayer = view.player();
        requestedFocus = view.focusText();
        requestedX = view.focusX();
        requestedY = view.focusY();
        draw();
    }

    private void home() {
        views.clear();
        screen = Screen.HOME;
        page = 0;
        selectedPlan = -1;
        draw();
    }

    private void saveNames() {
        if (screen == Screen.NAMES && firstName != null) {
            firstValue = firstName.getValue();
            secondValue = secondName.getValue();
        }
    }

    // ------------------------------------ sestaveni obrazovky a obnoveni vyberu ------------------------------------

    private void draw() {
        saveNames();
        MiniTui.Element oldFocus = tui.getFocusedElement();

        width = tui.getTerminalWidth();
        height = tui.getTerminalHeight();

        // Pravy panel ma 32 az 44 sloupcu, zbytek sirky dostane hlavni obsah.
        rightWidth = Math.min(44, Math.max(32, width / 3));
        rightX = width - rightWidth - 2;
        leftWidth = rightX - panelGap - 2;

        // Prvky vytvarime znovu, ale Game i rozepsana jmena zustavaji zachovany.
        tui.clear();
        toolkitRows.clear();
        tui.setTheme(accent());
        tui.setStatus("Šipky: výběr  Enter: potvrdit  I: popis  F1: návod  Esc: zpět  Ctrl+C: konec");

        if (width < 100 || height < 32) {
            label(3, 2, "Zvětši terminál alespoň na 100 × 32 znaků.", WHITE);
            label(3, 4, "Pro pohodlné hraní doporučujeme 140 × 42. Hra počká.", MUTED);
            return;
        }

        label(3, 1, fit(header(), width - 6), MiniTui.Color.BOLD + accent());
        if (game != null) {
            drawMatchPanel();
            drawPlayerCard(0, 3, (height - 7) / 2);
            drawPlayerCard(1, 4 + (height - 7) / 2, height - 4 - (4 + (height - 7) / 2));
            box(2, 8, leftWidth, 3, "Info", message.isEmpty() ? MUTED : accent());
            label(4, 9, fit(message.isEmpty() ? hint() : message, leftWidth - 4), message.isEmpty() ? MUTED : WHITE);
        }

        switch (screen) {
            case WELCOME -> drawWelcome();
            case NAMES -> drawNames();
            case HOME -> drawHome();
            case DETAIL -> drawDetail();
            case HELP -> drawHelp();
            case HISTORY -> drawHistory();
            case PLAYER -> drawPlayerDetail();
        }

        drawUtilities();
        tui.setLayer(0);
        // Po clear() maji prvky nove instance. Vyber proto hledame podle textu a souradnic.
        // Zachovame jej jen ve stejnem pohledu nebo pri navratu z napovedy.
        MiniTui.Element restore = null;
        if (requestedFocus != null) {
            restore = tui.getElements().stream().filter(element -> element.focusable && element.x == requestedX
                    && element.y == requestedY && element.getText().equals(requestedFocus)).findFirst().orElse(null);
            requestedFocus = null;
        } else if (oldFocus != null && screen == lastDrawnScreen
                && (game == null || game.getCurrentIndex() == lastDrawnActor && game.getPhase() == lastDrawnPhase)) {
            restore = tui.getElements().stream().filter(element -> element.focusable && element.x == oldFocus.x
                    && element.y == oldFocus.y && element.getText().equals(oldFocus.getText())).findFirst().orElse(null);
        }
        if (restore == null) restore = tui.getElements().stream().filter(element -> element.focusable && (element instanceof MiniTui.Input
                || element instanceof MiniTui.TableRow)).findFirst().orElse(null);
        if (restore == null) restore = tui.getElements().stream().filter(element -> element instanceof MiniTui.Button button && button.primary)
                .findFirst().orElse(null);
        if (restore != null) tui.focus(restore);
        lastDrawnScreen = screen;
        lastDrawnActor = game == null ? -1 : game.getCurrentIndex();
        lastDrawnPhase = game == null ? null : game.getPhase();
    }

    // ------------------------------------ text zahlavi a napovedy aktualni faze ------------------------------------

    private String header() {
        if (screen == Screen.WELCOME) return "DUEL 1V1";
        if (screen == Screen.NAMES) return "Kdo bude hrát?";
        if (game == null) return "Návod";
        String phase = switch (game.getPhase()) {
            case ANNOUNCEMENT -> "Obchod";
            case SHOP -> "Obchod";
            case COMBAT -> game.isPreparation() ? "Příprava"
                    : "Kolo " + game.getCombatRound() + " (" + game.getTurnPosition() + "/2)";
            case RESULT -> "Výsledek";
            case FINISHED -> "Vítěz zápasu";
        };
        String owner = game.getPhase() == Game.Phase.SHOP || game.getPhase() == Game.Phase.COMBAT
                ? "  ·  " + game.getCurrentPlayer().getName() : "";
        return "Souboj " + game.getBattle() + "  /  " + phase + owner;
    }

    private String hint() {
        return switch (game.getPhase()) {
            case COMBAT -> "Naplánuj akce. Provedou se až po potvrzení konce tahu.";
            case SHOP, ANNOUNCEMENT -> "Vyber předmět pro popis. Pokračovat předá hru dál.";
            case RESULT -> "Souboj skončil. Pokračuj do dalšího obchodu.";
            case FINISHED -> "Zápas skončil. Můžeš začít nový zápas.";
        };
    }

    // ------------------------------------ trvale panely zapasu a hracu ------------------------------------

    private void drawMatchPanel() {
        box(2, 3, leftWidth, 4, "Zápas · výhry", accent());
        int half = (leftWidth - 4) / 2;
        for (int index = 0; index < 2; index++) {
            int x = 4 + index * half;
            label(x, 4, fit(game.getPlayer(index).getName(), half - 8), playerColor(index));
            label(x + half - 7, 4, game.getWins(index) + " / " + GameRules.WINS_REQUIRED, WHITE);
        }
        label(4, 5, fit("Obchod: " + game.getPlayer(game.getShopStarter()).getName(), half - 1), MUTED);
        label(4 + half, 5, fit("Boj: " + game.getPlayer(game.getCombatStarter()).getName(), half - 1), MUTED);
    }

    private void drawPlayerCard(int index, int y, int cardHeight) {
        Player player = game.getPlayer(index);
        String color = playerColor(index);
        box(rightX, y, rightWidth, cardHeight, fit(player.getName(), rightWidth - 7), color);
        int x = rightX + 2;
        int row = y + 1;
        label(x, row++, "Životy  " + player.getHealth() + " / " + Player.MAX_HEALTH, color);
        // vydrz patri k aktualnimu rozhodovani ve vybave a ne do karty soupere
        if (game.getPhase() != Game.Phase.COMBAT) label(x, row++, "Zlato   " + player.getGold(), GOLD);
        label(x, row++, "Štít    " + player.getShield(), WHITE);
        label(x, row++, fit("Zbroj: " + ownedName(player.getEquipment().getEquippedArmor()), rightWidth - 4), MUTED);
        label(x, row++, fit("Talisman: " + ownedName(player.getEquipment().getEquippedTrinket()), rightWidth - 4), MUTED);
        int room = Math.max(0, y + cardHeight - 2 - row);
        boolean moreEffects = player.getEffects().size() > room;
        int visibleEffects = moreEffects ? Math.max(0, room - 1) : room;
        if (player.getEffects().isEmpty() && room > 0) label(x, row, "Žádné trvající účinky", MUTED);
        for (int i = 0; i < Math.min(visibleEffects, player.getEffects().size()); i++)
            label(x, row + i, fit(text.effect(player.getEffects().get(i)), rightWidth - 4), WHITE);
        if (moreEffects) label(x, row + visibleEffects, "+" + (player.getEffects().size() - visibleEffects) + " dalších účinků", MUTED);
        button(x, y + cardHeight - 2, "Podrobnosti", () -> {
            selectedPlayer = index;
            open(Screen.PLAYER);
        }).colorCode = color;
    }

    private String ownedName(Item item) {
        return item == null ? "—" : text.name(item);
    }

    // ------------------------------------ ovladani napovedy, historie a navratu ------------------------------------

    private void drawUtilities() {
        button(2, height - 2, "F1  Návod", () -> open(Screen.HELP)).colorCode = MUTED;
        if (game != null) button(width - 25, height - 2, "L  Historie", () -> open(Screen.HISTORY)).colorCode = MUTED;
        if (!views.isEmpty()) button(width / 2 - 6, height - 2, "Esc  Zpět", this::back).colorCode = MUTED;
    }

    // ------------------------------------ uvodni obrazovka a zadavani jmen ------------------------------------

    private void drawWelcome() {
        int x = width / 2 - 30;
        box(x, 7, 60, 9, "Duel 1V1", WHITE);
        primary(x + 4, 11, "Začít", () -> {
            screen = Screen.NAMES;
            draw();
        });

    }

    private void drawNames() {
        int x = width / 2 - 30;
        box(x, 7, 60, 15, "Jména hráčů", WHITE);
        firstName = tui.add(new MiniTui.Input(x + 3, 10, 0, "Hráč 1", firstValue, 30));
        secondName = tui.add(new MiniTui.Input(x + 3, 13, 0, "Hráč 2", secondValue, 30));
        label(x + 4, 9, "Oranžová", ORANGE);
        label(x + 4, 12, "Mátová", MINT);
        primary(x + 4, 17, "Připraveni — do obchodu", () -> {
            saveNames();
            game = new Game(firstValue, secondValue, new Random());
            game.openShop(); // poradi ukaze trvaly panel a nepotrebuje vlastni potvrzovaci menu
            home();
        });
    }

    // ------------------------------------ zahlavi tabulek ------------------------------------

    private void tableHeading(int x, int y, List<String> names, List<Integer> widths, List<String> colors) {
        MiniTui.TableRow heading = tui.add(new MiniTui.TableRow(x, y, 0, "", names, widths, colors, null));
        heading.focusable = false; // zahlavi sdili presne stejne hranice sloupcu jako data
    }

    // ------------------------------------ vyber obsahu podle faze zapasu ------------------------------------

    private void drawHome() {
        switch (game.getPhase()) {
            case ANNOUNCEMENT -> {
                game.openShop();
                drawHome();
            }
            case SHOP -> drawShop();
            case COMBAT -> drawCombat();
            case RESULT, FINISHED -> drawResult();
        }
    }

    // ------------------------------------ obchod, inventar a predani nakupovani ------------------------------------

    private void drawShop() {
        int y = contentY();
        int h = contentHeight();
        box(2, y, leftWidth, h, (inventoryView ? "Inventář" : "Obchod")
                + " — " + game.getCurrentPlayer().getGold() + " zlata", accent());
        button(4, y + 1, "Obchod", () -> {
            inventoryView = false;
            page = 0;
            draw();
        });
        button(23, y + 1, "Inventář", () -> {
            inventoryView = true;
            page = 0;
            draw();
        });

        List<Item> items = inventoryView ? game.getCurrentPlayer().getInventory() : new ArrayList<>(game.getShop().getStock().keySet());
        // Vyska panelu urcuje pocet nabidek na strance; sedm radku patri ovladani a okrajum.
        int size = Math.max(1, h - 7);
        page = Math.min(page, pages(items.size(), size) - 1);
        int nameWidth = Math.min(26, Math.max(15, leftWidth / 3));
        int effectWidth = Math.min(34, leftWidth - nameWidth - 23);
        int[] columns = {
            nameWidth, effectWidth, 8, 5
        };
        tableHeading(4, y + 3, List.of("Předmět", "Účinek", "Zlato", inventoryView ? "" : "Ks"),
                List.of(columns[0], columns[1], columns[2], columns[3]), List.of(MUTED, MUTED, GOLD, MINT));

        for (int row = 0, index = page * size; row < size && index < items.size(); row++, index++) {
            Item item = items.get(index);
            int stock = inventoryView ? 1 : game.getShop().getStock().get(item);
            boolean affordable = item.getPrice() <= game.getCurrentPlayer().getGold();
            String name = text.name(item) + (inventoryView && game.getCurrentPlayer().getEquipment().isEquipped(item) ? " *" : "");
            List<String> cells = List.of(name, text.summary(item), Integer.toString(inventoryView ? item.getPrice() / 2 : item.getPrice()), inventoryView ? "" : Integer.toString(stock));
            MiniTui.TableRow control = tui.add(new MiniTui.TableRow(4, y + 4 + row, 0, text.name(item), cells,
                    List.of(columns[0], columns[1], columns[2], columns[3]),
                    List.of(stock == 0 ? MUTED : WHITE, MUTED, affordable || inventoryView ? GOLD : RED, stock > 0 ? MINT : RED),
                    () -> inspect(item, !inventoryView)));
            control.colorCode = accent();
            control.backgroundCode = index % 2 == 1 ? MiniTui.Color.background(32, 36, 44) : "";
        }

        if (items.isEmpty()) label(5, y + 5, "Inventář je prázdný.", MUTED);
        pagination(items.size(), size, 4, y + h - 2);

        String next = game.willStartBattleOnContinue() ? "Zahájit souboj"
                : "Pokračovat → " + game.getOpponent().getName();
        primary(4, height - 4, next, () -> {
            game.continueShop();
            message = "";
            inventoryView = false;
            home();
        });
    }

    // ------------------------------------ detail predmetu a jeho dostupne akce ------------------------------------

    private void inspect(Item item, boolean offer) {
        selectedItem = item;
        shopItem = offer;
        open(Screen.DETAIL);
    }

    private void drawDetail() {
        if (selectedItem == null && game.getPhase() != Game.Phase.COMBAT) {
            back();
            return;
        }
        int y = contentY();
        int h = contentHeight();
        box(2, y, leftWidth, h, text.name(selectedItem) + (game.getPhase() == Game.Phase.COMBAT
                ? " — výdrž " + game.getCurrentPlayer().getStamina() + "/" + game.getCurrentPlayer().getMaxStamina() : ""), accent());
        List<String> lines = wrap(text.description(selectedItem), leftWidth - 6);
        int size = Math.max(1, h - 5);
        textPage(lines, 5, y + 2, size);
        pagination(lines.size(), size, 4, y + h - 2);
        if (game.getPhase() == Game.Phase.SHOP) {
            if (shopItem) {
                int stock = game.getShop().getStock().getOrDefault(selectedItem, 0);
                String reason = stock == 0 ? "Vyprodáno"
                        : selectedItem.getPrice() > game.getCurrentPlayer().getGold() ? "Nedostatek zlata"
                        : game.getCurrentPlayer().getInventory().size() >= game.getCurrentPlayer().getInventorySize()
                                ? "Plný inventář" : "";
                MiniTui.Button buy = button(4, height - 4, reason.isEmpty() ? "Koupit za " + selectedItem.getPrice() : reason,
                        () -> {
                            message = game.buy(selectedItem);
                            draw();
                        });
                buy.enabled = reason.isEmpty();
                buy.primary = true;
            } else {
                button(4, height - 4, "Prodat za " + selectedItem.getPrice() / 2, () -> {
                    message = game.sell(selectedItem);
                    if (!game.getCurrentPlayer().getInventory().contains(selectedItem)) back();
                    else draw();
                });
                if (selectedItem.isEquipment()) button(Math.max(25, leftWidth / 2), height - 4,
                        "Vybavit / sundat", () -> {
                            message = game.equip(selectedItem);
                            draw();
                        });
            }
        } else if (game.getPhase() == Game.Phase.COMBAT && (selectedItem == null || !selectedItem.isEquipment())) {
            Player.Action action = selectedItem == null ? Player.Action.attack() : Player.Action.use(selectedItem);
            boolean reserved = selectedItem != null && selectedItem.isConsumable()
                    && game.getCurrentPlayer().getActions().stream().anyMatch(entry -> entry.item() == selectedItem);
            String reason = reserved ? "Tento kus už je v plánu"
                    : game.isPreparation() && (selectedItem == null || selectedItem.getItemType() == ItemType.DAMAGE)
                            ? "Útoky jsou dostupné po přípravě"
                            : game.getActionCost(action) > game.getCurrentPlayer().getStamina()
                                    ? "Nedostatek výdrže" : "";
            MiniTui.Button add = button(4, height - 4, reason.isEmpty()
                    ? "Přidat do plánu · " + game.getActionCost(action) + " výdrže" : reason,
                    () -> {
                        message = game.act(action);
                        draw();
                    });
            add.primary = true;
            add.enabled = reason.isEmpty();
        }
    }

    // ------------------------------------ bojova vybava, plan akci a potvrzeni tahu ------------------------------------

    private void drawCombat() {
        Player actor = game.getCurrentPlayer();
        int y = contentY();
        int h = contentHeight();
        int toolkitHeight = Math.max(7, h / 2);
        int planY = y + toolkitHeight + 1;
        int planHeight = h - toolkitHeight - 1;
        box(2, y, leftWidth, toolkitHeight, "Výbava — výdrž "
                + actor.getStamina() + "/" + actor.getMaxStamina(), accent());
        // null zastupuje pest: nema fyzicky predmet v inventari.
        List<Item> items = new ArrayList<>();
        items.add(null);
        items.addAll(actor.getInventory().stream().filter(item -> !item.isEquipment()).toList());
        int size = Math.max(1, toolkitHeight - 4);
        page = Math.min(page, pages(items.size(), size) - 1);

        for (int row = 0, index = page * size; row < size && index < items.size(); row++, index++) {
            Item item = items.get(index);
            Player.Action action = item == null ? Player.Action.attack() : Player.Action.use(item);
            boolean hostile = item == null || item.getItemType() == ItemType.DAMAGE;
            int cost = game.getActionCost(action);
            boolean reserved = item != null && item.isConsumable() && actor.getActions().stream().anyMatch(entry -> entry.item() == item);
            String unavailable = reserved ? "v plánu" : game.isPreparation() && hostile ? "po přípravě"
                    : cost > actor.getStamina() ? "málo výdrže" : "";
            String summary = item == null ? GameRules.BASIC_DAMAGE + " fyzické" : text.summary(item);
            int nameWidth = Math.min(26, Math.max(15, leftWidth / 3));
            MiniTui.TableRow control = tui.add(new MiniTui.TableRow(4, y + 1 + row + (page == 0 && row > 0 ? 1 : 0), 0, text.name(item),
                    List.of(text.name(item), summary, unavailable.isEmpty() ? cost + " výd." : unavailable),
                    List.of(nameWidth, Math.min(34, leftWidth - nameWidth - 26), 17),
                    List.of(WHITE, MUTED, unavailable.isEmpty() ? MINT : MUTED), () -> {
                        message = game.act(action);
                        planPage = Math.max(0, (actor.getActions().size() - 1) / Math.max(1, planHeight - 5));
                        draw();
                    }));
            control.enabled = unavailable.isEmpty();
            toolkitRows.put(control, item);
            control.colorCode = accent();
            control.backgroundCode = index % 2 == 1 ? MiniTui.Color.background(32, 36, 44) : "";
        }
        if (page == 0) label(5, y + 2, "─".repeat(Math.max(1, leftWidth - 6)), MUTED);
        pagination(items.size(), size, 4, y + toolkitHeight - 2);
        box(2, planY, leftWidth, planHeight, "Plán — provede se shora dolů", accent());

        List<Player.Action> plan = actor.getActions();
        int planRows = Math.max(1, planHeight - 5);
        planPage = Math.min(planPage, pages(plan.size(), planRows) - 1);
        int offset = planPage * planRows;
        int planNameWidth = Math.min(26, Math.max(12, leftWidth / 3));
        tableHeading(4, planY + 1, List.of("#", "Akce", "Účinek", "Výdrž"),
                List.of(3, planNameWidth, Math.min(34, leftWidth - planNameWidth - 20), 7), List.of(MUTED, MUTED, MUTED, MINT));
        for (int i = offset; i < Math.min(plan.size(), offset + planRows); i++) {
            final int index = i;
            Player.Action action = plan.get(i);
            String name = text.name(action.basicAttack() ? null : action.item());
            int nameWidth = Math.min(26, Math.max(12, leftWidth / 3));
            MiniTui.TableRow row = tui.add(new MiniTui.TableRow(4, planY + 2 + i - offset, 0, (i + 1) + ". " + name,
                    List.of((i + 1) + ".", name, text.summary(action.basicAttack() ? null : action.item()), Integer.toString(game.getPlannedActionCost(i))),
                    List.of(3, nameWidth, Math.min(34, leftWidth - nameWidth - 20), 7),
                    List.of(MUTED, selectedPlan == i ? accent() : WHITE, MUTED, MINT),
                    () -> {
                        selectedPlan = index;
                        draw();
                    }));
            row.colorCode = accent();
            row.backgroundCode = i % 2 == 1 ? MiniTui.Color.background(32, 36, 44) : "";
        }
        if (plan.isEmpty()) label(5, planY + 2, "Zatím žádné akce. Vyber je ve výbavě.", MUTED);
        if (!plan.isEmpty()) {
            if (pages(plan.size(), planRows) > 1) {
                if (planPage > 0) button(4, planY + planHeight - 3, "←", () -> {
                    planPage--;
                    draw();
                });
                label(12, planY + planHeight - 3, (planPage + 1) + "/" + pages(plan.size(), planRows), MUTED);
                if (planPage + 1 < pages(plan.size(), planRows)) button(23, planY + planHeight - 3, "→", () -> {
                    planPage++;
                    draw();
                });
            }
            int row = planY + planHeight - 2;
            button(4, row, "Odebrat", () -> {
                game.removePlannedAction(selectedPlan < 0 ? plan.size() - 1 : selectedPlan);
                selectedPlan = -1;
                draw();
            });
        }
        primary(4, height - 4, "Provést plán a ukončit tah", () -> {
            game.endTurn();
            message = "";
            page = 0;
            selectedPlan = -1;
            home();
        });
        label(Math.max(40, leftWidth - 15), height - 4, "I: popis", MUTED);
    }

    // ------------------------------------ vysledek souboje a novy zapas ------------------------------------

    private void drawResult() {
        int y = contentY();
        box(2, y, leftWidth, contentHeight(), "Výsledek souboje", WHITE);
        label(5, y + 2, fit("Vítězí " + game.getPlayer(game.getWinner()).getName(), leftWidth - 6), MiniTui.Color.BOLD + playerColor(game.getWinner()));
        for (int index = 0; index < 2; index++) {
            int reward = index == game.getWinner() ? GameRules.WIN_REWARD : GameRules.LOSS_REWARD;
            label(5, y + 5 + index * 2, fit(game.getPlayer(index).getName() + "  +" + reward + " zlata", leftWidth - 6), GOLD);
        }
        if (game.getPhase() == Game.Phase.FINISHED) {
            primary(4, height - 4, "Nový zápas", () -> {
                game = null;
                screen = Screen.WELCOME;
                views.clear();
                message = "";
                draw();
            });
        } else {
            primary(4, height - 4, "Pokračovat do obchodu", () -> {
                game.nextBattle();
                game.openShop();
                message = "";
                inventoryView = false;
                home();
            });
        }
    }

    // ------------------------------------ uplny stav hrace ------------------------------------

    private void drawPlayerDetail() {
        Player player = game.getPlayer(selectedPlayer);
        List<String> paragraphs = new ArrayList<>();
        paragraphs.add(player.getName());
        paragraphs.add("Životy: " + player.getHealth() + "/" + Player.MAX_HEALTH
                + "  Výdrž: " + player.getStamina() + "/" + player.getMaxStamina());
        paragraphs.add("Zlato: " + player.getGold());
        paragraphs.add("Příští tah navíc: " + player.getNextStamina());
        List<Damage> effects = player.getEffects();
        if (effects.isEmpty()) paragraphs.add("Žádné trvající účinky.");
        else {
            paragraphs.add("Účinky · čísla označují pořadí působení");
            boolean damage = effects.stream().anyMatch(effect -> effect.getType() != Damage.DamageType.HEAL);
            if (damage) paragraphs.add("Poškození:");
            for (int i = 0; i < effects.size(); i++)
                if (effects.get(i).getType() != Damage.DamageType.HEAL) paragraphs.add("  " + (i + 1) + ". " + text.effect(effects.get(i)));
            boolean healing = effects.stream().anyMatch(effect -> effect.getType() == Damage.DamageType.HEAL);
            if (healing) paragraphs.add("Léčení:");
            for (int i = 0; i < effects.size(); i++)
                if (effects.get(i).getType() == Damage.DamageType.HEAL) paragraphs.add("  " + (i + 1) + ". " + text.effect(effects.get(i)));
        }
        paragraphs.add("Vybavení a inventář:");
        for (Item item : player.getInventory()) paragraphs.add(text.name(item) + " — " + text.summary(item)
                + (player.getEquipment().isEquipped(item) ? " [vybaveno]" : ""));
        document(player.getName(), paragraphs);
    }

    // ------------------------------------ historie hernich udalosti ------------------------------------

    private void drawHistory() {
        List<String> lines = new ArrayList<>(game.getLog());
        java.util.Collections.reverse(lines);
        document("Historie — nejnovější nahoře", lines);
    }

    // ------------------------------------ obsah a strankovani navodu ------------------------------------

    private void drawHelp() {
        List<String> titles = List.of("Začni tady", "Obchod a peníze",
                "Plánování tahu", "Pořadí účinků",
                "Poškození a obrana", "Zápas a vybavení");
        List<List<String>> topics = List.of(
                List.of("1. Nakup zbraň a pár užitečných předmětů.",
                        "2. V obchodě vybav zbroj a talisman, potom pokračuj.",
                        "3. Při boji přidej akce do plánu v požadovaném pořadí.",
                        "4. Potvrď konec tahu. Teprve teď se plán provede.",
                        "Barva záhlaví ukazuje aktivního hráče. Vpravo stále vidíš oba hráče."),
                List.of("Oba hráči nakupují ze stejné zásoby. Nákup ji sníží; prodej předmět odstraní.",
                        "Prodej vrací polovinu ceny zaokrouhlenou dolů.",
                        "Pokud oba hráči za sebou pokračují bez nákupu nebo prodeje, začne souboj."),
                List.of("Výdrž se rezervuje při přidání akce. Životy ani účinky se zatím nemění.",
                        "Akce se provedou v pořadí přidání. Vybranou akci můžeš odebrat.",
                        "Pěst je vždy dostupná, ale stojí výdrž. V popisu vidíš i zdražení opakovaného použití."),
                List.of("Každý dostane přípravný tah bez útoků. Pak začíná kolo 1: první a druhý hráč.",
                        "Na začátku vlastního tahu působí talisman a trvající účinky v pořadí vzniku.",
                        "Na konci tahu se plán provede shora dolů. Smrt okamžitě ukončí souboj."),
                List.of("Fyzický útok zasáhne při provedení plánu. Jed a oheň působí na začátku dalších tahů cíle.",
                        "Zápis Jed: 4 × 3 (12) znamená sílu 4 na 3 tahy, celkem 12. Po změně účinku může být místo násobení vypsaná posloupnost dávek.",
                        "Oheň slučuje zbývající součty a ponechá delší trvání. Jedy zůstávají samostatné.",
                        "Nejprve brání zbroj, pak štít. Vše se zaokrouhluje na nejbližší celé číslo; prodejní ceny dolů."),
                List.of("Tři vyhrané souboje znamenají vítězství v zápase.",
                        "Zlato a nevyužité vybavení zůstávají. Zdraví a bojové stavy se obnoví.",
                        "Na sobě můžeš mít jednu zbroj a jeden talisman. Brašna zvětší inventář při vybavení."));
        page = Math.max(0, Math.min(page, topics.size() - 1));
        int x = 2;
        int y = contentY();
        int w = game == null ? width - 4 : leftWidth;
        box(x, y, w, contentHeight(), "Návod · " + (page + 1) + "/" + topics.size() + " · " + titles.get(page), WHITE);
        List<String> lines = new ArrayList<>();
        for (String paragraph : topics.get(page)) {
            lines.addAll(wrap(paragraph, w - 6));
            lines.add("");
        }
        int size = Math.max(1, contentHeight() - 4);
        int slices = pages(lines.size(), size);
        helpSlice = Math.min(helpSlice, slices - 1);
        for (int row = 0, index = helpSlice * size; row < size && index < lines.size(); row++, index++)
            label(x + 3, y + 2 + row, lines.get(index), WHITE);
        if (helpSlice > 0 || page > 0) button(4, height - 4,
                helpSlice > 0 ? "← Předchozí část" : "← Předchozí téma", () -> {
                    if (helpSlice > 0) helpSlice--;
                    else {
                        page--;
                        helpSlice = 0;
                    }
                    draw();
                });
        if (helpSlice + 1 < slices || page < topics.size() - 1) primary(Math.max(27, w - 28), height - 4,
                helpSlice + 1 < slices ? "Pokračovat v tématu →" : "Další téma →", () -> {
                    if (helpSlice + 1 < slices) helpSlice++;
                    else {
                        page++;
                        helpSlice = 0;
                    }
                    draw();
                });
    }

    // ------------------------------------ zobrazeni dlouhych textu ------------------------------------

    private void document(String title, List<String> paragraphs) {
        int y = contentY();
        int h = contentHeight();
        box(2, y, leftWidth, h, title, accent());
        List<String> lines = new ArrayList<>();
        for (String paragraph : paragraphs) lines.addAll(wrap(paragraph, leftWidth - 6));
        int size = Math.max(1, h - 4);
        textPage(lines, 5, y + 2, size);
        pagination(lines.size(), size, 4, y + h - 2);
    }

    private void textPage(List<String> lines, int x, int y, int size) {
        page = Math.min(page, pages(lines.size(), size) - 1);

        for (int row = 0, index = page * size; row < size && index < lines.size(); row++, index++) label(x, y + row, lines.get(index), WHITE);
    }

    // ------------------------------------ strankovaci tlacitka ------------------------------------

    private void pagination(int count, int size, int x, int y) {
        int pages = pages(count, size);
        if (pages < 2) return; // jedinou stranku nema smysl prezentovat jako volbu
        if (page > 0) button(x, y, "←", () -> {
            page--;
            draw();
        });
        label(x + 9, y, (page + 1) + " / " + pages, MUTED);
        if (page + 1 < pages) button(x + 21, y, "→", () -> {
            page++;
            draw();
        });
    }

    // ------------------------------------ zalamovani odstavcu do radku ------------------------------------

    private List<String> wrap(String value, int columns) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : value.split("\\R", -1)) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                if (line.length() > 0 && line.length() + word.length() + 1 > columns) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                if (line.length() > 0) line.append(' ');
                line.append(word);
            }
            lines.add(line.toString());
        }
        return lines;
    }

    // ------------------------------------ spusteni aplikace ------------------------------------

    public static void main(String[] args) {
        new Main().tui.start();
    }
}
