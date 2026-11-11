import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {

    // ------------------------------------ stav rozhrani ------------------------------------

    private enum Screen { HOME, INVENTORY, DETAIL, STATES, LOG, HELP }

    private final MiniTui tui = new MiniTui();
    private Game game;
    private Screen screen = Screen.HOME;
    private Screen returnScreen = Screen.HOME;
    private Item selectedItem;
    private boolean shopItem;
    private int page;
    private int width;
    private int height;
    private String message = "";
    private MiniTui.Input firstName;
    private MiniTui.Input secondName;

    public Main() {
        draw();
        tui.onTick(() -> {
            if (width != tui.getTerminalWidth() || height != tui.getTerminalHeight()) draw();
        });
    }

    // ------------------------------------ pomocne prvky ------------------------------------

    private void label(int row, String text) {
        tui.add(new MiniTui.Label(2, row, 0, text));
    }

    private void button(int row, String text, Runnable action) {
        tui.add(new MiniTui.Button(2, row, 0, text, action));
    }

    private void navigate(Screen next) {
        screen = next;
        page = 0;
        draw();
    }

    private int bottom() { return height - 3; }
    private int contentRows() { return Math.max(1, height - 15); }

    private void draw() {
        String first = firstName == null ? "Hráč 1" : firstName.getValue();
        String second = secondName == null ? "Hráč 2" : secondName.getValue();
        width = tui.getTerminalWidth();
        height = tui.getTerminalHeight();
        tui.clear(); // obrazovka je vzdy jedina a nelze preskocit do jine faze klavesou
        tui.setStatus("Šipky: výběr | Enter: potvrdit / upravit | Ctrl+C: konec");
        if (width < 60 || height < 24) {
            label(1, "Zvětši terminál alespoň na 60 × 24 znaků.");
            label(3, "Hra je pozastavená; stav zůstává uložený v paměti.");
            // rozepsana jmena zachovame i kdyz male okno nezobrazi vstupy
            return;
        }
        tui.add(new MiniTui.Title(2, 0, 0, "DUEL — na tři vítězství"));
        if (game == null) {
            label(2, "Zadejte jména. Po potvrzení se vylosuje pořadí.");
            firstName = tui.add(new MiniTui.Input(2, 5, 0, "První hráč", first, 20));
            secondName = tui.add(new MiniTui.Input(2, 7, 0, "Druhý hráč", second, 20));
            button(10, "Připraveni", () -> {
                game = new Game(firstName.getValue(), secondName.getValue(), new Random());
                navigate(Screen.HOME);
            });
            button(12, "Konec", tui::stop);
            tui.setLayer(0);
            return;
        }
        label(2, game.getPlayer(0) + " | Výhry: " + game.getWins(0));
        label(3, game.getPlayer(1) + " | Výhry: " + game.getWins(1));
        label(4, "Souboj " + game.getBattle() + " | " + phaseLabel() + " | " + game.getCurrentPlayer().getName());
        label(5, message);
        switch (screen) {
            case HOME -> drawHome();
            case INVENTORY -> drawInventory();
            case DETAIL -> drawDetail();
            case STATES -> drawStates();
            case LOG -> drawLog();
            case HELP -> drawHelp();
        }
        tui.setLayer(0);
    }

    private String phaseLabel() {
        return switch (game.getPhase()) {
            case ANNOUNCEMENT -> "Rozdělení pořadí";
            case SHOP -> "Obchod";
            case COMBAT -> game.isPreparation() ? "Přípravný tah" : "Bojový tah " + (game.getTurn() + 1);
            case RESULT -> "Výsledek souboje";
            case FINISHED -> "Konec zápasu";
        };
    }

    // ------------------------------------ hlavni obrazovky ------------------------------------

    private void drawHome() {
        switch (game.getPhase()) {
            case ANNOUNCEMENT -> {
                label(7, "První v obchodě: " + game.getPlayer(game.getShopStarter()).getName());
                label(9, "První v souboji: " + game.getPlayer(game.getCombatStarter()).getName());
                label(11, "Každý dostane jeden přípravný tah bez útoků.");
                button(13, "Otevřít sdílený obchod", () -> { game.openShop(); message = ""; draw(); });
                button(15, "Pravidla hry", () -> navigate(Screen.HELP));
                button(17, "Konec", tui::stop);
            }
            case SHOP -> drawShop();
            case COMBAT -> drawCombat();
            case RESULT, FINISHED -> {
                label(7, "Souboj vyhrává " + game.getPlayer(game.getWinner()).getName() + ".");
                label(9, "Odměny: vítěz +" + GameRules.WIN_REWARD + ", poražený +" + GameRules.LOSS_REWARD + " zlata.");
                if (game.getPhase() == Game.Phase.FINISHED) {
                    label(11, "Vítěz zápasu: " + game.getPlayer(game.getWinner()).getName() + "!");
                    button(13, "Nový zápas", () -> { game = null; message = ""; draw(); });
                } else {
                    label(11, "Zlato a vybavení zůstávají. Pořadí se příště obrátí.");
                    button(13, "Další souboj", () -> { game.nextBattle(); message = ""; draw(); });
                }
                button(15, "Průběh souboje", () -> navigate(Screen.LOG));
                button(17, "Konec", tui::stop);
            }
        }
    }

    private void drawShop() {
        List<Item> offers = new ArrayList<>(game.getShop().getStock().keySet());
        int pageSize = Math.max(1, contentRows() - 2);
        page = Math.min(page, (offers.size() - 1) / pageSize);
        label(7, "Společná nabídka: 10 druhů | Beze změny: " + game.getUnchangedVisits() + "/2");
        for (int row = 0, index = page * pageSize; row < pageSize && index < offers.size(); row++, index++) {
            Item item = offers.get(index);
            button(8 + row, item.getName() + " | " + item.getPrice() + " zlata | "
                    + game.getShop().getStock().get(item) + " ks | " + item.getRarity(), () -> detail(item, true));
        }
        int row = bottom() - 5;
        button(row, "Další stránka nabídky (" + (page + 1) + "/" + pages(offers.size(), pageSize) + ")",
                () -> { page = (page + 1) % pages(offers.size(), pageSize); draw(); });
        button(row + 1, "Inventář — vybavit, prodat, přečíst popis", () -> navigate(Screen.INVENTORY));
        button(row + 2, "Pokračovat — předat obchod / zahájit souboj", () -> {
            game.continueShop(); page = 0; message = ""; draw();
        });
        button(row + 3, "Stavy obou hráčů", () -> navigate(Screen.STATES));
        button(row + 4, "Pravidla hry", () -> navigate(Screen.HELP));
        button(row + 5, "Konec", tui::stop);
    }

    private void drawCombat() {
        Player actor = game.getCurrentPlayer();
        label(7, game.isPreparation() ? "Příprava: použij léčení, výdrž nebo štít; útoky jsou zakázané."
                : "Akce působí ihned. Před koncem tahu můžeš použít více předmětů.");
        label(9, "Výdrž: " + actor.getStamina() + "/" + actor.getMaxStamina()
                + " | Příští tah navíc: " + actor.getNextStamina());
        button(11, "Základní útok (zdarma, " + GameRules.BASIC_DAMAGE + ", jednou za tah)",
                () -> { message = game.act(Player.Action.attack()); draw(); });
        button(12, "Předměty — použít / přečíst popis", () -> navigate(Screen.INVENTORY));
        button(13, "Stavy, vybavení a účinky obou hráčů", () -> navigate(Screen.STATES));
        button(14, "Průběh souboje", () -> navigate(Screen.LOG));
        button(16, "Ukončit tah — hraje soupeř", () -> { game.endTurn(); message = ""; draw(); });
        button(18, "Pravidla hry", () -> navigate(Screen.HELP));
        button(19, "Konec", tui::stop);
    }

    // ------------------------------------ predmety ------------------------------------

    private void drawInventory() {
        Player actor = game.getCurrentPlayer();
        List<Item> items = actor.getInventory();
        int pageSize = contentRows();
        page = Math.min(page, Math.max(0, pages(items.size(), pageSize) - 1));
        label(7, "Inventář: " + items.size() + "/" + actor.getInventorySize() + " | Vyber předmět pro popis a akce.");
        for (int row = 0, index = page * pageSize; row < pageSize && index < items.size(); row++, index++) {
            Item item = items.get(index);
            String suffix = actor.getEquipment().isEquipped(item) ? " [vybaveno]" : actor.hasUsed(item) ? " [použito]" : "";
            button(8 + row, item.getName() + " | " + item.getItemType() + suffix, () -> detail(item, false));
        }
        if (items.isEmpty()) label(9, "Inventář je prázdný.");
        button(bottom() - 2, "Další stránka (" + (page + 1) + "/" + pages(items.size(), pageSize) + ")",
                () -> { page = (page + 1) % pages(items.size(), pageSize); draw(); });
        button(bottom(), "Zpět", () -> navigate(Screen.HOME));
    }

    private void detail(Item item, boolean fromShop) {
        selectedItem = item;
        shopItem = fromShop;
        returnScreen = screen;
        navigate(Screen.DETAIL);
    }

    private void drawDetail() {
        Item item = selectedItem;
        label(7, item.getName() + " | " + item.getItemType() + " | " + item.getRarity());
        label(8, "Cena: " + item.getPrice() + " | Prodej: " + item.getPrice() / 2
                + " | Výdrž: " + item.getStaminaCost() + (shopItem ? " | Skladem: " + game.getShop().getStock().get(item) : ""));
        List<String> lines = wrap(item.getDescription(), width - 6);
        int pageSize = Math.max(1, height - 18);
        showTextPage(lines, 10, pageSize);
        button(bottom() - 4, "Další část popisu (" + (page + 1) + "/" + pages(lines.size(), pageSize) + ")",
                () -> { page = (page + 1) % pages(lines.size(), pageSize); draw(); });
        if (game.getPhase() == Game.Phase.SHOP) {
            if (shopItem) button(bottom() - 3, "Koupit jeden kus", () -> { message = game.buy(item); draw(); });
            else {
                button(bottom() - 3, "Prodat za " + item.getPrice() / 2 + " zlata", () -> {
                    message = game.sell(item);
                    if (!game.getCurrentPlayer().getInventory().contains(item)) navigate(Screen.INVENTORY);
                    else draw();
                });
                if (item.isEquipment()) button(bottom() - 2, "Vybavit / sundat", () -> { message = game.equip(item); draw(); });
            }
        } else if (game.getPhase() == Game.Phase.COMBAT && !shopItem && !item.isEquipment()) {
            button(bottom() - 3, "Použít předmět", () -> {
                message = game.act(Player.Action.use(item));
                if (game.getPhase() != Game.Phase.COMBAT) navigate(Screen.HOME);
                else if (!game.getCurrentPlayer().getInventory().contains(item)) navigate(Screen.INVENTORY);
                else draw();
            });
        }
        button(bottom(), "Zpět", () -> navigate(returnScreen));
    }

    // ------------------------------------ verejne stavy a napoveda ------------------------------------

    private void drawStates() {
        List<String> lines = new ArrayList<>();
        for (int index = 0; index < 2; index++) {
            Player player = game.getPlayer(index);
            lines.add(player.toString());
            lines.add("Příští tah: +" + player.getNextStamina() + " výdrže; inventář "
                    + player.getInventory().size() + "/" + player.getInventorySize());
            lines.add("Zbroj: " + itemName(player.getEquipment().getEquippedArmor()));
            lines.add("Talisman: " + itemName(player.getEquipment().getEquippedTrinket()));
            if (player.getEffects().isEmpty()) lines.add("Žádné časované účinky.");
            for (Damage effect : player.getEffects()) lines.add(effect.toString());
            lines.add("Předměty: " + (player.getInventory().isEmpty() ? "žádné" : player.getInventory().toString()));
            lines.add("");
        }
        textScreen(lines);
    }

    private String itemName(Item item) { return item == null ? "žádný předmět" : item.getName(); }

    private void drawLog() {
        List<String> lines = new ArrayList<>(game.getLog());
        java.util.Collections.reverse(lines); // nejnovejsi udalost je videt hned bez listovani
        textScreen(lines);
    }

    private void drawHelp() {
        textScreen(List.of(
                "Vyhrává první hráč se třemi vítěznými souboji. Zdraví a bojové účinky se mezi souboji obnoví; zlato a předměty zůstávají.",
                "Začínáš s " + GameRules.STARTING_GOLD + " zlatými. Vítěz dostane " + GameRules.WIN_REWARD + ", poražený " + GameRules.LOSS_REWARD + ". Prodej vrátí polovinu ceny zaokrouhlenou dolů.",
                "Obchod má 10 společných nabídek. Koupené kusy ubývají oběma hráčům. Prodané předměty zmizí. Nový sklad vznikne až před dalším soubojem.",
                "Pokračovat předá obchod druhému hráči. Dvě návštěvy za sebou bez nákupu a prodeje zahájí boj. Změna vybavení není nákup ani prodej.",
                "Pořadí se na začátku losuje. Druhý nakupující začíná boj. V příštím souboji se pořadí obrátí bez ohledu na vítěze.",
                "Každý má jeden přípravný tah bez útoků. Potom vybíráš akce, které působí ihned. Konec tahu předá hru soupeři.",
                "Výdrž se obnoví na začátku vlastního tahu. Těžká zbroj snižuje základ; bonus z lektvaru nebo talismanu může jít nad maximum.",
                "Nejprve působí vybavený talisman, pak časované účinky v pořadí vzniku. Smrt okamžitě ukončí souboj, pozdější léčení už nepomůže.",
                "Štít pohlcuje společnou zásobu poškození všech typů po zbroji. Vydrží i úvodní účinky příštího vlastního tahu, potom zanikne.",
                "Každý kus zbraně a štítu použiješ jednou za vlastní tah. Léčivé a výdržové lektvary se spotřebují. Základní útok je jednou za tah zdarma.",
                "Oheň sloučí zbývající celkové poškození a ponechá delší trvání. Jed se neslučuje. Dávka = zbývající součet / zbývající tahy, zaokrouhleno na nejbližší celé číslo.",
                "Příklad ohně: 5 + 10 poškození na 4 tahy = dávky 4, 4, 4, 3. Poloviny se zaokrouhlují nahoru. Pouze prodejní ceny se zaokrouhlují dolů.",
                "Zbroj a talisman musíš vybavit v obchodě. Každý zabírá místo v inventáři. Brašna rozšiřuje kapacitu jen při vybavení; při sundání se vše musí vejít.",
                "Vzácnost má váhy 4, 3 a 2. Zbraně do 7 zlata mají trojnásobnou váhu výběru. Každá nabídka je náhodná, žádný druh není zaručen. Počet kusů má každý předmět vlastní rozsah.",
                "Hra je pro dva hráče u jedné klávesnice. Všechny stavy jsou veřejné. Průběh událostí najdeš v bojovém záznamu."));
    }

    private void textScreen(List<String> paragraphs) {
        List<String> lines = new ArrayList<>();
        for (String paragraph : paragraphs) lines.addAll(wrap(paragraph, width - 6));
        int pageSize = contentRows();
        showTextPage(lines, 7, pageSize);
        button(bottom() - 2, "Další stránka (" + (page + 1) + "/" + pages(lines.size(), pageSize) + ")",
                () -> { page = (page + 1) % pages(lines.size(), pageSize); draw(); });
        button(bottom(), "Zpět", () -> navigate(Screen.HOME));
    }

    private void showTextPage(List<String> lines, int row, int size) {
        page = Math.min(page, pages(lines.size(), size) - 1);
        for (int offset = 0, index = page * size; offset < size && index < lines.size(); offset++, index++) {
            label(row + offset, lines.get(index));
        }
    }

    private int pages(int count, int size) { return Math.max(1, (count + size - 1) / size); }

    private List<String> wrap(String text, int columns) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() > 0 && line.length() + word.length() + 1 > columns) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        lines.add(line.toString());
        return lines;
    }

    public static void main(String[] args) {
        new Main().tui.start();
    }
}
