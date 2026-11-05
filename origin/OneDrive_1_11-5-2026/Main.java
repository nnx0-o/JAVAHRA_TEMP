class Main {
    public static void main(String[] args) {

        Hrac pepa = new Hrac("Pepa");
        System.out.println(pepa.pridejInventar(DruhPredmetu.LEKTVAR, "sily") ? "pridano" : "nepridano");
        System.out.println(pepa);

        System.out.println(pepa.pridejInventar(DruhPredmetu.LUK, "rychly") ? "pridano" : "nepridano");
        System.out.println(pepa.pridejInventar(DruhPredmetu.MEC, "kouzelny") ? "pridano" : "nepridano");
        System.out.println(pepa.pridejInventar(DruhPredmetu.STIT, "pevny") ? "pridano" : "nepridano");
        
        System.out.println(pepa);

        Hrac mahulena = new Hrac("Mahulena");
        System.out.println(mahulena.pridejInventar(DruhPredmetu.STIT, "pevny") ? "pridano" : "nepridano");
        System.out.println(mahulena);

        pepa.vyzvatNaSouboj(mahulena);
        System.out.println(pepa);
        System.out.println(mahulena);

        mahulena.vyzvatNaSouboj(pepa);
        System.out.println(pepa);
        System.out.println(mahulena);
    }
}
