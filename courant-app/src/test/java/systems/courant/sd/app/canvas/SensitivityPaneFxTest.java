package systems.courant.sd.app.canvas;

import systems.courant.sd.sweep.SensitivitySummary.ParameterImpact;

import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SensitivityPane (TestFX)")
@ExtendWith(ApplicationExtension.class)
class SensitivityPaneFxTest {

    private SensitivityPane pane;

    @Start
    void start(Stage stage) {
        List<String> trackableNames = List.of("Stock1", "Stock2");

        BiFunction<String, Void, List<ParameterImpact>> impactComputer = (target, unused) ->
                List.of(
                        new ParameterImpact("alpha", target, 50.0, 150.0, 100.0, 0.6),
                        new ParameterImpact("beta", target, 80.0, 120.0, 100.0, 0.3),
                        new ParameterImpact("gamma", target, 90.0, 110.0, 100.0, 0.1)
                );

        pane = new SensitivityPane(trackableNames, impactComputer, "Stock1");
        stage.setScene(new Scene(pane, 800, 600));
        stage.show();
    }

    @Test
    @DisplayName("Pane contains a target variable ComboBox")
    void shouldContainVariableComboBox(FxRobot robot) {
        ComboBox<?> combo = robot.lookup(".combo-box").queryAs(ComboBox.class);
        assertThat(combo).isNotNull();
        assertThat(combo.getValue()).isEqualTo("Stock1");
        assertThat(combo.getItems().size()).isEqualTo(2);
        assertThat(combo.getItems().get(0)).isEqualTo("Stock1");
        assertThat(combo.getItems().get(1)).isEqualTo("Stock2");
    }

    @Test
    @DisplayName("Pane contains a BarChart (tornado chart)")
    void shouldContainBarChart(FxRobot robot) {
        BarChart<?, ?> chart = robot.lookup(".chart").queryAs(BarChart.class);
        assertThat(chart).isNotNull();
        assertThat(chart.getTitle()).contains("Sensitivity");
    }

    @Test
    @DisplayName("Bar chart has one series with all parameter impacts")
    void barChartHasCorrectData(FxRobot robot) {
        BarChart<?, ?> chart = robot.lookup(".chart").queryAs(BarChart.class);
        assertThat(chart.getData()).hasSize(1);
        assertThat(chart.getData().getFirst().getData()).hasSize(3);
    }

    @Test
    @DisplayName("Pane contains a summary TextFlow")
    void shouldContainSummaryText(FxRobot robot) {
        var textFlows = robot.from(pane).lookup((java.util.function.Predicate<javafx.scene.Node>) n -> n instanceof TextFlow)
                .queryAllAs(TextFlow.class);
        assertThat(textFlows).isNotEmpty();
    }

    @Test
    @DisplayName("Top bar contains a label")
    void topBarContainsLabel(FxRobot robot) {
        Set<Label> labels = robot.lookup(".label").queryAllAs(Label.class);
        boolean foundTarget = labels.stream()
                .anyMatch(l -> l.getText() != null && l.getText().contains("Target variable"));
        assertThat(foundTarget).isTrue();
    }

    @Test
    @DisplayName("Switching variable recomputes chart")
    void switchingVariableRecomputesChart(FxRobot robot) {
        ComboBox<String> combo = robot.lookup(".combo-box").queryAs(ComboBox.class);
        javafx.application.Platform.runLater(() -> combo.setValue("Stock2"));
        org.testfx.util.WaitForAsyncUtils.waitForFxEvents();

        BarChart<?, ?> chart = robot.lookup(".chart").queryAs(BarChart.class);
        assertThat(chart).isNotNull();
        assertThat(chart.getData()).hasSize(1);
    }
}
