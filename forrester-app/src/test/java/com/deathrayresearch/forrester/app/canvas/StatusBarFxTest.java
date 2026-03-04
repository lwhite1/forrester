package com.deathrayresearch.forrester.app.canvas;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StatusBar (TestFX)")
@ExtendWith(ApplicationExtension.class)
class StatusBarFxTest {

    private StatusBar statusBar;

    @Start
    void start(Stage stage) {
        statusBar = new StatusBar();
        stage.setScene(new Scene(new StackPane(statusBar), 800, 50));
        stage.show();
    }

    @Test
    @DisplayName("Tool label shows Select by default")
    void defaultTool(FxRobot robot) {
        Label toolLabel = robot.lookup("#statusTool").queryAs(Label.class);
        assertThat(toolLabel.getText()).isEqualTo("Select");
    }

    @Test
    @DisplayName("Selection label shows no selection by default")
    void defaultSelection(FxRobot robot) {
        Label selectionLabel = robot.lookup("#statusSelection").queryAs(Label.class);
        assertThat(selectionLabel.getText()).isEqualTo("No selection");
    }

    @Test
    @DisplayName("Elements label shows empty model by default")
    void defaultElements(FxRobot robot) {
        Label elementsLabel = robot.lookup("#statusElements").queryAs(Label.class);
        assertThat(elementsLabel.getText()).isEqualTo("Empty model");
    }

    @Test
    @DisplayName("Zoom label shows 100% by default")
    void defaultZoom(FxRobot robot) {
        Label zoomLabel = robot.lookup("#statusZoom").queryAs(Label.class);
        assertThat(zoomLabel.getText()).isEqualTo("100%");
    }

    @Test
    @DisplayName("Updating tool changes the tool label")
    void updateTool(FxRobot robot) {
        Platform.runLater(() -> statusBar.updateTool(CanvasToolBar.Tool.PLACE_STOCK));
        WaitForAsyncUtils.waitForFxEvents();

        Label toolLabel = robot.lookup("#statusTool").queryAs(Label.class);
        assertThat(toolLabel.getText()).isEqualTo("Place Stock");
    }

    @Test
    @DisplayName("Updating selection count changes the selection label")
    void updateSelection(FxRobot robot) {
        Platform.runLater(() -> statusBar.updateSelection(3));
        WaitForAsyncUtils.waitForFxEvents();

        Label selectionLabel = robot.lookup("#statusSelection").queryAs(Label.class);
        assertThat(selectionLabel.getText()).isEqualTo("3 selected");
    }

    @Test
    @DisplayName("Single selection shows singular text")
    void singleSelection(FxRobot robot) {
        Platform.runLater(() -> statusBar.updateSelection(1));
        WaitForAsyncUtils.waitForFxEvents();

        Label selectionLabel = robot.lookup("#statusSelection").queryAs(Label.class);
        assertThat(selectionLabel.getText()).isEqualTo("1 selected");
    }

    @Test
    @DisplayName("Updating elements shows element summary")
    void updateElements(FxRobot robot) {
        Platform.runLater(() -> statusBar.updateElements(2, 3, 1, 4, 0));
        WaitForAsyncUtils.waitForFxEvents();

        Label elementsLabel = robot.lookup("#statusElements").queryAs(Label.class);
        assertThat(elementsLabel.getText()).contains("10 elements");
        assertThat(elementsLabel.getText()).contains("2 stocks");
        assertThat(elementsLabel.getText()).contains("3 flows");
    }

    @Test
    @DisplayName("Updating zoom changes the zoom label")
    void updateZoom(FxRobot robot) {
        Platform.runLater(() -> statusBar.updateZoom(1.5));
        WaitForAsyncUtils.waitForFxEvents();

        Label zoomLabel = robot.lookup("#statusZoom").queryAs(Label.class);
        assertThat(zoomLabel.getText()).isEqualTo("150%");
    }

    @Test
    @DisplayName("Loop status becomes visible when updated")
    void updateLoops(FxRobot robot) {
        Label loopLabel = robot.lookup("#statusLoops").queryAs(Label.class);
        assertThat(loopLabel.isVisible()).isFalse();

        Platform.runLater(() -> statusBar.updateLoops(3));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(loopLabel.isVisible()).isTrue();
        assertThat(loopLabel.getText()).isEqualTo("3 loops");
    }

    @Test
    @DisplayName("Clearing loops hides the loop label")
    void clearLoops(FxRobot robot) {
        Platform.runLater(() -> statusBar.updateLoops(2));
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> statusBar.clearLoops());
        WaitForAsyncUtils.waitForFxEvents();

        Label loopLabel = robot.lookup("#statusLoops").queryAs(Label.class);
        assertThat(loopLabel.isVisible()).isFalse();
    }

    @Test
    @DisplayName("Validation status becomes visible when updated")
    void updateValidation(FxRobot robot) {
        Label validationLabel = robot.lookup("#statusValidation").queryAs(Label.class);
        assertThat(validationLabel.isVisible()).isFalse();

        Platform.runLater(() -> statusBar.updateValidation(1, 2));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(validationLabel.isVisible()).isTrue();
        assertThat(validationLabel.getText()).contains("1 errors");
        assertThat(validationLabel.getText()).contains("2 warnings");
    }
}
