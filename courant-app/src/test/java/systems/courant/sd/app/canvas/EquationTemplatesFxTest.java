package systems.courant.sd.app.canvas;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EquationTemplates (TestFX)")
@ExtendWith(ApplicationExtension.class)
class EquationTemplatesFxTest {

    private ContextMenu menu;

    @Start
    void start(Stage stage) {
        // Create a minimal EquationField stub for template menu construction
        EquationField stubField = new EquationField() {
            private String text = "";

            @Override
            public String getText() {
                return text;
            }

            @Override
            public void setText(String text) {
                this.text = text;
            }

            @Override
            public void selectAll() {
            }

            @Override
            public int getCaretPosition() {
                return 0;
            }

            @Override
            public void positionCaret(int position) {
            }

            @Override
            public void requestFocus() {
            }

            @Override
            public javafx.beans.value.ObservableValue<String> textObservable() {
                return new javafx.beans.property.SimpleStringProperty(text);
            }

            @Override
            public javafx.beans.value.ObservableValue<Number> caretPositionObservable() {
                return new javafx.beans.property.SimpleIntegerProperty(0);
            }

            @Override
            public javafx.beans.property.ReadOnlyBooleanProperty focusedProperty() {
                return new javafx.beans.property.SimpleBooleanProperty(false);
            }

            @Override
            public javafx.scene.Node node() {
                return new javafx.scene.control.TextField();
            }

            @Override
            public void setFieldStyle(String style) {
            }

            @Override
            public void setOnAction(javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
            }

            @Override
            public javafx.event.EventHandler<javafx.event.ActionEvent> getOnAction() {
                return null;
            }
        };

        menu = EquationTemplates.createMenu(stubField);

        stage.setScene(new Scene(new StackPane(), 100, 100));
        stage.show();
    }

    @Test
    @DisplayName("createMenu returns a ContextMenu with 3 sub-menus")
    void createMenuHasThreeSubMenus(FxRobot robot) {
        assertThat(menu).isNotNull();
        assertThat(menu.getItems()).hasSize(3);
        assertThat(menu.getItems()).allSatisfy(item ->
                assertThat(item).isInstanceOf(Menu.class));
    }

    @Test
    @DisplayName("sub-menus have expected titles")
    void subMenusHaveExpectedTitles(FxRobot robot) {
        var titles = menu.getItems().stream()
                .map(MenuItem::getText)
                .toList();
        assertThat(titles).containsExactly("Flow patterns", "Variable patterns", "Math functions");
    }

    @Test
    @DisplayName("Flow patterns sub-menu has 6 items")
    void flowPatternsHasSixItems(FxRobot robot) {
        Menu flowPatterns = (Menu) menu.getItems().get(0);
        assertThat(flowPatterns.getItems()).hasSize(6);
    }

    @Test
    @DisplayName("Variable patterns sub-menu has 5 items")
    void variablePatternsHasFiveItems(FxRobot robot) {
        Menu auxPatterns = (Menu) menu.getItems().get(1);
        assertThat(auxPatterns.getItems()).hasSize(5);
    }

    @Test
    @DisplayName("Math functions sub-menu has 5 items")
    void mathPatternsHasFiveItems(FxRobot robot) {
        Menu mathPatterns = (Menu) menu.getItems().get(2);
        assertThat(mathPatterns.getItems()).hasSize(5);
    }

    @Test
    @DisplayName("template items contain arrow separator")
    void templateItemsContainArrow(FxRobot robot) {
        Menu flowPatterns = (Menu) menu.getItems().get(0);
        for (MenuItem item : flowPatterns.getItems()) {
            assertThat(item.getText()).contains("\u2192");
        }
    }
}
