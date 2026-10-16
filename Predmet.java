public class Predmet {

    private DruhPredmetu druhPredmetu;
    private String jmeno;
    
    public Predmet(DruhPredmetu druhPredmetu, String jmeno) {
        this.druhPredmetu = druhPredmetu;
        this.jmeno = jmeno;
    }

    @Override
    public String toString() {
        return "Predmet [druhPredmetu=" + druhPredmetu + ", jmeno=" + jmeno + "]";
    }   

}
