import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Shop {

    // sdilene zasoby obchodu

    private final Map<Item, Integer> stock = new LinkedHashMap<>();

    // cteni nabidky

    public Map<Item, Integer> getStock() {
        return Collections.unmodifiableMap(stock);
    }

    // random refresh nabidky a vahy vyberu

    public void refresh(Random random) {
        stock.clear();
        List<Item> available = new ArrayList<>(ItemCatalog.getItems());
        for (int count = 0; count < GameRules.SHOP_SIZE && !available.isEmpty(); count++) {
            // kazda jednotka vahy je los vetsi vaha znamena vetsi sanci
            int totalWeight = 0;
            for (Item item : available) totalWeight += selectionWeight(item);
            int ticket = random.nextInt(totalWeight); // vyber podle vah bez opakovani stejneho druhu
            Item selected = available.get(0);
            // odecitame vahy dokud nenajdeme item pro vylosovane cislo
            for (Item item : available) {
                ticket -= selectionWeight(item);
                if (ticket < 0) {
                    selected = item;
                    break;
                }
            }
            // vybrany typ uz nelosujeme pocet kusu ma vlastni random
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

    // nakup do inventare a prodej itemu

    public boolean purchase(Player player, Item item) {
        if (item == null || stock.getOrDefault(item, 0) < 1) return false;
        int price = item.getPrice();
        if (player.getGold() < price) return false;
        if (!player.addToInventory(item.copy())) return false; // nejdriv check mista bez ztraty goldu
        stock.put(item, stock.get(item) - 1);
        player.changeGold(-price);
        return true;
    }

    public boolean sell(Player player, Item item) {
        if (item == null || !player.getEquipment().remove(item)) return false;
        player.changeGold(item.getPrice() / 2); // jen prodej zaokrouhluje dolu
        return true; // item se nevraci do spolecne nabidky
    }
}
