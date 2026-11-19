public enum Rarity {

    COMMON(4), UNCOMMON(3), RARE(2);

    private final int weight;

    Rarity(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight; // vetsi vaha jen zvysi sanci kusy se losuji zvlast
    }

}
