import java.util.Random;

public class TimedItemTest {

    public static void main(String[] args) {
        poisonRateAndDescription();
        fireMergeAfterFirstTick();
        healingRate();
        repeatCostResets();
        System.out.println("TimedItemTest: OK");
    }

    private static void poisonRateAndDescription() {
        Game game = activeGame();
        Player actor = game.getCurrentPlayer();
        Player target = game.getOpponent();
        Item poison = give(actor, "Morová hůl");
        check(poison.getPower() == 4 && poison.getDuration() == 3 && poison.getTotalPower() == 12, "poison rate and total");
        GameText text = new GameText();
        check(text.description(poison).startsWith("Poškození:\n  Jed: 4 × 3 (12)"), "explicit per-turn description");
        check(text.summary(poison).contains("4 × 3 (12)"), "compact rate description");
        check(text.effect(new Damage(Damage.DamageType.POISON, 12, 3)).equals("Jed: 4 × 3 (12)"), "same notation on statuses");
        check(text.summary(null).equals("3 fyzické"), "short physical notation");
        game.act(Player.Action.use(poison));
        check(target.getHealth() == 40 && target.getEffects().isEmpty(), "selection remains deferred");
        game.endTurn();
        check(target.getHealth() == 36 && target.getEffects().get(0).getAmount() == 8, "first four damage");
        game.endTurn(); game.endTurn();
        check(target.getHealth() == 32, "second four damage");
        game.endTurn(); game.endTurn();
        check(target.getHealth() == 28 && target.getEffects().isEmpty(), "third four damage and expiry");
    }

    private static void fireMergeAfterFirstTick() {
        Game game = activeGame();
        Player actor = game.getCurrentPlayer(); Player target = game.getOpponent();
        Item torch = give(actor, "Pochodeň"); Item rod = give(actor, "Žhavý prut");
        game.act(Player.Action.use(torch)); game.endTurn();
        check(target.getHealth() == 38, "torch deals two per turn");
        game.endTurn();
        game.act(Player.Action.use(rod)); game.endTurn();
        check(target.getHealth() == 33, "remaining four plus new twelve merge to sixteen, first tick five");
        check(target.getEffects().size() == 1 && target.getEffects().get(0).getAmount() == 11,
                "fire preserves remaining total after merge");
        Damage irregular = new Damage(Damage.DamageType.FIRE, 16, 3);
        String description = new GameText().effect(irregular);
        check(description.equals("Oheň: 5 → 6 → 5 (16)"),
                "irregular status does not promise a constant tick");
        game.endTurn(); game.endTurn();
        check(target.getHealth() == 27, "second merged tick six");
        game.endTurn(); game.endTurn();
        check(target.getHealth() == 22 && target.getEffects().isEmpty(), "all eighteen original damage conserved");
    }

    private static void healingRate() {
        Game game = activeGame(); Player actor = game.getCurrentPlayer();
        Item salve = give(actor, "Hojivá mast");
        check(salve.getPower() == 3 && salve.getTotalPower() == 9, "healing uses same rate convention");
        GameText text = new GameText();
        check(text.description(salve).startsWith("Léčení:\n  3 × 3 (9)"), "separate healing field");
        check(text.summary(salve).equals("Léčení 3 × 3 (9)"), "same healing row notation");
        check(text.effect(new Damage(Damage.DamageType.HEAL, 9, 3)).equals("Léčení: 3 × 3 (9)"), "same healing status notation");
        actor.takeDamage(30);
        game.act(Player.Action.use(salve));
        check(actor.getHealth() == 10, "healing still queued");
        game.endTurn(); game.endTurn();
        check(actor.getHealth() == 13, "first healing tick three");
        game.endTurn(); game.endTurn(); game.endTurn(); game.endTurn();
        check(actor.getHealth() == 19 && actor.getEffects().isEmpty(), "nine total healing across three own turns");
    }

    private static void repeatCostResets() {
        Game game = activeGame(); Item dagger = give(game.getCurrentPlayer(), "Dýka");
        game.act(Player.Action.use(dagger)); game.act(Player.Action.use(dagger));
        check(game.getActionCost(Player.Action.use(dagger)) == 4, "cost rises within turn");
        game.endTurn(); game.endTurn();
        check(game.getActionCost(Player.Action.use(dagger)) == 2, "cost resets next own turn");
    }

    private static Game activeGame() {
        Game game = new Game("A", "B", new Random(1));
        game.openShop(); game.continueShop(); game.continueShop(); game.endTurn(); game.endTurn();
        return game;
    }

    private static Item give(Player player, String name) {
        Item item = ItemCatalog.getItems().stream().filter(value -> value.getName().equals(name)).findFirst().orElseThrow().copy();
        check(player.addToInventory(item), "fixture has capacity"); return item;
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
