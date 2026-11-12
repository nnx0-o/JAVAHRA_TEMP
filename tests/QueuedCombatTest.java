import java.util.List;
import java.util.Random;

public final class QueuedCombatTest {
    public static void main(String[] args) {
        queueAndReservation();
        repeatedCopiesAndBudget();
        consumeOnResolution();
        lethalStopsQueue();
        preparationAndRounds();
        continuePrediction();
        czechMessages();
        System.out.println("QueuedCombatTest passed");
    }

    private static Game game(boolean prepared) {
        Game game = new Game("Alice", "Bob", new Random(10));
        game.openShop();
        game.continueShop();
        game.continueShop();
        if (prepared) {
            game.endTurn();
            game.endTurn();
        }
        return game;
    }

    private static Item item(String name) {
        return ItemCatalog.getItems().stream().filter(i -> i.getName().equals(name)).findFirst().orElseThrow().copy();
    }

    private static void queueAndReservation() {
        Game game = game(true);
        Player actor = game.getCurrentPlayer();
        Player target = game.getOpponent();
        Item dagger = item("Dýka");
        actor.addToInventory(dagger);
        game.act(Player.Action.attack());
        game.act(Player.Action.use(dagger));
        game.act(Player.Action.attack());
        check(target.getHealth() == 40, "queued damage must not resolve immediately");
        check(actor.getStamina() == 5, "punch2 dagger2 punch3 reservation");
        check(game.getPlannedActionCost(2) == 3, "repeat price is shown per planned action");
        check(game.movePlannedAction(2, -1), "can move action up");
        check(actor.getStamina() == 5, "reordering preserves total reservation");
        check(game.removePlannedAction(0), "can remove first punch");
        check(actor.getStamina() == 8, "removal recalculates remaining repeat cost");
        check(game.getPlannedActionCost(0) == 2, "remaining punch returns to base price");
        List<Player.Action> before = actor.getActions();
        int stamina = actor.getStamina();
        check(!game.movePlannedAction(-1, 1) && !game.removePlannedAction(9), "invalid edits rejected");
        check(before.equals(actor.getActions()) && stamina == actor.getStamina(), "rejected edits are atomic");
        game.endTurn();
        check(target.getHealth() == 33, "planned punch and dagger resolve exactly once");
        game.endTurn();
        check(target.getHealth() == 33, "previous player's plan is never replayed");
    }

    private static void repeatedCopiesAndBudget() {
        Game game = game(true);
        Player actor = game.getCurrentPlayer();
        Item dagger = item("Dýka");
        actor.addToInventory(dagger);
        game.act(Player.Action.use(dagger));
        game.act(Player.Action.use(dagger));
        game.act(Player.Action.use(dagger));
        check(actor.getStamina() == 3, "same weapon costs two then three then four");
        List<Player.Action> before = actor.getActions();
        game.act(Player.Action.use(dagger));
        check(actor.getActions().equals(before) && actor.getStamina() == 3, "unaffordable append changes nothing");
        check(game.removePlannedAction(0) && actor.getStamina() == 7, "removal refunds recalculated repeat cost");
        Item custom = new Item(ItemType.DAMAGE, "Custom", 1, 2, 4, 1, Damage.DamageType.PHYSICAL,
                Rarity.COMMON, 1, 1, Item.Passive.NONE, 0, 0, "", 4);
        check(custom.copy().getRepeatCostIncrease() == 4, "copy preserves per-item repeat increase");
    }

    private static void consumeOnResolution() {
        Game game = game(true);
        Player actor = game.getCurrentPlayer();
        Item potion = item("Malý léčivý lektvar");
        Item torch = item("Pochodeň");
        actor.addToInventory(potion);
        actor.addToInventory(torch);
        actor.takeDamage(10);
        game.act(Player.Action.use(potion));
        int stamina = actor.getStamina();
        game.act(Player.Action.use(potion));
        check(actor.getActions().size() == 1 && actor.getStamina() == stamina, "cannot reserve the same potion twice");
        check(actor.getInventory().contains(potion) && actor.getHealth() == 30, "queued potion is neither consumed nor applied");
        game.act(Player.Action.use(torch));
        check(game.getOpponent().getEffects().isEmpty(), "queued status does not apply yet");
        game.endTurn();
        check(!actor.getInventory().contains(potion) && actor.getHealth() == 35, "potion consumed when resolved");
        check(!game.getCurrentPlayer().getEffects().isEmpty(), "status is applied on resolution");
    }

    private static void lethalStopsQueue() {
        Game game = game(true);
        Player actor = game.getCurrentPlayer();
        Player target = game.getOpponent();
        Item potion = item("Léčivý lektvar");
        actor.addToInventory(potion);
        actor.takeDamage(10);
        target.takeDamage(39);
        game.act(Player.Action.attack());
        game.act(Player.Action.use(potion));
        check(game.getPhase() == Game.Phase.COMBAT, "queued lethal damage does not finish battle yet");
        game.endTurn();
        check(game.getPhase() == Game.Phase.RESULT, "lethal action ends battle");
        check(actor.getHealth() == 30 && actor.getInventory().contains(potion), "later potion never resolves after lethal action");
    }

    private static void preparationAndRounds() {
        Game game = game(false);
        check(game.isPreparation() && game.getTurnPosition() == 1, "first preparation position");
        int stamina = game.getCurrentPlayer().getStamina();
        game.act(Player.Action.attack());
        check(game.getCurrentPlayer().getActions().isEmpty() && game.getCurrentPlayer().getStamina() == stamina,
                "cannot queue attacks during preparation");
        game.endTurn();
        check(game.isPreparation() && game.getTurnPosition() == 2, "second preparation position");
        game.endTurn();
        check(!game.isPreparation() && game.getCombatRound() == 1 && game.getTurnPosition() == 1, "first combat round excludes preparation");
        game.endTurn();
        check(game.getCombatRound() == 1 && game.getTurnPosition() == 2, "both players share round number");
        game.endTurn();
        check(game.getCombatRound() == 2 && game.getTurnPosition() == 1, "round advances after both turns");
    }

    private static void continuePrediction() {
        Game game = new Game("Alice", "Bob", new Random(1));
        game.openShop();
        check(!game.willStartBattleOnContinue(), "first visit cannot start battle");
        game.continueShop();
        check(game.willStartBattleOnContinue(), "second unchanged visit starts battle");
        Item offered = game.getShop().getStock().keySet().iterator().next();
        game.getCurrentPlayer().changeGold(100);
        game.buy(offered);
        check(!game.willStartBattleOnContinue(), "trade changes exact continue destination");
        game.continueShop();
        check(!game.willStartBattleOnContinue(), "trade resets consecutive unchanged visits");
    }

    private static void czechMessages() {
        Game game = game(true);
        check(game.act(Player.Action.attack()).contains("Přidáno do plánu: Pěst"), "queue response is Czech");
        game.getOpponent().takeDamage(39);
        game.endTurn();
        String reward = game.getLog().get(game.getLog().size() - 1);
        check(reward.contains("Alice") && reward.contains("Bob") && reward.contains("zlata"), "rewards name both players");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
