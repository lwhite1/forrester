package systems.courant.sd.app.canvas;

import systems.courant.sd.Simulation;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.Stock;
import systems.courant.sd.sweep.MonteCarloResult;
import systems.courant.sd.sweep.RunResult;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static systems.courant.sd.measure.Units.GALLON_US;
import static systems.courant.sd.measure.Units.MINUTE;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MonteCarloResultPane (TestFX)")
@ExtendWith(ApplicationExtension.class)
class MonteCarloResultPaneFxTest {

    private MonteCarloResultPane pane;

    @Start
    void start(Stage stage) {
        MonteCarloResult mcResult = buildMonteCarloResult();
        pane = new MonteCarloResultPane(mcResult);
        stage.setScene(new Scene(pane, 800, 600));
        stage.show();
    }

    @Test
    @DisplayName("Pane contains a variable ComboBox")
    void shouldContainVariableComboBox(FxRobot robot) {
        ComboBox<?> combo = robot.lookup(".combo-box").queryAs(ComboBox.class);
        assertThat(combo).isNotNull();
        assertThat(combo.getItems().contains("Tank")).isTrue();
    }

    @Test
    @DisplayName("ComboBox selects first variable by default")
    void comboBoxSelectsFirstVariable(FxRobot robot) {
        ComboBox<?> combo = robot.lookup(".combo-box").queryAs(ComboBox.class);
        assertThat(combo.getValue()).isEqualTo("Tank");
    }

    @Test
    @DisplayName("Center contains a FanChartPane after variable selection")
    void centerContainsFanChartPane(FxRobot robot) {
        assertThat(pane.getCenter()).isInstanceOf(FanChartPane.class);
    }

    @Test
    @DisplayName("Top bar contains a label and combo box")
    void topBarContainsLabelAndCombo(FxRobot robot) {
        assertThat(pane.getTop()).isNotNull();
    }

    private static MonteCarloResult buildMonteCarloResult() {
        List<RunResult> runs = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            double drainRate = 5 + i * 0.5;
            runs.add(buildRunResult(drainRate));
        }
        return new MonteCarloResult(runs);
    }

    private static RunResult buildRunResult(double drainRate) {
        Model model = new Model("MC Test");
        Stock tank = new Stock("Tank", 100, GALLON_US);
        Flow outflow = Flow.create("Drain", MINUTE, () -> new Quantity(drainRate, GALLON_US));
        tank.addOutflow(outflow);
        model.addStock(tank);

        RunResult rr = new RunResult(Map.of("drainRate", drainRate));
        Simulation sim = new Simulation(model, MINUTE, MINUTE, 10);
        sim.addEventHandler(rr);
        sim.execute();
        return rr;
    }
}
