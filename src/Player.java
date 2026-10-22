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

    public void obchod(int number) {
        if (this.gold <= 0) {
            System.out.println("Nemáš dostatek peněz, aby sis mohl něco koupit");
        }
        else {
            Item NewItem;
            switch (number) {
                case 1:
                    NewItem = new Item (ItemType.SWORD, "meč");
                    this.gold -= 3;
                    break;
                case 2:
                    NewItem = new Item (ItemType.POTION, "lektvar");
                    this.gold -= 2;
                    break;
                case 3:
                    NewItem = new Item (ItemType.SHIELD, "štít");
                    this.gold -= 2;
                    break;
                case 4:
                    NewItem = new Item (ItemType.ARMOR, "brnění");
                    this.gold -= 4;
                    break;
                case 5:
                    NewItem = new Item (ItemType.BOW, "luk");
                    this.gold -= 2;
                    break;
                default:
                    System.out.println("Vyberte pouze povolená čísla");
                    break;
            }
        }
    }
    /* 
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
    */

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
