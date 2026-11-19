public class Damage {

    // ------------------------------------ typ ucinku a jeho cesky nazev ------------------------------------

    public enum DamageType {
        PHYSICAL, POISON, HEAL
    }

    // ------------------------------------ zbyvajici sila a pocet tahu ------------------------------------

    private final DamageType type;
    private int amount; // celkovy zbytek ucinku a ne poskozeni za jeden tah
    private int timer;

    // ------------------------------------ vytvoreni a kontrola ucinku ------------------------------------

    public Damage(DamageType type, int amount, int timer) {
        if (type == null || amount < 0 || timer < 1) {
            throw new IllegalArgumentException("Neplatný účinek nebo délka trvání.");
        }
        this.type = type;
        this.amount = amount;
        this.timer = timer;
    }

    // ------------------------------------ cteni aktualniho stavu ucinku ------------------------------------

    public DamageType getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public int getTimer() {
        return timer;
    }

    // ------------------------------------ vypocet a spotrebovani jedne davky ------------------------------------

    public int takeTick() {
        int result = timer == 0 ? 0 : (int) Math.round((double) amount / timer);
        if (timer > 0) {
            amount -= result;
            timer--;
        }
        return result;
    }

    // ------------------------------------ odebrani casti zbyleho poskozeni pri ociste ------------------------------------

    public int reduceRemaining(int maximum) {
        int removed = Math.min(amount, Math.max(0, maximum));
        amount -= removed; // ocista ma omezeny rozpocet a nemeni trvani zbytku
        return removed;
    }

}
