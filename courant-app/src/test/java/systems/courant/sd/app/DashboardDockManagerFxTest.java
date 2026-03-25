package systems.courant.sd.app;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import systems.courant.sd.app.canvas.DashboardPanel;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DashboardDockManager (#1366)")
@ExtendWith(ApplicationExtension.class)
class DashboardDockManagerFxTest {

    private DashboardDockManager manager;
    private DashboardPanel dashboardPanel;
    private Tab dashboardTab;
    private TabPane rightTabPane;

    @Start
    void start(Stage stage) {
        dashboardPanel = new DashboardPanel();
        dashboardTab = new Tab("Dashboard", dashboardPanel);
        rightTabPane = new TabPane(dashboardTab);

        MenuItem popOutItem = new MenuItem("Pop Out Dashboard");

        manager = new DashboardDockManager(
                dashboardPanel, dashboardTab, rightTabPane, popOutItem, stage);

        stage.setScene(new Scene(new StackPane(rightTabPane), 400, 300));
        stage.show();
    }

    @Test
    @DisplayName("closePopout should restore dashboard panel to tab")
    void closePopoutShouldRestorePanel() {
        Platform.runLater(() -> manager.popOut("Test Model"));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(manager.isPoppedOut()).isTrue();
        assertThat(rightTabPane.getTabs()).doesNotContain(dashboardTab);

        Platform.runLater(() -> manager.closePopout());
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(manager.isPoppedOut()).isFalse();
        assertThat(rightTabPane.getTabs()).contains(dashboardTab);
        assertThat(dashboardTab.getContent()).isEqualTo(dashboardPanel);
    }

    @Test
    @DisplayName("closePopout when already docked should be a no-op")
    void closePopoutWhenDockedShouldBeNoop() {
        assertThat(manager.isPoppedOut()).isFalse();

        Platform.runLater(() -> manager.closePopout());
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(manager.isPoppedOut()).isFalse();
        assertThat(rightTabPane.getTabs()).contains(dashboardTab);
        assertThat(dashboardTab.getContent()).isEqualTo(dashboardPanel);
    }
}
