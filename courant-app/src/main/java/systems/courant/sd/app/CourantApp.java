package systems.courant.sd.app;

import systems.courant.sd.app.canvas.Clipboard;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * Application lifecycle manager. Creates and tracks open {@link ModelWindow}s.
 * Each window is an independent model editor with its own canvas, undo stack, and file state.
 */
public class CourantApp extends Application {

    private final List<ModelWindow> openWindows = new ArrayList<>();
    private final Clipboard clipboard = new Clipboard();

    @Override
    public void start(Stage primaryStage) {
        openWindow(primaryStage);
    }

    public void openNewWindow() {
        openWindow(new Stage());
    }

    private void openWindow(Stage stage) {
        ModelWindow window = new ModelWindow(stage, this, clipboard);
        openWindows.add(window);
        stage.setOnHidden(e -> {
            openWindows.remove(window);
            if (openWindows.isEmpty()) {
                // Close any remaining windows (help dialogs, etc.)
                List.copyOf(Window.getWindows()).forEach(w -> {
                    if (w instanceof Stage s) {
                        s.close();
                    }
                });
            }
        });
        stage.show();
        ensureWindowOnScreen(stage);
    }

    /**
     * Ensures that the window's title bar is within the visible bounds of at least one
     * active screen. If the title bar region is offscreen, the window is repositioned
     * to the center of the primary screen.
     */
    void ensureWindowOnScreen(Stage stage) {
        double titleBarHeight = 30;
        double x = stage.getX();
        double y = stage.getY();
        double width = stage.getWidth();

        // The title bar region: full window width, top edge down to titleBarHeight
        Rectangle2D titleBarRegion = new Rectangle2D(x, y, width, titleBarHeight);

        boolean titleBarVisible = Screen.getScreens().stream()
                .map(Screen::getVisualBounds)
                .anyMatch(bounds -> bounds.intersects(titleBarRegion));

        if (!titleBarVisible) {
            Rectangle2D primaryBounds = Screen.getPrimary().getVisualBounds();
            stage.setX(primaryBounds.getMinX()
                    + (primaryBounds.getWidth() - stage.getWidth()) / 2);
            stage.setY(primaryBounds.getMinY()
                    + (primaryBounds.getHeight() - stage.getHeight()) / 2);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
