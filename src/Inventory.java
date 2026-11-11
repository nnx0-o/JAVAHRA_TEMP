import java.util.ArrayList;
import java.util.List;

public class Inventory {

    // ------------------------------------ inicializace ------------------------------------

    private final List<Item> items;
    private int capacity;
    private Item equippedArmor;
    private Item equippedTrinket;

    public Inventory() {
        this(5);
    }

    public Inventory(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Kapacita inventáře nemůže být záporná");
        }
        this.items = new ArrayList<>();
        this.capacity = capacity;
    }

    // ------------------------------------ gettery ------------------------------------

    public List<Item> getItems() {
        return List.copyOf(items);
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean add(Item item) {
        if (item == null || items.size() >= capacity) {
            return false;
        }
        items.add(item);
        return true;
    }

    // ------------------------------------ upravy inventare ------------------------------------

    public boolean remove(Item item) {
        boolean removed = items.remove(item);
        if (removed) {
            if (item == equippedArmor) {
                equippedArmor = null;
            }
            if (item == equippedTrinket) {
                equippedTrinket = null;
            }
        }
        return removed;
    }

    public int getItemCount() {
        return items.size();
    }


    public boolean setCapacity(int newCapacity) {
        if (newCapacity < 0 || newCapacity < items.size()) {
            return false;
        }
        capacity = newCapacity;
        return true;
    }

    public void clear() {
        items.clear();
        equippedArmor = null;
        equippedTrinket = null;
    }

    // ------------------------------------ vybaveni ------------------------------------

    public Item getEquippedArmor() {
        return equippedArmor;
    }

    public Item getEquippedTrinket() {
        return equippedTrinket;
    }

    public boolean equipArmor(Item item) {
        if (item == null || item.getItemType() != ItemType.ARMOR || !items.contains(item)) {
            return false;
        }
        equippedArmor = item;
        return true;
    }

    public boolean equipTrinket(Item item) {
        if (item == null || item.getItemType() != ItemType.TRINKET || !items.contains(item)) {
            return false;
        }
        equippedTrinket = item;
        return true;
    }

    @Override
    public String toString() {
        return items.toString();
    }
}
