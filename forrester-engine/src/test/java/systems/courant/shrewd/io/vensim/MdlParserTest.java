package systems.courant.shrewd.io.vensim;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MdlParser")
class MdlParserTest {

    @Nested
    @DisplayName("Equation parsing")
    class EquationParsing {

        @Test
        void shouldParseSingleEquation() {
            String content = "x = 42\n\t~\tWidgets\n\t~\tA constant\n\t|";
            MdlParser.ParsedMdl result = MdlParser.parse(content);

            assertThat(result.equations()).hasSize(1);
            MdlEquation eq = result.equations().get(0);
            assertThat(eq.name()).isEqualTo("x");
            assertThat(eq.operator()).isEqualTo("=");
            assertThat(eq.expression()).isEqualTo("42");
            assertThat(eq.units()).isEqualTo("Widgets");
            assertThat(eq.comment()).isEqualTo("A constant");
        }

        @Test
        void shouldParseMultipleEquations() {
            String content = """
                    x = 10
                    \t~\tUnit1
                    \t~\tComment1
                    \t|

                    y = x * 2
                    \t~\tUnit2
                    \t~\tComment2
                    \t|
                    """;
            MdlParser.ParsedMdl result = MdlParser.parse(content);
            assertThat(result.equations()).hasSize(2);
            assertThat(result.equations().get(0).name()).isEqualTo("x");
            assertThat(result.equations().get(1).name()).isEqualTo("y");
        }

        @Test
        void shouldHandleContinuationLines() {
            String content = "Long Variable = a +\\\n\tb + c\n\t~\t\n\t~\t\n\t|";
            MdlParser.ParsedMdl result = MdlParser.parse(content);

            assertThat(result.equations()).hasSize(1);
            assertThat(result.equations().get(0).expression()).contains("a + b + c");
        }

        @Test
        void shouldHandleCrlfLineEndings() {
            String content = "x = 42\r\n\t~\tWidgets\r\n\t~\tA constant\r\n\t|";
            MdlParser.ParsedMdl result = MdlParser.parse(content);

            assertThat(result.equations()).hasSize(1);
            MdlEquation eq = result.equations().get(0);
            assertThat(eq.name()).isEqualTo("x");
            assertThat(eq.expression()).isEqualTo("42");
        }

        @Test
        void shouldHandleCrlfContinuationLines() {
            String content = "Long Variable = a +\\\r\n\tb + c\r\n\t~\t\r\n\t~\t\r\n\t|";
            MdlParser.ParsedMdl result = MdlParser.parse(content);

            assertThat(result.equations()).hasSize(1);
            assertThat(result.equations().get(0).expression()).contains("a + b + c");
        }

        @Test
        void shouldParseUnchangeableConstant() {
            String content = "Pi == 3.14159\n\t~\t\n\t~\t\n\t|";
            MdlParser.ParsedMdl result = MdlParser.parse(content);

            assertThat(result.equations()).hasSize(1);
            assertThat(result.equations().get(0).operator()).isEqualTo("==");
            assertThat(result.equations().get(0).expression()).isEqualTo("3.14159");
        }

        @Test
        void shouldParseDataVariable() {
            String content = "External Data := GET XLS DATA('file.xls', 'Sheet', 'A', 'B2')\n\t~\t\n\t~\t\n\t|";
            MdlParser.ParsedMdl result = MdlParser.parse(content);

            assertThat(result.equations()).hasSize(1);
            assertThat(result.equations().get(0).operator()).isEqualTo(":=");
        }

        @Test
        void shouldParseLookupDefinition() {
            String content = "my_table(\n\t[(0,0)-(10,10)],(0,0),(5,3),(10,10)\n\t)\n\t~\t\n\t~\t\n\t|";
            MdlParser.ParsedMdl result = MdlParser.parse(content);

            assertThat(result.equations()).hasSize(1);
            assertThat(result.equations().get(0).operator()).isEqualTo("()");
        }

        @Test
        void shouldParseINTEGExpression() {
            String content = "Stock = INTEG(inflow - outflow, 100)\n\t~\tItems\n\t~\tA stock\n\t|";
            MdlParser.ParsedMdl result = MdlParser.parse(content);

            assertThat(result.equations()).hasSize(1);
            MdlEquation eq = result.equations().get(0);
            assertThat(eq.name()).isEqualTo("Stock");
            assertThat(eq.operator()).isEqualTo("=");
            assertThat(eq.expression()).startsWith("INTEG(");
        }

        @Test
        void shouldHandleEmptyContent() {
            MdlParser.ParsedMdl result = MdlParser.parse("");
            assertThat(result.equations()).isEmpty();
            assertThat(result.sketchLines()).isEmpty();
        }

        @Test
        void shouldHandleNullContent() {
            MdlParser.ParsedMdl result = MdlParser.parse(null);
            assertThat(result.equations()).isEmpty();
        }

        @Test
        void shouldSkipEmptyBlocks() {
            String content = "x = 5\n\t~\t\n\t~\t\n\t|\n\n\t|";
            MdlParser.ParsedMdl result = MdlParser.parse(content);
            assertThat(result.equations()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Header handling")
    class HeaderHandling {

        @Test
        void shouldStripUtf8Header() {
            String content = "{UTF-8}\nx = 42\n\t~\t\n\t~\t\n\t|";
            MdlParser.ParsedMdl result = MdlParser.parse(content);
            assertThat(result.equations()).hasSize(1);
            assertThat(result.equations().get(0).name()).isEqualTo("x");
        }

        @Test
        void shouldStripBom() {
            String content = "\uFEFFx = 42\n\t~\t\n\t~\t\n\t|";
            MdlParser.ParsedMdl result = MdlParser.parse(content);
            assertThat(result.equations()).hasSize(1);
        }

        @Test
        void shouldStripBomAndUtf8Header() {
            String content = "\uFEFF{UTF-8}\nx = 42\n\t~\t\n\t~\t\n\t|";
            MdlParser.ParsedMdl result = MdlParser.parse(content);
            assertThat(result.equations()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Group handling")
    class GroupHandling {

        @Test
        void shouldExtractGroupFromControlSection() {
            String content = """
                    x = 5
                    \t~\t
                    \t~\t
                    \t|

                    ********************************************************
                    \t.Control
                    ********************************************************~
                    \t\tControl section.
                    \t|

                    INITIAL TIME = 0
                    \t~\tDay
                    \t~\t
                    \t|
                    """;
            MdlParser.ParsedMdl result = MdlParser.parse(content);
            // x is before the group
            assertThat(result.equations().get(0).group()).isEmpty();

            // INITIAL TIME should be in the .Control group
            MdlEquation controlEq = result.equations().stream()
                    .filter(eq -> eq.name().equals("INITIAL TIME"))
                    .findFirst()
                    .orElse(null);
            assertThat(controlEq).isNotNull();
            assertThat(controlEq.group()).isEqualTo(".Control");
        }
    }

    @Nested
    @DisplayName("Sketch separation")
    class SketchSeparation {

        @Test
        void shouldSeparateSketchFromEquations() {
            String content = "x = 5\n\t~\t\n\t~\t\n\t|\n\n\\---///\n*View 1\n10,1,x,100,200";
            MdlParser.ParsedMdl result = MdlParser.parse(content);

            assertThat(result.equations()).hasSize(1);
            assertThat(result.sketchLines()).isNotEmpty();
            assertThat(result.sketchLines().get(0)).isEqualTo("*View 1");
        }

        @Test
        void shouldHandleNoSketchSection() {
            String content = "x = 5\n\t~\t\n\t~\t\n\t|";
            MdlParser.ParsedMdl result = MdlParser.parse(content);
            assertThat(result.equations()).hasSize(1);
            assertThat(result.sketchLines()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Subscript definitions")
    class SubscriptDefinitions {

        @Test
        void shouldParseSubscriptRange() {
            String content = "Region : North, South, East, West\n\t~\t\n\t~\t\n\t|";
            MdlParser.ParsedMdl result = MdlParser.parse(content);

            assertThat(result.equations()).hasSize(1);
            MdlEquation eq = result.equations().get(0);
            assertThat(eq.name()).isEqualTo("Region");
            assertThat(eq.operator()).isEqualTo(":");
            assertThat(eq.expression()).contains("North");
        }
    }
}
