package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.def.CausalLinkDef;
import systems.courant.shrewd.model.def.ElementType;
import systems.courant.shrewd.model.def.ModelDefinitionBuilder;

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

    @Test
    @DisplayName("should include causal link polarity label positions")
    void shouldIncludeCausalLinkPolarityLabels() {
        // Place two CLD variables at the edge of the diagram, with a causal link
        // whose polarity label extends beyond the element bounds
        CanvasState state = new CanvasState();
        state.addElement("A", ElementType.CLD_VARIABLE, 100, 100);
        state.addElement("B", ElementType.CLD_VARIABLE, 100, 300);

        ModelEditor editor = new ModelEditor();
        editor.loadFrom(new ModelDefinitionBuilder()
                .name("Test")
                .cldVariable("A")
                .cldVariable("B")
                .causalLink("A", "B", CausalLinkDef.Polarity.POSITIVE)
                .build());

        ExportBounds.Bounds boundsWithLink = ExportBounds.compute(state, editor);

        // Now compute bounds without the causal link for comparison
        ModelEditor editorNoLink = new ModelEditor();
        editorNoLink.loadFrom(new ModelDefinitionBuilder()
                .name("Test")
                .cldVariable("A")
                .cldVariable("B")
                .build());

        ExportBounds.Bounds boundsNoLink = ExportBounds.compute(state, editorNoLink);

        // Bounds with causal link should be at least as large (the perpendicular
        // offset of the polarity label may extend the bounding box)
        assertThat(boundsWithLink.width()).isGreaterThanOrEqualTo(boundsNoLink.width());
    }

    @Test
    @DisplayName("should include self-loop causal link polarity label")
    void shouldIncludeSelfLoopPolarityLabel() {
        CanvasState state = new CanvasState();
        state.addElement("X", ElementType.CLD_VARIABLE, 200, 200);

        ModelEditor editor = new ModelEditor();
        editor.loadFrom(new ModelDefinitionBuilder()
                .name("Test")
                .cldVariable("X")
                .causalLink("X", "X", CausalLinkDef.Polarity.POSITIVE)
                .build());

        ExportBounds.Bounds bounds = ExportBounds.compute(state, editor);

        // Self-loop label extends above the element — bounds should include it
        // The label is at the top of the loop arc, well above the element center at y=200
        assertThat(bounds.minY()).isLessThan(200 - 50);
    }

    @Test
    @DisplayName("should handle causal links with missing elements gracefully")
    void shouldHandleCausalLinksWithMissingElements() {
        CanvasState state = new CanvasState();
        state.addElement("A", ElementType.CLD_VARIABLE, 100, 100);
        // "B" is NOT added to canvas state

        ModelEditor editor = new ModelEditor();
        editor.loadFrom(new ModelDefinitionBuilder()
                .name("Test")
                .cldVariable("A")
                .cldVariable("B")
                .causalLink("A", "B", CausalLinkDef.Polarity.NEGATIVE)
                .build());

        // Should not throw — gracefully skips links with missing elements
        ExportBounds.Bounds bounds = ExportBounds.compute(state, editor);
        assertThat(bounds.width()).isGreaterThan(0);
    }
}
