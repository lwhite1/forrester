package systems.courant.forrester.app.canvas;

import systems.courant.forrester.model.def.AuxDef;
import systems.courant.forrester.model.def.ElementType;
import systems.courant.forrester.model.def.FlowDef;

import javafx.scene.layout.Pane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Manages inline editing of element names, values, and equations on the canvas.
 * Handles the double-click edit sequences: name-only for stocks/modules,
 * name-then-value for constants, name-then-equation for flows and auxiliaries.
 */
final class InlineEditController {

    private static final Logger log = LoggerFactory.getLogger(InlineEditController.class);

    /**
     * Callbacks for communicating edit results back to the canvas.
     */
    interface Callbacks {
        void applyRename(String oldName, String newName);
        void saveAndSetFlowEquation(String name, String equation);
        void saveAndSetAuxEquation(String name, String equation);
        void postEdit();
    }

    private InlineEditor inlineEditor;

    void setOverlayPane(Pane overlayPane) {
        this.inlineEditor = new InlineEditor(overlayPane);
    }

    boolean isActive() {
        return inlineEditor != null && inlineEditor.isActive();
    }

    /**
     * Starts inline editing for the given element on double-click.
     */
    void startEdit(String elementName, CanvasState canvasState, ModelEditor editor,
                   Viewport viewport, Callbacks callbacks) {
        if (inlineEditor == null || inlineEditor.isActive()) {
            return;
        }

        ElementType type = canvasState.getType(elementName).orElse(null);
        if (type == null) {
            return;
        }

        double worldX = canvasState.getX(elementName);
        double worldY = canvasState.getY(elementName);
        double screenX = viewport.toScreenX(worldX);
        double screenY = viewport.toScreenY(worldY);
        double baseWidth = type == ElementType.FLOW
                ? LayoutMetrics.AUX_WIDTH
                : LayoutMetrics.widthFor(type);
        double fieldWidth = (baseWidth + 20) * viewport.getScale();
        double scale = viewport.getScale();

        switch (type) {
            case FLOW -> {
                double flowNameY = screenY
                        + (LayoutMetrics.FLOW_INDICATOR_SIZE / 2 + LayoutMetrics.FLOW_NAME_GAP) * scale;
                startNameEditThenChain(elementName, screenX, flowNameY,
                        fieldWidth, callbacks,
                        name -> startFlowEquationEdit(name, editor,
                                screenX, screenY, scale, callbacks));
            }
            case AUX -> startNameEditThenChain(elementName, screenX, screenY,
                    fieldWidth, callbacks,
                    name -> startAuxEquationEdit(name, editor,
                            screenX, screenY, scale, callbacks));
            default -> inlineEditor.open(screenX, screenY, elementName,
                    fieldWidth, newName -> {
                        if (newName != null && !newName.equals(elementName)
                                && ModelEditor.isValidName(newName)) {
                            callbacks.applyRename(elementName, newName);
                        }
                        callbacks.postEdit();
                    });
        }
    }

    private void startNameEditThenChain(String elementName, double screenX, double screenY,
                                        double fieldWidth, Callbacks callbacks,
                                        Consumer<String> chainAction) {
        inlineEditor.open(screenX, screenY, elementName, fieldWidth, newName -> {
            String effectiveName;
            if (newName != null && !newName.equals(elementName)
                    && ModelEditor.isValidName(newName)) {
                callbacks.applyRename(elementName, newName);
                effectiveName = newName;
            } else {
                effectiveName = elementName;
            }
            chainAction.accept(effectiveName);
        });
    }

    private void startFlowEquationEdit(String flowName, ModelEditor editor,
                                       double screenX, double screenY, double scale,
                                       Callbacks callbacks) {
        String currentEquation = editor.getFlowByName(flowName)
                .map(FlowDef::equation).orElse("0");

        double eqScreenY = screenY
                + LayoutMetrics.FLOW_EQUATION_EDITOR_OFFSET * scale;
        double eqFieldWidth = Math.max(LayoutMetrics.EQUATION_EDITOR_MIN_WIDTH,
                LayoutMetrics.AUX_WIDTH + 20) * scale;

        inlineEditor.open(screenX, eqScreenY, currentEquation, eqFieldWidth, eqText -> {
            if (eqText != null && !eqText.isBlank()) {
                callbacks.saveAndSetFlowEquation(flowName, eqText);
            }
            callbacks.postEdit();
        });
        EquationAutoComplete.attach(inlineEditor.getTextField(), editor, flowName);
    }

    private void startAuxEquationEdit(String auxName, ModelEditor editor,
                                      double screenX, double screenY, double scale,
                                      Callbacks callbacks) {
        String currentEquation = editor.getAuxByName(auxName)
                .map(AuxDef::equation).orElse("0");

        double eqScreenY = screenY
                + LayoutMetrics.LABEL_SUBLABEL_OFFSET * scale;
        double eqFieldWidth = Math.max(LayoutMetrics.EQUATION_EDITOR_MIN_WIDTH,
                LayoutMetrics.AUX_WIDTH + 20) * scale;

        inlineEditor.open(screenX, eqScreenY, currentEquation, eqFieldWidth, eqText -> {
            if (eqText != null && !eqText.isBlank()) {
                callbacks.saveAndSetAuxEquation(auxName, eqText);
            }
            callbacks.postEdit();
        });
        EquationAutoComplete.attach(inlineEditor.getTextField(), editor, auxName);
    }
}
