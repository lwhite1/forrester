package systems.courant.forrester.model.compile;

import systems.courant.forrester.measure.TimeUnit;
import systems.courant.forrester.measure.UnitRegistry;
import systems.courant.forrester.model.Constant;
import systems.courant.forrester.model.Flow;
import systems.courant.forrester.model.LookupTable;
import systems.courant.forrester.model.Stock;
import systems.courant.forrester.model.Variable;
import systems.courant.forrester.model.def.LookupTableDef;
import systems.courant.forrester.measure.Quantity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static systems.courant.forrester.measure.Units.MINUTE;
import static systems.courant.forrester.measure.Units.THING;

@DisplayName("CompilationContext")
class CompilationContextTest {

    private CompilationContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new CompilationContext(new UnitRegistry(), () -> 0);
    }

    @Nested
    @DisplayName("Element registration and retrieval")
    class Registration {

        @Test
        void shouldRegisterAndRetrieveStock() {
            Stock stock = new Stock("Pop", 100, THING);
            ctx.addStock("Pop", stock);
            assertThat(ctx.getStocks()).containsKey("Pop");
            assertThat(ctx.resolveValue("Pop")).isEqualTo(100.0);
        }

        @Test
        void shouldRegisterAndRetrieveConstant() {
            Constant c = new Constant("Rate", THING, 0.05);
            ctx.addConstant("Rate", c);
            assertThat(ctx.getConstants()).containsKey("Rate");
            assertThat(ctx.resolveValue("Rate")).isEqualTo(0.05);
        }

        @Test
        void shouldRegisterAndRetrieveVariable() {
            Variable v = new Variable("Ratio", THING, () -> 0.5);
            ctx.addVariable("Ratio", v);
            assertThat(ctx.getVariables()).containsKey("Ratio");
            assertThat(ctx.resolveValue("Ratio")).isCloseTo(0.5, within(1e-9));
        }

        @Test
        void shouldRegisterAndRetrieveFlow() {
            Flow flow = Flow.create("Drain", MINUTE, () -> new Quantity(10, THING));
            ctx.addFlow("Drain", flow);
            assertThat(ctx.getFlows()).containsKey("Drain");
        }

        @Test
        void shouldReturnUnmodifiableMaps() {
            assertThatThrownBy(() -> ctx.getStocks().put("x", null))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> ctx.getFlows().put("x", null))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> ctx.getVariables().put("x", null))
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> ctx.getConstants().put("x", null))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("resolveValue")
    class ResolveValue {

        @Test
        void shouldThrowForUnknownName() {
            assertThatThrownBy(() -> ctx.resolveValue("Missing"))
                    .isInstanceOf(CompilationException.class)
                    .hasMessageContaining("Missing");
        }

        @Test
        void shouldResolveWithUnderscoreFallback() {
            Constant c = new Constant("Birth Rate", THING, 0.03);
            ctx.addConstant("Birth Rate", c);
            assertThat(ctx.resolveValue("Birth_Rate")).isEqualTo(0.03);
        }

        @Test
        void shouldPreferExactMatchOverUnderscore() {
            ctx.addConstant("A_B", new Constant("A_B", THING, 1.0));
            ctx.addConstant("A B", new Constant("A B", THING, 2.0));
            // "A_B" has no underscore fallback needed — exact match
            assertThat(ctx.resolveValue("A_B")).isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("resolveConstant")
    class ResolveConstant {

        @Test
        void shouldReturnEmptyForNonConstant() {
            Stock stock = new Stock("Pop", 100, THING);
            ctx.addStock("Pop", stock);
            assertThat(ctx.resolveConstant("Pop")).isEmpty();
        }

        @Test
        void shouldResolveConstantValue() {
            ctx.addConstant("Rate", new Constant("Rate", THING, 0.05));
            assertThat(ctx.resolveConstant("Rate")).hasValue(0.05);
        }

        @Test
        void shouldResolveConstantWithUnderscoreFallback() {
            ctx.addConstant("Growth Rate", new Constant("Growth Rate", THING, 0.1));
            assertThat(ctx.resolveConstant("Growth_Rate")).hasValue(0.1);
        }

        @Test
        void shouldReturnEmptyForUnknownName() {
            assertThat(ctx.resolveConstant("Missing")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Parent context (hierarchical resolution)")
    class ParentContext {

        private CompilationContext child;

        @BeforeEach
        void setUp() {
            ctx.addConstant("Global", new Constant("Global", THING, 42.0));
            child = new CompilationContext(new UnitRegistry(), () -> 0, ctx);
        }

        @Test
        void shouldResolveFromParent() {
            assertThat(child.resolveValue("Global")).isEqualTo(42.0);
        }

        @Test
        void shouldPreferChildOverParent() {
            child.addConstant("Global", new Constant("Global", THING, 99.0));
            assertThat(child.resolveValue("Global")).isEqualTo(99.0);
        }

        @Test
        void shouldResolveConstantFromParent() {
            assertThat(child.resolveConstant("Global")).hasValue(42.0);
        }

        @Test
        void shouldThrowWhenNotInParentEither() {
            assertThatThrownBy(() -> child.resolveValue("Nowhere"))
                    .isInstanceOf(CompilationException.class);
        }

        @Test
        void shouldShareDtHolderWithParent() {
            assertThat(child.getDtHolder()).isSameAs(ctx.getDtHolder());
        }

        @Test
        void shouldShareSimTimeUnitHolderWithParent() {
            assertThat(child.getSimTimeUnitHolder()).isSameAs(ctx.getSimTimeUnitHolder());
        }
    }

    @Nested
    @DisplayName("Lookup tables")
    class LookupTables {

        @Test
        void shouldRegisterAndResolveLookupTable() {
            double[] input = {0.0};
            LookupTable table = LookupTable.linear(
                    new double[]{0, 1}, new double[]{0, 10}, () -> input[0]);
            ctx.addLookupTable("Effect", table, input);
            assertThat(ctx.resolveLookupTable("Effect")).isPresent();
        }

        @Test
        void shouldResolveLookupTableWithUnderscoreFallback() {
            double[] input = {0.0};
            LookupTable table = LookupTable.linear(
                    new double[]{0, 1}, new double[]{0, 10}, () -> input[0]);
            ctx.addLookupTable("My Table", table, input);
            assertThat(ctx.resolveLookupTable("My_Table")).isPresent();
        }

        @Test
        void shouldReturnEmptyForMissingLookup() {
            assertThat(ctx.resolveLookupTable("Missing")).isEmpty();
        }

        @Test
        void shouldRegisterAndResolveTableDef() {
            LookupTableDef def = new LookupTableDef("Effect",
                    new double[]{0, 1}, new double[]{0, 10}, "LINEAR");
            ctx.addLookupTableDef("Effect", def);
            assertThat(ctx.resolveLookupTableDef("Effect")).isPresent();
        }

        @Test
        void shouldCreateFreshLookupFromDef() {
            LookupTableDef def = new LookupTableDef("Effect",
                    new double[]{0, 1}, new double[]{0, 10}, "LINEAR");
            ctx.addLookupTableDef("Effect", def);
            var table = ctx.createFreshLookupTable("Effect", () -> 0.5);
            assertThat(table).isPresent();
            assertThat(table.get().getCurrentValue()).isCloseTo(5.0, within(0.01));
        }

        @Test
        void shouldCreateSplineLookupFromDef() {
            LookupTableDef def = new LookupTableDef("Curve",
                    new double[]{0, 0.5, 1}, new double[]{0, 8, 10}, "SPLINE");
            ctx.addLookupTableDef("Curve", def);
            var table = ctx.createFreshLookupTable("Curve", () -> 0.25);
            assertThat(table).isPresent();
        }

        @Test
        void shouldResolveLookupInputHolder() {
            double[] input = {0.0};
            LookupTable table = LookupTable.linear(
                    new double[]{0, 1}, new double[]{0, 10}, () -> input[0]);
            ctx.addLookupTable("Effect", table, input);
            assertThat(ctx.resolveLookupInputHolder("Effect")).hasValue(input);
        }

        @Test
        void shouldReturnEmptyForMissingInputHolder() {
            assertThat(ctx.resolveLookupInputHolder("Missing")).isEmpty();
        }

        @Test
        void shouldResolveLookupFromParentContext() {
            LookupTableDef def = new LookupTableDef("ParentTable",
                    new double[]{0, 1}, new double[]{0, 5}, "LINEAR");
            ctx.addLookupTableDef("ParentTable", def);
            CompilationContext child = new CompilationContext(new UnitRegistry(), () -> 0, ctx);
            assertThat(child.resolveLookupTableDef("ParentTable")).isPresent();
            assertThat(child.createFreshLookupTable("ParentTable", () -> 0.5)).isPresent();
        }
    }

    @Nested
    @DisplayName("DT and time unit holders")
    class DtAndTimeUnit {

        @Test
        void shouldDefaultDtToOne() {
            assertThat(ctx.getDt()).isEqualTo(1.0);
        }

        @Test
        void shouldShareDtHolderAcrossContexts() {
            ctx.getDtHolder()[0] = 0.25;
            assertThat(ctx.getDt()).isEqualTo(0.25);
        }

        @Test
        void shouldProvideCurrentStepSupplier() {
            int[] step = {0};
            CompilationContext stepCtx = new CompilationContext(new UnitRegistry(), () -> step[0]);
            step[0] = 5;
            assertThat(stepCtx.getCurrentStep().getAsInt()).isEqualTo(5);
        }

        @Test
        void shouldProvideUnitRegistry() {
            assertThat(ctx.getUnitRegistry()).isNotNull();
        }
    }
}
