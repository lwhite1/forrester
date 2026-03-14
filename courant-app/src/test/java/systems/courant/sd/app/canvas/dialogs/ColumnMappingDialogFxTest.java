package systems.courant.sd.app.canvas.dialogs;

import systems.courant.sd.model.def.ReferenceDataset;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.StackPane;
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

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ColumnMappingDialog (TestFX)")
@ExtendWith(ApplicationExtension.class)
class ColumnMappingDialogFxTest {

    private ColumnMappingDialog dialog;

    @Start
    void start(Stage stage) {
        stage.setScene(new Scene(new StackPane(), 200, 200));
        stage.show();
    }

    private void showDialog(ReferenceDataset dataset, List<String> modelVars) {
        Platform.runLater(() -> {
            dialog = new ColumnMappingDialog(dataset, modelVars);
            dialog.show();
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("Should auto-match CSV columns to model variables by name")
    void shouldAutoMatchColumnsByName(FxRobot robot) {
        ReferenceDataset dataset = new ReferenceDataset("test",
                new double[]{0, 1, 2},
                Map.of("Population", new double[]{100, 110, 120},
                        "Revenue", new double[]{50, 55, 60}));
        showDialog(dataset, List.of("Population", "Revenue", "Cost"));

        // Check that Population combo auto-selected "Population"
        ComboBox<?> combo0 = robot.lookup("#mapping_0").queryAs(ComboBox.class);
        ComboBox<?> combo1 = robot.lookup("#mapping_1").queryAs(ComboBox.class);

        // One of them should be Population, one should be Revenue
        List<Object> values = List.of(combo0.getValue(), combo1.getValue());
        assertThat(values).contains("Population", "Revenue");
    }

    @Test
    @DisplayName("Should display CSV column labels")
    void shouldDisplayCsvColumnLabels(FxRobot robot) {
        ReferenceDataset dataset = new ReferenceDataset("test",
                new double[]{0, 1},
                Map.of("Infected", new double[]{10, 20}));
        showDialog(dataset, List.of("Susceptible", "Infected", "Recovered"));

        // The combo for "Infected" should auto-match
        ComboBox<?> combo = robot.lookup("#mapping_0").queryAs(ComboBox.class);
        assertThat(combo.getValue()).isEqualTo("Infected");
    }

    @Test
    @DisplayName("Should include skip option in combo boxes")
    void shouldIncludeSkipOption(FxRobot robot) {
        ReferenceDataset dataset = new ReferenceDataset("test",
                new double[]{0, 1},
                Map.of("X", new double[]{1, 2}));
        showDialog(dataset, List.of("A", "B"));

        @SuppressWarnings("unchecked")
        ComboBox<String> combo = robot.lookup("#mapping_0").queryAs(ComboBox.class);
        assertThat(combo.getItems()).contains("(skip)");
    }

    @Test
    @DisplayName("Should match with underscore/space normalization")
    void shouldMatchWithNormalization(FxRobot robot) {
        ReferenceDataset dataset = new ReferenceDataset("test",
                new double[]{0, 1},
                Map.of("Birth_Rate", new double[]{0.1, 0.2}));
        showDialog(dataset, List.of("Birth Rate", "Death Rate"));

        ComboBox<?> combo = robot.lookup("#mapping_0").queryAs(ComboBox.class);
        assertThat(combo.getValue()).isEqualTo("Birth Rate");
    }
}
