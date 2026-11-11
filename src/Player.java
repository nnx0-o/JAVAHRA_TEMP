import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Player {

    // ------------------------------------ akce hrace ------------------------------------

    public record Action(Item item, boolean basicAttack) {
        public static Action use(Item item) { return new Action(item, false); }
        public static Action attack() { return new Action(null, true); }
    }

    // ------------------------------------ inicializace ------------------------------------

    public static final int MAX_HEALTH = GameRules.MAX_HEALTH;
    private final String name;
    private final Inventory inventory = new Inventory();
    private int health = MAX_HEALTH;
    private int gold = GameRules.STARTING_GOLD;
    private int stamina;
    private int shield;
    private int nextStamina;
    private boolean basicAttackUsed;
    private final Set<Item> usedItems = new HashSet<>();
    private final List<Damage> effects = new ArrayList<>();
    private final List<Action> actions = new ArrayList<>();

    public Player(String name) {
        this.name = name == null || name.isBlank() ? "Hráč" : name.trim();
    }

    // ------------------------------------ stav ------------------------------------

    public String getName() { return name; }
    public int getGold() { return gold; }
    public int getHealth() { return health; }
    public int getStamina() { return stamina; }
    public int getShield() { return shield; }
    public int getNextStamina() { return nextStamina; }
    public boolean isAlive() { return health > 0; }
    public boolean hasUsed(Item item) { return usedItems.contains(item); }
    public boolean hasUsedBasicAttack() { return basicAttackUsed; }
    public List<Damage> getEffects() { return List.copyOf(effects); }
    public List<Action> getActions() { return List.copyOf(actions); }
    public Inventory getEquipment() { return inventory; }
    public List<Item> getInventory() { return inventory.getItems(); }
    public int getInventorySize() { return inventory.getCapacity(); }
    public boolean addToInventory(Item item) { return inventory.add(item); }
    public void removeFromInventory(Item item) { inventory.remove(item); }
    public boolean setInventorySize(int newSize) { return inventory.setCapacity(newSize); }
    public void resetInventory() { inventory.clear(); }

    public void changeGold(int amount) { gold = Math.max(0, gold + amount); }
    public void takeDamage(int amount) { health = Math.max(0, health - Math.max(0, amount)); }
    public void heal(int amount) { health = Math.min(MAX_HEALTH, health + Math.max(0, amount)); }

    public int getMaxStamina() {
        Item armor = inventory.getEquippedArmor();
        return Math.max(0, GameRules.MAX_STAMINA - (armor == null ? 0 : armor.getStaminaPenalty()));
    }

    // ------------------------------------ zacatek souboje a tahu ------------------------------------

    public void resetBattle() {
        health = MAX_HEALTH;
        stamina = getMaxStamina();
        shield = nextStamina = 0;
        effects.clear(); // zlato a predmety zustavaji ale bojove ucinky ne
        usedItems.clear();
        actions.clear();
        basicAttackUsed = false;
    }

    public void beginTurn() {
        stamina = getMaxStamina() + nextStamina; // bonus se neprirezava na bezne maximum
        nextStamina = 0;
        usedItems.clear();
        actions.clear();
        basicAttackUsed = false;
        // stit se smaze az po uvodnich ucincich aby pokryl i prichozi jed a ohen
    }

    public void clearShield() { shield = 0; }
    public void addShield(int amount) { shield += Math.max(0, amount); }
    public void addNextStamina(int amount) { nextStamina += Math.max(0, amount); }
    public void addStamina(int amount) { stamina += Math.max(0, amount); }

    public boolean spendStamina(int amount) {
        if (amount < 0 || amount > stamina) return false;
        stamina -= amount;
        return true;
    }

    public void recordAction(Action action) {
        actions.add(action); // historie tahu zustava viditelna ale akce se uz neopakuje
        if (action.basicAttack()) basicAttackUsed = true;
        else usedItems.add(action.item());
    }

    // ------------------------------------ stavy a obrana ------------------------------------

    public void addEffect(Damage effect) {
        if (effect.getType() == Damage.DamageType.FIRE) {
            for (Damage current : effects) {
                if (current.getType() == Damage.DamageType.FIRE) {
                    current.mergeFire(effect); // slouceny ohen si ponecha misto v casovem poradi
                    return;
                }
            }
        }
        effects.add(effect); // jedy a leceni maji samostatne casovace v poradi vzniku
    }

    public void removeEffect(Damage effect) { effects.remove(effect); }
    public int cleanse(Damage.DamageType type, int budget) {
        int remaining = Math.max(0, budget);
        for (Damage effect : getEffects()) {
            if (effect.getType() != type || remaining == 0) continue;
            remaining -= effect.reduceRemaining(remaining); // jeden rozpocet pro vsechny jedy dohromady
            if (effect.getAmount() == 0) effects.remove(effect);
        }
        return Math.max(0, budget) - remaining;
    }

    public int receiveDamage(int amount, Damage.DamageType type) {
        int reduced = Math.max(0, amount);
        Item armor = inventory.getEquippedArmor();
        if (armor != null) {
            if (armor.getPassive() == Item.Passive.SMALL_HIT_BLOCK && reduced <= armor.getThreshold()
                    || armor.getPassive() == Item.Passive.LARGE_HIT_BLOCK && reduced >= armor.getThreshold()) {
                // prahova zbroj nevymaze kazdou malou davku jedu uplne
                reduced = Math.max(reduced - armor.getPower(), (int) Math.round(reduced / 2.0));
            } else if (armor.getPassive() == Item.Passive.NONE && armor.getDamageType() == type) {
                reduced = (int) Math.round(reduced * (100 - armor.getPower()) / 100.0);
            }
        }
        int blocked = Math.min(shield, reduced); // procenta a prahy se vyhodnoti pred stitem
        shield -= blocked;
        reduced -= blocked;
        int previous = health;
        takeDamage(reduced);
        return previous - health;
    }

    @Override
    public String toString() {
        return name + ": " + health + "/" + MAX_HEALTH + " životů, " + stamina + "/"
                + getMaxStamina() + " výdrže, " + gold + " zlata, štít " + shield;
    }
}
