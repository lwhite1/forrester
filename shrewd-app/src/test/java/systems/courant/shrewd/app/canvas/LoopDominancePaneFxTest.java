package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.graph.FeedbackAnalysis;
import systems.courant.shrewd.model.graph.LoopDominanceAnalysis;

import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.control.TitledPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoopDominancePane (TestFX)")
@ExtendWith(ApplicationExtension.class)
class LoopDominancePaneFxTest {

    private LoopDominancePane pane;

    @Start
    void start(Stage stage) {
        LoopDominanceAnalysis dominance = buildTestDominance();
        pane = new LoopDominancePane(dominance);
        stage.setScene(new Scene(pane, 800, 600));
        stage.show();
    }

    @Test
    @DisplayName("Pane contains an AreaChart")
    void shouldContainAreaChart(FxRobot robot) {
        AreaChart<?, ?> chart = robot.lookup(".chart").queryAs(AreaChart.class);
        assertThat(chart).isNotNull();
        assertThat(chart.getTitle()).isEqualTo("Loop Dominance");
    }

    @Test
    @DisplayName("Chart has series for each loop")
    void chartHasSeriesForEachLoop(FxRobot robot) {
        AreaChart<?, ?> chart = robot.lookup(".chart").queryAs(AreaChart.class);
        assertThat(chart.getData()).hasSize(2);
    }

    @Test
    @DisplayName("Pane contains a help TitledPane")
    void shouldContainHelpPane(FxRobot robot) {
        TitledPane help = robot.lookup(".titled-pane").queryAs(TitledPane.class);
        assertThat(help).isNotNull();
        assertThat(help.getText()).isEqualTo("How to read this chart");
        assertThat(help.isExpanded()).isFalse();
    }

    @Test
    @DisplayName("Pane has two children (chart and help)")
    void paneHasTwoChildren(FxRobot robot) {
        assertThat(pane.getChildren()).hasSize(2);
    }

    private static LoopDominanceAnalysis buildTestDominance() {
        // Two loops, 5 time steps
        double[][] activity = new double[2][5];
        // Loop 0: reinforcing, increasing activity
        activity[0] = new double[]{0, 10, 20, 30, 40};
        // Loop 1: balancing, decreasing activity
        activity[1] = new double[]{0, 40, 30, 20, 10};

        return new LoopDominanceAnalysis(
                List.of("R1: Growth", "B1: Decay"),
                List.of(FeedbackAnalysis.LoopType.REINFORCING, FeedbackAnalysis.LoopType.BALANCING),
                5,
                activity
        );
    }
}
