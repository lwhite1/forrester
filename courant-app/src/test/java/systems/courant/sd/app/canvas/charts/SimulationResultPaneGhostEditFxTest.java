package systems.courant.sd.app.canvas.charts;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
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
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import systems.courant.sd.app.canvas.DashboardPanel;
import systems.courant.sd.app.canvas.SimulationRunner;

@DisplayName("SimulationResultPane ghost run name editing (#439)")
@ExtendWith(ApplicationExtension.class)
class SimulationResultPaneGhostEditFxTest {

    private DashboardPanel panel;

    private SimulationRunner.SimulationResult simulationResult() {
        return new SimulationRunner.SimulationResult(
                List.of("Step", "Population"),
                List.of(
                        new double[]{0, 100},
                        new double[]{1, 110}
                ),
                Map.of("Population", "Person"),
                Set.of("Population")
        );
    }

    @Start
    void start(Stage stage) {
        panel = new DashboardPanel();
        stage.setScene(new Scene(new StackPane(panel), 800, 600));
        stage.show();
    }

    private void switchToChartTab(FxRobot robot) {
        Platform.runLater(() -> {
            TabPane resultTabs = robot.lookup("#dashboardResultTabs").queryAs(TabPane.class);
            TabPane innerTabs = (TabPane) resultTabs.getTabs().getFirst().getContent()
                    .lookup(".tab-pane");
            if (innerTabs != null) {
                innerTabs.getSelectionModel().select(1); // Chart tab
            }
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("Double-click ghost label to edit, then commit restores label")
    void shouldEditGhostNameAndCommit(FxRobot robot) {
        // Two runs to create a ghost
        Platform.runLater(() -> panel.showSimulationResult(simulationResult()));
        WaitForAsyncUtils.waitForFxEvents();
        Platform.runLater(() -> panel.showSimulationResult(simulationResult()));
        WaitForAsyncUtils.waitForFxEvents();
        switchToChartTab(robot);

        // Find the ghost name label in the sidebar
        Label ghostHeader = robot.lookup("#ghostRunsHeader").queryAs(Label.class);
        assertThat(ghostHeader).isNotNull();

        // The ghost row HBox contains [CheckBox, Region(swatch), Label(name)]
        // Find the name label by looking for labels in ghost rows
        var ghostLabels = robot.lookup(".label").queryAllAs(Label.class).stream()
                .filter(l -> l.getParent() instanceof HBox
                        && ((HBox) l.getParent()).getChildren().size() == 3
                        && l.getStyle().contains("-fx-cursor: hand"))
                .toList();
        assertThat(ghostLabels).isNotEmpty();

        Label nameLabel = ghostLabels.getFirst();
        String originalName = nameLabel.getText();
        HBox parentRow = (HBox) nameLabel.getParent();

        // Double-click to enter edit mode
        robot.doubleClickOn(nameLabel);
        WaitForAsyncUtils.waitForFxEvents();

        // Editor should replace the label
        var textFields = parentRow.getChildren().stream()
                .filter(n -> n instanceof TextField)
                .map(n -> (TextField) n)
                .toList();
        assertThat(textFields).hasSize(1);
        TextField editor = textFields.getFirst();

        // Type a new name and press enter to commit
        Platform.runLater(() -> {
            editor.selectAll();
            editor.setText("Renamed Run");
        });
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> editor.getOnAction().handle(new javafx.event.ActionEvent()));
        WaitForAsyncUtils.waitForFxEvents();

        // Label should be back with the new name
        assertThat(parentRow.getChildren()).contains(nameLabel);
        assertThat(nameLabel.getText()).isEqualTo("Renamed Run");
    }

    @Test
    @DisplayName("Commit after sidebar modification does not throw AIOOBE")
    void shouldNotThrowWhenSidebarChangedDuringEdit(FxRobot robot) {
        // Two runs to create a ghost
        Platform.runLater(() -> panel.showSimulationResult(simulationResult()));
        WaitForAsyncUtils.waitForFxEvents();
        Platform.runLater(() -> panel.showSimulationResult(simulationResult()));
        WaitForAsyncUtils.waitForFxEvents();
        switchToChartTab(robot);

        // Find the ghost name label
        var ghostLabels = robot.lookup(".label").queryAllAs(Label.class).stream()
                .filter(l -> l.getParent() instanceof HBox
                        && ((HBox) l.getParent()).getChildren().size() == 3
                        && l.getStyle().contains("-fx-cursor: hand"))
                .toList();
        assertThat(ghostLabels).isNotEmpty();

        Label nameLabel = ghostLabels.getFirst();
        HBox parentRow = (HBox) nameLabel.getParent();

        // Double-click to enter edit mode
        robot.doubleClickOn(nameLabel);
        WaitForAsyncUtils.waitForFxEvents();

        var textFields = parentRow.getChildren().stream()
                .filter(n -> n instanceof TextField)
                .map(n -> (TextField) n)
                .toList();
        assertThat(textFields).hasSize(1);
        TextField editor = textFields.getFirst();

        // Simulate sidebar modification: insert an extra node before the editor
        // This shifts the editor's index, making the old labelIndex stale
        Platform.runLater(() -> parentRow.getChildren().addFirst(new Label("extra")));
        WaitForAsyncUtils.waitForFxEvents();

        // Commit should not throw IndexOutOfBoundsException
        assertThatCode(() -> {
            Platform.runLater(() -> editor.getOnAction().handle(new javafx.event.ActionEvent()));
            WaitForAsyncUtils.waitForFxEvents();
        }).doesNotThrowAnyException();

        // Label should still be restored
        assertThat(parentRow.getChildren()).contains(nameLabel);
    }
}
