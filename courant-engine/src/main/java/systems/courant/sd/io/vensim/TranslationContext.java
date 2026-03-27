package systems.courant.sd.io.vensim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Mutable context carried through the Vensim expression transformation pipeline.
 *
 * <p>Holds the expression being translated, reference data needed by the stages,
 * and accumulates extracted lookups and warnings.
 */
final class TranslationContext {

    private String expression;
    private final String varName;
    private final Set<String> knownNames;
    private final Set<String> knownNamesLower;
    private final Set<String> lookupNames;
    private final Map<String, List<String>> subscriptDimensions;
    private final List<VensimExprTranslator.ExtractedLookup> lookups;
    private final List<String> warnings;

    TranslationContext(String expression, String varName,
                       Set<String> knownNames, Set<String> lookupNames,
                       Map<String, List<String>> subscriptDimensions) {
        this.expression = expression;
        this.varName = varName;
        this.knownNames = knownNames;
        this.lookupNames = lookupNames != null ? lookupNames : Set.of();
        this.subscriptDimensions = subscriptDimensions != null ? subscriptDimensions : Map.of();
        this.lookups = new ArrayList<>();
        this.warnings = new ArrayList<>();

        this.knownNamesLower = new HashSet<>(knownNames.size());
        for (String name : knownNames) {
            this.knownNamesLower.add(name.toLowerCase(Locale.ROOT));
        }
    }

    String expression() {
        return expression;
    }

    void setExpression(String expression) {
        this.expression = expression;
    }

    String varName() {
        return varName;
    }

    Set<String> knownNames() {
        return knownNames;
    }

    Set<String> knownNamesLower() {
        return knownNamesLower;
    }

    Set<String> lookupNames() {
        return lookupNames;
    }

    Map<String, List<String>> subscriptDimensions() {
        return subscriptDimensions;
    }

    List<VensimExprTranslator.ExtractedLookup> lookups() {
        return lookups;
    }

    List<String> warnings() {
        return warnings;
    }
}
