class Main {
    public static void main(String[] args) {

        Player pepa = new Player("Pepa");
        System.out.println(pepa.addToInventory(ItemType.POTION, "sily") ? "pridano" : "nepridano");
        System.out.println(pepa);

        System.out.println(pepa.addToInventory(ItemType.BOW, "rychly") ? "pridano" : "nepridano");
        System.out.println(pepa.addToInventory(ItemType.SWORD, "kouzelny") ? "pridano" : "nepridano");
        System.out.println(pepa.addToInventory(ItemType.SHIELD, "pevny") ? "pridano" : "nepridano");
        
        System.out.println(pepa);

        Player mahulena = new Player("Mahulena");
        System.out.println(mahulena.addToInventory(ItemType.SHIELD, "pevny") ? "pridano" : "nepridano");
        System.out.println(mahulena);

        pepa.challengeToDuel(mahulena);
        System.out.println(pepa);
        System.out.println(mahulena);

        mahulena.challengeToDuel(pepa);
        System.out.println(pepa);
        System.out.println(mahulena);
    }
}
