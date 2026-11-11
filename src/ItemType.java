public enum ItemType {

    // ------------------------------------ typy predmetu ------------------------------------

    DAMAGE("Zbraň"),
    HEAL("Léčení"),
    DEFEND("Štít"),
    STAMINA("Výdrž"),
    ARMOR("Zbroj"),
    TRINKET("Talisman");

    private final String label;

    ItemType(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label; // nazvy v rozhrani jsou ceske ale konstanty zustavaji anglicke
    }
}
