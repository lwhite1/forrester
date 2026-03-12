package systems.courant.shrewd.app.canvas;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChartTimeCursor (#397)")
@ExtendWith(ApplicationExtension.class)
class ChartTimeCursorFxTest {

    private ChartTimeCursor cursor;
    private LineChart<Number, Number> chart;

    @Start
    void start(Stage stage) {
        NumberAxis xAxis = new NumberAxis(0, 100, 10);
        xAxis.setLabel("Step");
        NumberAxis yAxis = new NumberAxis(0, 200, 20);
        yAxis.setLabel("Value");

        chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Test");
        for (int i = 0; i <= 100; i++) {
            series.getData().add(new XYChart.Data<>(i, i * 2));
        }
        chart.getData().add(series);

        ChartTimeCursor[] holder = new ChartTimeCursor[1];
        StackPane wrapper = ChartTimeCursor.install(chart, holder);
        cursor = holder[0];

        stage.setScene(new Scene(wrapper, 600, 400));
        stage.show();
    }

    @Test
    @DisplayName("Cursor property defaults to NaN")
    void cursorDefaultsToNaN() {
        assertThat(cursor.cursorTimeStepProperty().get()).isNaN();
    }

    @Test
    @DisplayName("Cursor line is hidden by default")
    void cursorLineHiddenByDefault(FxRobot robot) {
        Line line = robot.lookup("#" + ChartTimeCursor.CURSOR_LINE_ID).queryAs(Line.class);
        assertThat(line.isVisible()).isFalse();
    }

    @Test
    @DisplayName("Setting cursor property programmatically shows the cursor line")
    void settingPropertyShowsCursorLine(FxRobot robot) {
        Platform.runLater(() -> cursor.cursorTimeStepProperty().set(50.0));
        WaitForAsyncUtils.waitForFxEvents();

        Line line = robot.lookup("#" + ChartTimeCursor.CURSOR_LINE_ID).queryAs(Line.class);
        assertThat(line.isVisible()).isTrue();
    }

    @Test
    @DisplayName("Setting cursor to NaN hides the cursor line")
    void settingNaNHidesCursorLine(FxRobot robot) {
        Platform.runLater(() -> cursor.cursorTimeStepProperty().set(50.0));
        WaitForAsyncUtils.waitForFxEvents();

        Platform.runLater(() -> cursor.cursorTimeStepProperty().set(Double.NaN));
        WaitForAsyncUtils.waitForFxEvents();

        Line line = robot.lookup("#" + ChartTimeCursor.CURSOR_LINE_ID).queryAs(Line.class);
        assertThat(line.isVisible()).isFalse();
    }

    @Test
    @DisplayName("Cursor label shows formatted time step")
    void cursorLabelShowsTimeStep(FxRobot robot) {
        Platform.runLater(() -> cursor.cursorTimeStepProperty().set(42.0));
        WaitForAsyncUtils.waitForFxEvents();

        javafx.scene.control.Label label = robot.lookup("#" + ChartTimeCursor.CURSOR_LABEL_ID)
                .queryAs(javafx.scene.control.Label.class);
        assertThat(label.isVisible()).isTrue();
        assertThat(label.getText()).contains("42");
    }

    @Test
    @DisplayName("Two cursors can be synchronized via listener")
    void twoChartsSynchronizeCursors(FxRobot robot) {
        // Create a second chart with its own cursor
        NumberAxis xAxis2 = new NumberAxis(0, 100, 10);
        NumberAxis yAxis2 = new NumberAxis(0, 50, 5);
        LineChart<Number, Number> chart2 = new LineChart<>(xAxis2, yAxis2);
        chart2.setAnimated(false);

        XYChart.Series<Number, Number> series2 = new XYChart.Series<>();
        for (int i = 0; i <= 100; i++) {
            series2.getData().add(new XYChart.Data<>(i, i / 2.0));
        }
        chart2.getData().add(series2);

        ChartTimeCursor[] holder2 = new ChartTimeCursor[1];
        ChartTimeCursor.install(chart2, holder2);
        ChartTimeCursor cursor2 = holder2[0];

        // Bind them together
        DoubleProperty prop1 = cursor.cursorTimeStepProperty();
        DoubleProperty prop2 = cursor2.cursorTimeStepProperty();
        prop1.addListener((obs, oldVal, newVal) -> prop2.set(newVal.doubleValue()));
        prop2.addListener((obs, oldVal, newVal) -> prop1.set(newVal.doubleValue()));

        // Setting one should update the other
        Platform.runLater(() -> prop1.set(75.0));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(prop2.get()).isEqualTo(75.0);

        // And vice versa
        Platform.runLater(() -> prop2.set(25.0));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(prop1.get()).isEqualTo(25.0);
    }

    @Test
    @DisplayName("Mouse hover over chart plot area updates cursor property")
    void mouseHoverUpdatesCursorProperty(FxRobot robot) {
        // Move to center of chart — should be over the plot area
        robot.moveTo(chart);
        WaitForAsyncUtils.waitForFxEvents();

        double value = cursor.cursorTimeStepProperty().get();
        // Should have a valid numeric value (not NaN) since we're over the chart
        assertThat(value).isNotNaN();
    }
}
