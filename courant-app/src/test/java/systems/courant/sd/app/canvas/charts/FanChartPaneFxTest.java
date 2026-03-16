package systems.courant.sd.app.canvas.charts;

import systems.courant.sd.Simulation;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.Stock;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.sweep.MonteCarloResult;
import systems.courant.sd.sweep.RunResult;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static systems.courant.sd.measure.Units.GALLON_US;
import static systems.courant.sd.measure.Units.MINUTE;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FanChartPane (TestFX)")
@ExtendWith(ApplicationExtension.class)
class FanChartPaneFxTest {

    private FanChartPane pane;

    @Start
    void start(Stage stage) {
        MonteCarloResult mcResult = buildMonteCarloResult();
        pane = new FanChartPane(mcResult, "Tank");
        stage.setScene(new Scene(pane, 800, 600));
        stage.show();
    }

    @Test
    @DisplayName("Pane contains a Canvas child")
    void shouldContainCanvas(FxRobot robot) {
        Canvas canvas = robot.from(pane).lookup((java.util.function.Predicate<javafx.scene.Node>) n -> n instanceof Canvas).queryAs(Canvas.class);
        assertThat(canvas).isNotNull();
    }

    @Test
    @DisplayName("Canvas has positive dimensions")
    void canvasHasPositiveDimensions(FxRobot robot) {
        Canvas canvas = robot.from(pane).lookup((java.util.function.Predicate<javafx.scene.Node>) n -> n instanceof Canvas).queryAs(Canvas.class);
        assertThat(canvas.getWidth()).isGreaterThan(0);
        assertThat(canvas.getHeight()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Pane has exactly one child")
    void paneHasOneChild(FxRobot robot) {
        assertThat(pane.getChildren()).hasSize(1);
        assertThat(pane.getChildren().getFirst()).isInstanceOf(Canvas.class);
    }

    @Test
    @DisplayName("Canvas should not overflow a smaller parent (#710)")
    void shouldNotOverflowSmallerParent(FxRobot robot) {
        MonteCarloResult mcResult = buildMonteCarloResult();
        FanChartPane smallPane = new FanChartPane(mcResult, "Tank");
        javafx.scene.layout.StackPane container = new javafx.scene.layout.StackPane(smallPane);
        container.setPrefSize(200, 150);
        container.setMaxSize(200, 150);

        javafx.application.Platform.runLater(() -> {
            Stage stage = new Stage();
            stage.setScene(new Scene(container, 200, 150));
            stage.show();
        });
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

        Canvas canvas = (Canvas) smallPane.getChildren().getFirst();
        assertThat(canvas.getWidth()).isLessThanOrEqualTo(201);
        assertThat(canvas.getHeight()).isLessThanOrEqualTo(151);
    }

    @Test
    @DisplayName("Redraw does not throw")
    void redrawDoesNotThrow(FxRobot robot) {
        MonteCarloResult mcResult = buildMonteCarloResult();
        javafx.application.Platform.runLater(() -> pane.redraw(mcResult, "Tank"));
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
        // No exception means success
        assertThat(pane.getChildren()).hasSize(1);
    }

    @Test
    @DisplayName("Redraw with missing variable does not throw (#894)")
    void redrawWithMissingVariableDoesNotThrow(FxRobot robot) {
        MonteCarloResult mcResult = buildMonteCarloResult();
        javafx.application.Platform.runLater(() -> pane.redraw(mcResult, "NonExistentVariable"));
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();
        // Should display a message instead of throwing
        assertThat(pane.getChildren()).hasSize(1);
    }

    private static MonteCarloResult buildMonteCarloResult() {
        List<RunResult> runs = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            double drainRate = 5 + i * 0.5;
            RunResult rr = buildRunResult(drainRate);
            runs.add(rr);
        }
        return new MonteCarloResult(runs);
    }

    static RunResult buildRunResult(double drainRate) {
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
