import java.util.ArrayList;
import java.util.List;

public class Inventory {

    // co hrac ma a co ma na sobe

    private final List<Item> items = new ArrayList<>(List.of(ItemCatalog.FIST));
    private Item equippedArmor;
    private Item equippedTrinket;

    // co je v inventari

    public List<Item> getItems() {
        return List.copyOf(items);
    }

    public int getCapacity() {
        return GameRules.INVENTORY_CAPACITY + 1; // pest a deset koupenych itemu
    }

    public Item getEquippedArmor() {
        return equippedArmor;
    }

    public Item getEquippedTrinket() {
        return equippedTrinket;
    }

    // pridat nebo vyndat item

    public boolean add(Item item) {
        if (item == null || items.contains(item) || items.size() >= getCapacity()) return false;
        items.add(item); // ten samy kus sem dvakrat nedame
        return true;
    }

    public boolean remove(Item item) {
        if (item == ItemCatalog.FIST || !items.remove(item)) return false;
        if (item == equippedArmor) equippedArmor = null;
        if (item == equippedTrinket) equippedTrinket = null;
        return true;
    }

    // oblect nebo sundat

    public boolean equipArmor(Item item) {
        if (item != null && (item.getItemType() != ItemType.ARMOR || !items.contains(item))) return false;
        equippedArmor = item; // null jen sunda item v inventari zustane
        return true;
    }

    public boolean equipTrinket(Item item) {
        if (item != null && (item.getItemType() != ItemType.TRINKET || !items.contains(item))) return false;
        equippedTrinket = item;
        return true;
    }

    public boolean isEquipped(Item item) {
        return item != null && (item == equippedArmor || item == equippedTrinket);
    }

}
