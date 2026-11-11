import java.util.List;

public class Player {

    // ------------------------------------ inicializace ------------------------------------

    public static final int MAX_HEALTH = 4;
    private int health;
    private final String name;
    private int gold;
    private final Inventory inventory;

    public Player(String name) {
        this.name = name;
        this.health = MAX_HEALTH;
        this.gold = 10;
        this.inventory = new Inventory();
    }

    // ------------------------------------ zakladni metody ------------------------------------

    public String getName() {
        return name;
    }


    public int getGold() {
        return gold;
    }

    public void changeGold(int amount) {
        gold = Math.max(0, gold + amount);
    }

    // ------------------------------------ inventar ------------------------------------

    public List<Item> getInventory() {
        return inventory.getItems();
    }

    public int getInventorySize() {
        return inventory.getCapacity();
    }



    public boolean addToInventory(Item item) {		// pridani do inventare, pokud je misto
        return inventory.add(item);
    }

    public void removeFromInventory(Item item) {
        inventory.remove(item);
    }


    public boolean setInventorySize(int newSize) {
        return inventory.setCapacity(newSize);
    }


    public void resetInventory() {
        inventory.clear();
    }

    // ------------------------------------ zdravi ------------------------------------

    public int getHealth() {
        return health;
    }

    public boolean isAlive() {
        return health > 0;
    }


    public void takeDamage(int damage) {
        health = Math.max(0, health - Math.max(0, damage));
    }

    public void heal(int amount) {
        health = Math.min(MAX_HEALTH, health + Math.max(0, amount));
    }

    // ------------------------------------ dalsi ------------------------------------

    @Override
    public String toString() {
        return "Hráč [jméno=" + name + ", životy=" + health + ", zlaťáky=" + gold + ", inventář=" + inventory + "]";
    }
}
