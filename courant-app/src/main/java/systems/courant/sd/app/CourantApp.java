package systems.courant.sd.app;

import systems.courant.sd.app.canvas.Clipboard;

import javafx.application.Application;
import javafx.application.Platform;
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

    static final double DECORATION_ALLOWANCE = 40;

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
        centerOnPrimaryScreen(stage);
        stage.show();
        Platform.runLater(() -> ensureWindowOnScreen(stage));
    }

    /**
     * Sets the stage position to center on the primary screen before it is shown.
     * This prevents the OS window manager from placing the window with the title
     * bar or menu bar above the visible screen area.
     */
    void centerOnPrimaryScreen(Stage stage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        var scene = stage.getScene();
        double sceneWidth = scene != null ? scene.getWidth() : 1200;
        double sceneHeight = scene != null ? scene.getHeight() : 800;
        double windowHeight = sceneHeight + DECORATION_ALLOWANCE;

        stage.setX(Math.max(bounds.getMinX(),
                bounds.getMinX() + (bounds.getWidth() - sceneWidth) / 2));
        stage.setY(Math.max(bounds.getMinY(),
                bounds.getMinY() + (bounds.getHeight() - windowHeight) / 2));
    }

    /**
     * Ensures that the window's title bar and menu bar are within the visible bounds
     * of at least one active screen. The check region extends from the top of the
     * window decoration down through the menu bar area. If the window position
     * contains NaN values (not yet placed by the window manager) or the top region
     * is offscreen, the window is repositioned to the center of the primary screen.
     */
    void ensureWindowOnScreen(Stage stage) {
        double x = stage.getX();
        double y = stage.getY();
        double width = stage.getWidth();
        double height = stage.getHeight();

        if (Double.isNaN(x) || Double.isNaN(y)
                || Double.isNaN(width) || Double.isNaN(height)) {
            centerOnPrimaryScreen(stage);
            return;
        }

        // Check from the decoration top (above content) through the menu bar area.
        // stage.getY() is the content top on some platforms, so the native title
        // bar may sit above it.
        double regionTop = y - DECORATION_ALLOWANCE;
        double regionHeight = 2 * DECORATION_ALLOWANCE;
        Rectangle2D topRegion = new Rectangle2D(x, regionTop, width, regionHeight);

        boolean topVisible = Screen.getScreens().stream()
                .map(Screen::getVisualBounds)
                .anyMatch(bounds -> bounds.intersects(topRegion));

        if (!topVisible) {
            centerOnPrimaryScreen(stage);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
