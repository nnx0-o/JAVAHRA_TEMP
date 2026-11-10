public class Test {

    private static final int DEMO_LAYER = 0;
    private static final int STORE_LAYER = 1;

    public final MiniTui tui = new MiniTui();

    public Test() {
        // Layer 0: button demo.
        tui.add(new MiniTui.Title(2, 1, DEMO_LAYER, "Button demo"));
        MiniTui.Label variableLabel = tui.add(new MiniTui.Label(2, 3, DEMO_LAYER, "Hellou"));
        tui.add(new MiniTui.Button(2, 5, DEMO_LAYER, "Open store", () -> {
            tui.setLayer(STORE_LAYER);
        }));
        MiniTui.Button variableButton = tui.add(
                new MiniTui.Button(2, 7, DEMO_LAYER, "Its not Hellou yknow...", null));
        variableButton.action = () -> {
            if (variableButton.getText().equals("Its not Hellou yknow...")) {
                variableLabel.setText("Hellow");
                variableButton.setText("Its not Hellow yknow...");
            } else {
                variableLabel.setText("Hellou");
                variableButton.setText("Its not Hellou yknow...");
            }
        };

        tui.add(new MiniTui.Button(2, 9, DEMO_LAYER, "Exit", tui::stop));

        // Layer 1: store.
        tui.add(new MiniTui.Title(2, 1, STORE_LAYER, "Store"));
        tui.add(new MiniTui.Button(2, 5, STORE_LAYER, "Back to demo", () -> tui.setLayer(DEMO_LAYER)));
        tui.add(new MiniTui.Button(2, 9, STORE_LAYER, "Exit", tui::stop));

        tui.setStatus("Up/Down: select | Enter: activate | [/]: switch layer | Ctrl+C: exit");
        tui.setLayer(DEMO_LAYER);

        MiniTui.Label bottomLabel = tui.add(new MiniTui.Label(30, 30, 0, "Ahoj~"));

        variableButton.setText(bottomLabel.getText());
    }

    // entry point
    public static void main(String[] args) {
        new Test().tui.start();
    }
}
