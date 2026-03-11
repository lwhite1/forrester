package systems.courant.forrester.model.expr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExprRenamer")
class ExprRenamerTest {

    /** Parse, rename, stringify helper. */
    private static String renameInEquation(String equation, String oldName, String newName) {
        Expr ast = ExprParser.parse(equation);
        Expr renamed = ExprRenamer.rename(ast, oldName, newName);
        return ExprStringifier.stringify(renamed);
    }

    @Nested
    @DisplayName("Simple references")
    class SimpleRefs {

        @Test
        void shouldRenameSimpleRef() {
            assertThat(renameInEquation("Population * birth_rate", "birth_rate", "growth_rate"))
                    .isEqualTo("Population * growth_rate");
        }

        @Test
        void shouldRenameMultipleOccurrences() {
            assertThat(renameInEquation("x + x * x", "x", "y"))
                    .isEqualTo("y + y * y");
        }

        @Test
        void shouldNotRenameNonMatching() {
            Expr ast = ExprParser.parse("Population * birth_rate");
            Expr renamed = ExprRenamer.rename(ast, "death_rate", "mortality");
            assertThat(renamed).isSameAs(ast);
        }

        @Test
        void shouldNotRenamePartialMatch() {
            // "Pop" should not match "Population" — AST refs are exact
            assertThat(renameInEquation("Population * rate", "Pop", "People"))
                    .isEqualTo("Population * rate");
        }
    }

    @Nested
    @DisplayName("Case insensitive matching")
    class CaseInsensitive {

        @Test
        void shouldMatchCaseInsensitively() {
            assertThat(renameInEquation("population * Birth_Rate", "birth_rate", "growth_rate"))
                    .isEqualTo("population * growth_rate");
        }
    }

    @Nested
    @DisplayName("Function calls and lookup tables")
    class FunctionCalls {

        @Test
        void shouldRenameLookupTableName() {
            assertThat(renameInEquation("price_curve(inventory_ratio)", "price_curve", "cost_curve"))
                    .isEqualTo("cost_curve(inventory_ratio)");
        }

        @Test
        void shouldRenameArgumentInsideFunction() {
            assertThat(renameInEquation("SMOOTH(demand, smoothing_time)", "demand", "customer_demand"))
                    .isEqualTo("SMOOTH(customer_demand, smoothing_time)");
        }

        @Test
        void shouldNotRenameBuiltinFunctionName() {
            assertThat(renameInEquation("SMOOTH(x, 5)", "SMOOTH", "AVERAGE"))
                    .isEqualTo("SMOOTH(x, 5)");
        }

        @Test
        void shouldRenameLookupArgumentAndName() {
            assertThat(renameInEquation("my_table(my_input)", "my_table", "new_table"))
                    .isEqualTo("new_table(my_input)");
            assertThat(renameInEquation("my_table(my_input)", "my_input", "new_input"))
                    .isEqualTo("my_table(new_input)");
        }
    }

    @Nested
    @DisplayName("Complex expressions")
    class ComplexExpressions {

        @Test
        void shouldRenameInConditional() {
            assertThat(renameInEquation("IF(stock > 0, stock / delay, 0)", "stock", "inventory"))
                    .isEqualTo("IF(inventory > 0, inventory / delay, 0)");
        }

        @Test
        void shouldRenameInNestedBinaryOps() {
            assertThat(renameInEquation("(a + b) * (c - a)", "a", "alpha"))
                    .isEqualTo("(alpha + b) * (c - alpha)");
        }

        @Test
        void shouldRenameInUnaryOp() {
            assertThat(renameInEquation("-rate * population", "rate", "decay_rate"))
                    .isEqualTo("-decay_rate * population");
        }

        @Test
        void shouldRenameInDelayFunction() {
            assertThat(renameInEquation("DELAY3(order_rate, production_delay)",
                    "order_rate", "purchase_rate"))
                    .isEqualTo("DELAY3(purchase_rate, production_delay)");
        }
    }

    @Nested
    @DisplayName("Numeric literal replacement (element deletion)")
    class NumericLiteralReplacement {

        @Test
        void shouldProduceLiteralWhenReplacingWithZero() {
            assertThat(renameInEquation("S * rate", "rate", "0"))
                    .isEqualTo("S * 0");
        }

        @Test
        void shouldProduceLiteralInComplexExpression() {
            assertThat(renameInEquation("0 * k", "0", "0"))
                    .isEqualTo("0 * k");
            assertThat(renameInEquation("removed + 1", "removed", "0"))
                    .isEqualTo("0 + 1");
        }
    }

    @Nested
    @DisplayName("Identity and structural preservation")
    class StructuralPreservation {

        @Test
        void shouldReturnSameInstanceWhenNoChange() {
            Expr ast = ExprParser.parse("a + b");
            Expr renamed = ExprRenamer.rename(ast, "c", "d");
            assertThat(renamed).isSameAs(ast);
        }

        @Test
        void shouldPreserveLiterals() {
            Expr lit = new Expr.Literal(42);
            assertThat(ExprRenamer.rename(lit, "x", "y")).isSameAs(lit);
        }
    }
}
