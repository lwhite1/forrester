package systems.courant.sd.io.vensim;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MacroExpander")
class MacroExpanderTest {

    private static MdlEquation eq(String name, String expression) {
        return new MdlEquation(name, "=", expression, "", "", "");
    }

    private static MacroDef smoothMacro() {
        // :MACRO: SMOOTH CUSTOM(input, delay, output)
        // internal stock = INTEG((input - internal stock) / delay, input)
        // output = internal stock
        // :END OF MACRO:
        return new MacroDef(
                "SMOOTH CUSTOM",
                List.of("input", "delay"),
                List.of("output"),
                List.of(
                        eq("internal stock", "INTEG((input - internal stock) / delay, input)"),
                        eq("output", "internal stock")
                )
        );
    }

    @Nested
    @DisplayName("Single-output macro expansion")
    class SingleOutputExpansion {

        @Test
        void shouldExpandSimpleMacroCall() {
            MacroDef macro = new MacroDef(
                    "DOUBLE",
                    List.of("x"),
                    List.of("result"),
                    List.of(eq("result", "x * 2"))
            );

            List<MdlEquation> equations = List.of(
                    eq("y", "DOUBLE(42)")
            );

            MacroExpander.ExpansionResult result = MacroExpander.expand(equations, List.of(macro));

            assertThat(result.warnings()).isEmpty();
            assertThat(result.expandedEquations()).hasSize(1);
            MdlEquation expanded = result.expandedEquations().get(0);
            assertThat(expanded.name()).isEqualTo("y");
            assertThat(expanded.expression()).isEqualTo("(42) * 2");
        }

        @Test
        void shouldExpandSmoothMacroWithINTEG() {
            MacroDef macro = smoothMacro();

            List<MdlEquation> equations = List.of(
                    eq("smoothed", "SMOOTH CUSTOM(raw data, 5)")
            );

            MacroExpander.ExpansionResult result = MacroExpander.expand(equations, List.of(macro));

            assertThat(result.warnings()).isEmpty();
            // Should have 2 equations: the local stock and the output equation
            assertThat(result.expandedEquations()).hasSize(2);

            // First: the internal stock (prefixed)
            MdlEquation stockEq = result.expandedEquations().get(0);
            assertThat(stockEq.name()).contains("__smooth_custom_1_");
            assertThat(stockEq.expression()).contains("INTEG");
            // Input param should be substituted with parenthesized actual arg
            assertThat(stockEq.expression()).contains("(raw data)");
            // Delay param substituted
            assertThat(stockEq.expression()).contains("(5)");

            // Second: the output equation bound to caller's LHS
            MdlEquation outputEq = result.expandedEquations().get(1);
            assertThat(outputEq.name()).isEqualTo("smoothed");
            assertThat(outputEq.expression()).contains("__smooth_custom_1_");
        }

        @Test
        void shouldPreserveNonMacroEquations() {
            MacroDef macro = new MacroDef(
                    "DOUBLE",
                    List.of("x"),
                    List.of("result"),
                    List.of(eq("result", "x * 2"))
            );

            List<MdlEquation> equations = List.of(
                    eq("a", "10"),
                    eq("b", "DOUBLE(a)"),
                    eq("c", "a + 5")
            );

            MacroExpander.ExpansionResult result = MacroExpander.expand(equations, List.of(macro));

            assertThat(result.warnings()).isEmpty();
            assertThat(result.expandedEquations()).hasSize(3);
            assertThat(result.expandedEquations().get(0).name()).isEqualTo("a");
            assertThat(result.expandedEquations().get(0).expression()).isEqualTo("10");
            assertThat(result.expandedEquations().get(1).name()).isEqualTo("b");
            assertThat(result.expandedEquations().get(2).name()).isEqualTo("c");
            assertThat(result.expandedEquations().get(2).expression()).isEqualTo("a + 5");
        }
    }

    @Nested
    @DisplayName("Multiple instantiations")
    class MultipleInstantiations {

        @Test
        void shouldGenerateUniquePrefixesForEachInstantiation() {
            MacroDef macro = smoothMacro();

            List<MdlEquation> equations = List.of(
                    eq("smooth1", "SMOOTH CUSTOM(input1, 3)"),
                    eq("smooth2", "SMOOTH CUSTOM(input2, 5)")
            );

            MacroExpander.ExpansionResult result = MacroExpander.expand(equations, List.of(macro));

            assertThat(result.warnings()).isEmpty();
            // 2 instantiations × 2 body equations = 4 equations
            assertThat(result.expandedEquations()).hasSize(4);

            // First instantiation uses prefix _1_
            MdlEquation stock1 = result.expandedEquations().get(0);
            assertThat(stock1.name()).contains("_1_");

            // Second instantiation uses prefix _2_
            MdlEquation stock2 = result.expandedEquations().get(2);
            assertThat(stock2.name()).contains("_2_");

            // Prefixes should be different
            assertThat(stock1.name()).isNotEqualTo(stock2.name());
        }
    }

    @Nested
    @DisplayName("Multi-word parameter names")
    class MultiWordParams {

        @Test
        void shouldHandleMultiWordParameterNames() {
            MacroDef macro = new MacroDef(
                    "MY SMOOTH",
                    List.of("input value", "delay time"),
                    List.of("smoothed output"),
                    List.of(
                            eq("internal state", "INTEG((input value - internal state) / delay time, input value)"),
                            eq("smoothed output", "internal state")
                    )
            );

            List<MdlEquation> equations = List.of(
                    eq("result", "MY SMOOTH(raw, 10)")
            );

            MacroExpander.ExpansionResult result = MacroExpander.expand(equations, List.of(macro));

            assertThat(result.warnings()).isEmpty();
            assertThat(result.expandedEquations()).hasSize(2);

            MdlEquation stockEq = result.expandedEquations().get(0);
            assertThat(stockEq.expression()).contains("(raw)");
            assertThat(stockEq.expression()).contains("(10)");
            // Should not contain original parameter names
            assertThat(stockEq.expression()).doesNotContain("input value");
            assertThat(stockEq.expression()).doesNotContain("delay time");
        }
    }

    @Nested
    @DisplayName("Multiple macro calls in one expression")
    class MultipleCalls {

        @Test
        void shouldExpandMultipleMacroCallsInSameExpression() {
            MacroDef macro = new MacroDef(
                    "DOUBLE",
                    List.of("x"),
                    List.of("result"),
                    List.of(eq("result", "x * 2"))
            );

            List<MdlEquation> equations = List.of(
                    eq("y", "DOUBLE(3) + DOUBLE(5)")
            );

            MacroExpander.ExpansionResult result = MacroExpander.expand(equations, List.of(macro));

            assertThat(result.warnings()).isEmpty();
            // The output equation should have both calls expanded
            MdlEquation outputEq = result.expandedEquations().stream()
                    .filter(e -> e.name().equals("y"))
                    .findFirst()
                    .orElseThrow();
            // Neither DOUBLE call should remain
            assertThat(outputEq.expression()).doesNotContainIgnoringCase("DOUBLE");
            // Both expansions should be present
            assertThat(outputEq.expression()).contains("(3) * 2");
            assertThat(outputEq.expression()).contains("(5) * 2");
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        void shouldWarnOnMultiOutputMacro() {
            MacroDef macro = new MacroDef(
                    "MULTI_OUT",
                    List.of("x"),
                    List.of("out1", "out2"),
                    List.of(
                            eq("out1", "x * 2"),
                            eq("out2", "x * 3")
                    )
            );

            List<MdlEquation> equations = List.of(
                    eq("y", "MULTI_OUT(5)")
            );

            MacroExpander.ExpansionResult result = MacroExpander.expand(equations, List.of(macro));

            assertThat(result.warnings()).hasSize(1);
            assertThat(result.warnings().get(0)).contains("output parameters");
            // Original equation preserved
            assertThat(result.expandedEquations()).hasSize(1);
            assertThat(result.expandedEquations().get(0).expression()).isEqualTo("MULTI_OUT(5)");
        }

        @Test
        void shouldWarnOnArgumentCountMismatch() {
            MacroDef macro = new MacroDef(
                    "DOUBLE",
                    List.of("x"),
                    List.of("result"),
                    List.of(eq("result", "x * 2"))
            );

            List<MdlEquation> equations = List.of(
                    eq("y", "DOUBLE(1, 2)")
            );

            MacroExpander.ExpansionResult result = MacroExpander.expand(equations, List.of(macro));

            assertThat(result.warnings()).hasSize(1);
            assertThat(result.warnings().get(0)).contains("expects 1");
            assertThat(result.expandedEquations()).hasSize(1);
        }

        @Test
        void shouldWarnOnNestedMacroCalls() {
            MacroDef outerMacro = new MacroDef(
                    "OUTER",
                    List.of("x"),
                    List.of("result"),
                    List.of(eq("result", "INNER(x)"))
            );
            MacroDef innerMacro = new MacroDef(
                    "INNER",
                    List.of("x"),
                    List.of("result"),
                    List.of(eq("result", "x * 2"))
            );

            List<MdlEquation> equations = List.of(
                    eq("y", "OUTER(5)")
            );

            MacroExpander.ExpansionResult result = MacroExpander.expand(
                    equations, List.of(outerMacro, innerMacro));

            assertThat(result.warnings()).hasSize(1);
            assertThat(result.warnings().get(0)).containsIgnoringCase("nested");
        }

        @Test
        void shouldHandleNoMacros() {
            List<MdlEquation> equations = List.of(eq("x", "42"));
            MacroExpander.ExpansionResult result = MacroExpander.expand(equations, List.of());

            assertThat(result.warnings()).isEmpty();
            assertThat(result.expandedEquations()).hasSize(1);
            assertThat(result.expandedEquations().get(0).expression()).isEqualTo("42");
        }

        @Test
        void shouldHandleNoMatchingCalls() {
            MacroDef macro = new MacroDef(
                    "UNUSED",
                    List.of("x"),
                    List.of("result"),
                    List.of(eq("result", "x"))
            );

            List<MdlEquation> equations = List.of(eq("x", "42"));
            MacroExpander.ExpansionResult result = MacroExpander.expand(equations, List.of(macro));

            assertThat(result.warnings()).isEmpty();
            assertThat(result.expandedEquations()).hasSize(1);
        }
    }
}
