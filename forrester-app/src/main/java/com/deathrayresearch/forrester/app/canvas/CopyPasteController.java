package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.CausalLinkDef;
import com.deathrayresearch.forrester.model.def.CldVariableDef;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.ElementType;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.LookupTableDef;
import com.deathrayresearch.forrester.model.def.ModuleInstanceDef;
import com.deathrayresearch.forrester.model.def.StockDef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
            String newName = switch (entry.type()) {
                case STOCK -> editor.addStockFrom((StockDef) entry.elementDef());
                case FLOW -> editor.addFlowFrom((FlowDef) entry.elementDef(), null, null);
                case AUX -> {
                    AuxDef auxDef = (AuxDef) entry.elementDef();
                    yield editor.addAuxFrom(auxDef, auxDef.equation());
                }
                case CONSTANT -> editor.addConstantFrom(
                        (ConstantDef) entry.elementDef());
                case MODULE -> editor.addModuleFrom(
                        (ModuleInstanceDef) entry.elementDef());
                case LOOKUP -> editor.addLookupFrom(
                        (LookupTableDef) entry.elementDef());
                case CLD_VARIABLE -> editor.addCldVariableFrom(
                        (CldVariableDef) entry.elementDef());
            };

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
                    updated = clearDanglingReferences(updated, editor);
                    if (!updated.equals(eq)) {
                        editor.setFlowEquation(newName, updated);
                    }
                }
            } else if (entry.type() == ElementType.AUX) {
                String eq = editor.getAuxEquation(newName);
                if (eq != null) {
                    String updated = remapEquationTokens(eq, nameMapping);
                    updated = clearDanglingReferences(updated, editor);
                    if (!updated.equals(eq)) {
                        editor.setAuxEquation(newName, updated);
                    }
                }
            } else if (entry.type() == ElementType.MODULE) {
                ModuleInstanceDef module = editor.getModuleByName(newName);
                if (module != null) {
                    Map<String, String> newInputs =
                            remapBindings(module.inputBindings(), nameMapping, editor);
                    Map<String, String> newOutputs =
                            remapBindings(module.outputBindings(), nameMapping, editor);
                    if (!newInputs.equals(module.inputBindings())
                            || !newOutputs.equals(module.outputBindings())) {
                        editor.updateModuleBindings(newName, newInputs, newOutputs);
                    }
                }
            }
        }

        // Third pass: recreate causal links between pasted CLD variables
        for (CausalLinkDef link : clipboard.getCausalLinks()) {
            String newFrom = nameMapping.get(link.from());
            String newTo = nameMapping.get(link.to());
            if (newFrom != null && newTo != null) {
                editor.addCausalLink(newFrom, newTo, link.polarity());
            }
        }

        return pastedNames;
    }

    /**
     * Remaps all binding expression values using the name mapping and clears dangling
     * references against the target editor. Input bindings contain equation expressions;
     * output bindings contain alias names — both are remapped the same way.
     */
    private static Map<String, String> remapBindings(Map<String, String> bindings,
                                                     Map<String, String> nameMapping,
                                                     ModelEditor editor) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> binding : bindings.entrySet()) {
            String value = binding.getValue();
            if (value != null && !value.isBlank()) {
                value = remapEquationTokens(value, nameMapping);
                value = clearDanglingReferences(value, editor);
            }
            result.put(binding.getKey(), value);
        }
        return result;
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

    private static final Set<String> EQUATION_KEYWORDS = Set.of("TIME", "DT", "IF");

    /**
     * Replaces identifier tokens in the equation that do not correspond to any element
     * in the target editor with "0". This prevents dangling references when pasting
     * elements whose equations reference elements that were not part of the selection.
     * Numbers, keywords (TIME, DT, IF), and function calls (token followed by '(') are
     * left untouched.
     */
    static String clearDanglingReferences(String equation, ModelEditor editor) {
        StringBuilder result = new StringBuilder();
        int len = equation.length();
        int i = 0;

        while (i < len) {
            char c = equation.charAt(i);
            if (!ModelEditor.isTokenChar(c)) {
                result.append(c);
                i++;
                continue;
            }

            // Extract the full token
            int start = i;
            while (i < len && ModelEditor.isTokenChar(equation.charAt(i))) {
                i++;
            }
            String token = equation.substring(start, i);

            // Skip numeric literals (start with a digit)
            if (Character.isDigit(c)) {
                result.append(token);
                continue;
            }

            // Skip keywords
            if (EQUATION_KEYWORDS.contains(token)) {
                result.append(token);
                continue;
            }

            // Skip function calls (token followed by optional whitespace then '(')
            int peek = i;
            while (peek < len && Character.isWhitespace(equation.charAt(peek))) {
                peek++;
            }
            if (peek < len && equation.charAt(peek) == '(') {
                result.append(token);
                continue;
            }

            // Check if the element exists in the target editor
            // Try the token as-is first (handles names with underscores like Outflow_Rate),
            // then try with underscores replaced by spaces (the common convention).
            if (editor.hasElement(token) || editor.hasElement(token.replace('_', ' '))) {
                result.append(token);
            } else {
                result.append("0");
            }
        }

        return result.toString();
    }
}
