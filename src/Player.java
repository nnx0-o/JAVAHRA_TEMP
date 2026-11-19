import java.util.ArrayList;
import java.util.List;

public class Player {

    // jmeno inventar a stav hrace

    public static final int MAX_HEALTH = GameRules.MAX_HEALTH;
    private final String name;
    private final Inventory inventory = new Inventory();
    private int health = MAX_HEALTH;
    private int gold = GameRules.STARTING_GOLD;
    private int stamina;
    private int shield;
    private int nextStamina;

    // plan tahu a efekty s timerem

    private final List<Damage> effects = new ArrayList<>();
    private final List<Item> actions = new ArrayList<>();

    // vytvoreni hrace

    public Player(String name) {
        this.name = name == null || name.isBlank() ? "Hráč" : name.trim();
    }

    // cteni jmena zdroju a bojoveho stavu

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

    // kopie chrani seznam pred pridavanim zvenku efekty zustavaji sdilene
    public List<Damage> getEffects() {
        // kopie sdili instance efektu takze tick meni skutecny efekt
        return List.copyOf(effects);
    }

    public List<Item> getActions() {
        return List.copyOf(actions);
    }

    // pristup k inventari a zmena jeho obsahu

    public Inventory getEquipment() {
        // vrati manager inventare vcetne armoru a talismanu
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

    // zmena goldu a hp

    public void changeGold(int amount) {
        gold = Math.max(0, gold + amount);
    }

    public void takeDamage(int amount) {
        health = Math.max(0, health - Math.max(0, amount));
    }

    public void heal(int amount) {
        health = Math.min(MAX_HEALTH, health + Math.max(0, amount));
    }

    // bezna maximalni stamina

    public int getMaxStamina() {
        return GameRules.MAX_STAMINA;
    }

    // reset stavu na zacatku souboje a tahu

    public void resetBattle() {
        health = MAX_HEALTH;
        stamina = getMaxStamina();
        shield = nextStamina = 0;
        effects.clear(); // gold a itemy zustavaji ale bojove efekty ne
        actions.clear();
    }

    public void beginTurn() {
        stamina = getMaxStamina() + nextStamina; // bonus se neprirezava na bezne maximum
        nextStamina = 0;
        actions.clear();
        // stit smazeme az po startovnich efektech aby kryl i jed
    }

    // zmena stitu a staminy

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

    // ulozeni planu a prepocet rezervovane staminy

    public boolean replacePlan(List<Item> plan, int previousCost, int nextCost) {
        // stamina uz nema cenu stareho planu tak ji vratime a odecteme novou
        // kdyz novy plan neprojde puvodni plan i stamina zustavaji
        int budget = stamina + previousCost;
        if (nextCost < 0 || nextCost > budget) return false;
        stamina = budget - nextCost;
        actions.clear();
        actions.addAll(plan);
        // pouzite itemy cteme z planu bez druhe kopie stavu
        return true;
    }

    // pridavani a ocista samostatnych efektu s timerem

    public void addEffect(Damage effect) {
        effects.add(effect); // jedy a heal maji vlastni timery podle poradi vzniku
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

    // prichozi poskozeni armor stit a hp

    public int receiveDamage(int amount, Damage.DamageType type) {
        int reduced = Math.max(0, amount);
        Item armor = inventory.getEquippedArmor();
        if (armor != null && armor.getDamageType() == type) {
            reduced = (int) Math.round(reduced * (100 - armor.getPower()) / 100.0);
        }
        int blocked = Math.min(shield, reduced); // armor se pocita pred stitem
        shield -= blocked;
        reduced -= blocked;
        int previous = health;
        takeDamage(reduced);
        return previous - health;
    }

}
