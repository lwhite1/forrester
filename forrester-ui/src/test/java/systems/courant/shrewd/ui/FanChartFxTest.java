package systems.courant.forrester.ui;

import systems.courant.forrester.Simulation;
import systems.courant.forrester.model.Flow;
import systems.courant.forrester.model.Model;
import systems.courant.forrester.model.Stock;
import systems.courant.forrester.measure.Quantity;
import systems.courant.forrester.sweep.MonteCarloResult;
import systems.courant.forrester.sweep.RunResult;

import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static systems.courant.forrester.measure.Units.MINUTE;
import static systems.courant.forrester.measure.Units.THING;

@DisplayName("FanChart (TestFX)")
@ExtendWith(ApplicationExtension.class)
class FanChartFxTest {

    @Start
    void start(Stage stage) throws Exception {
        MonteCarloResult mcResult = buildMonteCarloResult(20, 10);

        // Set static fields via reflection (FanChart.show() calls Application.launch()
        // which cannot be used in TestFX)
        Field resultField = FanChart.class.getDeclaredField("pendingResult");
        resultField.setAccessible(true);
        resultField.set(null, mcResult);

        Field nameField = FanChart.class.getDeclaredField("pendingVariableName");
        nameField.setAccessible(true);
        nameField.set(null, "Population");

        FanChart fanChart = new FanChart();
        fanChart.start(stage);
    }

    @Test
    @DisplayName("Stage title includes variable name")
    void shouldSetStageTitle(FxRobot robot) {
        Stage stage = (Stage) robot.listWindows().getFirst();
        assertThat(stage.getTitle()).contains("Population");
    }

    @Test
    @DisplayName("Canvas is present in the scene")
    void shouldContainCanvas(FxRobot robot) {
        Stage stage = (Stage) robot.listWindows().getFirst();
        StackPane root = (StackPane) stage.getScene().getRoot();
        assertThat(root.getChildren()).hasSize(1);
        assertThat(root.getChildren().getFirst()).isInstanceOf(Canvas.class);
        Canvas canvas = (Canvas) root.getChildren().getFirst();
        assertThat(canvas.getWidth()).isGreaterThan(0);
        assertThat(canvas.getHeight()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Stage title includes Fan Chart text")
    void shouldShowFanChartTitle(FxRobot robot) {
        Stage stage = (Stage) robot.listWindows().getFirst();
        assertThat(stage.getTitle()).contains("Fan Chart");
    }

    private static MonteCarloResult buildMonteCarloResult(int runs, int steps) {
        List<RunResult> results = new ArrayList<>();
        for (int r = 0; r < runs; r++) {
            double growthRate = 0.05 + r * 0.005;
            Model model = new Model("TestModel");
            Stock pop = new Stock("Population", 100, THING);
            Flow growth = Flow.create("Growth", MINUTE,
                    () -> new Quantity(pop.getValue() * growthRate, THING));
            pop.addInflow(growth);
            model.addStock(pop);
            model.addFlow(growth);

            RunResult rr = new RunResult(Map.of("rate", growthRate));
            Simulation sim = new Simulation(model, MINUTE, MINUTE, steps);
            sim.addEventHandler(rr);
            sim.execute();
            results.add(rr);
        }
        return new MonteCarloResult(results);
    }
}
