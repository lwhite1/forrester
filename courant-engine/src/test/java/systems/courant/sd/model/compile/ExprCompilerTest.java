package systems.courant.sd.model.compile;

import systems.courant.sd.measure.UnitRegistry;
import systems.courant.sd.measure.units.item.ItemUnits;
import systems.courant.sd.model.Formula;
import systems.courant.sd.model.LookupTable;
import systems.courant.sd.model.Stock;
import systems.courant.sd.model.expr.Expr;
import systems.courant.sd.model.expr.ExprParser;
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
    private long[] step;

    @BeforeEach
    void setUp() {
        step = new long[]{0};
        UnitRegistry registry = new UnitRegistry();
        context = new CompilationContext(registry, () -> step[0]);
        resettables = new ArrayList<>();
        compiler = new ExprCompiler(context, resettables);

        // Register some test elements
        context.addStock("Population", new Stock("Population", 1000, ItemUnits.PEOPLE));
        context.addLiteralConstant("Rate", 0.05);
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
    void shouldRejectSumWithNoArguments() {
        assertThatThrownBy(() -> compiler.compile("SUM()"))
                .isInstanceOf(CompilationException.class)
                .hasMessageContaining("at least 1 argument");
    }

    @Test
    void shouldRejectMEANWithZeroArgs() {
        assertThatThrownBy(() -> compiler.compile("MEAN()"))
                .isInstanceOf(CompilationException.class)
                .hasMessageContaining("at least 1 argument");
    }

    @Test
    void shouldCompileDELAY1() {
        Formula formula = compiler.compile("DELAY1(100, 3)");
        // At step 0, DELAY1 initializes with input value
        assertThat(formula.getCurrentValue()).isCloseTo(100.0, within(1e-10));
        assertThat(resettables).hasSize(1);
    }

    @Test
    void shouldCompileDELAY1I() {
        Formula formula = compiler.compile("DELAY1I(100, 3, 50)");
        // At step 0, DELAY1I initializes with explicit initial value
        assertThat(formula.getCurrentValue()).isCloseTo(50.0, within(1e-10));
        assertThat(resettables).hasSize(1);
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
        context.addLiteralConstant("Birth Rate", 0.03);
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
        context.addLiteralConstant("Infectivity", 0.10);
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
        context.addLiteralConstant("A", 0.1);
        context.addLiteralConstant("B", 0.2);
        context.addLiteralConstant("C", 0.3);
        Formula formula = compiler.compile("(A + B) == C");
        assertThat(formula.getCurrentValue())
                .as("0.1 + 0.2 should equal 0.3 with epsilon comparison")
                .isEqualTo(1.0);
    }

    @Test
    void shouldUseEpsilonForInequalityComparison() {
        context.addLiteralConstant("A", 0.1);
        context.addLiteralConstant("B", 0.2);
        context.addLiteralConstant("C", 0.3);
        Formula formula = compiler.compile("(A + B) != C");
        assertThat(formula.getCurrentValue())
                .as("0.1 + 0.2 should not be != 0.3 with epsilon comparison")
                .isEqualTo(0.0);
    }

    @Test
    void shouldDistinguishSmallValuesBeyondEpsilonFloor() {
        // Values that differ by more than the absolute epsilon floor (1e-10)
        // should not compare equal, even when both are small
        context.addLiteralConstant("Small1", 0.0);
        context.addLiteralConstant("Small2", 1e-5);
        Formula eq = compiler.compile("Small1 == Small2");
        assertThat(eq.getCurrentValue())
                .as("0 and 1e-5 differ by more than epsilon floor and should not be equal")
                .isEqualTo(0.0);

        Formula ne = compiler.compile("Small1 != Small2");
        assertThat(ne.getCurrentValue())
                .as("0 and 1e-5 should be not-equal")
                .isEqualTo(1.0);
    }

    @Test
    void shouldUseRelativeEpsilonForLargeValues() {
        // For large values, the epsilon should scale with the magnitude
        context.addLiteralConstant("Big1", 1_000_000.0);
        context.addLiteralConstant("Big2", 1_000_000.0 + 1e-4);
        Formula eq = compiler.compile("Big1 == Big2");
        assertThat(eq.getCurrentValue())
                .as("Values differing by 1e-4 at scale 1e6 should be equal (relative diff ~1e-10)")
                .isEqualTo(1.0);

        // Values differing more significantly should not be equal
        context.addLiteralConstant("Big3", 1_000_000.0);
        context.addLiteralConstant("Big4", 1_000_001.0);
        Formula neq = compiler.compile("Big3 == Big4");
        assertThat(neq.getCurrentValue())
                .as("1_000_000 and 1_000_001 should not be equal")
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
        void shouldWarnForDELAY1WithNegativeDelayTime() {
            compiler.compile("DELAY1(100, -5)");
            assertThat(context.getWarnings())
                    .anyMatch(w -> w.contains("DELAY1") && w.contains("inaccurate"));
        }

        @Test
        void shouldWarnForDELAY1WithZeroDelayTime() {
            compiler.compile("DELAY1(100, 0)");
            assertThat(context.getWarnings())
                    .anyMatch(w -> w.contains("DELAY1") && w.contains("inaccurate"));
        }

        @Test
        void shouldWarnForDELAY3WithNegativeDelayTime() {
            compiler.compile("DELAY3(100, -3)");
            assertThat(context.getWarnings())
                    .anyMatch(w -> w.contains("DELAY3") && w.contains("inaccurate"));
        }

        @Test
        void shouldWarnForDELAY3WithZeroDelayTime() {
            compiler.compile("DELAY3(100, 0)");
            assertThat(context.getWarnings())
                    .anyMatch(w -> w.contains("DELAY3") && w.contains("inaccurate"));
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
    @DisplayName("constant binary expressions (#295)")
    class ConstantBinaryExpressions {

        @Test
        void shouldAcceptDivisionInSmoothTime() {
            context.addLiteralConstant("delay", 10);
            Formula formula = compiler.compile("SMOOTH(Population, delay / 2)");
            assertThat(formula.getCurrentValue()).isCloseTo(1000.0, within(1.0));
        }

        @Test
        void shouldAcceptMultiplicationInSmoothTime() {
            context.addLiteralConstant("base_time", 3);
            Formula formula = compiler.compile("SMOOTH(Population, base_time * 2)");
            assertThat(formula.getCurrentValue()).isCloseTo(1000.0, within(1.0));
        }

        @Test
        void shouldAcceptAdditionInStepTime() {
            context.addLiteralConstant("start", 5);
            Formula formula = compiler.compile("STEP(10, start + 3)");
            // Step hasn't happened yet (step 0 < time 8), so value should be 0
            assertThat(formula.getCurrentValue()).isCloseTo(0.0, within(0.001));
        }

        @Test
        void shouldAcceptNestedBinaryExpression() {
            context.addLiteralConstant("a", 4);
            context.addLiteralConstant("b", 2);
            Formula formula = compiler.compile("SMOOTH(Population, a / b + 1)");
            // smoothing time = 4/2+1 = 3
            assertThat(formula.getCurrentValue()).isCloseTo(1000.0, within(1.0));
        }

        @Test
        void shouldAcceptLiteralBinaryInPulse() {
            Formula formula = compiler.compile("PULSE(100, 10 / 2)");
            // start = 5; at step 0 pulse hasn't fired
            assertThat(formula.getCurrentValue()).isCloseTo(0.0, within(0.001));
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

        @Test
        void shouldAcceptVariableSmoothingTime() {
            // Smoothing time is a variable reference, not a constant (#669)
            context.addLiteralConstant("avg_life", 5);
            Formula formula = compiler.compile("SMOOTH(Population, avg_life)");
            assertThat(formula.getCurrentValue()).isCloseTo(1000.0, within(1.0));
        }

        @Test
        void shouldAcceptVariableSmoothingTimeInSmooth3() {
            context.addLiteralConstant("avg_life", 6);
            Formula formula = compiler.compile("SMOOTH3(Population, avg_life)");
            assertThat(formula.getCurrentValue()).isCloseTo(1000.0, within(1.0));
        }

        @Test
        void shouldWarnWhenSmoothInitialValueIsNaN() {
            compiler.compile("SMOOTH(Population, 5, 0/0)");
            assertThat(context.getWarnings())
                    .anyMatch(w -> w.contains("SMOOTH initialValue") && w.contains("NaN"));
        }

        @Test
        void shouldWarnWhenDelay1InitialValueIsNaN() {
            compiler.compile("DELAY1(Population, 5, 0/0)");
            assertThat(context.getWarnings())
                    .anyMatch(w -> w.contains("DELAY1 initialValue") && w.contains("NaN"));
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
        void shouldCompileARCSIN() {
            Formula formula = compiler.compile("ARCSIN(1)");
            assertThat(formula.getCurrentValue()).isCloseTo(Math.PI / 2, within(1e-10));
        }

        @Test
        void shouldCompileARCSINOfZero() {
            Formula formula = compiler.compile("ARCSIN(0)");
            assertThat(formula.getCurrentValue()).isCloseTo(0.0, within(1e-10));
        }

        @Test
        void shouldReturnNaNForARCSINOutOfRange() {
            Formula formula = compiler.compile("ARCSIN(2)");
            assertThat(formula.getCurrentValue()).isNaN();
        }

        @Test
        void shouldCompileARCCOS() {
            Formula formula = compiler.compile("ARCCOS(0)");
            assertThat(formula.getCurrentValue()).isCloseTo(Math.PI / 2, within(1e-10));
        }

        @Test
        void shouldCompileARCCOSOfOne() {
            Formula formula = compiler.compile("ARCCOS(1)");
            assertThat(formula.getCurrentValue()).isCloseTo(0.0, within(1e-10));
        }

        @Test
        void shouldReturnNaNForARCCOSOutOfRange() {
            Formula formula = compiler.compile("ARCCOS(-2)");
            assertThat(formula.getCurrentValue()).isNaN();
        }

        @Test
        void shouldCompileARCTAN() {
            Formula formula = compiler.compile("ARCTAN(1)");
            assertThat(formula.getCurrentValue()).isCloseTo(Math.PI / 4, within(1e-10));
        }

        @Test
        void shouldCompileARCTANOfZero() {
            Formula formula = compiler.compile("ARCTAN(0)");
            assertThat(formula.getCurrentValue()).isCloseTo(0.0, within(1e-10));
        }

        @Test
        void shouldCompileSIGN() {
            Formula pos = compiler.compile("SIGN(5)");
            assertThat(pos.getCurrentValue()).isEqualTo(1.0);

            Formula neg = compiler.compile("SIGN(-3)");
            assertThat(neg.getCurrentValue()).isEqualTo(-1.0);

            Formula zero = compiler.compile("SIGN(0)");
            assertThat(zero.getCurrentValue()).isEqualTo(0.0);
        }

        @Test
        void shouldCompilePI() {
            Formula formula = compiler.compile("PI");
            assertThat(formula.getCurrentValue()).isCloseTo(Math.PI, within(1e-10));
        }

        @Test
        void shouldUsePIInTrigExpression() {
            Formula formula = compiler.compile("SIN(PI / 2)");
            assertThat(formula.getCurrentValue()).isCloseTo(1.0, within(1e-10));
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
        void shouldUseHalfToEvenRoundingForROUND() {
            // Half-to-even (banker's rounding): 0.5 rounds to nearest even
            assertThat(compiler.compile("ROUND(0.5)").getCurrentValue()).isEqualTo(0.0);
            assertThat(compiler.compile("ROUND(1.5)").getCurrentValue()).isEqualTo(2.0);
            assertThat(compiler.compile("ROUND(2.5)").getCurrentValue()).isEqualTo(2.0);
            assertThat(compiler.compile("ROUND(3.5)").getCurrentValue()).isEqualTo(4.0);
            assertThat(compiler.compile("ROUND(-2.5)").getCurrentValue()).isEqualTo(-2.0);
        }

        @Test
        void shouldNotClampLargeDoublesInROUND() {
            // Values beyond Long.MAX_VALUE must not clamp
            double huge = 1e19;
            Formula formula = compiler.compile("ROUND(" + huge + ")");
            assertThat(formula.getCurrentValue()).isEqualTo(huge);
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
        void shouldCompileQUANTUM() {
            Formula formula = compiler.compile("QUANTUM(7.5, 2)");
            assertThat(formula.getCurrentValue()).isCloseTo(6.0, within(1e-10));
        }

        @Test
        void shouldCompileQUANTUMExactMultiple() {
            Formula formula = compiler.compile("QUANTUM(10, 5)");
            assertThat(formula.getCurrentValue()).isCloseTo(10.0, within(1e-10));
        }

        @Test
        void shouldCompileQUANTUMWithZeroQuantum() {
            Formula formula = compiler.compile("QUANTUM(7.5, 0)");
            assertThat(formula.getCurrentValue()).isCloseTo(7.5, within(1e-10));
        }

        @Test
        void shouldCompileQUANTUMNegativeValue() {
            Formula formula = compiler.compile("QUANTUM(-7.5, 2)");
            assertThat(formula.getCurrentValue()).isCloseTo(-8.0, within(1e-10));
        }

        @Test
        void shouldCompilePOWER() {
            Formula formula = compiler.compile("POWER(2, 10)");
            assertThat(formula.getCurrentValue()).isEqualTo(1024.0);
        }
    }

    @Nested
    @DisplayName("Variadic functions (VMIN, VMAX, PROD)")
    class VariadicFunctions {

        @Test
        void shouldCompileVMIN() {
            Formula formula = compiler.compile("VMIN(5, 2, 8, 1)");
            assertThat(formula.getCurrentValue()).isEqualTo(1.0);
        }

        @Test
        void shouldCompileVMINWithSingleArg() {
            Formula formula = compiler.compile("VMIN(7)");
            assertThat(formula.getCurrentValue()).isEqualTo(7.0);
        }

        @Test
        void shouldRejectVMINWithZeroArgs() {
            assertThatThrownBy(() -> compiler.compile("VMIN()"))
                    .isInstanceOf(CompilationException.class)
                    .hasMessageContaining("at least 1 argument");
        }

        @Test
        void shouldCompileVMAX() {
            Formula formula = compiler.compile("VMAX(5, 2, 8, 1)");
            assertThat(formula.getCurrentValue()).isEqualTo(8.0);
        }

        @Test
        void shouldCompileVMAXWithSingleArg() {
            Formula formula = compiler.compile("VMAX(3)");
            assertThat(formula.getCurrentValue()).isEqualTo(3.0);
        }

        @Test
        void shouldRejectVMAXWithZeroArgs() {
            assertThatThrownBy(() -> compiler.compile("VMAX()"))
                    .isInstanceOf(CompilationException.class)
                    .hasMessageContaining("at least 1 argument");
        }

        @Test
        void shouldCompilePROD() {
            Formula formula = compiler.compile("PROD(2, 3, 4)");
            assertThat(formula.getCurrentValue()).isEqualTo(24.0);
        }

        @Test
        void shouldCompilePRODWithSingleArg() {
            Formula formula = compiler.compile("PROD(5)");
            assertThat(formula.getCurrentValue()).isEqualTo(5.0);
        }

        @Test
        void shouldRejectPRODWithZeroArgs() {
            assertThatThrownBy(() -> compiler.compile("PROD()"))
                    .isInstanceOf(CompilationException.class)
                    .hasMessageContaining("at least 1 argument");
        }
    }

    @Nested
    @DisplayName("Safe division functions (XIDZ, ZIDZ)")
    class SafeDivisionFunctions {

        @Test
        void shouldCompileXIDZNormal() {
            Formula formula = compiler.compile("XIDZ(10, 2, 0)");
            assertThat(formula.getCurrentValue()).isCloseTo(5.0, within(1e-10));
        }

        @Test
        void shouldCompileXIDZDivByZero() {
            Formula formula = compiler.compile("XIDZ(10, 0, -1)");
            assertThat(formula.getCurrentValue()).isCloseTo(-1.0, within(1e-10));
        }

        @Test
        void shouldCompileZIDZNormal() {
            Formula formula = compiler.compile("ZIDZ(10, 2)");
            assertThat(formula.getCurrentValue()).isCloseTo(5.0, within(1e-10));
        }

        @Test
        void shouldCompileZIDZDivByZero() {
            Formula formula = compiler.compile("ZIDZ(10, 0)");
            assertThat(formula.getCurrentValue()).isCloseTo(0.0, within(1e-10));
        }

        @Test
        void shouldRejectXIDZWrongArgCount() {
            assertThatThrownBy(() -> compiler.compile("XIDZ(1, 2)"))
                    .isInstanceOf(CompilationException.class)
                    .hasMessageContaining("3 arguments");
        }

        @Test
        void shouldRejectZIDZWrongArgCount() {
            assertThatThrownBy(() -> compiler.compile("ZIDZ(1)"))
                    .isInstanceOf(CompilationException.class)
                    .hasMessageContaining("2 arguments");
        }
    }

    @Nested
    @DisplayName("INITIAL function")
    class InitialFunction {

        @Test
        void shouldCaptureInitialValue() {
            double[] inputHolder = {100.0};
            context.addVariable("Input",
                    new systems.courant.sd.model.Variable("Input",
                            ItemUnits.PEOPLE, () -> inputHolder[0]));
            Formula formula = compiler.compile("INITIAL(Input)");

            // First evaluation captures the value
            assertThat(formula.getCurrentValue()).isCloseTo(100.0, within(1e-10));

            // Change the input — INITIAL should still return the original value
            inputHolder[0] = 500.0;
            step[0] = 5;
            assertThat(formula.getCurrentValue()).isCloseTo(100.0, within(1e-10));
        }

        @Test
        void shouldBeResettable() {
            double[] inputHolder = {100.0};
            context.addVariable("Input",
                    new systems.courant.sd.model.Variable("Input",
                            ItemUnits.PEOPLE, () -> inputHolder[0]));
            Formula formula = compiler.compile("INITIAL(Input)");

            // First evaluation
            assertThat(formula.getCurrentValue()).isCloseTo(100.0, within(1e-10));
            assertThat(resettables).hasSize(1);

            // Reset and change input
            resettables.get(0).reset();
            inputHolder[0] = 200.0;

            // After reset, should capture new initial value
            assertThat(formula.getCurrentValue()).isCloseTo(200.0, within(1e-10));
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

        @Test
        void shouldAcceptFiveArgVensimForm() {
            Formula formula = compiler.compile("RANDOM_NORMAL(0, 100, 50, 10, 42)");
            for (int i = 0; i < 100; i++) {
                double val = formula.getCurrentValue();
                assertThat(val).isBetween(0.0, 100.0);
            }
        }

        @Test
        void shouldRejectThreeArgs() {
            assertThatThrownBy(() -> compiler.compile("RANDOM_NORMAL(0, 100, 50)"))
                    .isInstanceOf(CompilationException.class)
                    .hasMessageContaining("4-5 arguments");
        }

        @Test
        void shouldRejectSixArgs() {
            assertThatThrownBy(() -> compiler.compile("RANDOM_NORMAL(0, 100, 50, 10, 42, 99)"))
                    .isInstanceOf(CompilationException.class)
                    .hasMessageContaining("4-5 arguments");
        }

        @Test
        void shouldProduceDifferentSequencesForDistinctFormulas() {
            Formula formula1 = compiler.compile("RANDOM_NORMAL(0, 100, 50, 10)");
            Formula formula2 = compiler.compile("RANDOM_NORMAL(0, 100, 50, 10)");

            boolean foundDifference = false;
            for (int i = 0; i < 20; i++) {
                if (formula1.getCurrentValue() != formula2.getCurrentValue()) {
                    foundDifference = true;
                    break;
                }
            }
            assertThat(foundDifference)
                    .as("Two RANDOM_NORMAL formulas compiled separately should produce different sequences")
                    .isTrue();
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

        @Test
        void shouldDefaultToOneStepAndWarnWhenDelayTimeRoundsToZero() {
            // delayTime = 0.3 rounds to 0 — should default to 1 and add a compilation warning
            Formula formula = compiler.compile("DELAY_FIXED(Population, 0.3, 0)");
            step[0] = 0;
            assertThat(formula.getCurrentValue()).isEqualTo(0.0);
            // After 1 step, the delay of 1 should have elapsed
            step[0] = 1;
            assertThat(formula.getCurrentValue()).isEqualTo(1000.0);
            // Verify a compilation warning was recorded
            assertThat(context.getWarnings())
                    .anyMatch(w -> w.contains("DELAY_FIXED") && w.contains("inaccurate"));
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
            context.addLiteralConstant("Payment", 100);
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
            context.addLiteralConstant("Payment", 100);
            Formula formula = compiler.compile("NPV(Payment, 0.10, 2)");
            // Step 0: NPV = 100 * 2 = 200
            step[0] = 0;
            assertThat(formula.getCurrentValue()).isCloseTo(200.0, within(0.01));
        }

        @Test
        void shouldAcceptFourArgVariantWithInitialValue() {
            // NPV(stream, discount_rate, initial_value, factor)
            context.addLiteralConstant("CashFlow", 100);
            Formula formula = compiler.compile("NPV(CashFlow, 0.10, 50, 1)");
            // Step 0: NPV = 50 (initial) + 100 * 1 (first payment) = 150
            step[0] = 0;
            assertThat(formula.getCurrentValue()).isCloseTo(150.0, within(0.01));
            // Step 1: NPV = 150 + 100/1.1 = 240.91
            step[0] = 1;
            assertThat(formula.getCurrentValue()).isCloseTo(240.91, within(0.01));
        }

        @Test
        void shouldHandleFourArgWithZeroInitialValue() {
            // NPV(stream, discount_rate, 0, 1) — common Vensim pattern
            context.addLiteralConstant("CashFlow", 100);
            Formula formula = compiler.compile("NPV(CashFlow, 0.10, 0, 1)");
            // Step 0: NPV = 0 + 100 * 1 = 100
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
        void shouldApplyBothInitialValueAndFactor() {
            // NPV(stream, discount_rate, initial_value, factor)
            context.addLiteralConstant("CashFlow", 100);
            Formula formula = compiler.compile("NPV(CashFlow, 0.10, 50, 2)");
            // Step 0: NPV = 50 (initial) + 100 * 2 = 250
            step[0] = 0;
            assertThat(formula.getCurrentValue()).isCloseTo(250.0, within(0.01));
            // Step 1: NPV = 250 + 100 * 2 / 1.1 = 431.82
            step[0] = 1;
            assertThat(formula.getCurrentValue()).isCloseTo(431.82, within(0.01));
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

        @Test
        void shouldResolveMixedCaseLookupTableWithVensimSyntax() {
            double[] inputHolder = {0};
            LookupTable table = LookupTable.linear(
                    new double[]{0, 50, 100},
                    new double[]{1.0, 0.5, 0.0},
                    () -> inputHolder[0]);
            context.addLookupTable("Effect_of_Crowding", table, inputHolder);
            // Vensim syntax: table(input) — should preserve original case
            Formula formula = compiler.compile("Effect_of_Crowding(50)");
            assertThat(formula.getCurrentValue()).isCloseTo(0.5, within(0.01));
        }
    }

    @Nested
    @DisplayName("Conditional branch evaluation")
    class ConditionalBranch {

        @Test
        void shouldEvaluateBothBranchesForStatefulFunctions() {
            // SMOOTH in the else branch should stay current even when condition is true
            double[] inputHolder = {10.0};
            context.addVariable("Input",
                    new systems.courant.sd.model.Variable("Input",
                            ItemUnits.PEOPLE, () -> inputHolder[0]));
            double[] switchHolder = {1.0};
            context.addVariable("Switch",
                    new systems.courant.sd.model.Variable("Switch",
                            ItemUnits.PEOPLE, () -> switchHolder[0]));

            // IF(Switch, 0, SMOOTH(Input, 3))
            Formula formula = compiler.compile("IF(Switch, 0, SMOOTH(Input, 3))");

            // Run several steps with Switch=1 (then-branch active, else-branch has SMOOTH)
            for (int i = 0; i < 10; i++) {
                step[0] = i;
                formula.getCurrentValue();
                inputHolder[0] = 50.0; // change input while SMOOTH is in untaken branch
            }

            // Now switch to the else-branch: SMOOTH should have tracked the input
            switchHolder[0] = 0.0;
            step[0] = 10;
            double smoothed = formula.getCurrentValue();

            // If SMOOTH was evaluated each step, it should have moved toward 50.
            // If it went stale, it would still be near 10.
            assertThat(smoothed).as("SMOOTH should have tracked input while in untaken branch")
                    .isGreaterThan(30.0);
        }
    }

    @Nested
    @DisplayName("Non-constant initial values (#514)")
    class NonConstantInitialValues {

        @Test
        void shouldAcceptVariableReferenceAsSmoothIInitial() {
            context.addVariable("normal_price",
                    new systems.courant.sd.model.Variable("normal_price",
                            ItemUnits.PEOPLE, () -> 50.0));
            context.addVariable("input",
                    new systems.courant.sd.model.Variable("input",
                            ItemUnits.PEOPLE, () -> 100.0));

            Formula formula = compiler.compile("SMOOTHI(input, 5, normal_price)");
            step[0] = 0;
            double val = formula.getCurrentValue();
            // Initial value should be 50 (from normal_price), not 0 or error
            assertThat(val).isCloseTo(50.0, within(0.01));
        }

        @Test
        void shouldAcceptExpressionAsSmooth3IInitial() {
            context.addLiteralConstant("base_val", 20.0);
            context.addVariable("input",
                    new systems.courant.sd.model.Variable("input",
                            ItemUnits.PEOPLE, () -> 100.0));

            Formula formula = compiler.compile("SMOOTH3I(input, 5, base_val + 10)");
            step[0] = 0;
            double val = formula.getCurrentValue();
            // Initial value should be 30 (20 + 10)
            assertThat(val).isCloseTo(30.0, within(0.01));
        }

        @Test
        void shouldAcceptVariableReferenceAsSmooth3Initial() {
            context.addVariable("init_val",
                    new systems.courant.sd.model.Variable("init_val",
                            ItemUnits.PEOPLE, () -> 75.0));
            context.addVariable("input",
                    new systems.courant.sd.model.Variable("input",
                            ItemUnits.PEOPLE, () -> 100.0));

            Formula formula = compiler.compile("SMOOTH3(input, 5, init_val)");
            step[0] = 0;
            double val = formula.getCurrentValue();
            // Initial value should be 75 (from init_val)
            assertThat(val).isCloseTo(75.0, within(0.01));
        }

        @Test
        void shouldWarnWhenInitialValueIsNotCompileTimeConstant() {
            context.addVariable("normal_price",
                    new systems.courant.sd.model.Variable("normal_price",
                            ItemUnits.PEOPLE, () -> 50.0));
            context.addVariable("input",
                    new systems.courant.sd.model.Variable("input",
                            ItemUnits.PEOPLE, () -> 100.0));

            compiler.compile("SMOOTHI(input, 5, normal_price)");
            assertThat(context.getWarnings())
                    .anyMatch(w -> w.contains("SMOOTHI initialValue")
                            && w.contains("not a compile-time constant")
                            && w.contains("uninitialized variables"));
        }
    }

    @Nested
    @DisplayName("Logical function forms (NOT, OR, AND, TRUE, FALSE)")
    class LogicalFunctionForms {

        @Test
        void shouldCompileNotFunctionWithZero() {
            Formula formula = compiler.compile("NOT(0)");
            assertThat(formula.getCurrentValue()).isEqualTo(1.0);
        }

        @Test
        void shouldCompileNotFunctionWithNonZero() {
            Formula formula = compiler.compile("NOT(5)");
            assertThat(formula.getCurrentValue()).isEqualTo(0.0);
        }

        @Test
        void shouldCompileOrFunctionBothFalse() {
            Formula formula = compiler.compile("OR(0, 0)");
            assertThat(formula.getCurrentValue()).isEqualTo(0.0);
        }

        @Test
        void shouldCompileOrFunctionOneTrueOneFalse() {
            Formula formula = compiler.compile("OR(0, 1)");
            assertThat(formula.getCurrentValue()).isEqualTo(1.0);
        }

        @Test
        void shouldCompileAndFunctionBothTrue() {
            Formula formula = compiler.compile("AND(1, 1)");
            assertThat(formula.getCurrentValue()).isEqualTo(1.0);
        }

        @Test
        void shouldCompileAndFunctionOneFalse() {
            Formula formula = compiler.compile("AND(1, 0)");
            assertThat(formula.getCurrentValue()).isEqualTo(0.0);
        }

        @Test
        void shouldCompileTrueFunction() {
            Formula formula = compiler.compile("TRUE()");
            assertThat(formula.getCurrentValue()).isEqualTo(1.0);
        }

        @Test
        void shouldCompileFalseFunction() {
            Formula formula = compiler.compile("FALSE()");
            assertThat(formula.getCurrentValue()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("LOOKUP_AREA")
    class LookupAreaCompilation {

        @Test
        void shouldComputeAreaUnderConstantLookup() {
            // Lookup: y = 2.0 for all x (flat line from 0 to 10)
            // Area from 0 to 10 = 2.0 * 10 = 20.0
            var def = new systems.courant.sd.model.def.LookupTableDef(
                    "flat_table",
                    new double[]{0.0, 10.0},
                    new double[]{2.0, 2.0},
                    "LINEAR");
            context.addLookupTableDef("flat_table", def);
            context.addLookupTable("flat_table",
                    LookupTable.linear(def.xValues(), def.yValues(), () -> 0),
                    new double[1]);

            Formula formula = compiler.compile("LOOKUP_AREA(flat_table, 0, 10)");
            assertThat(formula.getCurrentValue()).isCloseTo(20.0, within(0.001));
        }

        @Test
        void shouldComputeAreaUnderLinearRamp() {
            // Lookup: y = x (linear from 0,0 to 10,10)
            // Area from 0 to 10 = 0.5 * 10 * 10 = 50.0 (triangle)
            var def = new systems.courant.sd.model.def.LookupTableDef(
                    "ramp_table",
                    new double[]{0.0, 10.0},
                    new double[]{0.0, 10.0},
                    "LINEAR");
            context.addLookupTableDef("ramp_table", def);
            context.addLookupTable("ramp_table",
                    LookupTable.linear(def.xValues(), def.yValues(), () -> 0),
                    new double[1]);

            Formula formula = compiler.compile("LOOKUP_AREA(ramp_table, 0, 10)");
            assertThat(formula.getCurrentValue()).isCloseTo(50.0, within(0.001));
        }

        @Test
        void shouldReturnNegativeAreaWhenReversed() {
            var def = new systems.courant.sd.model.def.LookupTableDef(
                    "flat2",
                    new double[]{0.0, 10.0},
                    new double[]{2.0, 2.0},
                    "LINEAR");
            context.addLookupTableDef("flat2", def);
            context.addLookupTable("flat2",
                    LookupTable.linear(def.xValues(), def.yValues(), () -> 0),
                    new double[1]);

            Formula formula = compiler.compile("LOOKUP_AREA(flat2, 10, 0)");
            assertThat(formula.getCurrentValue()).isCloseTo(-20.0, within(0.001));
        }

        @Test
        void shouldComputePartialArea() {
            // Lookup: y = x from 0 to 10. Area from 2 to 6 = trapezoid: (2+6)/2 * 4 = 16
            var def = new systems.courant.sd.model.def.LookupTableDef(
                    "ramp2",
                    new double[]{0.0, 10.0},
                    new double[]{0.0, 10.0},
                    "LINEAR");
            context.addLookupTableDef("ramp2", def);
            context.addLookupTable("ramp2",
                    LookupTable.linear(def.xValues(), def.yValues(), () -> 0),
                    new double[1]);

            Formula formula = compiler.compile("LOOKUP_AREA(ramp2, 2, 6)");
            assertThat(formula.getCurrentValue()).isCloseTo(16.0, within(0.001));
        }
    }

    @Nested
    @DisplayName("IF_SHORT short-circuit conditional (#504)")
    class IfShortCircuit {

        @Test
        void shouldReturnThenBranchWhenConditionIsTrue() {
            context.addLiteralConstant("x", 1.0);
            Formula formula = compiler.compile("IF_SHORT(x, 10, 20)");
            assertThat(formula.getCurrentValue()).isEqualTo(10.0);
        }

        @Test
        void shouldReturnElseBranchWhenConditionIsFalse() {
            context.addLiteralConstant("x", 0.0);
            Formula formula = compiler.compile("IF_SHORT(x, 10, 20)");
            assertThat(formula.getCurrentValue()).isEqualTo(20.0);
        }

        @Test
        void shouldNotEvaluateElseBranchWhenConditionIsTrue() {
            context.addLiteralConstant("cond", 1.0);
            int[] elseCount = {0};
            context.addVariable("side_effect",
                    new systems.courant.sd.model.Variable("side_effect",
                            ItemUnits.THING, () -> {
                        elseCount[0]++;
                        return 99.0;
                    }));
            Formula formula = compiler.compile("IF_SHORT(cond, 10, side_effect)");
            formula.getCurrentValue();
            assertThat(elseCount[0]).isZero();
        }

        @Test
        void shouldNotEvaluateThenBranchWhenConditionIsFalse() {
            context.addLiteralConstant("cond", 0.0);
            int[] thenCount = {0};
            context.addVariable("side_effect",
                    new systems.courant.sd.model.Variable("side_effect",
                            ItemUnits.THING, () -> {
                        thenCount[0]++;
                        return 99.0;
                    }));
            Formula formula = compiler.compile("IF_SHORT(cond, side_effect, 20)");
            formula.getCurrentValue();
            assertThat(thenCount[0]).isZero();
        }

        @Test
        void shouldParseAndStringifyIfShort() {
            Expr ast = ExprParser.parse("IF_SHORT(x > 0, x, 0)");
            assertThat(ast).isInstanceOf(Expr.Conditional.class);
            Expr.Conditional cond = (Expr.Conditional) ast;
            assertThat(cond.shortCircuit()).isTrue();
            assertThat(systems.courant.sd.model.expr.ExprStringifier.stringify(ast))
                    .isEqualTo("IF_SHORT(x > 0, x, 0)");
        }

        @Test
        void shouldEvaluateConditionBeforeBranchesInNonShortCircuit() {
            // The condition depends on a value. Both branches have side effects
            // that could change the value. Condition must be evaluated first.
            double[] counter = {0};
            context.addVariable("counter",
                    new systems.courant.sd.model.Variable("counter",
                            ItemUnits.THING, () -> counter[0]));
            context.addVariable("inc",
                    new systems.courant.sd.model.Variable("inc",
                            ItemUnits.THING, () -> { counter[0]++; return 1.0; }));

            // counter starts at 0, so condition (counter > 0) is false.
            // Both branches evaluate 'inc' which increments counter.
            // Without the fix, branches evaluate first, making counter > 0 true.
            Formula formula = compiler.compile("IF(counter > 0, inc, inc + 10)");
            double result = formula.getCurrentValue();
            // Condition evaluated first (counter=0, false), then branches run.
            // Should return else branch value: inc + 10 = 1 + 10 = 11
            assertThat(result).isEqualTo(11.0);
        }

        @Test
        void shouldDistinguishIfFromIfShort() {
            Expr ifExpr = ExprParser.parse("IF(x > 0, x, 0)");
            Expr ifShortExpr = ExprParser.parse("IF_SHORT(x > 0, x, 0)");
            assertThat(((Expr.Conditional) ifExpr).shortCircuit()).isFalse();
            assertThat(((Expr.Conditional) ifShortExpr).shortCircuit()).isTrue();
        }
    }

    @Nested
    @DisplayName("Warned flag reset")
    class WarnedFlagReset {

        @Test
        @DisplayName("should register resettables for warned flags in division by zero")
        void shouldResetWarnedFlagsOnReset() {
            context.addLiteralConstant("zero", 0);
            Formula formula = compiler.compile("Population / zero");

            // First evaluation triggers warning and returns NaN
            assertThat(formula.getCurrentValue()).isNaN();

            // Resettables should have been registered
            assertThat(resettables).isNotEmpty();

            // Reset all resettables (simulating model reset between runs)
            resettables.forEach(Resettable::reset);

            // After reset, the warned flag should be cleared so warning can fire again
            // The formula should still return NaN (the behavior is the same,
            // but the warning log would fire again on a real run)
            assertThat(formula.getCurrentValue()).isNaN();
        }
    }

    @Nested
    @DisplayName("FIND_ZERO scoping")
    class FindZeroScoping {

        @Test
        @DisplayName("should not shadow model variable after FIND_ZERO compilation")
        void shouldNotShadowVariableAfterFindZero() {
            // Register x as a literal constant with value 5
            context.addLiteralConstant("x", 5);

            // Compile FIND_ZERO that uses x as the loop variable
            // FIND_ZERO(x - 3, x, 0, 10) should find x = 3
            Formula findZeroFormula = compiler.compile("FIND_ZERO(x - 3, x, 0, 10)");
            assertThat(findZeroFormula.getCurrentValue()).isCloseTo(3.0, within(1e-6));

            // After FIND_ZERO, compiling a new expression referencing x should resolve
            // to the original model variable (value 5), not the FIND_ZERO holder
            Formula afterFormula = compiler.compile("x");
            assertThat(afterFormula.getCurrentValue()).isEqualTo(5.0);
        }
    }

    @Nested
    @DisplayName("Fractional DT (#858)")
    class FractionalDt {

        private CompilationContext fracCtx;
        private ExprCompiler fracCompiler;
        private long[] fracStep;

        @BeforeEach
        void setUp() {
            fracStep = new long[]{0};
            UnitRegistry registry = new UnitRegistry();
            double[] dtHolder = {0.25};
            fracCtx = new CompilationContext(registry, () -> fracStep[0], null, dtHolder);
            fracCompiler = new ExprCompiler(fracCtx, new ArrayList<>());
            fracCtx.addLiteralConstant("Rate", 0.05);
        }

        @Test
        void shouldReturnSimulationTimeNotStepIndex() {
            Formula formula = fracCompiler.compile("TIME");
            fracStep[0] = 0;
            assertThat(formula.getCurrentValue()).isEqualTo(0.0);
            fracStep[0] = 4;  // 4 steps * 0.25 = time 1.0
            assertThat(formula.getCurrentValue()).isEqualTo(1.0);
            fracStep[0] = 40; // 40 steps * 0.25 = time 10.0
            assertThat(formula.getCurrentValue()).isEqualTo(10.0);
        }

        @Test
        void shouldFireStepAtCorrectTime() {
            Formula formula = fracCompiler.compile("STEP(10, 5)");
            // time 5 = step 20 (5 / 0.25)
            fracStep[0] = 19;
            assertThat(formula.getCurrentValue()).isEqualTo(0.0);
            fracStep[0] = 20;
            assertThat(formula.getCurrentValue()).isEqualTo(10.0);
        }

        @Test
        void shouldFirePulseAtCorrectTime() {
            Formula formula = fracCompiler.compile("PULSE(100, 3)");
            // time 3 = step 12 (3 / 0.25)
            fracStep[0] = 11;
            assertThat(formula.getCurrentValue()).isEqualTo(0.0);
            fracStep[0] = 12;
            assertThat(formula.getCurrentValue()).isEqualTo(100.0);
            fracStep[0] = 13;
            assertThat(formula.getCurrentValue()).isEqualTo(0.0);
        }

        @Test
        void shouldRampAtCorrectRate() {
            Formula formula = fracCompiler.compile("RAMP(2, 4)");
            // time 4 = step 16, slope=2 per time unit, so 2 * dt = 0.5 per step
            fracStep[0] = 15;
            assertThat(formula.getCurrentValue()).isEqualTo(0.0);
            fracStep[0] = 16; // time 4, ramp starts
            assertThat(formula.getCurrentValue()).isEqualTo(0.0);
            fracStep[0] = 20; // time 5, elapsed = 4 steps, value = 0.5 * 4 = 2.0
            assertThat(formula.getCurrentValue()).isCloseTo(2.0, within(1e-10));
        }

        @Test
        void shouldComputePulseTrainWithCorrectTiming() {
            fracCtx.addLiteralConstant("start", 2);
            fracCtx.addLiteralConstant("dur", 1);
            fracCtx.addLiteralConstant("repeat", 4);
            fracCtx.addLiteralConstant("fin", 20);
            Formula formula = fracCompiler.compile("PULSE_TRAIN(start, dur, repeat, fin)");
            // At time 2 (step 8): inside pulse (2 <= 2 < 3)
            fracStep[0] = 8;
            assertThat(formula.getCurrentValue()).isEqualTo(1.0);
            // At time 3.5 (step 14): outside pulse
            fracStep[0] = 14;
            assertThat(formula.getCurrentValue()).isEqualTo(0.0);
            // At time 6 (step 24): second pulse (6 <= 6 < 7)
            fracStep[0] = 24;
            assertThat(formula.getCurrentValue()).isEqualTo(1.0);
        }

        @Test
        void shouldFirePulseTrainAtExactRepeatBoundary() {
            // DT=0.25, repeat=4: at time 6 (step 24), elapsed=4 which is exactly repeat.
            // FP modulo can give 4.0 % 4.0 ≈ tiny positive instead of 0.
            fracCtx.addLiteralConstant("start", 2);
            fracCtx.addLiteralConstant("dur", 1);
            fracCtx.addLiteralConstant("repeat", 4);
            fracCtx.addLiteralConstant("fin", 20);
            Formula formula = fracCompiler.compile("PULSE_TRAIN(start, dur, repeat, fin)");
            // At time 10 (step 40): elapsed=8, 8%4 should be 0 → inside pulse
            fracStep[0] = 40;
            assertThat(formula.getCurrentValue()).isEqualTo(1.0);
            // At time 14 (step 56): elapsed=12, 12%4 should be 0 → inside pulse
            fracStep[0] = 56;
            assertThat(formula.getCurrentValue()).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("DT consistency across time-dependent functions (#1062)")
    class DtConsistency {

        /**
         * Verifies that all time-dependent functions produce the same simulation-time
         * behavior regardless of DT. Each function is compiled at DT=1.0 and DT=0.25,
         * and both must agree on the value at a given simulation time.
         */

        private Formula compileWithDt(double dt, String expr, long[] stepHolder,
                                      double[] dtHolder) {
            dtHolder[0] = dt;
            UnitRegistry registry = new UnitRegistry();
            CompilationContext ctx = new CompilationContext(
                    registry, () -> stepHolder[0], null, dtHolder);
            ctx.addLiteralConstant("Population", 1000);
            ctx.addLiteralConstant("Input", 100);
            ExprCompiler comp = new ExprCompiler(ctx, new ArrayList<>());
            return comp.compile(expr);
        }

        @Test
        @DisplayName("STEP fires at same simulation time for DT=1.0 and DT=0.25")
        void stepFiresAtSameSimulationTime() {
            long[] step1 = {0};
            double[] dt1 = {1.0};
            Formula f1 = compileWithDt(1.0, "STEP(10, 5)", step1, dt1);

            long[] step025 = {0};
            double[] dt025 = {0.25};
            Formula f025 = compileWithDt(0.25, "STEP(10, 5)", step025, dt025);

            // Before time 5: both return 0
            step1[0] = 4;       // time 4.0
            step025[0] = 16;    // time 4.0
            assertThat(f1.getCurrentValue()).isEqualTo(0.0);
            assertThat(f025.getCurrentValue()).isEqualTo(0.0);

            // At time 5: both return 10
            step1[0] = 5;       // time 5.0
            step025[0] = 20;    // time 5.0
            assertThat(f1.getCurrentValue()).isEqualTo(10.0);
            assertThat(f025.getCurrentValue()).isEqualTo(10.0);
        }

        @Test
        @DisplayName("RAMP produces same value at same simulation time for DT=1.0 and DT=0.25")
        void rampProducesSameValueAtSameTime() {
            long[] step1 = {0};
            double[] dt1 = {1.0};
            Formula f1 = compileWithDt(1.0, "RAMP(2, 4, 10)", step1, dt1);

            long[] step025 = {0};
            double[] dt025 = {0.25};
            Formula f025 = compileWithDt(0.25, "RAMP(2, 4, 10)", step025, dt025);

            // Before start: both return 0
            step1[0] = 3;       // time 3
            step025[0] = 12;    // time 3
            assertThat(f1.getCurrentValue()).isEqualTo(0.0);
            assertThat(f025.getCurrentValue()).isEqualTo(0.0);

            // At time 7: elapsed = 3, value = 2 * 3 = 6
            step1[0] = 7;
            step025[0] = 28;
            assertThat(f1.getCurrentValue()).isCloseTo(6.0, within(1e-10));
            assertThat(f025.getCurrentValue()).isCloseTo(6.0, within(1e-10));

            // After end (time 12): clamped at elapsed = 6, value = 2 * 6 = 12
            step1[0] = 12;
            step025[0] = 48;
            assertThat(f1.getCurrentValue()).isCloseTo(12.0, within(1e-10));
            assertThat(f025.getCurrentValue()).isCloseTo(12.0, within(1e-10));
        }

        @Test
        @DisplayName("PULSE fires at same simulation time for DT=1.0 and DT=0.25")
        void pulseFiresAtSameSimulationTime() {
            long[] step1 = {0};
            double[] dt1 = {1.0};
            Formula f1 = compileWithDt(1.0, "PULSE(100, 3, 5)", step1, dt1);

            long[] step025 = {0};
            double[] dt025 = {0.25};
            Formula f025 = compileWithDt(0.25, "PULSE(100, 3, 5)", step025, dt025);

            // At time 3: both fire
            step1[0] = 3;
            step025[0] = 12;
            assertThat(f1.getCurrentValue()).isEqualTo(100.0);
            assertThat(f025.getCurrentValue()).isEqualTo(100.0);

            // At time 4: neither fires
            step1[0] = 4;
            step025[0] = 16;
            assertThat(f1.getCurrentValue()).isEqualTo(0.0);
            assertThat(f025.getCurrentValue()).isEqualTo(0.0);

            // At time 8: both fire (second pulse: 3 + 5 = 8)
            step1[0] = 8;
            step025[0] = 32;
            assertThat(f1.getCurrentValue()).isEqualTo(100.0);
            assertThat(f025.getCurrentValue()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("DELAY_FIXED divides delay_time by DT (#1031)")
        void delayFixedDividesByDt() {
            long[] step025 = {0};
            double[] dt025 = {0.25};
            Formula f025 = compileWithDt(0.25, "DELAY_FIXED(Input, 2, 0)", step025, dt025);

            // delay = 2 time units = 8 steps at DT=0.25
            // Steps 0-7: should return initial value (0)
            step025[0] = 0;
            assertThat(f025.getCurrentValue()).isEqualTo(0.0);
            for (int s = 1; s <= 7; s++) {
                step025[0] = s;
                f025.getCurrentValue(); // advance
            }
            // Step 8 (time 2.0): delay elapsed, should return the input from step 0
            step025[0] = 8;
            assertThat(f025.getCurrentValue()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("SMOOTH converges at same rate regardless of DT (#940)")
        void smoothConvergesAtSameRate() {
            long[] step1 = {0};
            double[] dt1 = {1.0};
            Formula f1 = compileWithDt(1.0, "SMOOTH(Input, 5, 0)", step1, dt1);

            long[] step025 = {0};
            double[] dt025 = {0.25};
            Formula f025 = compileWithDt(0.25, "SMOOTH(Input, 5, 0)", step025, dt025);

            // Initialize both
            step1[0] = 0;
            f1.getCurrentValue();
            step025[0] = 0;
            f025.getCurrentValue();

            // Advance both to time = 10 (2 smoothing times)
            // DT=1: step 10, DT=0.25: step 40
            for (long s = 1; s <= 10; s++) {
                step1[0] = s;
                f1.getCurrentValue();
            }
            for (long s = 1; s <= 40; s++) {
                step025[0] = s;
                f025.getCurrentValue();
            }
            double val1 = f1.getCurrentValue();
            double val025 = f025.getCurrentValue();
            // Both should be close to the same value (approaching 100)
            // Allow 5% tolerance due to Euler discretization differences
            assertThat(val025).isCloseTo(val1, within(val1 * 0.05));
        }

        @Test
        @DisplayName("DELAY1 produces similar dynamics regardless of DT (#940)")
        void delay1ProducesSimilarDynamics() {
            long[] step1 = {0};
            double[] dt1 = {1.0};
            Formula f1 = compileWithDt(1.0, "DELAY1(Input, 6, 0)", step1, dt1);

            long[] step025 = {0};
            double[] dt025 = {0.25};
            Formula f025 = compileWithDt(0.25, "DELAY1(Input, 6, 0)", step025, dt025);

            // Initialize both
            step1[0] = 0;
            f1.getCurrentValue();
            step025[0] = 0;
            f025.getCurrentValue();

            // Advance both to time = 12 (2 delay times)
            for (long s = 1; s <= 12; s++) {
                step1[0] = s;
                f1.getCurrentValue();
            }
            for (long s = 1; s <= 48; s++) {
                step025[0] = s;
                f025.getCurrentValue();
            }
            double val1 = f1.getCurrentValue();
            double val025 = f025.getCurrentValue();
            // Both should be close (approaching 100)
            assertThat(val025).isCloseTo(val1, within(val1 * 0.05));
        }
    }
}
