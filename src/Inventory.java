import java.util.ArrayList;
import java.util.List;

public class Inventory {

    // ------------------------------------ obsah, kapacita a odkazy na vybavene predmety ------------------------------------

    private final List<Item> items = new ArrayList<>(List.of(ItemCatalog.FIST));
    private Item equippedArmor;
    private Item equippedTrinket;

    // ------------------------------------ cteni obsahu, kapacity a vybaveni ------------------------------------

    public List<Item> getItems() {
        return List.copyOf(items);
    }

    public int getCapacity() {
        return GameRules.INVENTORY_CAPACITY + 1; // pest a deset koupenych predmetu
    }

    public Item getEquippedArmor() {
        return equippedArmor;
    }

    public Item getEquippedTrinket() {
        return equippedTrinket;
    }

    // ------------------------------------ pridani a bezpecne odebrani predmetu ------------------------------------

    public boolean add(Item item) {
        if (item == null || items.contains(item) || items.size() >= getCapacity()) return false;
        items.add(item); // kazdy fyzicky kus muze byt ulozen pouze jednou
        return true;
    }

    public boolean remove(Item item) {
        if (item == ItemCatalog.FIST || !items.remove(item)) return false;
        if (item == equippedArmor) equippedArmor = null;
        if (item == equippedTrinket) equippedTrinket = null;
        return true;
    }

    // ------------------------------------ vybaveni a sundani zbroje nebo talismanu ------------------------------------

    public boolean equipArmor(Item item) {
        if (item != null && (item.getItemType() != ItemType.ARMOR || !items.contains(item))) return false;
        equippedArmor = item; // null znamena sundani bez vyhozeni z inventare
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
