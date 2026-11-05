public class PlayerPurchaseTest {		// kontrola nakupu bez dalsi knihovny
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        Item potion = new Item(ItemType.POTION, "Lektvar");
        Player player = new Player("Test");
        check(player.addToInventory(potion), "Přidání předmětu musí uspět.");
        check(player.getGold() == 10, "Přidání předmětu nesmí stát zlato.");
        check(player.buy(potion, 3), "Dostupný nákup musí uspět.");
        check(player.getGold() == 7 && player.getInventory().size() == 2,
                "Nákup musí přidat předmět a odečíst přesnou cenu.");
        check(!player.buy(potion, 8), "Nedostatek zlata musí nákup odmítnout.");
        check(!player.buy(potion, -1), "Záporná cena musí být odmítnuta.");
        check(!player.buy(null, 3), "Null nesmí být zakoupen.");
        check(player.getGold() == 7 && player.getInventory().size() == 2,
                "Odmítnuté nákupy nesmí změnit stav.");
        while (!player.getInventory().isFull()) player.addToInventory(potion);
        check(!player.buy(potion, 3), "Plný inventář musí nákup odmítnout.");
        check(player.getGold() == 7 && player.getInventory().size() == 5,
                "Plný inventář nesmí způsobit ztrátu zlata.");
        player.getInventory().removeItem(0);
        check(player.getGold() == 7, "Odebrání předmětu nesmí změnit zlato.");
        check(player.buy(potion, 7), "Musí být možné utratit poslední zlato.");
        check(player.getGold() == 0 && player.getInventory().size() == 5,
                "Nákup za přesný zůstatek musí být dokončen.");
        player.getInventory().removeItem(0);
        check(player.addToInventory(potion), "Předmět lze přidat i bez zlata.");
        System.out.println("PlayerPurchaseTest: OK");
    }
}
