import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game {

    public enum Phase { ANNOUNCEMENT, SHOP, COMBAT, RESULT, FINISHED }

    // ------------------------------------ stav zapasu ------------------------------------

    private final Player[] players;
    private final int[] wins = new int[2];
    private final Shop shop = new Shop();
    private final Random random;
    private final List<String> log = new ArrayList<>();
    private Phase phase = Phase.ANNOUNCEMENT;
    private int shopStarter;
    private int current;
    private int battle = 1;
    private int turn;
    private int unchangedVisits;
    private boolean traded;
    private int winner = -1;

    public Game(String firstName, String secondName, Random random) {
        this.random = random;
        players = new Player[]{new Player(firstName), new Player(secondName)};
        shopStarter = random.nextInt(2); // losujeme jednou a dalsi souboje poradi otaceji
        current = shopStarter;
    }

    public Player getPlayer(int index) { return players[index]; }
    public Player getCurrentPlayer() { return players[current]; }
    public Player getOpponent() { return players[1 - current]; }
    public int getCurrentIndex() { return current; }
    public int getShopStarter() { return shopStarter; }
    public int getCombatStarter() { return 1 - shopStarter; }
    public int getBattle() { return battle; }
    public int getTurn() { return turn; }
    public int getWins(int index) { return wins[index]; }
    public int getWinner() { return winner; }
    public Shop getShop() { return shop; }
    public Phase getPhase() { return phase; }
    public List<String> getLog() { return List.copyOf(log); }
    public boolean isPreparation() { return phase == Phase.COMBAT && turn < GameRules.PREPARATION_TURNS; }
    public int getUnchangedVisits() { return unchangedVisits; }

    private void report(String message) { log.add(message); }

    // ------------------------------------ obchod ------------------------------------

    public void openShop() {
        if (phase != Phase.ANNOUNCEMENT) return;
        shop.refresh(random);
        current = shopStarter;
        unchangedVisits = 0;
        traded = false;
        phase = Phase.SHOP;
        report("Začíná obchod před soubojem " + battle + ".");
    }

    public String buy(Item item) {
        if (phase != Phase.SHOP) return "Nakupovat lze pouze v obchodě.";
        if (!shop.purchase(getCurrentPlayer(), item)) return "Nákup se nezdařil: chybí zlato, místo nebo zásoba.";
        traded = true; // uspesny obchod ovlivni az uzavreni navstevy a nerusi nakoupene veci
        String message = getCurrentPlayer().getName() + " kupuje: " + item.getName() + ".";
        report(message);
        return message;
    }

    public String sell(Item item) {
        if (phase != Phase.SHOP) return "Prodávat lze pouze v obchodě.";
        if (!shop.sell(getCurrentPlayer(), item)) return "Prodej se nezdařil. Po odebrání brašny se musí vše vejít.";
        traded = true;
        String message = getCurrentPlayer().getName() + " prodává: " + item.getName() + " za " + item.getPrice() / 2 + " zlata.";
        report(message);
        return message;
    }

    public String equip(Item item) {
        if (phase != Phase.SHOP || item == null) return "Vybavení lze měnit pouze v obchodě.";
        Inventory inventory = getCurrentPlayer().getEquipment();
        Item next = inventory.isEquipped(item) ? null : item;
        boolean success = item.getItemType() == ItemType.ARMOR ? inventory.equipArmor(next)
                : item.getItemType() == ItemType.TRINKET && inventory.equipTrinket(next);
        if (!success) return "Vybavení nelze změnit. Zkontroluj typ předmětu a kapacitu inventáře.";
        return next == null ? "Předmět byl sundán." : "Vybaveno: " + item.getName() + ".";
    }

    public void continueShop() {
        if (phase != Phase.SHOP) return;
        unchangedVisits = traded ? 0 : unchangedVisits + 1;
        traded = false;
        if (unchangedVisits >= 2) startBattle();
        else current = 1 - current;
    }

    // ------------------------------------ tahy a pasivni ucinky ------------------------------------

    private void startBattle() {
        for (Player player : players) player.resetBattle();
        phase = Phase.COMBAT;
        turn = 0;
        current = getCombatStarter();
        report("Souboj začíná. Každý má jeden přípravný tah bez útoků.");
        beginTurn();
    }

    private void beginTurn() {
        Player actor = getCurrentPlayer();
        actor.beginTurn();
        report("Tah " + (turn + 1) + ": " + actor.getName() + (isPreparation() ? " — příprava." : "."));
        Item trinket = actor.getEquipment().getEquippedTrinket();
        if (trinket != null) {
            int before = actor.getHealth();
            switch (trinket.getPassive()) {
                case REGENERATION -> actor.heal(trinket.getPower());
                case LOW_HEALTH_HEAL -> {
                    if (actor.getHealth() * 2 < Player.MAX_HEALTH) actor.heal(trinket.getPower());
                }
                case STAMINA_BONUS -> actor.addStamina(trinket.getPower());
                case CLEANSE_POISON -> actor.cleanse(Damage.DamageType.POISON, trinket.getPower());
                case CLEANSE_FIRE -> actor.cleanse(Damage.DamageType.FIRE, trinket.getPower());
                default -> { }
            }
            if (actor.getHealth() > before) report(trinket.getName() + " obnovuje " + (actor.getHealth() - before) + " životů.");
        }
        for (Damage effect : actor.getEffects()) {
            int amount = effect.takeTick(); // snimek zachova poradi i pri odstranovani vyprselych ucinku
            if (effect.getType() == Damage.DamageType.HEAL) {
                int before = actor.getHealth();
                actor.heal(amount);
                report("Postupné léčení: +" + (actor.getHealth() - before) + " životů.");
            } else {
                int actual = actor.receiveDamage(amount, effect.getType());
                report(effect.getType() + ": " + actual + " poškození po obraně (před obranou " + amount + ").");
            }
            if (effect.getTimer() == 0) actor.removeEffect(effect);
            if (finishIfDefeated()) return; // pozdejsi leceni uz mrtveho hrace nezachrani
        }
        actor.clearShield(); // stary stit chrani i uvodni davky a potom zanikne
    }

    public void endTurn() {
        if (phase != Phase.COMBAT) return;
        turn++;
        current = 1 - current;
        beginTurn();
    }

    // ------------------------------------ aktivni akce ------------------------------------

    public String act(Player.Action action) {
        if (phase != Phase.COMBAT) return "Souboj nyní neprobíhá.";
        if (action == null) return "Vyber akci.";
        Player actor = getCurrentPlayer();
        Player target = getOpponent();
        Item item = action.item();
        boolean attack = action.basicAttack() || item != null && item.getItemType() == ItemType.DAMAGE;
        if (attack && isPreparation()) return "Přípravný tah: zatím nelze zasáhnout soupeře.";
        if (action.basicAttack()) {
            if (actor.hasUsedBasicAttack()) return "Základní útok už byl v tomto tahu použit.";
            int actual = target.receiveDamage(GameRules.BASIC_DAMAGE + attackBonus(actor), Damage.DamageType.PHYSICAL);
            report(actor.getName() + " používá základní útok za " + actual + " poškození.");
        } else {
            if (item == null || !actor.getInventory().contains(item)) return "Tento předmět nemáš v inventáři.";
            if (item.isEquipment()) return "Zbroj a talisman působí pasivně; mění se v obchodě.";
            if (actor.hasUsed(item)) return "Tento kus už byl v tomto tahu použit.";
            if (!actor.spendStamina(item.getStaminaCost())) return "Nemáš dost výdrže.";
            switch (item.getItemType()) {
                case DAMAGE -> {
                    if (item.getDamageType() == Damage.DamageType.PHYSICAL) {
                        int actual = target.receiveDamage(item.getPower() + attackBonus(actor), item.getDamageType());
                        report(item.getName() + ": " + actual + " poškození po obraně.");
                    } else {
                        target.addEffect(new Damage(item.getDamageType(), item.getPower(), item.getDuration()));
                        report(item.getName() + ": " + item.getPower() + " poškození během " + item.getDuration() + " tahů soupeře.");
                    }
                }
                case HEAL -> {
                    if (item.getDuration() == 1) {
                        int before = actor.getHealth();
                        actor.heal(item.getPower());
                        report(item.getName() + ": obnoveno " + (actor.getHealth() - before) + " životů.");
                    } else {
                        actor.addEffect(new Damage(Damage.DamageType.HEAL, item.getPower(), item.getDuration()));
                        report(item.getName() + ": léčení začne příští vlastní tah.");
                    }
                }
                case DEFEND -> {
                    actor.addShield(item.getPower());
                    report(item.getName() + ": štít posílen o " + item.getPower() + ".");
                }
                case STAMINA -> {
                    actor.addNextStamina(item.getPower());
                    report(item.getName() + ": příští vlastní tah +" + item.getPower() + " výdrže.");
                }
                default -> throw new IllegalStateException("Neznámá aktivní akce.");
            }
            if (item.isConsumable()) actor.removeFromInventory(item);
        }
        actor.recordAction(action); // akce probiha ihned a konec tahu ji uz neprovadi znovu
        String result = log.get(log.size() - 1);
        finishIfDefeated();
        return result;
    }

    private int attackBonus(Player player) {
        Item trinket = player.getEquipment().getEquippedTrinket();
        return trinket != null && trinket.getPassive() == Item.Passive.ATTACK_BONUS ? trinket.getPower() : 0;
    }

    // ------------------------------------ vysledek ------------------------------------

    private boolean finishIfDefeated() {
        if (players[0].isAlive() && players[1].isAlive()) return false;
        if (phase != Phase.COMBAT) return true;
        winner = players[0].isAlive() ? 0 : 1;
        wins[winner]++;
        players[winner].changeGold(GameRules.WIN_REWARD);
        players[1 - winner].changeGold(GameRules.LOSS_REWARD);
        phase = wins[winner] >= GameRules.WINS_REQUIRED ? Phase.FINISHED : Phase.RESULT;
        report("Vítězí " + players[winner].getName() + ". Odměna vítězi " + GameRules.WIN_REWARD
                + ", poraženému " + GameRules.LOSS_REWARD + " zlata.");
        return true;
    }

    public void nextBattle() {
        if (phase != Phase.RESULT) return;
        battle++;
        shopStarter = 1 - shopStarter;
        current = shopStarter;
        for (Player player : players) player.resetBattle();
        phase = Phase.ANNOUNCEMENT;
    }
}
