import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GameTest {

    private static int assertions;

    public static void main(String[] args) {
        catalogAndStock();
        purchasesAndPasses();
        inventoryCapacity();
        turnsAndActions();
        effectsAndOrdering();
        defenseAndPassives();
        matchAndRotation();
        System.out.println("GameTest: OK (" + assertions + " checks)");
    }

    private static void catalogAndStock() {
        List<Item> catalog = ItemCatalog.getItems();
        check(catalog.size() >= 70, "catalog size");
        check(new HashSet<>(catalog.stream().map(Item::getName).toList()).size() == catalog.size(), "unique names");
        check(new HashSet<>(catalog.stream().map(Item::getItemType).toList()).size() == ItemType.values().length, "all categories");
        for (Item item : catalog) {
            check(item.getDescription().length() > 40, "described item " + item.getName());
            check(item.getPrice() >= 2 && item.getPrice() <= 16, "price range");
            check(item.copy() != item && item.copy().getName().equals(item.getName()), "independent copies");
        }
        for (int seed = 0; seed < 200; seed++) {
            Shop shop = new Shop();
            shop.refresh(new Random(seed));
            check(shop.getStock().size() == 10, "ten offers");
            for (var offer : shop.getStock().entrySet()) {
                check(offer.getValue() >= offer.getKey().getMinStock()
                        && offer.getValue() <= offer.getKey().getMaxStock(), "stock bounds");
            }
        }
    }

    private static void purchasesAndPasses() {
        Game game = new Game("A", "B", new Random(10));
        game.openShop();
        Item offer = game.getShop().getStock().keySet().iterator().next();
        Player buyer = game.getCurrentPlayer();
        buyer.changeGold(100);
        int initialStock = game.getShop().getStock().get(offer);
        int gold = buyer.getGold();
        game.buy(offer);
        check(buyer.getGold() == gold - offer.getPrice(), "payment");
        check(buyer.getInventory().get(0) != offer, "purchase is separate copy");
        check(game.getShop().getStock().get(offer) == initialStock - 1, "shared decrement");
        game.continueShop();
        check(game.getPhase() == Game.Phase.SHOP && game.getUnchangedVisits() == 0, "traded visit not a pass");
        check(game.getCurrentPlayer() != buyer, "shop alternates");
        check(game.getShop().getStock().get(offer) == initialStock - 1, "stock stays across visits");
        game.continueShop();
        check(game.getUnchangedVisits() == 1, "one empty visit");
        Item bought = buyer.getInventory().get(0);
        game.sell(bought);
        check(buyer.getGold() == gold - offer.getPrice() + offer.getPrice() / 2, "sale rounds down");
        check(game.getShop().getStock().get(offer) == initialStock - 1, "sale destroys item");
        game.continueShop();
        check(game.getUnchangedVisits() == 0, "sale breaks consecutive empty visits");
        game.continueShop();
        game.continueShop();
        check(game.getPhase() == Game.Phase.COMBAT, "two consecutive passes start battle");

        Shop shop = new Shop();
        Item dagger = item("Dýka");
        shop.addItem(dagger, dagger.getPrice(), 2);
        Player poor = new Player("Poor");
        poor.changeGold(-poor.getGold());
        check(!shop.purchase(poor, dagger) && poor.getInventory().isEmpty() && shop.getStock().get(dagger) == 2,
                "failed payment is atomic");
        poor.changeGold(100);
        poor.setInventorySize(0);
        check(!shop.purchase(poor, dagger) && poor.getGold() == 100 && shop.getStock().get(dagger) == 2,
                "full inventory is atomic");
        poor.setInventorySize(10);
        check(shop.purchase(poor, dagger) && shop.purchase(poor, dagger), "buy separate copies");
        check(poor.getInventory().get(0) != poor.getInventory().get(1), "copy identity");
        int before = poor.getGold();
        check(!shop.purchase(poor, dagger) && poor.getGold() == before, "sold out is atomic");
    }

    private static void inventoryCapacity() {
        Inventory inventory = new Inventory();
        Item bag = item("Velký batoh").copy();
        inventory.add(bag);
        check(inventory.equipTrinket(bag) && inventory.getCapacity() == 16, "equipped bag capacity");
        for (int count = 0; count < 15; count++) check(inventory.add(item("Dýka").copy()), "fill expanded bag");
        check(!inventory.add(item("Dýka").copy()), "expanded cap enforced");
        check(!inventory.remove(bag) && !inventory.equipTrinket(null), "no overflow on removal");
        for (int count = 0; count < 5; count++) inventory.remove(inventory.getItems().get(1));
        check(inventory.getItemCount() == 11 && !inventory.equipTrinket(null), "unequip retains occupied bag slot");
        check(inventory.remove(bag) && inventory.getCapacity() == 10 && inventory.getItemCount() == 10,
                "selling bag frees its own slot");
        check(inventory.getEquippedTrinket() == null, "equipment link cleared");
        check(!inventory.equipArmor(item("Prošívanice")), "cannot equip unowned item");
    }

    private static void turnsAndActions() {
        Game game = battle();
        Player first = game.getCurrentPlayer();
        Player second = game.getOpponent();
        check(game.isPreparation(), "first preparation turn");
        game.act(Player.Action.attack());
        check(second.getHealth() == 40 && !first.hasUsedBasicAttack(), "preparation rejects basic attack");
        Item fire = add(first, "Pochodeň");
        game.act(Player.Action.use(fire));
        check(first.getStamina() == 12 && second.getEffects().isEmpty(), "preparation rejects hostile item without payment");
        Item potion = add(first, "Doušek síly");
        game.act(Player.Action.use(potion));
        check(first.getStamina() == 9 && first.getNextStamina() == 3 && !first.getInventory().contains(potion), "delayed stamina");
        game.endTurn();
        check(game.isPreparation() && game.getCurrentPlayer() == second, "second preparation turn");
        game.endTurn();
        check(!game.isPreparation() && first.getStamina() == 15, "stamina exceeds cap for next turn");
        game.act(Player.Action.attack());
        game.act(Player.Action.attack());
        check(second.getHealth() == 37, "basic attack once per turn");
        game.act(Player.Action.use(fire));
        game.act(Player.Action.use(fire));
        check(second.getEffects().size() == 1 && first.getStamina() == 13, "weapon once per copy");
        check(first.getActions().size() == 2, "actions recorded once");
        Item fireCopy = add(first, "Pochodeň");
        game.act(Player.Action.use(fireCopy));
        check(second.getEffects().get(0).getAmount() == 14, "distinct weapons can both be used");
        Item hammer = add(first, "Těžké kladivo");
        game.act(Player.Action.use(hammer));
        Item sword = add(first, "Obouruční meč");
        int before = second.getHealth();
        game.act(Player.Action.use(sword));
        check(second.getHealth() == before && !first.hasUsed(sword), "insufficient stamina has no effect");
        game.endTurn();
        game.endTurn();
        check(first.getStamina() == 12 && first.getNextStamina() == 0 && first.getActions().isEmpty(), "bonus lasts one turn");
        game.act(Player.Action.use(fire));
        check(first.hasUsed(fire), "weapon reusable next turn");
    }

    private static void effectsAndOrdering() {
        Game healingFirst = activeBattle();
        Player target = healingFirst.getOpponent();
        target.takeDamage(39);
        target.addEffect(new Damage(Damage.DamageType.HEAL, 6, 1));
        target.addEffect(new Damage(Damage.DamageType.POISON, 5, 1));
        healingFirst.endTurn();
        check(target.getHealth() == 2 && healingFirst.getPhase() == Game.Phase.COMBAT, "earlier heal saves life");

        Game poisonFirst = activeBattle();
        target = poisonFirst.getOpponent();
        target.takeDamage(39);
        target.addEffect(new Damage(Damage.DamageType.POISON, 5, 1));
        target.addEffect(new Damage(Damage.DamageType.HEAL, 6, 1));
        poisonFirst.endTurn();
        check(target.getHealth() == 0 && poisonFirst.getPhase() == Game.Phase.RESULT, "death cancels later heal");
        int gold = target.getGold();
        poisonFirst.endTurn();
        check(target.getGold() == gold, "result cannot be awarded twice");

        Game game = activeBattle();
        Player actor = game.getCurrentPlayer();
        actor.takeDamage(20);
        Item tea = add(actor, "Bylinný čaj");
        game.act(Player.Action.use(tea));
        check(actor.getHealth() == 20 && actor.getEffects().size() == 1, "regeneration starts next turn");
        game.endTurn();
        game.endTurn();
        check(actor.getHealth() == 24, "first healing tick");
        game.endTurn();
        game.endTurn();
        check(actor.getHealth() == 28 && actor.getEffects().isEmpty(), "second healing tick");
        Item instant = add(actor, "Životabudič");
        game.act(Player.Action.use(instant));
        check(actor.getHealth() == 40, "instant heal capped");

        Player player = new Player("Effects");
        player.addEffect(new Damage(Damage.DamageType.POISON, 5, 3));
        player.addEffect(new Damage(Damage.DamageType.FIRE, 5, 3));
        player.addEffect(new Damage(Damage.DamageType.POISON, 5, 3));
        player.addEffect(new Damage(Damage.DamageType.FIRE, 10, 4));
        check(player.getEffects().size() == 3 && player.getEffects().get(1).getAmount() == 15,
                "poison independent fire merges in place");
    }

    private static void defenseAndPassives() {
        Player player = new Player("Armor");
        Item armor = add(player, "Plátová zbroj");
        player.getEquipment().equipArmor(armor);
        check(player.getMaxStamina() == 10, "heavy armor stamina cost");
        check(player.receiveDamage(9, Damage.DamageType.PHYSICAL) == 5, "nearest integer armor reduction");
        check(player.receiveDamage(5, Damage.DamageType.FIRE) == 5, "physical armor does not stop fire");
        player.addShield(5);
        check(player.receiveDamage(10, Damage.DamageType.PHYSICAL) == 1 && player.getShield() == 0, "armor then shared shield pool");
        Item threshold = add(player, "Zpevněná vesta");
        player.getEquipment().equipArmor(threshold);
        check(player.receiveDamage(5, Damage.DamageType.POISON) == 3, "threshold applies to all types");
        check(player.receiveDamage(6, Damage.DamageType.POISON) == 6, "threshold boundary");

        Game game = activeBattle();
        Player actor = game.getCurrentPlayer();
        Item shield = add(actor, "Dřevěný štít");
        game.act(Player.Action.use(shield));
        actor.addEffect(new Damage(Damage.DamageType.POISON, 3, 1));
        game.endTurn();
        game.endTurn();
        check(actor.getHealth() == 40 && actor.getShield() == 0, "shield protects opening effects then expires");
        Item cleanse = add(actor, "Hadí kámen");
        actor.getEquipment().equipTrinket(cleanse);
        actor.addEffect(new Damage(Damage.DamageType.POISON, 3, 1));
        game.endTurn();
        game.endTurn();
        check(actor.isAlive() && actor.getEffects().isEmpty(), "cleanse precedes damage");
        Item low = add(actor, "Srdce poutníka");
        actor.getEquipment().equipTrinket(low);
        actor.takeDamage(20);
        game.endTurn(); game.endTurn();
        check(actor.getHealth() == 20, "exact half does not trigger passive");
        actor.takeDamage(1);
        game.endTurn(); game.endTurn();
        check(actor.getHealth() == 22, "below half triggers passive");
        Item power = add(actor, "Válečníkův amulet");
        actor.getEquipment().equipTrinket(power);
        game.act(Player.Action.attack());
        check(game.getOpponent().getHealth() == 35, "offensive trinket");
    }

    private static void matchAndRotation() {
        Game game = activeBattle();
        int initialStarter = game.getShopStarter();
        int champion = game.getCurrentIndex();
        Item weapon = add(game.getCurrentPlayer(), "Dýka");
        for (int round = 0; round < 3; round++) {
            if (game.getCurrentIndex() != champion) game.endTurn();
            Player loser = game.getOpponent();
            loser.takeDamage(39);
            int winnerGold = game.getCurrentPlayer().getGold();
            int loserGold = loser.getGold();
            game.act(Player.Action.attack());
            check(game.getWins(champion) == round + 1, "score advances");
            check(game.getPlayer(champion).getGold() == winnerGold + 6 && loser.getGold() == loserGold + 9,
                    "catch up rewards");
            if (round < 2) {
                game.nextBattle();
                check(game.getPhase() == Game.Phase.ANNOUNCEMENT, "order announcement each battle");
                check(game.getShopStarter() == (round == 0 ? 1 - initialStarter : initialStarter), "rotation independent of winner");
                check(loser.getHealth() == 40 && loser.getEffects().isEmpty(), "health and effects reset");
                check(game.getPlayer(champion).getInventory().contains(weapon), "gear persists");
                game.openShop(); game.continueShop(); game.continueShop();
                game.endTurn(); game.endTurn();
            }
        }
        check(game.getPhase() == Game.Phase.FINISHED, "three wins ends match");
        game.nextBattle();
        check(game.getPhase() == Game.Phase.FINISHED, "finished match cannot continue");
    }

    private static Game battle() {
        Game game = new Game("A", "B", new Random(1));
        game.openShop(); game.continueShop(); game.continueShop();
        return game;
    }

    private static Game activeBattle() {
        Game game = battle();
        game.endTurn(); game.endTurn();
        return game;
    }

    private static Item item(String name) {
        return ItemCatalog.getItems().stream().filter(item -> item.getName().equals(name)).findFirst().orElseThrow();
    }

    private static Item add(Player player, String name) {
        Item item = item(name).copy();
        if (!player.addToInventory(item)) throw new AssertionError("fixture inventory full");
        return item;
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
