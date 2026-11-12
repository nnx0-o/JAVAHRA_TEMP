public enum Rarity {

    // ------------------------------------ dostupne hodnoty ------------------------------------

    COMMON("Běžný", 4),
    UNCOMMON("Neobvyklý", 3),
    RARE("Vzácný", 2);

    // ------------------------------------ cesky nazev a vaha vyberu ------------------------------------

    private final String label;
    private final int weight;

    // ------------------------------------ vytvoreni hodnoty ------------------------------------

    Rarity(String label, int weight) {
        this.label = label;
        this.weight = weight;
    }

    // ------------------------------------ cteni hodnoty a textovy vypis ------------------------------------

    public int getWeight() {
        return weight; // vaha meni sanci na vyber ale ne pocet kusu
    }

    @Override
    public String toString() {
        return label;
    }
}
