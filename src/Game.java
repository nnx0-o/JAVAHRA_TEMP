import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game {

    // ------------------------------------ faze zapasu ------------------------------------

    public enum Phase {
        ANNOUNCEMENT, SHOP, COMBAT, RESULT, FINISHED
    }

    // ------------------------------------ hraci, obchod, historie a stav zapasu ------------------------------------

    private final Player[] players;
    private final int[] wins = new int[2];
    private final Shop shop = new Shop();
    private final Random random;
    private final List<String> log = new ArrayList<>();
    private final GameText text = new GameText();
    private Phase phase = Phase.ANNOUNCEMENT;
    private int shopStarter;
    private int current;
    private int battle = 1;
    private int turn;
    private int unchangedVisits;
    private boolean traded;
    private int winner = -1;

    // ------------------------------------ vytvoreni zapasu a losovani poradi ------------------------------------

    public Game(String firstName, String secondName, Random random) {
        this.random = random;
        players = new Player[]{
            new Player(firstName), new Player(secondName)
        };
        shopStarter = random.nextInt(2); // dalsi souboje poradi otaceji
        current = shopStarter;
    }

    // ------------------------------------ cteni hracu, poradi, faze a vysledku ------------------------------------

    public Player getPlayer(int index) {
        return players[index];
    }

    public Player getCurrentPlayer() {
        return players[current];
    }

    public Player getOpponent() {
        return players[1 - current];
    }

    public int getCurrentIndex() {
        return current;
    }

    public int getShopStarter() {
        return shopStarter;
    }

    public int getCombatStarter() {
        return 1 - shopStarter;
    }

    public int getBattle() {
        return battle;
    }

    public int getTurn() {
        return turn;
    }

    public int getCombatRound() {
        return Math.max(0, turn - GameRules.PREPARATION_TURNS) / 2 + 1;
    }

    public int getTurnPosition() {
        return turn % 2 + 1;
    }

    public int getWins(int index) {
        return wins[index];
    }

    public int getWinner() {
        return winner;
    }

    public Shop getShop() {
        return shop;
    }

    public Phase getPhase() {
        return phase;
    }

    public List<String> getLog() {
        return List.copyOf(log);
    }

    public boolean isPreparation() {
        return phase == Phase.COMBAT && turn < GameRules.PREPARATION_TURNS;
    }

    public int getUnchangedVisits() {
        return unchangedVisits;
    }

    public boolean willStartBattleOnContinue() {
        return phase == Phase.SHOP && !traded && unchangedVisits == 1;
    }

    // ------------------------------------ zapis do historie ------------------------------------

    private void report(String message) {
        log.add(message);
    }

    // ------------------------------------ otevreni obchodu, nakup, prodej a vybaveni ------------------------------------

    public void openShop() {
        if (phase != Phase.ANNOUNCEMENT) return;
        shop.refresh(random);
        current = shopStarter;
        unchangedVisits = 0;
        traded = false;
        phase = Phase.SHOP;
        report("Začíná obchod před soubojem " + battle + ".");
    }

    public String buy(Item item) {
        if (phase != Phase.SHOP) return "Nakupovat lze pouze v obchodě.";
        if (!shop.purchase(getCurrentPlayer(), item)) return "Nákup se nezdařil: chybí zlato, místo nebo zásoba.";
        traded = true;
        String message = getCurrentPlayer().getName() + " kupuje: " + text.name(item) + ".";
        report(message);
        return message;
    }

    public String sell(Item item) {
        if (phase != Phase.SHOP) return "Prodávat lze pouze v obchodě.";
        if (!shop.sell(getCurrentPlayer(), item)) return "Prodej se nezdařil. Po odebrání brašny se musí vše vejít.";
        traded = true;
        String message = getCurrentPlayer().getName() + " prodává: " + text.name(item)
                + " za " + item.getPrice() / 2 + " zlata.";
        report(message);
        return message;
    }

    public String equip(Item item) {
        if (phase != Phase.SHOP || item == null) return "Vybavení lze měnit pouze v obchodě.";
        Inventory inventory = getCurrentPlayer().getEquipment();
        Item next = inventory.isEquipped(item) ? null : item;
        boolean success = item.getItemType() == ItemType.ARMOR ? inventory.equipArmor(next)
                : item.getItemType() == ItemType.TRINKET && inventory.equipTrinket(next);
        if (!success) return "Vybavení nelze změnit. Zkontroluj typ předmětu a kapacitu inventáře.";
        return next == null ? "Předmět byl sundán." : "Vybaveno: " + text.name(item) + ".";
    }

    // ------------------------------------ predani obchodu a rozhodnuti o zacatku boje ------------------------------------

    public void continueShop() {
        if (phase != Phase.SHOP) return;
        unchangedVisits = traded ? 0 : unchangedVisits + 1;
        traded = false;
        if (unchangedVisits >= 2) startBattle();
        else current = 1 - current;
    }

    // ------------------------------------ zahajeni souboje ------------------------------------

    private void startBattle() {
        for (Player player : players) player.resetBattle();
        phase = Phase.COMBAT;
        turn = 0;
        current = getCombatStarter();
        report("Souboj začíná. Každý má jeden přípravný tah bez útoků.");
        beginTurn();
    }

    // ------------------------------------ zacatek tahu: vydrz, talisman a casovane ucinky ------------------------------------

    private void beginTurn() {
        Player actor = getCurrentPlayer();
        actor.beginTurn();
        report((isPreparation() ? "Příprava" : "Kolo " + getCombatRound())
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
            if (actor.getHealth() > before) report(text.name(trinket) + ": obnoveno životů: " + (actor.getHealth() - before) + ".");
            if (cleansed > 0) report(text.name(trinket) + ": odstraněné zbývající poškození: " + cleansed + ".");
        }
        for (Damage effect : actor.getEffects()) {
            int amount = effect.takeTick(); // snimek drzi poradi i pri odstranovani ucinku
            if (effect.getType() == Damage.DamageType.HEAL) {
                int before = actor.getHealth();
                actor.heal(amount);
                report("Postupné léčení: obnoveno životů: " + (actor.getHealth() - before) + ".");
            } else {
                int actual = actor.receiveDamage(amount, effect.getType());
                report(text.damageType(effect.getType()) + ": " + actual
                        + " poškození po obraně (před obranou " + amount + ").");
            }
            if (effect.getTimer() == 0) actor.removeEffect(effect);
            if (finishIfDefeated()) return;
        }
        actor.clearShield(); // predchozi stit kryje i uvodni davky
    }

    // ------------------------------------ provedeni planu a predani tahu ------------------------------------

    public void endTurn() {
        if (phase != Phase.COMBAT) return;
        Player actor = getCurrentPlayer();
        // rezervace uz byla zaplacena pri planovani a ted se akce provedou presne jednou
        for (Player.Action action : actor.getActions()) {
            resolveAction(action);
            if (finishIfDefeated()) return;
        }
        turn++;
        // Indexy hracu jsou 0 a 1; tento vyraz je vzajemne prohodi.
        current = 1 - current;
        beginTurn();
    }

    // ------------------------------------ kontrola a pridani akce do planu ------------------------------------

    public String act(Player.Action action) {
        if (phase != Phase.COMBAT) return "Souboj nyní neprobíhá.";
        if (action == null) return "Vyber akci.";
        Player actor = getCurrentPlayer();
        Item item = action.item();
        boolean attack = action.basicAttack() || item != null && item.getItemType() == ItemType.DAMAGE;
        if (attack && isPreparation()) return "Přípravný tah: zatím nelze zasáhnout soupeře.";
        if (!action.basicAttack()) {
            if (item == null || !actor.getInventory().contains(item)) return "Tento předmět nemáš v inventáři.";
            if (item.isEquipment()) return "Zbroj a talisman působí pasivně; mění se v obchodě.";
            if (item.isConsumable() && actor.getActions().stream().anyMatch(a -> a.item() == item)) {
                return "Tento kus je již v plánu.";
            }
        }
        List<Player.Action> next = new ArrayList<>(actor.getActions());
        next.add(action);
        if (!replacePlan(next)) return "Nemáš dost výdrže.";
        return "Přidáno do plánu: " + actionName(action)
                + ". Provede se při ukončení tahu.";
    }

    // ------------------------------------ vypocet cen a rezervace vydrze ------------------------------------

    public int getActionCost(Player.Action action) {
        return actionCost(action, getCurrentPlayer().getActions());
    }

    public int getPlannedActionCost(int index) {
        List<Player.Action> plan = getCurrentPlayer().getActions();
        if (index < 0 || index >= plan.size()) return 0;
        return actionCost(plan.get(index), plan.subList(0, index));
    }

    // Porovnavame identitu kusu (==): dve stejne pojmenovane zbrane jsou ruzne kusy.
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

    // ------------------------------------ odebirani a presouvani naplanovanych akci ------------------------------------

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

    // ------------------------------------ nazev a provedeni jednotlive akce ------------------------------------

    private String actionName(Player.Action action) {
        return text.name(action.basicAttack() ? null : action.item());
    }

    private void resolveAction(Player.Action action) {
        Player actor = getCurrentPlayer();
        Player target = getOpponent();
        Item item = action.item();
        if (action.basicAttack()) {
            int actual = target.receiveDamage(GameRules.BASIC_DAMAGE + attackBonus(actor), Damage.DamageType.PHYSICAL);
            report(actor.getName() + " — " + text.name(null) + ": " + actual + " poškození po obraně.");
            return;
        }
        switch (item.getItemType()) {
            case DAMAGE -> {
                if (item.getDamageType() == Damage.DamageType.PHYSICAL) {
                    int actual = target.receiveDamage(item.getPower() + attackBonus(actor), item.getDamageType());
                    report(text.name(item) + ": " + actual + " poškození po obraně.");
                } else {
                    Damage effect = new Damage(item.getDamageType(), item.getTotalPower(), item.getDuration());
                    target.addEffect(effect);
                    report(text.name(item) + ": přidán účinek — " + text.effect(effect) + ".");
                }
            }
            case HEAL -> {
                if (item.getDuration() == 1) {
                    int before = actor.getHealth();
                    actor.heal(item.getPower());
                    report(text.name(item) + ": obnoveno životů: " + (actor.getHealth() - before) + ".");
                } else {
                    actor.addEffect(new Damage(Damage.DamageType.HEAL, item.getTotalPower(), item.getDuration()));
                    report(text.name(item) + ": léčení začne příští vlastní tah.");
                }
            }
            case DEFEND -> {
                actor.addShield(item.getPower());
                report(text.name(item) + ": štít posílen o " + item.getPower() + ".");
            }
            case STAMINA -> {
                actor.addNextStamina(item.getPower());
                report(text.name(item) + ": příští vlastní tah +" + item.getPower() + " výdrže.");
            }
            default -> throw new IllegalStateException("Neznámá aktivní akce.");
        }
        if (item.isConsumable()) actor.removeFromInventory(item);
    }

    // ------------------------------------ utocny bonus vybaveneho talismanu ------------------------------------

    private int attackBonus(Player player) {
        Item trinket = player.getEquipment().getEquippedTrinket();
        return trinket != null && trinket.getPassive() == Item.Passive.ATTACK_BONUS ? trinket.getPower() : 0;
    }

    // ------------------------------------ ukonceni souboje, odmeny a dalsi souboj ------------------------------------

    private boolean finishIfDefeated() {
        if (players[0].isAlive() && players[1].isAlive()) return false;
        if (phase != Phase.COMBAT) return true;
        winner = players[0].isAlive() ? 0 : 1;
        wins[winner]++;
        players[winner].changeGold(GameRules.WIN_REWARD);
        players[1 - winner].changeGold(GameRules.LOSS_REWARD);
        phase = wins[winner] >= GameRules.WINS_REQUIRED ? Phase.FINISHED : Phase.RESULT;
        report("Vítězí " + players[winner].getName() + ". "
                + players[winner].getName() + ": +" + GameRules.WIN_REWARD + " zlata; "
                + players[1 - winner].getName() + ": +" + GameRules.LOSS_REWARD + " zlata.");
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
