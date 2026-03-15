package systems.courant.sd.io.vensim;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Low-level parser for Vensim .mdl file format.
 *
 * <p>Splits raw .mdl text into a list of {@link MdlEquation} records and extracts
 * sketch section lines. This class handles the structural parsing only — expression
 * translation is handled by {@link VensimExprTranslator}.
 */
public final class MdlParser {

    private static final String SKETCH_SEPARATOR = "\\\\\\---///";
    private static final String GROUP_DELIMITER = "****";
    private static final Pattern GROUP_NAME_PATTERN = Pattern.compile(
            "^\\*{4,}\\s*$");
    private static final Pattern GROUP_HEADER_PATTERN = Pattern.compile(
            "^\\.(.+)$");
    private static final Pattern MACRO_START_PATTERN = Pattern.compile(
            "^:MACRO:", Pattern.CASE_INSENSITIVE);
    private static final Pattern MACRO_END_PATTERN = Pattern.compile(
            "^:END OF MACRO:", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTINUATION_PATTERN = Pattern.compile(
            "\\\\\n\\s*");
    private static final Pattern OPERATOR_PATTERN = Pattern.compile(
            "^((?:\"[^\"]*\"|[^=:()])+?)\\s*(<->|==|:=|=|:)\\s*(.*)$", Pattern.DOTALL);
    private static final Pattern LOOKUP_DEF_PATTERN = Pattern.compile(
            "^((?:\"[^\"]*\"|[^(])+?)\\s*\\(\\s*$", Pattern.DOTALL);

    private static final Pattern MACRO_HEADER_PATTERN = Pattern.compile(
            ":MACRO:\\s+(.+?)\\s*\\(([^)]*)\\)", Pattern.CASE_INSENSITIVE);

    /**
     * Result of parsing a .mdl file: equation blocks, macro definitions, and sketch lines.
     */
    public record ParsedMdl(List<MdlEquation> equations, List<MacroDef> macros,
                             List<String> sketchLines) {
        public ParsedMdl {
            equations = List.copyOf(equations);
            macros = List.copyOf(macros);
            sketchLines = List.copyOf(sketchLines);
        }

        /**
         * Backward-compatible constructor for callers that don't need macros.
         */
        public ParsedMdl(List<MdlEquation> equations, List<String> sketchLines) {
            this(equations, List.of(), sketchLines);
        }
    }

    private MdlParser() {
    }

    /**
     * Parses the raw content of a .mdl file.
     *
     * @param content the full .mdl file content
     * @return the parsed equations and sketch lines
     */
    public static ParsedMdl parse(String content) {
        if (content == null || content.isEmpty()) {
            return new ParsedMdl(List.of(), List.of());
        }

        // Normalize line endings to LF (Vensim files are typically CRLF on Windows)
        content = content.replace("\r\n", "\n").replace("\r", "\n");

        content = stripHeader(content);

        String equationSection;
        List<String> sketchLines;
        int sketchSep = content.indexOf(SKETCH_SEPARATOR);
        if (sketchSep >= 0) {
            equationSection = content.substring(0, sketchSep);
            String sketchSection = content.substring(sketchSep + SKETCH_SEPARATOR.length());
            sketchLines = splitLines(sketchSection);
        } else {
            equationSection = content;
            sketchLines = List.of();
        }

        List<MacroDef> macros = new ArrayList<>();
        List<MdlEquation> equations = parseEquations(equationSection, macros);
        return new ParsedMdl(equations, macros, sketchLines);
    }

    private static String stripHeader(String content) {
        // Strip UTF-8 BOM
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }
        // Strip {UTF-8} encoding headers — some Vensim files emit multiple
        // (e.g., "{UTF-8}\n{UTF-8}\n"), so strip repeatedly
        boolean changed = true;
        while (changed) {
            changed = false;
            String trimmed = content.stripLeading();
            if (trimmed.startsWith("{UTF-8}")) {
                content = trimmed.substring(7);
                changed = true;
            }
        }
        return content;
    }

    private static List<MdlEquation> parseEquations(String section, List<MacroDef> macros) {
        // Join continuation lines (backslash + newline + optional whitespace → space)
        section = CONTINUATION_PATTERN.matcher(section).replaceAll(" ");

        List<MdlEquation> equations = new ArrayList<>();
        String currentGroup = "";
        boolean inMacro = false;
        String macroName = null;
        List<String> macroParams = null;
        List<MdlEquation> macroBody = null;

        // Split on pipe delimiter
        String[] blocks = section.split("\\|");

        for (String block : blocks) {
            String trimmed = block.strip();
            if (trimmed.isEmpty()) {
                continue;
            }

            // Check for group delimiters (lines of ****)
            if (isGroupDelimiter(trimmed)) {
                currentGroup = extractGroupName(trimmed);
                continue;
            }

            // Split block on tilde to get [equation, units, comment]
            String[] tildeParts = trimmed.split("~", -1);
            String equationPart = tildeParts[0].strip();
            String unitsPart = tildeParts.length > 1 ? tildeParts[1].strip() : "";
            String commentPart = tildeParts.length > 2 ? tildeParts[2].strip() : "";

            if (equationPart.isEmpty()) {
                continue;
            }

            // The :MACRO: header or :END OF MACRO: may share a pipe-block with
            // an equation (the header line followed by the first body equation, or
            // :END OF MACRO: followed by the next regular equation). Split by
            // newlines to detect and separate them.
            String[] eqLines = equationPart.split("\n");
            String macroHeaderLine = null;
            boolean endMacroInBlock = false;
            List<String> bodyLines = new ArrayList<>();

            for (String line : eqLines) {
                String lineStripped = line.strip();
                if (lineStripped.isEmpty()) {
                    continue;
                }
                if (MACRO_START_PATTERN.matcher(lineStripped).find()) {
                    macroHeaderLine = lineStripped;
                } else if (MACRO_END_PATTERN.matcher(lineStripped).find()) {
                    endMacroInBlock = true;
                } else {
                    bodyLines.add(lineStripped);
                }
            }

            // Handle :END OF MACRO: — finalize current macro
            if (endMacroInBlock) {
                if (inMacro && macroName != null && macroParams != null && macroBody != null) {
                    macros.add(buildMacroDef(macroName, macroParams, macroBody));
                }
                inMacro = false;
                macroName = null;
                macroParams = null;
                macroBody = null;
            }

            // Handle :MACRO: header — start new macro
            if (macroHeaderLine != null) {
                inMacro = true;
                macroBody = new ArrayList<>();
                Matcher headerMatcher = MACRO_HEADER_PATTERN.matcher(macroHeaderLine);
                if (headerMatcher.find()) {
                    macroName = headerMatcher.group(1).strip();
                    String paramStr = headerMatcher.group(2).strip();
                    macroParams = new ArrayList<>();
                    if (!paramStr.isEmpty()) {
                        for (String p : paramStr.split(",")) {
                            String param = p.strip();
                            if (!param.isEmpty()) {
                                macroParams.add(param);
                            }
                        }
                    }
                } else {
                    macroName = null;
                    macroParams = null;
                }
            }

            // Process the remaining equation text (if any) from the same block
            String remainingEq = String.join(" ", bodyLines).strip();
            if (remainingEq.isEmpty()) {
                continue;
            }

            MdlEquation equation = parseEquationBlock(remainingEq, unitsPart, commentPart,
                    currentGroup);
            if (equation != null) {
                if (inMacro && macroBody != null) {
                    macroBody.add(equation);
                } else {
                    equations.add(equation);
                }
            }
        }

        return equations;
    }

    /**
     * Builds a {@link MacroDef} by classifying parameters as inputs or outputs.
     * A parameter is an output if it appears as the LHS name of a body equation.
     */
    private static MacroDef buildMacroDef(String name, List<String> allParams,
                                           List<MdlEquation> bodyEquations) {
        // Collect body LHS names (case-insensitive comparison for classification)
        java.util.Set<String> bodyLhsNames = new java.util.HashSet<>();
        for (MdlEquation eq : bodyEquations) {
            bodyLhsNames.add(eq.name().strip().toLowerCase(java.util.Locale.ROOT));
        }

        List<String> inputs = new ArrayList<>();
        List<String> outputs = new ArrayList<>();
        for (String param : allParams) {
            if (bodyLhsNames.contains(param.strip().toLowerCase(java.util.Locale.ROOT))) {
                outputs.add(param);
            } else {
                inputs.add(param);
            }
        }

        return new MacroDef(name, inputs, outputs, bodyEquations);
    }

    private static MdlEquation parseEquationBlock(String equationPart, String units,
                                                    String comment, String group) {
        // Try standard operators: ==, :=, =, :
        Matcher operatorMatcher = OPERATOR_PATTERN.matcher(equationPart);
        if (operatorMatcher.matches()) {
            String name = operatorMatcher.group(1).strip();
            String operator = operatorMatcher.group(2);
            String expression = operatorMatcher.group(3).strip();
            return new MdlEquation(name, operator, expression, units, comment, group);
        }

        // Try lookup definition: name(  — standalone lookup with () operator
        Matcher lookupMatcher = LOOKUP_DEF_PATTERN.matcher(equationPart);
        if (lookupMatcher.matches()) {
            String name = lookupMatcher.group(1).strip();
            return new MdlEquation(name, "()", "", units, comment, group);
        }

        // Check if the whole thing looks like a lookup with inline data
        // Pattern: Name( [(x1,y1),(x2,y2),...] )
        int parenPos = equationPart.indexOf('(');
        if (parenPos > 0) {
            String beforeParen = equationPart.substring(0, parenPos).strip();
            // Make sure what's before the paren looks like a name (no operators)
            if (!beforeParen.isEmpty() && !beforeParen.contains("=")
                    && !beforeParen.contains(":")) {
                String afterParen = equationPart.substring(parenPos + 1).strip();
                // Strip trailing )
                if (afterParen.endsWith(")")) {
                    afterParen = afterParen.substring(0, afterParen.length() - 1).strip();
                }
                return new MdlEquation(beforeParen, "()", afterParen, units, comment, group);
            }
        }

        // Fallback: treat entire block as a name with empty expression (e.g., subscript range)
        return new MdlEquation(equationPart, "", "", units, comment, group);
    }

    private static boolean isGroupDelimiter(String line) {
        // Group sections start and end with lines of ****
        return line.contains(GROUP_DELIMITER);
    }

    private static String extractGroupName(String block) {
        // Group blocks look like:
        // ********************************************************~
        //   .Control
        // ********************************************************|
        // The group name is on a line starting with "."
        String[] lines = block.split("\n");
        for (String line : lines) {
            String trimmedLine = line.strip();
            Matcher m = GROUP_HEADER_PATTERN.matcher(trimmedLine);
            if (m.matches()) {
                return "." + m.group(1).strip();
            }
        }
        return "";
    }

    private static List<String> splitLines(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\n")) {
            String trimmed = line.strip();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        return lines;
    }
}
