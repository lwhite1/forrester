package systems.courant.forrester.model.compile;

import systems.courant.forrester.model.def.AuxDef;
import systems.courant.forrester.model.def.FlowDef;
import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.ModelDefinitionBuilder;
import systems.courant.forrester.model.def.StockDef;
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
                    .aux("births_per_year", "Population * birth_rate", "Person/Year", List.of("Region"))
                    .build();

            ModelDefinition expanded = SubscriptExpander.expand(def);

            // 1 literal constant (birth_rate) + 2 expanded subscripted auxes
            assertThat(expanded.auxiliaries()).hasSize(3);
            assertThat(expanded.auxiliaries().stream().map(AuxDef::name))
                    .containsExactly("birth_rate", "births_per_year[North]", "births_per_year[South]");
            // birth_rate is not subscripted, so it stays unchanged in equations
            assertThat(expanded.auxiliaries().get(1).equation())
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
                    .aux("density", "Pop * 2", "Person", List.of("Region"))
                    .flow("growth", "Pop + density", "Year", null, "Pop", List.of("Region"))
                    .build();

            ModelDefinition expanded = SubscriptExpander.expand(def);

            assertThat(expanded.flows().get(0).equation()).isEqualTo("Pop[X] + density[X]");
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
