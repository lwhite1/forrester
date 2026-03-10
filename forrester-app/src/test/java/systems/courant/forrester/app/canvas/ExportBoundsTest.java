package systems.courant.forrester.app.canvas;

import systems.courant.forrester.model.def.ElementType;
import systems.courant.forrester.model.def.ModelDefinitionBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExportBounds")
class ExportBoundsTest {

    @Test
    void shouldComputeBoundsForSingleElement() {
        CanvasState state = new CanvasState();
        state.addElement("Stock 1", ElementType.STOCK, 200, 300);

        ModelEditor editor = new ModelEditor();
        editor.loadFrom(new ModelDefinitionBuilder()
                .name("Test")
                .stock("Stock 1", 0, null)
                .build());

        ExportBounds.Bounds bounds = ExportBounds.compute(state, editor);

        assertThat(bounds.width()).isGreaterThan(0);
        assertThat(bounds.height()).isGreaterThan(0);
        // The bounds should be centered around the element with padding
        assertThat(bounds.minX()).isLessThan(200);
        assertThat(bounds.minY()).isLessThan(300);
    }

    @Test
    void shouldExpandBoundsForMultipleElements() {
        CanvasState state = new CanvasState();
        state.addElement("S1", ElementType.STOCK, 100, 100);
        state.addElement("S2", ElementType.STOCK, 500, 500);

        ModelEditor editor = new ModelEditor();
        editor.loadFrom(new ModelDefinitionBuilder()
                .name("Test")
                .stock("S1", 0, null)
                .stock("S2", 0, null)
                .build());

        ExportBounds.Bounds bounds = ExportBounds.compute(state, editor);

        // Should span from near S1 to near S2 plus padding
        assertThat(bounds.minX()).isLessThan(100);
        assertThat(bounds.minY()).isLessThan(100);
        assertThat(bounds.minX() + bounds.width()).isGreaterThan(500);
        assertThat(bounds.minY() + bounds.height()).isGreaterThan(500);
    }

    @Test
    void shouldIncludeCloudPositionsForFlows() {
        CanvasState state = new CanvasState();
        state.addElement("Stock 1", ElementType.STOCK, 300, 300);
        state.addElement("Flow 1", ElementType.FLOW, 200, 300);

        ModelEditor editor = new ModelEditor();
        editor.loadFrom(new ModelDefinitionBuilder()
                .name("Test")
                .stock("Stock 1", 100, null)
                .flow("Flow 1", "10", "day", null, "Stock 1")
                .build());

        ExportBounds.Bounds bounds = ExportBounds.compute(state, editor);

        // Cloud position should extend bounds beyond just the stock and flow positions
        assertThat(bounds.width()).isGreaterThan(0);
        assertThat(bounds.height()).isGreaterThan(0);
    }
}
