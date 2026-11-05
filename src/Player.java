public class Player {
    public static final int MAX_HEALTH = 4;
    private final String name;
    private int health;
    private int gold;
    private final ItemCollection<Item> inventory;

    public Player(String name) {
        this.name = name;
        this.health = MAX_HEALTH;
        this.gold = 10;
        this.inventory = new ItemCollection<>(5);
    }

    public boolean addToInventory(Item item) {		// pridani bez platby
        return inventory.addItem(item);
    }

    public boolean buy(Item item, int price) {
        if (price < 0 || gold < price) return false;
        if (!addToInventory(item)) return false;
        gold -= price;		// platba az po uspesnem pridani
        return true;
    }

    public String getName() {
        return name;
    }

    public int getHealth() {
        return health;
    }

    public void takeDamage(int damage) {
        health = Math.max(0, health - Math.max(0, damage));
    }

    public void heal(int amount) {
        health = Math.min(MAX_HEALTH, health + Math.max(0, amount));
    }

    public boolean isAlive() { return health > 0; }

    public int getGold() {
        return gold;
    }

    public ItemCollection<Item> getInventory() {
        return inventory;
    }

    public void receiveGold(int amount) {
        if (amount > 0) gold += amount;
    }

    public boolean spendGold(int amount) {
        if (amount < 0 || gold < amount) return false;
        gold -= amount;
        return true;
    }

    @Override
    public String toString() {
        return "Hrac [name=" + name + ", zivoty=" + health + ", zlataky=" + gold + ", inventar=" + inventory + "]";
    }

}
