public class Item {

    // ------------------------------------ inicializace ------------------------------------

    private final ItemType itemType;
    private final String name;

    public Item(ItemType itemType, String name) {
        this.itemType = itemType;
        this.name = name;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Predmet [druhPredmetu=" + itemType + ", jmeno=" + name + "]";
    }
}
