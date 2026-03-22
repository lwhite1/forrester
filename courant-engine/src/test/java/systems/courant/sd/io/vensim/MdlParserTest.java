package systems.courant.sd.io.vensim;

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
        void shouldNotMisclassifyExpressionWithParensAsLookup() {
            String content = "result = a + b(x,y)\n\t~\t\n\t~\t\n\t|";
            MdlParser.ParsedMdl result = MdlParser.parse(content);

            assertThat(result.equations()).hasSize(1);
            MdlEquation eq = result.equations().get(0);
            assertThat(eq.operator()).isEqualTo("=");
            assertThat(eq.expression()).isEqualTo("a + b(x,y)");
        }

        @Test
        void shouldNotMisclassifyMultiplicationWithParensAsLookup() {
            String content = "z = x * func(a)\n\t~\t\n\t~\t\n\t|";
            MdlParser.ParsedMdl result = MdlParser.parse(content);

            assertThat(result.equations()).hasSize(1);
            MdlEquation eq = result.equations().get(0);
            assertThat(eq.operator()).isEqualTo("=");
            assertThat(eq.expression()).isEqualTo("x * func(a)");
        }

        @Test
        void shouldNotMisclassifySubtractionWithParensAsLookup() {
            String content = "w = total - offset(t)\n\t~\t\n\t~\t\n\t|";
            MdlParser.ParsedMdl result = MdlParser.parse(content);

            assertThat(result.equations()).hasSize(1);
            MdlEquation eq = result.equations().get(0);
            assertThat(eq.operator()).isEqualTo("=");
            assertThat(eq.expression()).isEqualTo("total - offset(t)");
        }

        @Test
        void shouldNotMisclassifyDivisionWithParensAsLookup() {
            String content = "r = num / denom(x)\n\t~\t\n\t~\t\n\t|";
            MdlParser.ParsedMdl result = MdlParser.parse(content);

            assertThat(result.equations()).hasSize(1);
            MdlEquation eq = result.equations().get(0);
            assertThat(eq.operator()).isEqualTo("=");
            assertThat(eq.expression()).isEqualTo("num / denom(x)");
        }

        @Test
        void shouldParseInlineLookupViaFallbackPath() {
            // Inline lookup data where the whole equationPart is: name( data )
            String content = "my table( [(0,0)-(10,10)],(0,0),(5,3),(10,10) )\n\t~\t\n\t~\t\n\t|";
            MdlParser.ParsedMdl result = MdlParser.parse(content);

            assertThat(result.equations()).hasSize(1);
            MdlEquation eq = result.equations().get(0);
            assertThat(eq.operator()).isEqualTo("()");
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
            String content = "x = 5\n\t~\t\n\t~\t\n\t|\n\n\\\\\\---///\n*View 1\n10,1,x,100,200";
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

    @Nested
    @DisplayName("Macro parsing")
    class MacroParsing {

        @Test
        void shouldParseMacroDefinition() {
            String content = """
                    :MACRO: SMOOTH CUSTOM(input, delay, output)
                    internal stock = INTEG((input - internal stock) / delay, input)
                    \t~\t
                    \t~\t
                    \t|
                    output = internal stock
                    \t~\t
                    \t~\t
                    \t|
                    :END OF MACRO:
                    x = 5
                    \t~\t
                    \t~\t
                    \t|
                    """;
            MdlParser.ParsedMdl result = MdlParser.parse(content);

            // Macro body equations should NOT be in the main equations list
            assertThat(result.equations()).hasSize(1);
            assertThat(result.equations().get(0).name()).isEqualTo("x");

            // Macro should be parsed
            assertThat(result.macros()).hasSize(1);
            MacroDef macro = result.macros().get(0);
            assertThat(macro.name()).isEqualTo("SMOOTH CUSTOM");
            assertThat(macro.inputParams()).containsExactly("input", "delay");
            assertThat(macro.outputParams()).containsExactly("output");
            assertThat(macro.bodyEquations()).hasSize(2);
        }

        @Test
        void shouldParseMultipleMacros() {
            String content = """
                    :MACRO: MACRO1(a, result)
                    result = a * 2
                    \t~\t
                    \t~\t
                    \t|
                    :END OF MACRO:
                    :MACRO: MACRO2(b, result)
                    result = b + 1
                    \t~\t
                    \t~\t
                    \t|
                    :END OF MACRO:
                    y = 10
                    \t~\t
                    \t~\t
                    \t|
                    """;
            MdlParser.ParsedMdl result = MdlParser.parse(content);

            assertThat(result.equations()).hasSize(1);
            assertThat(result.macros()).hasSize(2);
            assertThat(result.macros().get(0).name()).isEqualTo("MACRO1");
            assertThat(result.macros().get(1).name()).isEqualTo("MACRO2");
        }

        @Test
        void shouldClassifyOutputParameters() {
            String content = """
                    :MACRO: CALC(x, y, out)
                    temp = x + y
                    \t~\t
                    \t~\t
                    \t|
                    out = temp * 2
                    \t~\t
                    \t~\t
                    \t|
                    :END OF MACRO:
                    """;
            MdlParser.ParsedMdl result = MdlParser.parse(content);

            assertThat(result.macros()).hasSize(1);
            MacroDef macro = result.macros().get(0);
            assertThat(macro.inputParams()).containsExactly("x", "y");
            assertThat(macro.outputParams()).containsExactly("out");
        }

        @Test
        void shouldHandleCaseInsensitiveMacroKeywords() {
            String content = """
                    :macro: double(x, result)
                    result = x * 2
                    \t~\t
                    \t~\t
                    \t|
                    :end of macro:
                    """;
            MdlParser.ParsedMdl result = MdlParser.parse(content);

            assertThat(result.macros()).hasSize(1);
            assertThat(result.macros().get(0).name()).isEqualTo("double");
        }

        @Test
        void shouldClassifyEquationBetweenEndAndMacroAsRegular() {
            // When :END OF MACRO: and :MACRO: appear in the same pipe-block with
            // an equation between them, the middle equation should be a regular
            // equation, not part of either macro.
            String content = """
                    :MACRO: FIRST(a, out)
                    out = a * 2
                    \t~\t
                    \t~\t
                    \t|
                    :END OF MACRO:
                    middle = 99
                    :MACRO: SECOND(b, out)
                    \t~\t
                    \t~\t
                    \t|
                    out = b + 1
                    \t~\t
                    \t~\t
                    \t|
                    :END OF MACRO:
                    """;
            MdlParser.ParsedMdl result = MdlParser.parse(content);

            // "middle" should be in main equations, not in either macro
            assertThat(result.equations()).hasSize(1);
            assertThat(result.equations().get(0).name()).isEqualTo("middle");

            // Both macros should be parsed
            assertThat(result.macros()).hasSize(2);
            assertThat(result.macros().get(0).name()).isEqualTo("FIRST");
            assertThat(result.macros().get(0).bodyEquations()).hasSize(1);
            assertThat(result.macros().get(1).name()).isEqualTo("SECOND");
            assertThat(result.macros().get(1).bodyEquations()).hasSize(1);
        }

        @Test
        void shouldHandleNoParsedMacros() {
            String content = "x = 5\n\t~\t\n\t~\t\n\t|";
            MdlParser.ParsedMdl result = MdlParser.parse(content);
            assertThat(result.macros()).isEmpty();
        }

        @Test
        void shouldRecoverEquationsFromUnclosedMacro() {
            String content = """
                    x = 1
                    \t~\t
                    \t~\t
                    \t|
                    :MACRO: UnclosedMacro(a, b)
                    y = a * 2
                    \t~\t
                    \t~\t
                    \t|
                    z = 10
                    \t~\t
                    \t~\t
                    \t|
                    """;
            MdlParser.ParsedMdl result = MdlParser.parse(content);
            // x should be in main equations, and y+z should be recovered
            assertThat(result.equations()).hasSize(3);
            assertThat(result.equations().get(0).name()).isEqualTo("x");
            assertThat(result.equations().get(1).name()).isEqualTo("y");
            assertThat(result.equations().get(2).name()).isEqualTo("z");
        }

        @Test
        void shouldNotLoseEquationsWhenMacroHeaderIsMalformed() {
            String content = """
                    x = 1
                    \t~\t
                    \t~\t
                    \t|
                    :MACRO: InvalidMacroNoParentheses
                    eq1 = 5
                    \t~\t
                    \t~\t
                    \t|
                    :END OF MACRO:
                    y = 2
                    \t~\t
                    \t~\t
                    \t|
                    """;
            MdlParser.ParsedMdl result = MdlParser.parse(content);
            // Malformed macro should not consume equations; eq1 and y should be present
            List<String> names = result.equations().stream()
                    .map(MdlEquation::name)
                    .toList();
            assertThat(names).contains("x", "eq1", "y");
            assertThat(result.macros()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Group delimiter matching (#696)")
    class GroupDelimiterMatching {

        @Test
        void shouldNotMatchEquationContainingFourStars() {
            // An equation like "x = y **** z" should NOT be treated as a group delimiter
            String content = "x = y **** z\n\t~\t\n\t~\t\n\t|";
            MdlParser.ParsedMdl result = MdlParser.parse(content);

            assertThat(result.equations()).hasSize(1);
            assertThat(result.equations().get(0).name()).isEqualTo("x");
        }

        @Test
        void shouldMatchActualGroupDelimiterLines() {
            String content = """
                    ********************************************************
                    \t.MyGroup
                    ********************************************************~
                    \t\tGroup comment.
                    \t|

                    x = 5
                    \t~\t
                    \t~\t
                    \t|
                    """;
            MdlParser.ParsedMdl result = MdlParser.parse(content);

            assertThat(result.equations()).hasSize(1);
            assertThat(result.equations().get(0).group()).isEqualTo(".MyGroup");
        }
    }
}
