package systems.courant.sd.app.canvas;

import systems.courant.sd.api.ExpressionFacade;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Computes syntax highlighting {@link StyleSpans} for Courant equation text.
 * Recognizes built-in function keywords, numeric literals, operators, and parentheses.
 */
public final class EquationHighlighter {

    private EquationHighlighter() {
    }

    private static final Pattern TOKEN_PATTERN;

    static {
        // Build keyword alternation from the function registry, longest first
        // to prevent partial matches
        String keywords = ExpressionFacade.builtinFunctionNames().stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .collect(Collectors.joining("|"));

        String pattern = "(?<KEYWORD>\\b(?:" + keywords + ")\\b)"
                + "|(?<NUMBER>\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b)"
                + "|(?<OPERATOR>[+\\-*/^]|<=|>=|<>|<|>|=)"
                + "|(?<PAREN>[()])"
                + "|(?<COMMA>,)";

        TOKEN_PATTERN = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    }

    /**
     * Computes syntax highlighting spans for the given equation text.
     *
     * @param text the equation source text
     * @return style spans mapping character ranges to CSS style class collections
     */
    public static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = TOKEN_PATTERN.matcher(text);
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        int lastEnd = 0;

        while (matcher.find()) {
            // Gap before this match gets no style
            if (matcher.start() > lastEnd) {
                builder.add(List.of(), matcher.start() - lastEnd);
            }

            String styleClass;
            if (matcher.group("KEYWORD") != null) {
                styleClass = "keyword";
            } else if (matcher.group("NUMBER") != null) {
                styleClass = "number";
            } else if (matcher.group("OPERATOR") != null) {
                styleClass = "operator";
            } else if (matcher.group("PAREN") != null) {
                styleClass = "paren";
            } else {
                styleClass = "comma";
            }
            builder.add(List.of(styleClass), matcher.end() - matcher.start());
            lastEnd = matcher.end();
        }

        // Remaining text after last match
        if (lastEnd < text.length()) {
            builder.add(List.of(), text.length() - lastEnd);
        }

        // Handle empty text
        if (text.isEmpty()) {
            builder.add(List.of(), 0);
        }

        return builder.create();
    }
}
