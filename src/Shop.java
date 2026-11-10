import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Shop { // nabídka předmětů, jejich cen a množství
    private final Map<Item, Integer> stock = new LinkedHashMap<>();
    private final Map<Item, Integer> prices = new LinkedHashMap<>();

    
    public Shop() {
        addOffer(new Item(ItemType.POTION, "Lektvar"), 3, 3);
        addOffer(new Item(ItemType.SWORD, "Meč"), 5, 5);
        addOffer(new Item(ItemType.SHIELD, "Štít"), 4, 4);
    }


    public Map<Item, Integer> getStock() {
        return Collections.unmodifiableMap(stock); // nabídka pouze pro čtení
    }


    public int getPrice(Item item) {
        return prices.getOrDefault(item, -1);
    }


    public boolean addItem(Item item, int price, int quantity) {
        if (item == null || price < 0 || quantity < 0) {
            return false;
        }

        addOffer(item, price, quantity);
        return true;
    }


    private void addOffer(Item item, int price, int quantity) {
        stock.merge(item, quantity, Integer::sum);
        prices.put(item, price);
    }


    public boolean buy(Item item, int quantity) {
        if (item == null || quantity <= 0 || stock.getOrDefault(item, 0) < quantity) {
            return false;
        }

        stock.put(item, stock.get(item) - quantity);
        return true;
    }
}
