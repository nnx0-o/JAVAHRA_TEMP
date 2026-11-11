import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Shop {

    // ------------------------------------ inicializace ------------------------------------

    private final Map<Item, Integer> stock = new LinkedHashMap<>();
    private final Map<Item, Integer> prices = new LinkedHashMap<>();

    public Shop() {
        
    }

    // ------------------------------------ gettery ------------------------------------

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

        stock.merge(item, quantity, Integer::sum);
        prices.put(item, price);
        return true;
    }

    public boolean buy(Item item, int quantity) {
        if (item == null || quantity <= 0 || stock.getOrDefault(item, 0) < quantity) {
            return false;
        }

        stock.put(item, stock.get(item) - quantity);
        return true;
    }
}
