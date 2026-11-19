import java.util.ArrayList;
import java.util.List;

public class Player {

    // ------------------------------------ jmeno, inventar a stav hrace ------------------------------------

    public static final int MAX_HEALTH = GameRules.MAX_HEALTH;
    private final String name;
    private final Inventory inventory = new Inventory();
    private int health = MAX_HEALTH;
    private int gold = GameRules.STARTING_GOLD;
    private int stamina;
    private int shield;
    private int nextStamina;

    // ------------------------------------ plan tahu a casovane ucinky ------------------------------------

    private final List<Damage> effects = new ArrayList<>();
    private final List<Item> actions = new ArrayList<>();

    // ------------------------------------ vytvoreni hrace ------------------------------------

    public Player(String name) {
        this.name = name == null || name.isBlank() ? "Hráč" : name.trim();
    }

    // ------------------------------------ cteni jmena, zdroju a bojoveho stavu ------------------------------------

    public String getName() {
        return name;
    }

    public int getGold() {
        return gold;
    }

    public int getHealth() {
        return health;
    }

    public int getStamina() {
        return stamina;
    }

    public int getShield() {
        return shield;
    }

    public int getNextStamina() {
        return nextStamina;
    }

    public boolean isAlive() {
        return health > 0;
    }

    // Kopie chrani seznam pred pridavanim zvenku; objekty ucinku zustavaji sdilene.
    public List<Damage> getEffects() {
        // Kopie seznamu sdili objekty Damage: takeTick() tedy stale meni skutecny ucinek.
        return List.copyOf(effects);
    }

    public List<Item> getActions() {
        return List.copyOf(actions);
    }

    // ------------------------------------ pristup k inventari a zmena jeho obsahu ------------------------------------

    public Inventory getEquipment() {
        // Vraci cely spravce inventare, ktery si pamatuje take vybavenou zbroj a talisman.
        return inventory;
    }

    public List<Item> getInventory() {
        return inventory.getItems();
    }

    public boolean addToInventory(Item item) {
        return inventory.add(item);
    }

    public void removeFromInventory(Item item) {
        inventory.remove(item);
    }

    // ------------------------------------ zmena zlata a zdravi ------------------------------------

    public void changeGold(int amount) {
        gold = Math.max(0, gold + amount);
    }

    public void takeDamage(int amount) {
        health = Math.max(0, health - Math.max(0, amount));
    }

    public void heal(int amount) {
        health = Math.min(MAX_HEALTH, health + Math.max(0, amount));
    }

    // ------------------------------------ bezna maximalni vydrz ------------------------------------

    public int getMaxStamina() {
        return GameRules.MAX_STAMINA;
    }

    // ------------------------------------ obnoveni stavu na zacatku souboje a tahu ------------------------------------

    public void resetBattle() {
        health = MAX_HEALTH;
        stamina = getMaxStamina();
        shield = nextStamina = 0;
        effects.clear(); // zlato a predmety zustavaji ale bojove ucinky ne
        actions.clear();
    }

    public void beginTurn() {
        stamina = getMaxStamina() + nextStamina; // bonus se neprirezava na bezne maximum
        nextStamina = 0;
        actions.clear();
        // Stit se smaze az po uvodnich ucincich, aby pokryl i prichozi jed.
    }

    // ------------------------------------ zmena stitu a vydrze ------------------------------------

    public void clearShield() {
        shield = 0;
    }

    public void addShield(int amount) {
        shield += Math.max(0, amount);
    }

    public void addNextStamina(int amount) {
        nextStamina += Math.max(0, amount);
    }

    public void addStamina(int amount) {
        stamina += Math.max(0, amount);
    }

    // ------------------------------------ ulozeni planu a prepocet rezervovane vydrze ------------------------------------

    public boolean replacePlan(List<Item> plan, int previousCost, int nextCost) {
        // Vydrz uz nezahrnuje stary plan. Jeho cenu vratime do rozpoctu a odecteme novou.
        // Kdyz novy plan neprojde kontrolou, puvodni plan i vydrz zustanou beze zmeny.
        int budget = stamina + previousCost;
        if (nextCost < 0 || nextCost > budget) return false;
        stamina = budget - nextCost;
        actions.clear();
        actions.addAll(plan);
        // Pouzite predmety zjistujeme primo z planu; neudrzujeme druhou kopii stejneho stavu.
        return true;
    }

    // ------------------------------------ pridavani a ocista samostatnych casovanych ucinku ------------------------------------

    public void addEffect(Damage effect) {
        effects.add(effect); // Jedy a leceni maji samostatne casovace v poradi vzniku.
    }

    public void removeEffect(Damage effect) {
        effects.remove(effect);
    }

    public int cleanse(Damage.DamageType type, int budget) {
        int remaining = Math.max(0, budget);
        for (Damage effect : getEffects()) {
            if (effect.getType() != type || remaining == 0) continue;
            remaining -= effect.reduceRemaining(remaining); // jeden rozpocet pro vsechny jedy dohromady
            if (effect.getAmount() == 0) effects.remove(effect);
        }
        return Math.max(0, budget) - remaining;
    }

    // ------------------------------------ prichozi poskozeni: zbroj, stit a zdravi ------------------------------------

    public int receiveDamage(int amount, Damage.DamageType type) {
        int reduced = Math.max(0, amount);
        Item armor = inventory.getEquippedArmor();
        if (armor != null && armor.getDamageType() == type) {
            reduced = (int) Math.round(reduced * (100 - armor.getPower()) / 100.0);
        }
        int blocked = Math.min(shield, reduced); // Zbroj se vyhodnoti pred stitem.
        shield -= blocked;
        reduced -= blocked;
        int previous = health;
        takeDamage(reduced);
        return previous - health;
    }

}
