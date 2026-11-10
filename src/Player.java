import java.util.ArrayList;
import java.util.List;

public class Player {
    
    // ------------------------------------ inicializace ------------------------------------
    
    public static final int MAX_HEALTH = 4;
    private int health;
    private final String name;
    private int gold;
    private final List<Item> inventory;
    private int inventorySize;

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
        return List.copyOf(inventory);
    }

    public int getInventorySize() {
        return inventorySize;
    }


    
    public boolean addToInventory(Item item) {		// pridani do inventare, pokud je misto
        if (item == null || inventory.size() >= inventorySize) return false;
        inventory.add(item);
        return true;
    }

    public void removeFromInventory(Item item) {
        inventory.remove(item);
    }

    public int _getInventoryCount() {
        return inventory.size();
    }

    public boolean setInventorySize(int newSize) {
        if (newSize < 0 || newSize < _getInventoryCount()) return false;
        this.inventorySize = newSize;
        return true;
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
        return "Hrac [name=" + name + ", zivoty=" + health + ", zlataky=" + gold + ", inventar=" + inventory + "]";
    }
}
