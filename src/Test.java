public class Test {

    // ------------------------------------ inicializace ------------------------------------

    private static final int DEMO_LAYER = 0;
    private static final int STORE_LAYER = 1;

    public final MiniTui tui = new MiniTui();

    public Test() {
        // vrstva 0 ukazuje tlacitka
        tui.add(new MiniTui.Title(2, 1, DEMO_LAYER, "Ukázka tlačítek"));
        MiniTui.Label variableLabel = tui.add(new MiniTui.Label(2, 3, DEMO_LAYER, "Ahoj"));
        tui.add(new MiniTui.Button(2, 5, DEMO_LAYER, "Otevřít obchod", () -> {
            tui.setLayer(STORE_LAYER);
        }));
        MiniTui.Button variableButton = tui.add(
                new MiniTui.Button(2, 7, DEMO_LAYER, "Změnit na dobrý den", null));
        variableButton.action = () -> {
            if (variableButton.getText().equals("Změnit na dobrý den")) {
                variableLabel.setText("Dobrý den");
                variableButton.setText("Změnit na ahoj");
            } else {
                variableLabel.setText("Ahoj");
                variableButton.setText("Změnit na dobrý den");
            }
        };

        tui.add(new MiniTui.Button(2, 9, DEMO_LAYER, "Konec", tui::stop));

        // vrstva 1 ukazuje obchod
        tui.add(new MiniTui.Title(2, 1, STORE_LAYER, "Obchod"));
        tui.add(new MiniTui.Button(2, 5, STORE_LAYER, "Zpět na ukázku", () -> tui.setLayer(DEMO_LAYER)));
        tui.add(new MiniTui.Button(2, 9, STORE_LAYER, "Konec", tui::stop));

        tui.setStatus("Šipky: výběr | Enter: potvrdit | [/]: obrazovka | Ctrl+C: konec");
        tui.setLayer(DEMO_LAYER);

        MiniTui.Label bottomLabel = tui.add(new MiniTui.Label(30, 30, 0, "Ahoj~"));

        variableButton.setText(bottomLabel.getText());
    }

    // vstupni bod
    public static void main(String[] args) {
        new Test().tui.start();
    }
}
