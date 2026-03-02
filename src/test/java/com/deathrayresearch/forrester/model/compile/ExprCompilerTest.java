package com.deathrayresearch.forrester.model.compile;

import com.deathrayresearch.forrester.measure.UnitRegistry;
import com.deathrayresearch.forrester.measure.units.item.ItemUnits;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Formula;
import com.deathrayresearch.forrester.model.LookupTable;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.expr.Expr;
import com.deathrayresearch.forrester.model.expr.ExprParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("ExprCompiler")
class ExprCompilerTest {

    private CompilationContext context;
    private ExprCompiler compiler;
    private List<Resettable> resettables;
    private int[] step;

    @BeforeEach
    void setUp() {
        step = new int[]{0};
        UnitRegistry registry = new UnitRegistry();
        context = new CompilationContext(registry, () -> step[0]);
        resettables = new ArrayList<>();
        compiler = new ExprCompiler(context, resettables);

        // Register some test elements
        context.addStock("Population", new Stock("Population", 1000, ItemUnits.PEOPLE));
        context.addConstant("Rate", new Constant("Rate", ItemUnits.THING, 0.05));
    }

    @Test
    void shouldCompileLiteral() {
        Formula formula = compiler.compile("42");
        assertThat(formula.getCurrentValue()).isEqualTo(42.0);
    }

    @Test
    void shouldCompileRef() {
        Formula formula = compiler.compile("Population");
        assertThat(formula.getCurrentValue()).isEqualTo(1000.0);
    }

    @Test
    void shouldCompileAddition() {
        Formula formula = compiler.compile("Population + 100");
        assertThat(formula.getCurrentValue()).isEqualTo(1100.0);
    }

    @Test
    void shouldCompileMultiplication() {
        Formula formula = compiler.compile("Population * Rate");
        assertThat(formula.getCurrentValue()).isEqualTo(50.0);
    }

    @Test
    void shouldCompileDivision() {
        Formula formula = compiler.compile("Population / 2");
        assertThat(formula.getCurrentValue()).isEqualTo(500.0);
    }

    @Test
    void shouldCompilePower() {
        Formula formula = compiler.compile("2 ^ 10");
        assertThat(formula.getCurrentValue()).isEqualTo(1024.0);
    }

    @Test
    void shouldCompileNegation() {
        Formula formula = compiler.compile("-Rate");
        assertThat(formula.getCurrentValue()).isCloseTo(-0.05, within(1e-10));
    }

    @Test
    void shouldCompileModulo() {
        Formula formula = compiler.compile("7 % 3");
        assertThat(formula.getCurrentValue()).isCloseTo(1.0, within(1e-10));
    }

    @Test
    void shouldCompileLogicalAnd() {
        Formula trueAnd = compiler.compile("1 && 1");
        assertThat(trueAnd.getCurrentValue()).isEqualTo(1.0);

        Formula falseAnd = compiler.compile("1 && 0");
        assertThat(falseAnd.getCurrentValue()).isEqualTo(0.0);
    }

    @Test
    void shouldCompileLogicalOr() {
        Formula trueOr = compiler.compile("0 || 1");
        assertThat(trueOr.getCurrentValue()).isEqualTo(1.0);

        Formula falseOr = compiler.compile("0 || 0");
        assertThat(falseOr.getCurrentValue()).isEqualTo(0.0);
    }

    @Test
    void shouldCompileNot() {
        Formula notTrue = compiler.compile("!1");
        assertThat(notTrue.getCurrentValue()).isEqualTo(0.0);

        Formula notFalse = compiler.compile("!0");
        assertThat(notFalse.getCurrentValue()).isEqualTo(1.0);
    }

    @Test
    void shouldCompileABS() {
        Formula formula = compiler.compile("ABS(-5)");
        assertThat(formula.getCurrentValue()).isEqualTo(5.0);
    }

    @Test
    void shouldCompileSQRT() {
        Formula formula = compiler.compile("SQRT(16)");
        assertThat(formula.getCurrentValue()).isEqualTo(4.0);
    }

    @Test
    void shouldCompileMIN() {
        Formula formula = compiler.compile("MIN(3, 7)");
        assertThat(formula.getCurrentValue()).isEqualTo(3.0);
    }

    @Test
    void shouldCompileMAX() {
        Formula formula = compiler.compile("MAX(3, 7)");
        assertThat(formula.getCurrentValue()).isEqualTo(7.0);
    }

    @Test
    void shouldCompileLN() {
        Formula formula = compiler.compile("LN(1)");
        assertThat(formula.getCurrentValue()).isCloseTo(0.0, within(1e-10));
    }

    @Test
    void shouldCompileEXP() {
        Formula formula = compiler.compile("EXP(0)");
        assertThat(formula.getCurrentValue()).isCloseTo(1.0, within(1e-10));
    }

    @Test
    void shouldCompileSUM() {
        Formula formula = compiler.compile("SUM(1, 2, 3, 4)");
        assertThat(formula.getCurrentValue()).isEqualTo(10.0);
    }

    @Test
    void shouldCompileMEAN() {
        Formula formula = compiler.compile("MEAN(2, 4, 6)");
        assertThat(formula.getCurrentValue()).isCloseTo(4.0, within(1e-10));
    }

    @Test
    void shouldRejectMEANWithZeroArgs() {
        assertThatThrownBy(() -> compiler.compile("MEAN()"))
                .isInstanceOf(CompilationException.class)
                .hasMessageContaining("at least 1 argument");
    }

    @Test
    void shouldCompileDELAY3() {
        Formula formula = compiler.compile("DELAY3(100, 3)");
        // At step 0, DELAY3 initializes with input value
        assertThat(formula.getCurrentValue()).isCloseTo(100.0, within(1e-10));
        assertThat(resettables).hasSize(1);
    }

    @Test
    void shouldCompileTIME() {
        Formula formula = compiler.compile("TIME");
        assertThat(formula.getCurrentValue()).isEqualTo(0.0);
        step[0] = 5;
        assertThat(formula.getCurrentValue()).isEqualTo(5.0);
    }

    @Test
    void shouldCompileConditional() {
        Formula formula = compiler.compile("IF(Population > 500, 1, 0)");
        assertThat(formula.getCurrentValue()).isEqualTo(1.0);
    }

    @Test
    void shouldCompileComparison() {
        Formula formula = compiler.compile("Population == 1000");
        assertThat(formula.getCurrentValue()).isEqualTo(1.0);
    }

    @Test
    void shouldResolveUnderscoreAsSpace() {
        context.addConstant("Birth Rate",
                new Constant("Birth Rate", ItemUnits.THING, 0.03));
        Formula formula = compiler.compile("Birth_Rate");
        assertThat(formula.getCurrentValue()).isCloseTo(0.03, within(1e-10));
    }

    @Test
    void shouldThrowForUnknownRef() {
        assertThatThrownBy(() -> compiler.compile("NonExistent").getCurrentValue())
                .isInstanceOf(CompilationException.class);
    }

    @Test
    void shouldThrowForUnknownFunction() {
        assertThatThrownBy(() -> compiler.compile("UNKNOWN_FUNC(x)"))
                .isInstanceOf(CompilationException.class);
    }

    @Test
    void shouldCompileComplexExpression() {
        context.addConstant("Infectivity",
                new Constant("Infectivity", ItemUnits.THING, 0.10));
        Formula formula = compiler.compile("Population * Rate * Infectivity");
        assertThat(formula.getCurrentValue()).isCloseTo(1000 * 0.05 * 0.10, within(1e-10));
    }

    @Test
    void shouldCompileDTWithDefaultValue() {
        Formula formula = compiler.compile("DT");
        assertThat(formula.getCurrentValue()).isEqualTo(1.0);
    }

    @Test
    void shouldUseEpsilonForEqualityComparison() {
        context.addConstant("A", new Constant("A", ItemUnits.THING, 0.1));
        context.addConstant("B", new Constant("B", ItemUnits.THING, 0.2));
        context.addConstant("C", new Constant("C", ItemUnits.THING, 0.3));
        Formula formula = compiler.compile("(A + B) == C");
        assertThat(formula.getCurrentValue())
                .as("0.1 + 0.2 should equal 0.3 with epsilon comparison")
                .isEqualTo(1.0);
    }

    @Test
    void shouldUseEpsilonForInequalityComparison() {
        context.addConstant("A", new Constant("A", ItemUnits.THING, 0.1));
        context.addConstant("B", new Constant("B", ItemUnits.THING, 0.2));
        context.addConstant("C", new Constant("C", ItemUnits.THING, 0.3));
        Formula formula = compiler.compile("(A + B) != C");
        assertThat(formula.getCurrentValue())
                .as("0.1 + 0.2 should not be != 0.3 with epsilon comparison")
                .isEqualTo(0.0);
    }

    @Test
    void shouldCompileDTWithCustomValue() {
        UnitRegistry registry = new UnitRegistry();
        CompilationContext customContext = new CompilationContext(
                registry, () -> step[0], null, 0.25);
        ExprCompiler customCompiler = new ExprCompiler(customContext, resettables);
        Formula formula = customCompiler.compile("DT");
        assertThat(formula.getCurrentValue()).isEqualTo(0.25);
    }

    @Nested
    @DisplayName("Compilation failure tests")
    class CompilationFailures {

        @Test
        void shouldThrowForWrongArgCount() {
            assertThatThrownBy(() -> compiler.compile("ABS(1, 2)"))
                    .isInstanceOf(CompilationException.class)
                    .hasMessageContaining("1 arguments");
        }

        @Test
        void shouldThrowForMINWithWrongArgs() {
            assertThatThrownBy(() -> compiler.compile("MIN(1)"))
                    .isInstanceOf(CompilationException.class)
                    .hasMessageContaining("2 arguments");
        }

        @Test
        void shouldThrowForSMOOTHWithTooFewArgs() {
            assertThatThrownBy(() -> compiler.compile("SMOOTH(x)"))
                    .isInstanceOf(CompilationException.class)
                    .hasMessageContaining("2-3 arguments");
        }

        @Test
        void shouldThrowForSTEPWithNonConstantTime() {
            assertThatThrownBy(() -> compiler.compile("STEP(10, Population)"))
                    .isInstanceOf(CompilationException.class)
                    .hasMessageContaining("constant");
        }
    }
}
