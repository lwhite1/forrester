package systems.courant.sd.app.canvas.renderers;

import javafx.scene.canvas.GraphicsContext;

import java.util.List;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.LayoutMetrics;
import systems.courant.sd.app.canvas.ModelEditor;
import systems.courant.sd.model.def.ModuleInterface;
import systems.courant.sd.model.def.PortDef;

/**
 * Renders module elements: thick bordered rectangle with port indicators
 * on left (inputs) and right (outputs) edges.
 */
final class ModuleRenderer implements ElementTypeRenderer {

    @Override
    public void render(GraphicsContext gc, String name, double cx, double cy,
                       CanvasState canvasState, ModelEditor editor, boolean showDelay) {
        double w = LayoutMetrics.effectiveWidth(canvasState, name);
        double h = LayoutMetrics.effectiveHeight(canvasState, name);
        List<String> inputPorts = List.of();
        List<String> outputPorts = List.of();
        var moduleOpt = editor.getModuleByName(name);
        if (moduleOpt.isPresent()) {
            ModuleInterface iface = moduleOpt.get().definition().moduleInterface();
            if (iface != null) {
                inputPorts = iface.inputs().stream()
                        .map(PortDef::name).toList();
                outputPorts = iface.outputs().stream()
                        .map(PortDef::name).toList();
            }
        }
        ElementRenderer.drawModule(gc, name, inputPorts, outputPorts,
                cx - w / 2, cy - h / 2, w, h);
    }
}
