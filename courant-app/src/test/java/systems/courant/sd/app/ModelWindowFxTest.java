package systems.courant.sd.app;

import systems.courant.sd.app.canvas.Clipboard;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ModelWindow (TestFX)")
@ExtendWith(ApplicationExtension.class)
class ModelWindowFxTest {

    private Stage stage;

    @Start
    void start(Stage stage) {
        this.stage = stage;
        CourantApp app = new CourantApp();
        app.start(stage);
    }

    /**
     * Transition past the start screen into the editor by clicking "New Model".
     */
    private void enterEditor(FxRobot robot) {
        robot.clickOn("#startNewModel");
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("Window title is 'Courant' on start screen")
    void windowTitleOnStartScreen(FxRobot robot) {
        assertThat(stage.getTitle()).isEqualTo("Courant");
    }

    @Test
    @DisplayName("Start screen shows New Model, Open Model, and Tutorials cards")
    void startScreenCardsPresent(FxRobot robot) {
        assertThat(robot.lookup("#startNewModel").tryQuery()).isPresent();
        assertThat(robot.lookup("#startOpenModel").tryQuery()).isPresent();
        assertThat(robot.lookup("#startTutorials").tryQuery()).isPresent();
    }

    @Test
    @DisplayName("Window title contains 'Courant' after entering editor")
    void windowTitleContainsCourant(FxRobot robot) {
        enterEditor(robot);
        assertThat(stage.getTitle()).contains("Courant");
    }

    @Test
    @DisplayName("Root node is a BorderPane with expected ID")
    void rootNodeIsPresent(FxRobot robot) {
        BorderPane root = robot.lookup("#modelWindowRoot").queryAs(BorderPane.class);
        assertThat(root).isNotNull();
    }

    @Test
    @DisplayName("Toolbar is present with all tool buttons")
    void toolbarPresent(FxRobot robot) {
        enterEditor(robot);
        assertThat(robot.lookup("#toolSelect").tryQuery()).isPresent();
        assertThat(robot.lookup("#toolStock").tryQuery()).isPresent();
        assertThat(robot.lookup("#toolFlow").tryQuery()).isPresent();
        assertThat(robot.lookup("#toolAux").tryQuery()).isPresent();
        assertThat(robot.lookup("#toolModule").tryQuery()).isPresent();
        assertThat(robot.lookup("#toolLookup").tryQuery()).isPresent();
        assertThat(robot.lookup("#toolLoops").tryQuery()).isPresent();
        assertThat(robot.lookup("#toolValidate").tryQuery()).isPresent();
    }

    @Test
    @DisplayName("Status bar is present with default state")
    void statusBarPresent(FxRobot robot) {
        enterEditor(robot);
        assertThat(robot.lookup("#statusBar").tryQuery()).isPresent();

        Label toolLabel = robot.lookup("#statusTool").queryAs(Label.class);
        assertThat(toolLabel.getText()).isEqualTo("Select");

        Label elementsLabel = robot.lookup("#statusElements").queryAs(Label.class);
        assertThat(elementsLabel.getText()).isEqualTo("Empty model");
    }

    @Test
    @DisplayName("Properties panel shows model summary when nothing selected")
    void propertiesPanelModelSummary(FxRobot robot) {
        enterEditor(robot);
        TextField nameField = robot.lookup("#modelNameField").queryAs(TextField.class);
        assertThat(nameField).isNotNull();
        Label counts = robot.lookup("#modelElementCounts").queryAs(Label.class);
        assertThat(counts.getText()).isEqualTo("No elements yet");
    }

    @Test
    @DisplayName("Right tab pane has Properties and Dashboard tabs")
    void rightTabPaneTabs(FxRobot robot) {
        enterEditor(robot);
        TabPane tabPane = robot.lookup("#rightTabPane").queryAs(TabPane.class);
        assertThat(tabPane.getTabs()).hasSize(2);
        assertThat(tabPane.getTabs().get(0).getText()).isEqualTo("Properties");
        assertThat(tabPane.getTabs().get(1).getText()).isEqualTo("Dashboard");
    }

    @Test
    @DisplayName("Clicking a tool button changes the status bar tool label")
    void toolSelectionUpdatesStatusBar(FxRobot robot) {
        enterEditor(robot);
        robot.clickOn("#toolStock");
        WaitForAsyncUtils.waitForFxEvents();

        Label toolLabel = robot.lookup("#statusTool").queryAs(Label.class);
        assertThat(toolLabel.getText()).isEqualTo("Place Stock");
    }

    @Test
    @DisplayName("Menu bar has File, Edit, View, Layout, Simulate, and Help menus")
    void menuBarPresent(FxRobot robot) {
        MenuBar menuBar = robot.lookup(".menu-bar").queryAs(MenuBar.class);
        assertThat(menuBar.getMenus()).hasSize(6);
        assertThat(menuBar.getMenus().get(0).getText()).isEqualTo("File");
        assertThat(menuBar.getMenus().get(1).getText()).isEqualTo("Edit");
        assertThat(menuBar.getMenus().get(2).getText()).isEqualTo("View");
        assertThat(menuBar.getMenus().get(3).getText()).isEqualTo("Layout");
        assertThat(menuBar.getMenus().get(4).getText()).isEqualTo("Simulate");
        assertThat(menuBar.getMenus().get(5).getText()).isEqualTo("Help");
    }

    @Test
    @DisplayName("File > New resets the model to Untitled")
    void fileNewResetsModel(FxRobot robot) {
        enterEditor(robot);
        // First change something — select stock tool
        robot.clickOn("#toolStock");
        WaitForAsyncUtils.waitForFxEvents();

        // Use keyboard shortcut for New (Ctrl+N)
        robot.push(javafx.scene.input.KeyCode.CONTROL, javafx.scene.input.KeyCode.N);
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(stage.getTitle()).contains("Untitled");

        Label elementsLabel = robot.lookup("#statusElements").queryAs(Label.class);
        assertThat(elementsLabel.getText()).isEqualTo("Empty model");
    }

    @Test
    @DisplayName("Breadcrumb bar is hidden at root level")
    void breadcrumbHiddenAtRoot(FxRobot robot) {
        enterEditor(robot);
        assertThat(robot.lookup("#breadcrumbBar").queryAs(javafx.scene.Node.class).isVisible())
                .isFalse();
    }

    @Test
    @DisplayName("Close returns to start screen without closing window")
    void closeReturnsToStartScreen(FxRobot robot) {
        enterEditor(robot);
        // Verify we are in the editor
        assertThat(stage.getTitle()).contains("Untitled");

        // Fire Close via keyboard shortcut (Ctrl+W)
        robot.push(javafx.scene.input.KeyCode.CONTROL, javafx.scene.input.KeyCode.W);
        WaitForAsyncUtils.waitForFxEvents();

        // Window should still be showing
        assertThat(stage.isShowing()).isTrue();

        // Title should be the start screen title
        assertThat(stage.getTitle()).isEqualTo("Courant");

        // Start screen cards should be visible again
        assertThat(robot.lookup("#startNewModel").tryQuery()).isPresent();
        assertThat(robot.lookup("#startOpenModel").tryQuery()).isPresent();
        assertThat(robot.lookup("#startTutorials").tryQuery()).isPresent();
    }

    @Test
    @DisplayName("Close disables editor-only menu items")
    void closeDisablesEditorMenuItems(FxRobot robot) {
        enterEditor(robot);

        // Fire Close via keyboard shortcut
        robot.push(javafx.scene.input.KeyCode.CONTROL, javafx.scene.input.KeyCode.W);
        WaitForAsyncUtils.waitForFxEvents();

        // File menu should have Close and Save disabled
        MenuBar menuBar = robot.lookup(".menu-bar").queryAs(MenuBar.class);
        Menu fileMenu = menuBar.getMenus().get(0);
        MenuItem closeItem = fileMenu.getItems().stream()
                .filter(i -> "Close".equals(i.getText()))
                .findFirst().orElseThrow();
        assertThat(closeItem.isDisable()).isTrue();

        MenuItem saveItem = fileMenu.getItems().stream()
                .filter(i -> "Save".equals(i.getText()))
                .findFirst().orElseThrow();
        assertThat(saveItem.isDisable()).isTrue();

        // Edit menu should be disabled
        Menu editMenu = menuBar.getMenus().get(1);
        assertThat(editMenu.isDisable()).isTrue();
    }

    @Test
    @DisplayName("Can reopen editor after Close by clicking New Model")
    void canReopenAfterClose(FxRobot robot) {
        enterEditor(robot);

        // Close back to start screen
        robot.push(javafx.scene.input.KeyCode.CONTROL, javafx.scene.input.KeyCode.W);
        WaitForAsyncUtils.waitForFxEvents();

        // Click New Model again
        robot.clickOn("#startNewModel");
        WaitForAsyncUtils.waitForFxEvents();

        // Should be back in editor
        assertThat(stage.getTitle()).contains("Untitled");
        assertThat(robot.lookup("#toolSelect").tryQuery()).isPresent();

        Label elementsLabel = robot.lookup("#statusElements").queryAs(Label.class);
        assertThat(elementsLabel.getText()).isEqualTo("Empty model");
    }
}
