package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ElementPlacement;
import com.deathrayresearch.forrester.model.def.ViewDef;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("CanvasState")
class CanvasStateTest {

    private CanvasState state;

    @BeforeEach
    void setUp() {
        state = new CanvasState();
    }

    @Nested
    @DisplayName("loadFrom")
    class LoadFrom {

        @Test
        void shouldCopyPositionsFromViewDef() {
            ViewDef view = new ViewDef("test", List.of(
                    new ElementPlacement("S", "stock", 100, 200),
                    new ElementPlacement("I", "stock", 300, 200)
            ), List.of(), List.of());

            state.loadFrom(view);

            assertThat(state.getX("S")).isCloseTo(100, within(0.001));
            assertThat(state.getY("S")).isCloseTo(200, within(0.001));
            assertThat(state.getX("I")).isCloseTo(300, within(0.001));
            assertThat(state.getY("I")).isCloseTo(200, within(0.001));
        }

        @Test
        void shouldCopyElementTypes() {
            ViewDef view = new ViewDef("test", List.of(
                    new ElementPlacement("S", "stock", 100, 200),
                    new ElementPlacement("rate", "flow", 200, 200),
                    new ElementPlacement("k", "constant", 200, 300)
            ), List.of(), List.of());

            state.loadFrom(view);

            assertThat(state.getType("S")).isEqualTo("stock");
            assertThat(state.getType("rate")).isEqualTo("flow");
            assertThat(state.getType("k")).isEqualTo("constant");
        }

        @Test
        void shouldClearPreviousState() {
            ViewDef view1 = new ViewDef("v1", List.of(
                    new ElementPlacement("A", "stock", 10, 20)
            ), List.of(), List.of());
            state.loadFrom(view1);
            state.select("A");

            ViewDef view2 = new ViewDef("v2", List.of(
                    new ElementPlacement("B", "stock", 50, 60)
            ), List.of(), List.of());
            state.loadFrom(view2);

            assertThat(state.hasElement("A")).isFalse();
            assertThat(state.hasElement("B")).isTrue();
            assertThat(state.getSelection()).isEmpty();
        }

        @Test
        void shouldPreserveDrawOrder() {
            ViewDef view = new ViewDef("test", List.of(
                    new ElementPlacement("first", "stock", 0, 0),
                    new ElementPlacement("second", "aux", 100, 0),
                    new ElementPlacement("third", "constant", 200, 0)
            ), List.of(), List.of());

            state.loadFrom(view);

            assertThat(state.getDrawOrder()).containsExactly("first", "second", "third");
        }
    }

    @Nested
    @DisplayName("position mutation")
    class PositionMutation {

        @Test
        void shouldUpdatePosition() {
            ViewDef view = new ViewDef("test", List.of(
                    new ElementPlacement("S", "stock", 100, 200)
            ), List.of(), List.of());
            state.loadFrom(view);

            state.setPosition("S", 500, 600);

            assertThat(state.getX("S")).isCloseTo(500, within(0.001));
            assertThat(state.getY("S")).isCloseTo(600, within(0.001));
        }

        @Test
        void shouldReturnNaNForUnknownElement() {
            assertThat(state.getX("nonexistent")).isNaN();
            assertThat(state.getY("nonexistent")).isNaN();
        }

        @Test
        void shouldIgnoreSetPositionForUnknownElement() {
            state.setPosition("nonexistent", 100, 200);
            assertThat(state.getX("nonexistent")).isNaN();
        }
    }

    @Nested
    @DisplayName("selection")
    class Selection {

        @BeforeEach
        void loadElements() {
            ViewDef view = new ViewDef("test", List.of(
                    new ElementPlacement("A", "stock", 0, 0),
                    new ElementPlacement("B", "stock", 100, 0),
                    new ElementPlacement("C", "aux", 200, 0)
            ), List.of(), List.of());
            state.loadFrom(view);
        }

        @Test
        void shouldSelectSingleElement() {
            state.select("A");

            assertThat(state.isSelected("A")).isTrue();
            assertThat(state.isSelected("B")).isFalse();
            assertThat(state.getSelection()).containsExactly("A");
        }

        @Test
        void shouldClearPreviousSelectionOnSelect() {
            state.select("A");
            state.select("B");

            assertThat(state.isSelected("A")).isFalse();
            assertThat(state.isSelected("B")).isTrue();
            assertThat(state.getSelection()).containsExactly("B");
        }

        @Test
        void shouldToggleSelectionOn() {
            state.toggleSelection("A");

            assertThat(state.isSelected("A")).isTrue();
        }

        @Test
        void shouldToggleSelectionOff() {
            state.select("A");
            state.toggleSelection("A");

            assertThat(state.isSelected("A")).isFalse();
        }

        @Test
        void shouldToggleWithoutClearingOthers() {
            state.select("A");
            state.toggleSelection("B");

            assertThat(state.isSelected("A")).isTrue();
            assertThat(state.isSelected("B")).isTrue();
        }

        @Test
        void shouldClearAllSelections() {
            state.select("A");
            state.toggleSelection("B");
            state.clearSelection();

            assertThat(state.getSelection()).isEmpty();
        }

        @Test
        void shouldIgnoreSelectForUnknownElement() {
            state.select("nonexistent");
            assertThat(state.getSelection()).isEmpty();
        }

        @Test
        void shouldIgnoreToggleForUnknownElement() {
            state.toggleSelection("nonexistent");
            assertThat(state.getSelection()).isEmpty();
        }
    }
}
