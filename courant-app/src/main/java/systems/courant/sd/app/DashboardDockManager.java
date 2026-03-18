package systems.courant.sd.app;

import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import systems.courant.sd.app.canvas.DashboardPanel;

/**
 * Manages the pop-out / dock lifecycle for the dashboard panel.
 * The dashboard can either live as a tab in the main window's right
 * pane or be detached into its own floating {@link Stage}.
 */
final class DashboardDockManager {

    private final DashboardPanel dashboardPanel;
    private final Tab dashboardTab;
    private final TabPane rightTabPane;
    private final MenuItem popOutDashboardItem;
    private final Stage ownerStage;
    private Stage dashboardStage;

    DashboardDockManager(DashboardPanel dashboardPanel, Tab dashboardTab,
                         TabPane rightTabPane, MenuItem popOutDashboardItem,
                         Stage ownerStage) {
        this.dashboardPanel = dashboardPanel;
        this.dashboardTab = dashboardTab;
        this.rightTabPane = rightTabPane;
        this.popOutDashboardItem = popOutDashboardItem;
        this.ownerStage = ownerStage;
    }

    void popOut(String modelName) {
        if (dashboardStage != null) {
            return;
        }
        rightTabPane.getTabs().remove(dashboardTab);

        dashboardStage = new Stage();
        dashboardStage.setTitle("Dashboard \u2014 " + modelName);
        dashboardStage.initOwner(ownerStage);

        BorderPane dashRoot = new BorderPane(dashboardPanel);
        Scene dashScene = new Scene(dashRoot, 600, 500);
        dashboardStage.setScene(dashScene);

        dashboardStage.setOnHidden(e -> {
            if (dashboardStage != null) {
                dock();
            }
        });

        dashboardStage.show();
        popOutDashboardItem.setText("Dock Dashboard");
    }

    void dock() {
        if (dashboardStage == null) {
            return;
        }
        Stage stageToClose = dashboardStage;
        dashboardStage = null;

        BorderPane dashRoot = (BorderPane) stageToClose.getScene().getRoot();
        dashRoot.setCenter(null);

        dashboardTab.setContent(dashboardPanel);
        if (!rightTabPane.getTabs().contains(dashboardTab)) {
            rightTabPane.getTabs().add(dashboardTab);
        }

        stageToClose.setOnHidden(null);
        stageToClose.close();

        popOutDashboardItem.setText("Pop Out Dashboard");
    }

    void switchTo() {
        if (dashboardStage != null) {
            dashboardStage.toFront();
            dashboardStage.requestFocus();
        } else if (rightTabPane != null
                && rightTabPane.getTabs().contains(dashboardTab)) {
            rightTabPane.getSelectionModel().select(dashboardTab);
        }
    }

    boolean isPoppedOut() {
        return dashboardStage != null;
    }

    /** Update the floating dashboard window title. No-op if docked. */
    void updateTitle(String modelName) {
        if (dashboardStage != null) {
            dashboardStage.setTitle("Dashboard \u2014 " + modelName);
        }
    }

    /** Close the floating dashboard stage if currently popped out. */
    void closePopout() {
        if (dashboardStage != null) {
            dashboardStage.setOnHidden(null);
            dashboardStage.close();
            dashboardStage = null;
        }
    }
}
