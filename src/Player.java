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

    // jak je na tom hrac

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

    // seznam ven davame jako kopii efekty jsou porad ty same
    public List<Damage> getEffects() {
        // tick sahne na skutecny efekt i pres tuhle kopii
        return List.copyOf(effects);
    }

    public List<Item> getActions() {
        return List.copyOf(actions);
    }

    // veci hrace

    public Inventory getEquipment() {
        // je to cely inventar i s tim co ma na sobe
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

    // stamina bez bonusu na dalsi tah

    public int getMaxStamina() {
        return GameRules.MAX_STAMINA;
    }

    // novy souboj nebo tah

    public void resetBattle() {
        health = MAX_HEALTH;
        stamina = getMaxStamina();
        shield = nextStamina = 0;
        effects.clear(); // penize a veci si necha efekty jdou pryc
        actions.clear();
    }

    public void beginTurn() {
        stamina = getMaxStamina() + nextStamina; // bonus muze jit nad normalni maximum
        nextStamina = 0;
        actions.clear();
        // stit jeste nechame na uvodni efekty
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

    // vymenime plan a srovname staminu

    public boolean replacePlan(List<Item> plan, int previousCost, int nextCost) {
        // nejdriv vratit starou cenu pak odecist novou
        // kdyz to nevyjde nechame puvodni plan
        int budget = stamina + previousCost;
        if (nextCost < 0 || nextCost > budget) return false;
        stamina = budget - nextCost;
        actions.clear();
        actions.addAll(plan);
        // co se pouzilo uz vime z planu
        return true;
    }

    // pridani efektu a ocista

    public void addEffect(Damage effect) {
        effects.add(effect); // kazdy efekt si odpocitava svoje tahy
    }

    public void removeEffect(Damage effect) {
        effects.remove(effect);
    }

    public int cleanse(Damage.DamageType type, int budget) {
        int remaining = Math.max(0, budget);
        for (Damage effect : getEffects()) {
            if (effect.getType() != type || remaining == 0) continue;
            remaining -= effect.reduceRemaining(remaining); // tenhle limit je pro vsechny jedy dohromady
            if (effect.getAmount() == 0) effects.remove(effect);
        }
        return Math.max(0, budget) - remaining;
    }

    // nejdriv armor pak stit pak hp

    public int receiveDamage(int amount, Damage.DamageType type) {
        int reduced = Math.max(0, amount);
        Item armor = inventory.getEquippedArmor();
        if (armor != null && armor.getDamageType() == type) {
            reduced = (int) Math.round(reduced * (100 - armor.getPower()) / 100.0);
        }
        int blocked = Math.min(shield, reduced); // armor ubere jako prvni
        shield -= blocked;
        reduced -= blocked;
        int previous = health;
        takeDamage(reduced);
        return previous - health;
    }

}
