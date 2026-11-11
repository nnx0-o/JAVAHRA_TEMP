public class Damage {

    // ------------------------------------ typy poskozeni ------------------------------------

    public enum DamageType {
        PHYSICAL("Fyzické"),
        FIRE("Oheň"),
        POISON("Jed"),
        HEAL("Léčení");

        private final String label;

        DamageType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    // ------------------------------------ inicializace ------------------------------------

    private final DamageType type;
    private int amount; // celkovy zbytek ucinku a ne poskozeni za jeden tah
    private int timer;

    public Damage(DamageType type, int amount) {
        this(type, amount, 1);
    }

    public Damage(DamageType type, int amount, int timer) {
        if (type == null || amount < 0 || timer < 1) {
            throw new IllegalArgumentException("Neplatný účinek nebo délka trvání.");
        }
        this.type = type;
        this.amount = amount;
        this.timer = timer;
    }

    // ------------------------------------ rozdeleni ucinku ------------------------------------

    public DamageType getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public int getTimer() {
        return timer;
    }

    public int nextAmount() {
        return timer == 0 ? 0 : (int) Math.round((double) amount / timer);
    }

    public int takeTick() {
        int result = nextAmount(); // zaokrouhlujeme zbytek aby se posledni cast neztratila
        if (timer > 0) {
            amount -= result;
            timer--;
        }
        return result;
    }

    public boolean tick() {
        if (timer == 0) return false;
        takeTick();
        return true;
    }

    public int reduceRemaining(int maximum) {
        int removed = Math.min(amount, Math.max(0, maximum));
        amount -= removed; // ocista ma omezeny rozpocet a nemeni trvani zbytku
        return removed;
    }

    public void mergeFire(Damage incoming) {
        if (type != DamageType.FIRE || incoming.type != DamageType.FIRE || incoming.timer == 0) {
            throw new IllegalArgumentException("Sloučit lze pouze trvající ohnivé účinky.");
        }
        amount = Math.addExact(amount, incoming.amount); // zachovame oba zbyvajici soucty
        timer = Math.max(timer, incoming.timer); // kratsi ucinek se rozlozi do delsiho
    }

    @Override
    public String toString() {
        return type + ": zbývá " + amount + " / " + timer + " tahů, příště " + nextAmount();
    }
}
