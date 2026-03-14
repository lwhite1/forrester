package systems.courant.sd.app.canvas.charts;

import systems.courant.sd.model.graph.FeedbackAnalysis;
import systems.courant.sd.model.graph.LoopDominanceAnalysis;

import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TitledPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

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

    @Test
    @DisplayName("Series nodes receive loop colors after layout pass")
    @SuppressWarnings("unchecked")
    void seriesNodesReceiveLoopColors(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        AreaChart<Number, Number> chart = (AreaChart<Number, Number>)
                robot.lookup(".chart").queryAs(AreaChart.class);

        // Reinforcing loop (index 0) should get green fill with 40 alpha suffix
        XYChart.Series<Number, Number> reinforcingSeries = chart.getData().get(0);
        assertThat(reinforcingSeries.getNode()).isNotNull();
        assertThat(reinforcingSeries.getNode().getStyle()).contains("#27ae60");

        // Balancing loop (index 1) should get blue fill with 40 alpha suffix
        XYChart.Series<Number, Number> balancingSeries = chart.getData().get(1);
        assertThat(balancingSeries.getNode()).isNotNull();
        assertThat(balancingSeries.getNode().getStyle()).contains("#3498db");
    }

    @Test
    @DisplayName("Data point nodes are hidden after layout pass")
    @SuppressWarnings("unchecked")
    void dataPointNodesAreHidden(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();
        AreaChart<Number, Number> chart = (AreaChart<Number, Number>)
                robot.lookup(".chart").queryAs(AreaChart.class);

        for (XYChart.Series<Number, Number> series : chart.getData()) {
            for (XYChart.Data<Number, Number> data : series.getData()) {
                if (data.getNode() != null) {
                    assertThat(data.getNode().isVisible()).isFalse();
                }
            }
        }
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
