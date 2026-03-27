package systems.courant.sd.tools.importer;

import java.util.function.Consumer;

/**
 * Fluent builder for generating Java source code with automatic indentation.
 *
 * <p>Tracks an indent level (each level = 4 spaces) and provides convenience
 * methods for emitting indented lines, blank lines, and block structures.
 */
public class JavaCodeBuilder {

    private final StringBuilder sb;
    private int indentLevel;
    private static final String INDENT_UNIT = "    ";

    public JavaCodeBuilder() {
        this.sb = new StringBuilder(4096);
    }

    /**
     * Appends an indented line followed by a newline.
     */
    public JavaCodeBuilder line(String code) {
        sb.append(currentIndent()).append(code).append('\n');
        return this;
    }

    /**
     * Appends a blank line (no indent).
     */
    public JavaCodeBuilder blankLine() {
        sb.append('\n');
        return this;
    }

    /**
     * Appends raw text without indent or newline.
     */
    public JavaCodeBuilder raw(String text) {
        sb.append(text);
        return this;
    }

    /**
     * Increases the indent level by one (4 spaces).
     */
    public JavaCodeBuilder indent() {
        indentLevel++;
        return this;
    }

    /**
     * Decreases the indent level by one (clamped to 0).
     */
    public JavaCodeBuilder dedent() {
        if (indentLevel > 0) {
            indentLevel--;
        }
        return this;
    }

    /**
     * Emits a block: {@code header + " {\n"} at the current indent, increases
     * indent, runs the body, decreases indent, and emits {@code "}\n"}.
     */
    public JavaCodeBuilder block(String header, Consumer<JavaCodeBuilder> body) {
        line(header + " {");
        indent();
        body.accept(this);
        dedent();
        line("}");
        return this;
    }

    /**
     * Returns the current indent string.
     */
    public String currentIndent() {
        return INDENT_UNIT.repeat(indentLevel);
    }

    /**
     * Returns the indent string at the given number of extra levels beyond current.
     */
    public String indentAt(int extraLevels) {
        return INDENT_UNIT.repeat(indentLevel + extraLevels);
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
