import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Celý průběh lokální hry 1v1; vstup i výstup zajišťuje MiniTui. */
public class Main {
    private final MiniTui tui = new MiniTui();
    private final Shop shop = new Shop();
    private final Random random = new Random();
    private final Player[] players = new Player[2];
    private final boolean[] defending = new boolean[2];
    private final List<MiniTui.Element> setup = new ArrayList<>();
    private final List<MiniTui.Element> shopping = new ArrayList<>();
    private final List<MiniTui.Element> combat = new ArrayList<>();
    private final MiniTui.Label stats;
    private final MiniTui.Label details;
    private final MiniTui.Label message;
    private int turn;
    private boolean fighting;
    private boolean finished;

    public Main() {
        tui.add(new MiniTui.Title(2, 1, 0, "DUEL 1v1"));
        stats = tui.add(new MiniTui.Label(3, 3, 0, "Zadejte jména obou hráčů."));
        details = tui.add(new MiniTui.Label(3, 4, 0, ""));
        message = tui.add(new MiniTui.Label(3, 6, 0, ""));
        MiniTui.Input first = add(setup, new MiniTui.Input(3, 8, 0, "Hráč 1", "Pepa", 18));
        MiniTui.Input second = add(setup, new MiniTui.Input(3, 10, 0, "Hráč 2", "Mahulena", 18));
        add(setup, new MiniTui.Button(3, 12, 0, "Začít přípravu", () -> {
            players[0] = new Player(name(first, "Pepa"));
            players[1] = new Player(name(second, "Mahulena"));
            show(setup, false);
            show(shopping, true);
            message.setText("Každý má 10 zlata. Nakupte vybavení, pak potvrďte připravenost.");
            tui.setLayer(0);
        }));

        add(shopping, new MiniTui.Button(3, 8, 0, "Lektvar (3g)", () -> buy(ItemType.POTION)));
        add(shopping, new MiniTui.Button(3, 10, 0, "Meč (5g)", () -> buy(ItemType.SWORD)));
        add(shopping, new MiniTui.Button(3, 12, 0, "Štít (4g)", () -> buy(ItemType.SHIELD)));
        add(shopping, new MiniTui.Button(3, 14, 0, "Připraven", this::ready));
        add(combat, new MiniTui.Button(3, 8, 0, "Útok", this::attack));
        add(combat, new MiniTui.Button(3, 10, 0, "Obrana", () -> {
            if (!canAct()) return;
            defending[turn] = true;
            endTurn(players[turn].getName() + " se brání.");
        }));
        add(combat, new MiniTui.Button(3, 12, 0, "Použít lektvar", this::heal));
        tui.add(new MiniTui.Button(3, 17, 0, "Konec", tui::stop));
        show(shopping, false);
        show(combat, false);
        // Stejná obrazovka se aktualizuje v reálném čase; žádné Scanner prompty.
        tui.onTick(this::refresh);
        tui.setStatus("Šipky: výběr | Enter: potvrdit / upravit | Ctrl+C: konec");
    }

    private <T extends MiniTui.Element> T add(List<MiniTui.Element> group, T element) {
        group.add(element);
        return tui.add(element);
    }

    private void show(List<MiniTui.Element> group, boolean visible) {
        for (MiniTui.Element element : group) element.visible = visible;
    }

    private String name(MiniTui.Input input, String fallback) {
        String value = input.getValue().trim();
        return value.isEmpty() ? fallback : value;
    }

    private void buy(ItemType type) {
        if (fighting || players[turn] == null) return;
        message.setText(shop.buy(players[turn], type) ? "Koupeno: " + type
                : "Nedostatek zlata nebo plný inventář.");
    }

    private void ready() {
        if (fighting || players[turn] == null) return;
        if (turn == 0) {
            turn = 1;
            message.setText("Nyní nakupuje druhý hráč.");
        } else {
            turn = 0;
            fighting = true;
            show(shopping, false);
            show(combat, true);
            message.setText("Souboj začíná. Meč: +1 útok; štít: -1 poškození.");
            tui.setLayer(0);
        }
    }

    private boolean canAct() { return fighting && !finished; }

    private int findItem(Player player, ItemType type) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (player.getInventory().getItem(i).getItemType() == type) return i;
        }
        return -1;
    }

    private void attack() {
        if (!canAct()) return;
        Player actor = players[turn];
        Player opponent = players[1 - turn];
        int damage = 1 + random.nextInt(2);
        // Více stejných zbraní či štítů bonus nesčítá.
        if (findItem(actor, ItemType.SWORD) >= 0) damage++;
        if (findItem(opponent, ItemType.SHIELD) >= 0) damage--;
        if (defending[1 - turn]) damage--;
        damage = Math.max(0, damage);
        opponent.takeDamage(damage);
        endTurn(actor.getName() + " útočí za " + damage + ".");
    }

    private void heal() {
        if (!canAct()) return;
        Player actor = players[turn];
        int index = findItem(actor, ItemType.POTION);
        if (index < 0 || actor.getHealth() == Player.MAX_HEALTH) {
            message.setText("Chybí lektvar nebo máš plné zdraví. Tah pokračuje.");
            return;
        }
        actor.getInventory().removeItem(index);
        actor.heal(2);
        endTurn(actor.getName() + " použil lektvar (+2 HP, nejvýše do maxima).");
    }

    private void endTurn(String result) {
        if (!players[1 - turn].isAlive()) {
            finished = true;
            players[turn].receiveGold(1);
            show(combat, false);
            message.setText(result + " Vítězí " + players[turn].getName() + " (+1g)!");
            tui.setLayer(0);
            return;
        }
        turn = 1 - turn;
        // Obrana končí na začátku dalšího vlastního tahu.
        defending[turn] = false;
        message.setText(result);
    }

    private void refresh() {
        if (players[0] == null) return;
        stats.setText(players[0].getName() + ": " + players[0].getHealth() + " HP, "
                + players[0].getGold() + "g | " + players[1].getName() + ": "
                + players[1].getHealth() + " HP, " + players[1].getGold() + "g");
        Player current = players[turn];
        details.setText((finished ? "Vítěz: " : fighting ? "Na tahu: " : "Nakupuje: ")
                + current.getName() + " | Předměty: " + current.getInventory().size() + "/5");
    }

    public static void main(String[] args) {
        new Main().tui.start();
    }
}
