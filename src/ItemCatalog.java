import java.util.ArrayList;
import java.util.List;

public final class ItemCatalog {

    // trvala zbran kazdeho hrace

    public static final Item FIST = new Item(ItemType.DAMAGE, "Pěst", 0,
            GameRules.BASIC_STAMINA_COST, GameRules.BASIC_DAMAGE, 1, Damage.DamageType.PHYSICAL,
            Rarity.COMMON, 1, 1, Item.Passive.NONE);

    // sdileny seznam definic itemu

    private static final List<Item> ITEMS = createItems();

    // pristup ke katalogu

    private ItemCatalog() {
    }

    public static List<Item> getItems() {
        return ITEMS;
    }

    // sestaveni katalogu podle kategorii

    private static List<Item> createItems() {
        List<Item> items = new ArrayList<>();
        // casovane zbrane maji silu za kazdy vlastni tah cile a pocet techto tahu
        // fyzicke zbrane
        weapon(items, "Dýka", 3, 2, 4, 1, Damage.DamageType.PHYSICAL, Rarity.COMMON, 2, 3);
        weapon(items, "Krátký meč", 4, 3, 6, 1, Damage.DamageType.PHYSICAL, Rarity.COMMON, 1, 3);
        weapon(items, "Tesák", 5, 3, 7, 1, Damage.DamageType.PHYSICAL, Rarity.COMMON, 1, 2);
        weapon(items, "Lovecké kopí", 5, 4, 8, 1, Damage.DamageType.PHYSICAL, Rarity.COMMON, 1, 2);
        weapon(items, "Válečná sekera", 7, 5, 11, 1, Damage.DamageType.PHYSICAL, Rarity.UNCOMMON, 1, 2);
        weapon(items, "Těžké kladivo", 9, 7, 16, 1, Damage.DamageType.PHYSICAL, Rarity.UNCOMMON, 1, 1);
        weapon(items, "Rapír", 8, 3, 8, 1, Damage.DamageType.PHYSICAL, Rarity.UNCOMMON, 1, 2);
        weapon(items, "Dlouhý luk", 6, 4, 9, 1, Damage.DamageType.PHYSICAL, Rarity.COMMON, 1, 2);
        weapon(items, "Kuše", 10, 6, 14, 1, Damage.DamageType.PHYSICAL, Rarity.UNCOMMON, 1, 1);
        weapon(items, "Obouruční meč", 12, 8, 19, 1, Damage.DamageType.PHYSICAL, Rarity.RARE, 1, 1);
        weapon(items, "Mistrovská čepel", 14, 5, 13, 1, Damage.DamageType.PHYSICAL, Rarity.RARE, 1, 1);
        weapon(items, "Železný palcát", 4, 4, 8, 1, Damage.DamageType.PHYSICAL, Rarity.COMMON, 1, 3);
        // jedove zbrane
        weapon(items, "Jedovatá jehla", 3, 2, 2, 3, Damage.DamageType.POISON, Rarity.COMMON, 2, 3);
        weapon(items, "Hadí dýka", 5, 3, 2, 4, Damage.DamageType.POISON, Rarity.COMMON, 1, 2);
        weapon(items, "Morová hůl", 7, 4, 4, 3, Damage.DamageType.POISON, Rarity.UNCOMMON, 1, 2);
        weapon(items, "Štíří kopí", 9, 5, 4, 4, Damage.DamageType.POISON, Rarity.UNCOMMON, 1, 1);
        weapon(items, "Zmijí tesák", 12, 6, 4, 4, Damage.DamageType.POISON, Rarity.RARE, 1, 1);
        weapon(items, "Trnový bič", 6, 3, 3, 3, Damage.DamageType.POISON, Rarity.COMMON, 1, 2);

        // kratky heal hned pri akci delsi od dalsiho vlastniho tahu
        heal(items, "Malý léčivý lektvar", 2, 2, 5, 1, Rarity.COMMON, 2, 4);
        heal(items, "Léčivý lektvar", 3, 3, 8, 1, Rarity.COMMON, 2, 3);
        heal(items, "Silný léčivý lektvar", 5, 4, 13, 1, Rarity.UNCOMMON, 1, 3);
        heal(items, "Životabudič", 6, 5, 17, 1, Rarity.RARE, 1, 2);
        heal(items, "Bylinný čaj", 2, 2, 4, 2, Rarity.COMMON, 2, 4);
        heal(items, "Hojivá mast", 3, 2, 3, 3, Rarity.COMMON, 2, 3);
        heal(items, "Regenerační odvar", 4, 3, 5, 3, Rarity.UNCOMMON, 1, 3);
        heal(items, "Jarní esence", 5, 3, 4, 4, Rarity.UNCOMMON, 1, 2);
        heal(items, "Balzám obnovy", 6, 4, 5, 4, Rarity.RARE, 1, 2);
        heal(items, "Obvaz", 2, 1, 4, 1, Rarity.COMMON, 2, 4);
        heal(items, "Polní tinktura", 3, 4, 10, 1, Rarity.COMMON, 1, 3);
        heal(items, "Měsíční voda", 4, 2, 3, 4, Rarity.UNCOMMON, 1, 3);

        stamina(items, "Doušek síly", 2, 3, 3, Rarity.COMMON, 2, 4);
        stamina(items, "Lektvar výdrže", 3, 3, 4, Rarity.COMMON, 2, 3);
        stamina(items, "Silný lektvar výdrže", 5, 4, 7, Rarity.UNCOMMON, 1, 2);
        stamina(items, "Bojový elixír", 6, 5, 9, Rarity.RARE, 1, 2);
        stamina(items, "Mátový odvar", 2, 2, 2, Rarity.COMMON, 2, 4);
        stamina(items, "Káva průzkumníka", 3, 2, 3, Rarity.COMMON, 1, 3);
        stamina(items, "Horská vzpruha", 4, 3, 5, Rarity.UNCOMMON, 1, 3);
        stamina(items, "Dračí dech", 6, 4, 8, Rarity.RARE, 1, 2);

        shield(items, "Dřevěný štít", 3, 2, 3, Rarity.COMMON, 1, 3);
        shield(items, "Pukléř", 4, 2, 3, Rarity.COMMON, 1, 2);
        shield(items, "Kulatý štít", 5, 3, 4, Rarity.COMMON, 1, 2);
        shield(items, "Železný štít", 7, 4, 6, Rarity.UNCOMMON, 1, 2);
        shield(items, "Věžový štít", 10, 6, 10, Rarity.UNCOMMON, 1, 1);
        shield(items, "Strážcův štít", 12, 5, 9, Rarity.RARE, 1, 1);
        shield(items, "Proutěný štít", 3, 1, 2, Rarity.COMMON, 2, 3);
        shield(items, "Ocelový pukléř", 8, 3, 5, Rarity.UNCOMMON, 1, 2);
        shield(items, "Královská pavéza", 16, 7, 13, Rarity.RARE, 1, 1);
        shield(items, "Kožený štít", 4, 3, 3, Rarity.COMMON, 1, 3);

        armor(items, "Prošívanice", 4, 15, Damage.DamageType.PHYSICAL, Rarity.COMMON);
        armor(items, "Kožená zbroj", 6, 25, Damage.DamageType.PHYSICAL, Rarity.COMMON);
        armor(items, "Kroužková košile", 8, 35, Damage.DamageType.PHYSICAL, Rarity.UNCOMMON);
        armor(items, "Plátová zbroj", 11, 40, Damage.DamageType.PHYSICAL, Rarity.UNCOMMON);
        armor(items, "Rytířská zbroj", 15, 50, Damage.DamageType.PHYSICAL, Rarity.RARE);
        armor(items, "Alchymistický kabát", 6, 35, Damage.DamageType.POISON, Rarity.COMMON);
        armor(items, "Morový hábit", 12, 60, Damage.DamageType.POISON, Rarity.RARE);

        trinket(items, "Prsten obnovy", 6, 1, Item.Passive.REGENERATION, Rarity.UNCOMMON);
        trinket(items, "Srdce poutníka", 6, 3, Item.Passive.LOW_HEALTH_HEAL, Rarity.COMMON);
        trinket(items, "Fénixův přívěsek", 12, 4, Item.Passive.LOW_HEALTH_HEAL, Rarity.RARE);
        trinket(items, "Prsten výdrže", 8, 2, Item.Passive.STAMINA_BONUS, Rarity.UNCOMMON);
        trinket(items, "Válečníkův amulet", 10, 2, Item.Passive.ATTACK_BONUS, Rarity.UNCOMMON);
        trinket(items, "Hadí kámen", 7, 3, Item.Passive.CLEANSE_POISON, Rarity.UNCOMMON);
        trinket(items, "Jantarová brož", 5, 2, Item.Passive.LOW_HEALTH_HEAL, Rarity.COMMON);
        return List.copyOf(items); // kazdy nakup vytvori vlastni instanci itemu
    }

    // vytvareni jednotlivych kategorii itemu

    // popisy sklada gametext z hodnot bez druhe kopie textu
    private static void weapon(List<Item> items, String name, int price, int cost, int power,
            int duration, Damage.DamageType type, Rarity rarity, int min, int max) {
        items.add(new Item(ItemType.DAMAGE, name, price, cost, power, duration, type,
                rarity, min, max, Item.Passive.NONE));
    }

    private static void heal(List<Item> items, String name, int price, int cost, int power,
            int duration, Rarity rarity, int min, int max) {
        items.add(new Item(ItemType.HEAL, name, price, cost, power, duration, Damage.DamageType.HEAL,
                rarity, min, max, Item.Passive.NONE));
    }

    private static void stamina(List<Item> items, String name, int price, int cost, int power,
            Rarity rarity, int min, int max) {
        items.add(new Item(ItemType.STAMINA, name, price, cost, power, 1, Damage.DamageType.PHYSICAL,
                rarity, min, max, Item.Passive.NONE));
    }

    private static void shield(List<Item> items, String name, int price, int cost, int power,
            Rarity rarity, int min, int max) {
        items.add(new Item(ItemType.DEFEND, name, price, cost, power, 1, Damage.DamageType.PHYSICAL,
                rarity, min, max, Item.Passive.NONE));
    }

    private static void armor(List<Item> items, String name, int price, int percent,
            Damage.DamageType type, Rarity rarity) {
        items.add(new Item(ItemType.ARMOR, name, price, 0, percent, 1, type,
                rarity, 1, 2, Item.Passive.NONE));
    }

    private static void trinket(List<Item> items, String name, int price, int power,
            Item.Passive passive, Rarity rarity) {
        items.add(new Item(ItemType.TRINKET, name, price, 0, power, 1, Damage.DamageType.PHYSICAL,
                rarity, 1, 2, passive));
    }
}
