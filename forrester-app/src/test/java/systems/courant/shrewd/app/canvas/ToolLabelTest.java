package systems.courant.shrewd.app.canvas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CanvasToolBar.Tool labels for undo history")
class ToolLabelTest {

    @Test
    @DisplayName("Each tool has a non-empty human-readable label")
    void shouldHaveNonEmptyLabels() {
        for (CanvasToolBar.Tool tool : CanvasToolBar.Tool.values()) {
            assertThat(tool.label())
                    .as("label for %s", tool.name())
                    .isNotBlank();
        }
    }

    @Test
    @DisplayName("Placement tools produce readable undo labels")
    void shouldProduceReadableUndoLabels() {
        assertThat(CanvasToolBar.Tool.PLACE_STOCK.label()).isEqualTo("stock");
        assertThat(CanvasToolBar.Tool.PLACE_FLOW.label()).isEqualTo("flow");
        assertThat(CanvasToolBar.Tool.PLACE_AUX.label()).isEqualTo("auxiliary");
        assertThat(CanvasToolBar.Tool.PLACE_MODULE.label()).isEqualTo("module");
        assertThat(CanvasToolBar.Tool.PLACE_LOOKUP.label()).isEqualTo("lookup");
        assertThat(CanvasToolBar.Tool.PLACE_CLD_VARIABLE.label()).isEqualTo("variable");
        assertThat(CanvasToolBar.Tool.PLACE_CAUSAL_LINK.label()).isEqualTo("causal link");
    }

    @Test
    @DisplayName("Add prefix with label reads naturally")
    void shouldReadNaturallyWithAddPrefix() {
        String label = "Add " + CanvasToolBar.Tool.PLACE_STOCK.label();
        assertThat(label).isEqualTo("Add stock");
    }
}
