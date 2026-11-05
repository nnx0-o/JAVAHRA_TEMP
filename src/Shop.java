import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Shop {		// nabidka predmetu a jejich cen
    private final Map<Item, Integer> stock = new LinkedHashMap<>();

    public Shop() {
        stock.put(new Item(ItemType.POTION, "Lektvar"), 3);
        stock.put(new Item(ItemType.SWORD, "Meč"), 5);
        stock.put(new Item(ItemType.SHIELD, "Štít"), 4);
    }

    public Map<Item, Integer> getStock() {
        return Collections.unmodifiableMap(stock);	// nabidka pouze pro cteni
    }
}
