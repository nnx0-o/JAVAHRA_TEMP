// Sklada ceske popisy z aktualnich hodnot predmetu a ucinku.
public final class GameText {

    // ------------------------------------ nazvy typu poskozeni ------------------------------------

    public String damageType(Damage.DamageType type) {
        return switch (type) {
            case PHYSICAL -> "Fyzické";
            case POISON -> "Jed";
            case HEAL -> "Léčení";
        };
    }

    // ------------------------------------ kratky popis predmetu v tabulce ------------------------------------

    public String summary(Item item) {
        int power = item.getPower();
        return switch (item.getItemType()) {
            case DAMAGE -> item.getDamageType() == Damage.DamageType.PHYSICAL
                    ? power + " fyzické"
                    : damageType(item.getDamageType()) + " " + timed(power, item.getDuration());
            case HEAL -> "Léčení " + (item.getDuration() == 1 ? power : timed(power, item.getDuration()));
            case STAMINA -> "+" + power + " výdrže příští tah";
            case DEFEND -> "Pohltí " + power + " poškození";
            case ARMOR -> "−" + power + "% " + damageType(item.getDamageType());
            case TRINKET -> switch (item.getPassive()) {
                case REGENERATION -> "Životy +" + power + " na začátku tahu";
                case LOW_HEALTH_HEAL -> "Životy +" + power + " pod polovinou zdraví";
                case STAMINA_BONUS -> "Výdrž +" + power + " na začátku tahu";
                case ATTACK_BONUS -> "+" + power + " k fyzickým útokům";
                case CLEANSE_POISON -> "Očista jedu: " + power + " za tah";
                case NONE -> "Bez zvláštního účinku";
            };
        };
    }

    // ------------------------------------ stejny prehled vlastnosti pro kazdy predmet ------------------------------------

    public String description(Item item) {
        String timing = switch (item.getItemType()) {
            case DAMAGE -> item.hasTimedEffect() ? "Začátek soupeřova tahu." : "Při provedení akce.";
            case HEAL -> item.hasTimedEffect() ? "Od začátku příštího vlastního tahu." : "Při provedení akce.";
            case STAMINA -> "Začátek příštího vlastního tahu.";
            case DEFEND -> "Od provedení akce do konce úvodních účinků příštího vlastního tahu.";
            case ARMOR -> "Každý odpovídající zásah, pokud je zbroj vybavená.";
            case TRINKET -> item.getPassive() == Item.Passive.ATTACK_BONUS
                    ? "Každý fyzický útok před obranou cíle."
                    : "Začátek vlastního tahu před časovanými účinky.";
        };
        String result = "Účinek: " + summary(item) + "\nKdy: " + timing;
        if (item.isEquipment()) return result + "\nPoužití: pasivní, pouze při vybavení.";
        result += "\nVýdrž: " + item.getStaminaCost();
        if (item.isConsumable()) return result + "\nPoužití: spotřebuje se při provedení akce.";
        result += "\nOpakování v tahu: +" + item.getRepeatCostIncrease() + " výdrže.";
        return result + (item == ItemCatalog.FIST
                ? "\nPoužití: trvalá výbava, nelze prodat. Nezabírá místo pro nákup."
                : "\nPoužití: opakovaně, zůstává v inventáři.");
    }

    // ------------------------------------ zbytek ucinku bez simulace budoucich davek ------------------------------------

    public String effect(Damage effect) {
        return damageType(effect.getType()) + ": " + effect.getAmount()
                + " zbývá / " + effect.getTimer() + " tahů";
    }

    // ------------------------------------ sila jedne davky, pocet tahu a celkova sila ------------------------------------

    private String timed(int power, int turns) {
        return power + " × " + turns + " (" + Math.multiplyExact(power, turns) + ")";
    }
}
