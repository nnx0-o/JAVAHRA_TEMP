import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class MainUiTest {

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        Field field = Main.class.getDeclaredField("tui");
        field.setAccessible(true);
        MiniTui tui = (MiniTui) field.get(main);
        click(tui, "Připraveni");
        click(tui, "Otevřít sdílený obchod");
        checkLayout(tui);
        click(tui, "Další stránka nabídky");
        MiniTui.Button offer = (MiniTui.Button) tui.getElements().stream()
                .filter(element -> element instanceof MiniTui.Button && element.getText().contains(" ks | ")).findFirst().orElseThrow();
        offer.action.run();
        checkLayout(tui);
        click(tui, "Koupit jeden kus");
        click(tui, "Zpět");
        click(tui, "Inventář");
        checkLayout(tui);
        click(tui, "Zpět");
        Field gameField = Main.class.getDeclaredField("game");
        gameField.setAccessible(true);
        Game game = (Game) gameField.get(main);
        for (int count = 0; count < 4 && game.getPhase() == Game.Phase.SHOP; count++) click(tui, "Pokračovat");
        check(game.getPhase() == Game.Phase.COMBAT, "UI reaches battle");
        click(tui, "Stavy, vybavení");
        checkLayout(tui);
        click(tui, "Další stránka");
        click(tui, "Zpět");
        click(tui, "Ukončit tah");
        click(tui, "Ukončit tah");
        int health = game.getOpponent().getHealth();
        click(tui, "Základní útok");
        check(game.getOpponent().getHealth() == health - GameRules.BASIC_DAMAGE, "UI attack");
        click(tui, "Průběh souboje");
        checkLayout(tui);
        click(tui, "Zpět");
        click(tui, "Pravidla hry");
        checkLayout(tui);
        click(tui, "Zpět");
        Method geometry = MiniTui.class.getDeclaredMethod("updateTerminalGeometry", int.class, int.class, int.class, int.class);
        geometry.setAccessible(true);
        Method draw = Main.class.getDeclaredMethod("draw");
        draw.setAccessible(true);
        geometry.invoke(tui, 60, 24, 0, 0);
        draw.invoke(main);
        checkLayout(tui);
        geometry.invoke(tui, 40, 12, 0, 0);
        draw.invoke(main);
        check(tui.getElements().stream().noneMatch(element -> element instanceof MiniTui.Button), "small window pauses controls");
        geometry.invoke(tui, 100, 35, 0, 0);
        draw.invoke(main);
        checkLayout(tui);
        check(game.getPhase() == Game.Phase.COMBAT, "resize preserves phase");
        check(tui.getElements().size() < 40, "no hidden widget accumulation");
        System.out.println("MainUiTest: OK");
    }

    private static void click(MiniTui tui, String prefix) {
        MiniTui.Button button = (MiniTui.Button) tui.getElements().stream()
                .filter(element -> element instanceof MiniTui.Button && element.visible && element.getText().startsWith(prefix))
                .findFirst().orElseThrow(() -> new AssertionError("missing button " + prefix));
        button.action.run();
    }

    private static void checkLayout(MiniTui tui) {
        Set<String> positions = new HashSet<>();
        for (MiniTui.Element element : tui.getElements()) {
            check(element.y < tui.getTerminalHeight() - 1 && element.y >= 0, "element within viewport");
            check(positions.add(element.x + ":" + element.y), "no overlapping anchors");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
