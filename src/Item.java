public class Item {

    // druhy pasivnich efektu

    public enum Passive {
        NONE,
        REGENERATION,
        LOW_HEALTH_HEAL,
        STAMINA_BONUS,
        ATTACK_BONUS,
        CLEANSE_POISON
    }

    // nemenne vlastnosti jednoho itemu

    private final ItemType itemType;
    private final String name;
    private final int price;
    private final int staminaCost;
    private final int power;
    private final int duration;
    private final Damage.DamageType damageType;
    private final Rarity rarity;
    private final int minStock;
    private final int maxStock;
    private final Passive passive;

    // vytvoreni itemu a kontrola hodnot

    public Item(ItemType itemType, String name, int price, int staminaCost, int power,
            int duration, Damage.DamageType damageType, Rarity rarity, int minStock,
            int maxStock, Passive passive) {
        if (itemType == null || name == null || name.isBlank() || price < 0 || staminaCost < 0
                || power < 0 || duration < 1 || damageType == null || rarity == null
                || minStock < 1 || maxStock < minStock || passive == null) {
            throw new IllegalArgumentException("Neplatné vlastnosti předmětu.");
        }
        this.itemType = itemType;
        this.name = name;
        this.price = price;
        this.staminaCost = staminaCost;
        this.power = power;
        this.duration = duration;
        this.damageType = damageType;
        this.rarity = rarity;
        this.minStock = minStock;
        this.maxStock = maxStock;
        this.passive = passive;
    }

    // cteni typu jmena ceny a sily

    public ItemType getItemType() {
        return itemType;
    }

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public int getStaminaCost() {
        return staminaCost;
    }

    public int getRepeatCostIncrease() {
        return Math.max(1, (int) Math.round(staminaCost / 2.0));
    }

    public int getPower() {
        return power;
    } // u efektu s timerem jde o silu jedne davky

    // rozpoznani efektu s timerem a jeho celkova sila

    public boolean hasTimedEffect() {
        return itemType == ItemType.DAMAGE && damageType != Damage.DamageType.PHYSICAL
                || itemType == ItemType.HEAL && duration > 1;
    }

    // efekt drzi zbyvajici silu tick a cleanse ji postupne ubiraji
    public int getTotalPower() {
        return hasTimedEffect() ? Math.multiplyExact(power, duration) : power;
    }

    // cteni trvani rarity zasoby a pasivnich vlastnosti

    public int getDuration() {
        return duration;
    }

    public Damage.DamageType getDamageType() {
        return damageType;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public int getMinStock() {
        return minStock;
    }

    public int getMaxStock() {
        return maxStock;
    }

    public Passive getPassive() {
        return passive;
    }

    // rozliseni spotrebnich itemu a vybaveni

    public boolean isConsumable() {
        return itemType == ItemType.HEAL || itemType == ItemType.STAMINA;
    }

    public boolean isEquipment() {
        return itemType == ItemType.ARMOR || itemType == ItemType.TRINKET;
    }

    // kopie pro samostatny fyzicky kus

    public Item copy() {
        // kazdy koupeny kus ma vlastni identitu i kdyz pochazi ze stejne nabidky
        return new Item(itemType, name, price, staminaCost, power, duration, damageType,
                rarity, minStock, maxStock, passive);
    }

}
