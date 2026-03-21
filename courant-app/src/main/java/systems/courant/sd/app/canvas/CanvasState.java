package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ElementPlacement;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.ViewDef;

import systems.courant.sd.model.graph.CldLoopInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;

/**
 * Mutable canvas state layer that holds element positions and selection state.
 * Copies positions from an immutable {@link ViewDef} at load time so that
 * drag operations can modify positions without touching engine records.
 */
public class CanvasState {

    /**
     * An (x, y) position on the canvas in world coordinates.
     */
    public record Position(double x, double y) {}

    /**
     * Custom width and height for an element (0 values mean use default).
     */
    public record Size(double width, double height) {}

    private static final String DEFAULT_VIEW_NAME = "Main";

    private final Map<String, Position> positions = new LinkedHashMap<>();
    private final Map<String, Size> sizes = new LinkedHashMap<>();
    private final Map<String, ElementType> types = new LinkedHashMap<>();
    private final Set<String> selection = new LinkedHashSet<>();
    private final SequencedSet<String> drawOrder = new LinkedHashSet<>();
    private final Object drawOrderLock = new Object();
    private List<String> drawOrderCache;
    private String viewName = DEFAULT_VIEW_NAME;
    private CldLoopInfo cldLoopInfo;

    /**
     * Loads element positions and types from a ViewDef, clearing any previous state.
     */
    public void loadFrom(ViewDef view) {
        positions.clear();
        types.clear();
        sizes.clear();
        selection.clear();
        cldLoopInfo = null;
        viewName = view.name();

        synchronized (drawOrderLock) {
            drawOrder.clear();
            drawOrderCache = null;
            for (ElementPlacement ep : view.elements()) {
                positions.put(ep.name(), new Position(ep.x(), ep.y()));
                types.put(ep.name(), ep.type());
                if (ep.hasCustomSize()) {
                    sizes.put(ep.name(), new Size(ep.width(), ep.height()));
                } else if (ep.type() == ElementType.CLD_VARIABLE) {
                    double w = LayoutMetrics.cldVarWidthForName(ep.name());
                    sizes.put(ep.name(), new Size(w, LayoutMetrics.CLD_VAR_HEIGHT));
                } else if (ep.type() == ElementType.AUX) {
                    double w = LayoutMetrics.auxWidthForName(ep.name());
                    sizes.put(ep.name(), new Size(w, LayoutMetrics.AUX_HEIGHT));
                } else if (ep.type() == ElementType.LOOKUP) {
                    double w = LayoutMetrics.lookupWidthForName(ep.name());
                    sizes.put(ep.name(), new Size(w, LayoutMetrics.LOOKUP_HEIGHT));
                }
                drawOrder.add(ep.name());
            }
        }
    }

    /**
     * Returns the X position of the named element, or NaN if not found.
     */
    public double getX(String name) {
        Position pos = positions.get(name);
        return pos != null ? pos.x() : Double.NaN;
    }

    /**
     * Returns the Y position of the named element, or NaN if not found.
     */
    public double getY(String name) {
        Position pos = positions.get(name);
        return pos != null ? pos.y() : Double.NaN;
    }

    /**
     * Sets the position of the named element.
     */
    public void setPosition(String name, double x, double y) {
        if (positions.containsKey(name)) {
            positions.put(name, new Position(x, y));
        }
    }

    /**
     * Returns the custom width for the named element, or 0 if not set.
     */
    public double getWidth(String name) {
        Size size = sizes.get(name);
        return size != null ? size.width() : 0;
    }

    /**
     * Returns the custom height for the named element, or 0 if not set.
     */
    public double getHeight(String name) {
        Size size = sizes.get(name);
        return size != null ? size.height() : 0;
    }

    /**
     * Sets a custom size for the named element.
     */
    public void setSize(String name, double width, double height) {
        if (positions.containsKey(name)) {
            sizes.put(name, new Size(width, height));
        }
    }

    /**
     * Removes any custom size for the named element, reverting it to
     * the default (or auto-computed) size on the next render pass.
     */
    public void clearSize(String name) {
        sizes.remove(name);
    }

    /**
     * Returns true if the named element has a custom (non-default) size.
     */
    public boolean hasCustomSize(String name) {
        Size size = sizes.get(name);
        return size != null && size.width() > 0 && size.height() > 0;
    }

    /**
     * Returns the type of the named element.
     */
    public Optional<ElementType> getType(String name) {
        return Optional.ofNullable(types.get(name));
    }

    /**
     * Changes the type of an existing element (e.g. during CLD variable classification).
     */
    public void setType(String name, ElementType type) {
        if (types.containsKey(name)) {
            types.put(name, type);
        }
    }

    /**
     * Returns all element names in draw order.
     */
    public List<String> getDrawOrder() {
        synchronized (drawOrderLock) {
            List<String> cached = drawOrderCache;
            if (cached == null) {
                cached = List.copyOf(drawOrder);
                drawOrderCache = cached;
            }
            return cached;
        }
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
     * Selects all elements on the canvas.
     */
    public void selectAll() {
        selection.clear();
        selection.addAll(drawOrder);
    }

    /**
     * Adds the given element to the selection without clearing existing selections.
     */
    public void addToSelection(String name) {
        if (positions.containsKey(name)) {
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
     * Returns the CLD loop membership info, or null if not set.
     */
    public CldLoopInfo getCldLoopInfo() {
        return cldLoopInfo;
    }

    /**
     * Sets the CLD loop membership info for loop-aware edge routing.
     */
    public void setCldLoopInfo(CldLoopInfo loopInfo) {
        this.cldLoopInfo = loopInfo;
    }

    /**
     * Returns true if positions map contains the named element.
     */
    public boolean hasElement(String name) {
        return positions.containsKey(name);
    }

    /**
     * Adds a new element to the canvas state with the given position and type.
     * If an element with the same name already exists, it is overwritten.
     */
    public void addElement(String name, ElementType type, double x, double y) {
        positions.put(name, new Position(x, y));
        types.put(name, type);
        synchronized (drawOrderLock) {
            drawOrder.add(name);
            drawOrderCache = null;
        }
    }

    /**
     * Renames an element atomically across all canvas state maps (positions, types, draw order, selection).
     * @return true if the element was found and renamed
     */
    public boolean renameElement(String oldName, String newName) {
        if (!positions.containsKey(oldName) || oldName.equals(newName)
                || positions.containsKey(newName)) {
            return false;
        }

        Position pos = positions.remove(oldName);
        positions.put(newName, pos);

        ElementType type = types.remove(oldName);
        types.put(newName, type);

        Size size = sizes.remove(oldName);
        if (size != null) {
            sizes.put(newName, size);
        }

        synchronized (drawOrderLock) {
            LinkedHashSet<String> reordered = new LinkedHashSet<>(drawOrder.size());
            for (String name : drawOrder) {
                reordered.add(name.equals(oldName) ? newName : name);
            }
            drawOrder.clear();
            drawOrder.addAll(reordered);
            drawOrderCache = null;
        }

        if (selection.remove(oldName)) {
            selection.add(newName);
        }

        return true;
    }

    /**
     * Converts the current canvas state back to an immutable {@link ViewDef},
     * preserving element positions and draw order for serialization.
     */
    public ViewDef toViewDef() {
        List<ElementPlacement> placements = new ArrayList<>();
        List<String> order;
        synchronized (drawOrderLock) {
            order = List.copyOf(drawOrder);
        }
        for (String name : order) {
            ElementType type = types.get(name);
            if (type == null) {
                continue;
            }
            Position pos = positions.get(name);
            Size size = sizes.get(name);
            if (size != null && size.width() > 0 && size.height() > 0) {
                placements.add(new ElementPlacement(
                        name, type, pos.x(), pos.y(), size.width(), size.height()));
            } else {
                placements.add(new ElementPlacement(name, type, pos.x(), pos.y()));
            }
        }
        return new ViewDef(viewName, placements, List.of(), List.of());
    }

    /**
     * Removes the named element from all canvas state (positions, types, draw order, selection).
     */
    public void removeElement(String name) {
        positions.remove(name);
        types.remove(name);
        sizes.remove(name);
        synchronized (drawOrderLock) {
            drawOrder.remove(name);
            drawOrderCache = null;
        }
        selection.remove(name);
    }
}
