package systems.courant.sd.tools.importer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JavaCodeBuilder")
class JavaCodeBuilderTest {

    @Nested
    @DisplayName("line()")
    class Line {

        @Test
        void shouldEmitLineWithNewline() {
            JavaCodeBuilder cb = new JavaCodeBuilder();
            cb.line("int x = 1;");
            assertThat(cb.toString()).isEqualTo("int x = 1;\n");
        }

        @Test
        void shouldIndentLineAtCurrentLevel() {
            JavaCodeBuilder cb = new JavaCodeBuilder();
            cb.indent().indent();
            cb.line("int x = 1;");
            assertThat(cb.toString()).isEqualTo("        int x = 1;\n");
        }
    }

    @Nested
    @DisplayName("blankLine()")
    class BlankLine {

        @Test
        void shouldEmitEmptyNewline() {
            JavaCodeBuilder cb = new JavaCodeBuilder();
            cb.blankLine();
            assertThat(cb.toString()).isEqualTo("\n");
        }

        @Test
        void shouldNotAddIndentToBlankLine() {
            JavaCodeBuilder cb = new JavaCodeBuilder();
            cb.indent().indent();
            cb.blankLine();
            assertThat(cb.toString()).isEqualTo("\n");
        }
    }

    @Nested
    @DisplayName("raw()")
    class Raw {

        @Test
        void shouldAppendWithoutIndentOrNewline() {
            JavaCodeBuilder cb = new JavaCodeBuilder();
            cb.indent();
            cb.raw("hello");
            assertThat(cb.toString()).isEqualTo("hello");
        }

        @Test
        void shouldAppendExactText() {
            JavaCodeBuilder cb = new JavaCodeBuilder();
            cb.raw("first");
            cb.raw(" second");
            assertThat(cb.toString()).isEqualTo("first second");
        }
    }

    @Nested
    @DisplayName("indent/dedent")
    class IndentDedent {

        @Test
        void shouldIncreaseAndDecreaseIndent() {
            JavaCodeBuilder cb = new JavaCodeBuilder();
            cb.indent();
            cb.line("level 1");
            cb.indent();
            cb.line("level 2");
            cb.dedent();
            cb.line("back to 1");
            cb.dedent();
            cb.line("back to 0");
            assertThat(cb.toString()).isEqualTo(
                    "    level 1\n"
                    + "        level 2\n"
                    + "    back to 1\n"
                    + "back to 0\n");
        }

        @Test
        void shouldClampDedentAtZero() {
            JavaCodeBuilder cb = new JavaCodeBuilder();
            cb.dedent();
            cb.line("still at zero");
            assertThat(cb.toString()).isEqualTo("still at zero\n");
        }
    }

    @Nested
    @DisplayName("block()")
    class Block {

        @Test
        void shouldEmitBlockWithBraces() {
            JavaCodeBuilder cb = new JavaCodeBuilder();
            cb.block("public class Foo", inner -> inner.line("int x = 1;"));
            assertThat(cb.toString()).isEqualTo(
                    "public class Foo {\n"
                    + "    int x = 1;\n"
                    + "}\n");
        }

        @Test
        void shouldNestBlocks() {
            JavaCodeBuilder cb = new JavaCodeBuilder();
            cb.block("class Foo", outer ->
                    outer.block("void bar()", inner ->
                            inner.line("return;")));
            assertThat(cb.toString()).isEqualTo(
                    "class Foo {\n"
                    + "    void bar() {\n"
                    + "        return;\n"
                    + "    }\n"
                    + "}\n");
        }

        @Test
        void shouldRestoreIndentAfterBlock() {
            JavaCodeBuilder cb = new JavaCodeBuilder();
            cb.indent();
            cb.block("if (true)", inner -> inner.line("x++;"));
            cb.line("next;");
            assertThat(cb.toString()).isEqualTo(
                    "    if (true) {\n"
                    + "        x++;\n"
                    + "    }\n"
                    + "    next;\n");
        }
    }

    @Nested
    @DisplayName("currentIndent and indentAt")
    class IndentStrings {

        @Test
        void shouldReturnCurrentIndentString() {
            JavaCodeBuilder cb = new JavaCodeBuilder();
            assertThat(cb.currentIndent()).isEmpty();
            cb.indent().indent();
            assertThat(cb.currentIndent()).isEqualTo("        ");
        }

        @Test
        void shouldReturnIndentAtExtraLevels() {
            JavaCodeBuilder cb = new JavaCodeBuilder();
            cb.indent().indent();
            assertThat(cb.indentAt(2)).isEqualTo("                ");
        }

        @Test
        void shouldReturnIndentAtZeroExtraLevels() {
            JavaCodeBuilder cb = new JavaCodeBuilder();
            cb.indent();
            assertThat(cb.indentAt(0)).isEqualTo("    ");
        }
    }

    @Test
    void shouldSupportFluentChaining() {
        JavaCodeBuilder cb = new JavaCodeBuilder();
        String result = cb.line("a").indent().line("b").dedent().line("c").toString();
        assertThat(result).isEqualTo("a\n    b\nc\n");
    }
}
