package systems.courant.forrester.model.expr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExprDependencies")
class ExprDependenciesTest {

    @Test
    void shouldReturnEmptyForLiteral() {
        Set<String> deps = ExprDependencies.extract(new Expr.Literal(42));
        assertThat(deps).isEmpty();
    }

    @Test
    void shouldReturnSingletonForRef() {
        Set<String> deps = ExprDependencies.extract(new Expr.Ref("Population"));
        assertThat(deps).isEqualTo(Set.of("Population"));
    }

    @Test
    void shouldReturnUnionForBinaryOp() {
        Expr expr = new Expr.BinaryOp(
                new Expr.Ref("a"), BinaryOperator.ADD, new Expr.Ref("b"));
        assertThat(ExprDependencies.extract(expr)).isEqualTo(Set.of("a", "b"));
    }

    @Test
    void shouldDeduplicateRefs() {
        // x * x -> should return {"x"}, not {"x", "x"}
        Expr expr = new Expr.BinaryOp(
                new Expr.Ref("x"), BinaryOperator.MUL, new Expr.Ref("x"));
        Set<String> deps = ExprDependencies.extract(expr);
        assertThat(deps).hasSize(1).contains("x");
    }

    @Test
    void shouldExtractFromUnaryOp() {
        Expr expr = new Expr.UnaryOp(UnaryOperator.NEGATE, new Expr.Ref("x"));
        assertThat(ExprDependencies.extract(expr)).isEqualTo(Set.of("x"));
    }

    @Test
    void shouldExtractFromFunctionArgs() {
        Expr expr = new Expr.FunctionCall("SMOOTH",
                List.of(new Expr.Ref("input"), new Expr.Literal(5)));
        assertThat(ExprDependencies.extract(expr)).isEqualTo(Set.of("input"));
    }

    @Test
    void shouldExtractFromConditional() {
        Expr expr = new Expr.Conditional(
                new Expr.Ref("condition"),
                new Expr.Ref("thenVal"),
                new Expr.Ref("elseVal"));
        assertThat(ExprDependencies.extract(expr))
                .isEqualTo(Set.of("condition", "thenVal", "elseVal"));
    }

    @Test
    void shouldExtractFromComplexNested() {
        // Parse a complex formula and verify all refs found
        Expr expr = ExprParser.parse(
                "Contact_Rate * Infectious / (Susceptible + Infectious + Recovered) * Infectivity * Susceptible");
        Set<String> deps = ExprDependencies.extract(expr);
        assertThat(deps)
                .isEqualTo(Set.of("Contact_Rate", "Infectious", "Susceptible", "Recovered", "Infectivity"));
    }

    @Test
    void shouldExtractFromNestedFunctions() {
        Expr expr = ExprParser.parse("MAX(ABS(x), MIN(y, z))");
        assertThat(ExprDependencies.extract(expr)).isEqualTo(Set.of("x", "y", "z"));
    }

    @Test
    void shouldExtractLookupTableNameFromVensimSyntax() {
        // table(input) syntax — function name IS the lookup table reference
        Expr expr = ExprParser.parse("my_table(input_var)");
        Set<String> deps = ExprDependencies.extract(expr);
        assertThat(deps).contains("my_table", "input_var");
    }

    @Test
    void shouldExtractLookupTableNameFromExplicitSyntax() {
        // LOOKUP(table, input) — table appears as a Ref argument
        Expr expr = ExprParser.parse("LOOKUP(my_table, input_var)");
        Set<String> deps = ExprDependencies.extract(expr);
        assertThat(deps).contains("my_table", "input_var");
    }

    @Test
    void shouldNotAddMultiArgFunctionNamesAsDeps() {
        // Multi-arg built-in functions should NOT have their name added
        Expr expr = ExprParser.parse("MIN(x, y)");
        Set<String> deps = ExprDependencies.extract(expr);
        assertThat(deps).containsExactlyInAnyOrder("x", "y");
        assertThat(deps).doesNotContain("MIN");
    }
}
