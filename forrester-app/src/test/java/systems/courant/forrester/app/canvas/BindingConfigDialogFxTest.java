package systems.courant.forrester.app.canvas;

import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.ModuleInstanceDef;
import systems.courant.forrester.model.def.ModuleInterface;
import systems.courant.forrester.model.def.PortDef;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BindingConfigDialog (TestFX)")
@ExtendWith(ApplicationExtension.class)
class BindingConfigDialogFxTest {

    private BindingConfigDialog dialog;
    private DialogPane dialogPane;

    @Start
    void start(Stage stage) {
        stage.setScene(new Scene(new StackPane(), 200, 200));
        stage.show();
    }

    private ModuleInstanceDef createModule(List<PortDef> inputs, List<PortDef> outputs,
                                            Map<String, String> inputBindings,
                                            Map<String, String> outputBindings) {
        ModuleInterface iface = new ModuleInterface(inputs, outputs);
        ModelDefinition def = new ModelDefinition(
                "TestModule", null, iface,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), null);
        return new ModuleInstanceDef("myModule", def, inputBindings, outputBindings);
    }

    private void showDialog(ModuleInstanceDef module) {
        Platform.runLater(() -> {
            dialog = new BindingConfigDialog(module);
            dialogPane = dialog.getDialogPane();
            dialog.show();
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("Dialog opens with input and output port fields")
    void opensWithPorts(FxRobot robot) {
        ModuleInstanceDef mod = createModule(
                List.of(new PortDef("contactRate", "Person/Day", null)),
                List.of(new PortDef("infected", "Person", null)),
                Map.of(), Map.of());
        showDialog(mod);

        Set<TextField> fields = robot.lookup(".text-field").queryAllAs(TextField.class);
        assertThat(fields).hasSize(2); // one input + one output
    }

    @Test
    @DisplayName("Dialog pre-fills existing bindings")
    void preFillsExistingBindings(FxRobot robot) {
        ModuleInstanceDef mod = createModule(
                List.of(new PortDef("rate", null, null)),
                List.of(),
                Map.of("rate", "myVar * 2"),
                Map.of());
        showDialog(mod);

        Set<TextField> fields = robot.lookup(".text-field").queryAllAs(TextField.class);
        boolean foundBinding = fields.stream()
                .anyMatch(f -> "myVar * 2".equals(f.getText()));
        assertThat(foundBinding).isTrue();
    }

    @Test
    @DisplayName("Dialog shows (No input ports) when interface has no inputs")
    void noInputPortsMessage(FxRobot robot) {
        ModuleInstanceDef mod = createModule(
                List.of(),
                List.of(new PortDef("out", null, null)),
                Map.of(), Map.of());
        showDialog(mod);

        Set<Label> labels = robot.lookup(".label").queryAllAs(Label.class);
        boolean foundNoInputs = labels.stream()
                .anyMatch(l -> "(No input ports defined)".equals(l.getText()));
        assertThat(foundNoInputs).isTrue();
    }

    @Test
    @DisplayName("Dialog title includes module instance name")
    void titleIncludesModuleName(FxRobot robot) {
        ModuleInstanceDef mod = createModule(
                List.of(), List.of(), Map.of(), Map.of());
        showDialog(mod);

        assertThat(dialog.getTitle()).contains("myModule");
    }

    @Test
    @DisplayName("Port label includes unit when present")
    void portLabelIncludesUnit(FxRobot robot) {
        ModuleInstanceDef mod = createModule(
                List.of(new PortDef("rate", "Person/Day", null)),
                List.of(),
                Map.of(), Map.of());
        showDialog(mod);

        Set<Label> labels = robot.lookup(".label").queryAllAs(Label.class);
        boolean foundWithUnit = labels.stream()
                .anyMatch(l -> l.getText() != null && l.getText().contains("(Person/Day)"));
        assertThat(foundWithUnit).isTrue();
    }
}
