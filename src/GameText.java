import java.util.ArrayList;
import java.util.List;

// Sklada ceske popisy z aktualnich hodnot modelu, aby cisla v textu odpovidala hre.
public final class GameText {

    // ------------------------------------ ceske nazvy predmetu a typu ------------------------------------

    public String name(Item item) {
        if (item == null) return "Pěst";
        return item.getName();
    }

    public String type(ItemType type) {
        return switch (type) {
            case DAMAGE -> "Zbraň";
            case HEAL -> "Léčení";
            case DEFEND -> "Štít";
            case STAMINA -> "Výdrž";
            case ARMOR -> "Zbroj";
            case TRINKET -> "Talisman";
        };
    }

    public String damageType(Damage.DamageType type) {
        return switch (type) {
            case PHYSICAL -> "Fyzické";
            case FIRE -> "Oheň";
            case POISON -> "Jed";
            case HEAL -> "Léčení";
        };
    }

    // ------------------------------------ kratky popis predmetu v tabulce ------------------------------------

    public String summary(Item item) {
        if (item == null) return GameRules.BASIC_DAMAGE + " fyzické";
        int power = item.getPower();
        return switch (item.getItemType()) {
            case DAMAGE -> item.getDamageType() == Damage.DamageType.PHYSICAL
                    ? power + " fyzické"
                    : damageType(item.getDamageType()) + " " + compactTimed(power, item.getDuration());
            case HEAL -> item.getDuration() == 1 ? "+" + health(power)
                    : "Léčení " + compactTimed(power, item.getDuration());
            case STAMINA -> "+" + power + " výdrže příští tah";
            case DEFEND -> "Pohltí " + power + " poškození";
            case ARMOR -> armorSummary(item);
            case TRINKET -> passiveSummary(item);
        };
    }

    // ------------------------------------ uplny popis predmetu a ceny akce ------------------------------------

    public String description(Item item) {
        if (item == null) {
            return "Poškození:\n  Fyzické: " + GameRules.BASIC_DAMAGE
                    + "\n\nTrvalá součást výbavy. Nezabírá místo v inventáři. Poškození je před obranou cíle."
                    + "\n\nVýdrž: " + GameRules.BASIC_STAMINA_COST
                    + "\nOpakování v tahu: +"
                    + GameRules.BASIC_REPEAT_COST_INCREASE + " výdrže. Příští vlastní tah se cena obnoví.";
        }
        String result = switch (item.getItemType()) {
            case DAMAGE -> weaponDescription(item);
            case HEAL -> item.getDuration() == 1
                    ? "Léčení:\n  " + health(item.getPower())
                            + "\n\nObnoví při provedení plánu, nejvýše do maxima."
                    : "Léčení:\n  " + compactTimed(item.getPower(), item.getDuration())
                            + "\n\nZačne na začátku příštího vlastního tahu. Léčení nepřekročí maximum životů.";
            case STAMINA -> "Na začátku příštího vlastního tahu přidá "
                    + item.getPower() + " výdrže nad běžné maximum. Bonusy z více lektvarů se sčítají.";
            case DEFEND -> "Po vyhodnocení akce vytvoří štít, který pohltí celkem až " + item.getPower()
                    + " poškození všech typů po zbroji. Chrání až do konce úvodních účinků příštího vlastního tahu. Síla více štítů se sčítá.";
            case ARMOR -> armorDescription(item);
            case TRINKET -> passiveDescription(item)
                    + " Účinkuje pouze vybavený. Lze nosit jeden talisman.";
        };
        if (item.isConsumable()) result += " Po vyhodnocení akce se spotřebuje.";
        if (!item.isEquipment()) {
            result += "\n\nVýdrž: " + item.getStaminaCost();
            if (!item.isConsumable()) result += "\nOpakování v tahu: +" + item.getRepeatCostIncrease()
                    + " výdrže. Příští vlastní tah se cena obnoví. Zůstává v inventáři.";
        }
        return result;
    }

    // ------------------------------------ popis zbyleho casovaneho ucinku ------------------------------------

    public String effect(Damage effect) {
        String name = damageType(effect.getType()) + ": ";
        if (effect.getTimer() == 0) return name + "ukončeno";
        if (effect.getAmount() % effect.getTimer() == 0) {
            return name + compactTimed(effect.getAmount() / effect.getTimer(), effect.getTimer());
        }
        return name + timed(effect.getAmount(), effect.getTimer()) + " (" + effect.getAmount() + ")";
    }

    // ------------------------------------ sklonovani poctu zivotu a mist ------------------------------------

    private String health(int amount) {
        return amount + (amount == 1 ? " život" : amount >= 2 && amount <= 4 ? " životy" : " životů");
    }

    private String slots(int amount) {
        return amount + (amount == 1 ? " místo" : amount >= 2 && amount <= 4 ? " místa" : " míst");
    }

    // ------------------------------------ zapis davek a celkove sily ucinku ------------------------------------

    private String compactTimed(int rate, int turns) {
        return rate + " × " + turns + " (" + Math.multiplyExact(rate, turns) + ")";
    }

    private String timed(int amount, int turns) {
        // pocitame stejne jednotlive davky jako model aby popis nesliboval jiny soucet
        List<String> ticks = new ArrayList<>();
        int remaining = amount;
        for (int timer = turns; timer > 0; timer--) {
            int tick = (int) Math.round((double) remaining / timer);
            ticks.add(Integer.toString(tick));
            remaining -= tick;
        }
        return String.join(" → ", ticks);
    }

    // ------------------------------------ popisy zbrani ------------------------------------

    private String weaponDescription(Item item) {
        if (item.getDamageType() == Damage.DamageType.PHYSICAL) {
            return "Poškození:\n  Fyzické: " + item.getPower()
                    + "\n\nZasáhne při provedení plánu, před obranou cíle.";
        }
        String result = compactTimed(item.getPower(), item.getDuration())
                + "\n\nPůsobí na začátku soupeřova tahu, poprvé po provedení plánu. Hodnoty jsou před obranou cíle. ";
        result += item.getDamageType() == Damage.DamageType.FIRE
                ? "Oheň přičte svůj celkový zbytek ke stávajícímu ohni a ponechá delší zbývající trvání. Rozdělení dávek se tím změní, celkové poškození zůstane zachováno."
                : "Každý jed působí samostatně. Zbroj a štít mohou snížit jednotlivé dávky.";
        return "Poškození:\n  " + damageType(item.getDamageType()) + ": " + result;
    }

    // ------------------------------------ kratke a uplne popisy zbroji ------------------------------------

    private String armorSummary(Item item) {
        if (item.getPassive() == Item.Passive.NONE) {
            return "−" + item.getPower() + "% " + damageType(item.getDamageType());
        }
        String condition = item.getPassive() == Item.Passive.SMALL_HIT_BLOCK ? "≤" : "≥";
        return "−" + item.getPower() + " ze zásahů " + condition + item.getThreshold();
    }

    private String armorDescription(Item item) {
        String result;
        if (item.getPassive() == Item.Passive.NONE) {
            result = "Sníží poškození typu " + damageType(item.getDamageType())
                    + " o " + item.getPower()
                    + " %. Výsledek se zaokrouhlí na nejbližší celé číslo. Platí i na každou jednotlivou dávku časovaného účinku.";
        } else {
            String condition = item.getPassive() == Item.Passive.SMALL_HIT_BLOCK
                    ? "nejvýše " : "alespoň ";
            result = "Ze zásahů všech typů o síle " + condition + item.getThreshold()
                    + " odečte " + item.getPower()
                    + " poškození před štítem. Vždy však projde alespoň polovina původního zásahu, zaokrouhlená na nejbližší celé číslo.";
        }
        return result + " Snížení základní výdrže: " + item.getStaminaPenalty()
                + ". Účinkuje pouze vybavená. Lze nosit jednu zbroj.";
    }

    // ------------------------------------ kratke a uplne popisy talismanu ------------------------------------

    private String passiveSummary(Item item) {
        int power = item.getPower();
        return switch (item.getPassive()) {
            case REGENERATION -> "+" + health(power) + " každý vlastní tah";
            case LOW_HEALTH_HEAL -> "+" + health(power) + " pod polovinou zdraví";
            case STAMINA_BONUS -> "+" + power + " výdrže každý vlastní tah";
            case ATTACK_BONUS -> "+" + power + " k fyzickým útokům";
            case CLEANSE_POISON -> "Očista jedu: " + power + " za tah";
            case CLEANSE_FIRE -> "Očista ohně: " + power + " za tah";
            case CAPACITY -> "+" + slots(power) + " v inventáři";
            case SMALL_HIT_BLOCK, LARGE_HIT_BLOCK -> armorSummary(item);
            case NONE -> "Bez zvláštního účinku";
        };
    }

    private String passiveDescription(Item item) {
        int power = item.getPower();
        return switch (item.getPassive()) {
            case REGENERATION -> "Na začátku vlastního tahu před časovanými účinky obnoví " + health(power)
                    + ", nejvýše do maxima.";
            case LOW_HEALTH_HEAL -> "Pokud máš na začátku vlastního tahu méně než polovinu maximálních životů, obnoví " + health(power)
                    + " před časovanými účinky, nejvýše do maxima.";
            case STAMINA_BONUS -> "Na začátku vlastního tahu přidá " + power
                    + " výdrže nad běžné maximum.";
            case ATTACK_BONUS -> "Každý fyzický útok včetně pěsti způsobí o "
                    + power + " poškození více před obranou cíle.";
            case CLEANSE_POISON -> "Na začátku vlastního tahu před časovanými účinky odstraní celkem až " + power
                    + " zbývajícího poškození jedem. Postupuje od nejstaršího jedu; limit platí pro všechny jedy dohromady.";
            case CLEANSE_FIRE -> "Na začátku vlastního tahu před časovanými účinky odstraní až " + power
                    + " zbývajícího poškození ohněm.";
            case CAPACITY -> "Při vybavení přidá " + slots(power)
                    + " v inventáři. Sám zabírá jedno místo. Před sundáním nebo nahrazením se musí obsah vejít do menší kapacity.";
            case SMALL_HIT_BLOCK, LARGE_HIT_BLOCK -> armorDescription(item);
            case NONE -> "Bez zvláštního účinku.";
        };
    }
}
