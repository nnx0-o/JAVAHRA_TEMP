public enum ItemType {

    // ------------------------------------ dostupne hodnoty ------------------------------------

    DAMAGE("Zbraň"),
    HEAL("Léčení"),
    DEFEND("Štít"),
    STAMINA("Výdrž"),
    ARMOR("Zbroj"),
    TRINKET("Talisman");

    // ------------------------------------ cesky nazev ------------------------------------

    private final String label;

    // ------------------------------------ vytvoreni hodnoty ------------------------------------

    ItemType(String label) {
        this.label = label;
    }

    // ------------------------------------ cteni hodnoty a textovy vypis ------------------------------------

    @Override
    public String toString() {
        return label; // nazvy v rozhrani jsou ceske ale konstanty zustavaji anglicke
    }
}
