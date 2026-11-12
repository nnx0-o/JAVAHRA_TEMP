import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MainUiTest {

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        MiniTui tui = (MiniTui) field(main, "tui");
        resize(main, tui, 140, 42);
        check(has(tui, "Začít"), "welcome before names");
        snapshot(tui, "welcome");
        click(tui, "F1  Návod");
        check(tui.getElements().stream().anyMatch(e -> e.getText().contains("Začni tady")), "tutorial starts with brief overview");
        click(tui, "Esc  Zpět");
        click(tui, "Začít");
        check(tui.getElements().stream().anyMatch(e -> e instanceof MiniTui.Input && e.getText().equals("Hráč 1")), "default names");
        click(tui, "Připraveni");
        Game game = (Game) field(main, "game");
        check(game.getPhase() == Game.Phase.SHOP, "ready goes straight to shop");
        check(tui.getElements().stream().filter(e -> e instanceof MiniTui.TableRow && e.focusable).count() == 10, "large shop shows all offers");
        check(tui.getElements().stream().noneMatch(e -> e.getText().contains("Beze změny") || e.getText().contains("sdílen")), "no developer labels");
        check(tui.getElements().stream().noneMatch(e -> e instanceof MiniTui.Button && e.getText().equals("Konec")), "no exit menu button");
        check(tui.getElements().stream().filter(e -> e instanceof MiniTui.Box).count() >= 5, "persistent framed areas");
        check(!hasExact(tui, "→") && !hasExact(tui, "←"), "no pagination when all offers fit");
        snapshot(tui, "shop");
        resize(main, tui, 100, 32);
        check(hasExact(tui, "→"), "pagination appears only when offers overflow");
        snapshot(tui, "compact-shop");
        resize(main, tui, 140, 42);
        String firstTheme = tui.getTheme();
        Item affordable = game.getShop().getStock().keySet().stream().filter(i -> i.getPrice() <= game.getCurrentPlayer().getGold()).findFirst().orElseThrow();
        click(tui, affordable.getName());
        check(has(tui, "Koupit za"), "detail allows purchase");
        click(tui, "Koupit za");
        click(tui, "Esc  Zpět");
        click(tui, "Pokračovat →");
        check(!tui.getTheme().equals(firstTheme), "active player changes theme");
        snapshot(tui, "shop-second-player");
        click(tui, "Pokračovat →");
        check(has(tui, "Zahájit souboj"), "continue predicts exact next state");
        click(tui, "Zahájit souboj");
        check(game.isPreparation(), "preparation label distinct from round one");
        click(tui, "Provést plán");
        click(tui, "Provést plán");
        check(game.getCombatRound() == 1 && game.getTurnPosition() == 1, "round pairs exclude preparation");
        int before = game.getOpponent().getHealth();
        click(tui, "Pěst");
        check(game.getOpponent().getHealth() == before, "selection does not execute attack");
        check(game.getCurrentPlayer().getActions().size() == 1, "plan visible in engine");
        check(tui.getElements().stream().anyMatch(e -> e instanceof MiniTui.Box && e.getText().equals("Výbava — výdrž 10/12")), "available stamina beside toolkit");
        check(!hasExact(tui, "↑") && !hasExact(tui, "↓"), "no plan reorder buttons");
        int rightX = (int) field(main, "rightX");
        check(tui.getElements().stream().noneMatch(e -> e instanceof MiniTui.Label && e.x >= rightX
                && (e.getText().startsWith("Výdrž") || e.getText().startsWith("Zlato"))), "combat sidebar hides turn resources");
        check(tui.getElements().stream().anyMatch(e -> e instanceof MiniTui.Box && e.getText().equals("Info")), "info panel title");
        snapshot(tui, "planned-combat");
        Method key = MiniTui.class.getDeclaredMethod("handleKey", String.class); key.setAccessible(true);
        key.invoke(tui, "i");
        check(tui.getElements().stream().anyMatch(e -> e instanceof MiniTui.Label && e.getText().equals("Poškození:")), "damage field occupies separate line");
        check(tui.getElements().stream().noneMatch(e -> e instanceof MiniTui.Label && e.getText().contains("\n")), "description lines render separately");
        snapshot(tui, "punch-detail");
        click(tui, "Esc  Zpět");
        click(tui, "F1  Návod");
        click(tui, "Další téma");
        snapshot(tui, "tutorial");
        click(tui, "Esc  Zpět");
        check(game.getCurrentPlayer().getActions().size() == 1, "help preserves plan");
        click(tui, "L  Historie");
        click(tui, "Esc  Zpět");
        check(game.getCurrentPlayer().getActions().size() == 1, "history preserves plan");
        click(tui, "Odebrat");
        check(game.getCurrentPlayer().getActions().isEmpty(), "remove planned action");
        click(tui, "Pěst");
        click(tui, "Provést plán");
        check(game.getCurrentPlayer().getHealth() == before - GameRules.BASIC_DAMAGE, "end turn executes attack");
        resize(main, tui, 100, 32);
        snapshot(tui, "compact-combat");
        checkBounds(tui);
        resize(main, tui, 60, 24);
        check(tui.getElements().stream().noneMatch(e -> e instanceof MiniTui.Button), "small terminal pauses controls");
        resize(main, tui, 180, 50);
        snapshot(tui, "large-combat");
        checkBounds(tui);
        check(game.getPhase() == Game.Phase.COMBAT, "resize preserves game");
        check(tui.getElements().size() < 100, "no stale widgets");

        game.getOpponent().takeDamage(game.getOpponent().getHealth() - 1);
        game.act(Player.Action.attack()); game.endTurn();
        resize(main, tui, 140, 42);
        check(game.getPhase() == Game.Phase.RESULT, "result screen reached");
        String winnerName = game.getPlayer(game.getWinner()).getName();
        check(tui.getElements().stream().anyMatch(e -> e.getText().contains(winnerName) && e.getText().contains("+6")),
                "rewards name their recipients");
        snapshot(tui, "result");
        click(tui, "Pokračovat do obchodu");
        check(game.getPhase() == Game.Phase.SHOP, "result continues directly to next shop");

        Main czech = new Main(); MiniTui czechTui = (MiniTui) field(czech, "tui");
        resize(czech, czechTui, 140, 42);
        check(!has(czechTui, "English"), "Czech-only interface has no language selector");
        System.out.println("MainUiTest: OK (rendered frames in build/ui-snapshots)");
    }

    private static boolean hasExact(MiniTui tui, String name) {
        return tui.getElements().stream().anyMatch(e -> e instanceof MiniTui.Button && e.getText().equals(name));
    }
    private static boolean has(MiniTui tui, String prefix) {
        return tui.getElements().stream().anyMatch(e -> e instanceof MiniTui.Button && e.getText().startsWith(prefix));
    }
    private static void click(MiniTui tui, String prefix) {
        MiniTui.Button button = (MiniTui.Button) tui.getElements().stream().filter(e -> e instanceof MiniTui.Button
                && e.visible && e.getText().startsWith(prefix)).findFirst().orElseThrow(() -> new AssertionError("missing button " + prefix));
        check(button.enabled, "control enabled: " + prefix);
        button.action.run();
    }
    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
    private static void resize(Main main, MiniTui tui, int width, int height) throws Exception {
        Method geometry = MiniTui.class.getDeclaredMethod("updateTerminalGeometry", int.class, int.class, int.class, int.class);
        geometry.setAccessible(true); geometry.invoke(tui, width, height, 0, 0);
        Method draw = Main.class.getDeclaredMethod("draw"); draw.setAccessible(true); draw.invoke(main);
    }
    private static void snapshot(MiniTui tui, String name) throws Exception {
        Method render = MiniTui.class.getDeclaredMethod("renderScreen"); render.setAccessible(true);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); PrintStream previous = System.out;
        try { System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8)); render.invoke(tui); }
        finally { System.setOut(previous); }
        Files.createDirectories(Path.of("build/ui-snapshots"));
        Files.write(Path.of("build/ui-snapshots/" + name + ".ansi"), bytes.toByteArray());
        Files.writeString(Path.of("build/ui-snapshots/" + name + ".size"), tui.getTerminalWidth() + " " + tui.getTerminalHeight());
    }
    private static void checkBounds(MiniTui tui) {
        for (MiniTui.Element element : tui.getElements()) {
            check(element.x >= 0 && element.y >= 0 && element.y < tui.getTerminalHeight() - 1, "anchor in viewport");
            if (element instanceof MiniTui.Box box) {
                check(box.x + box.width <= tui.getTerminalWidth() && box.y + box.height < tui.getTerminalHeight(), "box in viewport");
            }
        }
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
