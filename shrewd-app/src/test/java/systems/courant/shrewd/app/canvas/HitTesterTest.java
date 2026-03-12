package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.def.ElementPlacement;
import systems.courant.shrewd.model.def.ElementType;
import systems.courant.shrewd.model.def.ViewDef;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HitTester")
class HitTesterTest {

    private CanvasState state;

    @BeforeEach
    void setUp() {
        state = new CanvasState();
    }

    @Nested
    @DisplayName("stock hit testing (rectangular)")
    class StockHitTest {

        @BeforeEach
        void loadStock() {
            ViewDef view = new ViewDef("test", List.of(
                    new ElementPlacement("S", ElementType.STOCK, 200, 300)
            ), List.of(), List.of());
            state.loadFrom(view);
        }

        @Test
        void shouldHitAtCenter() {
            assertThat(HitTester.hitTest(state, 200, 300)).isEqualTo("S");
        }

        @Test
        void shouldHitAtEdge() {
            // Stock is 140x80, so half-width=70, half-height=40
            assertThat(HitTester.hitTest(state, 200 + 70, 300)).isEqualTo("S");
            assertThat(HitTester.hitTest(state, 200 - 70, 300)).isEqualTo("S");
            assertThat(HitTester.hitTest(state, 200, 300 + 40)).isEqualTo("S");
            assertThat(HitTester.hitTest(state, 200, 300 - 40)).isEqualTo("S");
        }

        @Test
        void shouldMissOutside() {
            assertThat(HitTester.hitTest(state, 200 + 71, 300)).isNull();
            assertThat(HitTester.hitTest(state, 200, 300 + 41)).isNull();
            assertThat(HitTester.hitTest(state, 0, 0)).isNull();
        }
    }

    @Nested
    @DisplayName("aux hit testing (rectangular)")
    class AuxHitTest {

        @BeforeEach
        void loadAux() {
            ViewDef view = new ViewDef("test", List.of(
                    new ElementPlacement("rate", ElementType.AUX, 150, 250)
            ), List.of(), List.of());
            state.loadFrom(view);
        }

        @Test
        void shouldHitAtCenter() {
            assertThat(HitTester.hitTest(state, 150, 250)).isEqualTo("rate");
        }

        @Test
        void shouldHitAtEdge() {
            // Aux is 100x55, half-width=50, half-height=27.5
            assertThat(HitTester.hitTest(state, 150 + 50, 250)).isEqualTo("rate");
            assertThat(HitTester.hitTest(state, 150, 250 + 27)).isEqualTo("rate");
        }

        @Test
        void shouldMissOutside() {
            assertThat(HitTester.hitTest(state, 150 + 51, 250)).isNull();
            assertThat(HitTester.hitTest(state, 150, 250 + 28)).isNull();
        }
    }

    @Nested
    @DisplayName("aux (parameter) hit testing (rectangular)")
    class AuxParameterHitTest {

        @BeforeEach
        void loadAux() {
            ViewDef view = new ViewDef("test", List.of(
                    new ElementPlacement("k", ElementType.AUX, 100, 100)
            ), List.of(), List.of());
            state.loadFrom(view);
        }

        @Test
        void shouldHitAtCenter() {
            assertThat(HitTester.hitTest(state, 100, 100)).isEqualTo("k");
        }

        @Test
        void shouldMissOutside() {
            // Aux is 100x55, half-width=50, half-height=27.5
            assertThat(HitTester.hitTest(state, 100 + 51, 100)).isNull();
        }
    }

    @Nested
    @DisplayName("flow hit testing (rectangular, covers name/equation text)")
    class FlowHitTest {

        @BeforeEach
        void loadFlow() {
            ViewDef view = new ViewDef("test", List.of(
                    new ElementPlacement("rate", ElementType.FLOW, 200, 200)
            ), List.of(), List.of());
            state.loadFrom(view);
        }

        @Test
        void shouldHitAtCenter() {
            assertThat(HitTester.hitTest(state, 200, 200)).isEqualTo("rate");
        }

        @Test
        void shouldHitInNameLabelArea() {
            // Flow hit area extends 35 pixels below center, covering name/equation text
            assertThat(HitTester.hitTest(state, 200, 200 + 30)).isEqualTo("rate");
        }

        @Test
        void shouldHitWideForTextLabels() {
            // Flow hit area extends 55 pixels to each side, covering text labels
            assertThat(HitTester.hitTest(state, 200 + 50, 200)).isEqualTo("rate");
            assertThat(HitTester.hitTest(state, 200 - 50, 200)).isEqualTo("rate");
        }

        @Test
        void shouldMissOutside() {
            // Half-width=55, half-height=35
            assertThat(HitTester.hitTest(state, 200 + 56, 200)).isNull();
            assertThat(HitTester.hitTest(state, 200, 200 + 36)).isNull();
        }
    }

    @Nested
    @DisplayName("draw order priority")
    class DrawOrderPriority {

        @Test
        void shouldHitTopmostElementFirst() {
            // Two overlapping elements — second one drawn on top
            ViewDef view = new ViewDef("test", List.of(
                    new ElementPlacement("bottom", ElementType.STOCK, 200, 200),
                    new ElementPlacement("top", ElementType.STOCK, 200, 200)
            ), List.of(), List.of());
            state.loadFrom(view);

            assertThat(HitTester.hitTest(state, 200, 200)).isEqualTo("top");
        }
    }

    @Nested
    @DisplayName("empty state")
    class EmptyState {

        @Test
        void shouldReturnNullWhenNoElements() {
            assertThat(HitTester.hitTest(state, 100, 100)).isNull();
        }
    }

    @Nested
    @DisplayName("hideVariables filtering")
    class HideVariables {

        @BeforeEach
        void loadMixedElements() {
            ViewDef view = new ViewDef("test", List.of(
                    new ElementPlacement("Population", ElementType.STOCK, 200, 200),
                    new ElementPlacement("rate", ElementType.AUX, 300, 200),
                    new ElementPlacement("inflow", ElementType.FLOW, 100, 200)
            ), List.of(), List.of());
            state.loadFrom(view);
        }

        @Test
        void shouldHitAuxWhenNotHidden() {
            assertThat(HitTester.hitTest(state, 300, 200, false)).isEqualTo("rate");
        }

        @Test
        void shouldSkipAuxWhenHidden() {
            assertThat(HitTester.hitTest(state, 300, 200, true)).isNull();
        }

        @Test
        void shouldStillHitStockWhenAuxHidden() {
            assertThat(HitTester.hitTest(state, 200, 200, true)).isEqualTo("Population");
        }

        @Test
        void shouldStillHitFlowWhenAuxHidden() {
            assertThat(HitTester.hitTest(state, 100, 200, true)).isEqualTo("inflow");
        }

        @Test
        void shouldHitStockBehindHiddenAux() {
            // Place an AUX on top of a stock — when hidden, stock is hittable
            ViewDef view = new ViewDef("test", List.of(
                    new ElementPlacement("S", ElementType.STOCK, 200, 200),
                    new ElementPlacement("A", ElementType.AUX, 200, 200)
            ), List.of(), List.of());
            state.loadFrom(view);

            // Without hiding: aux is on top
            assertThat(HitTester.hitTest(state, 200, 200, false)).isEqualTo("A");
            // With hiding: aux skipped, stock is hit
            assertThat(HitTester.hitTest(state, 200, 200, true)).isEqualTo("S");
        }
    }
}
