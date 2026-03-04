package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.ElementType;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.LookupTableDef;
import com.deathrayresearch.forrester.model.def.ModuleInstanceDef;
import com.deathrayresearch.forrester.model.def.StockDef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles copy/paste of diagram elements. Maintains an internal clipboard
 * and implements the two-pass paste algorithm (create elements, then reconnect
 * flows and remap equations).
 */
final class CopyPasteController {

    private final Clipboard clipboard;

    CopyPasteController(Clipboard clipboard) {
        this.clipboard = clipboard;
    }

    boolean hasContent() {
        return !clipboard.isEmpty();
    }

    /**
     * Copies the current selection to the clipboard.
     */
    void copy(CanvasState canvasState, ModelEditor editor) {
        Set<String> selection = canvasState.getSelection();
        if (selection.isEmpty()) {
            return;
        }
        clipboard.capture(canvasState, editor, selection);
    }

    /**
     * Pastes clipboard contents, creating new elements offset from the originals.
     * Returns the names of all pasted elements, or an empty list if nothing was pasted.
     * The caller is responsible for undo state, selection, and connector regeneration.
     */
    List<String> paste(CanvasState canvasState, ModelEditor editor) {
        if (clipboard.isEmpty()) {
            return List.of();
        }

        double offsetX = 30;
        double offsetY = 30;

        // Compute anchor from current selection or default offset
        Set<String> currentSel = canvasState.getSelection();
        double anchorX;
        double anchorY;
        if (!currentSel.isEmpty()) {
            double sx = 0;
            double sy = 0;
            int cnt = 0;
            for (String n : currentSel) {
                double nx = canvasState.getX(n);
                double ny = canvasState.getY(n);
                if (!Double.isNaN(nx) && !Double.isNaN(ny)) {
                    sx += nx;
                    sy += ny;
                    cnt++;
                }
            }
            anchorX = cnt > 0 ? sx / cnt + offsetX : offsetX;
            anchorY = cnt > 0 ? sy / cnt + offsetY : offsetY;
        } else {
            anchorX = offsetX;
            anchorY = offsetY;
        }

        Map<String, String> nameMapping = new HashMap<>();
        List<String> pastedNames = new ArrayList<>();

        // First pass: create elements and build name mapping
        for (Clipboard.Entry entry : clipboard.getEntries()) {
            String newName;
            switch (entry.type()) {
                case STOCK -> newName = editor.addStockFrom((StockDef) entry.elementDef());
                case FLOW -> {
                    FlowDef flowDef = (FlowDef) entry.elementDef();
                    newName = editor.addFlowFrom(flowDef, null, null);
                }
                case AUX -> {
                    AuxDef auxDef = (AuxDef) entry.elementDef();
                    newName = editor.addAuxFrom(auxDef, auxDef.equation());
                }
                case CONSTANT -> newName = editor.addConstantFrom(
                        (ConstantDef) entry.elementDef());
                case MODULE -> newName = editor.addModuleFrom(
                        (ModuleInstanceDef) entry.elementDef());
                case LOOKUP -> newName = editor.addLookupFrom(
                        (LookupTableDef) entry.elementDef());
                default -> { continue; }
            }

            nameMapping.put(entry.originalName(), newName);
            pastedNames.add(newName);

            double px = anchorX + entry.relativeX();
            double py = anchorY + entry.relativeY();
            canvasState.addElement(newName, entry.type(), px, py);

            if (entry.customWidth() > 0 && entry.customHeight() > 0) {
                canvasState.setSize(newName, entry.customWidth(), entry.customHeight());
            }
        }

        // Second pass: reconnect flows and update equations
        for (Clipboard.Entry entry : clipboard.getEntries()) {
            String newName = nameMapping.get(entry.originalName());
            if (newName == null) {
                continue;
            }

            if (entry.type() == ElementType.FLOW) {
                FlowDef original = (FlowDef) entry.elementDef();
                String newSource = original.source() != null
                        ? nameMapping.get(original.source()) : null;
                String newSink = original.sink() != null
                        ? nameMapping.get(original.sink()) : null;
                if (newSource != null) {
                    editor.reconnectFlow(newName,
                            FlowEndpointCalculator.FlowEnd.SOURCE, newSource);
                }
                if (newSink != null) {
                    editor.reconnectFlow(newName,
                            FlowEndpointCalculator.FlowEnd.SINK, newSink);
                }

                String eq = editor.getFlowEquation(newName);
                if (eq != null) {
                    String updated = remapEquationTokens(eq, nameMapping);
                    if (!updated.equals(eq)) {
                        editor.setFlowEquation(newName, updated);
                    }
                }
            } else if (entry.type() == ElementType.AUX) {
                String eq = editor.getAuxEquation(newName);
                if (eq != null) {
                    String updated = remapEquationTokens(eq, nameMapping);
                    if (!updated.equals(eq)) {
                        editor.setAuxEquation(newName, updated);
                    }
                }
            }
        }

        return pastedNames;
    }

    /**
     * Replaces equation tokens that reference original names with their new names.
     */
    static String remapEquationTokens(String equation, Map<String, String> nameMapping) {
        String result = equation;
        for (Map.Entry<String, String> mapping : nameMapping.entrySet()) {
            String oldToken = mapping.getKey().replace(' ', '_');
            String newToken = mapping.getValue().replace(' ', '_');
            result = ModelEditor.replaceToken(result, oldToken, newToken);
        }
        return result;
    }
}
