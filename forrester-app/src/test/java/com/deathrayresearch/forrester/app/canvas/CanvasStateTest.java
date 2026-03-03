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
                    new ElementPlacement("S", ElementType.STOCK, 100, 200),
                    new ElementPlacement("I", ElementType.STOCK, 300, 200)
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
                    new ElementPlacement("S", ElementType.STOCK, 100, 200),
                    new ElementPlacement("rate", ElementType.FLOW, 200, 200),
                    new ElementPlacement("k", ElementType.CONSTANT, 200, 300)
            ), List.of(), List.of());

            state.loadFrom(view);

            assertThat(state.getType("S")).isEqualTo(ElementType.STOCK);
            assertThat(state.getType("rate")).isEqualTo(ElementType.FLOW);
            assertThat(state.getType("k")).isEqualTo(ElementType.CONSTANT);
        }

        @Test
        void shouldClearPreviousState() {
            ViewDef view1 = new ViewDef("v1", List.of(
                    new ElementPlacement("A", ElementType.STOCK, 10, 20)
            ), List.of(), List.of());
            state.loadFrom(view1);
            state.select("A");

            ViewDef view2 = new ViewDef("v2", List.of(
                    new ElementPlacement("B", ElementType.STOCK, 50, 60)
            ), List.of(), List.of());
            state.loadFrom(view2);

            assertThat(state.hasElement("A")).isFalse();
            assertThat(state.hasElement("B")).isTrue();
            assertThat(state.getSelection()).isEmpty();
        }

        @Test
        void shouldPreserveDrawOrder() {
            ViewDef view = new ViewDef("test", List.of(
                    new ElementPlacement("first", ElementType.STOCK, 0, 0),
                    new ElementPlacement("second", ElementType.AUX, 100, 0),
                    new ElementPlacement("third", ElementType.CONSTANT, 200, 0)
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
                    new ElementPlacement("S", ElementType.STOCK, 100, 200)
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
                    new ElementPlacement("A", ElementType.STOCK, 0, 0),
                    new ElementPlacement("B", ElementType.STOCK, 100, 0),
                    new ElementPlacement("C", ElementType.AUX, 200, 0)
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

    @Nested
    @DisplayName("addElement")
    class AddElement {

        @Test
        void shouldAddNewElement() {
            state.addElement("NewStock", ElementType.STOCK, 300, 400);

            assertThat(state.hasElement("NewStock")).isTrue();
            assertThat(state.getX("NewStock")).isCloseTo(300, within(0.001));
            assertThat(state.getY("NewStock")).isCloseTo(400, within(0.001));
            assertThat(state.getType("NewStock")).isEqualTo(ElementType.STOCK);
        }

        @Test
        void shouldAppendToDrawOrder() {
            state.addElement("A", ElementType.STOCK, 0, 0);
            state.addElement("B", ElementType.AUX, 100, 0);

            assertThat(state.getDrawOrder()).containsExactly("A", "B");
        }

        @Test
        void shouldOverwriteExistingElement() {
            state.addElement("X", ElementType.STOCK, 10, 20);
            state.addElement("X", ElementType.AUX, 50, 60);

            assertThat(state.getType("X")).isEqualTo(ElementType.AUX);
            assertThat(state.getX("X")).isCloseTo(50, within(0.001));
            // Should not duplicate in draw order
            assertThat(state.getDrawOrder()).containsExactly("X");
        }
    }

    @Nested
    @DisplayName("renameElement")
    class RenameElement {

        @BeforeEach
        void loadElements() {
            ViewDef view = new ViewDef("test", List.of(
                    new ElementPlacement("A", ElementType.STOCK, 100, 200),
                    new ElementPlacement("B", ElementType.FLOW, 200, 200),
                    new ElementPlacement("C", ElementType.AUX, 300, 200)
            ), List.of(), List.of());
            state.loadFrom(view);
        }

        @Test
        void shouldRenamePosition() {
            state.renameElement("A", "Alpha");

            assertThat(state.hasElement("A")).isFalse();
            assertThat(state.hasElement("Alpha")).isTrue();
            assertThat(state.getX("Alpha")).isCloseTo(100, within(0.001));
            assertThat(state.getY("Alpha")).isCloseTo(200, within(0.001));
        }

        @Test
        void shouldRenameType() {
            state.renameElement("A", "Alpha");

            assertThat(state.getType("A")).isNull();
            assertThat(state.getType("Alpha")).isEqualTo(ElementType.STOCK);
        }

        @Test
        void shouldRenameInDrawOrder() {
            state.renameElement("B", "Beta");

            assertThat(state.getDrawOrder()).containsExactly("A", "Beta", "C");
        }

        @Test
        void shouldRenameInSelection() {
            state.select("A");
            state.renameElement("A", "Alpha");

            assertThat(state.isSelected("A")).isFalse();
            assertThat(state.isSelected("Alpha")).isTrue();
        }

        @Test
        void shouldReturnFalseForNonexistentElement() {
            assertThat(state.renameElement("ghost", "new")).isFalse();
        }

        @Test
        void shouldReturnFalseForSameName() {
            assertThat(state.renameElement("A", "A")).isFalse();
        }

        @Test
        void shouldNotAffectOtherElements() {
            state.renameElement("A", "Alpha");

            assertThat(state.hasElement("B")).isTrue();
            assertThat(state.hasElement("C")).isTrue();
        }

        @Test
        void shouldRejectRenameToExistingName() {
            boolean result = state.renameElement("A", "B");

            assertThat(result).isFalse();
            // Both elements preserved unchanged
            assertThat(state.hasElement("A")).isTrue();
            assertThat(state.hasElement("B")).isTrue();
            assertThat(state.getType("A")).isEqualTo(ElementType.STOCK);
            assertThat(state.getType("B")).isEqualTo(ElementType.FLOW);
            assertThat(state.getDrawOrder()).containsExactly("A", "B", "C");
        }
    }

    @Nested
    @DisplayName("toViewDef")
    class ToViewDef {

        @Test
        void shouldRoundTripPositionsAndTypes() {
            ViewDef original = new ViewDef("test", List.of(
                    new ElementPlacement("S", ElementType.STOCK, 100, 200),
                    new ElementPlacement("F", ElementType.FLOW, 200, 200),
                    new ElementPlacement("C", ElementType.CONSTANT, 300, 400)
            ), List.of(), List.of());
            state.loadFrom(original);

            ViewDef result = state.toViewDef();

            assertThat(result.name()).isEqualTo("Main");
            assertThat(result.elements()).hasSize(3);

            ElementPlacement s = result.elements().get(0);
            assertThat(s.name()).isEqualTo("S");
            assertThat(s.type()).isEqualTo(ElementType.STOCK);
            assertThat(s.x()).isCloseTo(100, within(0.001));
            assertThat(s.y()).isCloseTo(200, within(0.001));

            ElementPlacement f = result.elements().get(1);
            assertThat(f.name()).isEqualTo("F");
            assertThat(f.type()).isEqualTo(ElementType.FLOW);

            ElementPlacement c = result.elements().get(2);
            assertThat(c.name()).isEqualTo("C");
            assertThat(c.type()).isEqualTo(ElementType.CONSTANT);
            assertThat(c.x()).isCloseTo(300, within(0.001));
            assertThat(c.y()).isCloseTo(400, within(0.001));
        }

        @Test
        void shouldReflectMutatedPositions() {
            ViewDef original = new ViewDef("test", List.of(
                    new ElementPlacement("A", ElementType.STOCK, 10, 20)
            ), List.of(), List.of());
            state.loadFrom(original);
            state.setPosition("A", 500, 600);

            ViewDef result = state.toViewDef();

            assertThat(result.elements().get(0).x()).isCloseTo(500, within(0.001));
            assertThat(result.elements().get(0).y()).isCloseTo(600, within(0.001));
        }

        @Test
        void shouldIncludeAddedElements() {
            state.addElement("New", ElementType.AUX, 42, 84);

            ViewDef result = state.toViewDef();

            assertThat(result.elements()).hasSize(1);
            assertThat(result.elements().get(0).name()).isEqualTo("New");
            assertThat(result.elements().get(0).type()).isEqualTo(ElementType.AUX);
        }
    }

    @Nested
    @DisplayName("removeElement")
    class RemoveElement {

        @BeforeEach
        void loadElements() {
            ViewDef view = new ViewDef("test", List.of(
                    new ElementPlacement("A", ElementType.STOCK, 0, 0),
                    new ElementPlacement("B", ElementType.STOCK, 100, 0),
                    new ElementPlacement("C", ElementType.AUX, 200, 0)
            ), List.of(), List.of());
            state.loadFrom(view);
        }

        @Test
        void shouldRemoveFromPositions() {
            state.removeElement("B");

            assertThat(state.hasElement("B")).isFalse();
            assertThat(state.getX("B")).isNaN();
        }

        @Test
        void shouldRemoveFromDrawOrder() {
            state.removeElement("B");

            assertThat(state.getDrawOrder()).containsExactly("A", "C");
        }

        @Test
        void shouldRemoveFromSelection() {
            state.select("B");
            state.removeElement("B");

            assertThat(state.getSelection()).isEmpty();
        }

        @Test
        void shouldRemoveFromTypes() {
            state.removeElement("B");

            assertThat(state.getType("B")).isNull();
        }

        @Test
        void shouldNotAffectOtherElements() {
            state.removeElement("B");

            assertThat(state.hasElement("A")).isTrue();
            assertThat(state.hasElement("C")).isTrue();
        }
    }
}
