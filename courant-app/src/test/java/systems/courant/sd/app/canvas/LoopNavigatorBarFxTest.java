package systems.courant.sd.app.canvas;

import systems.courant.sd.model.graph.FeedbackAnalysis;
import systems.courant.sd.model.graph.FeedbackAnalysis.LoopType;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
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

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoopNavigatorBar (TestFX)")
@ExtendWith(ApplicationExtension.class)
class LoopNavigatorBarFxTest {

    private LoopNavigatorBar bar;

    @Start
    void start(Stage stage) {
        bar = new LoopNavigatorBar();
        stage.setScene(new Scene(new StackPane(bar), 800, 50));
        stage.show();
    }

    @Test
    @DisplayName("Bar has expected child buttons by ID")
    void hasExpectedButtons(FxRobot robot) {
        assertThat(robot.lookup("#loopPrev").tryQuery()).isPresent();
        assertThat(robot.lookup("#loopNext").tryQuery()).isPresent();
        assertThat(robot.lookup("#loopAll").tryQuery()).isPresent();
        assertThat(robot.lookup("#loopNavigatorLabel").tryQuery()).isPresent();
    }

    @Test
    @DisplayName("Filter All button is selected by default")
    void filterAllSelectedByDefault(FxRobot robot) {
        ToggleButton filterAll = robot.lookup("#filterAll").queryAs(ToggleButton.class);
        assertThat(filterAll.isSelected()).isTrue();
    }

    @Test
    @DisplayName("Filter buttons are mutually exclusive via ToggleGroup")
    void filterButtonsMutuallyExclusive(FxRobot robot) {
        robot.clickOn("#filterR");
        ToggleButton filterAll = robot.lookup("#filterAll").queryAs(ToggleButton.class);
        ToggleButton filterR = robot.lookup("#filterR").queryAs(ToggleButton.class);
        assertThat(filterR.isSelected()).isTrue();
        assertThat(filterAll.isSelected()).isFalse();
    }

    @Test
    @DisplayName("Previous button fires onPrev callback")
    void prevCallbackFires(FxRobot robot) {
        boolean[] fired = {false};
        bar.setOnPrev(() -> fired[0] = true);

        robot.clickOn("#loopPrev");
        assertThat(fired[0]).isTrue();
    }

    @Test
    @DisplayName("Next button fires onNext callback")
    void nextCallbackFires(FxRobot robot) {
        boolean[] fired = {false};
        bar.setOnNext(() -> fired[0] = true);

        robot.clickOn("#loopNext");
        assertThat(fired[0]).isTrue();
    }

    @Test
    @DisplayName("All button fires onShowAll callback")
    void allCallbackFires(FxRobot robot) {
        boolean[] fired = {false};
        bar.setOnShowAll(() -> fired[0] = true);

        robot.clickOn("#loopAll");
        assertThat(fired[0]).isTrue();
    }

    @Test
    @DisplayName("Filter change fires onFilterChanged callback")
    void filterChangeCallbackFires(FxRobot robot) {
        LoopType[] received = {null};
        bar.setOnFilterChanged(type -> received[0] = type);

        robot.clickOn("#filterR");
        assertThat(received[0]).isEqualTo(LoopType.REINFORCING);
    }

    @Test
    @DisplayName("Selecting Balancing filter reports BALANCING")
    void balancingFilter(FxRobot robot) {
        LoopType[] received = {null};
        bar.setOnFilterChanged(type -> received[0] = type);

        robot.clickOn("#filterB");
        assertThat(received[0]).isEqualTo(LoopType.BALANCING);
    }

    @Test
    @DisplayName("Re-selecting All filter reports null (all types)")
    void allFilterReportsNull(FxRobot robot) {
        LoopType[] received = {LoopType.REINFORCING}; // start non-null
        bar.setOnFilterChanged(type -> received[0] = type);

        robot.clickOn("#filterR");
        robot.clickOn("#filterAll");
        assertThat(received[0]).isNull();
    }

    @Test
    @DisplayName("update() with null analysis shows 'No loops detected'")
    void updateWithNullAnalysis(FxRobot robot) {
        Platform.runLater(() -> bar.update(null, -1, null, 0));
        WaitForAsyncUtils.waitForFxEvents();

        Label label = robot.lookup("#loopNavigatorLabel").queryAs(Label.class);
        assertThat(label.getText()).isEqualTo("No loops detected");
    }

    @Test
    @DisplayName("update() with empty analysis shows 'No loops detected'")
    void updateWithEmptyAnalysis(FxRobot robot) {
        FeedbackAnalysis empty = new FeedbackAnalysis(
                Collections.emptySet(), Collections.emptyList(),
                Collections.emptySet(), Collections.emptyList());
        Platform.runLater(() -> bar.update(empty, -1, null, 0));
        WaitForAsyncUtils.waitForFxEvents();

        Label label = robot.lookup("#loopNavigatorLabel").queryAs(Label.class);
        assertThat(label.getText()).isEqualTo("No loops detected");
    }

    @Test
    @DisplayName("update() with empty analysis disables navigation buttons")
    void emptyAnalysisDisablesButtons(FxRobot robot) {
        FeedbackAnalysis empty = new FeedbackAnalysis(
                Collections.emptySet(), Collections.emptyList(),
                Collections.emptySet(), Collections.emptyList());
        Platform.runLater(() -> bar.update(empty, -1, null, 0));
        WaitForAsyncUtils.waitForFxEvents();

        Button prev = robot.lookup("#loopPrev").queryAs(Button.class);
        Button next = robot.lookup("#loopNext").queryAs(Button.class);
        Button all = robot.lookup("#loopAll").queryAs(Button.class);
        assertThat(prev.isDisabled()).isTrue();
        assertThat(next.isDisabled()).isTrue();
        assertThat(all.isDisabled()).isTrue();
    }

    @Test
    @DisplayName("resetFilter() selects the All toggle")
    void resetFilterSelectsAll(FxRobot robot) {
        robot.clickOn("#filterR");
        Platform.runLater(() -> bar.resetFilter());
        WaitForAsyncUtils.waitForFxEvents();

        ToggleButton filterAll = robot.lookup("#filterAll").queryAs(ToggleButton.class);
        assertThat(filterAll.isSelected()).isTrue();
    }

    @Test
    @DisplayName("Help button exists")
    void helpButtonExists(FxRobot robot) {
        assertThat(robot.lookup("#loopHelpIcon").tryQuery()).isPresent();
    }

    @Nested
    @DisplayName("formatType()")
    class FormatType {

        @Test
        @DisplayName("Returns 'reinforcing' for REINFORCING")
        void reinforcing() {
            assertThat(LoopNavigatorBar.formatType(LoopType.REINFORCING))
                    .isEqualTo("reinforcing");
        }

        @Test
        @DisplayName("Returns 'balancing' for BALANCING")
        void balancing() {
            assertThat(LoopNavigatorBar.formatType(LoopType.BALANCING))
                    .isEqualTo("balancing");
        }

        @Test
        @DisplayName("Returns 'indeterminate' for INDETERMINATE")
        void indeterminate() {
            assertThat(LoopNavigatorBar.formatType(LoopType.INDETERMINATE))
                    .isEqualTo("indeterminate");
        }

        @Test
        @DisplayName("Returns null for null input")
        void nullInput() {
            assertThat(LoopNavigatorBar.formatType(null)).isNull();
        }
    }
}
