package systems.courant.sd.app.canvas;

import systems.courant.sd.Simulation;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.Stock;
import systems.courant.sd.sweep.MultiSweepResult;
import systems.courant.sd.sweep.RunResult;

import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static systems.courant.sd.measure.Units.GALLON_US;
import static systems.courant.sd.measure.Units.MINUTE;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MultiSweepResultPane (TestFX)")
@ExtendWith(ApplicationExtension.class)
class MultiSweepResultPaneFxTest {

    private MultiSweepResultPane pane;

    @Start
    void start(Stage stage) {
        MultiSweepResult result = buildMultiSweepResult();
        pane = new MultiSweepResultPane(result);
        stage.setScene(new Scene(pane, 800, 600));
        stage.show();
    }

    @Test
    @DisplayName("Pane contains a TabPane with Summary and Time Series tabs")
    void shouldContainTabPane(FxRobot robot) {
        TabPane tabPane = robot.lookup("#multiSweepTabs").queryAs(TabPane.class);
        assertThat(tabPane).isNotNull();
        assertThat(tabPane.getTabs()).hasSize(2);
        assertThat(tabPane.getTabs().get(0).getText()).isEqualTo("Summary");
        assertThat(tabPane.getTabs().get(1).getText()).isEqualTo("Time Series");
    }

    @Test
    @DisplayName("Summary table has rows matching run count")
    void summaryTableHasCorrectRowCount(FxRobot robot) {
        TableView<?> table = robot.lookup("#multiSweepSummaryTable").queryAs(TableView.class);
        assertThat(table).isNotNull();
        assertThat(table.getItems()).hasSize(4); // 2x2 parameter grid
    }

    @Test
    @DisplayName("Summary table has columns for parameters and stocks")
    void summaryTableHasCorrectColumns(FxRobot robot) {
        TableView<?> table = robot.lookup("#multiSweepSummaryTable").queryAs(TableView.class);
        // 2 params + 2 stock columns (final + max) = 4
        assertThat(table.getColumns().size()).isGreaterThanOrEqualTo(4);
    }

    @Test
    @DisplayName("Time Series tab contains a run combo box")
    void timeSeriesTabContainsRunCombo(FxRobot robot) {
        // Switch to Time Series tab
        TabPane tabPane = robot.lookup("#multiSweepTabs").queryAs(TabPane.class);
        javafx.application.Platform.runLater(() -> tabPane.getSelectionModel().select(1));
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

        var combo = robot.lookup("#multiSweepRunCombo").queryAs(javafx.scene.control.ComboBox.class);
        assertThat(combo).isNotNull();
        assertThat(combo.getItems()).hasSize(4);
    }

    private static MultiSweepResult buildMultiSweepResult() {
        List<RunResult> runs = new ArrayList<>();
        for (double rate : new double[]{5.0, 10.0}) {
            for (double initial : new double[]{100.0, 200.0}) {
                Map<String, Double> params = new LinkedHashMap<>();
                params.put("drainRate", rate);
                params.put("initialLevel", initial);
                runs.add(buildRunResult(params, rate, initial));
            }
        }
        return new MultiSweepResult(List.of("drainRate", "initialLevel"), runs);
    }

    private static RunResult buildRunResult(Map<String, Double> params,
                                            double drainRate, double initialLevel) {
        Model model = new Model("MultiSweep Test");
        Stock tank = new Stock("Tank", initialLevel, GALLON_US);
        Flow outflow = Flow.create("Drain", MINUTE, () -> new Quantity(drainRate, GALLON_US));
        tank.addOutflow(outflow);
        model.addStock(tank);

        RunResult rr = new RunResult(params);
        Simulation sim = new Simulation(model, MINUTE, MINUTE, 5);
        sim.addEventHandler(rr);
        sim.execute();
        return rr;
    }
}
