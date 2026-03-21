package systems.courant.sd.app.canvas.controllers;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.LayoutMetrics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Layout alignment and distribution operations for selected canvas elements.
 * All operations require at least two selected elements (three for distribute).
 * Positions are center-based; alignment accounts for element bounding boxes
 * using effective width/height from {@link LayoutMetrics}.
 */
public final class AlignmentController {

    /** Default grid spacing in world-coordinate units. */
    public static final double GRID_SIZE = 20;

    private AlignmentController() {
    }

    // --- Alignment operations ---

    /**
     * Aligns the top edges of all selected elements to the topmost top edge.
     */
    public static void alignTop(CanvasState state) {
        List<String> selected = selectedList(state);
        if (selected.size() < 2) {
            return;
        }
        double minTop = Double.MAX_VALUE;
        for (String name : selected) {
            double top = state.getY(name) - LayoutMetrics.effectiveHeight(state, name) / 2;
            if (top < minTop) {
                minTop = top;
            }
        }
        for (String name : selected) {
            double halfH = LayoutMetrics.effectiveHeight(state, name) / 2;
            state.setPosition(name, state.getX(name), minTop + halfH);
        }
    }

    /**
     * Aligns the vertical centers of all selected elements to the average center Y.
     */
    public static void alignCenterVertical(CanvasState state) {
        List<String> selected = selectedList(state);
        if (selected.size() < 2) {
            return;
        }
        double sumY = 0;
        for (String name : selected) {
            sumY += state.getY(name);
        }
        double avgY = sumY / selected.size();
        for (String name : selected) {
            state.setPosition(name, state.getX(name), avgY);
        }
    }

    /**
     * Aligns the bottom edges of all selected elements to the bottommost bottom edge.
     */
    public static void alignBottom(CanvasState state) {
        List<String> selected = selectedList(state);
        if (selected.size() < 2) {
            return;
        }
        double maxBottom = -Double.MAX_VALUE;
        for (String name : selected) {
            double bottom = state.getY(name) + LayoutMetrics.effectiveHeight(state, name) / 2;
            if (bottom > maxBottom) {
                maxBottom = bottom;
            }
        }
        for (String name : selected) {
            double halfH = LayoutMetrics.effectiveHeight(state, name) / 2;
            state.setPosition(name, state.getX(name), maxBottom - halfH);
        }
    }

    /**
     * Aligns the left edges of all selected elements to the leftmost left edge.
     */
    public static void alignLeft(CanvasState state) {
        List<String> selected = selectedList(state);
        if (selected.size() < 2) {
            return;
        }
        double minLeft = Double.MAX_VALUE;
        for (String name : selected) {
            double left = state.getX(name) - LayoutMetrics.effectiveWidth(state, name) / 2;
            if (left < minLeft) {
                minLeft = left;
            }
        }
        for (String name : selected) {
            double halfW = LayoutMetrics.effectiveWidth(state, name) / 2;
            state.setPosition(name, minLeft + halfW, state.getY(name));
        }
    }

    /**
     * Aligns the horizontal centers of all selected elements to the average center X.
     */
    public static void alignCenterHorizontal(CanvasState state) {
        List<String> selected = selectedList(state);
        if (selected.size() < 2) {
            return;
        }
        double sumX = 0;
        for (String name : selected) {
            sumX += state.getX(name);
        }
        double avgX = sumX / selected.size();
        for (String name : selected) {
            state.setPosition(name, avgX, state.getY(name));
        }
    }

    /**
     * Aligns the right edges of all selected elements to the rightmost right edge.
     */
    public static void alignRight(CanvasState state) {
        List<String> selected = selectedList(state);
        if (selected.size() < 2) {
            return;
        }
        double maxRight = -Double.MAX_VALUE;
        for (String name : selected) {
            double right = state.getX(name) + LayoutMetrics.effectiveWidth(state, name) / 2;
            if (right > maxRight) {
                maxRight = right;
            }
        }
        for (String name : selected) {
            double halfW = LayoutMetrics.effectiveWidth(state, name) / 2;
            state.setPosition(name, maxRight - halfW, state.getY(name));
        }
    }

    // --- Distribution operations ---

    /**
     * Distributes selected elements evenly along the horizontal axis.
     * The leftmost and rightmost elements stay in place; intermediate elements
     * are spaced so the gaps between bounding-box edges are equal.
     * Requires at least three selected elements.
     */
    public static void distributeHorizontally(CanvasState state) {
        List<String> selected = selectedList(state);
        if (selected.size() < 3) {
            return;
        }
        selected.sort(Comparator.comparingDouble(name -> state.getX(name)));

        String first = selected.getFirst();
        String last = selected.getLast();
        double leftEdge = state.getX(first) - LayoutMetrics.effectiveWidth(state, first) / 2;
        double rightEdge = state.getX(last) + LayoutMetrics.effectiveWidth(state, last) / 2;

        double totalWidths = 0;
        for (String name : selected) {
            totalWidths += LayoutMetrics.effectiveWidth(state, name);
        }

        double totalGap = (rightEdge - leftEdge) - totalWidths;
        double gapStep = totalGap / (selected.size() - 1);

        double cursor = leftEdge;
        for (String name : selected) {
            double w = LayoutMetrics.effectiveWidth(state, name);
            double newCenterX = cursor + w / 2;
            state.setPosition(name, newCenterX, state.getY(name));
            cursor += w + gapStep;
        }
    }

    /**
     * Distributes selected elements evenly along the vertical axis.
     * The topmost and bottommost elements stay in place; intermediate elements
     * are spaced so the gaps between bounding-box edges are equal.
     * Requires at least three selected elements.
     */
    public static void distributeVertically(CanvasState state) {
        List<String> selected = selectedList(state);
        if (selected.size() < 3) {
            return;
        }
        selected.sort(Comparator.comparingDouble(name -> state.getY(name)));

        String first = selected.getFirst();
        String last = selected.getLast();
        double topEdge = state.getY(first) - LayoutMetrics.effectiveHeight(state, first) / 2;
        double bottomEdge = state.getY(last) + LayoutMetrics.effectiveHeight(state, last) / 2;

        double totalHeights = 0;
        for (String name : selected) {
            totalHeights += LayoutMetrics.effectiveHeight(state, name);
        }

        double totalGap = (bottomEdge - topEdge) - totalHeights;
        double gapStep = totalGap / (selected.size() - 1);

        double cursor = topEdge;
        for (String name : selected) {
            double h = LayoutMetrics.effectiveHeight(state, name);
            double newCenterY = cursor + h / 2;
            state.setPosition(name, state.getX(name), newCenterY);
            cursor += h + gapStep;
        }
    }

    // --- Grid snap ---

    /**
     * Snaps all selected element centers to the nearest grid point.
     * Uses {@link #GRID_SIZE} as the grid spacing.
     */
    public static void snapToGrid(CanvasState state) {
        snapToGrid(state, GRID_SIZE);
    }

    /**
     * Snaps all selected element centers to the nearest grid point with
     * the given grid spacing.
     */
    public static void snapToGrid(CanvasState state, double gridSize) {
        Set<String> selected = state.getSelection();
        if (selected.isEmpty()) {
            return;
        }
        for (String name : selected) {
            double x = Math.round(state.getX(name) / gridSize) * gridSize;
            double y = Math.round(state.getY(name) / gridSize) * gridSize;
            state.setPosition(name, x, y);
        }
    }

    // --- Helpers ---

    private static List<String> selectedList(CanvasState state) {
        return new ArrayList<>(state.getSelection());
    }
}
