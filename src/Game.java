import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game {

    // game resi poradi a pravidla zapasu bez ui
    // main vola akce game updatuje hrace inventar a shop a vraci zpravu

    // faze zapasu

    public enum Phase {
        ANNOUNCEMENT, SHOP, COMBAT, RESULT, FINISHED
    }

    // hraci obchod historie a stav zapasu

    private final Player[] players;
    private final int[] wins = new int[2];
    private final Shop shop = new Shop();
    private final Random random;
    private final List<String> log = new ArrayList<>();
    private final GameText text = new GameText();
    private Phase phase = Phase.ANNOUNCEMENT;
    private int shopStarter;
    private int current; // index aktivniho hrace 0 nebo 1
    private int battle = 1;
    private int turn; // hotove tahy obou hracu vcetne pripravy
    private int unchangedVisits; // navstevy shopu za sebou bez nakupu nebo prodeje
    private boolean traded; // jestli behem tehle navstevy probehl trade
    private int winner = -1; // minus jedna znamena ze souboj jeste nema viteze

    // vytvoreni zapasu a losovani poradi

    public Game(String firstName, String secondName, Random random) {
        this.random = random;
        players = new Player[]{
            new Player(firstName), new Player(secondName)
        };
        shopStarter = random.nextInt(2); // dalsi souboje poradi otaceji
        current = shopStarter;
    }

    // cteni hracu poradi faze a vysledku

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

    public boolean willStartBattleOnContinue() {
        return phase == Phase.SHOP && !traded && unchangedVisits == 1;
    }

    // zapis do historie

    private void report(String message) {
        log.add(message);
    }

    // otevreni obchodu nakup prodej a vybaveni

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
        String message = getCurrentPlayer().getName() + " kupuje: " + item.getName() + ".";
        report(message);
        return message;
    }

    public String sell(Item item) {
        if (phase != Phase.SHOP) return "Prodávat lze pouze v obchodě.";
        if (item == ItemCatalog.FIST) return "Pěst je trvalou součástí inventáře a nelze prodat.";
        if (!shop.sell(getCurrentPlayer(), item)) return "Prodej se nezdařil. Předmět musí být v inventáři.";
        traded = true;
        String message = getCurrentPlayer().getName() + " prodává: " + item.getName()
                + " za " + item.getPrice() / 2 + " zlata.";
        report(message);
        return message;
    }

    public String equip(Item item) {
        if (phase != Phase.SHOP || item == null) return "Vybavení lze měnit pouze v obchodě.";
        Inventory inventory = getCurrentPlayer().getEquipment();
        // druhy klik vybaveny item sunda null je prazdny slot
        Item next = inventory.isEquipped(item) ? null : item;
        boolean success = item.getItemType() == ItemType.ARMOR ? inventory.equipArmor(next)
                : item.getItemType() == ItemType.TRINKET && inventory.equipTrinket(next);
        if (!success) return "Vybavení nelze změnit. Zkontroluj typ a vlastnictví předmětu.";
        return next == null ? "Předmět byl sundán." : "Vybaveno: " + item.getName() + ".";
    }

    // predani obchodu a rozhodnuti o zacatku boje

    public void continueShop() {
        if (phase != Phase.SHOP) return;
        // trade resetne prazdne navstevy samotne prepnuti vybavy ne
        unchangedVisits = traded ? 0 : unchangedVisits + 1;
        traded = false;
        if (unchangedVisits >= 2) startBattle();
        else current = 1 - current;
    }

    // zahajeni souboje

    private void startBattle() {
        for (Player player : players) player.resetBattle();
        phase = Phase.COMBAT;
        turn = 0;
        current = getCombatStarter();
        report("Souboj začíná. Každý má jeden přípravný tah bez útoků.");
        beginTurn();
    }

    // zacatek tahu stamina talisman a efekty s timerem

    private void beginTurn() {
        // stamina a prazdny plan noveho tahu

        Player actor = getCurrentPlayer();
        actor.beginTurn();
        report((isPreparation() ? "Příprava" : "Kolo " + getCombatRound())
                + " (" + getTurnPosition() + "): " + actor.getName() + ".");
        // pasivni talisman pred casovanymi efekty

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
                default -> { }
            }
            if (actor.getHealth() > before) report(trinket.getName() + ": obnoveno životů: " + (actor.getHealth() - before) + ".");
            if (cleansed > 0) report(trinket.getName() + ": odstraněné zbývající poškození: " + cleansed + ".");
        }
        // jednotlive efekty v poradi vzniku

        // kopie seznamu dovoli mazat hotove efekty behem loopu
        for (Damage effect : actor.getEffects()) {
            int amount = effect.takeTick(); // kopie drzi poradi i pri mazani efektu
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
        // konec ochrany stitu z predchoziho tahu

        actor.clearShield(); // predchozi stit kryje i uvodni davky
    }

    // provedeni planu a predani tahu

    public void endTurn() {
        if (phase != Phase.COMBAT) return;
        Player actor = getCurrentPlayer();
        // rezervace uz byla zaplacena pri planovani a ted se akce provedou presne jednou
        for (Item item : actor.getActions()) {
            resolveAction(item);
            if (finishIfDefeated()) return;
        }
        turn++;
        // prohodi indexy hracu 0 a 1
        current = 1 - current;
        beginTurn();
    }

    // kontrola a pridani akce do planu

    public String act(Item item) {
        // nejdriv check cele akce pri odmitnuti zustava plan i stamina
        if (phase != Phase.COMBAT) return "Souboj nyní neprobíhá.";
        if (item == null) return "Vyber předmět.";
        Player actor = getCurrentPlayer();
        if (!actor.getInventory().contains(item)) return "Tento předmět nemáš v inventáři.";
        if (item.getItemType() == ItemType.DAMAGE && isPreparation()) return "Přípravný tah: zatím nelze zasáhnout soupeře.";
        if (item.isEquipment()) return "Zbroj a talisman působí pasivně; mění se v obchodě.";
        if (item.isConsumable() && actor.getActions().contains(item)) return "Tento kus je již v plánu.";

        // na kopii zkontrolujeme cenu nez prepiseme plan
        List<Item> next = new ArrayList<>(actor.getActions());
        next.add(item);
        if (!replacePlan(next)) return "Nemáš dost výdrže.";
        return "Přidáno do plánu: " + item.getName()
                + ". Provede se při ukončení tahu.";
    }

    // vypocet cen a rezervace staminy

    public int getActionCost(Item item) {
        // cena dalsiho pridani pocita vsechna dosavadni pouziti v planu
        return actionCost(item, getCurrentPlayer().getActions());
    }

    public int getPlannedActionCost(int index) {
        List<Item> plan = getCurrentPlayer().getActions();
        if (index < 0 || index >= plan.size()) return 0;
        // cena ulozene akce pocita jen akce pred ni
        return actionCost(plan.get(index), plan.subList(0, index));
    }

    // porovnavame konkretni instance dve stejne zbrane muzou byt ruzne kusy
    private int actionCost(Item item, List<Item> earlier) {
        int repeats = 0;
        for (Item previous : earlier) {
            if (previous == item) repeats++;
        }
        return item.getStaminaCost() + repeats * item.getRepeatCostIncrease();
    }

    private int planCost(List<Item> plan) {
        int cost = 0;
        for (int i = 0; i < plan.size(); i++) cost += actionCost(plan.get(i), plan.subList(0, i));
        return cost;
    }

    private boolean replacePlan(List<Item> next) {
        Player actor = getCurrentPlayer();
        // vratime cenu stareho planu a odecteme novou akce jeste nebezely
        return actor.replacePlan(next, planCost(actor.getActions()), planCost(next));
    }

    // odebrani naplanovane akce

    public boolean removePlannedAction(int index) {
        if (phase != Phase.COMBAT) return false;
        List<Item> next = new ArrayList<>(getCurrentPlayer().getActions());
        if (index < 0 || index >= next.size()) return false;
        next.remove(index);
        return replacePlan(next);
    }

    // provedeni jednoho itemu z planu

    private void resolveAction(Item item) {
        Player actor = getCurrentPlayer();
        Player target = getOpponent();
        switch (item.getItemType()) {
            case DAMAGE -> {
                if (item.getDamageType() == Damage.DamageType.PHYSICAL) {
                    int actual = target.receiveDamage(item.getPower() + attackBonus(actor), item.getDamageType());
                    report(item.getName() + ": " + actual + " poškození po obraně.");
                } else {
                    Damage effect = new Damage(item.getDamageType(), item.getTotalPower(), item.getDuration());
                    target.addEffect(effect);
                    report(item.getName() + ": přidán účinek — " + text.effect(effect) + ".");
                }
            }
            case HEAL -> {
                if (item.getDuration() == 1) {
                    int before = actor.getHealth();
                    actor.heal(item.getPower());
                    report(item.getName() + ": obnoveno životů: " + (actor.getHealth() - before) + ".");
                } else {
                    actor.addEffect(new Damage(Damage.DamageType.HEAL, item.getTotalPower(), item.getDuration()));
                    report(item.getName() + ": léčení začne příští vlastní tah.");
                }
            }
            case DEFEND -> {
                actor.addShield(item.getPower());
                report(item.getName() + ": štít posílen o " + item.getPower() + ".");
            }
            case STAMINA -> {
                actor.addNextStamina(item.getPower());
                report(item.getName() + ": příští vlastní tah +" + item.getPower() + " výdrže.");
            }
            default -> throw new IllegalStateException("Neznámá aktivní akce.");
        }
        if (item.isConsumable()) actor.removeFromInventory(item);
    }

    // utocny bonus vybaveneho talismanu

    private int attackBonus(Player player) {
        Item trinket = player.getEquipment().getEquippedTrinket();
        return trinket != null && trinket.getPassive() == Item.Passive.ATTACK_BONUS ? trinket.getPower() : 0;
    }

    // ukonceni souboje odmeny a dalsi souboj

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
