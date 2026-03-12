package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.def.CausalLinkDef;
import systems.courant.shrewd.model.def.ElementDef;
import systems.courant.shrewd.model.def.ElementType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Clipboard for copy/paste of model elements.
 * Captures element definitions and positions relative to the selection centroid.
 * A single instance can be shared across multiple windows for cross-window copy/paste.
 */
public class Clipboard {

    public record Entry(
            String originalName,
            ElementType type,
            double relativeX,
            double relativeY,
            double customWidth,
            double customHeight,
            ElementDef elementDef
    ) {}

    private final List<Entry> entries = new ArrayList<>();
    private final List<CausalLinkDef> causalLinks = new ArrayList<>();

    /**
     * Captures the selected elements into the clipboard.
     * Positions are stored relative to the centroid of the selection.
     * Also captures causal links where both endpoints are in the selection.
     */
    public void capture(CanvasState state, ModelEditor editor, Set<String> names) {
        entries.clear();
        causalLinks.clear();

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
            Optional<ElementType> typeOpt = state.getType(name);
            if (typeOpt.isEmpty()) {
                continue;
            }
            ElementType type = typeOpt.get();

            double rx = state.getX(name) - centroidX;
            double ry = state.getY(name) - centroidY;
            double cw = state.hasCustomSize(name) ? state.getWidth(name) : 0;
            double ch = state.hasCustomSize(name) ? state.getHeight(name) : 0;

            Optional<? extends ElementDef> defOpt = switch (type) {
                case STOCK -> editor.getStockByName(name);
                case FLOW -> editor.getFlowByName(name);
                case AUX -> editor.getVariableByName(name);
                case MODULE -> editor.getModuleByName(name);
                case LOOKUP -> editor.getLookupTableByName(name);
                case CLD_VARIABLE -> Optional.ofNullable(editor.getCldVariableByName(name));
                case COMMENT -> Optional.ofNullable(editor.getCommentByName(name));
            };

            defOpt.ifPresent(def -> entries.add(new Entry(name, type, rx, ry, cw, ch, def)));
        }

        // Capture causal links where both endpoints are in the selection
        for (CausalLinkDef link : editor.getCausalLinks()) {
            if (names.contains(link.from()) && names.contains(link.to())) {
                causalLinks.add(link);
            }
        }
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public List<Entry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public List<CausalLinkDef> getCausalLinks() {
        return Collections.unmodifiableList(causalLinks);
    }
}
