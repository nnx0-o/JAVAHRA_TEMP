public final class GameRules {

    // ------------------------------------ vychozi zdravi, vydrz a zlato ------------------------------------

    public static final int MAX_HEALTH = 40;
    public static final int MAX_STAMINA = 12;
    public static final int STARTING_GOLD = 15;

    // ------------------------------------ odmeny za souboj ------------------------------------

    public static final int WIN_REWARD = 6;
    public static final int LOSS_REWARD = 9;

    // ------------------------------------ velikost inventare a nabidky ------------------------------------

    public static final int INVENTORY_CAPACITY = 10;
    public static final int SHOP_SIZE = 10;

    // ------------------------------------ pocet vyher k ukonceni zapasu ------------------------------------

    public static final int WINS_REQUIRED = 3;

    // ------------------------------------ sila a cena opakovaneho utoku pesti ------------------------------------

    public static final int BASIC_DAMAGE = 3;
    public static final int BASIC_STAMINA_COST = 2;
    public static final int BASIC_REPEAT_COST_INCREASE = 1;

    // ------------------------------------ delka pripravne faze ------------------------------------

    public static final int PREPARATION_TURNS = 2; // jeden vlastni tah kazdeho hrace

    // ------------------------------------ zakaz vytvareni instance: trida obsahuje pouze konstanty ------------------------------------

    private GameRules() {
    }
}
