import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game {
    public enum Phase { ANNOUNCEMENT, SHOP, COMBAT, RESULT, FINISHED }

    // ------------------------------------ stav zapasu ------------------------------------

    private final Player[] players;
    private final int[] wins = new int[2];
    private final Shop shop = new Shop();
    private final Random random;
    private final List<String> log = new ArrayList<>();
    private GameText language = new GameText(false);
    private Phase phase = Phase.ANNOUNCEMENT;
    private int shopStarter;
    private int current;
    private int battle = 1;
    private int turn;
    private int unchangedVisits;
    private boolean traded;
    private int winner = -1;

    public Game(String firstName, String secondName, Random random) {
        this.random = random;
        players = new Player[]{new Player(firstName), new Player(secondName)};
        shopStarter = random.nextInt(2); // dalsi souboje poradi otaceji
        current = shopStarter;
    }

    public void setEnglish(boolean english) { language = new GameText(english); }
    public Player getPlayer(int index) { return players[index]; }
    public Player getCurrentPlayer() { return players[current]; }
    public Player getOpponent() { return players[1 - current]; }
    public int getCurrentIndex() { return current; }
    public int getShopStarter() { return shopStarter; }
    public int getCombatStarter() { return 1 - shopStarter; }
    public int getBattle() { return battle; }
    public int getTurn() { return turn; }
    public int getCombatRound() { return Math.max(0, turn - GameRules.PREPARATION_TURNS) / 2 + 1; }
    public int getTurnPosition() { return turn % 2 + 1; }
    public int getWins(int index) { return wins[index]; }
    public int getWinner() { return winner; }
    public Shop getShop() { return shop; }
    public Phase getPhase() { return phase; }
    public List<String> getLog() { return List.copyOf(log); }
    public boolean isPreparation() { return phase == Phase.COMBAT && turn < GameRules.PREPARATION_TURNS; }
    public int getUnchangedVisits() { return unchangedVisits; }
    public boolean willStartBattleOnContinue() { return phase == Phase.SHOP && !traded && unchangedVisits == 1; }

    private String text(String cs, String en) { return language.text(cs, en); }
    private void report(String message) { log.add(message); }

    // ------------------------------------ obchod ------------------------------------

    public void openShop() {
        if (phase != Phase.ANNOUNCEMENT) return;
        shop.refresh(random);
        current = shopStarter;
        unchangedVisits = 0;
        traded = false;
        phase = Phase.SHOP;
        report(text("Začíná obchod před soubojem ", "Shop opens before battle ") + battle + ".");
    }

    public String buy(Item item) {
        if (phase != Phase.SHOP) return text("Nakupovat lze pouze v obchodě.", "You can only buy in the shop.");
        if (!shop.purchase(getCurrentPlayer(), item)) return text("Nákup se nezdařil: chybí zlato, místo nebo zásoba.", "Purchase failed: insufficient gold, inventory space or stock.");
        traded = true;
        String message = getCurrentPlayer().getName() + text(" kupuje: ", " buys: ") + language.name(item) + ".";
        report(message);
        return message;
    }

    public String sell(Item item) {
        if (phase != Phase.SHOP) return text("Prodávat lze pouze v obchodě.", "You can only sell in the shop.");
        if (!shop.sell(getCurrentPlayer(), item)) return text("Prodej se nezdařil. Po odebrání brašny se musí vše vejít.", "Sale failed. Your items must fit after removing a bag.");
        traded = true;
        String message = getCurrentPlayer().getName() + text(" prodává: ", " sells: ") + language.name(item)
                + text(" za ", " for ") + item.getPrice() / 2 + text(" zlata.", " gold.");
        report(message);
        return message;
    }

    public String equip(Item item) {
        if (phase != Phase.SHOP || item == null) return text("Vybavení lze měnit pouze v obchodě.", "Equipment can only be changed in the shop.");
        Inventory inventory = getCurrentPlayer().getEquipment();
        Item next = inventory.isEquipped(item) ? null : item;
        boolean success = item.getItemType() == ItemType.ARMOR ? inventory.equipArmor(next)
                : item.getItemType() == ItemType.TRINKET && inventory.equipTrinket(next);
        if (!success) return text("Vybavení nelze změnit. Zkontroluj typ předmětu a kapacitu inventáře.", "Cannot change equipment. Check item type and inventory capacity.");
        return next == null ? text("Předmět byl sundán.", "Item unequipped.") : text("Vybaveno: ", "Equipped: ") + language.name(item) + ".";
    }

    public void continueShop() {
        if (phase != Phase.SHOP) return;
        unchangedVisits = traded ? 0 : unchangedVisits + 1;
        traded = false;
        if (unchangedVisits >= 2) startBattle();
        else current = 1 - current;
    }

    // ------------------------------------ tahy a pasivni ucinky ------------------------------------

    private void startBattle() {
        for (Player player : players) player.resetBattle();
        phase = Phase.COMBAT;
        turn = 0;
        current = getCombatStarter();
        report(text("Souboj začíná. Každý má jeden přípravný tah bez útoků.", "Battle begins. Each player has one preparation turn without attacks."));
        beginTurn();
    }

    private void beginTurn() {
        Player actor = getCurrentPlayer();
        actor.beginTurn();
        report((isPreparation() ? text("Příprava", "Preparation") : text("Kolo ", "Round ") + getCombatRound())
                + " (" + getTurnPosition() + "): " + actor.getName() + ".");
        Item trinket = actor.getEquipment().getEquippedTrinket();
        if (trinket != null) {
            int before = actor.getHealth();
            int cleansed = 0;
            switch (trinket.getPassive()) {
                case REGENERATION -> actor.heal(trinket.getPower());
                case LOW_HEALTH_HEAL -> {
                    if (actor.getHealth() * 2 < Player.MAX_HEALTH) actor.heal(trinket.getPower());
                }
                case STAMINA_BONUS -> actor.addStamina(trinket.getPower());
                case CLEANSE_POISON -> cleansed = actor.cleanse(Damage.DamageType.POISON, trinket.getPower());
                case CLEANSE_FIRE -> cleansed = actor.cleanse(Damage.DamageType.FIRE, trinket.getPower());
                default -> { }
            }
            if (actor.getHealth() > before) report(language.name(trinket) + text(": obnoveno životů: ", ": health restored: ") + (actor.getHealth() - before) + ".");
            if (cleansed > 0) report(language.name(trinket) + text(": odstraněné zbývající poškození: ", ": remaining damage removed: ") + cleansed + ".");
        }
        for (Damage effect : actor.getEffects()) {
            int amount = effect.takeTick(); // snimek drzi poradi i pri odstranovani ucinku
            if (effect.getType() == Damage.DamageType.HEAL) {
                int before = actor.getHealth();
                actor.heal(amount);
                report(text("Postupné léčení: obnoveno životů: ", "Timed healing: health restored: ") + (actor.getHealth() - before) + ".");
            } else {
                int actual = actor.receiveDamage(amount, effect.getType());
                report(language.damageType(effect.getType()) + ": " + actual
                        + text(" poškození po obraně (před obranou ", " damage after defenses (before defenses ") + amount + ").");
            }
            if (effect.getTimer() == 0) actor.removeEffect(effect);
            if (finishIfDefeated()) return;
        }
        actor.clearShield(); // predchozi stit kryje i uvodni davky
    }

    public void endTurn() {
        if (phase != Phase.COMBAT) return;
        Player actor = getCurrentPlayer();
        // rezervace uz byla zaplacena pri planovani a ted se akce provedou presne jednou
        for (Player.Action action : actor.getActions()) {
            resolveAction(action);
            if (finishIfDefeated()) return;
        }
        turn++;
        current = 1 - current;
        beginTurn();
    }

    // ------------------------------------ plan a rezervace vydrze ------------------------------------

    public String act(Player.Action action) {
        if (phase != Phase.COMBAT) return text("Souboj nyní neprobíhá.", "No battle is in progress.");
        if (action == null) return text("Vyber akci.", "Choose an action.");
        Player actor = getCurrentPlayer();
        Item item = action.item();
        boolean attack = action.basicAttack() || item != null && item.getItemType() == ItemType.DAMAGE;
        if (attack && isPreparation()) return text("Přípravný tah: zatím nelze zasáhnout soupeře.", "Preparation: you cannot attack the opponent yet.");
        if (!action.basicAttack()) {
            if (item == null || !actor.getInventory().contains(item)) return text("Tento předmět nemáš v inventáři.", "This item is not in your inventory.");
            if (item.isEquipment()) return text("Zbroj a talisman působí pasivně; mění se v obchodě.", "Armor and trinkets are passive; change them in the shop.");
            if (item.isConsumable() && actor.getActions().stream().anyMatch(a -> a.item() == item)) {
                return text("Tento kus je již v plánu.", "This copy is already in your plan.");
            }
        }
        List<Player.Action> next = new ArrayList<>(actor.getActions());
        next.add(action);
        if (!replacePlan(next)) return text("Nemáš dost výdrže.", "Not enough stamina.");
        return text("Přidáno do plánu: ", "Added to plan: ") + actionName(action)
                + text(". Provede se při ukončení tahu.", ". Resolves when you end your turn.");
    }

    public int getActionCost(Player.Action action) {
        return actionCost(action, getCurrentPlayer().getActions());
    }

    public int getPlannedActionCost(int index) {
        List<Player.Action> plan = getCurrentPlayer().getActions();
        if (index < 0 || index >= plan.size()) return 0;
        return actionCost(plan.get(index), plan.subList(0, index));
    }

    private int actionCost(Player.Action action, List<Player.Action> earlier) {
        if (action == null || !action.basicAttack() && action.item() == null) return 0;
        int repeats = 0;
        for (Player.Action previous : earlier) {
            if (action.basicAttack() ? previous.basicAttack() : !previous.basicAttack() && previous.item() == action.item()) repeats++;
        }
        // jedine misto pro politiku opakovanych pouziti
        int base = action.basicAttack() ? GameRules.BASIC_STAMINA_COST : action.item().getStaminaCost();
        int increase = action.basicAttack() ? GameRules.BASIC_REPEAT_COST_INCREASE : action.item().getRepeatCostIncrease();
        return base + repeats * increase;
    }

    private int planCost(List<Player.Action> plan) {
        int cost = 0;
        for (int i = 0; i < plan.size(); i++) cost += actionCost(plan.get(i), plan.subList(0, i));
        return cost;
    }

    private boolean replacePlan(List<Player.Action> next) {
        Player actor = getCurrentPlayer();
        return actor.replacePlan(next, planCost(actor.getActions()), planCost(next));
    }

    public boolean removePlannedAction(int index) {
        if (phase != Phase.COMBAT) return false;
        List<Player.Action> next = new ArrayList<>(getCurrentPlayer().getActions());
        if (index < 0 || index >= next.size()) return false;
        next.remove(index);
        return replacePlan(next);
    }

    public boolean movePlannedAction(int index, int direction) {
        if (phase != Phase.COMBAT || direction != -1 && direction != 1) return false;
        List<Player.Action> next = new ArrayList<>(getCurrentPlayer().getActions());
        int destination = index + direction;
        if (index < 0 || index >= next.size() || destination < 0 || destination >= next.size()) return false;
        Player.Action action = next.remove(index);
        next.add(destination, action);
        return replacePlan(next);
    }

    private String actionName(Player.Action action) { return language.name(action.basicAttack() ? null : action.item()); }

    private void resolveAction(Player.Action action) {
        Player actor = getCurrentPlayer();
        Player target = getOpponent();
        Item item = action.item();
        if (action.basicAttack()) {
            int actual = target.receiveDamage(GameRules.BASIC_DAMAGE + attackBonus(actor), Damage.DamageType.PHYSICAL);
            report(actor.getName() + " — " + language.name(null) + ": " + actual + text(" poškození po obraně.", " damage after defenses."));
            return;
        }
        switch (item.getItemType()) {
            case DAMAGE -> {
                if (item.getDamageType() == Damage.DamageType.PHYSICAL) {
                    int actual = target.receiveDamage(item.getPower() + attackBonus(actor), item.getDamageType());
                    report(language.name(item) + ": " + actual + text(" poškození po obraně.", " damage after defenses."));
                } else {
                    Damage effect = new Damage(item.getDamageType(), item.getTotalPower(), item.getDuration());
                    target.addEffect(effect);
                    report(language.name(item) + text(": přidán účinek — ", ": effect applied — ") + language.effect(effect) + ".");
                }
            }
            case HEAL -> {
                if (item.getDuration() == 1) {
                    int before = actor.getHealth();
                    actor.heal(item.getPower());
                    report(language.name(item) + text(": obnoveno životů: ", ": health restored: ") + (actor.getHealth() - before) + ".");
                } else {
                    actor.addEffect(new Damage(Damage.DamageType.HEAL, item.getTotalPower(), item.getDuration()));
                    report(language.name(item) + text(": léčení začne příští vlastní tah.", ": healing starts on your next turn."));
                }
            }
            case DEFEND -> {
                actor.addShield(item.getPower());
                report(language.name(item) + text(": štít posílen o ", ": shield increased by ") + item.getPower() + ".");
            }
            case STAMINA -> {
                actor.addNextStamina(item.getPower());
                report(language.name(item) + text(": příští vlastní tah +", ": next own turn +") + item.getPower() + text(" výdrže.", " stamina."));
            }
            default -> throw new IllegalStateException("Neznámá aktivní akce.");
        }
        if (item.isConsumable()) actor.removeFromInventory(item);
    }

    private int attackBonus(Player player) {
        Item trinket = player.getEquipment().getEquippedTrinket();
        return trinket != null && trinket.getPassive() == Item.Passive.ATTACK_BONUS ? trinket.getPower() : 0;
    }

    // ------------------------------------ vysledek ------------------------------------

    private boolean finishIfDefeated() {
        if (players[0].isAlive() && players[1].isAlive()) return false;
        if (phase != Phase.COMBAT) return true;
        winner = players[0].isAlive() ? 0 : 1;
        wins[winner]++;
        players[winner].changeGold(GameRules.WIN_REWARD);
        players[1 - winner].changeGold(GameRules.LOSS_REWARD);
        phase = wins[winner] >= GameRules.WINS_REQUIRED ? Phase.FINISHED : Phase.RESULT;
        report(text("Vítězí ", "Winner: ") + players[winner].getName() + ". "
                + players[winner].getName() + ": +" + GameRules.WIN_REWARD + text(" zlata; ", " gold; ")
                + players[1 - winner].getName() + ": +" + GameRules.LOSS_REWARD + text(" zlata.", " gold."));
        return true;
    }

    public void nextBattle() {
        if (phase != Phase.RESULT) return;
        battle++;
        shopStarter = 1 - shopStarter;
        current = shopStarter;
        for (Player player : players) player.resetBattle();
        phase = Phase.ANNOUNCEMENT;
    }
}
