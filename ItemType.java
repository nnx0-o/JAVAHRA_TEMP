public enum ItemType {
    SWORD("MEC"),
    POTION("LEKTVAR"),
    SHIELD("STIT"),
    ARMOR("BRNENI"),
    BOW("LUK");

    private final String czechName;

    ItemType(String czechName) {
        this.czechName = czechName;
    }

    @Override
    public String toString() {
        return czechName;
    }
}
