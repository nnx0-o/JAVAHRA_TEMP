class Pole<E> {
    private E[] polozky;
    private int pocetPolozek;

    public Pole(int velikost) {
        this.polozky = (E[])(new Object[velikost]);
        this.pocetPolozek = 0;
    }

    public boolean pridejPolozku(E novaPolozka) {
        if (this.pocetPolozek == this.polozky.length)
            return false;
        this.polozky[this.pocetPolozek++] = novaPolozka;
        return true;
    }

    public E vratPolozku(int index) {
        if (index >= this.pocetPolozek) return null;
        return this.polozky[index];
    }

    public E odeberPolozku(int index) {
        if (index >= this.pocetPolozek) return null;
        E odebrano = this.polozky[index];
        for (int i=index; i<this.pocetPolozek-1; i++) {
            this.polozky[i] = this.polozky[i+1];
        }
        this.polozky[--this.pocetPolozek] = null;
        return odebrano;
    }

    @Override
    public String toString() {
        String tisk = "Polozky\n";
        for (int i=0; i<this.pocetPolozek; i++) {
            tisk += this.polozky[i] + "\n";
        }
        return tisk;
    }

}