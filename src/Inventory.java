import java.util.ArrayList;
import java.util.List;

public class Inventory {

    // ------------------------------------ obsah, kapacita a odkazy na vybavene predmety ------------------------------------

    private final List<Item> items = new ArrayList<>();
    private int capacity;
    private Item equippedArmor;
    private Item equippedTrinket;

    // ------------------------------------ vytvoreni inventare ------------------------------------

    public Inventory() {
        this(GameRules.INVENTORY_CAPACITY);
    }

    public Inventory(int capacity) {
        if (capacity < 0) throw new IllegalArgumentException("Kapacita nesmí být záporná.");
        this.capacity = capacity;
    }

    // ------------------------------------ cteni obsahu, kapacity a vybaveni ------------------------------------

    public List<Item> getItems() {
        return List.copyOf(items);
    }

    public int getItemCount() {
        return items.size();
    }

    public int getCapacity() {
        return capacity + capacityBonus(equippedTrinket);
    }

    public Item getEquippedArmor() {
        return equippedArmor;
    }

    public Item getEquippedTrinket() {
        return equippedTrinket;
    }

    // ------------------------------------ kapacita pridana talismanem ------------------------------------

    private int capacityBonus(Item item) {
        return item != null && item.getPassive() == Item.Passive.CAPACITY ? item.getPower() : 0;
    }

    // ------------------------------------ pridani a bezpecne odebrani predmetu ------------------------------------

    public boolean add(Item item) {
        if (item == null || items.contains(item) || items.size() >= getCapacity()) return false;
        items.add(item); // kazdy fyzicky kus muze byt ulozen pouze jednou
        return true;
    }

    public boolean canRemove(Item item) {
        if (!items.contains(item)) return false;
        int remainingCapacity = item == equippedTrinket ? capacity : getCapacity();
        return items.size() - 1 <= remainingCapacity; // pri prodeji vaku nesmime ztratit ostatni veci
    }

    public boolean remove(Item item) {
        if (!canRemove(item)) return false;
        items.remove(item);
        if (item == equippedArmor) equippedArmor = null;
        if (item == equippedTrinket) equippedTrinket = null;
        return true;
    }

    // ------------------------------------ zmena kapacity a vyprazdneni inventare ------------------------------------

    public boolean setCapacity(int newCapacity) {
        if (newCapacity < 0 || newCapacity + capacityBonus(equippedTrinket) < items.size()) return false;
        capacity = newCapacity;
        return true;
    }

    public void clear() {
        items.clear();
        equippedArmor = null;
        equippedTrinket = null;
    }

    // ------------------------------------ vybaveni a sundani zbroje nebo talismanu ------------------------------------

    public boolean equipArmor(Item item) {
        if (item != null && (item.getItemType() != ItemType.ARMOR || !items.contains(item))) return false;
        equippedArmor = item; // null znamena sundani bez vyhozeni z inventare
        return true;
    }

    public boolean equipTrinket(Item item) {
        if (item != null && (item.getItemType() != ItemType.TRINKET || !items.contains(item))) return false;
        if (items.size() > capacity + capacityBonus(item)) return false;
        equippedTrinket = item;
        return true;
    }

    public boolean isEquipped(Item item) {
        return item != null && (item == equippedArmor || item == equippedTrinket);
    }

    // ------------------------------------ textovy vypis obsahu ------------------------------------

    @Override
    public String toString() {
        return items.toString();
    }
}
