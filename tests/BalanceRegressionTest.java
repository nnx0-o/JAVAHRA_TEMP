import java.util.Random;

public class BalanceRegressionTest {

    public static void main(String[] args) {
        Player player = new Player("Tester");
        Damage first = new Damage(Damage.DamageType.POISON, 2, 3);
        Damage heal = new Damage(Damage.DamageType.HEAL, 5, 2);
        Damage second = new Damage(Damage.DamageType.POISON, 7, 4);
        player.addEffect(first);
        player.addEffect(heal);
        player.addEffect(second);
        check(player.cleanse(Damage.DamageType.POISON, 3) == 3, "one shared cleansing budget");
        check(player.getEffects().size() == 2 && player.getEffects().get(0) == heal,
                "cleansing respects unrelated effects and chronological order");
        check(second.getAmount() == 6 && second.getTimer() == 4, "surviving poison keeps duration");
        check(second.takeTick() == 2, "remaining poison can still damage after cleansing");
        player.cleanse(Damage.DamageType.POISON, -10);
        check(second.getAmount() == 4, "negative budget cannot amplify an effect");

        Player armored = new Player("Armor");
        Item vest = item("Zpevněná vesta").copy();
        armored.addToInventory(vest);
        armored.getEquipment().equipArmor(vest);
        check(armored.receiveDamage(1, Damage.DamageType.POISON) == 1, "small tick not erased");
        check(armored.receiveDamage(2, Damage.DamageType.POISON) == 1, "threshold blocks half of small tick");
        check(armored.receiveDamage(5, Damage.DamageType.PHYSICAL) == 3, "threshold still blocks eligible physical hit");
        check(armored.receiveDamage(6, Damage.DamageType.PHYSICAL) == 6, "threshold boundary retained");
        armored.addShield(2);
        check(armored.receiveDamage(2, Damage.DamageType.POISON) == 0, "active shield can still fully block");

        Game queued = new Game("A", "B", new Random(2));
        queued.openShop();
        queued.continueShop(); queued.continueShop();
        queued.endTurn(); queued.endTurn();
        Player attacker = queued.getCurrentPlayer();
        Player defender = queued.getOpponent();
        Item defenderArmor = item("Kožená zbroj").copy();
        defender.addToInventory(defenderArmor);
        defender.getEquipment().equipArmor(defenderArmor);
        queued.act(Player.Action.attack());
        check(defender.getHealth() == 40, "planned damage does not resolve before end turn");
        queued.endTurn();
        check(defender.getHealth() == 38, "physical mitigation applies on resolution with nearest rounding");
        queued.endTurn();
        check(defender.getHealth() == 38 && queued.getCurrentPlayer() == attacker,
                "completed damage is not repeated on a later turn");
        defender.takeDamage(37);
        queued.act(Player.Action.attack());
        check(queued.getPhase() == Game.Phase.COMBAT, "planned lethal damage is deferred");
        queued.endTurn();
        check(queued.getPhase() == Game.Phase.RESULT, "deferred lethal action finishes combat");
        int wins = queued.getWins(queued.getWinner());
        queued.endTurn();
        check(queued.getWins(queued.getWinner()) == wins, "result cannot resolve the dead player's turn or award twice");

        Shop shop = new Shop();
        shop.refresh(new Random() {
            @Override public int nextInt(int bound) { return bound - 1; }
        });
        check(shop.getStock().size() == 10, "ten distinct offers retained");
        check(shop.getStock().keySet().stream().noneMatch(i -> i.getItemType() == ItemType.DAMAGE),
                "weapon offers are weighted but not reserved");
        System.out.println("BalanceRegressionTest: OK");
    }

    private static Item item(String name) {
        return ItemCatalog.getItems().stream().filter(i -> i.getName().equals(name)).findFirst().orElseThrow();
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
