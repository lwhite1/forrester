package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ElementPlacement;
import com.deathrayresearch.forrester.model.def.ViewDef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mutable canvas state layer that holds element positions and selection state.
 * Copies positions from an immutable {@link ViewDef} at load time so that
 * drag operations can modify positions without touching engine records.
 */
public class CanvasState {

    private final Map<String, double[]> positions = new LinkedHashMap<>();
    private final Map<String, String> types = new LinkedHashMap<>();
    private final Set<String> selection = new LinkedHashSet<>();
    private final List<String> drawOrder = new ArrayList<>();

    /**
     * Loads element positions and types from a ViewDef, clearing any previous state.
     */
    public void loadFrom(ViewDef view) {
        positions.clear();
        types.clear();
        selection.clear();
        drawOrder.clear();

        for (ElementPlacement ep : view.elements()) {
            positions.put(ep.name(), new double[]{ep.x(), ep.y()});
            types.put(ep.name(), ep.type());
            drawOrder.add(ep.name());
        }
    }

    /**
     * Returns the X position of the named element, or NaN if not found.
     */
    public double getX(String name) {
        double[] pos = positions.get(name);
        return pos != null ? pos[0] : Double.NaN;
    }

    /**
     * Returns the Y position of the named element, or NaN if not found.
     */
    public double getY(String name) {
        double[] pos = positions.get(name);
        return pos != null ? pos[1] : Double.NaN;
    }

    /**
     * Sets the position of the named element.
     */
    public void setPosition(String name, double x, double y) {
        double[] pos = positions.get(name);
        if (pos != null) {
            pos[0] = x;
            pos[1] = y;
        }
    }

    /**
     * Returns the type of the named element, or null if not found.
     */
    public String getType(String name) {
        return types.get(name);
    }

    /**
     * Returns all element names in draw order.
     */
    public List<String> getDrawOrder() {
        return Collections.unmodifiableList(drawOrder);
    }

    /**
     * Selects only the given element (clears previous selection first).
     */
    public void select(String name) {
        selection.clear();
        if (positions.containsKey(name)) {
            selection.add(name);
        }
    }

    /**
     * Toggles the selection of the given element.
     */
    public void toggleSelection(String name) {
        if (!positions.containsKey(name)) {
            return;
        }
        if (selection.contains(name)) {
            selection.remove(name);
        } else {
            selection.add(name);
        }
    }

    /**
     * Clears all selections.
     */
    public void clearSelection() {
        selection.clear();
    }

    /**
     * Returns true if the given element is selected.
     */
    public boolean isSelected(String name) {
        return selection.contains(name);
    }

    /**
     * Returns an unmodifiable view of the current selection.
     */
    public Set<String> getSelection() {
        return Collections.unmodifiableSet(selection);
    }

    /**
     * Returns true if positions map contains the named element.
     */
    public boolean hasElement(String name) {
        return positions.containsKey(name);
    }
}
