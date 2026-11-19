public enum Rarity {

    // moznosti

    COMMON(4), UNCOMMON(3), RARE(2);

    // jak casto to padne

    private final int weight;

    // nastavime vahu

    Rarity(int weight) {
        this.weight = weight;
    }

    // vaha

    public int getWeight() {
        return weight; // vetsi vaha jen zvysi sanci kusy se losuji zvlast
    }

}
