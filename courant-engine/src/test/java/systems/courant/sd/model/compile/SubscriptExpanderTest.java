package systems.courant.sd.model.compile;

import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;
import systems.courant.sd.model.def.StockDef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SubscriptExpander")
class SubscriptExpanderTest {

    @Test
    void shouldReturnUnchangedWhenNoSubscripts() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Simple")
                .stock("Tank", 100, "Thing")
                .flow("Drain", "Tank * 0.1", "Day", "Tank", null)
                .build();

        ModelDefinition expanded = SubscriptExpander.expand(def);
        assertThat(expanded.stocks()).hasSize(1);
        assertThat(expanded.flows()).hasSize(1);
    }

    @Nested
    @DisplayName("Stock expansion")
    class StockExpansion {

        @Test
        void shouldExpandSubscriptedStockIntoScalarStocks() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Regional")
                    .subscript("Region", List.of("North", "South"))
                    .stock("Population", 100, "Person", List.of("Region"))
                    .build();

            ModelDefinition expanded = SubscriptExpander.expand(def);

            assertThat(expanded.stocks()).hasSize(2);
            assertThat(expanded.stocks().stream().map(StockDef::name))
                    .containsExactly("Population[North]", "Population[South]");
            assertThat(expanded.stocks().stream().map(StockDef::subscripts))
                    .allMatch(List::isEmpty);
        }
    }

    @Nested
    @DisplayName("Flow expansion")
    class FlowExpansion {

        @Test
        void shouldExpandFlowWithSubscriptedSourceSink() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Regional Drain")
                    .subscript("Region", List.of("North", "South"))
                    .stock("Population", 100, "Person", List.of("Region"))
                    .flow("births", "Population * 0.02", "Year", null, "Population", List.of("Region"))
                    .build();

            ModelDefinition expanded = SubscriptExpander.expand(def);

            assertThat(expanded.flows()).hasSize(2);
            assertThat(expanded.flows().stream().map(FlowDef::name))
                    .containsExactly("births[North]", "births[South]");
            assertThat(expanded.flows().get(0).equation()).isEqualTo("Population[North] * 0.02");
            assertThat(expanded.flows().get(0).sink()).isEqualTo("Population[North]");
            assertThat(expanded.flows().get(1).equation()).isEqualTo("Population[South] * 0.02");
            assertThat(expanded.flows().get(1).sink()).isEqualTo("Population[South]");
        }
    }

    @Nested
    @DisplayName("Auxiliary expansion")
    class AuxExpansion {

        @Test
        void shouldExpandAuxWithSubscriptedReferences() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Regional Aux")
                    .subscript("Region", List.of("North", "South"))
                    .stock("Population", 100, "Person", List.of("Region"))
                    .constant("birth_rate", 0.02, "1/Year")
                    .variable("births_per_year", "Population * birth_rate", "Person/Year", List.of("Region"))
                    .build();

            ModelDefinition expanded = SubscriptExpander.expand(def);

            // 1 literal constant (birth_rate) + 2 expanded subscripted auxes
            assertThat(expanded.variables()).hasSize(3);
            assertThat(expanded.variables().stream().map(VariableDef::name))
                    .containsExactly("birth_rate", "births_per_year[North]", "births_per_year[South]");
            // birth_rate is not subscripted, so it stays unchanged in equations
            assertThat(expanded.variables().get(1).equation())
                    .isEqualTo("Population[North] * birth_rate");
        }
    }

    @Nested
    @DisplayName("Equation rewriting")
    class EquationRewriting {

        @Test
        void shouldNotRewriteNonSubscriptedNames() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Mixed")
                    .subscript("Region", List.of("A", "B"))
                    .stock("Pop", 100, "Person", List.of("Region"))
                    .constant("rate", 0.5, "1/Year")
                    .flow("inflow", "Pop * rate", "Year", null, "Pop", List.of("Region"))
                    .build();

            ModelDefinition expanded = SubscriptExpander.expand(def);

            assertThat(expanded.flows().get(0).equation()).isEqualTo("Pop[A] * rate");
            assertThat(expanded.flows().get(1).equation()).isEqualTo("Pop[B] * rate");
        }

        @Test
        void shouldHandleMultipleSubscriptedReferencesInEquation() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Multi Ref")
                    .subscript("Region", List.of("X", "Y"))
                    .stock("Pop", 100, "Person", List.of("Region"))
                    .variable("density", "Pop * 2", "Person", List.of("Region"))
                    .flow("growth", "Pop + density", "Year", null, "Pop", List.of("Region"))
                    .build();

            ModelDefinition expanded = SubscriptExpander.expand(def);

            assertThat(expanded.flows().get(0).equation()).isEqualTo("Pop[X] + density[X]");
        }
    }

    @Nested
    @DisplayName("Multi-dimensional expansion")
    class MultiDimensionalExpansion {

        @Test
        void shouldExpandTwoDimensionalStockAsCartesianProduct() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("2D")
                    .subscript("Region", List.of("North", "South", "East"))
                    .subscript("Age", List.of("Young", "Old"))
                    .stock("Pop", 100, "Person", List.of("Region", "Age"))
                    .build();

            ModelDefinition expanded = SubscriptExpander.expand(def);

            assertThat(expanded.stocks()).hasSize(6); // 3 × 2 = 6
            assertThat(expanded.stocks().stream().map(StockDef::name))
                    .containsExactly(
                            "Pop[North,Young]", "Pop[North,Old]",
                            "Pop[South,Young]", "Pop[South,Old]",
                            "Pop[East,Young]", "Pop[East,Old]");
        }

        @Test
        void shouldExpandTwoDimensionalFlowWithEquationRewriting() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("2D Flow")
                    .subscript("Region", List.of("A", "B"))
                    .subscript("Age", List.of("X", "Y"))
                    .stock("Pop", 100, "Person", List.of("Region", "Age"))
                    .flow("births", "Pop * 0.02", "Year", null, "Pop",
                            List.of("Region", "Age"))
                    .build();

            ModelDefinition expanded = SubscriptExpander.expand(def);

            assertThat(expanded.flows()).hasSize(4); // 2 × 2 = 4
            assertThat(expanded.flows().stream().map(FlowDef::name))
                    .containsExactly("births[A,X]", "births[A,Y]",
                            "births[B,X]", "births[B,Y]");
            assertThat(expanded.flows().get(0).equation()).isEqualTo("Pop[A,X] * 0.02");
            assertThat(expanded.flows().get(0).sink()).isEqualTo("Pop[A,X]");
            assertThat(expanded.flows().get(3).equation()).isEqualTo("Pop[B,Y] * 0.02");
        }

        @Test
        void shouldExpandTwoDimensionalAuxiliary() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("2D Aux")
                    .subscript("Region", List.of("A", "B"))
                    .subscript("Age", List.of("X", "Y"))
                    .stock("Pop", 100, "Person", List.of("Region", "Age"))
                    .variable("density", "Pop * 2", "Person", List.of("Region", "Age"))
                    .build();

            ModelDefinition expanded = SubscriptExpander.expand(def);

            assertThat(expanded.variables()).hasSize(4); // 2 × 2 = 4
            assertThat(expanded.variables().get(0).name()).isEqualTo("density[A,X]");
            assertThat(expanded.variables().get(0).equation()).isEqualTo("Pop[A,X] * 2");
        }

        @Test
        void shouldExpandPartialSubscriptInTwoDimensionalEquation() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("2D Partial")
                    .subscript("Region", List.of("North", "South"))
                    .subscript("Age", List.of("Young", "Old"))
                    .stock("Pop", 100, "Person", List.of("Region", "Age"))
                    .variable("rate", "Pop[North] * 0.02", "Person/Year",
                            List.of("Region", "Age"))
                    .build();

            ModelDefinition expanded = SubscriptExpander.expand(def);

            // rate[North,Young] should reference Pop[North,Young]
            assertThat(expanded.variables().get(0).name()).isEqualTo("rate[North,Young]");
            assertThat(expanded.variables().get(0).equation()).isEqualTo("Pop[North,Young] * 0.02");
            // rate[North,Old] should reference Pop[North,Old]
            assertThat(expanded.variables().get(1).name()).isEqualTo("rate[North,Old]");
            assertThat(expanded.variables().get(1).equation()).isEqualTo("Pop[North,Old] * 0.02");
        }

        @Test
        void shouldLeaveFullSubscriptUnchanged() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("2D Full")
                    .subscript("Region", List.of("North", "South"))
                    .subscript("Age", List.of("Young", "Old"))
                    .stock("Pop", 100, "Person", List.of("Region", "Age"))
                    .variable("rate", "Pop[North,Young] * 0.02", "Person/Year",
                            List.of("Region", "Age"))
                    .build();

            ModelDefinition expanded = SubscriptExpander.expand(def);

            // Full subscript should remain unchanged
            assertThat(expanded.variables().get(0).equation())
                    .isEqualTo("Pop[North,Young] * 0.02");
            assertThat(expanded.variables().get(1).equation())
                    .isEqualTo("Pop[North,Young] * 0.02");
        }

        @Test
        void shouldExpandThreeDimensions() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("3D")
                    .subscript("A", List.of("a1", "a2"))
                    .subscript("B", List.of("b1", "b2"))
                    .subscript("C", List.of("c1", "c2"))
                    .stock("S", 0, "Thing", List.of("A", "B", "C"))
                    .build();

            ModelDefinition expanded = SubscriptExpander.expand(def);

            assertThat(expanded.stocks()).hasSize(8); // 2 × 2 × 2 = 8
            assertThat(expanded.stocks().stream().map(StockDef::name))
                    .contains("S[a1,b1,c1]", "S[a2,b2,c2]");
        }
    }

    @Nested
    @DisplayName("Cartesian product helper")
    class CartesianProductHelper {

        @Test
        void shouldReturnSingleLabelsForOneDimension() {
            List<List<String>> result = SubscriptExpander.cartesianProduct(
                    List.of(List.of("A", "B", "C")));
            assertThat(result).hasSize(3);
            assertThat(result.get(0)).containsExactly("A");
        }

        @Test
        void shouldComputeProductForTwoDimensions() {
            List<List<String>> result = SubscriptExpander.cartesianProduct(
                    List.of(List.of("X", "Y"), List.of("1", "2", "3")));
            assertThat(result).hasSize(6);
            assertThat(result.get(0)).containsExactly("X", "1");
            assertThat(result.get(5)).containsExactly("Y", "3");
        }
    }

    @Nested
    @DisplayName("Backtick-quoted identifiers (#859)")
    class BacktickQuotedIdentifiers {

        @Test
        @DisplayName("should place [label] after closing backtick, not inside")
        void shouldHandleBacktickQuotedName() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Quoted Ref")
                    .subscript("Region", List.of("North", "South"))
                    .stock("Population Growth", 100, "Person", List.of("Region"))
                    .variable("net rate", "`Population Growth` * 0.02", "Person/Year",
                            List.of("Region"))
                    .build();

            ModelDefinition expanded = SubscriptExpander.expand(def);

            assertThat(expanded.variables().get(0).equation())
                    .isEqualTo("`Population Growth`[North] * 0.02");
            assertThat(expanded.variables().get(1).equation())
                    .isEqualTo("`Population Growth`[South] * 0.02");
        }

        @Test
        @DisplayName("should handle mix of quoted and unquoted subscripted refs")
        void shouldHandleMixedQuotedAndUnquoted() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Mixed Quoting")
                    .subscript("Region", List.of("A"))
                    .stock("Population Growth", 100, "Person", List.of("Region"))
                    .stock("Pop", 50, "Person", List.of("Region"))
                    .variable("total", "`Population Growth` + Pop", "Person",
                            List.of("Region"))
                    .build();

            ModelDefinition expanded = SubscriptExpander.expand(def);

            assertThat(expanded.variables().get(0).equation())
                    .isEqualTo("`Population Growth`[A] + Pop[A]");
        }
    }

    @Test
    void shouldThrowOnUnknownSubscriptDimension() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Bad")
                .subscript("Region", List.of("North", "South"))
                .stock("Pop", 100, "Person", List.of("Missing"))
                .build();

        assertThatThrownBy(() -> SubscriptExpander.expand(def))
                .isInstanceOf(CompilationException.class)
                .hasMessageContaining("unknown subscript");
    }
}
