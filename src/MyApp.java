
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MyApp {
    public static void main(String[] args) {
        MiniTui tui = new MiniTui();
        Shop shop = new Shop();
        List<String> submittedPasses = new ArrayList<>();

        // ====================================================================
        // LAYER 0: REAL-TIME MULTI-PASS DASHBOARD (Z = 0)
        // ====================================================================
        tui.add(new MiniTui.Title(2, 2, 0, "REAL-TIME MULTI-PASS DASHBOARD", "\033[1;36m"));

        MiniTui.Label clockLabel   = tui.add(new MiniTui.Label(4, 4, 0, "Clock: --:--:--"));
        MiniTui.Label mainGoldLbl  = tui.add(new MiniTui.Label(28, 4, 0, "Gold: 100g (+1/s)"));
        MiniTui.Label previewLabel = tui.add(new MiniTui.Label(52, 4, 0, "Preview: (empty)"));

        // Command Input: spans x=4 to x=39 on row y=6
        MiniTui.Input commandInput = tui.add(new MiniTui.Input(4, 6, 0, "Command", "", 22))
            .onChange(text -> previewLabel.setText("Preview: " + text.toUpperCase()))
            .onSubmit(text -> tui.setStatus("Locked in '" + text + "'. Press Right Arrow -> Enter to Send!"));

        // Send Button placed safely at x=42 on row y=6 (zero overlap!)
        tui.add(new MiniTui.Button(42, 6, 0, "Send / Next Pass", () -> {
            String entry = commandInput.getValue().trim();
            if (!entry.isEmpty()) {
                submittedPasses.add(entry);
                shop.rewardForSubmittedPass();
                commandInput.setValue("");
                previewLabel.setText("Preview: (empty)");
                tui.setStatus("Pass #" + submittedPasses.size() + " saved (+10g bonus)!");
            } else {
                tui.setStatus("Command is empty! Select Command and press Enter to type.");
            }
        }));

        // Multi-pass log history & Inventory summary on main screen
        tui.add(new MiniTui.Label(4, 9, 0, "--- Submitted Passes ---"));
        MiniTui.Label log1 = tui.add(new MiniTui.Label(4, 10, 0, "1. -"));
        MiniTui.Label log2 = tui.add(new MiniTui.Label(4, 11, 0, "2. -"));
        MiniTui.Label log3 = tui.add(new MiniTui.Label(4, 12, 0, "3. -"));

        tui.add(new MiniTui.Label(42, 9, 0, "--- Equipped Shop Gear ---"));
        MiniTui.Label gear1 = tui.add(new MiniTui.Label(42, 10, 0, "* Starter Boots"));
        MiniTui.Label gear2 = tui.add(new MiniTui.Label(42, 11, 0, "* -"));
        MiniTui.Label gear3 = tui.add(new MiniTui.Label(42, 12, 0, "* -"));

        // Bottom Navigation Bar on Layer 0
        tui.add(new MiniTui.Button(4, 15, 0, ">>> Open Merchant Shop (Layer 1) >>>", () -> {
            tui.setLayer(1);
            tui.setStatus("Entered the Shop! Everything outside is hidden.");
        }));

        tui.add(new MiniTui.Button(48, 15, 0, "Finish & Exit", () -> tui.stop()));


        // ====================================================================
        // LAYER 1: COMPLETELY SEPARATE SHOP UI (Z = 1)
        // ====================================================================
        tui.add(new MiniTui.Title(2, 2, 1, "$$$ MERCHANT BAZAAR & UPGRADE SHOP $$$", "\033[1;33m"));

        // Left & Right Framed Panels
        tui.add(new MiniTui.Box(2, 4, 1, 44, 11, "ITEMS FOR SALE", "\033[33m"));
        MiniTui.Box walletBox = tui.add(new MiniTui.Box(48, 4, 1, 28, 11, "WALLET & BAG", "\033[32m"));

        MiniTui.Label shopGoldLbl = tui.add(new MiniTui.Label(50, 6, 1, "Gold: 100g"));
        MiniTui.Label shopRateLbl = tui.add(new MiniTui.Label(50, 7, 1, "Income: +1g/sec"));
        MiniTui.Label shopBagLbl  = tui.add(new MiniTui.Label(50, 9, 1, "Items Owned: 1"));
        MiniTui.Label shopLastLbl = tui.add(new MiniTui.Label(50, 10, 1, "Latest: Starter Boots"));

        // Shop Buy Buttons inside the left box
        tui.add(new MiniTui.Button(4, 6, 1, "Buy Health Potion (25g)", () -> {
            if (shop.buyHealthPotion()) {
                tui.setStatus("Bought Health Potion!");
            } else {
                tui.setStatus("Need 25g for Health Potion!");
            }
        }));

        tui.add(new MiniTui.Button(4, 8, 1, "Buy Knight Sword (80g)", () -> {
            if (shop.buyKnightSword()) {
                tui.setStatus("Bought Knight Sword!");
            } else {
                tui.setStatus("Need 80g for Knight Sword!");
            }
        }));

        tui.add(new MiniTui.Button(4, 10, 1, "Hire Miner (+5g/s) (120g)", () -> {
            if (shop.hireMiner()) {
                tui.setStatus("Hired Miner! Income is now +" + shop.getGoldPerSecond() + "g/s!");
            } else {
                tui.setStatus("Need 120g to hire a Miner!");
            }
        }));

        // Promo code input inside the shop (x=4 to x=30) + Redeem button at x=32
        MiniTui.Input promoInput = tui.add(new MiniTui.Input(4, 12, 1, "Coupon", "", 10));
        tui.add(new MiniTui.Button(29, 12, 1, "Redeem", () -> {
            if (shop.redeemCoupon(promoInput.getValue())) {
                promoInput.setValue("");
                tui.setStatus("Redeemed code 'RICH' for +300g!");
            } else {
                tui.setStatus("Invalid coupon! (Try typing: RICH)");
            }
        }));

        // Return to Main Dashboard Button
        tui.add(new MiniTui.Button(2, 16, 1, "<<< Leave Shop (Back to Dashboard)", () -> {
            tui.setLayer(0);
            tui.setStatus("Returned to the Main Dashboard.");
        }));


        // ====================================================================
        // REAL-TIME TICK LOOP (20 FPS)
        // ====================================================================
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss.S");
        final int[] ticks = {0};

        tui.onTick(() -> {
            ticks[0]++;
            if (ticks[0] % 20 == 0) {
                shop.collectIncome();
            }

            // Update Layer 0 (Dashboard)
            clockLabel.setText("Clock: " + LocalTime.now().format(fmt));
            mainGoldLbl.setText("Gold: " + shop.getGold() + "g (+" + shop.getGoldPerSecond() + "/s)");

            int pSize = submittedPasses.size();
            if (pSize >= 1) log1.setText("Latest: " + submittedPasses.get(pSize - 1));
            if (pSize >= 2) log2.setText("Prev 1: " + submittedPasses.get(pSize - 2));
            if (pSize >= 3) log3.setText("Prev 2: " + submittedPasses.get(pSize - 3));

            List<String> inventory = shop.getInventory();
            int iSize = inventory.size();
            if (iSize >= 1) gear1.setText("* " + inventory.get(iSize - 1));
            if (iSize >= 2) gear2.setText("* " + inventory.get(iSize - 2));
            if (iSize >= 3) gear3.setText("* " + inventory.get(iSize - 3));

            // Update Layer 1 (Shop)
            shopGoldLbl.setText("Gold: " + shop.getGold() + "g");
            shopRateLbl.setText("Income: +" + shop.getGoldPerSecond() + "g/sec");
            shopBagLbl.setText("Items Owned: " + iSize);
            shopLastLbl.setText("Latest: " + shop.getLatestItem());

            // Responsive width for the right-side Shop box
            walletBox.width = Math.max(24, tui.getTerminalWidth() - 50);
        });

        tui.start();

        System.out.println("Exited TUI! Submitted passes: " + submittedPasses);
        System.out.println("Final Inventory: " + shop.getInventory());
    }
}
