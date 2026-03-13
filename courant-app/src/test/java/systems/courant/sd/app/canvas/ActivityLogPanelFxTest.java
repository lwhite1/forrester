package systems.courant.sd.app.canvas;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
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

@DisplayName("ActivityLogPanel (TestFX)")
@ExtendWith(ApplicationExtension.class)
class ActivityLogPanelFxTest {

    private ActivityLogPanel panel;

    @Start
    void start(Stage stage) {
        panel = new ActivityLogPanel();
        stage.setScene(new Scene(new StackPane(panel), 300, 400));
        stage.show();
    }

    @Test
    @DisplayName("Panel uses centralized Styles constants (#77)")
    void usesCentralizedStyles(FxRobot robot) {
        assertThat(panel.getStyle()).isEqualTo(Styles.ACTIVITY_LOG_PANEL);
    }

    @Test
    @DisplayName("Panel starts with empty log")
    @SuppressWarnings("unchecked")
    void startsEmpty(FxRobot robot) {
        ListView<ActivityLogEntry> listView = robot.lookup(".list-view")
                .queryAs(ListView.class);
        assertThat(listView.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Logging an entry adds it to the list")
    @SuppressWarnings("unchecked")
    void logAddsEntry(FxRobot robot) {
        Platform.runLater(() -> panel.log("edit", "Added stock \"Population\""));
        WaitForAsyncUtils.waitForFxEvents();

        ListView<ActivityLogEntry> listView = robot.lookup(".list-view")
                .queryAs(ListView.class);
        assertThat(listView.getItems()).hasSize(1);
        assertThat(listView.getItems().getFirst().type()).isEqualTo("edit");
        assertThat(listView.getItems().getFirst().message()).isEqualTo("Added stock \"Population\"");
    }

    @Test
    @DisplayName("Multiple log entries accumulate in order")
    @SuppressWarnings("unchecked")
    void multipleEntriesAccumulate(FxRobot robot) {
        Platform.runLater(() -> {
            panel.log("edit", "First action");
            panel.log("simulation", "Second action");
            panel.log("file", "Third action");
        });
        WaitForAsyncUtils.waitForFxEvents();

        ListView<ActivityLogEntry> listView = robot.lookup(".list-view")
                .queryAs(ListView.class);
        assertThat(listView.getItems()).hasSize(3);
        assertThat(listView.getItems().get(0).message()).isEqualTo("First action");
        assertThat(listView.getItems().get(1).message()).isEqualTo("Second action");
        assertThat(listView.getItems().get(2).message()).isEqualTo("Third action");
    }

    @Test
    @DisplayName("createListener produces a working ModelEditListener")
    @SuppressWarnings("unchecked")
    void createListenerWorks(FxRobot robot) {
        ModelEditListener listener = panel.createListener();
        Platform.runLater(() -> {
            listener.onElementAdded("Population", "stock");
            listener.onSimulationRun();
        });
        WaitForAsyncUtils.waitForFxEvents();

        ListView<ActivityLogEntry> listView = robot.lookup(".list-view")
                .queryAs(ListView.class);
        assertThat(listView.getItems()).hasSize(2);
        assertThat(listView.getItems().get(0).message()).contains("Population");
        assertThat(listView.getItems().get(1).message()).contains("Simulation completed");
    }

    @Test
    @DisplayName("Listener onElementRemoved logs removal")
    @SuppressWarnings("unchecked")
    void listenerLogsRemoval(FxRobot robot) {
        ModelEditListener listener = panel.createListener();
        Platform.runLater(() -> listener.onElementRemoved("OldVar"));
        WaitForAsyncUtils.waitForFxEvents();

        ListView<ActivityLogEntry> listView = robot.lookup(".list-view")
                .queryAs(ListView.class);
        assertThat(listView.getItems()).hasSize(1);
        assertThat(listView.getItems().getFirst().message()).contains("OldVar");
    }

    @Test
    @DisplayName("Listener onElementRenamed logs rename")
    @SuppressWarnings("unchecked")
    void listenerLogsRename(FxRobot robot) {
        ModelEditListener listener = panel.createListener();
        Platform.runLater(() -> listener.onElementRenamed("oldName", "newName"));
        WaitForAsyncUtils.waitForFxEvents();

        ListView<ActivityLogEntry> listView = robot.lookup(".list-view")
                .queryAs(ListView.class);
        assertThat(listView.getItems()).hasSize(1);
        assertThat(listView.getItems().getFirst().message()).contains("oldName");
        assertThat(listView.getItems().getFirst().message()).contains("newName");
    }

    @Test
    @DisplayName("Listener onValidation logs error and warning counts")
    @SuppressWarnings("unchecked")
    void listenerLogsValidation(FxRobot robot) {
        ModelEditListener listener = panel.createListener();
        Platform.runLater(() -> listener.onValidation(2, 5));
        WaitForAsyncUtils.waitForFxEvents();

        ListView<ActivityLogEntry> listView = robot.lookup(".list-view")
                .queryAs(ListView.class);
        assertThat(listView.getItems()).hasSize(1);
        assertThat(listView.getItems().getFirst().message()).contains("2 errors");
        assertThat(listView.getItems().getFirst().message()).contains("5 warnings");
    }

    @Test
    @DisplayName("Listener onModelSaved logs filename")
    @SuppressWarnings("unchecked")
    void listenerLogsSave(FxRobot robot) {
        ModelEditListener listener = panel.createListener();
        Platform.runLater(() -> listener.onModelSaved("model.json"));
        WaitForAsyncUtils.waitForFxEvents();

        ListView<ActivityLogEntry> listView = robot.lookup(".list-view")
                .queryAs(ListView.class);
        assertThat(listView.getItems()).hasSize(1);
        assertThat(listView.getItems().getFirst().message()).contains("model.json");
    }

    @Test
    @DisplayName("Log entries are capped at 1000")
    @SuppressWarnings("unchecked")
    void entriesCappedAtMax(FxRobot robot) {
        Platform.runLater(() -> {
            for (int i = 0; i < 1010; i++) {
                panel.log("edit", "Entry " + i);
            }
        });
        WaitForAsyncUtils.waitForFxEvents();

        ListView<ActivityLogEntry> listView = robot.lookup(".list-view")
                .queryAs(ListView.class);
        assertThat(listView.getItems()).hasSize(1000);
        // Oldest entries should have been removed
        assertThat(listView.getItems().getFirst().message()).isEqualTo("Entry 10");
        assertThat(listView.getItems().getLast().message()).isEqualTo("Entry 1009");
    }
}
