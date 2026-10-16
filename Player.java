public class Player {
    private final String name;
    private int health;
    private int gold;
    private final ItemCollection<Item> inventory;
    
    public Player(String name) {
        this.name = name;
        this.health = 4;
        this.gold = 10;
        this.inventory = new ItemCollection<>(5);
    }

    public boolean addToInventory(ItemType itemType, String itemName) {
        if (this.gold < 3) return false;
        this.gold -= 3;
        return inventory.addItem(new Item(itemType, itemName));
    }

    public void challengeToDuel(Player opponent) {
        int myRoll = this.rollDie();
        int opponentRoll = opponent.rollDie();
        if (myRoll == opponentRoll) return;
        if (myRoll > opponentRoll) {
            this.gold++;
            opponent.health--;
        }
        else {
            this.health--;
            opponent.gold++;
        }

    }

    private int rollDie() {
        return (int)(Math.random()*6 + 1);
    }

    public String getName() {
        return name;
    }

    public int getHealth() {
        return health;
    }

    public int getGold() {
        return gold;
    }

    public ItemCollection<Item> getInventory() {
        return inventory;
    }

    @Override
    public String toString() {
        return "Hrac [name=" + name + ", zivoty=" + health + ", zlataky=" + gold + ", inventar=" + inventory + "]";
    }  
    
}
