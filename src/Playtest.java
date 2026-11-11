import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Playtest {

    // ------------------------------------ textovy pristup ke stejne hre ------------------------------------

    private final Game game;

    public Playtest(long seed) {
        game = new Game("Hráč 1", "Hráč 2", new Random(seed));
    }

    private void state() {
        System.out.println("\nFáze: " + phaseLabel() + " | Souboj: " + game.getBattle()
                + " | " + (game.isPreparation() ? "Příprava" : "Kolo: " + game.getCombatRound())
                + " (" + game.getTurnPosition() + ") | Na řadě: " + game.getCurrentPlayer().getName());
        for (int index = 0; index < 2; index++) {
            Player player = game.getPlayer(index);
            System.out.println(player + " | Výhry: " + game.getWins(index));
            System.out.println("  Účinky: " + player.getEffects().stream().map(new GameText(false)::effect).toList());
            System.out.println("  Zbroj: " + itemName(player.getEquipment().getEquippedArmor())
                    + " | Talisman: " + itemName(player.getEquipment().getEquippedTrinket()));
        }
        System.out.println("Příprava: " + (game.isPreparation() ? "ano" : "ne")
                + " | První v obchodě: " + game.getPlayer(game.getShopStarter()).getName());
        List<Item> inventory = game.getCurrentPlayer().getInventory();
        System.out.println("Inventář aktuálního hráče:");
        for (int i = 0; i < inventory.size(); i++) {
            Item item = inventory.get(i);
            System.out.println("  " + (i + 1) + ": " + item + " | Výdrž dalšího použití: "
                    + (item.isEquipment() ? "pasivní" : game.getActionCost(Player.Action.use(item)))
                    + (item.isConsumable() && game.getCurrentPlayer().hasUsed(item) ? " | Již v plánu" : ""));
        }
        if (game.getPhase() == Game.Phase.COMBAT) {
            GameText language = new GameText(false);
            System.out.println("Pěst | Výdrž dalšího použití: " + game.getActionCost(Player.Action.attack()));
            System.out.println("Plán akcí — vyhodnotí se shora dolů při ukončení tahu:");
            List<Player.Action> plan = game.getCurrentPlayer().getActions();
            if (plan.isEmpty()) System.out.println("  Zatím žádné akce.");
            for (int i = 0; i < plan.size(); i++) {
                Player.Action action = plan.get(i);
                System.out.println("  " + (i + 1) + ": " + language.name(action.basicAttack() ? null : action.item())
                        + " | Rezervovaná výdrž: " + game.getPlannedActionCost(i));
            }
        }
        if (game.getPhase() == Game.Phase.SHOP) {
            System.out.println("Sdílený obchod:");
            int index = 1;
            for (var offer : game.getShop().getStock().entrySet()) {
                System.out.println("  " + index++ + ": " + offer.getKey() + " | " + offer.getKey().getPrice()
                        + " zlata | " + offer.getValue() + " ks");
            }
        }
    }

    private String itemName(Item item) {
        return item == null ? "žádný předmět" : item.toString();
    }

    private String phaseLabel() {
        return switch (game.getPhase()) {
            case ANNOUNCEMENT -> "Pořadí hráčů";
            case SHOP -> "Obchod";
            case COMBAT -> "Souboj";
            case RESULT -> "Výsledek souboje";
            case FINISHED -> "Konec zápasu";
        };
    }

    private Item select(String number, boolean offer) {
        List<Item> items = offer ? new ArrayList<>(game.getShop().getStock().keySet()) : game.getCurrentPlayer().getInventory();
        int index = Integer.parseInt(number) - 1;
        if (index < 0 || index >= items.size()) throw new IllegalArgumentException("Neplatné číslo předmětu.");
        return items.get(index);
    }

    public void run(Scanner input) {
        System.out.println("Textová zkušební verze — používá stejná pravidla jako hlavní hra.");
        System.out.println("Příkazy: shop, buy N, sell N, equip N, use N, attack, end, next, state, log, quit");
        System.out.println("use a attack přidají akci do plánu. end provede plán a předá tah. remove N odebere akci; up N a down N mění pořadí.");
        System.out.println("Popis nabídky: offer N | Popis vlastního předmětu: item N");
        state();
        while (input.hasNextLine()) {
            String[] parts = input.nextLine().trim().split("\\s+");
            try {
                switch (parts[0]) {
                    case "shop" -> game.openShop();
                    case "buy" -> System.out.println(game.buy(select(parts[1], true)));
                    case "sell" -> System.out.println(game.sell(select(parts[1], false)));
                    case "equip" -> System.out.println(game.equip(select(parts[1], false)));
                    case "use" -> System.out.println(game.act(Player.Action.use(select(parts[1], false))));
                    case "remove" -> System.out.println(game.removePlannedAction(Integer.parseInt(parts[1]) - 1)
                            ? "Akce odebrána." : "Akci nelze odebrat.");
                    case "up", "down" -> System.out.println(game.movePlannedAction(Integer.parseInt(parts[1]) - 1,
                            parts[0].equals("up") ? -1 : 1) ? "Pořadí změněno." : "Akci nelze přesunout.");
                    case "attack" -> System.out.println(game.act(Player.Action.attack()));
                    case "end" -> {
                        if (game.getPhase() == Game.Phase.SHOP) game.continueShop();
                        else game.endTurn();
                    }
                    case "next" -> game.nextBattle();
                    case "offer", "item" -> System.out.println(new GameText(false).description(select(parts[1], parts[0].equals("offer"))));
                    case "log" -> game.getLog().forEach(System.out::println);
                    case "state" -> { }
                    case "quit" -> { return; }
                    default -> System.out.println("Neznámý příkaz.");
                }
            } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException invalid) {
                System.out.println("Zadej platný příkaz a případně číslo předmětu.");
            }
            state(); // po kazdem kroku lze videt skutecne zmeny bez emulace obrazovky
        }
    }

    public static void main(String[] args) {
        long seed = args.length == 0 ? 1 : Long.parseLong(args[0]);
        System.out.println("Semínko náhody: " + seed);
        new Playtest(seed).run(new Scanner(System.in));
    }
}
