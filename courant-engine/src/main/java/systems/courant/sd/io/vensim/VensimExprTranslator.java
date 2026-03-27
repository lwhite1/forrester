package systems.courant.sd.io.vensim;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Translates Vensim expression syntax to Courant expression syntax.
 *
 * <p>Performs text-level transformations including multi-word name replacement,
 * function name mapping, logical operator conversion, and lookup extraction.
 *
 * <p>Delegates to a {@link VensimExprTransformationPipeline} with four stages:
 * name normalization, operator transformation, function translation, and
 * subscript expansion.
 */
public final class VensimExprTranslator {

    private static final VensimExprTransformationPipeline DEFAULT_PIPELINE =
            VensimExprTransformationPipeline.defaultPipeline();

    /**
     * Result of translating a Vensim expression.
     *
     * @param expression the translated Courant expression
     * @param lookups any lookup tables extracted from WITH LOOKUP constructs
     * @param warnings any translation warnings
     */
    public record TranslationResult(
            String expression,
            List<ExtractedLookup> lookups,
            List<String> warnings
    ) {
        public TranslationResult {
            lookups = lookups == null ? List.of() : List.copyOf(lookups);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }
    }

    /**
     * A lookup table extracted from a WITH LOOKUP expression.
     *
     * @param name the generated lookup table name
     * @param xValues the x-axis data points
     * @param yValues the y-axis data points
     */
    public record ExtractedLookup(String name, double[] xValues, double[] yValues) {
        public ExtractedLookup {
            xValues = xValues.clone();
            yValues = yValues.clone();
        }

        @Override
        public double[] xValues() {
            return xValues.clone();
        }

        @Override
        public double[] yValues() {
            return yValues.clone();
        }
    }

    private VensimExprTranslator() {
    }

    /**
     * Translates a Vensim expression to Courant syntax.
     *
     * @param vensimExpr the Vensim expression string
     * @param varName the name of the variable this expression belongs to (used for lookup naming)
     * @param knownNames the set of all known multi-word variable names (in original Vensim form)
     * @return the translation result
     */
    public static TranslationResult translate(String vensimExpr, String varName,
                                               Set<String> knownNames) {
        return translate(vensimExpr, varName, knownNames, Set.of());
    }

    /**
     * Translates a Vensim expression to Courant syntax.
     *
     * @param vensimExpr the Vensim expression string
     * @param varName the name of the variable this expression belongs to (used for lookup naming)
     * @param knownNames the set of all known multi-word variable names (in original Vensim form)
     * @param lookupNames the set of known lookup table names (normalized); function calls matching
     *                    these are rewritten to LOOKUP(name, input) syntax
     * @return the translation result
     */
    public static TranslationResult translate(String vensimExpr, String varName,
                                               Set<String> knownNames,
                                               Set<String> lookupNames) {
        return translate(vensimExpr, varName, knownNames, lookupNames, Map.of());
    }

    /**
     * Translates a Vensim expression to Courant syntax with subscript dimension info
     * for expanding vector functions like SUM and VMIN.
     *
     * @param vensimExpr the Vensim expression string
     * @param varName the name of the variable this expression belongs to (used for lookup naming)
     * @param knownNames the set of all known multi-word variable names (in original Vensim form)
     * @param lookupNames the set of known lookup table names (normalized)
     * @param subscriptDimensions map from normalized dimension name to its normalized labels
     * @return the translation result
     */
    public static TranslationResult translate(String vensimExpr, String varName,
                                               Set<String> knownNames,
                                               Set<String> lookupNames,
                                               Map<String, List<String>> subscriptDimensions) {
        if (vensimExpr == null || vensimExpr.isBlank()) {
            return new TranslationResult(vensimExpr, List.of(), List.of());
        }

        TranslationContext ctx = new TranslationContext(
                vensimExpr.strip(), varName, knownNames, lookupNames, subscriptDimensions);
        DEFAULT_PIPELINE.transform(ctx);
        return new TranslationResult(ctx.expression(), ctx.lookups(), ctx.warnings());
    }

    /**
     * Normalizes a Vensim variable name to Courant identifier format.
     * Converts spaces to underscores, strips quotes, and trims whitespace.
     * Use this form for equation references (identifiers in formula text).
     *
     * @param vensimName the Vensim variable name
     * @return the normalized name with underscores instead of spaces
     */
    public static String normalizeName(String vensimName) {
        if (vensimName == null || vensimName.isBlank()) {
            return "";
        }
        String name = vensimName.strip();
        // Remove surrounding quotes if present
        if (name.startsWith("\"") && name.endsWith("\"") && name.length() > 2) {
            name = name.substring(1, name.length() - 1);
        }
        // Replace spaces and newlines with underscores
        name = name.replaceAll("\\s+", "_");
        // Remove any characters not valid in identifiers
        name = name.replaceAll("[^a-zA-Z0-9_]", "");
        // Ensure it doesn't start with a digit
        if (!name.isEmpty() && Character.isDigit(name.charAt(0))) {
            name = "_" + name;
        }
        return name;
    }

    /**
     * Normalizes a Vensim variable name to a human-readable display form.
     * Preserves spaces (collapsed to single space), strips quotes and special
     * characters. Use this form for element names displayed on the canvas,
     * in plots, and in dashboards.
     *
     * @param vensimName the Vensim variable name
     * @return the display name with spaces preserved
     */
    public static String normalizeDisplayName(String vensimName) {
        if (vensimName == null || vensimName.isBlank()) {
            return "";
        }
        String name = vensimName.strip();
        // Remove surrounding quotes if present
        if (name.startsWith("\"") && name.endsWith("\"") && name.length() > 2) {
            name = name.substring(1, name.length() - 1);
        }
        // Collapse whitespace to single spaces
        name = name.replaceAll("\\s+", " ");
        // Remove any characters not valid in display names
        name = name.replaceAll("[^a-zA-Z0-9_ ]", "");
        name = name.strip();
        // Ensure it doesn't start with a digit
        if (!name.isEmpty() && Character.isDigit(name.charAt(0))) {
            name = "_" + name;
        }
        return name;
    }
}
