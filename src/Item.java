public class Item {

    private ItemType itemType;
    private String name;
    
    public Item(ItemType itemType, String name) {
        this.itemType = itemType;
        this.name = name;
    }

    @Override
    public String toString() {
        return "Predmet [druhPredmetu=" + itemType + ", jmeno=" + name + "]";
    }   

}
