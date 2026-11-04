// /**
//  * Original snapshot of the 1v1 duel coordinator.
//  */
// public class Game {
//
//     private final Player firstPlayer;
//     private final Player secondPlayer;
//
//     public Game(String firstPlayerName, String secondPlayerName) {
//         this.firstPlayer = new Player(firstPlayerName);
//         this.secondPlayer = new Player(secondPlayerName);
//     }
//
//     public void start() {
//         giveStartingItems();
//         printPlayer(firstPlayer);
//         printPlayer(secondPlayer);
//
//         playDuel(firstPlayer, secondPlayer);
//
//         System.out.println("\nPo souboji:");
//         printPlayer(firstPlayer);
//         printPlayer(secondPlayer);
//     }
//
//     private void giveStartingItems() {
//         firstPlayer.addToInventory(ItemType.SWORD, "Zakladni mec");
//         secondPlayer.addToInventory(ItemType.SHIELD, "Zakladni stit");
//     }
//
//     private void playDuel(Player challenger, Player opponent) {
//         System.out.println("\n" + challenger.getName() + " vyzyva " + opponent.getName() + " na souboj.");
//         challenger.challengeToDuel(opponent);
//     }
//
//     private void printPlayer(Player player) {
//         System.out.println("---------------------");
//         System.out.println(player.getName());
//         System.out.println("Zdravi: " + player.getHealth());
//         System.out.println("Zlato: " + player.getGold());
//         System.out.println("Inventar: " + player.getInventory());
//         System.out.println("---------------------");
//     }
//
//     public static void main(String[] args) {
//         new Game("Pepa", "Mahulena").start();
//     }
// }
