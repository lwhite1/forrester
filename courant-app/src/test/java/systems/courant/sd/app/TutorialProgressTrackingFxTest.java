package systems.courant.sd.app;

import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import systems.courant.sd.app.canvas.dialogs.ContentTutorialDialog;
import systems.courant.sd.app.canvas.dialogs.TutorialContentLoader;

import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests verifying that tutorial dialogs interact correctly
 * with {@link TutorialProgressStore} for completion tracking and resume.
 */
@DisplayName("Tutorial Progress Tracking (TestFX)")
@ExtendWith(ApplicationExtension.class)
class TutorialProgressTrackingFxTest {

    private Preferences testPrefs;

    @Start
    void start(Stage stage) {
        stage.setScene(new Scene(new StackPane(), 100, 100));
        stage.show();
    }

    @BeforeEach
    void setUp() throws Exception {
        testPrefs = Preferences.userRoot().node("/test/courant/tutorial-progress-fx");
        testPrefs.clear();
        TutorialProgressStore.setPreferences(testPrefs);
    }

    @AfterEach
    void tearDown() throws Exception {
        TutorialProgressStore.resetProgress();
        testPrefs.removeNode();
        TutorialProgressStore.restoreDefaultPreferences();
    }

    private static ContentTutorialDialog loadDialog(String jsonPath) {
        return new ContentTutorialDialog(TutorialContentLoader.load(jsonPath));
    }

    @Nested
    @DisplayName("Completion")
    class Completion {

        @Test
        @DisplayName("reaching the last tab marks tutorial completed")
        void shouldMarkCompletedOnLastTab(FxRobot robot) {
            robot.interact(() -> {
                ContentTutorialDialog dialog = loadDialog("modeling/feedback-loops.json");
                TabPane tabPane = (TabPane) dialog.getScene().getRoot();
                tabPane.getSelectionModel().selectLast();
            });

            assertThat(TutorialProgressStore.isCompleted("feedback-loops")).isTrue();
        }

        @Test
        @DisplayName("middle tab does not mark completed")
        void shouldNotMarkCompletedOnMiddleTab(FxRobot robot) {
            robot.interact(() -> {
                ContentTutorialDialog dialog = loadDialog("modeling/feedback-loops.json");
                TabPane tabPane = (TabPane) dialog.getScene().getRoot();
                tabPane.getSelectionModel().select(3);
            });

            assertThat(TutorialProgressStore.isCompleted("feedback-loops")).isFalse();
        }

        @Test
        @DisplayName("first-model completes as 'first-model'")
        void firstModelCompletesCorrectly(FxRobot robot) {
            robot.interact(() -> {
                ContentTutorialDialog dialog = loadDialog("modeling/first-model.json");
                TabPane tabPane = (TabPane) dialog.getScene().getRoot();
                tabPane.getSelectionModel().selectLast();
            });

            assertThat(TutorialProgressStore.isCompleted("first-model")).isTrue();
        }

        @Test
        @DisplayName("delays completes as 'delays'")
        void delaysCompletesCorrectly(FxRobot robot) {
            robot.interact(() -> {
                ContentTutorialDialog dialog = loadDialog("modeling/delays.json");
                TabPane tabPane = (TabPane) dialog.getScene().getRoot();
                tabPane.getSelectionModel().selectLast();
            });

            assertThat(TutorialProgressStore.isCompleted("delays")).isTrue();
        }

        @Test
        @DisplayName("cld-basics completes as 'cld-basics'")
        void cldBasicsCompletesCorrectly(FxRobot robot) {
            robot.interact(() -> {
                ContentTutorialDialog dialog = loadDialog("modeling/cld-basics.json");
                TabPane tabPane = (TabPane) dialog.getScene().getRoot();
                tabPane.getSelectionModel().selectLast();
            });

            assertThat(TutorialProgressStore.isCompleted("cld-basics")).isTrue();
        }
    }

    @Nested
    @DisplayName("Resume point")
    class Resume {

        @Test
        @DisplayName("selecting a middle tab sets resume point")
        void shouldSetResumePointOnMiddleTab(FxRobot robot) {
            robot.interact(() -> {
                ContentTutorialDialog dialog = loadDialog("modeling/feedback-loops.json");
                TabPane tabPane = (TabPane) dialog.getScene().getRoot();
                tabPane.getSelectionModel().select(3);
            });

            var rp = TutorialProgressStore.getResumePoint();
            assertThat(rp).isPresent();
            assertThat(rp.get().tutorialId()).isEqualTo("feedback-loops");
            assertThat(rp.get().stepIndex()).isEqualTo(3);
        }

        @Test
        @DisplayName("completing a tutorial clears the resume point")
        void shouldClearResumePointOnCompletion(FxRobot robot) {
            robot.interact(() -> {
                ContentTutorialDialog dialog = loadDialog("modeling/feedback-loops.json");
                TabPane tabPane = (TabPane) dialog.getScene().getRoot();
                tabPane.getSelectionModel().select(3);
            });
            assertThat(TutorialProgressStore.getResumePoint()).isPresent();

            robot.interact(() -> {
                ContentTutorialDialog dialog = loadDialog("modeling/feedback-loops.json");
                TabPane tabPane = (TabPane) dialog.getScene().getRoot();
                tabPane.getSelectionModel().selectLast();
            });

            assertThat(TutorialProgressStore.getResumePoint()).isEmpty();
        }

        @Test
        @DisplayName("dialog resumes at the saved step")
        void shouldResumeAtSavedStep(FxRobot robot) {
            TutorialProgressStore.setResumePoint("feedback-loops", 4);

            int[] selectedIndex = {-1};
            robot.interact(() -> {
                ContentTutorialDialog dialog = loadDialog("modeling/feedback-loops.json");
                TabPane tabPane = (TabPane) dialog.getScene().getRoot();
                selectedIndex[0] = tabPane.getSelectionModel().getSelectedIndex();
            });

            assertThat(selectedIndex[0]).isEqualTo(4);
        }

        @Test
        @DisplayName("resume point for different tutorial is ignored")
        void shouldIgnoreResumePointForDifferentTutorial(FxRobot robot) {
            TutorialProgressStore.setResumePoint("supply-chain", 3);

            int[] selectedIndex = {-1};
            robot.interact(() -> {
                ContentTutorialDialog dialog = loadDialog("modeling/feedback-loops.json");
                TabPane tabPane = (TabPane) dialog.getScene().getRoot();
                selectedIndex[0] = tabPane.getSelectionModel().getSelectedIndex();
            });

            assertThat(selectedIndex[0]).isZero();
        }

        @Test
        @DisplayName("resume point beyond tab count is clamped")
        void shouldClampResumePointBeyondTabCount(FxRobot robot) {
            TutorialProgressStore.setResumePoint("feedback-loops", 99);

            int[] selectedIndex = {-1};
            robot.interact(() -> {
                ContentTutorialDialog dialog = loadDialog("modeling/feedback-loops.json");
                TabPane tabPane = (TabPane) dialog.getScene().getRoot();
                selectedIndex[0] = tabPane.getSelectionModel().getSelectedIndex();
            });

            // feedback-loops has 7 tabs (0-6), so clamped to 6 (the last tab)
            assertThat(selectedIndex[0]).isEqualTo(6);
        }
    }
}
