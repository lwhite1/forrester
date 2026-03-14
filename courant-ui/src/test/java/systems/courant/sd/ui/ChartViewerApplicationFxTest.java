package systems.courant.sd.ui;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.junit.jupiter.api.BeforeEach;
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

@DisplayName("ChartViewerApplication (TestFX)")
@ExtendWith(ApplicationExtension.class)
class ChartViewerApplicationFxTest {

    private ChartViewerApplication app;

    @Start
    void start(Stage stage) {
        // Clear any leftover state from previous tests or simulations
        ChartViewerApplication.reset();

        // Seed the static state, then snapshot via showChart-style flow
        ChartViewerApplication.setSeries(List.of("Population"), List.of("BirthRate"));
        ChartViewerApplication.addValues(List.of(100.0), List.of(5.0), 0);
        ChartViewerApplication.addValues(List.of(105.0), List.of(5.2), 1);
        ChartViewerApplication.addValues(List.of(110.0), List.of(5.5), 2);
        ChartViewerApplication.setSize(600, 400);

        app = new ChartViewerApplication();
        app.chartData = ChartViewerApplication.snapshot();
        app.start(stage);
    }

    @Test
    @DisplayName("Chart should show correct number of series")
    @SuppressWarnings("unchecked")
    void shouldShowCorrectSeriesCount(FxRobot robot) {
        LineChart<String, Number> chart = robot.lookup(".chart").queryAs(LineChart.class);
        assertThat(chart.getData()).hasSize(2);
    }

    @Test
    @DisplayName("Series should have correct names")
    @SuppressWarnings("unchecked")
    void shouldHaveCorrectSeriesNames(FxRobot robot) {
        LineChart<String, Number> chart = robot.lookup(".chart").queryAs(LineChart.class);
        List<String> names = chart.getData().stream()
                .map(XYChart.Series::getName)
                .toList();
        assertThat(names).containsExactly("Population", "BirthRate");
    }

    @Test
    @DisplayName("Series should contain data points")
    @SuppressWarnings("unchecked")
    void shouldContainDataPoints(FxRobot robot) {
        LineChart<String, Number> chart = robot.lookup(".chart").queryAs(LineChart.class);
        XYChart.Series<String, Number> populationSeries = chart.getData().get(0);
        assertThat(populationSeries.getData()).hasSize(3);
        assertThat(populationSeries.getData().get(0).getYValue().doubleValue()).isEqualTo(100.0);
        assertThat(populationSeries.getData().get(2).getYValue().doubleValue()).isEqualTo(110.0);
    }

    @Test
    @DisplayName("Sidebar should have checkboxes for each series")
    void shouldShowCheckboxes(FxRobot robot) {
        List<CheckBox> checkBoxes = robot.lookup(".check-box").queryAllAs(CheckBox.class)
                .stream().toList();
        assertThat(checkBoxes).hasSize(2);
        assertThat(checkBoxes.get(0).getText()).isEqualTo("Population");
        assertThat(checkBoxes.get(1).getText()).isEqualTo("BirthRate");
        assertThat(checkBoxes).allMatch(CheckBox::isSelected);
    }

    @Test
    @DisplayName("Unchecking a checkbox should remove its series from the chart")
    @SuppressWarnings("unchecked")
    void shouldRemoveSeriesWhenUnchecked(FxRobot robot) {
        CheckBox firstCheckBox = robot.lookup(".check-box").queryAs(CheckBox.class);
        robot.clickOn(firstCheckBox);
        WaitForAsyncUtils.waitForFxEvents();

        LineChart<String, Number> chart = robot.lookup(".chart").queryAs(LineChart.class);
        assertThat(chart.getData()).hasSize(1);
        assertThat(chart.getData().get(0).getName()).isEqualTo("BirthRate");
    }

    @Test
    @DisplayName("Re-checking a checkbox should re-add the series")
    @SuppressWarnings("unchecked")
    void shouldReAddSeriesWhenRechecked(FxRobot robot) {
        CheckBox firstCheckBox = robot.lookup(".check-box").queryAs(CheckBox.class);
        robot.clickOn(firstCheckBox); // uncheck
        WaitForAsyncUtils.waitForFxEvents();
        robot.clickOn(firstCheckBox); // re-check
        WaitForAsyncUtils.waitForFxEvents();

        LineChart<String, Number> chart = robot.lookup(".chart").queryAs(LineChart.class);
        assertThat(chart.getData()).hasSize(2);
    }

    @Test
    @DisplayName("Repeated toggle cycles should not accumulate duplicate series (#464)")
    @SuppressWarnings("unchecked")
    void shouldNotAccumulateDuplicatesOnRepeatedToggle(FxRobot robot) {
        CheckBox firstCheckBox = robot.lookup(".check-box").queryAs(CheckBox.class);

        // Toggle several times: uncheck → recheck → uncheck → recheck
        for (int i = 0; i < 5; i++) {
            robot.clickOn(firstCheckBox); // uncheck
            WaitForAsyncUtils.waitForFxEvents();
            robot.clickOn(firstCheckBox); // recheck
            WaitForAsyncUtils.waitForFxEvents();
        }

        LineChart<String, Number> chart = robot.lookup(".chart").queryAs(LineChart.class);
        assertThat(chart.getData())
                .as("Chart should still have exactly 2 series after repeated toggle cycles")
                .hasSize(2);
    }
}
