package systems.courant.sd.app.canvas.controllers;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.model.def.ElementType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("AlignmentController")
class AlignmentControllerTest {

    private CanvasState state;

    @BeforeEach
    void setUp() {
        state = new CanvasState();
        // A at (100, 50), B at (300, 150), C at (500, 250)
        // All are STOCK: default 140x80
        state.addElement("A", ElementType.STOCK, 100, 50);
        state.addElement("B", ElementType.STOCK, 300, 150);
        state.addElement("C", ElementType.STOCK, 500, 250);
    }

    @Nested
    @DisplayName("align top")
    class AlignTop {

        @Test
        void shouldAlignTopEdgesToTopmost() {
            state.select("A");
            state.addToSelection("B");
            state.addToSelection("C");

            AlignmentController.alignTop(state);

            // A is topmost: top edge = 50 - 40 = 10
            // All elements should have top edge at 10, so center Y = 10 + 40 = 50
            assertThat(state.getY("A")).isCloseTo(50, within(0.01));
            assertThat(state.getY("B")).isCloseTo(50, within(0.01));
            assertThat(state.getY("C")).isCloseTo(50, within(0.01));
        }

        @Test
        void shouldNotChangeXPositions() {
            state.select("A");
            state.addToSelection("B");

            AlignmentController.alignTop(state);

            assertThat(state.getX("A")).isCloseTo(100, within(0.01));
            assertThat(state.getX("B")).isCloseTo(300, within(0.01));
        }

        @Test
        void shouldNoOpWithSingleSelection() {
            state.select("A");

            AlignmentController.alignTop(state);

            assertThat(state.getY("A")).isCloseTo(50, within(0.01));
        }
    }

    @Nested
    @DisplayName("align bottom")
    class AlignBottom {

        @Test
        void shouldAlignBottomEdgesToBottommost() {
            state.select("A");
            state.addToSelection("B");
            state.addToSelection("C");

            AlignmentController.alignBottom(state);

            // C is bottommost: bottom edge = 250 + 40 = 290
            // All elements should have bottom edge at 290, so center Y = 290 - 40 = 250
            assertThat(state.getY("A")).isCloseTo(250, within(0.01));
            assertThat(state.getY("B")).isCloseTo(250, within(0.01));
            assertThat(state.getY("C")).isCloseTo(250, within(0.01));
        }
    }

    @Nested
    @DisplayName("align center vertical")
    class AlignCenterVertical {

        @Test
        void shouldAlignToAverageY() {
            state.select("A");
            state.addToSelection("B");
            state.addToSelection("C");

            AlignmentController.alignCenterVertical(state);

            // Average Y = (50 + 150 + 250) / 3 = 150
            assertThat(state.getY("A")).isCloseTo(150, within(0.01));
            assertThat(state.getY("B")).isCloseTo(150, within(0.01));
            assertThat(state.getY("C")).isCloseTo(150, within(0.01));
        }
    }

    @Nested
    @DisplayName("align left")
    class AlignLeft {

        @Test
        void shouldAlignLeftEdgesToLeftmost() {
            state.select("A");
            state.addToSelection("B");
            state.addToSelection("C");

            AlignmentController.alignLeft(state);

            // A is leftmost: left edge = 100 - 70 = 30
            // All elements should have left edge at 30, so center X = 30 + 70 = 100
            assertThat(state.getX("A")).isCloseTo(100, within(0.01));
            assertThat(state.getX("B")).isCloseTo(100, within(0.01));
            assertThat(state.getX("C")).isCloseTo(100, within(0.01));
        }

        @Test
        void shouldNotChangeYPositions() {
            state.select("A");
            state.addToSelection("B");

            AlignmentController.alignLeft(state);

            assertThat(state.getY("A")).isCloseTo(50, within(0.01));
            assertThat(state.getY("B")).isCloseTo(150, within(0.01));
        }
    }

    @Nested
    @DisplayName("align right")
    class AlignRight {

        @Test
        void shouldAlignRightEdgesToRightmost() {
            state.select("A");
            state.addToSelection("B");
            state.addToSelection("C");

            AlignmentController.alignRight(state);

            // C is rightmost: right edge = 500 + 70 = 570
            // All elements should have right edge at 570, so center X = 570 - 70 = 500
            assertThat(state.getX("A")).isCloseTo(500, within(0.01));
            assertThat(state.getX("B")).isCloseTo(500, within(0.01));
            assertThat(state.getX("C")).isCloseTo(500, within(0.01));
        }
    }

    @Nested
    @DisplayName("align center horizontal")
    class AlignCenterHorizontal {

        @Test
        void shouldAlignToAverageX() {
            state.select("A");
            state.addToSelection("B");
            state.addToSelection("C");

            AlignmentController.alignCenterHorizontal(state);

            // Average X = (100 + 300 + 500) / 3 = 300
            assertThat(state.getX("A")).isCloseTo(300, within(0.01));
            assertThat(state.getX("B")).isCloseTo(300, within(0.01));
            assertThat(state.getX("C")).isCloseTo(300, within(0.01));
        }
    }

    @Nested
    @DisplayName("distribute horizontally")
    class DistributeHorizontally {

        @Test
        void shouldSpaceElementsEvenlyAlongX() {
            state.select("A");
            state.addToSelection("B");
            state.addToSelection("C");

            AlignmentController.distributeHorizontally(state);

            // All STOCK width = 140, so half-width = 70
            // Left edge of A = 100 - 70 = 30
            // Right edge of C = 500 + 70 = 570
            // Total span = 570 - 30 = 540
            // Total widths = 3 * 140 = 420
            // Total gap = 540 - 420 = 120, gap per pair = 60
            // A center = 30 + 70 = 100 (unchanged)
            // B center = 30 + 140 + 60 + 70 = 300
            // C center = 30 + 140 + 60 + 140 + 60 + 70 = 500 (unchanged)
            assertThat(state.getX("A")).isCloseTo(100, within(0.01));
            assertThat(state.getX("B")).isCloseTo(300, within(0.01));
            assertThat(state.getX("C")).isCloseTo(500, within(0.01));
        }

        @Test
        void shouldNoOpWithTwoElements() {
            state.select("A");
            state.addToSelection("B");

            AlignmentController.distributeHorizontally(state);

            assertThat(state.getX("A")).isCloseTo(100, within(0.01));
            assertThat(state.getX("B")).isCloseTo(300, within(0.01));
        }

        @Test
        void shouldDistributeUnevenlySpacedElements() {
            // Move B closer to A to create uneven spacing
            state.setPosition("B", 200, 150);

            state.select("A");
            state.addToSelection("B");
            state.addToSelection("C");

            AlignmentController.distributeHorizontally(state);

            // Left edge A = 100 - 70 = 30
            // Right edge C = 500 + 70 = 570
            // Total span = 540, total widths = 420, gap per pair = 60
            // B center = 30 + 140 + 60 + 70 = 300
            assertThat(state.getX("A")).isCloseTo(100, within(0.01));
            assertThat(state.getX("B")).isCloseTo(300, within(0.01));
            assertThat(state.getX("C")).isCloseTo(500, within(0.01));
        }
    }

    @Nested
    @DisplayName("distribute vertically")
    class DistributeVertically {

        @Test
        void shouldSpaceElementsEvenlyAlongY() {
            state.select("A");
            state.addToSelection("B");
            state.addToSelection("C");

            AlignmentController.distributeVertically(state);

            // All STOCK height = 80, half-height = 40
            // Top edge A = 50 - 40 = 10
            // Bottom edge C = 250 + 40 = 290
            // Total span = 280, total heights = 240, gap per pair = 20
            // A center = 10 + 40 = 50 (unchanged)
            // B center = 10 + 80 + 20 + 40 = 150
            // C center = 10 + 80 + 20 + 80 + 20 + 40 = 250 (unchanged)
            assertThat(state.getY("A")).isCloseTo(50, within(0.01));
            assertThat(state.getY("B")).isCloseTo(150, within(0.01));
            assertThat(state.getY("C")).isCloseTo(250, within(0.01));
        }
    }

    @Nested
    @DisplayName("snap to grid")
    class SnapToGrid {

        @Test
        void shouldSnapCentersToNearestGridPoint() {
            state.setPosition("A", 107, 53);
            state.select("A");

            AlignmentController.snapToGrid(state);

            // Grid = 20: 107 -> 100, 53 -> 60
            assertThat(state.getX("A")).isCloseTo(100, within(0.01));
            assertThat(state.getY("A")).isCloseTo(60, within(0.01));
        }

        @Test
        void shouldSnapMultipleElements() {
            state.setPosition("A", 113, 47);
            state.setPosition("B", 289, 162);
            state.select("A");
            state.addToSelection("B");

            AlignmentController.snapToGrid(state);

            assertThat(state.getX("A")).isCloseTo(120, within(0.01));
            assertThat(state.getY("A")).isCloseTo(40, within(0.01));
            assertThat(state.getX("B")).isCloseTo(280, within(0.01));
            assertThat(state.getY("B")).isCloseTo(160, within(0.01));
        }

        @Test
        void shouldRespectCustomGridSize() {
            state.setPosition("A", 107, 53);
            state.select("A");

            AlignmentController.snapToGrid(state, 10);

            assertThat(state.getX("A")).isCloseTo(110, within(0.01));
            assertThat(state.getY("A")).isCloseTo(50, within(0.01));
        }

        @Test
        void shouldNoOpWithEmptySelection() {
            AlignmentController.snapToGrid(state);

            assertThat(state.getX("A")).isCloseTo(100, within(0.01));
            assertThat(state.getY("A")).isCloseTo(50, within(0.01));
        }
    }

    @Nested
    @DisplayName("mixed element types")
    class MixedElementTypes {

        @BeforeEach
        void setUp() {
            state = new CanvasState();
            // STOCK (140x80) at (100, 100)
            state.addElement("S", ElementType.STOCK, 100, 100);
            // FLOW (30x30) at (300, 200)
            state.addElement("F", ElementType.FLOW, 300, 200);
        }

        @Test
        void shouldAlignLeftEdgesAccountingForDifferentWidths() {
            state.select("S");
            state.addToSelection("F");

            AlignmentController.alignLeft(state);

            // S left edge = 100 - 70 = 30
            // F should have left edge at 30, so center = 30 + 15 = 45
            assertThat(state.getX("S")).isCloseTo(100, within(0.01));
            assertThat(state.getX("F")).isCloseTo(45, within(0.01));
        }

        @Test
        void shouldAlignRightEdgesAccountingForDifferentWidths() {
            state.select("S");
            state.addToSelection("F");

            AlignmentController.alignRight(state);

            // F right edge = 300 + 15 = 315
            // S should have right edge at 315, so center = 315 - 70 = 245
            assertThat(state.getX("S")).isCloseTo(245, within(0.01));
            assertThat(state.getX("F")).isCloseTo(300, within(0.01));
        }

        @Test
        void shouldAlignTopEdgesAccountingForDifferentHeights() {
            state.select("S");
            state.addToSelection("F");

            AlignmentController.alignTop(state);

            // S top edge = 100 - 40 = 60
            // F should have top edge at 60, so center = 60 + 15 = 75
            assertThat(state.getY("S")).isCloseTo(100, within(0.01));
            assertThat(state.getY("F")).isCloseTo(75, within(0.01));
        }

        @Test
        void shouldAlignBottomEdgesAccountingForDifferentHeights() {
            state.select("S");
            state.addToSelection("F");

            AlignmentController.alignBottom(state);

            // F bottom edge = 200 + 15 = 215
            // S should have bottom edge at 215, so center = 215 - 40 = 175
            assertThat(state.getY("S")).isCloseTo(175, within(0.01));
            assertThat(state.getY("F")).isCloseTo(200, within(0.01));
        }
    }

    @Nested
    @DisplayName("does not affect unselected elements")
    class UnselectedElements {

        @Test
        void shouldNotMoveUnselectedElements() {
            state.select("A");
            state.addToSelection("B");
            // C is not selected

            AlignmentController.alignTop(state);

            assertThat(state.getY("C")).isCloseTo(250, within(0.01));
            assertThat(state.getX("C")).isCloseTo(500, within(0.01));
        }
    }
}
