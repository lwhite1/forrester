package systems.courant.forrester.app;

import systems.courant.forrester.app.canvas.Clipboard;

import javafx.scene.control.Label;
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
        ForresterApp app = new ForresterApp();
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
    @DisplayName("Window title is 'Forrester' on start screen")
    void windowTitleOnStartScreen(FxRobot robot) {
        assertThat(stage.getTitle()).isEqualTo("Forrester");
    }

    @Test
    @DisplayName("Start screen shows New Model, Open Model, and Getting Started cards")
    void startScreenCardsPresent(FxRobot robot) {
        assertThat(robot.lookup("#startNewModel").tryQuery()).isPresent();
        assertThat(robot.lookup("#startOpenModel").tryQuery()).isPresent();
        assertThat(robot.lookup("#startGettingStarted").tryQuery()).isPresent();
    }

    @Test
    @DisplayName("Window title contains 'Forrester' after entering editor")
    void windowTitleContainsForrester(FxRobot robot) {
        enterEditor(robot);
        assertThat(stage.getTitle()).contains("Forrester");
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
        assertThat(robot.lookup("#toolConstant").tryQuery()).isPresent();
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
    @DisplayName("Menu bar has File, Edit, View, Simulate, and Help menus")
    void menuBarPresent(FxRobot robot) {
        MenuBar menuBar = robot.lookup(".menu-bar").queryAs(MenuBar.class);
        assertThat(menuBar.getMenus()).hasSize(5);
        assertThat(menuBar.getMenus().get(0).getText()).isEqualTo("File");
        assertThat(menuBar.getMenus().get(1).getText()).isEqualTo("Edit");
        assertThat(menuBar.getMenus().get(2).getText()).isEqualTo("View");
        assertThat(menuBar.getMenus().get(3).getText()).isEqualTo("Simulate");
        assertThat(menuBar.getMenus().get(4).getText()).isEqualTo("Help");
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
}
