class Hra {		// starsi ukazka vypisu hracu
    public static void main(String[] args) {

        Player pepa = new Player("Pepa");
        System.out.println("---------------------");
        System.out.println("Pepa");
        System.out.println("Jméno: " + pepa.getName());
        System.out.println("Health: " + pepa.getHealth());
        System.out.println("Gold: " + pepa.getGold());
        System.out.print("Inventář: " + pepa.getInventory());
        System.out.println("---------------------");

        System.out.println("");
        Player mahulena = new Player("Mahulena");
        System.out.println("---------------------");
        System.out.println("Mahulena");
        System.out.println("Jméno: " + mahulena.getName());
        System.out.println("Health: " + mahulena.getHealth());
        System.out.println("Gold: " + mahulena.getGold());
        System.out.print("Inventář: " + mahulena.getInventory());
        System.out.println("---------------------");

    }
}
