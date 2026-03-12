package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.def.ModuleInterface;
import systems.courant.shrewd.model.def.PortDef;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DefinePortsDialog with existing ports (TestFX)")
@ExtendWith(ApplicationExtension.class)
class DefinePortsDialogPopulatedFxTest {

    private DialogPane dialogPane;

    @Start
    void start(Stage stage) {
        ModuleInterface existing = new ModuleInterface(
                List.of(new PortDef("inPort1", "units"), new PortDef("inPort2", null)),
                List.of(new PortDef("outPort1", "kg"))
        );
        DefinePortsDialog dialog = new DefinePortsDialog("Populated", existing);
        dialogPane = dialog.getDialogPane();
        stage.setScene(new Scene(new StackPane(dialogPane), 500, 600));
        stage.show();
    }

    @Test
    @DisplayName("Pre-populates input port names from existing interface")
    void populatesInputPortNames(FxRobot robot) {
        List<TextField> fields = robot.lookup(".text-field").queryAllAs(TextField.class)
                .stream().toList();
        // 3 ports x 2 fields = 6 text fields total
        assertThat(fields).hasSize(6);

        // First input port name
        assertThat(fields.get(0).getText()).isEqualTo("inPort1");
        // First input port unit
        assertThat(fields.get(1).getText()).isEqualTo("units");
    }

    @Test
    @DisplayName("Null unit is populated as empty string")
    void nullUnitBecomesEmpty(FxRobot robot) {
        List<TextField> fields = robot.lookup(".text-field").queryAllAs(TextField.class)
                .stream().toList();
        // Second input port unit (index 3)
        assertThat(fields.get(3).getText()).isEmpty();
    }

    @Test
    @DisplayName("Pre-populates output port from existing interface")
    void populatesOutputPort(FxRobot robot) {
        List<TextField> fields = robot.lookup(".text-field").queryAllAs(TextField.class)
                .stream().toList();
        // Output port name (index 4), unit (index 5)
        assertThat(fields.get(4).getText()).isEqualTo("outPort1");
        assertThat(fields.get(5).getText()).isEqualTo("kg");
    }
}
