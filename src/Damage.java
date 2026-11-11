public class Damage {

    // ------------------------------------ typy poskozeni ------------------------------------

    public enum DamageType {
        PHYSICAL,
        FIRE,
        POISON,
        HEAL
    }

    // ------------------------------------ inicializace ------------------------------------

    private DamageType type;
    private int amount;
    private int timer;

    public Damage(DamageType type, int amount) {
        this.type = type;
        this.amount = amount;
        this.timer = 1;
    }

    public Damage(DamageType type, int amount, int timer) {
        this.type = type;
        this.amount = amount;
        this.timer = timer;
    }

    public DamageType getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public int getTimer() {
        return timer;
    }

    public boolean tick() {
        if (timer > 0) {
            timer--;
            return true;
        }
        return false;
    }
}
