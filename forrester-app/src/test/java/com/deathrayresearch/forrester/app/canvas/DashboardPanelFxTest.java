package com.deathrayresearch.forrester.app.canvas;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DashboardPanel stale indicator (TestFX)")
@ExtendWith(ApplicationExtension.class)
class DashboardPanelFxTest {

    private DashboardPanel panel;

    @Start
    void start(Stage stage) {
        panel = new DashboardPanel();
        stage.setScene(new Scene(new StackPane(panel), 600, 400));
        stage.show();
    }

    private SimulationRunner.SimulationResult dummyResult() {
        return new SimulationRunner.SimulationResult(
                List.of("Step", "Stock1"),
                List.of(new double[]{0, 100}, new double[]{1, 110})
        );
    }

    @Test
    @DisplayName("Stale banner is hidden by default")
    void staleBannerHiddenByDefault(FxRobot robot) {
        HBox banner = robot.lookup("#staleBanner").queryAs(HBox.class);
        assertThat(banner.isVisible()).isFalse();
    }

    @Test
    @DisplayName("markStale has no effect when no results are showing")
    void markStaleWithNoResults(FxRobot robot) {
        Platform.runLater(() -> panel.markStale());
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(panel.isStale()).isFalse();
        HBox banner = robot.lookup("#staleBanner").queryAs(HBox.class);
        assertThat(banner.isVisible()).isFalse();
    }

    @Test
    @DisplayName("markStale shows banner when results exist")
    void markStaleWithResults(FxRobot robot) {
        Platform.runLater(() -> panel.showSimulationResult(dummyResult()));
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> panel.markStale());
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(panel.isStale()).isTrue();
        HBox banner = robot.lookup("#staleBanner").queryAs(HBox.class);
        assertThat(banner.isVisible()).isTrue();
        assertThat(banner.isManaged()).isTrue();

        TabPane tabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
        assertThat(tabs.getStyle()).contains("-fx-border-color: #F59E0B");
    }

    @Test
    @DisplayName("New simulation result clears stale indicator")
    void newResultClearsStale(FxRobot robot) {
        Platform.runLater(() -> panel.showSimulationResult(dummyResult()));
        WaitForAsyncUtils.waitForFxEvents();
        Platform.runLater(() -> panel.markStale());
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> panel.showSimulationResult(dummyResult()));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(panel.isStale()).isFalse();
        HBox banner = robot.lookup("#staleBanner").queryAs(HBox.class);
        assertThat(banner.isVisible()).isFalse();
    }

    @Test
    @DisplayName("clear() resets stale state")
    void clearResetsStale(FxRobot robot) {
        Platform.runLater(() -> panel.showSimulationResult(dummyResult()));
        WaitForAsyncUtils.waitForFxEvents();
        Platform.runLater(() -> panel.markStale());
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> panel.clear());
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(panel.isStale()).isFalse();
        HBox banner = robot.lookup("#staleBanner").queryAs(HBox.class);
        assertThat(banner.isVisible()).isFalse();
    }

    @Test
    @DisplayName("Re-run link invokes the configured action")
    void rerunLinkInvokesAction(FxRobot robot) {
        AtomicBoolean invoked = new AtomicBoolean(false);
        Platform.runLater(() -> {
            panel.setRerunAction(() -> invoked.set(true));
            panel.showSimulationResult(dummyResult());
        });
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> panel.markStale());
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("#staleRerunLink");
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(invoked).isTrue();
    }

    @Test
    @DisplayName("hasResults returns false initially and true after showing results")
    void hasResultsReflectsState(FxRobot robot) {
        assertThat(panel.hasResults()).isFalse();

        Platform.runLater(() -> panel.showSimulationResult(dummyResult()));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(panel.hasResults()).isTrue();
    }
}
