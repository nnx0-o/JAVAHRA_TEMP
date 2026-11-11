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

        for (String[] build : new String[][]{{"Dýka", "Kožený štít", "Kožený štít"}, {"Kožená zbroj", "Prsten obnovy"}}) {
            Game game = new Game("A", "B", new Random(2));
            game.openShop();
            for (int index = 0; index < 2; index++) {
                Player actor = game.getPlayer(index);
                for (String name : build) {
                    Item piece = item(name).copy();
                    actor.addToInventory(piece);
                    if (piece.getItemType() == ItemType.ARMOR) actor.getEquipment().equipArmor(piece);
                    if (piece.getItemType() == ItemType.TRINKET) actor.getEquipment().equipTrinket(piece);
                }
            }
            game.continueShop(); game.continueShop();
            for (int turn = 0; turn < 100 && game.getPhase() == Game.Phase.COMBAT; turn++) BalanceSimulation.playTurn(game);
            check(game.getPhase() == Game.Phase.RESULT, "reproduced defensive loop must finish without exhaustion");
        }

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
