package systems.courant.sd.io.vensim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pipeline stage that normalizes variable names in Vensim expressions.
 *
 * <p>Handles:
 * <ul>
 *   <li>Quoted variable name replacement</li>
 *   <li>WITH LOOKUP extraction to separate lookup tables</li>
 *   <li>Multi-word name replacement (spaces → underscores)</li>
 *   <li>Consecutive identifier normalization (final cleanup)</li>
 * </ul>
 */
final class NameNormalizationStage implements ExprTransformationStage {

    private static final Logger log = LoggerFactory.getLogger(NameNormalizationStage.class);

    private static final Pattern QUOTED_NAME_PATTERN = Pattern.compile("\"([^\"]+)\"");
    private static final Pattern WITH_LOOKUP_PATTERN = Pattern.compile(
            "(?i)WITH\\s+LOOKUP\\s*\\(");
    private static final Pattern LOOKUP_RANGE_PATTERN = Pattern.compile(
            "^\\s*\\(?\\s*\\[\\s*\\([^)]*\\)\\s*-\\s*\\([^)]*\\)\\s*\\]\\s*,?");
    private static final Pattern LOOKUP_PAIR_PATTERN = Pattern.compile(
            "\\(\\s*(-?[\\d.eE+\\-]+)\\s*,\\s*(-?[\\d.eE+\\-]+)\\s*\\)");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?[\\d.eE+\\-]+");

    private static final Set<String> EXPRESSION_KEYWORDS = Set.of("and", "or", "not");
    private static final Pattern CONSECUTIVE_IDENTS = Pattern.compile(
            "([a-zA-Z_][a-zA-Z0-9_]*)\\s+([a-zA-Z_][a-zA-Z0-9_]*)");

    @Override
    public void apply(TranslationContext ctx) {
        String expr = ctx.expression();
        expr = translateQuotedNames(expr);
        expr = translateWithLookup(expr, ctx.varName(), ctx.lookups(), ctx.warnings());
        expr = replaceMultiWordNames(expr, ctx.knownNames());
        ctx.setExpression(expr);
    }

    /**
     * Applies the consecutive-identifier normalization pass (must run after all other stages).
     */
    void applyFinalCleanup(TranslationContext ctx) {
        ctx.setExpression(underscoreConsecutiveIdentifiers(ctx.expression()));
    }

    private static String translateQuotedNames(String expr) {
        Matcher m = QUOTED_NAME_PATTERN.matcher(expr);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String name = m.group(1);
            m.appendReplacement(sb,
                    Matcher.quoteReplacement(VensimExprTranslator.normalizeName(name)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String translateWithLookup(String expr, String varName,
                                               List<VensimExprTranslator.ExtractedLookup> lookups,
                                               List<String> warnings) {
        Matcher m = WITH_LOOKUP_PATTERN.matcher(expr);
        if (!m.find()) {
            return expr;
        }

        int funcStart = m.start();
        int argsStart = m.end();
        int closeParen = ExprParsingUtils.findMatchingParen(expr, argsStart - 1);
        if (closeParen < 0) {
            warnings.add("Malformed WITH LOOKUP expression");
            return expr;
        }

        String argsContent = expr.substring(argsStart, closeParen);
        int splitComma = ExprParsingUtils.findTopLevelComma(argsContent);
        if (splitComma < 0) {
            warnings.add("Malformed WITH LOOKUP: cannot find input/data separator");
            return expr;
        }

        String inputExpr = argsContent.substring(0, splitComma).strip();
        String lookupData = argsContent.substring(splitComma + 1).strip();

        Optional<double[][]> pointsOpt = parseLookupPoints(lookupData);
        if (pointsOpt.isEmpty() || pointsOpt.get()[0].length < 2) {
            warnings.add("Could not parse lookup data points in WITH LOOKUP");
            return expr;
        }
        double[][] points = pointsOpt.get();

        String lookupName = VensimExprTranslator.normalizeName(varName) + "_lookup";
        lookups.add(new VensimExprTranslator.ExtractedLookup(lookupName, points[0], points[1]));

        String replacement = "LOOKUP(" + lookupName + ", " + inputExpr + ")";
        return expr.substring(0, funcStart) + replacement + expr.substring(closeParen + 1);
    }

    private static String replaceMultiWordNames(String expr, Set<String> knownNames) {
        List<String> sortedNames = knownNames.stream()
                .filter(n -> n.contains(" "))
                .sorted(Comparator.comparingInt(String::length).reversed()
                        .thenComparing(Comparator.naturalOrder()))
                .toList();

        List<String> unmatchedNames = new ArrayList<>();
        for (String name : sortedNames) {
            String normalized = VensimExprTranslator.normalizeName(name);
            String escaped = Pattern.quote(name);
            Pattern exact = Pattern.compile(
                    "(?<![a-zA-Z0-9_])" + escaped + "(?![a-zA-Z0-9_])");
            Matcher m = exact.matcher(expr);
            if (m.find()) {
                expr = m.replaceAll(normalized);
            } else {
                unmatchedNames.add(name);
            }
        }

        for (String name : unmatchedNames) {
            String normalized = VensimExprTranslator.normalizeName(name);
            String escaped = Pattern.quote(name);
            Pattern p = Pattern.compile(
                    "(?<![a-zA-Z0-9_])" + escaped + "(?![a-zA-Z0-9_])",
                    Pattern.CASE_INSENSITIVE);
            expr = p.matcher(expr).replaceAll(normalized);
        }
        return expr;
    }

    static Optional<double[][]> parseLookupPoints(String data) {
        String cleaned = data.strip();

        Matcher rangeMatcher = LOOKUP_RANGE_PATTERN.matcher(cleaned);
        if (rangeMatcher.find()) {
            cleaned = cleaned.substring(rangeMatcher.end()).strip();
        } else {
            if (cleaned.startsWith("[")) {
                cleaned = cleaned.substring(1);
            }
            if (cleaned.endsWith("]")) {
                cleaned = cleaned.substring(0, cleaned.length() - 1);
            }
            cleaned = cleaned.strip();
        }

        List<double[]> points = new ArrayList<>();
        Matcher m = LOOKUP_PAIR_PATTERN.matcher(cleaned);
        while (m.find()) {
            try {
                double x = Double.parseDouble(m.group(1));
                double y = Double.parseDouble(m.group(2));
                points.add(new double[]{x, y});
            } catch (NumberFormatException ex) {
                log.debug("Skip malformed lookup pair: ({}, {})", m.group(1), m.group(2), ex);
            }
        }

        if (points.size() >= 2) {
            double[] xValues = new double[points.size()];
            double[] yValues = new double[points.size()];
            for (int i = 0; i < points.size(); i++) {
                xValues[i] = points.get(i)[0];
                yValues[i] = points.get(i)[1];
            }
            return Optional.of(new double[][]{xValues, yValues});
        }

        return parseFlatCsvLookup(cleaned);
    }

    private static Optional<double[][]> parseFlatCsvLookup(String data) {
        Matcher m = NUMBER_PATTERN.matcher(data);
        List<Double> values = new ArrayList<>();
        while (m.find()) {
            try {
                values.add(Double.parseDouble(m.group()));
            } catch (NumberFormatException ex) {
                log.debug("Skip malformed flat lookup value: {}", m.group(), ex);
            }
        }

        if (values.size() < 4 || values.size() % 2 != 0) {
            return Optional.empty();
        }

        int half = values.size() / 2;
        double[] xValues = new double[half];
        double[] yValues = new double[half];
        for (int i = 0; i < half; i++) {
            xValues[i] = values.get(i);
            yValues[i] = values.get(half + i);
        }
        return Optional.of(new double[][]{xValues, yValues});
    }

    static String underscoreConsecutiveIdentifiers(String expr) {
        String prev;
        do {
            prev = expr;
            Matcher m = CONSECUTIVE_IDENTS.matcher(expr);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                String left = m.group(1);
                String right = m.group(2);
                if (EXPRESSION_KEYWORDS.contains(left.toLowerCase(Locale.ROOT))
                        || EXPRESSION_KEYWORDS.contains(right.toLowerCase(Locale.ROOT))) {
                    m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
                } else {
                    m.appendReplacement(sb, Matcher.quoteReplacement(left + "_" + right));
                }
            }
            m.appendTail(sb);
            expr = sb.toString();
        } while (!expr.equals(prev));
        return expr;
    }
}
