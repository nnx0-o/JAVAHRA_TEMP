import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Shop {

    // ------------------------------------ sdilene zasoby a ceny nabidek ------------------------------------

    private final Map<Item, Integer> stock = new LinkedHashMap<>();
    private final Map<Item, Integer> prices = new LinkedHashMap<>();

    // ------------------------------------ cteni nabidky a cen ------------------------------------

    public Map<Item, Integer> getStock() {
        return Collections.unmodifiableMap(stock);
    }

    public int getPrice(Item item) {
        return prices.getOrDefault(item, -1);
    }

    // ------------------------------------ nahodne obnoveni nabidky a vahy vyberu ------------------------------------

    public void refresh(Random random) {
        stock.clear();
        prices.clear();
        List<Item> available = new ArrayList<>(ItemCatalog.getItems());
        for (int count = 0; count < GameRules.SHOP_SIZE && !available.isEmpty(); count++) {
            int totalWeight = available.stream().mapToInt(this::selectionWeight).sum();
            int ticket = random.nextInt(totalWeight); // vyber podle vah bez opakovani stejneho druhu
            Item selected = available.get(0);
            for (Item item : available) {
                ticket -= selectionWeight(item);
                if (ticket < 0) {
                    selected = item;
                    break;
                }
            }
            available.remove(selected);
            int quantity = selected.getMinStock() + random.nextInt(selected.getMaxStock() - selected.getMinStock() + 1);
            addItem(selected, selected.getPrice(), quantity);
        }
    }

    private int selectionWeight(Item item) {
        // vsechny nabidky zustavaji nahodne ale levne zbrane jsou dostupne casteji
        int multiplier = item.getItemType() == ItemType.DAMAGE && item.getPrice() <= 7 ? 3 : 1;
        return item.getRarity().getWeight() * multiplier;
    }

    // ------------------------------------ pridani nabidky a odebrani kusu ze skladu ------------------------------------

    public boolean addItem(Item item, int price, int quantity) {
        if (item == null || price < 0 || quantity < 0) return false;
        stock.merge(item, quantity, Integer::sum);
        prices.put(item, price);
        return true;
    }

    public boolean buy(Item item, int quantity) {
        if (item == null || quantity <= 0 || stock.getOrDefault(item, 0) < quantity) return false;
        stock.put(item, stock.get(item) - quantity);
        return true;
    }

    // ------------------------------------ nakup do inventare a prodej predmetu ------------------------------------

    public boolean purchase(Player player, Item item) {
        int price = getPrice(item);
        if (price < 0 || player.getGold() < price || stock.getOrDefault(item, 0) < 1) return false;
        if (!player.addToInventory(item.copy())) return false; // nejprve overime misto bez ztraty zlata
        buy(item, 1);
        player.changeGold(-price);
        return true;
    }

    public boolean sell(Player player, Item item) {
        if (item == null || !player.getEquipment().remove(item)) return false;
        player.changeGold(item.getPrice() / 2); // jedina vyjimka zaokrouhlovani je prodej dolu
        return true; // predmet se nikdy nevraci do sdilene nabidky
    }
}
