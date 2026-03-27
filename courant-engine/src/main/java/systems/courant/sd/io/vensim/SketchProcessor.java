package systems.courant.sd.io.vensim;

import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.CommentDef;
import systems.courant.sd.model.def.ConnectorRoute;
import systems.courant.sd.model.def.ModelDefinitionBuilder;
import systems.courant.sd.model.def.ViewDef;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles sketch/view parsing from Vensim .mdl files. Separated from
 * {@link VensimImporter} so that sketch-related logic can be tested
 * and modified independently of equation parsing.
 */
final class SketchProcessor {

    private SketchProcessor() {
    }

    /**
     * Returns true if the model should be imported in CLD (causal loop diagram)
     * mode — when there are no stocks and no flow valves in the sketch.
     */
    static boolean detectCldMode(Set<String> stockNames, List<String> sketchLines) {
        boolean hasFlowValves = sketchLines.stream()
                .anyMatch(line -> line.strip().startsWith("11,"));
        return stockNames.isEmpty() && !hasFlowValves && !sketchLines.isEmpty();
    }

    /**
     * Extracts flow valve display names from sketch section type-11 lines.
     */
    static Set<String> extractSketchFlowValveNames(List<String> sketchLines) {
        Set<String> names = new HashSet<>();
        for (String line : sketchLines) {
            String trimmed = line.strip();
            if (!trimmed.startsWith("11,")) {
                continue;
            }
            String[] parts = trimmed.split(",");
            if (parts.length < 3) {
                continue;
            }
            String displayName = VensimExprTranslator.normalizeDisplayName(parts[2].strip());
            if (!displayName.isEmpty()) {
                names.add(displayName);
            }
        }
        return names;
    }

    /**
     * Parses sketch views from the .mdl sketch section and adds them to the
     * model builder. In CLD mode, also extracts causal links from connectors.
     */
    static void parseSketchViews(List<String> sketchLines,
                                  ModelDefinitionBuilder builder, boolean isCld,
                                  Set<String> stockNames, Set<String> sketchFlowNames,
                                  Set<String> lookupNames,
                                  Set<String> cldVariableNames) {
        if (sketchLines.isEmpty()) {
            return;
        }
        SketchParser.ParseResult result = SketchParser.parseWithComments(
                sketchLines, stockNames, sketchFlowNames, lookupNames,
                cldVariableNames);
        for (ViewDef view : result.views()) {
            builder.view(view);
            if (isCld) {
                for (ConnectorRoute connector : view.connectors()) {
                    builder.causalLink(new CausalLinkDef(
                            connector.from(), connector.to(),
                            connector.polarity()));
                }
            }
        }
        for (CommentDef comment : result.comments()) {
            builder.comment(comment);
        }
    }

    /**
     * Attempts to match a rate term expression to a sketch flow valve name.
     * Returns the matching display name when the term is a simple variable
     * reference that matches a sketch valve, or null if no match is found.
     */
    static String matchSketchValveName(String termExpr, Set<String> sketchValveNames) {
        if (termExpr == null || sketchValveNames.isEmpty()) {
            return null;
        }
        String stripped = termExpr.strip();
        if (stripped.contains("(") || stripped.contains(")") || stripped.contains("+")
                || stripped.contains("-") || stripped.contains("*") || stripped.contains("/")) {
            return null;
        }
        String displayName = VensimExprTranslator.normalizeDisplayName(stripped);
        if (sketchValveNames.contains(displayName)) {
            return displayName;
        }
        return null;
    }
}
