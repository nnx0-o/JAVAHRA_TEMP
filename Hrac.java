public class Hrac {
    private final String name;
    private int zivoty;
    private int zlataky;
    private final Pole<Predmet> inventar;
    
    public Hrac(String name) {
        this.name = name;
        this.zivoty = 4;
        this.zlataky = 10;
        this.inventar = new Pole<>(5);
    }

    public boolean pridejInventar(DruhPredmetu druh, String nazev) {
        if (this.zlataky < 3) return false;
        this.zlataky -= 3;
        return inventar.pridejPolozku(new Predmet(druh, nazev));
    }

    public void vyzvatNaSouboj(Hrac souper) {
        int ja = this.hodKostkou();
        int protivnik = souper.hodKostkou();
        if (ja == protivnik) return;
        if (ja > protivnik) {
            this.zlataky++;
            souper.zivoty--;
        }
        else {
            this.zivoty--;
            souper.zlataky++;
        }

    }

    private int hodKostkou() {
        return (int)(Math.random()*6 + 1);
    }

    @Override
    public String toString() {
        return "Hrac [name=" + name + ", zivoty=" + zivoty + ", zlataky=" + zlataky + ", inventar=" + inventar + "]";
    }  
    
}
