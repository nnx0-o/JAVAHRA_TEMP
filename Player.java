public class Player {
    private final String name;
    private int health;
    private int gold;
    private final Array<Item> inventory;
    
    public Player(String name) {
        this.name = name;
        this.health = 4;
        this.gold = 10;
        this.inventory = new Array<>(5);
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

    @Override
    public String toString() {
        return "Hrac [name=" + name + ", zivoty=" + health + ", zlataky=" + gold + ", inventar=" + inventory + "]";
    }  
    
}
