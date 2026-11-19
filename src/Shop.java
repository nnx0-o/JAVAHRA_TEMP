import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Shop {

    // oba nakupuji ze stejne nabidky

    private final Map<Item, Integer> stock = new LinkedHashMap<>();

    // co je na prodej

    public Map<Item, Integer> getStock() {
        return Collections.unmodifiableMap(stock);
    }

    // namichame novou nabidku

    public void refresh(Random random) {
        stock.clear();
        List<Item> available = new ArrayList<>(ItemCatalog.getItems());
        for (int count = 0; count < GameRules.SHOP_SIZE && !available.isEmpty(); count++) {
            // vetsi vaha ma vic losu
            int totalWeight = 0;
            for (Item item : available) totalWeight += selectionWeight(item);
            int ticket = random.nextInt(totalWeight); // kazdy typ jen jednou
            Item selected = available.get(0);
            // odecteme vahy a uvidime na koho to vyslo
            for (Item item : available) {
                ticket -= selectionWeight(item);
                if (ticket < 0) {
                    selected = item;
                    break;
                }
            }
            // tenhle typ uz mame ted jeste kolik kusu
            available.remove(selected);
            int quantity = selected.getMinStock() + random.nextInt(selected.getMaxStock() - selected.getMinStock() + 1);
            stock.put(selected, quantity);
        }
    }

    private int selectionWeight(Item item) {
        // porad random jen levne zbrane padaji casteji
        int multiplier = item.getItemType() == ItemType.DAMAGE && item.getPrice() <= 7 ? 3 : 1;
        return item.getRarity().getWeight() * multiplier;
    }

    // nakup a prodej

    public boolean purchase(Player player, Item item) {
        if (item == null || stock.getOrDefault(item, 0) < 1) return false;
        int price = item.getPrice();
        if (player.getGold() < price) return false;
        if (!player.addToInventory(item.copy())) return false; // nejdriv misto pak penize
        stock.put(item, stock.get(item) - 1);
        player.changeGold(-price);
        return true;
    }

    public boolean sell(Player player, Item item) {
        if (item == null || !player.getEquipment().remove(item)) return false;
        player.changeGold(item.getPrice() / 2); // pri prodeji zaokrouhlime dolu
        return true; // do nabidky uz to nevracime
    }
}
