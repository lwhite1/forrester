package systems.courant.sd.app.canvas;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for cross-cutting dialog improvements:
 * - #202: Screen-aware sizing
 * - #406: Standardized config dialog widths
 * - #408: Consistent fx:id values on controls
 */
@DisplayName("Dialog consistency")
@ExtendWith(ApplicationExtension.class)
class DialogConsistencyFxTest {

    @Start
    void start(Stage stage) {
        stage.setScene(new Scene(new StackPane(), 200, 200));
        stage.show();
    }

    @Nested
    @DisplayName("Standardized config dialog widths (#406)")
    class StandardizedWidths {

        @Test
        @DisplayName("SimulationSettingsDialog uses CONFIG_DIALOG_WIDTH")
        void simSettingsWidth(FxRobot robot) {
            DialogPane[] pane = {null};
            Platform.runLater(() -> {
                var d = new SimulationSettingsDialog(null);
                pane[0] = d.getDialogPane();
                d.show();
            });
            WaitForAsyncUtils.waitForFxEvents();
            assertThat(pane[0].getPrefWidth())
                    .isEqualTo(Styles.screenAwareWidth(Styles.CONFIG_DIALOG_WIDTH));
        }

        @Test
        @DisplayName("ParameterSweepDialog uses CONFIG_DIALOG_WIDTH")
        void sweepWidth(FxRobot robot) {
            DialogPane[] pane = {null};
            Platform.runLater(() -> {
                var d = new ParameterSweepDialog(List.of("a"), List.of("b"));
                pane[0] = d.getDialogPane();
                d.show();
            });
            WaitForAsyncUtils.waitForFxEvents();
            assertThat(pane[0].getPrefWidth())
                    .isEqualTo(Styles.screenAwareWidth(Styles.CONFIG_DIALOG_WIDTH));
        }

        @Test
        @DisplayName("MultiParameterSweepDialog uses CONFIG_DIALOG_WIDTH")
        void multiSweepWidth(FxRobot robot) {
            DialogPane[] pane = {null};
            Platform.runLater(() -> {
                var d = new MultiParameterSweepDialog(List.of("a", "b"));
                pane[0] = d.getDialogPane();
                d.show();
            });
            WaitForAsyncUtils.waitForFxEvents();
            assertThat(pane[0].getPrefWidth())
                    .isEqualTo(Styles.screenAwareWidth(Styles.CONFIG_DIALOG_WIDTH));
        }

        @Test
        @DisplayName("MonteCarloDialog uses CONFIG_DIALOG_WIDTH")
        void mcWidth(FxRobot robot) {
            DialogPane[] pane = {null};
            Platform.runLater(() -> {
                var d = new MonteCarloDialog(List.of("a"));
                pane[0] = d.getDialogPane();
                d.show();
            });
            WaitForAsyncUtils.waitForFxEvents();
            assertThat(pane[0].getPrefWidth())
                    .isEqualTo(Styles.screenAwareWidth(Styles.CONFIG_DIALOG_WIDTH));
        }

        @Test
        @DisplayName("OptimizerDialog uses CONFIG_DIALOG_WIDTH")
        void optWidth(FxRobot robot) {
            DialogPane[] pane = {null};
            Platform.runLater(() -> {
                var d = new OptimizerDialog(List.of("a"), List.of("Pop"));
                pane[0] = d.getDialogPane();
                d.show();
            });
            WaitForAsyncUtils.waitForFxEvents();
            assertThat(pane[0].getPrefWidth())
                    .isEqualTo(Styles.screenAwareWidth(Styles.CONFIG_DIALOG_WIDTH));
        }
    }

    @Nested
    @DisplayName("ParameterSweepDialog fx:id values (#408)")
    class SweepIds {

        @Test
        @DisplayName("all controls have fx:id")
        void allIdsPresent(FxRobot robot) {
            Platform.runLater(() -> {
                var d = new ParameterSweepDialog(List.of("alpha"), List.of("Pop"));
                d.show();
            });
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(robot.lookup("#sweepParameter").tryQuery()).isPresent();
            assertThat(robot.lookup("#sweepStart").tryQuery()).isPresent();
            assertThat(robot.lookup("#sweepEnd").tryQuery()).isPresent();
            assertThat(robot.lookup("#sweepStep").tryQuery()).isPresent();
            assertThat(robot.lookup("#sweepTrack").tryQuery()).isPresent();
            assertThat(robot.lookup("#sweepValidationLabel").tryQuery()).isPresent();
        }
    }

    @Nested
    @DisplayName("MonteCarloDialog fx:id values (#408)")
    class McIds {

        @Test
        @DisplayName("settings controls have fx:id")
        void settingsIdsPresent(FxRobot robot) {
            Platform.runLater(() -> {
                var d = new MonteCarloDialog(List.of("alpha"));
                d.show();
            });
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(robot.lookup("#mcIterations").tryQuery()).isPresent();
            assertThat(robot.lookup("#mcSampling").tryQuery()).isPresent();
            assertThat(robot.lookup("#mcSeed").tryQuery()).isPresent();
            assertThat(robot.lookup("#mcAddParam").tryQuery()).isPresent();
            assertThat(robot.lookup("#mcValidationLabel").tryQuery()).isPresent();
        }

        @Test
        @DisplayName("default parameter row has indexed fx:id values")
        void rowIdsPresent(FxRobot robot) {
            Platform.runLater(() -> {
                var d = new MonteCarloDialog(List.of("alpha"));
                d.show();
            });
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(robot.lookup("#mcParamName0").tryQuery()).isPresent();
            assertThat(robot.lookup("#mcParamDist0").tryQuery()).isPresent();
            assertThat(robot.lookup("#mcParam1_0").tryQuery()).isPresent();
            assertThat(robot.lookup("#mcParam2_0").tryQuery()).isPresent();
        }
    }

    @Nested
    @DisplayName("OptimizerDialog fx:id values (#408)")
    class OptIds {

        @Test
        @DisplayName("settings controls have fx:id")
        void settingsIdsPresent(FxRobot robot) {
            Platform.runLater(() -> {
                var d = new OptimizerDialog(List.of("alpha"), List.of("Pop"));
                d.show();
            });
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(robot.lookup("#optObjective").tryQuery()).isPresent();
            assertThat(robot.lookup("#optTargetVar").tryQuery()).isPresent();
            assertThat(robot.lookup("#optTargetValue").tryQuery()).isPresent();
            assertThat(robot.lookup("#optAlgorithm").tryQuery()).isPresent();
            assertThat(robot.lookup("#optMaxEvals").tryQuery()).isPresent();
            assertThat(robot.lookup("#optAddParam").tryQuery()).isPresent();
            assertThat(robot.lookup("#optimizerValidationLabel").tryQuery()).isPresent();
        }

        @Test
        @DisplayName("default parameter row has indexed fx:id values")
        void rowIdsPresent(FxRobot robot) {
            Platform.runLater(() -> {
                var d = new OptimizerDialog(List.of("alpha"), List.of("Pop"));
                d.show();
            });
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(robot.lookup("#optParamName0").tryQuery()).isPresent();
            assertThat(robot.lookup("#optLower0").tryQuery()).isPresent();
            assertThat(robot.lookup("#optUpper0").tryQuery()).isPresent();
            assertThat(robot.lookup("#optGuess0").tryQuery()).isPresent();
        }
    }
}
