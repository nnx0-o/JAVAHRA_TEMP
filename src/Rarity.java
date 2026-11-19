public enum Rarity {

    // dostupne hodnoty

    COMMON(4), UNCOMMON(3), RARE(2);

    // vaha pri random vyberu

    private final int weight;

    // vytvoreni hodnoty

    Rarity(int weight) {
        this.weight = weight;
    }

    // cteni vahy

    public int getWeight() {
        return weight; // vaha meni sanci na vyber ale ne pocet kusu
    }

}
