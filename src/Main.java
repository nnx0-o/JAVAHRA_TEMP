import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

public class Main {

    // Main propojuje ovladani s Game: vytvori prvky, zavola herni akci a zobrazi vysledek.
    // Pravidla souboje patri do Game a Player; zde se meni hlavne obsah obrazovky.

    // ------------------------------------ vrstvy a pevna velikost rozhrani ------------------------------------

    private static final int NAMES = 0, SHOP = 1, COMBAT = 2, DETAIL = 3;
    private static final int HELP = 4, HISTORY = 5, PLAYER = 6, RESULT = 7, SIZE_WARNING = 8;
    private static final int WIDTH = 140, HEIGHT = 38, TEXT_ROWS = 18, PLAN_ROWS = 16;
    private static final String WHITE = MiniTui.Color.WHITE, MUTED = MiniTui.Color.BRIGHT_BLACK;
    private static final String ORANGE = MiniTui.Color.foreground(255, 170, 80);
    private static final String MINT = MiniTui.Color.foreground(112, 235, 180);

    // ------------------------------------ hra a udaje potrebne pro navrat mezi menu ------------------------------------

    private final MiniTui tui = new MiniTui();
    private final GameText text = new GameText();
    private final Deque<Integer> previousLayers = new ArrayDeque<>();
    private Game game; // null do potvrzeni jmen, potom jeden objekt pro cely zapas
    private int layer = NAMES; // cislo menu; herni fazi uchovava samostatne Game
    private String message = "";
    private boolean shopInventoryOpen, detailIsOffer;
    private Item detailItem;
    private int playerDetailsIndex, combatPlanPage;

    // ------------------------------------ trvale prvky pojmenovane podle sve obrazovky ------------------------------------

    private MiniTui.Input namesFirstInput, namesSecondInput;
    private MiniTui.Box shopPanel, combatEquipmentPanel;
    private MiniTui.Button shopContinueButton, resultContinueButton;
    private MiniTui.Button detailBuyButton, detailSellButton, detailEquipButton, detailAddButton;
    private MiniTui.Button combatPlanPreviousButton, combatPlanNextButton;
    private MiniTui.Label shopColumnsLabel, combatPlanPageLabel, resultLabel;
    private final MiniTui.Button[] shopOfferButtons = new MiniTui.Button[GameRules.SHOP_SIZE];
    private final MiniTui.Button[] shopInventoryButtons = new MiniTui.Button[GameRules.INVENTORY_CAPACITY + 1];
    private final MiniTui.Button[] combatEquipmentButtons = new MiniTui.Button[GameRules.INVENTORY_CAPACITY + 1];
    private final Item[] combatEquipmentItems = new Item[combatEquipmentButtons.length]; // predmet za tlacitkem se stejnym indexem
    private final MiniTui.Button[] combatPlanButtons = new MiniTui.Button[PLAN_ROWS];

    // ------------------------------------ spolecne prvky presouvane na aktivni vrstvu ------------------------------------

    private final List<MiniTui.Element> navigationElements = new ArrayList<>();
    private final List<MiniTui.Element> matchElements = new ArrayList<>();
    private final List<MiniTui.Element> documentElements = new ArrayList<>();
    private MiniTui.Label menuHeading, matchMessage, sizeWarningLabel;
    private MiniTui.Button menuBackButton, menuHistoryButton;
    private MiniTui.Box matchScorePanel, documentPanel;
    private final MiniTui.Label[][] matchPlayerStats = new MiniTui.Label[2][7];
    private final MiniTui.Label[] documentRows = new MiniTui.Label[TEXT_ROWS];
    private MiniTui.Button documentPreviousButton, documentNextButton;
    private MiniTui.Label documentPageLabel;
    private final int[] documentPages = new int[RESULT + 1];
    private List<String> helpLines;

    // ------------------------------------ vsechny prvky vzniknou jednou, zacina se zadanim jmen ------------------------------------

    public Main() {
        // Registrace prvku a jejich akci probiha pred spustenim smycky terminalu.
        buildMenus();
        registerKeys();

        refresh();
        focusDefault();

        // this::checkTerminalSize predava metodu k pozdejsimu volani pri kazdem ticku.
        tui.onTick(this::checkTerminalSize);
    }

    public static void main(String[] args) {
        // new Main() provede konstruktor vyse; teprve start() zacne cist klavesy a kreslit.
        new Main().tui.start();
    }

    // ------------------------------------ vytvareni prvku na dane vrstve ------------------------------------

    private MiniTui.Label label(int z, int x, int y, String value, String color) {
        return tui.add(new MiniTui.Label(x, y, z, value, color));
    }

    private MiniTui.Box box(int z, int x, int y, int width, int height, String title) {
        return tui.add(new MiniTui.Box(x, y, z, width, height, title, MUTED));
    }

    private MiniTui.Button button(int z, int x, int y, String value, Runnable action) {
        // Runnable je akce bez parametru a navratove hodnoty. MiniTui pozdeji zavola action.run().
        // Napr. () -> open(HELP) znamena "po stisku zavolej open(HELP)", nikoli "zavolej ted".
        return tui.add(new MiniTui.Button(x, y, z, value, action));
    }

    private MiniTui.Button primary(int z, int x, int y, String value, Runnable action) {
        // Stejne tlacitko jako button(), pouze s vyraznejsim vzhledem.
        MiniTui.Button button = button(z, x, y, value, action);
        button.primary = true;
        return button;
    }

    // ------------------------------------ jmena, navigace a jednotlive obrazovky ------------------------------------

    private void buildMenus() {
        // ------------------------------------ zadani jmen pred vytvorenim hry ------------------------------------

        box(NAMES, 40, 7, 60, 15, "Duel 1V1 — jména hráčů");
        namesFirstInput = tui.add(new MiniTui.Input(43, 10, NAMES, "Hráč 1", "Hráč 1", 30));
        namesSecondInput = tui.add(new MiniTui.Input(43, 13, NAMES, "Hráč 2", "Hráč 2", 30));

        // this::startGame je zkraceny zapis () -> startGame(). Hra vznikne az po stisku.
        primary(NAMES, 44, 17, "Připraveni — do obchodu", this::startGame);

        // ------------------------------------ spolecny nadpis a navigace mezi menu ------------------------------------

        menuHeading = label(NAMES, 3, 1, "DUEL 1V1", WHITE);
        menuBackButton = button(NAMES, 60, 36, "Esc  Zpět", this::back);
        menuHistoryButton = button(NAMES, 22, 36, "L  Historie", () -> open(HISTORY));

        // List.of vytvori seznam odkazu. Samotne prvky uz zaregistrovaly label() a button().
        // Tuto skupinu pak moveElements() presune na prave otevrenou vrstvu.
        navigationElements.addAll(List.of(
                menuHeading,
                menuBackButton,
                menuHistoryButton,
                button(NAMES, 2, 36, "F1  Návod", () -> open(HELP)),
                button(NAMES, 107, 36, "R  Obnovit", this::refresh)
        ));

        // ------------------------------------ herni panely a jejich tlacitka ------------------------------------

        // Kazda metoda vytvori svoji cast rozhrani. Aktualni cisla doplni refresh().
        buildMatchPanel();
        buildShop();
        buildCombat();
        buildDocuments();

        // ------------------------------------ vysledek souboje nebo celeho zapasu ------------------------------------

        // Prazdny text je zamer: viteze zatim nezname. Doplni se pri otevreni RESULT.
        box(RESULT, 2, 12, 99, 22, "Výsledek souboje");
        resultLabel = label(RESULT, 5, 15, "", MiniTui.Color.YELLOW);
        resultContinueButton = primary(RESULT, 4, 34, "", this::continueResult);

        // ------------------------------------ upozorneni pro prilis maly terminal ------------------------------------

        sizeWarningLabel = label(SIZE_WARNING, 3, 2, "", WHITE);
        label(SIZE_WARNING, 3, 4, "Zvětši okno nebo zmenši velikost písma.", MUTED);
    }

    // ------------------------------------ jeden spolecny panel zapasu a obou hracu ------------------------------------

    private void buildMatchPanel() {
        matchScorePanel = box(SHOP, 2, 3, 99, 4, "Zápas");
        matchMessage = label(SHOP, 4, 9, "", WHITE);
        matchElements.addAll(List.of(matchScorePanel, matchMessage, box(SHOP, 2, 8, 99, 3, "Info")));

        // Oba hraci maji stejny panel. Smycka meni pouze polohu, barvu a index hrace.
        for (int player = 0; player < 2; player++) {
            // Lambda potrebuje vlastni nemenny index; promenna player se v cyklu meni.
            final int index = player;
            int top = player == 0 ? 3 : 20;
            String color = player == 0 ? ORANGE : MINT;
            matchElements.add(box(SHOP, 104, top, 34, 15, "Hráč " + (player + 1)));

            // Sedm popisku naplni updateMatch(): jmeno, zdravi, stit/zlato, vybaveni a ucinky.
            for (int row = 0; row < 7; row++) {
                matchPlayerStats[player][row] = label(SHOP, 106, top + 2 + row, "", color);
                matchElements.add(matchPlayerStats[player][row]);
            }

            matchElements.add(button(SHOP, 106, top + 12, "Podrobnosti", () -> {
                playerDetailsIndex = index;
                open(PLAYER);
            }));
        }
    }

    // ------------------------------------ deset nabidek obchodu a inventar vcetne trvale pesti ------------------------------------

    private void buildShop() {
        // ------------------------------------ prepinani nabidky a vlastnenych predmetu ------------------------------------

        shopPanel = box(SHOP, 2, 12, 99, 22, "Obchod");
        button(SHOP, 4, 13, "Obchod", () -> {
            shopInventoryOpen = false;
            refresh();
        });
        button(SHOP, 23, 13, "Inventář", () -> {
            shopInventoryOpen = true;
            refresh();
        });

        // Text tlacitka zacina az za tremi znaky ukazatele vyberu, proto hlavicka zacina na x = 7.
        shopColumnsLabel = label(SHOP, 7, 14, "", MUTED);

        // ------------------------------------ prazdne radky, ktere pozdeji naplni updateShopRows ------------------------------------

        // null znamena "zatim bez akce". Pri doplneni radku dostane tlacitko konkretni predmet.
        for (int row = 0; row < shopOfferButtons.length; row++) {
            shopOfferButtons[row] = button(SHOP, 4, 15 + row, "", null);
        }

        for (int row = 0; row < shopInventoryButtons.length; row++) {
            shopInventoryButtons[row] = button(SHOP, 4, 15 + row, "", null);
        }

        // ------------------------------------ potvrzeni navstevy obchodu ------------------------------------

        // Game rozhodne, zda se vystrida hrac, nebo uz zacne souboj. showGame() vybere menu.
        shopContinueButton = primary(SHOP, 4, 34, "Pokračovat", () -> {
            game.continueShop();
            shopInventoryOpen = false;
            message = "";
            showGame();
        });
    }

    // ------------------------------------ enter vlevo pridava do planu a vpravo odebira ------------------------------------

    private void buildCombat() {
        combatEquipmentPanel = box(COMBAT, 2, 12, 48, 22, "Výbava");
        box(COMBAT, 52, 12, 49, 22, "Plán — provede se shora dolů");

        // ------------------------------------ levy seznam: vlozeni predmetu do planu ------------------------------------

        for (int row = 0; row < combatEquipmentButtons.length; row++) {
            // Tlacitko si pamatuje cislo radku. Aktualni predmet cte az pri stisku.
            final int index = row;
            combatEquipmentButtons[row] = button(COMBAT, 4, 14 + row, "", () -> {
                message = game.act(combatEquipmentItems[index]);
                combatPlanPage = Math.max(0, (game.getCurrentPlayer().getActions().size() - 1) / PLAN_ROWS);
                refresh();
            });
        }

        // ------------------------------------ pravy seznam: odebrani konkretni naplanovane akce ------------------------------------

        for (int row = 0; row < combatPlanButtons.length; row++) {
            final int index = row;
            combatPlanButtons[row] = button(COMBAT, 54, 14 + row, "", () -> {
                game.removePlannedAction(combatPlanPage * PLAN_ROWS + index);
                refresh();
            });
        }

        // ------------------------------------ listovani dlouhym planem ------------------------------------

        // PLAN_ROWS je pocet viditelnych radku na strance, nikoli limit poctu akci.
        label(COMBAT, 54, 31, "Enter: odebrat akci", MUTED);
        combatPlanPreviousButton = button(COMBAT, 54, 32, "←", () -> {
            combatPlanPage--;
            refresh();
        });
        combatPlanPageLabel = label(COMBAT, 64, 32, "", MUTED);
        combatPlanNextButton = button(COMBAT, 85, 32, "→", () -> {
            combatPlanPage++;
            refresh();
        });

        // ------------------------------------ provedeni celeho planu a predani tahu ------------------------------------

        primary(COMBAT, 4, 34, "Provést plán a ukončit tah", () -> {
            game.endTurn();
            message = "";
            combatPlanPage = 0;
            showGame();
        });
        label(COMBAT, 64, 34, "I: popis výbavy", MUTED);
    }

    // ------------------------------------ spolecne radky pro detail, navod, historii a udaje hrace ------------------------------------

    private void buildDocuments() {
        // ------------------------------------ jeden textovy panel sdileny ctyrmi menu ------------------------------------

        // Kazde menu si pamatuje vlastni cislo stranky v documentPages[layer].
        documentPanel = box(DETAIL, 2, 12, 99, 22, "");
        documentPreviousButton = button(DETAIL, 4, 32, "←", () -> {
            documentPages[layer]--;
            refresh();
        });
        documentPageLabel = label(DETAIL, 14, 32, "", MUTED);
        documentNextButton = button(DETAIL, 30, 32, "→", () -> {
            documentPages[layer]++;
            refresh();
        });
        documentElements.addAll(List.of(documentPanel, documentPreviousButton, documentPageLabel, documentNextButton));

        for (int row = 0; row < TEXT_ROWS; row++) {
            documentRows[row] = label(DETAIL, 5, 14 + row, "", WHITE);
            documentElements.add(documentRows[row]);
        }

        // ------------------------------------ akce dostupne pouze v detailu predmetu ------------------------------------

        // Hra vrati zpravu i pri odmitnuti pokusu. refresh() ji zobrazi v panelu Info.
        detailBuyButton = primary(DETAIL, 4, 34, "", () -> {
            message = game.buy(detailItem);
            refresh();
        });
        detailSellButton = button(DETAIL, 4, 34, "", () -> {
            message = game.sell(detailItem);
            if (!game.getCurrentPlayer().getInventory().contains(detailItem)) back();
            else refresh();
        });
        detailEquipButton = button(DETAIL, 32, 34, "Vybavit / sundat", () -> {
            message = game.equip(detailItem);
            refresh();
        });
        detailAddButton = primary(DETAIL, 4, 34, "", () -> {
            message = game.act(detailItem);
            refresh();
        });

        // ------------------------------------ staticky navod pripraveny jednou pri startu ------------------------------------

        helpLines = wrapText(List.of("Začni tady",
                "Nakup vybavení a potvrď pokračování. Obchod končí po dvou návštěvách bez nákupu nebo prodeje.",
                "V obchodě lze vybavit jednu zbroj a jeden talisman. Pěst je trvale v inventáři, mimo deset míst pro nákup.",
                "V boji přidává Enter na výbavě akci do plánu. Enter na akci v plánu ji odebere. I otevře popis výbavy.",
                "Každý hráč má přípravný tah bez útoků. Potom začínají běžná kola.",
                "Výdrž se rezervuje při plánování. Opakování stejného kusu stojí více; nový vlastní tah cenu obnoví. Odebrání přepočítá celý plán.",
                "Plán se provede shora dolů po potvrzení konce tahu. Smrt jej okamžitě zastaví. Lektvar se spotřebuje až při provedení.",
                "Výdrž navíc z lektvarů se sčítá a přijde příští vlastní tah. Léčení nepřekročí maximum životů.",
                "Jedy mají samostatné časovače. Každá dávka je zaokrouhlený podíl zbývající síly a tahů; odečte se od zbývající síly.",
                "Talisman působí před časovanými účinky. Očista ubírá zbývající jed od nejstaršího; její limit je společný pro všechny jedy.",
                "Odpovídající zbroj sníží každý zásah včetně dávek jedem, výsledek se zaokrouhlí. Potom se použije štít a nakonec životy.",
                "Štíty se sčítají. Starý štít kryje i úvodní účinky příštího vlastního tahu, potom zanikne.",
                "Tři vítězství ukončí zápas. Nevyužité předměty a zlato zůstávají do dalšího souboje.",
                "F1/H otevře návod, L historii. Esc vrátí předchozí menu. R obnoví obsah. Ctrl+C ukončí hru."));
    }

    // ------------------------------------ klavesy a dostupnost jednotlivych vrstev ------------------------------------

    private void registerKeys() {
        // Stejna metoda muze byt akci tlacitka i klavesove zkratky.
        tui.onKey("F1", () -> open(HELP));
        tui.onKey("h", () -> open(HELP));
        tui.onKey("l", () -> open(HISTORY));
        tui.onKey("ESC", this::back);
        tui.onKey("r", this::refresh);

        // I najde vybrane tlacitko vybavy a otevre predmet prirazeny ke stejnemu radku.
        tui.onKey("i", () -> {
            if (layer != COMBAT) return;
            for (int row = 0; row < combatEquipmentButtons.length; row++) {
                if (tui.getFocusedElement() == combatEquipmentButtons[row]) inspect(combatEquipmentItems[row], false);
            }
        });

        // Prazdna lambda zablokuje vychozi prepinani sousednich vrstev v MiniTui.
        // Prechody pak vzdy projdou nasi kontrolou isAvailable().
        for (String key : new String[]{"[", "]", "PGUP", "PGDN"}) tui.onKey(key, () -> { });
    }

    private boolean isAvailable(int next) {
        if (next == HELP) return true;
        if (next == NAMES) return game == null;
        if (game == null) return false;
        return switch (next) {
            case SHOP -> game.getPhase() == Game.Phase.SHOP;
            case COMBAT -> game.getPhase() == Game.Phase.COMBAT;
            case RESULT -> game.getPhase() == Game.Phase.RESULT || game.getPhase() == Game.Phase.FINISHED;
            case DETAIL -> detailItem != null;
            case HISTORY, PLAYER -> true;
            default -> false;
        };
    }

    private void open(int next) {
        if (next == layer || !isAvailable(next)) return;

        // Ulozime pouze cislo predchoziho menu. Herni stav se nekopiruje ani nevraci zpet.
        previousLayers.push(layer);
        layer = next;
        documentPages[next] = 0;
        refresh();
        focusDefault();
    }

    private void back() {
        if (previousLayers.isEmpty()) return;
        // pop vrati posledni vlozenou vrstvu a zaroven ji odstrani ze zasobniku.
        layer = previousLayers.pop();
        refresh();
        focusDefault();
    }

    private void showRoot(int next) {
        // Po predani tahu se uz nesmime vratit do menu predchoziho hrace.
        previousLayers.clear();
        layer = next;
        refresh();
        focusDefault();
    }

    private void showGame() {
        // Game zna fazi zapasu; Main ji pouze prelozi na odpovidajici cislo obrazovky.
        showRoot(switch (game.getPhase()) {
            case ANNOUNCEMENT, SHOP -> SHOP;
            case COMBAT -> COMBAT;
            case RESULT, FINISHED -> RESULT;
        });
    }

    private void inspect(Item item, boolean offer) {
        detailItem = item;
        detailIsOffer = offer;
        open(DETAIL);
    }

    private void startGame() {
        game = new Game(namesFirstInput.getValue(), namesSecondInput.getValue(), new Random());
        game.openShop();
        message = "";
        shopInventoryOpen = false;
        detailItem = null;
        combatPlanPage = 0;
        showGame();
    }

    private void continueResult() {
        if (game.getPhase() == Game.Phase.FINISHED) {
            game = null;
            showRoot(NAMES);
        } else {
            game.nextBattle();
            game.openShop();
            message = "";
            shopInventoryOpen = false;
            showGame();
        }
    }

    // ------------------------------------ jedna obnova po akci nebo prechodu, aktualizuje jen aktivni obrazovku ------------------------------------

    private void refresh() {
        // ------------------------------------ spolecne ovladani a viditelnost panelu ------------------------------------

        tui.setStatus("Šipky: výběr  Enter: potvrdit  I: popis  F1/H: návod  L: historie  R: obnovit  Esc: zpět");
        tui.setTheme(game == null ? WHITE : game.getCurrentIndex() == 0 ? ORANGE : MINT);
        moveElements(navigationElements, true);
        menuBackButton.visible = !previousLayers.isEmpty();
        menuHistoryButton.visible = game != null;
        moveElements(matchElements, game != null);
        moveElements(documentElements, layer == DETAIL || layer == HELP || layer == HISTORY || layer == PLAYER);

        // ------------------------------------ aktualni hodnoty v panelu zapasu ------------------------------------

        menuHeading.setText("DUEL 1V1");
        if (game != null) updateMatch();

        // ------------------------------------ obsah pouze prave otevrene obrazovky ------------------------------------

        // case ... -> je vetev switch, nikoli lambda. Vybrana vetev se provede hned ted.
        switch (layer) {
            case SHOP -> updateShop();
            case COMBAT -> updateCombat();
            case DETAIL -> updateDetail();
            case HELP -> updateDocument("Návod", helpLines);
            case HISTORY -> {
                List<String> history = new ArrayList<>(game.getLog());
                Collections.reverse(history);
                updateDocument("Historie — nejnovější nahoře", wrapText(history));
            }
            case PLAYER -> updatePlayerDetails();
            case RESULT -> {
                resultLabel.setText("Vítězí " + game.getPlayer(game.getWinner()).getName()
                        + " (vítěz +" + GameRules.WIN_REWARD + ", poražený +" + GameRules.LOSS_REWARD + " zlata)");
                resultContinueButton.setText(game.getPhase() == Game.Phase.FINISHED ? "Nový zápas" : "Pokračovat do obchodu");
            }
        }

        checkTerminalSize();
    }

    private void moveElements(List<MiniTui.Element> elements, boolean visible) {
        // Menime z a viditelnost existujicich objektu; dalsi kopie prvku nevznikaji.
        for (MiniTui.Element element : elements) {
            element.z = layer;
            element.visible = visible;
        }
    }

    // ------------------------------------ spolecny panel ukazuje aktualni stav zapasu ------------------------------------

    private void updateMatch() {
        String phase = switch (game.getPhase()) {
            case ANNOUNCEMENT, SHOP -> "Obchod";
            case COMBAT -> game.isPreparation() ? "Příprava" : "Kolo " + game.getCombatRound() + " (" + game.getTurnPosition() + "/2)";
            case RESULT -> "Výsledek";
            case FINISHED -> "Vítěz zápasu";
        };
        menuHeading.setText(fit("Souboj " + game.getBattle() + " / " + phase + " · " + game.getCurrentPlayer().getName(), 132));
        matchScorePanel.title = fit(game.getPlayer(0).getName() + " " + game.getWins(0) + ":" + game.getWins(1) + " " + game.getPlayer(1).getName(), 90);
        matchMessage.setText(fit(message.isEmpty() ? "Obchod: " + game.getPlayer(game.getShopStarter()).getName()
                + " · Boj: " + game.getPlayer(game.getCombatStarter()).getName() : message, 92));
        for (int index = 0; index < 2; index++) {
            Player player = game.getPlayer(index);
            MiniTui.Label[] stats = matchPlayerStats[index];
            stats[0].setText(fit(player.getName(), 28));
            stats[1].setText("Životy " + player.getHealth() + "/" + Player.MAX_HEALTH);
            stats[2].setText("Štít " + player.getShield() + " · Zlato " + player.getGold());
            stats[3].setText(fit("Zbroj: " + ownedName(player.getEquipment().getEquippedArmor()), 28));
            stats[4].setText(fit("Talisman: " + ownedName(player.getEquipment().getEquippedTrinket()), 28));
            stats[5].setText(player.getEffects().isEmpty() ? "Žádné trvající účinky" : fit(text.effect(player.getEffects().get(0)), 28));
            stats[6].setText(player.getEffects().size() > 1 ? "+" + (player.getEffects().size() - 1) + " dalších účinků" : "");
        }
    }

    // ------------------------------------ doplneni existujicich radku obchodu a inventare ------------------------------------

    private void updateShop() {
        Player player = game.getCurrentPlayer();
        shopPanel.title = (shopInventoryOpen ? "Inventář" : "Obchod") + " — " + player.getGold() + " zlata";
        shopColumnsLabel.setText(formatShopRow("Předmět", "Účinek", "Cena", shopInventoryOpen ? "Vybaveno" : "Zásoba"));
        updateShopRows(shopOfferButtons, new ArrayList<>(game.getShop().getStock().keySet()), !shopInventoryOpen, true);
        updateShopRows(shopInventoryButtons, player.getInventory(), shopInventoryOpen, false);
        shopContinueButton.setText(game.willStartBattleOnContinue() ? "Zahájit souboj" : "Pokračovat → " + game.getOpponent().getName());
    }

    private void updateShopRows(MiniTui.Button[] buttons, List<Item> items, boolean visible, boolean offers) {
        for (int row = 0; row < buttons.length; row++) {
            MiniTui.Button button = buttons[row];
            button.visible = visible && row < items.size();
            if (!button.visible) continue;

            // Stejna tlacitka pri dalsi navsteve obchodu dostanou novy text a novou akci.
            Item item = items.get(row);
            int price = offers ? item.getPrice() : item.getPrice() / 2;
            String quantity = offers ? game.getShop().getStock().get(item) + " ks"
                    : (game.getCurrentPlayer().getEquipment().isEquipped(item) ? "*" : "");
            button.setText(formatShopRow(item.getName(), text.summary(item), price + " zl.", quantity));
            button.action = () -> inspect(item, offers);
        }
    }

    // ------------------------------------ mezery drzi nazvy, ucinky a ceny ve stejnych sloupcich ------------------------------------

    private String formatShopRow(String name, String effect, String price, String quantity) {
        // %-24s a %-32s doplni mezery vpravo, %7s zarovna cenu doprava.
        // fit() nejdrive zkrati dlouhy text, aby neposunul dalsi sloupec.
        return String.format("%-24s | %-32s | %7s | %s", fit(name, 24), fit(effect, 32), price, quantity);
    }

    // ------------------------------------ vybava ukazuje cenu, o platnosti pokusu rozhoduje hra ------------------------------------

    private void updateCombat() {
        Player player = game.getCurrentPlayer();
        combatEquipmentPanel.title = "Výbava — výdrž " + player.getStamina() + "/" + player.getMaxStamina();

        // Levy seznam obsahuje aktivni predmety vcetne pesti; zbroj a talisman jsou pasivni.
        List<Item> items = new ArrayList<>();
        for (Item item : player.getInventory()) if (!item.isEquipment()) items.add(item);
        for (int row = 0; row < combatEquipmentButtons.length; row++) {
            MiniTui.Button button = combatEquipmentButtons[row];
            button.visible = row < items.size();
            if (!button.visible) continue;
            Item item = items.get(row);
            combatEquipmentItems[row] = item;
            button.setText(fit(item.getName(), 24) + " · " + game.getActionCost(item) + " výd.");
        }

        // Pravy seznam je jen viditelny vyrez celeho planu. Jeho ulozeny obsah se zde nemeni.
        List<Item> plan = player.getActions();
        int pages = Math.max(1, (plan.size() + PLAN_ROWS - 1) / PLAN_ROWS);
        // Po odebrani posledni akce na strance se vratime na nejblizsi existujici stranku.
        combatPlanPage = Math.max(0, Math.min(combatPlanPage, pages - 1));
        for (int row = 0; row < combatPlanButtons.length; row++) {
            int index = combatPlanPage * PLAN_ROWS + row;
            combatPlanButtons[row].visible = index < plan.size();
            if (index < plan.size()) combatPlanButtons[row].setText((index + 1) + ". "
                    + fit(plan.get(index).getName(), 25) + " · " + game.getPlannedActionCost(index));
        }
        combatPlanPreviousButton.visible = combatPlanPage > 0;
        combatPlanNextButton.visible = combatPlanPage + 1 < pages;
        combatPlanPageLabel.setText(pages > 1 ? (combatPlanPage + 1) + "/" + pages : "");
    }

    // ------------------------------------ detail predmetu a udaje hrace ------------------------------------

    private void updateDetail() {
        boolean shopping = game.getPhase() == Game.Phase.SHOP;
        detailBuyButton.visible = shopping && detailIsOffer;
        detailSellButton.visible = shopping && !detailIsOffer;
        detailEquipButton.visible = detailSellButton.visible && detailItem.isEquipment();
        detailAddButton.visible = game.getPhase() == Game.Phase.COMBAT && !detailItem.isEquipment();
        detailBuyButton.setText("Koupit za " + detailItem.getPrice());
        detailSellButton.setText("Prodat za " + detailItem.getPrice() / 2);
        if (detailAddButton.visible) detailAddButton.setText("Přidat do plánu · " + game.getActionCost(detailItem) + " výdrže");
        updateDocument(detailItem.getName(), wrapText(List.of(text.description(detailItem))));
    }

    private void updatePlayerDetails() {
        Player player = game.getPlayer(playerDetailsIndex);
        List<String> details = new ArrayList<>(List.of(player.getName(),
                "Životy: " + player.getHealth() + "/" + Player.MAX_HEALTH,
                "Výdrž: " + player.getStamina() + "/" + player.getMaxStamina(),
                "Příští tah navíc: " + player.getNextStamina(), "Zlato: " + player.getGold(), "Účinky:"));
        for (Damage effect : player.getEffects()) details.add(text.effect(effect));
        details.add("Vybavení a inventář:");
        for (Item item : player.getInventory()) details.add(item.getName() + " — " + text.summary(item)
                + (player.getEquipment().isEquipped(item) ? " [vybaveno]" : ""));
        updateDocument("Podrobnosti hráče", wrapText(details));
    }

    // ------------------------------------ zalomeni textu a zobrazeni jedne stranky ------------------------------------

    private List<String> wrapText(List<String> paragraphs) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : paragraphs) {
            for (String original : paragraph.split("\\R", -1)) {
                String line = "";
                for (String word : original.split(" ")) {
                    if (!line.isEmpty() && line.length() + word.length() + 1 > 91) {
                        lines.add(line);
                        line = "";
                    }
                    if (!line.isEmpty()) line += " ";
                    line += word;
                }
                lines.add(line);
            }
            lines.add("");
        }
        return lines;
    }

    private void updateDocument(String title, List<String> lines) {
        documentPanel.title = title;
        int pages = Math.max(1, (lines.size() + TEXT_ROWS - 1) / TEXT_ROWS);
        documentPages[layer] = Math.max(0, Math.min(documentPages[layer], pages - 1));
        for (int row = 0; row < TEXT_ROWS; row++) {
            int index = documentPages[layer] * TEXT_ROWS + row;
            documentRows[row].setText(index < lines.size() ? lines.get(index) : "");
        }
        documentPreviousButton.visible = documentPages[layer] > 0;
        documentNextButton.visible = documentPages[layer] + 1 < pages;
        documentPageLabel.setText(pages > 1 ? (documentPages[layer] + 1) + "/" + pages : "");
    }

    // ------------------------------------ velikost terminalu a vychozi vyber pri prechodu ------------------------------------

    private void checkTerminalSize() {
        sizeWarningLabel.setText("Terminál " + tui.getTerminalWidth() + " × " + tui.getTerminalHeight()
                + ": potřebujeme alespoň " + WIDTH + " × " + HEIGHT + " znaků.");
        int visibleLayer = tui.getTerminalWidth() < WIDTH || tui.getTerminalHeight() < HEIGHT ? SIZE_WARNING : layer;
        // Pri malem okne zmenime pouze zobrazovanou vrstvu; layer si pamatuje skutecne menu.
        if (tui.getLayer() != visibleLayer) {
            tui.setLayer(visibleLayer);
            focusDefault();
        }
    }

    private void focusDefault() {
        if (tui.getLayer() == SIZE_WARNING) return;
        switch (layer) {
            case NAMES -> tui.focus(namesFirstInput);
            case SHOP -> {
                MiniTui.Button[] rows = shopInventoryOpen ? shopInventoryButtons : shopOfferButtons;
                tui.focus(rows[0].visible ? rows[0] : shopContinueButton);
            }
            case COMBAT -> tui.focus(combatEquipmentButtons[0]);
            case DETAIL -> tui.focus(detailBuyButton.visible ? detailBuyButton
                    : detailSellButton.visible ? detailSellButton : detailAddButton);
            case RESULT -> tui.focus(resultContinueButton);
            default -> tui.focus(menuBackButton);
        }
    }

    private String ownedName(Item item) {
        return item == null ? "—" : item.getName();
    }

    private String fit(String value, int width) {
        return value.length() <= width ? value : value.substring(0, width - 1) + "…";
    }
}
