import java.util.ArrayList;
import java.util.List;

public class Test {

    public MiniTui tui = new MiniTui();

    private final List<MiniTui.Element> page1 = new ArrayList<>();
    private final List<MiniTui.Element> page2 = new ArrayList<>();

    MiniTui.Title globalTitle = tui.add(new MiniTui.Title(2, 1, 0, "Whale Cum"));

    MiniTui.Box page2Box = add(page2, new MiniTui.Box(2, 3, 0, 80, 20, "Good Choice", "\033[36m"));
    MiniTui.Label page2Labella = add(page2, new MiniTui.Label(5, 5, 0, "yasss"));

    MiniTui.Box page1Box = add(page1, new MiniTui.Box(2, 3, 0, 80, 20, "Jsme na:", "\033[36m"));
    MiniTui.Label page1Label = add(page1, new MiniTui.Label(5, 5, 0, "strance 1"));
    MiniTui.Label page1Labele = add(page1, new MiniTui.Label(5, 7, 0, "sex?"));
    MiniTui.Button page1SexButton = add(page1, new MiniTui.Button(5, 9, 0, "yeppa", () -> {show(page1, false); show(page2, true);}));

    private <T extends MiniTui.Element> T add(List<MiniTui.Element> group, T element) {
        group.add(element);
        return tui.add(element);
    }

    private void show(List<MiniTui.Element> group, boolean visible) {
        for (MiniTui.Element element : group) element.visible = visible;
    }

    public static void main(String[] args) {
        new Test().tui.start();
    }
}