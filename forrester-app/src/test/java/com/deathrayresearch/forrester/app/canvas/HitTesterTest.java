package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ElementPlacement;
import com.deathrayresearch.forrester.model.def.ElementType;
import com.deathrayresearch.forrester.model.def.ViewDef;

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
    @DisplayName("constant hit testing (rectangular)")
    class ConstantHitTest {

        @BeforeEach
        void loadConstant() {
            ViewDef view = new ViewDef("test", List.of(
                    new ElementPlacement("k", ElementType.CONSTANT, 100, 100)
            ), List.of(), List.of());
            state.loadFrom(view);
        }

        @Test
        void shouldHitAtCenter() {
            assertThat(HitTester.hitTest(state, 100, 100)).isEqualTo("k");
        }

        @Test
        void shouldMissOutside() {
            // Constant is 90x45, half-width=45, half-height=22.5
            assertThat(HitTester.hitTest(state, 100 + 46, 100)).isNull();
        }
    }

    @Nested
    @DisplayName("flow hit testing (diamond)")
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
        void shouldHitAtDiamondTip() {
            // Flow indicator size=30, half=15
            assertThat(HitTester.hitTest(state, 200 + 15, 200)).isEqualTo("rate");
            assertThat(HitTester.hitTest(state, 200, 200 + 15)).isEqualTo("rate");
        }

        @Test
        void shouldMissAtDiamondCorner() {
            // The point (cx+15, cy+15) has Manhattan distance 30 > 15, so should miss
            assertThat(HitTester.hitTest(state, 200 + 15, 200 + 15)).isNull();
        }

        @Test
        void shouldMissOutside() {
            assertThat(HitTester.hitTest(state, 200 + 16, 200)).isNull();
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
}
