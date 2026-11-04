import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contains the shop's economy and purchase rules.
 * This class has no UI knowledge; callers use its result/state to update a UI.
 */
public class Shop {
    // Nabídka pro skutečné hráče. Starší metody níže slouží jen demu MyApp.
    public int getPrice(ItemType type) {
        return switch (type) {
            case POTION -> 3;
            case SWORD -> 5;
            case SHIELD -> 4;
            default -> -1;
        };
    }

    public boolean buy(Player player, ItemType type) {
        int price = getPrice(type);
        if (price < 0 || player.getGold() < price || player.getInventory().isFull()) return false;
        if (!player.getInventory().addItem(new Item(type, type.name()))) return false;
        player.spendGold(price);
        return true;
    }
    private static final int HEALTH_POTION_PRICE = 25;
    private static final int KNIGHT_SWORD_PRICE = 80;
    private static final int MINER_PRICE = 120;

    private int gold = 100;
    private int goldPerSecond = 1;
    private final List<String> inventory = new ArrayList<>(List.of("Starter Boots"));

    public int getGold() {
        return gold;
    }

    public int getGoldPerSecond() {
        return goldPerSecond;
    }

    public List<String> getInventory() {
        return Collections.unmodifiableList(inventory);
    }

    public String getLatestItem() {
        return inventory.get(inventory.size() - 1);
    }

    public void collectIncome() {
        gold += goldPerSecond;
    }

    public void rewardForSubmittedPass() {
        gold += 10;
    }

    public boolean buyHealthPotion() {
        return buyItem("Health Potion", HEALTH_POTION_PRICE);
    }

    public boolean buyKnightSword() {
        return buyItem("Knight Sword", KNIGHT_SWORD_PRICE);
    }

    public boolean hireMiner() {
        if (!spendGold(MINER_PRICE)) return false;
        goldPerSecond += 5;
        return true;
    }

    public boolean redeemCoupon(String coupon) {
        if (!"RICH".equalsIgnoreCase(coupon.trim())) return false;
        gold += 300;
        return true;
    }

    private boolean buyItem(String itemName, int price) {
        if (!spendGold(price)) return false;
        inventory.add(itemName);
        return true;
    }

    private boolean spendGold(int amount) {
        if (gold < amount) return false;
        gold -= amount;
        return true;
    }
}
