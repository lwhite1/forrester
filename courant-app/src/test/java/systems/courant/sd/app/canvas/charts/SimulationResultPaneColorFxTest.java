package systems.courant.sd.app.canvas.charts;

import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

import systems.courant.sd.app.canvas.ChartUtils;
import systems.courant.sd.app.canvas.SimulationRunner;

@DisplayName("SimulationResultPane series color consistency (#1190)")
@ExtendWith(ApplicationExtension.class)
class SimulationResultPaneColorFxTest {

    private SimulationResultPane pane;

    @Start
    void start(Stage stage) {
        // Population is a stock (solid line), BirthRate is a variable (dashed line)
        SimulationRunner.SimulationResult result = new SimulationRunner.SimulationResult(
                List.of("Step", "Population", "BirthRate"),
                List.of(
                        new double[]{0, 100, 10},
                        new double[]{1, 200, 20},
                        new double[]{2, 400, 40}
                ),
                Map.of(),
                Set.of("Population")
        );
        pane = new SimulationResultPane(result, List.of(), List.of(), () -> {});
        stage.setScene(new Scene(pane, 900, 600));
        stage.show();
    }

    @Test
    @DisplayName("variable (dashed) series line color should match its label color")
    void variableSeriesLineColorShouldMatchLabelColor(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();

        TabPane tabPane = robot.lookup(".tab-pane").queryAs(TabPane.class);
        robot.interact(() -> tabPane.getSelectionModel().select(1));
        WaitForAsyncUtils.waitForFxEvents();

        // BirthRate is index 1 in currentSeries, so its color is SERIES_COLORS[1]
        String expectedColor = ChartUtils.SERIES_COLORS.get(1);

        // Verify label has the expected color
        Label birthRateLabel = robot.lookup("#seriesLabel-BirthRate").queryAs(Label.class);
        assertThat(birthRateLabel).isNotNull();
        assertThat(birthRateLabel.getStyle()).contains(expectedColor);

        // Verify the chart line has BOTH the expected color AND the dashed stroke
        LineChart<Number, Number> chart = robot.lookup(".chart").queryAs(LineChart.class);
        XYChart.Series<Number, Number> birthRateSeries = chart.getData().stream()
                .filter(s -> "BirthRate".equals(s.getName()))
                .findFirst().orElse(null);
        assertThat(birthRateSeries).isNotNull();
        assertThat(birthRateSeries.getNode()).isNotNull();

        String lineStyle = birthRateSeries.getNode().getStyle();
        assertThat(lineStyle)
                .as("Dashed variable line should retain its palette color")
                .contains(expectedColor);
        assertThat(lineStyle)
                .as("Variable line should have dashed stroke")
                .contains("-fx-stroke-dash-array");
    }

    @Test
    @DisplayName("stock (solid) series line color should match its label color")
    void stockSeriesLineColorShouldMatchLabelColor(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents();

        TabPane tabPane = robot.lookup(".tab-pane").queryAs(TabPane.class);
        robot.interact(() -> tabPane.getSelectionModel().select(1));
        WaitForAsyncUtils.waitForFxEvents();

        String expectedColor = ChartUtils.SERIES_COLORS.get(0);

        Label populationLabel = robot.lookup("#seriesLabel-Population").queryAs(Label.class);
        assertThat(populationLabel).isNotNull();
        assertThat(populationLabel.getStyle()).contains(expectedColor);

        LineChart<Number, Number> chart = robot.lookup(".chart").queryAs(LineChart.class);
        XYChart.Series<Number, Number> populationSeries = chart.getData().stream()
                .filter(s -> "Population".equals(s.getName()))
                .findFirst().orElse(null);
        assertThat(populationSeries).isNotNull();
        assertThat(populationSeries.getNode()).isNotNull();
        assertThat(populationSeries.getNode().getStyle()).contains(expectedColor);
    }
}
