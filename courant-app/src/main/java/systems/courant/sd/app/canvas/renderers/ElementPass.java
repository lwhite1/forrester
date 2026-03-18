package systems.courant.sd.app.canvas.renderers;

import javafx.scene.canvas.GraphicsContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.ModelEditor;
import systems.courant.sd.model.def.ElementType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Draws all model elements (stocks, flows, variables, modules, etc.)
 * using the per-type {@link ElementTypeRenderer} strategy.
 */
final class ElementPass implements RenderPass {

    private static final Logger log = LoggerFactory.getLogger(ElementPass.class);

    private static final Map<ElementType, ElementTypeRenderer> ELEMENT_RENDERERS;

    static {
        Map<ElementType, ElementTypeRenderer> map = new EnumMap<>(ElementType.class);
        map.put(ElementType.STOCK, new StockRenderer());
        map.put(ElementType.FLOW, new FlowRenderer());
        map.put(ElementType.AUX, new AuxRenderer());
        map.put(ElementType.MODULE, new ModuleRenderer());
        map.put(ElementType.LOOKUP, new LookupRenderer());
        map.put(ElementType.CLD_VARIABLE, new CldVariableRenderer());
        map.put(ElementType.COMMENT, new CommentRenderer());
        ELEMENT_RENDERERS = Map.copyOf(map);
    }

    private final CanvasState canvasState;

    ElementPass(CanvasState canvasState) {
        this.canvasState = canvasState;
    }

    @Override
    public void render(GraphicsContext gc, CanvasRenderer.RenderContext ctx) {
        ModelEditor editor = ctx.editor();
        boolean hideAux = ctx.hideVariables();
        boolean showDelay = ctx.showDelayBadges();

        for (String name : canvasState.getDrawOrder()) {
            ElementType type = canvasState.getType(name).orElse(null);
            if (type == null || (hideAux && type == ElementType.AUX)) {
                continue;
            }
            ElementTypeRenderer renderer = ELEMENT_RENDERERS.get(type);
            if (renderer != null) {
                renderer.render(gc, name, canvasState.getX(name), canvasState.getY(name),
                        canvasState, editor, showDelay);
            } else {
                log.warn("No renderer registered for element type: {}", type);
            }
        }
    }
}
