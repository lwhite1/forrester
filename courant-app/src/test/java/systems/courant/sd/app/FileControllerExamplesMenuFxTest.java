package systems.courant.sd.app;

import systems.courant.sd.app.canvas.Clipboard;

import javafx.application.Platform;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileController.buildExamplesMenu() sorts categories alphabetically (#778)")
@ExtendWith(ApplicationExtension.class)
class FileControllerExamplesMenuFxTest {

    private ModelWindow window;

    @Start
    void start(Stage stage) {
        CourantApp app = new CourantApp();
        window = new ModelWindow(stage, app, new Clipboard());
        stage.show();
    }

    @Test
    @DisplayName("Example menu categories are sorted alphabetically")
    void shouldSortCategoriesAlphabetically() {
        AtomicReference<Menu> menuRef = new AtomicReference<>();
        Platform.runLater(() -> menuRef.set(window.getFileController().buildExamplesMenu()));
        WaitForAsyncUtils.waitForFxEvents();

        Menu menu = menuRef.get();
        assertThat(menu).isNotNull();

        // Extract category names from sub-menus
        List<String> categoryNames = new ArrayList<>();
        for (MenuItem item : menu.getItems()) {
            if (item instanceof Menu subMenu) {
                categoryNames.add(subMenu.getText());
            }
        }

        assertThat(categoryNames)
                .as("Example menu categories should be sorted alphabetically")
                .hasSizeGreaterThan(1)
                .isSorted();
    }
}
