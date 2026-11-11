import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GameText {
    private final boolean english;

    // preklad se vaze na stabilni jmeno definice a ne na poradi katalogu
    private static final Map<String, String> ENGLISH_NAMES = Map.ofEntries(
            Map.entry("Dýka", "Dagger"),
            Map.entry("Krátký meč", "Short sword"),
            Map.entry("Tesák", "Falchion"),
            Map.entry("Lovecké kopí", "Hunting spear"),
            Map.entry("Válečná sekera", "Battle axe"),
            Map.entry("Těžké kladivo", "Heavy hammer"),
            Map.entry("Rapír", "Rapier"),
            Map.entry("Dlouhý luk", "Longbow"),
            Map.entry("Kuše", "Crossbow"),
            Map.entry("Obouruční meč", "Greatsword"),
            Map.entry("Mistrovská čepel", "Masterwork blade"),
            Map.entry("Železný palcát", "Iron mace"),
            Map.entry("Pochodeň", "Torch"),
            Map.entry("Žhavý prut", "Searing rod"),
            Map.entry("Plamenná hůl", "Flame staff"),
            Map.entry("Ohnivá čepel", "Fire blade"),
            Map.entry("Dračí žezlo", "Dragon scepter"),
            Map.entry("Jiskřící hůlka", "Spark wand"),
            Map.entry("Jedovatá jehla", "Poison needle"),
            Map.entry("Hadí dýka", "Serpent dagger"),
            Map.entry("Morová hůl", "Plague staff"),
            Map.entry("Štíří kopí", "Scorpion spear"),
            Map.entry("Zmijí tesák", "Viper fang"),
            Map.entry("Trnový bič", "Thorn whip"),
            Map.entry("Malý léčivý lektvar", "Small healing potion"),
            Map.entry("Léčivý lektvar", "Healing potion"),
            Map.entry("Silný léčivý lektvar", "Strong healing potion"),
            Map.entry("Životabudič", "Reviving draught"),
            Map.entry("Bylinný čaj", "Herbal tea"),
            Map.entry("Hojivá mast", "Healing salve"),
            Map.entry("Regenerační odvar", "Regeneration brew"),
            Map.entry("Jarní esence", "Spring essence"),
            Map.entry("Balzám obnovy", "Restoration balm"),
            Map.entry("Obvaz", "Bandage"),
            Map.entry("Polní tinktura", "Field tincture"),
            Map.entry("Měsíční voda", "Moon water"),
            Map.entry("Doušek síly", "Sip of vigor"),
            Map.entry("Lektvar výdrže", "Stamina potion"),
            Map.entry("Silný lektvar výdrže", "Strong stamina potion"),
            Map.entry("Bojový elixír", "Battle elixir"),
            Map.entry("Mátový odvar", "Mint brew"),
            Map.entry("Káva průzkumníka", "Scout's coffee"),
            Map.entry("Horská vzpruha", "Mountain tonic"),
            Map.entry("Dračí dech", "Dragon breath"),
            Map.entry("Dřevěný štít", "Wooden shield"),
            Map.entry("Pukléř", "Buckler"),
            Map.entry("Kulatý štít", "Round shield"),
            Map.entry("Železný štít", "Iron shield"),
            Map.entry("Věžový štít", "Tower shield"),
            Map.entry("Strážcův štít", "Guardian shield"),
            Map.entry("Proutěný štít", "Wicker shield"),
            Map.entry("Ocelový pukléř", "Steel buckler"),
            Map.entry("Královská pavéza", "Royal pavise"),
            Map.entry("Kožený štít", "Leather shield"),
            Map.entry("Prošívanice", "Gambeson"),
            Map.entry("Kožená zbroj", "Leather armor"),
            Map.entry("Kroužková košile", "Chainmail"),
            Map.entry("Plátová zbroj", "Plate armor"),
            Map.entry("Rytířská zbroj", "Knight's armor"),
            Map.entry("Žáruvzdorný plášť", "Fireproof cloak"),
            Map.entry("Dračí šupiny", "Dragon scales"),
            Map.entry("Alchymistický kabát", "Alchemist's coat"),
            Map.entry("Morový hábit", "Plague robe"),
            Map.entry("Zpevněná vesta", "Reinforced vest"),
            Map.entry("Nárazová zbroj", "Impact armor"),
            Map.entry("Strážní kabát", "Watchman's coat"),
            Map.entry("Prsten obnovy", "Ring of renewal"),
            Map.entry("Srdce poutníka", "Traveler's heart"),
            Map.entry("Fénixův přívěsek", "Phoenix pendant"),
            Map.entry("Prsten výdrže", "Stamina ring"),
            Map.entry("Válečníkův amulet", "Warrior's amulet"),
            Map.entry("Hadí kámen", "Serpent stone"),
            Map.entry("Kapka deště", "Raindrop"),
            Map.entry("Cestovní brašna", "Travel bag"),
            Map.entry("Velký batoh", "Large backpack"),
            Map.entry("Jantarová brož", "Amber brooch"));

    public GameText(boolean english) { this.english = english; }

    public String text(String cs, String en) { return english ? en : cs; }

    public String name(Item item) {
        if (item == null) return text("Pěst", "Punch");
        return english ? ENGLISH_NAMES.getOrDefault(item.getName(), item.getName()) : item.getName();
    }

    public String type(ItemType type) {
        return switch (type) {
            case DAMAGE -> text("Zbraň", "Weapon");
            case HEAL -> text("Léčení", "Healing");
            case DEFEND -> text("Štít", "Shield");
            case STAMINA -> text("Výdrž", "Stamina");
            case ARMOR -> text("Zbroj", "Armor");
            case TRINKET -> text("Talisman", "Trinket");
        };
    }

    public String damageType(Damage.DamageType type) {
        return switch (type) {
            case PHYSICAL -> text("Fyzické", "Physical");
            case FIRE -> text("Oheň", "Fire");
            case POISON -> text("Jed", "Poison");
            case HEAL -> text("Léčení", "Healing");
        };
    }

    public String summary(Item item) {
        if (item == null) return GameRules.BASIC_DAMAGE + text(" fyzické", " physical");
        int power = item.getPower();
        return switch (item.getItemType()) {
            case DAMAGE -> item.getDamageType() == Damage.DamageType.PHYSICAL
                    ? power + text(" fyzické", " physical")
                    : damageType(item.getDamageType()) + " " + compactTimed(power, item.getDuration());
            case HEAL -> item.getDuration() == 1 ? "+" + health(power)
                    : text("Léčení ", "Heal ") + compactTimed(power, item.getDuration());
            case STAMINA -> "+" + power + text(" výdrže příští tah", " stamina next turn");
            case DEFEND -> text("Pohltí ", "Absorbs ") + power + text(" poškození", " damage");
            case ARMOR -> armorSummary(item);
            case TRINKET -> passiveSummary(item);
        };
    }

    public String description(Item item) {
        if (item == null) {
            return text("Poškození:\n  Fyzické: ", "Damage:\n  Physical: ") + GameRules.BASIC_DAMAGE
                    + text("\n\nTrvalá součást výbavy. Nezabírá místo v inventáři. Poškození je před obranou cíle.",
                            "\n\nPermanent toolkit item. Uses no inventory space. Damage is before defenses.")
                    + text("\n\nVýdrž: ", "\n\nStamina: ") + GameRules.BASIC_STAMINA_COST
                    + text("\nOpakování v tahu: +", "\nReuse this turn: +")
                    + GameRules.BASIC_REPEAT_COST_INCREASE + text(" výdrže. Příští vlastní tah se cena obnoví.", " stamina. Cost resets next turn.");
        }
        String result = switch (item.getItemType()) {
            case DAMAGE -> weaponDescription(item);
            case HEAL -> item.getDuration() == 1
                    ? text("Léčení:\n  ", "Healing:\n  ") + health(item.getPower())
                            + text("\n\nObnoví při provedení plánu, nejvýše do maxima.", "\n\nRestores when the plan resolves, up to maximum health.")
                    : text("Léčení:\n  ", "Healing:\n  ") + compactTimed(item.getPower(), item.getDuration())
                            + text("\n\nZačne na začátku příštího vlastního tahu. Léčení nepřekročí maximum životů.",
                                    ". Starts at the beginning of your next turn. Healing cannot exceed maximum health.");
            case STAMINA -> text("Na začátku příštího vlastního tahu přidá ", "At the beginning of your next turn, adds ")
                    + item.getPower() + text(" výdrže nad běžné maximum. Bonusy z více lektvarů se sčítají.",
                            " stamina above your usual maximum. Bonuses from multiple potions stack.");
            case DEFEND -> text("Po vyhodnocení akce vytvoří štít, který pohltí celkem až ",
                    "When the action resolves, grants a shield that absorbs up to ") + item.getPower()
                    + text(" poškození všech typů po zbroji. Chrání až do konce úvodních účinků příštího vlastního tahu. Síla více štítů se sčítá.",
                            " total damage of any type after armor. Lasts through the opening effects of your next turn. Multiple shields stack.");
            case ARMOR -> armorDescription(item);
            case TRINKET -> passiveDescription(item)
                    + text(" Účinkuje pouze vybavený. Lze nosit jeden talisman.", " Only works while equipped. You can equip one trinket.");
        };
        if (item.isConsumable()) result += text(" Po vyhodnocení akce se spotřebuje.", " Consumed when the action resolves.");
        if (!item.isEquipment()) {
            result += text("\n\nVýdrž: ", "\n\nStamina: ") + item.getStaminaCost();
            if (!item.isConsumable()) result += text("\nOpakování v tahu: +", "\nReuse this turn: +") + item.getRepeatCostIncrease()
                    + text(" výdrže. Příští vlastní tah se cena obnoví. Zůstává v inventáři.",
                            " stamina. Cost resets next turn. Remains in inventory.");
        }
        return result;
    }

    public String effect(Damage effect) {
        String name = damageType(effect.getType()) + ": ";
        if (effect.getTimer() == 0) return name + text("ukončeno", "expired");
        if (effect.getAmount() % effect.getTimer() == 0) {
            return name + compactTimed(effect.getAmount() / effect.getTimer(), effect.getTimer());
        }
        return name + timed(effect.getAmount(), effect.getTimer()) + " (" + effect.getAmount() + ")";
    }

    private String health(int amount) {
        return amount + text(amount == 1 ? " život" : amount >= 2 && amount <= 4 ? " životy" : " životů", " health");
    }

    private String slots(int amount) {
        return amount + text(amount == 1 ? " místo" : amount >= 2 && amount <= 4 ? " místa" : " míst",
                amount == 1 ? " slot" : " slots");
    }

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

    private String weaponDescription(Item item) {
        if (item.getDamageType() == Damage.DamageType.PHYSICAL) {
            return text("Poškození:\n  Fyzické: ", "Damage:\n  Physical: ") + item.getPower()
                    + text("\n\nZasáhne při provedení plánu, před obranou cíle.", "\n\nHits when the plan resolves, before defenses.");
        }
        String result = compactTimed(item.getPower(), item.getDuration())
                + text("\n\nPůsobí na začátku soupeřova tahu, poprvé po provedení plánu. Hodnoty jsou před obranou cíle. ",
                        "\n\nTicks at the start of the opponent's turn after the plan resolves, before defenses. ");
        result += item.getDamageType() == Damage.DamageType.FIRE
                ? text("Oheň přičte svůj celkový zbytek ke stávajícímu ohni a ponechá delší zbývající trvání. Rozdělení dávek se tím změní, celkové poškození zůstane zachováno.",
                        "Fire adds its remaining total to existing fire and keeps the longer remaining duration. Tick amounts change, but the total is preserved.")
                : text("Každý jed působí samostatně. Zbroj a štít mohou snížit jednotlivé dávky.",
                        "Each poison acts independently. Armor and shields can reduce individual ticks.");
        return text("Poškození:\n  ", "Damage:\n  ") + damageType(item.getDamageType()) + ": " + result;
    }

    private String armorSummary(Item item) {
        if (item.getPassive() == Item.Passive.NONE) {
            return "−" + item.getPower() + "% " + damageType(item.getDamageType());
        }
        String condition = item.getPassive() == Item.Passive.SMALL_HIT_BLOCK ? "≤" : "≥";
        return "−" + item.getPower() + text(" ze zásahů ", " from hits ") + condition + item.getThreshold();
    }

    private String armorDescription(Item item) {
        String result;
        if (item.getPassive() == Item.Passive.NONE) {
            result = text("Sníží poškození typu ", "Reduces ") + damageType(item.getDamageType())
                    + text(" o ", " damage by ") + item.getPower()
                    + text(" %. Výsledek se zaokrouhlí na nejbližší celé číslo. Platí i na každou jednotlivou dávku časovaného účinku.",
                            "%. The result rounds to the nearest integer. Also applies to each individual timed damage tick.");
        } else {
            String condition = item.getPassive() == Item.Passive.SMALL_HIT_BLOCK
                    ? text("nejvýše ", "at most ") : text("alespoň ", "at least ");
            result = text("Ze zásahů všech typů o síle ", "For hits of any type dealing ") + condition + item.getThreshold()
                    + text(" odečte ", " damage, subtracts ") + item.getPower()
                    + text(" poškození před štítem. Vždy však projde alespoň polovina původního zásahu, zaokrouhlená na nejbližší celé číslo.",
                            " damage before shields. At least half the original hit still passes through, rounded to the nearest integer.");
        }
        return result + text(" Snížení základní výdrže: ", " Base stamina penalty: ") + item.getStaminaPenalty()
                + text(". Účinkuje pouze vybavená. Lze nosit jednu zbroj.", ". Only works while equipped. You can equip one armor item.");
    }

    private String passiveSummary(Item item) {
        int power = item.getPower();
        return switch (item.getPassive()) {
            case REGENERATION -> "+" + health(power) + text(" každý vlastní tah", " each own turn");
            case LOW_HEALTH_HEAL -> "+" + health(power) + text(" pod polovinou zdraví", " below half health");
            case STAMINA_BONUS -> "+" + power + text(" výdrže každý vlastní tah", " stamina each own turn");
            case ATTACK_BONUS -> "+" + power + text(" k fyzickým útokům", " to physical attacks");
            case CLEANSE_POISON -> text("Očista jedu: ", "Poison cleansing: ") + power + text(" za tah", " per turn");
            case CLEANSE_FIRE -> text("Očista ohně: ", "Fire cleansing: ") + power + text(" za tah", " per turn");
            case CAPACITY -> "+" + slots(power) + text(" v inventáři", " in inventory");
            case SMALL_HIT_BLOCK, LARGE_HIT_BLOCK -> armorSummary(item);
            case NONE -> text("Bez zvláštního účinku", "No special effect");
        };
    }

    private String passiveDescription(Item item) {
        int power = item.getPower();
        return switch (item.getPassive()) {
            case REGENERATION -> text("Na začátku vlastního tahu před časovanými účinky obnoví ",
                    "At the beginning of your turn, before timed effects, restores ") + health(power)
                    + text(", nejvýše do maxima.", ", up to maximum health.");
            case LOW_HEALTH_HEAL -> text("Pokud máš na začátku vlastního tahu méně než polovinu maximálních životů, obnoví ",
                    "If you start your turn below half your maximum health, restores ") + health(power)
                    + text(" před časovanými účinky, nejvýše do maxima.", " before timed effects, up to maximum health.");
            case STAMINA_BONUS -> text("Na začátku vlastního tahu přidá ", "At the beginning of your turn, grants ") + power
                    + text(" výdrže nad běžné maximum.", " stamina above your usual maximum.");
            case ATTACK_BONUS -> text("Každý fyzický útok včetně pěsti způsobí o ", "Every physical attack, including Punch, deals ")
                    + power + text(" poškození více před obranou cíle.", " extra damage before the target's defenses.");
            case CLEANSE_POISON -> text("Na začátku vlastního tahu před časovanými účinky odstraní celkem až ",
                    "At the beginning of your turn, before timed effects, removes up to ") + power
                    + text(" zbývajícího poškození jedem. Postupuje od nejstaršího jedu; limit platí pro všechny jedy dohromady.",
                            " remaining poison damage in total. Cleanses the oldest poison first; the limit is shared across all poisons.");
            case CLEANSE_FIRE -> text("Na začátku vlastního tahu před časovanými účinky odstraní až ",
                    "At the beginning of your turn, before timed effects, removes up to ") + power
                    + text(" zbývajícího poškození ohněm.", " remaining fire damage.");
            case CAPACITY -> text("Při vybavení přidá ", "While equipped, adds ") + slots(power)
                    + text(" v inventáři. Sám zabírá jedno místo. Před sundáním nebo nahrazením se musí obsah vejít do menší kapacity.",
                            " in inventory. Uses one slot itself. Before removing or replacing it, your items must fit the smaller capacity.");
            case SMALL_HIT_BLOCK, LARGE_HIT_BLOCK -> armorDescription(item);
            case NONE -> text("Bez zvláštního účinku.", "No special effect.");
        };
    }
}
