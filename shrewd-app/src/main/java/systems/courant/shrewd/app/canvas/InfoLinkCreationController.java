package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.def.ModuleInstanceDef;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Two-click state machine for creating info links between elements and module ports.
 * <p>
 * First click selects a source (element or output port). Second click selects a target
 * (input port or element). Direction determines binding type:
 * <ul>
 *   <li>Element → Input port: creates an input binding</li>
 *   <li>Output port → Element: creates an output binding</li>
 * </ul>
 */
public class InfoLinkCreationController {

    /**
     * Immutable snapshot of the info link creation state, used by the renderer.
     */
    public record State(
            boolean pending,
            String sourceName,
            HitTester.PortHit sourcePort,
            double sourceX,
            double sourceY,
            double rubberBandEndX,
            double rubberBandEndY,
            HitTester.PortHit hoveredPort
    ) {
        static final State IDLE = new State(false, null, null, 0, 0, 0, 0, null);
    }

    /**
     * Result of an info link creation click attempt.
     */
    public record LinkResult(boolean success, String rejectionReason) {

        static LinkResult pending() {
            return new LinkResult(false, null);
        }

        static LinkResult created() {
            return new LinkResult(true, null);
        }

        static LinkResult rejected(String reason) {
            return new LinkResult(false, reason);
        }

        public boolean isCreated() {
            return success;
        }

        public boolean isRejected() {
            return rejectionReason != null;
        }
    }

    private boolean pending;
    private String pendingSourceName;
    private HitTester.PortHit pendingSourcePort;
    private double sourceX;
    private double sourceY;
    private double rubberBandEndX;
    private double rubberBandEndY;
    private HitTester.PortHit currentHoveredPort;

    /**
     * Handles a click during info link creation.
     * First click: sets source. Second click: creates the binding.
     */
    public LinkResult handleClick(double worldX, double worldY,
                                  CanvasState canvasState, ModelEditor editor) {
        if (!pending) {
            return handleFirstClick(worldX, worldY, canvasState, editor);
        } else {
            return handleSecondClick(worldX, worldY, canvasState, editor);
        }
    }

    private LinkResult handleFirstClick(double worldX, double worldY,
                                        CanvasState canvasState, ModelEditor editor) {
        // Try port hit first (only output ports accepted as source)
        HitTester.PortHit portHit = HitTester.hitTestPort(canvasState, editor, worldX, worldY);
        if (portHit != null) {
            if (!portHit.isInput()) {
                pending = true;
                pendingSourceName = null;
                pendingSourcePort = portHit;
                sourceX = portHit.portX();
                sourceY = portHit.portY();
                rubberBandEndX = worldX;
                rubberBandEndY = worldY;
                return LinkResult.pending();
            }
            // Input port clicked as source — fall through to element hit
        }

        // Try element hit
        String hit = HitTester.hitTest(canvasState, worldX, worldY);
        if (hit != null) {
            pending = true;
            pendingSourceName = hit;
            pendingSourcePort = null;
            sourceX = canvasState.getX(hit);
            sourceY = canvasState.getY(hit);
            rubberBandEndX = worldX;
            rubberBandEndY = worldY;
            return LinkResult.pending();
        }

        return LinkResult.rejected("Click on an element or output port to start drawing an info link");
    }

    private LinkResult handleSecondClick(double worldX, double worldY,
                                         CanvasState canvasState, ModelEditor editor) {
        // Try port hit first
        HitTester.PortHit portHit = HitTester.hitTestPort(canvasState, editor, worldX, worldY);

        if (pendingSourceName != null) {
            // Source is an element — target must be an input port
            if (portHit != null && portHit.isInput()) {
                return createInputBinding(pendingSourceName, portHit, editor);
            }
            // Element to element or element to output port — reject
            cancel();
            return LinkResult.rejected("Click on a module input port to complete the info link");
        }

        if (pendingSourcePort != null) {
            // Source is an output port — target must be an element
            String hit = HitTester.hitTest(canvasState, worldX, worldY);
            if (hit != null) {
                return createOutputBinding(pendingSourcePort, hit, editor);
            }
            cancel();
            return LinkResult.rejected("Click on an element to complete the info link");
        }

        cancel();
        return LinkResult.rejected("Invalid info link state");
    }

    private LinkResult createInputBinding(String elementName, HitTester.PortHit targetPort,
                                          ModelEditor editor) {
        String moduleName = targetPort.moduleName();
        var moduleOpt = editor.getModuleByName(moduleName);
        if (moduleOpt.isEmpty()) {
            cancel();
            return LinkResult.rejected("Module not found: " + moduleName);
        }

        ModuleInstanceDef module = moduleOpt.get();
        String expression = elementName.replace(' ', '_');

        // Check for duplicate binding
        if (expression.equals(module.inputBindings().get(targetPort.portName()))) {
            cancel();
            return LinkResult.rejected("This input binding already exists");
        }

        Map<String, String> newInputs = new LinkedHashMap<>(module.inputBindings());
        newInputs.put(targetPort.portName(), expression);
        editor.updateModuleBindings(moduleName, newInputs, module.outputBindings());
        cancel();
        return LinkResult.created();
    }

    private LinkResult createOutputBinding(HitTester.PortHit sourcePort, String elementName,
                                           ModelEditor editor) {
        String moduleName = sourcePort.moduleName();
        var moduleOpt = editor.getModuleByName(moduleName);
        if (moduleOpt.isEmpty()) {
            cancel();
            return LinkResult.rejected("Module not found: " + moduleName);
        }

        ModuleInstanceDef module = moduleOpt.get();

        // Check for duplicate binding
        if (elementName.equals(module.outputBindings().get(sourcePort.portName()))) {
            cancel();
            return LinkResult.rejected("This output binding already exists");
        }

        Map<String, String> newOutputs = new LinkedHashMap<>(module.outputBindings());
        newOutputs.put(sourcePort.portName(), elementName);
        editor.updateModuleBindings(moduleName, module.inputBindings(), newOutputs);
        cancel();
        return LinkResult.created();
    }

    /**
     * Updates the rubber-band endpoint and hovered port during mouse movement.
     */
    public void updateRubberBand(double worldX, double worldY) {
        if (pending) {
            rubberBandEndX = worldX;
            rubberBandEndY = worldY;
        }
    }

    /**
     * Updates the hovered port for visual feedback during tool use.
     */
    public void updateHoveredPort(double worldX, double worldY,
                                  CanvasState canvasState, ModelEditor editor) {
        currentHoveredPort = HitTester.hitTestPort(canvasState, editor, worldX, worldY);
    }

    /**
     * Cancels any pending info link creation.
     */
    public void cancel() {
        pending = false;
        pendingSourceName = null;
        pendingSourcePort = null;
        sourceX = 0;
        sourceY = 0;
        rubberBandEndX = 0;
        rubberBandEndY = 0;
        currentHoveredPort = null;
    }

    public boolean isPending() {
        return pending;
    }

    /**
     * Returns an immutable snapshot of the current state.
     */
    public State getState() {
        if (!pending) {
            return new State(false, null, null, 0, 0, 0, 0, currentHoveredPort);
        }
        return new State(true, pendingSourceName, pendingSourcePort,
                sourceX, sourceY, rubberBandEndX, rubberBandEndY, currentHoveredPort);
    }
}
