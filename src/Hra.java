class Hra {
    public static void main(String[] args) {

        Player pepa = new Player("Pepa");
        //Vypsání pepy
        System.out.println("---------------------");
        System.out.println("Pepa");
        System.out.println("Jméno: " + pepa.getName());
        System.out.println("Health: " + pepa.getHealth());
        System.out.println("Gold: " + pepa.getGold());
        System.out.print("Inventář: " + pepa.getInventory());
        System.out.println("---------------------");
        //System.out.println(pepa);

        /*
        System.out.println(pepa.addToInventory(ItemType.BOW, "rychly") ? "pridano" : "nepridano");
        System.out.println(pepa.addToInventory(ItemType.SWORD, "kouzelny") ? "pridano" : "nepridano");
        System.out.println(pepa.addToInventory(ItemType.SHIELD, "pevny") ? "pridano" : "nepridano");

        System.out.println(pepa);
        */
       System.out.println("");
       //Vypsání mahuleny
        Player mahulena = new Player("Mahulena");
        //System.out.println(mahulena.addToInventory(ItemType.SHIELD, "pevny") ? "pridano" : "nepridano");
        System.out.println("---------------------");
        System.out.println("Mahulena");
        System.out.println("Jméno: " + mahulena.getName());
        System.out.println("Health: " + mahulena.getHealth());
        System.out.println("Gold: " + mahulena.getGold());
        System.out.print("Inventář: " + mahulena.getInventory());
        System.out.println("---------------------");

        /*
        pepa.challengeToDuel(mahulena);
        System.out.println(pepa);
        System.out.println(mahulena);

        mahulena.challengeToDuel(pepa);
        System.out.println(pepa);
        System.out.println(mahulena);
        */
    }
}
