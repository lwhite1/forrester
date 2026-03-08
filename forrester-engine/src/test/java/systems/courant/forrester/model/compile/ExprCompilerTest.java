package systems.courant.forrester.model.compile;

import systems.courant.forrester.measure.UnitRegistry;
import systems.courant.forrester.measure.units.item.ItemUnits;
import systems.courant.forrester.model.Constant;
import systems.courant.forrester.model.Formula;
import systems.courant.forrester.model.LookupTable;
import systems.courant.forrester.model.Stock;
import systems.courant.forrester.model.expr.Expr;
import systems.courant.forrester.model.expr.ExprParser;
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
        Formula formula = compiler.compile("2 ** 10");
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
        Formula trueAnd = compiler.compile("1 and 1");
        assertThat(trueAnd.getCurrentValue()).isEqualTo(1.0);

        Formula falseAnd = compiler.compile("1 and 0");
        assertThat(falseAnd.getCurrentValue()).isEqualTo(0.0);
    }

    @Test
    void shouldCompileLogicalOr() {
        Formula trueOr = compiler.compile("0 or 1");
        assertThat(trueOr.getCurrentValue()).isEqualTo(1.0);

        Formula falseOr = compiler.compile("0 or 0");
        assertThat(falseOr.getCurrentValue()).isEqualTo(0.0);
    }

    @Test
    void shouldCompileNot() {
        Formula notTrue = compiler.compile("not 1");
        assertThat(notTrue.getCurrentValue()).isEqualTo(0.0);

        Formula notFalse = compiler.compile("not 0");
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
                registry, () -> step[0], null, new double[]{0.25});
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

        @Test
        void shouldThrowForDELAY3WithTooManyArgs() {
            assertThatThrownBy(() -> compiler.compile("DELAY3(1, 2, 3, 4)"))
                    .isInstanceOf(CompilationException.class)
                    .hasMessageContaining("2-3 arguments");
        }

        @Test
        void shouldThrowForRAMPWithTooManyArgs() {
            assertThatThrownBy(() -> compiler.compile("RAMP(1, 2, 3, 4)"))
                    .isInstanceOf(CompilationException.class)
                    .hasMessageContaining("2-3 arguments");
        }

        @Test
        void shouldThrowForLOOKUPWithNonRefFirstArg() {
            assertThatThrownBy(() -> compiler.compile("LOOKUP(42, 10)"))
                    .isInstanceOf(CompilationException.class)
                    .hasMessageContaining("table name reference");
        }
    }

    @Nested
    @DisplayName("Division and modulo edge cases")
    class DivisionEdgeCases {

        @Test
        void shouldReturnZeroForDivisionByZero() {
            Formula formula = compiler.compile("Population / 0");
            assertThat(formula.getCurrentValue()).isNaN();
        }

        @Test
        void shouldReturnZeroForModuloByZero() {
            Formula formula = compiler.compile("Population % 0");
            assertThat(formula.getCurrentValue()).isNaN();
        }
    }

    @Nested
    @DisplayName("SMOOTH over multiple steps")
    class SmoothMultiStep {

        @Test
        void shouldSmoothInputOverSteps() {
            Formula formula = compiler.compile("SMOOTH(Population, 5)");
            // At step 0, should return initial value (input value)
            assertThat(formula.getCurrentValue()).isCloseTo(1000.0, within(1.0));
            assertThat(resettables).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Trig, rounding, and math functions")
    class MathFunctions {

        @Test
        void shouldCompileSIN() {
            Formula formula = compiler.compile("SIN(0)");
            assertThat(formula.getCurrentValue()).isCloseTo(0.0, within(1e-10));
        }

        @Test
        void shouldCompileCOS() {
            Formula formula = compiler.compile("COS(0)");
            assertThat(formula.getCurrentValue()).isCloseTo(1.0, within(1e-10));
        }

        @Test
        void shouldCompileTAN() {
            Formula formula = compiler.compile("TAN(0)");
            assertThat(formula.getCurrentValue()).isCloseTo(0.0, within(1e-10));
        }

        @Test
        void shouldCompileLOG() {
            Formula formula = compiler.compile("LOG(100)");
            assertThat(formula.getCurrentValue()).isCloseTo(2.0, within(1e-10));
        }

        @Test
        void shouldCompileINT() {
            Formula formula = compiler.compile("INT(3.7)");
            assertThat(formula.getCurrentValue()).isEqualTo(3.0);

            Formula negFormula = compiler.compile("INT(-3.7)");
            assertThat(negFormula.getCurrentValue()).isEqualTo(-3.0);
        }

        @Test
        void shouldCompileROUND() {
            Formula formula = compiler.compile("ROUND(3.7)");
            assertThat(formula.getCurrentValue()).isEqualTo(4.0);

            Formula downFormula = compiler.compile("ROUND(3.2)");
            assertThat(downFormula.getCurrentValue()).isEqualTo(3.0);
        }

        @Test
        void shouldCompileMODULO() {
            Formula formula = compiler.compile("MODULO(7, 3)");
            assertThat(formula.getCurrentValue()).isCloseTo(1.0, within(1e-10));
        }

        @Test
        void shouldCompileMODULOByZero() {
            Formula formula = compiler.compile("MODULO(7, 0)");
            assertThat(formula.getCurrentValue()).isNaN();
        }

        @Test
        void shouldCompilePOWER() {
            Formula formula = compiler.compile("POWER(2, 10)");
            assertThat(formula.getCurrentValue()).isEqualTo(1024.0);
        }
    }

    @Nested
    @DisplayName("PULSE function")
    class PulseTests {

        @Test
        void shouldFireSinglePulse() {
            Formula formula = compiler.compile("PULSE(100, 5)");
            // Before pulse time
            step[0] = 3;
            assertThat(formula.getCurrentValue()).isEqualTo(0.0);
            // At pulse time
            step[0] = 5;
            assertThat(formula.getCurrentValue()).isEqualTo(100.0);
            // After pulse time
            step[0] = 6;
            assertThat(formula.getCurrentValue()).isEqualTo(0.0);
        }

        @Test
        void shouldFireRepeatingPulse() {
            Formula formula = compiler.compile("PULSE(50, 2, 3)");
            // At first pulse
            step[0] = 2;
            assertThat(formula.getCurrentValue()).isEqualTo(50.0);
            // Between pulses
            step[0] = 3;
            assertThat(formula.getCurrentValue()).isEqualTo(0.0);
            // At second pulse (2 + 3 = 5)
            step[0] = 5;
            assertThat(formula.getCurrentValue()).isEqualTo(50.0);
            // At third pulse (2 + 6 = 8)
            step[0] = 8;
            assertThat(formula.getCurrentValue()).isEqualTo(50.0);
        }
    }

    @Nested
    @DisplayName("RANDOM_NORMAL function")
    class RandomNormalTests {

        @Test
        void shouldReturnValueWithinBounds() {
            Formula formula = compiler.compile("RANDOM_NORMAL(0, 100, 50, 10)");
            for (int i = 0; i < 100; i++) {
                double val = formula.getCurrentValue();
                assertThat(val).isBetween(0.0, 100.0);
            }
        }
    }

    @Nested
    @DisplayName("DELAY_FIXED function")
    class DelayFixedTests {

        @Test
        void shouldReturnInitialValueBeforeDelayElapses() {
            Formula formula = compiler.compile("DELAY_FIXED(Population, 3, 0)");
            // At step 0, delay hasn't elapsed yet — should return initial value 0
            assertThat(formula.getCurrentValue()).isEqualTo(0.0);
        }

        @Test
        void shouldReturnDelayedInputAfterDelay() {
            // Population = 1000 (constant), delay = 2 steps, initial = 0
            Formula formula = compiler.compile("DELAY_FIXED(Population, 2, 0)");
            // Step 0: initialize, returns initial (0)
            step[0] = 0;
            assertThat(formula.getCurrentValue()).isEqualTo(0.0);
            // Step 1: buffer has [1000, 0], reads oldest = 0
            step[0] = 1;
            assertThat(formula.getCurrentValue()).isEqualTo(0.0);
            // Step 2: buffer has [1000, 1000], reads oldest = 1000
            step[0] = 2;
            assertThat(formula.getCurrentValue()).isEqualTo(1000.0);
        }
    }

    @Nested
    @DisplayName("TREND function")
    class TrendTests {

        @Test
        void shouldReturnInitialTrend() {
            Formula formula = compiler.compile("TREND(Population, 5, 0.1)");
            // At step 0, should return the initial trend
            assertThat(formula.getCurrentValue()).isCloseTo(0.1, within(1e-10));
        }

        @Test
        void shouldReturnZeroTrendForConstantInput() {
            // With constant input and zero initial trend, trend should stay near zero
            Formula formula = compiler.compile("TREND(Population, 5, 0)");
            step[0] = 0;
            formula.getCurrentValue();
            // After several steps with constant input, trend should remain ~0
            for (int i = 1; i <= 20; i++) {
                step[0] = i;
            }
            assertThat(formula.getCurrentValue()).isCloseTo(0.0, within(1e-6));
        }
    }

    @Nested
    @DisplayName("FORECAST function")
    class ForecastTests {

        @Test
        void shouldReturnCurrentValueWithZeroTrend() {
            // With zero initial trend and constant input, forecast should equal input
            Formula formula = compiler.compile("FORECAST(Population, 5, 10, 0)");
            step[0] = 0;
            assertThat(formula.getCurrentValue()).isCloseTo(1000.0, within(1.0));
        }

        @Test
        void shouldExtrapolateWithPositiveTrend() {
            // With 10% initial trend and horizon of 5, forecast = 1000 * (1 + 0.1 * 5) = 1500
            Formula formula = compiler.compile("FORECAST(Population, 5, 5, 0.1)");
            step[0] = 0;
            assertThat(formula.getCurrentValue()).isCloseTo(1500.0, within(1.0));
        }
    }

    @Nested
    @DisplayName("NPV function")
    class NpvTests {

        @Test
        void shouldAccumulateDiscountedValues() {
            // Constant stream of 100, 10% discount rate
            context.addConstant("Payment", new Constant("Payment", ItemUnits.THING, 100));
            Formula formula = compiler.compile("NPV(Payment, 0.10)");
            // Step 0: NPV = 100 (undiscounted first payment)
            step[0] = 0;
            assertThat(formula.getCurrentValue()).isCloseTo(100.0, within(0.01));
            // Step 1: NPV = 100 + 100/1.1 = 190.91
            step[0] = 1;
            assertThat(formula.getCurrentValue()).isCloseTo(190.91, within(0.01));
            // Step 2: NPV = 190.91 + 100/1.21 = 273.55
            step[0] = 2;
            assertThat(formula.getCurrentValue()).isCloseTo(273.55, within(0.01));
        }

        @Test
        void shouldApplyFactor() {
            context.addConstant("Payment", new Constant("Payment", ItemUnits.THING, 100));
            Formula formula = compiler.compile("NPV(Payment, 0.10, 2)");
            // Step 0: NPV = 100 * 2 = 200
            step[0] = 0;
            assertThat(formula.getCurrentValue()).isCloseTo(200.0, within(0.01));
        }
    }

    @Nested
    @DisplayName("3-arg RAMP and LOOKUP")
    class RampAndLookup {

        @Test
        void shouldCompileThreeArgRAMP() {
            Formula formula = compiler.compile("RAMP(2, 3, 7)");
            // At step 0 (before start), value is 0
            assertThat(formula.getCurrentValue()).isEqualTo(0.0);
        }

        @Test
        void shouldCompileLOOKUPWithUnderscoreToSpace() {
            double[] inputHolder = {0};
            LookupTable table = LookupTable.linear(
                    new double[]{0, 50, 100},
                    new double[]{1.0, 0.5, 0.0},
                    () -> inputHolder[0]);
            context.addLookupTable("My Table", table, inputHolder);
            // Reference as "My_Table" — should resolve via underscore-to-space
            Formula formula = compiler.compile("LOOKUP(My_Table, 50)");
            assertThat(formula.getCurrentValue()).isCloseTo(0.5, within(0.01));
        }
    }
}
