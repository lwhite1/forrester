package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.def.ElementType;
import systems.courant.shrewd.model.def.ModelDefinition;
import systems.courant.shrewd.model.def.ModuleInstanceDef;
import systems.courant.shrewd.model.def.ModuleInterface;
import systems.courant.shrewd.model.def.PortDef;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HitTester port hit-testing")
class HitTesterPortTest {

    private CanvasState canvasState;
    private ModelEditor editor;

    private static final double MODULE_CX = 300;
    private static final double MODULE_CY = 200;

    @BeforeEach
    void setUp() {
        canvasState = new CanvasState();
        editor = new ModelEditor();

        // Add a module with one input and one output port
        ModuleInterface iface = new ModuleInterface(
                List.of(new PortDef("inPort", "units")),
                List.of(new PortDef("outPort", "units"))
        );
        ModelDefinition moduleDef = new ModelDefinition(
                "Module 1", null, iface,
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), null);
        editor.addModuleFrom(new ModuleInstanceDef("Module 1", moduleDef, Map.of(), Map.of()));
        canvasState.addElement("Module 1", ElementType.MODULE, MODULE_CX, MODULE_CY);
    }

    private double inputPortX() {
        return PortGeometry.inputPortX(MODULE_CX, LayoutMetrics.MODULE_WIDTH / 2);
    }

    private double outputPortX() {
        return PortGeometry.outputPortX(MODULE_CX, LayoutMetrics.MODULE_WIDTH / 2);
    }

    private double portY() {
        double halfH = LayoutMetrics.MODULE_HEIGHT / 2;
        return PortGeometry.portY(MODULE_CY - halfH, LayoutMetrics.MODULE_HEIGHT, 0, 1);
    }

    @Nested
    @DisplayName("input port")
    class InputPort {

        @Test
        void shouldHitInputPortAtCenter() {
            HitTester.PortHit hit = HitTester.hitTestPort(
                    canvasState, editor, inputPortX(), portY());
            assertThat(hit).isNotNull();
            assertThat(hit.moduleName()).isEqualTo("Module 1");
            assertThat(hit.portName()).isEqualTo("inPort");
            assertThat(hit.isInput()).isTrue();
        }

        @Test
        void shouldHitInputPortWithinTolerance() {
            HitTester.PortHit hit = HitTester.hitTestPort(
                    canvasState, editor, inputPortX() + 5, portY() + 5);
            assertThat(hit).isNotNull();
            assertThat(hit.portName()).isEqualTo("inPort");
            assertThat(hit.isInput()).isTrue();
        }

        @Test
        void shouldMissInputPortOutsideTolerance() {
            HitTester.PortHit hit = HitTester.hitTestPort(
                    canvasState, editor, inputPortX() + 20, portY());
            assertThat(hit).isNull();
        }
    }

    @Nested
    @DisplayName("output port")
    class OutputPort {

        @Test
        void shouldHitOutputPortAtCenter() {
            HitTester.PortHit hit = HitTester.hitTestPort(
                    canvasState, editor, outputPortX(), portY());
            assertThat(hit).isNotNull();
            assertThat(hit.moduleName()).isEqualTo("Module 1");
            assertThat(hit.portName()).isEqualTo("outPort");
            assertThat(hit.isInput()).isFalse();
        }

        @Test
        void shouldHitOutputPortWithinTolerance() {
            HitTester.PortHit hit = HitTester.hitTestPort(
                    canvasState, editor, outputPortX() - 3, portY() + 3);
            assertThat(hit).isNotNull();
            assertThat(hit.portName()).isEqualTo("outPort");
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        void shouldReturnNullWhenNoPortsDefined() {
            // Add a module with no ports
            ModelDefinition moduleDef = new ModelDefinition(
                    "Module 2", null, null,
                    List.of(), List.of(), List.of(),
                    List.of(), List.of(), List.of(), List.of(), null);
            editor.addModuleFrom(new ModuleInstanceDef("Module 2", moduleDef, Map.of(), Map.of()));
            canvasState.addElement("Module 2", ElementType.MODULE, 600, 200);

            HitTester.PortHit hit = HitTester.hitTestPort(canvasState, editor, 600, 200);
            assertThat(hit).isNull();
        }

        @Test
        void shouldReturnNullOnEmptySpace() {
            HitTester.PortHit hit = HitTester.hitTestPort(canvasState, editor, 800, 800);
            assertThat(hit).isNull();
        }
    }
}
