public class Damage {

    // jaky efekt to je

    public enum DamageType {
        PHYSICAL, POISON, HEAL
    }

    // kolik jeste zbyva a na jak dlouho

    private final DamageType type;
    private int amount; // tohle je cely zbytek ne jedna davka
    private int timer;

    // novy efekt

    public Damage(DamageType type, int amount, int timer) {
        if (type == null || amount < 0 || timer < 1) {
            throw new IllegalArgumentException("Neplatný účinek nebo délka trvání.");
        }
        this.type = type;
        this.amount = amount;
        this.timer = timer;
    }

    // co z efektu jeste zbyva

    public DamageType getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public int getTimer() {
        return timer;
    }

    // ukousneme jednu davku

    public int takeTick() {
        int result = timer == 0 ? 0 : (int) Math.round((double) amount / timer);
        if (timer > 0) {
            amount -= result;
            timer--;
        }
        return result;
    }

    // tady ubirame jed pri ociste

    public int reduceRemaining(int maximum) {
        int removed = Math.min(amount, Math.max(0, maximum));
        amount -= removed; // ubereme jen co ocista zvladne delka zustava
        return removed;
    }

}
