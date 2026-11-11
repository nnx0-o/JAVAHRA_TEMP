import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class BalanceSimulation {

    private static final int TURN_LIMIT = 160;
    private static String policy = "balanced";

    public static void main(String[] args) {
        policy = args.length < 3 ? "balanced" : args[2];
        System.out.println("policy=" + policy);
        duel("daggers vs fire", new String[]{"Dýka", "Dýka", "Dýka"}, new String[]{"Pochodeň", "Pochodeň", "Pochodeň"});
        duel("daggers vs poison", new String[]{"Dýka", "Dýka", "Dýka"}, new String[]{"Jedovatá jehla", "Jedovatá jehla", "Jedovatá jehla"});
        duel("fire mirror", new String[]{"Pochodeň", "Pochodeň", "Pochodeň"}, new String[]{"Pochodeň", "Pochodeň", "Pochodeň"});
        duel("shield mirror", new String[]{"Dýka", "Kožený štít", "Kožený štít"}, new String[]{"Dýka", "Kožený štít", "Kožený štít"});
        duel("regeneration mirror", new String[]{"Kožená zbroj", "Prsten obnovy"}, new String[]{"Kožená zbroj", "Prsten obnovy"});
        duel("poison vs cleanse", new String[]{"Jedovatá jehla", "Jedovatá jehla", "Jedovatá jehla"}, new String[]{"Dýka", "Hadí kámen"});
        duel("fire vs cleanse", new String[]{"Pochodeň", "Pochodeň", "Pochodeň"}, new String[]{"Dýka", "Kapka deště"});
        duel("physical vs plate", new String[]{"Dýka", "Dýka", "Dýka"}, new String[]{"Dýka", "Plátová zbroj"});
        duel("poison vs threshold", new String[]{"Jedovatá jehla", "Jedovatá jehla", "Jedovatá jehla"}, new String[]{"Dýka", "Zpevněná vesta"});
        randomMatches(args.length == 0 ? 300 : Integer.parseInt(args[0]),
                args.length < 2 ? 0 : Integer.parseInt(args[1]));
    }

    private static Item item(String name) {
        return ItemCatalog.getItems().stream().filter(item -> item.getName().equals(name)).findFirst().orElseThrow();
    }

    private static void duel(String label, String[] left, String[] right) {
        int[] wins = new int[2];
        int stalled = 0;
        int totalTurns = 0;
        for (int starter = 0; starter < 2; starter++) {
            final int choice = starter;
            Game game = new Game("Left", "Right", new Random(1) {
                private boolean first = true;
                @Override public int nextInt(int bound) {
                    if (first) { first = false; return choice; }
                    return super.nextInt(bound);
                }
            });
            game.openShop();
            for (int player = 0; player < 2; player++) {
                for (String name : player == 0 ? left : right) {
                    Item piece = item(name).copy();
                    game.getPlayer(player).addToInventory(piece);
                    if (piece.getItemType() == ItemType.ARMOR) game.getPlayer(player).getEquipment().equipArmor(piece);
                    if (piece.getItemType() == ItemType.TRINKET) game.getPlayer(player).getEquipment().equipTrinket(piece);
                }
            }
            game.continueShop(); game.continueShop();
            while (game.getPhase() == Game.Phase.COMBAT && game.getTurn() < TURN_LIMIT) playTurn(game);
            if (game.getPhase() == Game.Phase.COMBAT) stalled++;
            else wins[game.getWinner()]++;
            totalTurns += Math.min(TURN_LIMIT, game.getTurn() + 1);
        }
        System.out.printf("%s: left=%d right=%d stalled=%d meanTurns=%.1f%n", label, wins[0], wins[1], stalled, totalTurns / 2.0);
    }

    private static void randomMatches(int count, int seedOffset) {
        int stalled = 0;
        int starterWins = 0;
        int completed = 0;
        int battles = 0;
        int battleTurns = 0;
        int shopsWithoutWeapons = 0;
        int firstBattleShopperWins = 0;
        int firstBattles = 0;
        for (int seed = 0; seed < count; seed++) {
            Game game = new Game("A", "B", new Random((seed + seedOffset) * 7919L));
            int initialShopper = game.getShopStarter();
            int steps = 0;
            while (game.getPhase() != Game.Phase.FINISHED && steps++ < 1500) {
                if (game.getPhase() == Game.Phase.ANNOUNCEMENT) {
                    game.openShop();
                    if (game.getShop().getStock().keySet().stream().noneMatch(i -> i.getItemType() == ItemType.DAMAGE && i.getPrice() <= 15)) shopsWithoutWeapons++;
                } else if (game.getPhase() == Game.Phase.SHOP) {
                    shop(game);
                    game.continueShop();
                } else if (game.getPhase() == Game.Phase.COMBAT) {
                    if (game.getTurn() >= TURN_LIMIT) break;
                    playTurn(game);
                    if (game.getPhase() != Game.Phase.COMBAT) {
                        battles++;
                        battleTurns += game.getTurn() + 1;
                        if (game.getBattle() == 1) {
                            firstBattles++;
                            if (game.getWinner() == initialShopper) firstBattleShopperWins++;
                        }
                    }
                } else game.nextBattle();
            }
            if (game.getPhase() == Game.Phase.FINISHED) {
                completed++;
                if (game.getWinner() == initialShopper) starterWins++;
            } else stalled++;
        }
        System.out.printf("random matches: count=%d complete=%d stalled=%d initialShopperWins=%d/%d firstBattleShopperWins=%d/%d battles=%d meanBattleTurns=%.2f shopsWithoutWeapons=%d%n",
                count, completed, stalled, starterWins, completed, firstBattleShopperWins, firstBattles, battles,
                battles == 0 ? 0 : (double) battleTurns / battles, shopsWithoutWeapons);
    }

    private static void shop(Game game) {
        Player actor = game.getCurrentPlayer();
        // jednoducha strategie dava prednost utoku a potom obrane podle ceny
        for (int attempt = 0; attempt < 10; attempt++) {
            Item best = game.getShop().getStock().entrySet().stream()
                    .filter(entry -> entry.getValue() > 0 && entry.getKey().getPrice() <= actor.getGold())
                    .map(java.util.Map.Entry::getKey)
                    .filter(i -> purchaseValue(actor, i) > 0)
                    .max(Comparator.comparingDouble(i -> purchaseValue(actor, i) / i.getPrice())).orElse(null);
            if (best == null || actor.getInventory().size() >= actor.getInventorySize()) break;
            game.buy(best);
            Item owned = actor.getInventory().get(actor.getInventory().size() - 1);
            if (owned.isEquipment()) game.equip(owned);
        }
    }

    private static double purchaseValue(Player actor, Item item) {
        int weaponCost = actor.getInventory().stream().filter(i -> i.getItemType() == ItemType.DAMAGE).mapToInt(Item::getStaminaCost).sum();
        boolean shield = actor.getInventory().stream().anyMatch(i -> i.getItemType() == ItemType.DEFEND);
        return policyWeight(item.getItemType()) * switch (item.getItemType()) {
            case DAMAGE -> weaponCost + item.getStaminaCost() <= actor.getMaxStamina() - (shield ? 3 : 0)
                    ? item.getPower() * (item.getDamageType() == Damage.DamageType.PHYSICAL ? 1 : 0.8) : 0;
            case DEFEND -> !shield && weaponCost <= 9 ? item.getPower() * 0.8 : 0;
            case ARMOR -> actor.getEquipment().getEquippedArmor() == null ? 3 : 0;
            case TRINKET -> actor.getEquipment().getEquippedTrinket() == null ? 3 : 0;
            case HEAL -> 1;
            case STAMINA -> 0.5;
        };
    }

    public static void playTurn(Game game) {
        Player actor = game.getCurrentPlayer();
        boolean preparation = game.isPreparation();
        List<Item> items = new ArrayList<>(actor.getInventory());
        // bezplatny utok nejprve odcerpa stit a nemeni rozdeleni vydrze
        if (!preparation) game.act(Player.Action.attack());
        items.sort(Comparator.comparingDouble((Item i) -> actionValue(actor, i, preparation) / Math.max(1, i.getStaminaCost())).reversed());
        for (Item item : items) {
            if (game.getPhase() != Game.Phase.COMBAT) return;
            if (actionValue(actor, item, preparation) > 0) game.act(Player.Action.use(item));
        }
        if (game.getPhase() == Game.Phase.COMBAT) game.endTurn();
    }

    private static double policyWeight(ItemType type) {
        if (policy.equals("defensive")) return type == ItemType.DEFEND || type == ItemType.HEAL ? 1.8 : 1;
        if (policy.equals("aggressive")) return type == ItemType.DAMAGE ? 1.8 : 1;
        return 1;
    }

    private static double actionValue(Player actor, Item item, boolean preparation) {
        return policyWeight(item.getItemType()) * switch (item.getItemType()) {
            case DAMAGE -> preparation ? 0 : item.getPower() * (item.getDamageType() == Damage.DamageType.PHYSICAL ? 1 : 0.8);
            case DEFEND -> item.getPower();
            case HEAL -> actor.getHealth() <= Player.MAX_HEALTH - item.getPower() ? item.getPower() * 1.1 : 0;
            case STAMINA -> preparation ? item.getPower() : 0;
            default -> 0;
        };
    }
}
