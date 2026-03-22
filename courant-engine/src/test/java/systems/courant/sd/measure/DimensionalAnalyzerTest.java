package systems.courant.sd.measure;

import systems.courant.sd.model.expr.Expr;
import systems.courant.sd.model.expr.ExprParser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DimensionalAnalyzer")
class DimensionalAnalyzerTest {

    /**
     * Simple unit context backed by a map of element names to composite units.
     */
    private static class TestContext implements DimensionalAnalyzer.UnitContext {
        private final Map<String, CompositeUnit> units = new HashMap<>();

        TestContext put(String name, CompositeUnit unit) {
            units.put(name, unit);
            return this;
        }

        @Override
        public Optional<CompositeUnit> resolveUnit(String elementName) {
            CompositeUnit u = units.get(elementName);
            if (u == null) {
                u = units.get(elementName.replace('_', ' '));
            }
            return Optional.ofNullable(u);
        }
    }

    private static final CompositeUnit ITEMS = new CompositeUnit(Map.of(Dimension.ITEM, 1));
    private static final CompositeUnit TIME = new CompositeUnit(Map.of(Dimension.TIME, 1));
    private static final CompositeUnit RATE = new CompositeUnit(
            Map.of(Dimension.ITEM, 1, Dimension.TIME, -1));
    private static final CompositeUnit MASS = new CompositeUnit(Map.of(Dimension.MASS, 1));

    private DimensionalAnalyzer.AnalysisResult analyze(String equation, TestContext ctx) {
        Expr expr = ExprParser.parse(equation);
        return new DimensionalAnalyzer(ctx).analyze(expr);
    }

    @Nested
    @DisplayName("Basic expressions")
    class BasicExpressions {

        @Test
        void shouldReturnDimensionlessForLiteral() {
            var result = analyze("42", new TestContext());
            assertThat(result.inferredUnit()).isNotNull();
            assertThat(result.inferredUnit().isDimensionless()).isTrue();
            assertThat(result.isConsistent()).isTrue();
        }

        @Test
        void shouldReturnUnitForStockReference() {
            TestContext ctx = new TestContext().put("Population", ITEMS);
            var result = analyze("Population", ctx);
            assertThat(result.inferredUnit()).isEqualTo(ITEMS);
            assertThat(result.isConsistent()).isTrue();
        }

        @Test
        void shouldReturnNullForUnknownReference() {
            var result = analyze("unknown_var", new TestContext());
            assertThat(result.inferredUnit()).isNull();
        }
    }

    @Nested
    @DisplayName("Addition and subtraction")
    class AddSub {

        @Test
        void shouldAllowAddingCompatibleUnits() {
            TestContext ctx = new TestContext()
                    .put("A", ITEMS)
                    .put("B", ITEMS);
            var result = analyze("A + B", ctx);
            assertThat(result.inferredUnit()).isEqualTo(ITEMS);
            assertThat(result.isConsistent()).isTrue();
        }

        @Test
        void shouldWarnWhenAddingIncompatibleUnits() {
            TestContext ctx = new TestContext()
                    .put("Pop", ITEMS)
                    .put("Weight", MASS);
            var result = analyze("Pop + Weight", ctx);
            assertThat(result.isConsistent()).isFalse();
            assertThat(result.warnings()).hasSize(1);
            assertThat(result.warnings().getFirst().message())
                    .contains("incompatible");
        }

        @Test
        void shouldAllowSubtractingCompatibleUnits() {
            TestContext ctx = new TestContext()
                    .put("A", ITEMS)
                    .put("B", ITEMS);
            var result = analyze("A - B", ctx);
            assertThat(result.inferredUnit()).isEqualTo(ITEMS);
            assertThat(result.isConsistent()).isTrue();
        }
    }

    @Nested
    @DisplayName("Multiplication and division")
    class MulDiv {

        @Test
        void shouldMultiplyDimensions() {
            TestContext ctx = new TestContext()
                    .put("Pop", ITEMS)
                    .put("Duration", TIME);
            var result = analyze("Pop * Duration", ctx);
            assertThat(result.inferredUnit().exponents())
                    .containsEntry(Dimension.ITEM, 1)
                    .containsEntry(Dimension.TIME, 1);
            assertThat(result.isConsistent()).isTrue();
        }

        @Test
        void shouldDivideDimensions() {
            TestContext ctx = new TestContext()
                    .put("Pop", ITEMS)
                    .put("Duration", TIME);
            var result = analyze("Pop / Duration", ctx);
            assertThat(result.inferredUnit()).isEqualTo(RATE);
            assertThat(result.isConsistent()).isTrue();
        }

        @Test
        void shouldCancelDimensions() {
            TestContext ctx = new TestContext()
                    .put("A", ITEMS)
                    .put("B", ITEMS);
            var result = analyze("A / B", ctx);
            assertThat(result.inferredUnit().isDimensionless()).isTrue();
        }

        @Test
        void shouldMultiplyByDimensionlessScalar() {
            TestContext ctx = new TestContext().put("Pop", ITEMS);
            var result = analyze("Pop * 0.1", ctx);
            assertThat(result.inferredUnit()).isEqualTo(ITEMS);
            assertThat(result.isConsistent()).isTrue();
        }
    }

    @Nested
    @DisplayName("Power operator")
    class Power {

        @Test
        void shouldHandleIntegerPower() {
            TestContext ctx = new TestContext()
                    .put("L", new CompositeUnit(Map.of(Dimension.LENGTH, 1)));
            var result = analyze("L ** 2", ctx);
            assertThat(result.inferredUnit().exponents())
                    .containsEntry(Dimension.LENGTH, 2);
            assertThat(result.isConsistent()).isTrue();
        }

        @Test
        void shouldWarnOnNonIntegerPowerOfDimensionedQuantity() {
            TestContext ctx = new TestContext()
                    .put("L", new CompositeUnit(Map.of(Dimension.LENGTH, 1)))
                    .put("x", CompositeUnit.dimensionless());
            var result = analyze("L ** x", ctx);
            assertThat(result.isConsistent()).isFalse();
        }
    }

    @Nested
    @DisplayName("Comparison operators")
    class Comparisons {

        @Test
        void shouldReturnDimensionlessForComparison() {
            TestContext ctx = new TestContext()
                    .put("A", ITEMS)
                    .put("B", ITEMS);
            var result = analyze("A > B", ctx);
            assertThat(result.inferredUnit().isDimensionless()).isTrue();
            assertThat(result.isConsistent()).isTrue();
        }

        @Test
        void shouldWarnOnIncompatibleComparison() {
            TestContext ctx = new TestContext()
                    .put("Pop", ITEMS)
                    .put("Weight", MASS);
            var result = analyze("Pop > Weight", ctx);
            assertThat(result.isConsistent()).isFalse();
        }
    }

    @Nested
    @DisplayName("Functions")
    class Functions {

        @Test
        void shouldReturnTimeDimensionForTIME() {
            var result = analyze("TIME", new TestContext());
            assertThat(result.inferredUnit()).isEqualTo(TIME);
        }

        @Test
        void shouldReturnTimeDimensionForDT() {
            var result = analyze("DT", new TestContext());
            assertThat(result.inferredUnit()).isEqualTo(TIME);
        }

        @Test
        void shouldPreserveDimensionForABS() {
            TestContext ctx = new TestContext().put("Pop", ITEMS);
            var result = analyze("ABS(Pop)", ctx);
            assertThat(result.inferredUnit()).isEqualTo(ITEMS);
        }

        @Test
        void shouldPreserveDimensionForSMOOTH() {
            TestContext ctx = new TestContext().put("Flow", RATE);
            var result = analyze("SMOOTH(Flow, 5)", ctx);
            assertThat(result.inferredUnit()).isEqualTo(RATE);
        }

        @Test
        void shouldReturnDimensionlessForLN() {
            var result = analyze("LN(10)", new TestContext());
            assertThat(result.inferredUnit().isDimensionless()).isTrue();
        }

        @Test
        void shouldReturnDimensionlessForEXP() {
            var result = analyze("EXP(2)", new TestContext());
            assertThat(result.inferredUnit().isDimensionless()).isTrue();
        }

        @Test
        void shouldPreserveDimensionForMIN() {
            TestContext ctx = new TestContext()
                    .put("A", ITEMS)
                    .put("B", ITEMS);
            var result = analyze("MIN(A, B)", ctx);
            assertThat(result.inferredUnit()).isEqualTo(ITEMS);
            assertThat(result.isConsistent()).isTrue();
        }

        @Test
        void shouldWarnForMINWithIncompatibleUnits() {
            TestContext ctx = new TestContext()
                    .put("Pop", ITEMS)
                    .put("Weight", MASS);
            var result = analyze("MIN(Pop, Weight)", ctx);
            assertThat(result.isConsistent()).isFalse();
        }

        @Test
        void shouldReturnDimensionlessForLOOKUPWithExplicitDimensionlessUnit() {
            TestContext ctx = new TestContext().put("table", CompositeUnit.dimensionless());
            var result = analyze("LOOKUP(table, 5)", ctx);
            assertThat(result.inferredUnit().isDimensionless()).isTrue();
            assertThat(result.isConsistent()).isTrue();
        }

        @Test
        void shouldReturnNullForLOOKUPWhenTableHasNoUnit() {
            // Table not in context → unknown unit → return null
            var result = analyze("LOOKUP(unknown_table, 5)", new TestContext());
            assertThat(result.inferredUnit()).isNull();
            assertThat(result.isConsistent()).isTrue();
        }

        @Test
        void shouldAnalyzeLOOKUPArgumentsForWarnings() {
            TestContext ctx = new TestContext()
                    .put("Pop", ITEMS)
                    .put("Weight", MASS);
            // LOOKUP input adds incompatible units → should produce warning
            var result = analyze("LOOKUP(unknown_table, Pop + Weight)", ctx);
            assertThat(result.isConsistent()).isFalse();
            assertThat(result.warnings().getFirst().message())
                    .contains("incompatible");
        }

        @Test
        void shouldReturnDeclaredUnitForLOOKUP() {
            TestContext ctx = new TestContext().put("effect_table", ITEMS);
            var result = analyze("LOOKUP(effect_table, 5)", ctx);
            assertThat(result.inferredUnit()).isEqualTo(ITEMS);
            assertThat(result.isConsistent()).isTrue();
        }

        @Test
        void shouldReturnDeclaredUnitForLOOKUP_AREA() {
            TestContext ctx = new TestContext().put("area_table", MASS);
            var result = analyze("LOOKUP_AREA(area_table, 1, 10)", ctx);
            assertThat(result.inferredUnit()).isEqualTo(MASS);
            assertThat(result.isConsistent()).isTrue();
        }

        @Test
        void shouldWarnWhenLOOKUPUnitMismatchesContext() {
            TestContext ctx = new TestContext()
                    .put("effect_table", ITEMS)
                    .put("Weight", MASS);
            var result = analyze("LOOKUP(effect_table, 5) + Weight", ctx);
            assertThat(result.isConsistent()).isFalse();
            assertThat(result.warnings().getFirst().message())
                    .contains("incompatible");
        }

        @Test
        void shouldNotWarnWhenLOOKUPUnitMatchesContext() {
            TestContext ctx = new TestContext()
                    .put("effect_table", ITEMS)
                    .put("Pop", ITEMS);
            var result = analyze("LOOKUP(effect_table, 5) + Pop", ctx);
            assertThat(result.isConsistent()).isTrue();
            assertThat(result.inferredUnit()).isEqualTo(ITEMS);
        }

        @Test
        void shouldPropagateUnitForSQRTOfOddExponentInput() {
            // SQRT of a dimensioned quantity with odd exponents should warn
            // but propagate the original unit for downstream checking
            TestContext ctx = new TestContext()
                    .put("Volume", new CompositeUnit(Map.of(Dimension.LENGTH, 3)));
            var result = analyze("SQRT(Volume)", ctx);
            assertThat(result.isConsistent()).isFalse();
            assertThat(result.warnings().getFirst().message()).contains("SQRT");
            // Should propagate the original unit, not return dimensionless
            assertThat(result.inferredUnit()).isNotNull();
            assertThat(result.inferredUnit().isDimensionless()).isFalse();
        }

        @Test
        void shouldHalveExponentsForSQRTOfEvenDimensions() {
            TestContext ctx = new TestContext()
                    .put("Area", new CompositeUnit(Map.of(Dimension.LENGTH, 2)));
            var result = analyze("SQRT(Area)", ctx);
            assertThat(result.isConsistent()).isTrue();
            assertThat(result.inferredUnit().exponents())
                    .containsEntry(Dimension.LENGTH, 1);
        }

        @Test
        void shouldReturnDimensionlessForSTEP() {
            var result = analyze("STEP(10, 5)", new TestContext());
            assertThat(result.inferredUnit().isDimensionless()).isTrue();
        }
    }

    @Nested
    @DisplayName("Conditionals")
    class Conditionals {

        @Test
        void shouldInferDimensionFromMatchingBranches() {
            TestContext ctx = new TestContext()
                    .put("A", ITEMS)
                    .put("B", ITEMS)
                    .put("flag", CompositeUnit.dimensionless());
            var result = analyze("IF(flag > 0, A, B)", ctx);
            assertThat(result.inferredUnit()).isEqualTo(ITEMS);
            assertThat(result.isConsistent()).isTrue();
        }

        @Test
        void shouldWarnOnMismatchedBranches() {
            TestContext ctx = new TestContext()
                    .put("Pop", ITEMS)
                    .put("Weight", MASS);
            var result = analyze("IF(Pop > 0, Pop, Weight)", ctx);
            assertThat(result.isConsistent()).isFalse();
            assertThat(result.warnings().stream()
                    .anyMatch(w -> w.message().contains("IF branches")))
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("Unary operators")
    class UnaryOps {

        @Test
        void shouldPreserveDimensionForNegate() {
            TestContext ctx = new TestContext().put("Pop", ITEMS);
            var result = analyze("-Pop", ctx);
            assertThat(result.inferredUnit()).isEqualTo(ITEMS);
        }

        @Test
        void shouldReturnDimensionlessForNot() {
            var result = analyze("not 1", new TestContext());
            assertThat(result.inferredUnit().isDimensionless()).isTrue();
        }
    }

    @Nested
    @DisplayName("Complex expressions")
    class ComplexExpressions {

        @Test
        void shouldInferRateFromPopulationTimesBirthRate() {
            TestContext ctx = new TestContext()
                    .put("Population", ITEMS)
                    .put("birth_rate", CompositeUnit.dimensionless());
            var result = analyze("Population * birth_rate", ctx);
            assertThat(result.inferredUnit()).isEqualTo(ITEMS);
            assertThat(result.isConsistent()).isTrue();
        }

        @Test
        void shouldInferCorrectUnitsForSIRInfectionRate() {
            // Infection = contact_rate * Infectious / Total * Infectivity * Susceptible
            // All stocks are ITEMS, rates are dimensionless → result is ITEMS
            TestContext ctx = new TestContext()
                    .put("Contact_Rate", CompositeUnit.dimensionless())
                    .put("Infectious", ITEMS)
                    .put("Susceptible", ITEMS)
                    .put("Recovered", ITEMS)
                    .put("Infectivity", CompositeUnit.dimensionless());
            var result = analyze(
                    "Contact_Rate * Infectious / (Susceptible + Infectious + Recovered) "
                    + "* Infectivity * Susceptible", ctx);
            assertThat(result.inferredUnit()).isEqualTo(ITEMS);
            assertThat(result.isConsistent()).isTrue();
        }

        @Test
        void shouldHandleNestedFunctions() {
            TestContext ctx = new TestContext()
                    .put("x", ITEMS)
                    .put("y", ITEMS);
            var result = analyze("ABS(MIN(x, y))", ctx);
            assertThat(result.inferredUnit()).isEqualTo(ITEMS);
            assertThat(result.isConsistent()).isTrue();
        }
    }
}
