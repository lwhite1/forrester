package systems.courant.sd.app.canvas.forms;

import systems.courant.sd.measure.CompositeUnit;
import systems.courant.sd.measure.DimensionalAnalyzer;
import systems.courant.sd.measure.UnitRegistry;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;
import systems.courant.sd.model.expr.ExprParser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit auto-fill from equation inference (#1317)")
class UnitAutoFillTest {

    private UnitRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new UnitRegistry();
    }

    @Nested
    @DisplayName("Dimensional analysis inference")
    class DimensionalInference {

        @Test
        @DisplayName("should infer non-dimensionless unit from typed stock reference")
        void shouldInferFromStock() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, "Person")
                    .constant("Birth Rate", 0.03, "Dimensionless")
                    .build();

            var result = analyze(def, "Population * Birth_Rate");

            assertThat(result.inferredUnit()).isNotNull();
            assertThat(result.isConsistent()).isTrue();
            assertThat(result.inferredUnit().isDimensionless()).isFalse();
        }

        @Test
        @DisplayName("should return dimensionless for pure constant")
        void shouldReturnDimensionlessForConstant() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .build();

            var result = analyze(def, "42");

            assertThat(result.inferredUnit()).isNotNull();
            assertThat(result.inferredUnit().isDimensionless()).isTrue();
        }

        @Test
        @DisplayName("should infer compound unit from division")
        void shouldInferCompound() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 1000, "Person")
                    .stock("Area", 50, "Kilometer")
                    .build();

            var result = analyze(def, "Population / Area");

            assertThat(result.inferredUnit()).isNotNull();
            assertThat(result.isConsistent()).isTrue();
            assertThat(result.inferredUnit().isDimensionless()).isFalse();
        }

        @Test
        @DisplayName("should report inconsistency for mismatched addition")
        void shouldReportInconsistentAddition() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, "Person")
                    .stock("Money", 500, "USD")
                    .build();

            var result = analyze(def, "Population + Money");

            assertThat(result.isConsistent()).isFalse();
        }

        @Test
        @DisplayName("should infer from nested expressions")
        void shouldInferFromNested() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, "Person")
                    .constant("Rate", 0.05, "Dimensionless")
                    .constant("Factor", 2, "Dimensionless")
                    .build();

            var result = analyze(def, "(Population * Rate) * Factor");

            assertThat(result.inferredUnit()).isNotNull();
            assertThat(result.isConsistent()).isTrue();
            assertThat(result.inferredUnit().isDimensionless()).isFalse();
        }
    }

    @Nested
    @DisplayName("Flow material unit extraction")
    class FlowMaterial {

        @Test
        @DisplayName("multiplying rate by time cancels the time dimension")
        void shouldRecoverMaterial() {
            var timeUnit = registry.resolveTimeUnit("Day");
            var personUnit = registry.resolve("Person");
            CompositeUnit rate = CompositeUnit.ofRate(personUnit, timeUnit);

            CompositeUnit material = rate.multiply(CompositeUnit.of(timeUnit));

            assertThat(material.isDimensionless()).isFalse();
            // The dimension is ITEM^1, which is correct
            assertThat(material.exponents()).hasSize(1);
        }

        @Test
        @DisplayName("dimensionless rate times time yields just time (not useful)")
        void shouldNotProduceMaterialFromDimensionless() {
            CompositeUnit dimensionless = CompositeUnit.dimensionless();
            var timeUnit = registry.resolveTimeUnit("Day");

            CompositeUnit result = dimensionless.multiply(CompositeUnit.of(timeUnit));

            // Dimensionless * Day = TIME^1 — not a useful material unit
            assertThat(result.isDimensionless()).isFalse();
        }

        @Test
        @DisplayName("compound rate correctly cancels time")
        void shouldHandleCompoundRate() {
            var timeUnit = registry.resolveTimeUnit("Year");
            var usdUnit = registry.resolve("USD");
            CompositeUnit rate = CompositeUnit.ofRate(usdUnit, timeUnit);

            CompositeUnit material = rate.multiply(CompositeUnit.of(timeUnit));

            assertThat(material.isDimensionless()).isFalse();
            assertThat(material.exponents()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle TIME function in equation")
        void shouldHandleTimeFunction() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, "Person")
                    .build();

            var result = analyze(def, "Population * TIME");

            assertThat(result.inferredUnit()).isNotNull();
            assertThat(result.inferredUnit().isDimensionless()).isFalse();
        }

        @Test
        @DisplayName("should handle unknown references gracefully")
        void shouldHandleUnknownReferences() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .constant("X", 1, "")
                    .build();

            var result = analyze(def, "Unknown_Var * 2");

            assertThat(result.inferredUnit()).isNotNull();
        }
    }

    private DimensionalAnalyzer.AnalysisResult analyze(ModelDefinition def, String equation) {
        DimensionalAnalyzer.UnitContext context = elementName -> {
            String resolved = elementName.replace('_', ' ');
            for (var stock : def.stocks()) {
                if (stock.name().equals(elementName) || stock.name().equals(resolved)) {
                    if (stock.unit() != null && !stock.unit().isBlank()) {
                        return Optional.of(registry.resolveComposite(stock.unit()));
                    }
                    return Optional.of(CompositeUnit.dimensionless());
                }
            }
            for (var v : def.variables()) {
                if (v.name().equals(elementName) || v.name().equals(resolved)) {
                    if (v.unit() != null && !v.unit().isBlank()) {
                        return Optional.of(registry.resolveComposite(v.unit()));
                    }
                    return Optional.of(CompositeUnit.dimensionless());
                }
            }
            return Optional.empty();
        };
        return new DimensionalAnalyzer(context).analyze(ExprParser.parse(equation));
    }
}
