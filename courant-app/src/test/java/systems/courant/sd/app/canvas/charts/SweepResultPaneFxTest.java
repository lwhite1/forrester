package systems.courant.sd.app.canvas.charts;

import systems.courant.sd.Simulation;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.Stock;
import systems.courant.sd.sweep.RunResult;
import systems.courant.sd.sweep.SweepResult;

import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.ArrayList;
import java.util.List;

import static systems.courant.sd.measure.Units.GALLON_US;
import static systems.courant.sd.measure.Units.MINUTE;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SweepResultPane (TestFX)")
@ExtendWith(ApplicationExtension.class)
class SweepResultPaneFxTest {

    private SweepResultPane pane;

    @Start
    void start(Stage stage) {
        SweepResult result = buildSweepResult();
        pane = new SweepResultPane(result, "drainRate", "Day");
        stage.setScene(new Scene(pane, 800, 600));
        stage.show();
    }

    @Test
    @DisplayName("Pane contains a LineChart in center")
    void shouldContainLineChart(FxRobot robot) {
        LineChart<?, ?> chart = robot.lookup(".chart").queryAs(LineChart.class);
        assertThat(chart).isNotNull();
        assertThat(chart.animatedProperty().get()).isFalse();
    }

    @Test
    @DisplayName("Chart has one series per sweep run")
    void chartHasSeriesPerRun(FxRobot robot) {
        LineChart<?, ?> chart = robot.lookup(".chart").queryAs(LineChart.class);
        assertThat(chart.getData()).hasSize(3);
    }

    @Test
    @DisplayName("Top bar contains a ComboBox with variable names")
    void topBarContainsComboBox(FxRobot robot) {
        ComboBox<?> combo = robot.lookup(".combo-box").queryAs(ComboBox.class);
        assertThat(combo).isNotNull();
        assertThat(combo.getItems()).isNotEmpty();
        assertThat(combo.getValue()).isEqualTo("Tank");
    }

    @Test
    @DisplayName("Right sidebar contains checkboxes for series toggling")
    void rightSidebarContainsCheckboxes(FxRobot robot) {
        ScrollPane sidebar = robot.lookup(".scroll-pane").queryAs(ScrollPane.class);
        assertThat(sidebar).isNotNull();
    }

    private static SweepResult buildSweepResult() {
        List<RunResult> runs = new ArrayList<>();
        for (double rate : new double[]{5.0, 10.0, 15.0}) {
            runs.add(buildRunResult(rate));
        }
        return new SweepResult("drainRate", runs);
    }

    private static RunResult buildRunResult(double drainRate) {
        Model model = new Model("Sweep Test");
        Stock tank = new Stock("Tank", 100, GALLON_US);
        Flow outflow = Flow.create("Drain", MINUTE, () -> new Quantity(drainRate, GALLON_US));
        tank.addOutflow(outflow);
        model.addStock(tank);

        RunResult rr = new RunResult(drainRate);
        Simulation sim = new Simulation(model, MINUTE, MINUTE, 5);
        sim.addEventHandler(rr);
        sim.execute();
        return rr;
    }
}
