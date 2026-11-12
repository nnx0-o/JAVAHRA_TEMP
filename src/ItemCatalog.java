import java.util.ArrayList;
import java.util.List;

public final class ItemCatalog {

    // ------------------------------------ sdileny seznam definic predmetu ------------------------------------

    private static final List<Item> ITEMS = createItems();

    // ------------------------------------ pristup ke katalogu ------------------------------------

    private ItemCatalog() {
    }

    public static List<Item> getItems() {
        return ITEMS;
    }

    // ------------------------------------ sestaveni katalogu podle kategorii ------------------------------------

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
        // ohnive zbrane
        weapon(items, "Pochodeň", 3, 2, 2, 3, Damage.DamageType.FIRE, Rarity.COMMON, 2, 3);
        weapon(items, "Žhavý prut", 5, 3, 4, 3, Damage.DamageType.FIRE, Rarity.COMMON, 1, 2);
        weapon(items, "Plamenná hůl", 7, 4, 4, 4, Damage.DamageType.FIRE, Rarity.UNCOMMON, 1, 2);
        weapon(items, "Ohnivá čepel", 9, 5, 6, 3, Damage.DamageType.FIRE, Rarity.UNCOMMON, 1, 1);
        weapon(items, "Dračí žezlo", 13, 7, 6, 4, Damage.DamageType.FIRE, Rarity.RARE, 1, 1);
        weapon(items, "Jiskřící hůlka", 6, 2, 4, 2, Damage.DamageType.FIRE, Rarity.UNCOMMON, 1, 2);
        // jedove zbrane
        weapon(items, "Jedovatá jehla", 3, 2, 2, 3, Damage.DamageType.POISON, Rarity.COMMON, 2, 3);
        weapon(items, "Hadí dýka", 5, 3, 2, 4, Damage.DamageType.POISON, Rarity.COMMON, 1, 2);
        weapon(items, "Morová hůl", 7, 4, 4, 3, Damage.DamageType.POISON, Rarity.UNCOMMON, 1, 2);
        weapon(items, "Štíří kopí", 9, 5, 4, 4, Damage.DamageType.POISON, Rarity.UNCOMMON, 1, 1);
        weapon(items, "Zmijí tesák", 12, 6, 4, 4, Damage.DamageType.POISON, Rarity.RARE, 1, 1);
        weapon(items, "Trnový bič", 6, 3, 3, 3, Damage.DamageType.POISON, Rarity.COMMON, 1, 2);

        // kratke leceni probiha pri provedeni planu a delsi zacina v pristim vlastnim tahu
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

        armor(items, "Prošívanice", 4, 15, Damage.DamageType.PHYSICAL, 0, Rarity.COMMON);
        armor(items, "Kožená zbroj", 6, 25, Damage.DamageType.PHYSICAL, 0, Rarity.COMMON);
        armor(items, "Kroužková košile", 8, 35, Damage.DamageType.PHYSICAL, 1, Rarity.UNCOMMON);
        armor(items, "Plátová zbroj", 11, 40, Damage.DamageType.PHYSICAL, 2, Rarity.UNCOMMON);
        armor(items, "Rytířská zbroj", 15, 50, Damage.DamageType.PHYSICAL, 3, Rarity.RARE);
        armor(items, "Žáruvzdorný plášť", 6, 35, Damage.DamageType.FIRE, 0, Rarity.COMMON);
        armor(items, "Dračí šupiny", 12, 60, Damage.DamageType.FIRE, 1, Rarity.RARE);
        armor(items, "Alchymistický kabát", 6, 35, Damage.DamageType.POISON, 0, Rarity.COMMON);
        armor(items, "Morový hábit", 12, 60, Damage.DamageType.POISON, 1, Rarity.RARE);
        thresholdArmor(items, "Zpevněná vesta", 7, 2, 5, Item.Passive.SMALL_HIT_BLOCK, 0);
        thresholdArmor(items, "Nárazová zbroj", 10, 4, 9, Item.Passive.LARGE_HIT_BLOCK, 1);
        thresholdArmor(items, "Strážní kabát", 9, 3, 6, Item.Passive.SMALL_HIT_BLOCK, 1);

        trinket(items, "Prsten obnovy", 6, 1, Item.Passive.REGENERATION, Rarity.UNCOMMON,
                "Na začátku vlastního tahu obnoví 1 život.");
        trinket(items, "Srdce poutníka", 6, 3, Item.Passive.LOW_HEALTH_HEAL, Rarity.COMMON,
                "Na začátku vlastního tahu obnoví 3 životy, pokud máš méně než polovinu života.");
        trinket(items, "Fénixův přívěsek", 12, 4, Item.Passive.LOW_HEALTH_HEAL, Rarity.RARE,
                "Na začátku vlastního tahu obnoví 4 životy, pokud máš méně než polovinu života.");
        trinket(items, "Prsten výdrže", 8, 2, Item.Passive.STAMINA_BONUS, Rarity.UNCOMMON,
                "Na začátku vlastního tahu přidá 2 výdrže nad běžné maximum.");
        trinket(items, "Válečníkův amulet", 10, 2, Item.Passive.ATTACK_BONUS, Rarity.UNCOMMON,
                "Každý fyzický útok včetně základního způsobí o 2 poškození více před obranou cíle.");
        trinket(items, "Hadí kámen", 7, 3, Item.Passive.CLEANSE_POISON, Rarity.UNCOMMON,
                "Na začátku vlastního tahu odstraní celkem až 3 zbývajícího poškození jedem před jeho působením, od nejstaršího jedu.");
        trinket(items, "Kapka deště", 7, 3, Item.Passive.CLEANSE_FIRE, Rarity.UNCOMMON,
                "Na začátku vlastního tahu odstraní až 3 zbývajícího poškození ohněm před jeho působením.");
        trinket(items, "Cestovní brašna", 4, 3, Item.Passive.CAPACITY, Rarity.COMMON,
                "Při vybavení přidá 3 místa v inventáři. Zabírá místo talismanu i jedno místo v inventáři.");
        trinket(items, "Velký batoh", 7, 6, Item.Passive.CAPACITY, Rarity.COMMON,
                "Při vybavení přidá 6 míst v inventáři. Před sundáním musí obsah vejít do menší kapacity.");
        trinket(items, "Jantarová brož", 5, 2, Item.Passive.LOW_HEALTH_HEAL, Rarity.COMMON,
                "Na začátku vlastního tahu obnoví 2 životy před časovanými účinky, pokud máš méně než polovinu života.");
        return List.copyOf(items); // 76 definic a kazdy nakup z nich vytvori novy kus
    }

    // ------------------------------------ vytvareni zbrani ------------------------------------

    private static void weapon(List<Item> items, String name, int price, int cost, int power,
            int duration, Damage.DamageType type, Rarity rarity, int min, int max) {
        String description = type == Damage.DamageType.PHYSICAL
                ? "Při provedení plánu způsobí " + power + " fyzického poškození."
                : type + ": " + power + " poškození po dobu " + duration + " tahů (celkem " + power * duration
                        + "). Působí na začátku soupeřova tahu, poprvé po provedení plánu. "
                        + (type == Damage.DamageType.FIRE ? "Oheň se slučuje a zachovává celkový zbytek." : "Každý jed působí samostatně.");
        items.add(new Item(ItemType.DAMAGE, name, price, cost, power, duration, type, rarity, min, max,
                Item.Passive.NONE, 0, 0, description + " Cena: " + cost + " výdrže. Zůstává; opakované použití v témže tahu stojí více výdrže."));
    }

    // ------------------------------------ vytvareni lecivych predmetu ------------------------------------

    private static void heal(List<Item> items, String name, int price, int cost, int power,
            int duration, Rarity rarity, int min, int max) {
        String health = power + (power == 1 ? " život" : power <= 4 ? " životy" : " životů");
        String effect = duration == 1 ? "Při provedení plánu obnoví " + health
                : "Obnoví " + health + " za tah po dobu " + duration + " tahů (celkem " + power * duration + "), od příštího vlastního tahu";
        items.add(new Item(ItemType.HEAL, name, price, cost, power, duration, Damage.DamageType.HEAL,
                rarity, min, max, Item.Passive.NONE, 0, 0, effect
                + ", nejvýše do maxima. Cena: " + cost + " výdrže. Po použití se spotřebuje."));
    }

    // ------------------------------------ vytvareni predmetu pro obnovu vydrze ------------------------------------

    private static void stamina(List<Item> items, String name, int price, int cost, int power,
            Rarity rarity, int min, int max) {
        items.add(new Item(ItemType.STAMINA, name, price, cost, power, 1, Damage.DamageType.PHYSICAL,
                rarity, min, max, Item.Passive.NONE, 0, 0, "Na začátku příštího vlastního tahu přidá "
                + power + " výdrže nad běžné maximum. Nyní stojí " + cost + " výdrže. Spotřebuje se; bonusy se sčítají."));
    }

    // ------------------------------------ vytvareni stitu ------------------------------------

    private static void shield(List<Item> items, String name, int price, int cost, int power,
            Rarity rarity, int min, int max) {
        items.add(new Item(ItemType.DEFEND, name, price, cost, power, 1, Damage.DamageType.PHYSICAL,
                rarity, min, max, Item.Passive.NONE, 0, 0, "Pohltí celkem až " + power
                + " poškození všech typů po zbroji. Platí ihned až do konce úvodních účinků příštího vlastního tahu. "
                + "Cena: " + cost + " výdrže. Zůstává; opakované použití v témže tahu stojí více výdrže. Štíty se sčítají."));
    }

    // ------------------------------------ vytvareni procentualni a prahove zbroje ------------------------------------

    private static void armor(List<Item> items, String name, int price, int percent,
            Damage.DamageType type, int penalty, Rarity rarity) {
        items.add(new Item(ItemType.ARMOR, name, price, 0, percent, 1, type, rarity, 1, 2,
                Item.Passive.NONE, 0, penalty, "Při vybavení sníží poškození typu " + type + " o " + percent
                + " %. Výsledek se zaokrouhlí na nejbližší celé číslo. Snížení základní výdrže: " + penalty
                + ". Platí i na jednotlivé dávky účinků. Lze nosit jednu zbroj."));
    }

    private static void thresholdArmor(List<Item> items, String name, int price, int power,
            int threshold, Item.Passive passive, int penalty) {
        String condition = passive == Item.Passive.SMALL_HIT_BLOCK ? "nejvýše " : "alespoň ";
        items.add(new Item(ItemType.ARMOR, name, price, 0, power, 1, Damage.DamageType.PHYSICAL,
                Rarity.UNCOMMON, 1, 2, passive, threshold, penalty, "Při vybavení odečte " + power
                + " z každého zásahu libovolného typu o síle " + condition + threshold
                + " před štítem; vždy projde alespoň polovina zásahu zaokrouhlená na nejbližší celé číslo. Snížení základní výdrže: " + penalty + ". Lze nosit jednu zbroj."));
    }

    // ------------------------------------ vytvareni talismanu ------------------------------------

    private static void trinket(List<Item> items, String name, int price, int power,
            Item.Passive passive, Rarity rarity, String description) {
        items.add(new Item(ItemType.TRINKET, name, price, 0, power, 1, Damage.DamageType.PHYSICAL,
                rarity, 1, 2, passive, 0, 0, description + " Účinkuje pouze vybavený. Lze nosit jeden talisman."));
    }
}
