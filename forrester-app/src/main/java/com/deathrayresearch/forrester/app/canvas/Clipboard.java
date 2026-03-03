package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.ElementType;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.ModuleInstanceDef;
import com.deathrayresearch.forrester.model.def.StockDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Package-private clipboard for copy/paste of model elements.
 * Captures element definitions and positions relative to the selection centroid.
 */
class Clipboard {

    record Entry(
            String originalName,
            ElementType type,
            double relativeX,
            double relativeY,
            double customWidth,
            double customHeight,
            Object elementDef
    ) {}

    private final List<Entry> entries = new ArrayList<>();

    /**
     * Captures the selected elements into the clipboard.
     * Positions are stored relative to the centroid of the selection.
     */
    void capture(CanvasState state, ModelEditor editor, Set<String> names) {
        entries.clear();

        if (names.isEmpty()) {
            return;
        }

        // Compute centroid
        double sumX = 0;
        double sumY = 0;
        int count = 0;
        for (String name : names) {
            double x = state.getX(name);
            double y = state.getY(name);
            if (!Double.isNaN(x) && !Double.isNaN(y)) {
                sumX += x;
                sumY += y;
                count++;
            }
        }
        if (count == 0) {
            return;
        }
        double centroidX = sumX / count;
        double centroidY = sumY / count;

        for (String name : names) {
            ElementType type = state.getType(name);
            if (type == null) {
                continue;
            }

            double rx = state.getX(name) - centroidX;
            double ry = state.getY(name) - centroidY;
            double cw = state.hasCustomSize(name) ? state.getWidth(name) : 0;
            double ch = state.hasCustomSize(name) ? state.getHeight(name) : 0;

            Object def = switch (type) {
                case STOCK -> editor.getStockByName(name);
                case FLOW -> editor.getFlowByName(name);
                case AUX -> editor.getAuxByName(name);
                case CONSTANT -> editor.getConstantByName(name);
                case MODULE -> editor.getModuleByName(name);
                default -> null;
            };

            if (def != null) {
                entries.add(new Entry(name, type, rx, ry, cw, ch, def));
            }
        }
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }

    List<Entry> getEntries() {
        return entries;
    }
}
