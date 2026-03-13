package systems.courant.sd.model.def;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import systems.courant.sd.model.ModelMetadata;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("ModelDefinition records")
class ModelDefinitionTest {

    @Test
    void shouldCreateMinimalDefinition() {
        ModelDefinition def = new ModelDefinition(
                "Test", null, null,
                null, null, null, null, null, null, null, null);
        assertThat(def.name()).isEqualTo("Test");
        assertThat(def.stocks()).isEmpty();
        assertThat(def.flows()).isEmpty();
    }

    @Test
    void shouldRejectBlankName() {
        assertThatThrownBy(() ->
                new ModelDefinition("", null, null,
                        null, null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldReturnImmutableLists() {
        ModelDefinition def = new ModelDefinition(
                "Test", null, null,
                List.of(new StockDef("S", 100, "Person")),
                null, null, null, null, null, null, null);
        assertThatThrownBy(() ->
                def.stocks().add(new StockDef("S2", 50, "Person")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldSupportRecordEquality() {
        StockDef s1 = new StockDef("S", 100, "Person");
        StockDef s2 = new StockDef("S", 100, "Person");
        assertThat(s2).isEqualTo(s1);
        assertThat(s2.hashCode()).isEqualTo(s1.hashCode());
    }

    @Test
    void shouldCreateFlowDef() {
        FlowDef flow = new FlowDef("Infection", "rate * S", "Day", "Susceptible", "Infectious");
        assertThat(flow.name()).isEqualTo("Infection");
        assertThat(flow.equation()).isEqualTo("rate * S");
        assertThat(flow.timeUnit()).isEqualTo("Day");
        assertThat(flow.source()).isEqualTo("Susceptible");
        assertThat(flow.sink()).isEqualTo("Infectious");
    }

    @Test
    void shouldCreateLiteralVariableDef() {
        VariableDef c = new VariableDef("Rate", null, 0.5, "Dimensionless");
        assertThat(c.name()).isEqualTo("Rate");
        assertThat(c.literalValue()).isEqualTo(0.5);
    }

    @Test
    void shouldCreateVariableDef() {
        VariableDef v = new VariableDef("Infection Rate", "Contact_Rate * Infectivity", "Person");
        assertThat(v.name()).isEqualTo("Infection Rate");
        assertThat(v.equation()).isEqualTo("Contact_Rate * Infectivity");
    }

    @Test
    void shouldCloneLookupTableArrays() {
        double[] x = {0, 1, 2};
        double[] y = {0, 0.5, 1};
        LookupTableDef table = new LookupTableDef("Effect", x, y, "LINEAR");
        x[0] = 999; // mutate original
        assertThat(table.xValues()[0]).as("Array should be cloned").isCloseTo(0.0, within(0.0));
    }

    @Test
    void shouldCreateModuleInterface() {
        ModuleInterface iface = new ModuleInterface(
                List.of(new PortDef("in1", "Person")),
                List.of(new PortDef("out1", "Person")));
        assertThat(iface.inputs()).hasSize(1);
        assertThat(iface.outputs()).hasSize(1);
    }

    @Test
    void shouldCreateSubscriptDef() {
        SubscriptDef sub = new SubscriptDef("Region", List.of("North", "South", "East"));
        assertThat(sub.name()).isEqualTo("Region");
        assertThat(sub.labels()).containsExactly("North", "South", "East");
    }

    @Test
    void shouldReturnImmutableLabelsForSubscriptDef() {
        List<String> labels = new ArrayList<>(List.of("A", "B", "C"));
        SubscriptDef sub = new SubscriptDef("Dim", labels);
        labels.add("D"); // mutate original list
        assertThat(sub.labels()).containsExactly("A", "B", "C");
        assertThatThrownBy(() -> sub.labels().add("X"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldSupportSubscriptDefEqualsAndHashCode() {
        SubscriptDef s1 = new SubscriptDef("Region", List.of("North", "South"));
        SubscriptDef s2 = new SubscriptDef("Region", List.of("North", "South"));
        assertThat(s2).isEqualTo(s1);
        assertThat(s2.hashCode()).isEqualTo(s1.hashCode());

        SubscriptDef s3 = new SubscriptDef("Region", List.of("East", "West"));
        assertThat(s3).isNotEqualTo(s1);
    }

    @Test
    void shouldRejectBlankSubscriptName() {
        assertThatThrownBy(() -> new SubscriptDef("", List.of("A")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SubscriptDef(null, List.of("A")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectNullOrEmptyLabels() {
        assertThatThrownBy(() -> new SubscriptDef("Empty", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SubscriptDef("Empty", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Nested
    @DisplayName("toBuilder()")
    class ToBuilderTest {

        private ModelDefinition buildFullDefinition() {
            return new ModelDefinition(
                    "Original", "A comment", null,
                    List.of(new StockDef("Pop", 1000, "People")),
                    List.of(new FlowDef("Births", "Pop * br", "Year", null, "Pop")),
                    List.of(new VariableDef("br", null, 0.03, "1/Year")),
                    List.of(), List.of(), List.of(),
                    List.of(), List.of(), List.of(),
                    new SimulationSettings("Year", 100, "Year"),
                    new ModelMetadata("Author", "Source", "MIT", null));
        }

        @Test
        void shouldRoundTripAllFields() {
            ModelDefinition original = buildFullDefinition();
            ModelDefinition copy = original.toBuilder().build();

            assertThat(copy).isEqualTo(original);
        }

        @Test
        void shouldOverrideName() {
            ModelDefinition original = buildFullDefinition();
            ModelDefinition modified = original.toBuilder().name("Renamed").build();

            assertThat(modified.name()).isEqualTo("Renamed");
            assertThat(modified.stocks()).isEqualTo(original.stocks());
            assertThat(modified.metadata()).isEqualTo(original.metadata());
        }

        @Test
        void shouldOverrideSimulationSettings() {
            ModelDefinition original = buildFullDefinition();
            SimulationSettings newSettings = new SimulationSettings("Month", 12, "Month");
            ModelDefinition modified = original.toBuilder()
                    .defaultSimulation(newSettings)
                    .build();

            assertThat(modified.defaultSimulation()).isEqualTo(newSettings);
            assertThat(modified.name()).isEqualTo(original.name());
        }

        @Test
        void shouldOverrideMetadata() {
            ModelDefinition original = buildFullDefinition();
            ModelMetadata newMeta = new ModelMetadata("New Author", null, null, null);
            ModelDefinition modified = original.toBuilder()
                    .metadata(newMeta)
                    .build();

            assertThat(modified.metadata()).isEqualTo(newMeta);
            assertThat(modified.stocks()).isEqualTo(original.stocks());
        }

        @Test
        void shouldClearAndReplaceVariables() {
            ModelDefinition original = buildFullDefinition();
            VariableDef newVar = new VariableDef("gamma", "0.1", "1/Year");
            ModelDefinition modified = original.toBuilder()
                    .clearVariables()
                    .variable(newVar)
                    .build();

            assertThat(modified.variables()).containsExactly(newVar);
            assertThat(modified.stocks()).isEqualTo(original.stocks());
        }

        @Test
        void shouldClearAndReplaceViews() {
            ModelDefinition original = buildFullDefinition();
            ViewDef view = new ViewDef("Main", List.of(), List.of(), List.of());
            ModelDefinition modified = original.toBuilder()
                    .clearViews()
                    .view(view)
                    .build();

            assertThat(modified.views()).containsExactly(view);
        }

        @Test
        void shouldPreserveImmutability() {
            ModelDefinition original = buildFullDefinition();
            ModelDefinition copy = original.toBuilder().build();

            assertThatThrownBy(() -> copy.stocks().add(new StockDef("X", 0, "U")))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
