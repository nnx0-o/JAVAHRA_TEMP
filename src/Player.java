import java.util.ArrayList;
import java.util.List;

public class Player {
    
    // ------------------------------------ inicializace ------------------------------------
    
    public static final int MAX_HEALTH = 4;
    private final String name;
    private int health;
    private int gold;
    private final List<Item> inventory;
    private final int inventorySize;

    public Player(String name) {
        this.name = name;
        this.health = MAX_HEALTH;
        this.gold = 10;
        this.inventory = new ArrayList<>();
        this.inventorySize = 5;
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
        return inventory;
    }

    public int getInventorySize() {
        return inventorySize;
    }

    public boolean addToInventory(Item item) {		// pridani do inventare, pokud je misto
        if (item == null || inventory.size() >= inventorySize) return false;
        inventory.add(item);
        return true;
    }

    public boolean removeFromInventory(Item item) {
        return inventory.remove(item);
    }

    public boolean setInventorySize(int inventorySize) {
        if (inventorySize < 0) return false;
        while (inventory.size() > inventorySize) {
            inventory.remove(inventory.size() - 1);
        }
        return true;
    }

    public boolean resetInventory() {
        inventory.clear();
        return true;
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
        return "Hrac [name=" + name + ", zivoty=" + health + ", zlataky=" + gold + ", inventar=" + inventory + "]";
    }
}
