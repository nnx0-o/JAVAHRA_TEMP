public class DamageTest {

    public static void main(String[] args) {
        Damage fire = new Damage(Damage.DamageType.FIRE, 5, 3);
        fire.mergeFire(new Damage(Damage.DamageType.FIRE, 10, 4));
        check(fire.getAmount() == 15 && fire.getTimer() == 4, "fire conserves total");
        int[] expected = {4, 4, 4, 3};
        for (int amount : expected) check(fire.takeTick() == amount, "nearest integer distribution");
        check(fire.takeTick() == 0 && !fire.tick(), "expired effect has no damage");

        Damage existing = new Damage(Damage.DamageType.FIRE, 9, 3);
        check(existing.takeTick() == 3, "first tick");
        existing.mergeFire(new Damage(Damage.DamageType.FIRE, 5, 4));
        check(existing.getAmount() == 11 && existing.getTimer() == 4, "merge remaining damage only");
        int total = 0;
        while (existing.getTimer() > 0) total += existing.takeTick();
        check(total == 11, "no repeated damage after merge");

        Damage half = new Damage(Damage.DamageType.POISON, 1, 2);
        check(half.takeTick() == 1 && half.takeTick() == 0, "half rounds up");
        System.out.println("DamageTest: OK");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
