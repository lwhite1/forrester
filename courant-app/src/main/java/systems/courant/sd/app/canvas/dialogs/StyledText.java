package systems.courant.sd.app.canvas.dialogs;

import javafx.scene.text.Text;

/**
 * Static factory methods for styled {@link Text} nodes used in tutorial
 * and reference dialogs. Eliminates the per-dialog duplication of
 * {@code bold()}, {@code plain()}, {@code mono()}, and {@code italic()} helpers.
 */
public final class StyledText {

    private StyledText() {
    }

    public static Text bold(String s) {
        Text t = new Text(s);
        t.setStyle("-fx-font-weight: bold;");
        return t;
    }

    public static Text plain(String s) {
        return new Text(s);
    }

    public static Text mono(String s) {
        Text t = new Text(s);
        t.setStyle("-fx-font-family: monospace; -fx-font-weight: bold;");
        return t;
    }

    public static Text italic(String s) {
        Text t = new Text(s);
        t.setStyle("-fx-font-style: italic;");
        return t;
    }
}
