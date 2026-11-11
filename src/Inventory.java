import java.util.ArrayList;
import java.util.List;

public class Inventory {

    // ------------------------------------ inicializace ------------------------------------

    private final List<Item> items;
    private int capacity;
    private Item equippedHA;
    private Item equippedLA;

    public Inventory() {
        this(5);
    }

    public Inventory(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Inventory capacity cannot be negative");
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

    public boolean remove(Item item) {
        boolean removed = items.remove(item);
        if (removed) {
            if (item == equippedHA) {
                equippedHA = null;
            }
            if (item == equippedLA) {
                equippedLA = null;
            }
        }
        return removed;
    }

    public int getItemCount() {
        return items.size();
    }

    // ------------------------------------ upravy inventare ------------------------------------

    public boolean setCapacity(int newCapacity) {
        if (newCapacity < 0 || newCapacity < items.size()) {
            return false;
        }
        capacity = newCapacity;
        return true;
    }

    public void clear() {
        items.clear();
        equippedHA = null;
        equippedLA = null;
    }

    // ------------------------------------ vybaveni ------------------------------------

    public Item getEquippedHA() {
        return equippedHA;
    }

    public Item getEquippedLA() {
        return equippedLA;
    }

    public boolean equipHA(Item item) {
        if (item == null || item.getItemType() != ItemType.HA || !items.contains(item)) {
            return false;
        }
        equippedHA = item;
        return true;
    }

    public boolean equipLA(Item item) {
        if (item == null || item.getItemType() != ItemType.LA || !items.contains(item)) {
            return false;
        }
        equippedLA = item;
        return true;
    }

    @Override
    public String toString() {
        return items.toString();
    }
}
