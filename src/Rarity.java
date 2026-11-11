public enum Rarity {

    // ------------------------------------ cetnost nabidky ------------------------------------

    COMMON("Běžný", 4),
    UNCOMMON("Neobvyklý", 3),
    RARE("Vzácný", 2);

    private final String label;
    private final int weight;

    Rarity(String label, int weight) {
        this.label = label;
        this.weight = weight;
    }

    public int getWeight() {
        return weight; // vaha meni sanci na vyber ale ne pocet kusu
    }

    @Override
    public String toString() {
        return label;
    }
}
