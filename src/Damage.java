public class Damage {

    public enum DamageType {
        PHYSICAL, POISON, HEAL
    }

    private final DamageType type;
    private int amount; // tohle je cely zbytek ne jedna davka
    private int timer;

    public Damage(DamageType type, int amount, int timer) {
        if (type == null || amount < 0 || timer < 1) {
            throw new IllegalArgumentException("Neplatný účinek nebo délka trvání.");
        }
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

    public int takeTick() {
        // davku pocitame ze zbytku takze ocista zmensi i dalsi tiky
        // posledni tah vezme cely zbytek i kdyz se predtim zaokrouhlovalo
        int result = timer == 0 ? 0 : (int) Math.round((double) amount / timer);
        if (timer > 0) {
            amount -= result;
            timer--;
        }
        return result;
    }

    public int reduceRemaining(int maximum) {
        int removed = Math.min(amount, Math.max(0, maximum));
        amount -= removed; // ubereme jen co ocista zvladne delka zustava
        return removed;
    }

}
