public class Item {

    // ------------------------------------ pasivni ucinky ------------------------------------

    public enum Passive {
        NONE,
        REGENERATION,
        LOW_HEALTH_HEAL,
        STAMINA_BONUS,
        ATTACK_BONUS,
        CLEANSE_POISON,
        CLEANSE_FIRE,
        CAPACITY,
        SMALL_HIT_BLOCK,
        LARGE_HIT_BLOCK
    }

    // ------------------------------------ inicializace ------------------------------------

    private final ItemType itemType;
    private final String name;
    private final int price;
    private final int staminaCost;
    private final int repeatCostIncrease;
    private final int power;
    private final int duration;
    private final Damage.DamageType damageType;
    private final Rarity rarity;
    private final int minStock;
    private final int maxStock;
    private final Passive passive;
    private final int threshold;
    private final int staminaPenalty;
    private final String description;

    public Item(ItemType itemType, String name) {
        this(itemType, name, 0, 0, 0, 1, Damage.DamageType.PHYSICAL,
                Rarity.COMMON, 1, 1, Passive.NONE, 0, 0, "Bez zvláštního účinku.");
    }

    public Item(ItemType itemType, String name, int price, int staminaCost, int power,
            int duration, Damage.DamageType damageType, Rarity rarity, int minStock,
            int maxStock, Passive passive, int threshold, int staminaPenalty, String description) {
        this(itemType, name, price, staminaCost, power, duration, damageType, rarity, minStock,
                maxStock, passive, threshold, staminaPenalty, description,
                Math.max(1, (int) Math.round(staminaCost / 2.0)));
    }

    public Item(ItemType itemType, String name, int price, int staminaCost, int power,
            int duration, Damage.DamageType damageType, Rarity rarity, int minStock,
            int maxStock, Passive passive, int threshold, int staminaPenalty, String description,
            int repeatCostIncrease) {
        if (repeatCostIncrease < 0) throw new IllegalArgumentException("Neplatné navýšení ceny.");
        if (itemType == null || name == null || name.isBlank() || price < 0 || staminaCost < 0
                || power < 0 || duration < 1 || damageType == null || rarity == null
                || minStock < 1 || maxStock < minStock || passive == null || threshold < 0
                || staminaPenalty < 0 || description == null) {
            throw new IllegalArgumentException("Neplatné vlastnosti předmětu.");
        }
        this.itemType = itemType;
        this.name = name;
        this.price = price;
        this.staminaCost = staminaCost;
        this.repeatCostIncrease = repeatCostIncrease;
        this.power = power;
        this.duration = duration;
        this.damageType = damageType;
        this.rarity = rarity;
        this.minStock = minStock;
        this.maxStock = maxStock;
        this.passive = passive;
        this.threshold = threshold;
        this.staminaPenalty = staminaPenalty;
        this.description = description;
    }

    // ------------------------------------ vlastnosti ------------------------------------

    public ItemType getItemType() { return itemType; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public int getStaminaCost() { return staminaCost; }
    public int getRepeatCostIncrease() { return repeatCostIncrease; }
    public int getPower() { return power; } // u casovaneho ucinku jde o silu jedne davky
    public boolean hasTimedEffect() {
        return itemType == ItemType.DAMAGE && damageType != Damage.DamageType.PHYSICAL
                || itemType == ItemType.HEAL && duration > 1;
    }

    // model poskozeni potrebuje celkovy soucet aby mohl slucovat a ocistovat zbytky
    public int getTotalPower() {
        return hasTimedEffect() ? Math.multiplyExact(power, duration) : power;
    }
    public int getDuration() { return duration; }
    public Damage.DamageType getDamageType() { return damageType; }
    public Rarity getRarity() { return rarity; }
    public int getMinStock() { return minStock; }
    public int getMaxStock() { return maxStock; }
    public Passive getPassive() { return passive; }
    public int getThreshold() { return threshold; }
    public int getStaminaPenalty() { return staminaPenalty; }
    public String getDescription() { return description; }

    public boolean isConsumable() {
        return itemType == ItemType.HEAL || itemType == ItemType.STAMINA;
    }

    public boolean isEquipment() {
        return itemType == ItemType.ARMOR || itemType == ItemType.TRINKET;
    }

    public Item copy() {
        // kazdy koupeny kus ma vlastni identitu i kdyz pochazi ze stejne nabidky
        return new Item(itemType, name, price, staminaCost, power, duration, damageType,
                rarity, minStock, maxStock, passive, threshold, staminaPenalty, description, repeatCostIncrease);
    }

    @Override
    public String toString() {
        return name + " (" + itemType + ", " + rarity + ")";
    }
}
