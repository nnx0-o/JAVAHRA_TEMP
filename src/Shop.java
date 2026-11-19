import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Shop {

    // ------------------------------------ sdilene zasoby obchodu ------------------------------------

    private final Map<Item, Integer> stock = new LinkedHashMap<>();

    // ------------------------------------ cteni nabidky ------------------------------------

    public Map<Item, Integer> getStock() {
        return Collections.unmodifiableMap(stock);
    }

    // ------------------------------------ nahodne obnoveni nabidky a vahy vyberu ------------------------------------

    public void refresh(Random random) {
        stock.clear();
        List<Item> available = new ArrayList<>(ItemCatalog.getItems());
        for (int count = 0; count < GameRules.SHOP_SIZE && !available.isEmpty(); count++) {
            // Kazda jednotka vahy je jeden los. Vyssi vaha znamena vice sanci na vyber.
            int totalWeight = 0;
            for (Item item : available) totalWeight += selectionWeight(item);
            int ticket = random.nextInt(totalWeight); // vyber podle vah bez opakovani stejneho druhu
            Item selected = available.get(0);
            // Odectenim vah najdeme predmet, do jehoz intervalu vylosovane cislo spada.
            for (Item item : available) {
                ticket -= selectionWeight(item);
                if (ticket < 0) {
                    selected = item;
                    break;
                }
            }
            // Vybrany druh uz nelosujeme znovu. Jeho pocet kusu urci samostatny nahodny vyber.
            available.remove(selected);
            int quantity = selected.getMinStock() + random.nextInt(selected.getMaxStock() - selected.getMinStock() + 1);
            stock.put(selected, quantity);
        }
    }

    private int selectionWeight(Item item) {
        // vsechny nabidky zustavaji nahodne ale levne zbrane jsou dostupne casteji
        int multiplier = item.getItemType() == ItemType.DAMAGE && item.getPrice() <= 7 ? 3 : 1;
        return item.getRarity().getWeight() * multiplier;
    }

    // ------------------------------------ nakup do inventare a prodej predmetu ------------------------------------

    public boolean purchase(Player player, Item item) {
        if (item == null || stock.getOrDefault(item, 0) < 1) return false;
        int price = item.getPrice();
        if (player.getGold() < price) return false;
        if (!player.addToInventory(item.copy())) return false; // nejprve overime misto bez ztraty zlata
        stock.put(item, stock.get(item) - 1);
        player.changeGold(-price);
        return true;
    }

    public boolean sell(Player player, Item item) {
        if (item == null || !player.getEquipment().remove(item)) return false;
        player.changeGold(item.getPrice() / 2); // jedina vyjimka zaokrouhlovani je prodej dolu
        return true; // predmet se nikdy nevraci do sdilene nabidky
    }
}
