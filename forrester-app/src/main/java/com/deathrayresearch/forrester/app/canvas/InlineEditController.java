package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.ElementType;
import com.deathrayresearch.forrester.model.def.FlowDef;

import javafx.scene.layout.Pane;

import java.util.function.Consumer;

/**
 * Manages inline editing of element names, values, and equations on the canvas.
 * Handles the double-click edit sequences: name-only for stocks/modules,
 * name-then-value for constants, name-then-equation for flows and auxiliaries.
 */
final class InlineEditController {

    /**
     * Callbacks for communicating edit results back to the canvas.
     */
    interface Callbacks {
        void applyRename(String oldName, String newName);
        void saveAndSetConstantValue(String name, double value);
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

        ElementType type = canvasState.getType(elementName);
        if (type == null) {
            return;
        }

        double worldX = canvasState.getX(elementName);
        double worldY = canvasState.getY(elementName);
        double screenX = viewport.toScreenX(worldX);
        double screenY = viewport.toScreenY(worldY);
        double fieldWidth = (LayoutMetrics.widthFor(type) + 20) * viewport.getScale();
        double scale = viewport.getScale();

        switch (type) {
            case CONSTANT -> startNameEditThenChain(elementName, screenX, screenY,
                    fieldWidth, callbacks,
                    name -> startConstantValueEdit(name, editor,
                            screenX, screenY, fieldWidth, scale, callbacks));
            case FLOW -> startNameEditThenChain(elementName, screenX, screenY,
                    fieldWidth, callbacks,
                    name -> startFlowEquationEdit(name, editor,
                            screenX, screenY, scale, callbacks));
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

    private void startConstantValueEdit(String constantName, ModelEditor editor,
                                        double screenX, double screenY,
                                        double fieldWidth, double scale,
                                        Callbacks callbacks) {
        ConstantDef cd = editor.getConstantByName(constantName);
        String currentValue = cd != null ? ElementRenderer.formatValue(cd.value()) : "0";

        double valueScreenY = screenY + 16 * scale;

        inlineEditor.open(screenX, valueScreenY, currentValue, fieldWidth, valueText -> {
            if (valueText != null && !valueText.isBlank()) {
                try {
                    double value = Double.parseDouble(valueText);
                    callbacks.saveAndSetConstantValue(constantName, value);
                } catch (NumberFormatException ignored) {
                    // Invalid number — ignore
                }
            }
            callbacks.postEdit();
        });
    }

    private void startFlowEquationEdit(String flowName, ModelEditor editor,
                                       double screenX, double screenY, double scale,
                                       Callbacks callbacks) {
        FlowDef fd = editor.getFlowByName(flowName);
        String currentEquation = fd != null ? fd.equation() : "0";

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
    }

    private void startAuxEquationEdit(String auxName, ModelEditor editor,
                                      double screenX, double screenY, double scale,
                                      Callbacks callbacks) {
        AuxDef ad = editor.getAuxByName(auxName);
        String currentEquation = ad != null ? ad.equation() : "0";

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
    }
}
