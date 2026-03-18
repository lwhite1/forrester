package systems.courant.sd.io;

import systems.courant.sd.model.def.LookupTableDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.VariableDef;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Shared helpers for model exporters (Vensim, XMILE).
 * Centralises lookup-related parsing that was previously duplicated
 * in {@code VensimExporter} and {@code XmileExporter}.
 */
public final class ExportUtils {

    private static final Pattern LOOKUP_REF_PATTERN = Pattern.compile(
            "(?i)^LOOKUP\\s*\\(");

    private ExportUtils() {
    }

    /**
     * Extracts the lookup table name from a {@code LOOKUP(name, input)} equation.
     * Returns empty if the equation is not a simple LOOKUP call.
     */
    public static Optional<String> extractLookupReference(String equation) {
        if (equation == null) {
            return Optional.empty();
        }
        String trimmed = equation.strip();
        if (!LOOKUP_REF_PATTERN.matcher(trimmed).find()) {
            return Optional.empty();
        }
        int openParen = trimmed.indexOf('(');
        int closeParen = FormatUtils.findMatchingCloseParen(trimmed, openParen);
        if (closeParen < 0 || closeParen != trimmed.length() - 1) {
            return Optional.empty();
        }
        int comma = FormatUtils.findTopLevelComma(trimmed, openParen + 1);
        if (comma < 0) {
            return Optional.empty();
        }
        return Optional.of(trimmed.substring(openParen + 1, comma).strip());
    }

    /**
     * Extracts the input expression from a {@code LOOKUP(name, input)} equation.
     * Returns empty if the equation is not a simple LOOKUP call.
     */
    public static Optional<String> extractLookupInput(String equation) {
        if (equation == null) {
            return Optional.empty();
        }
        String trimmed = equation.strip();
        if (!LOOKUP_REF_PATTERN.matcher(trimmed).find()) {
            return Optional.empty();
        }
        int openParen = trimmed.indexOf('(');
        int closeParen = FormatUtils.findMatchingCloseParen(trimmed, openParen);
        if (closeParen < 0 || closeParen != trimmed.length() - 1) {
            return Optional.empty();
        }
        int comma = FormatUtils.findTopLevelComma(trimmed, openParen + 1);
        if (comma < 0) {
            return Optional.empty();
        }
        return Optional.of(trimmed.substring(comma + 1, closeParen).strip());
    }

    /**
     * Collects the names of all lookup tables that are embedded (referenced via
     * {@code LOOKUP(name, ...)}) in variable equations.
     */
    public static Set<String> collectEmbeddedLookupNames(ModelDefinition def) {
        Set<String> names = new HashSet<>();
        for (VariableDef v : def.variables()) {
            extractLookupReference(v.equation()).ifPresent(names::add);
        }
        return names;
    }

    /**
     * Finds a lookup table definition by name.
     */
    public static Optional<LookupTableDef> findLookup(ModelDefinition def, String name) {
        for (LookupTableDef lt : def.lookupTables()) {
            if (lt.name().equals(name)) {
                return Optional.of(lt);
            }
        }
        return Optional.empty();
    }
}
