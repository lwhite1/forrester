package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.def.AuxDef;
import systems.courant.shrewd.model.def.CausalLinkDef;
import systems.courant.shrewd.model.def.CldVariableDef;
import systems.courant.shrewd.model.def.ElementType;
import systems.courant.shrewd.model.def.FlowDef;
import systems.courant.shrewd.model.def.LookupTableDef;
import systems.courant.shrewd.model.def.ModuleInstanceDef;
import systems.courant.shrewd.model.def.StockDef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles copy/paste of diagram elements. Maintains an internal clipboard
 * and implements the two-pass paste algorithm (create elements, then reconnect
 * flows and remap equations).
 */
final class CopyPasteController {

    record PasteResult(List<String> pastedNames, Set<String> replacedReferences) {}

    record ClearResult(String equation, Set<String> replaced) {}

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
     * Returns the pasted element names and any references that were replaced with 0.
     * The caller is responsible for undo state, selection, and connector regeneration.
     */
    PasteResult paste(CanvasState canvasState, ModelEditor editor) {
        if (clipboard.isEmpty()) {
            return new PasteResult(List.of(), Set.of());
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
        Set<String> allReplaced = new LinkedHashSet<>();

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

                editor.getFlowEquation(newName).ifPresent(eq -> {
                    String updated = remapEquationTokens(eq, nameMapping);
                    ClearResult cr = clearDanglingReferences(updated, editor);
                    allReplaced.addAll(cr.replaced());
                    if (!cr.equation().equals(eq)) {
                        editor.setFlowEquation(newName, cr.equation());
                    }
                });
            } else if (entry.type() == ElementType.AUX) {
                editor.getAuxEquation(newName).ifPresent(eq -> {
                    String updated = remapEquationTokens(eq, nameMapping);
                    ClearResult cr = clearDanglingReferences(updated, editor);
                    allReplaced.addAll(cr.replaced());
                    if (!cr.equation().equals(eq)) {
                        editor.setAuxEquation(newName, cr.equation());
                    }
                });
            } else if (entry.type() == ElementType.MODULE) {
                editor.getModuleByName(newName).ifPresent(module -> {
                    Map<String, String> newInputs =
                            remapInputBindings(module.inputBindings(), nameMapping, editor);
                    Map<String, String> newOutputs =
                            remapOutputBindings(module.outputBindings(), nameMapping, editor);
                    if (!newInputs.equals(module.inputBindings())
                            || !newOutputs.equals(module.outputBindings())) {
                        editor.updateModuleBindings(newName, newInputs, newOutputs);
                    }
                });
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

        return new PasteResult(pastedNames, allReplaced);
    }

    /**
     * Remaps input binding expressions using the name mapping and clears dangling
     * references. Input bindings contain equation expressions where "0" is a valid
     * replacement for missing references.
     */
    private static Map<String, String> remapInputBindings(Map<String, String> bindings,
                                                          Map<String, String> nameMapping,
                                                          ModelEditor editor) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> binding : bindings.entrySet()) {
            String value = binding.getValue();
            if (value != null && !value.isBlank()) {
                value = remapEquationTokens(value, nameMapping);
                value = clearDanglingReferences(value, editor).equation();
            }
            result.put(binding.getKey(), value);
        }
        return result;
    }

    /**
     * Remaps output binding alias names using the name mapping. Output bindings map
     * port names to element names in the parent model — "0" is not a meaningful alias,
     * so dangling entries are removed entirely (the port becomes unbound).
     */
    private static Map<String, String> remapOutputBindings(Map<String, String> bindings,
                                                           Map<String, String> nameMapping,
                                                           ModelEditor editor) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> binding : bindings.entrySet()) {
            String value = binding.getValue();
            if (value == null || value.isBlank()) {
                result.put(binding.getKey(), value);
                continue;
            }
            // Remap the alias name
            String underscored = value.replace(' ', '_');
            for (Map.Entry<String, String> mapping : nameMapping.entrySet()) {
                if (underscored.equals(mapping.getKey().replace(' ', '_'))) {
                    value = mapping.getValue().replace(' ', '_');
                    break;
                }
            }
            // Keep only if the target element exists
            if (editor.hasElement(value) || editor.hasElement(value.replace('_', ' '))) {
                result.put(binding.getKey(), value);
            }
            // Otherwise omit — the port becomes unbound
        }
        return result;
    }

    /**
     * Replaces equation tokens that reference original names with their new names.
     * Handles both unquoted underscore-form tokens and backtick-quoted identifiers.
     */
    static String remapEquationTokens(String equation, Map<String, String> nameMapping) {
        // First pass: remap unquoted tokens
        String result = equation;
        for (Map.Entry<String, String> mapping : nameMapping.entrySet()) {
            String oldToken = mapping.getKey().replace(' ', '_');
            String newToken = mapping.getValue().replace(' ', '_');
            result = EquationReferenceManager.replaceToken(result, oldToken, newToken);
        }
        // Second pass: remap backtick-quoted identifiers
        result = remapBacktickTokens(result, nameMapping);
        return result;
    }

    /**
     * Scans for backtick-quoted identifiers and remaps them using the name mapping.
     */
    private static String remapBacktickTokens(String equation, Map<String, String> nameMapping) {
        if (equation.indexOf('`') < 0) {
            return equation;
        }
        // Build a lookup from space-form names to new space-form names
        Map<String, String> spaceMapping = new HashMap<>();
        for (Map.Entry<String, String> mapping : nameMapping.entrySet()) {
            spaceMapping.put(mapping.getKey().replace('_', ' '), mapping.getValue().replace('_', ' '));
        }

        StringBuilder sb = new StringBuilder();
        int len = equation.length();
        int i = 0;
        while (i < len) {
            if (equation.charAt(i) == '`') {
                int close = equation.indexOf('`', i + 1);
                if (close < 0) {
                    sb.append(equation, i, len);
                    break;
                }
                String inner = equation.substring(i + 1, close);
                String mapped = spaceMapping.get(inner);
                if (mapped != null) {
                    sb.append('`').append(mapped).append('`');
                } else {
                    sb.append('`').append(inner).append('`');
                }
                i = close + 1;
            } else {
                sb.append(equation.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }

    private static final Set<String> EQUATION_KEYWORDS = Set.of("TIME", "DT", "IF");

    /**
     * Replaces identifier tokens in the equation that do not correspond to any element
     * in the target editor with "0". This prevents dangling references when pasting
     * elements whose equations reference elements that were not part of the selection.
     * Numbers, keywords (TIME, DT, IF), and function calls (token followed by '(') are
     * left untouched. Backtick-quoted identifiers are also checked and replaced if dangling.
     * Returns the cleaned equation and the set of replaced reference names.
     */
    static ClearResult clearDanglingReferences(String equation, ModelEditor editor) {
        StringBuilder result = new StringBuilder();
        Set<String> replaced = new LinkedHashSet<>();
        int len = equation.length();
        int i = 0;

        while (i < len) {
            char c = equation.charAt(i);

            // Handle backtick-quoted identifiers
            if (c == '`') {
                int close = equation.indexOf('`', i + 1);
                if (close < 0) {
                    result.append(equation, i, len);
                    break;
                }
                String inner = equation.substring(i + 1, close);
                if (editor.hasElement(inner) || editor.hasElement(inner.replace(' ', '_'))) {
                    result.append('`').append(inner).append('`');
                } else {
                    replaced.add(inner);
                    result.append("0");
                }
                i = close + 1;
                continue;
            }

            if (!EquationReferenceManager.isTokenChar(c)) {
                result.append(c);
                i++;
                continue;
            }

            // Extract the full token
            int start = i;
            while (i < len && EquationReferenceManager.isTokenChar(equation.charAt(i))) {
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
            if (editor.hasElement(token) || editor.hasElement(token.replace('_', ' '))) {
                result.append(token);
            } else {
                replaced.add(token.replace('_', ' '));
                result.append("0");
            }
        }

        return new ClearResult(result.toString(), replaced);
    }
}
