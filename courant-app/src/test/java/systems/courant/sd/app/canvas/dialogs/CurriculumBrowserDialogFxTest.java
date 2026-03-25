package systems.courant.sd.app.canvas.dialogs;

import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CurriculumBrowserDialog (TestFX)")
@ExtendWith(ApplicationExtension.class)
class CurriculumBrowserDialogFxTest {

    private CurriculumBrowserDialog dialog;

    @Start
    void start(Stage stage) {
        dialog = new CurriculumBrowserDialog();

        stage.setScene(new Scene(new StackPane(), 100, 100));
        stage.show();
    }

    @SuppressWarnings("unchecked")
    private TreeView<Object> tree() {
        return (TreeView<Object>) dialog.getScene().lookup("#curriculumTree");
    }

    @Test
    @DisplayName("dialog has correct title")
    void dialogHasCorrectTitle(FxRobot robot) {
        assertThat(dialog.getTitle()).isEqualTo("Tutorial Curriculum");
    }

    @Test
    @DisplayName("tree has two top-level track nodes")
    void treeHasTwoTrackNodes(FxRobot robot) {
        TreeView<Object> treeView = tree();
        assertThat(treeView.getRoot().getChildren()).hasSize(2);
    }

    @Test
    @DisplayName("modeling track has three tier children")
    void modelingTrackHasThreeTiers(FxRobot robot) {
        TreeItem<Object> modelingTrack = tree().getRoot().getChildren().getFirst();
        assertThat(modelingTrack.getChildren()).hasSize(3);
    }

    @Test
    @DisplayName("foundations tier has three tutorial children")
    void foundationsTierHasThreeTutorials(FxRobot robot) {
        TreeItem<Object> foundations = tree().getRoot().getChildren().getFirst()
                .getChildren().getFirst();
        assertThat(foundations.getChildren()).hasSize(3);
    }

    @Test
    @DisplayName("tutorial entries carry correct IDs")
    void tutorialEntriesHaveCorrectIds(FxRobot robot) {
        TreeItem<Object> foundations = tree().getRoot().getChildren().getFirst()
                .getChildren().getFirst();
        assertThat(foundations.getChildren().stream()
                .map(item -> ((CurriculumBrowserDialog.TutorialEntry) item.getValue()).tutorialId())
                .toList())
                .containsExactly("first-model", "feedback-loops", "cld-basics");
    }

    @Test
    @DisplayName("selecting a tutorial shows the launch button")
    void selectingTutorialShowsLaunchButton(FxRobot robot) {
        TreeItem<Object> firstTutorial = tree().getRoot().getChildren().getFirst()
                .getChildren().getFirst().getChildren().getFirst();

        robot.interact(() -> tree().getSelectionModel().select(firstTutorial));

        assertThat(dialog.getScene().lookup("#launchTutorialButton")).isNotNull();
    }

    @Test
    @DisplayName("launch button fires callback with correct path")
    void launchButtonFiresCallback(FxRobot robot) {
        String[] captured = {null};
        dialog.setOnLaunchTutorial(path -> captured[0] = path);

        TreeItem<Object> firstTutorial = tree().getRoot().getChildren().getFirst()
                .getChildren().getFirst().getChildren().getFirst();

        robot.interact(() -> tree().getSelectionModel().select(firstTutorial));
        robot.interact(() -> ((javafx.scene.control.Button)
                dialog.getScene().lookup("#launchTutorialButton")).fire());

        assertThat(captured[0]).isEqualTo("modeling/first-model.json");
    }

    @Test
    @DisplayName("empty tiers have no tutorial children")
    void emptyTiersHaveNoChildren(FxRobot robot) {
        // Advanced tier (3rd tier of modeling track) should be empty
        TreeItem<Object> advanced = tree().getRoot().getChildren().getFirst()
                .getChildren().get(2);
        assertThat(advanced.getChildren()).isEmpty();
    }
}
