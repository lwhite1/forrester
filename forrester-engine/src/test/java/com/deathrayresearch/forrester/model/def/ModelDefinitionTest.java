package com.deathrayresearch.forrester.model.def;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
                null, null, null, null, null, null, null, null, null);
        assertThat(def.name()).isEqualTo("Test");
        assertThat(def.stocks()).isEmpty();
        assertThat(def.flows()).isEmpty();
    }

    @Test
    void shouldRejectBlankName() {
        assertThatThrownBy(() ->
                new ModelDefinition("", null, null,
                        null, null, null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldReturnImmutableLists() {
        ModelDefinition def = new ModelDefinition(
                "Test", null, null,
                List.of(new StockDef("S", 100, "Person")),
                null, null, null, null, null, null, null, null);
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
    void shouldCreateConstantDef() {
        ConstantDef c = new ConstantDef("Rate", 0.5, "Dimensionless");
        assertThat(c.name()).isEqualTo("Rate");
        assertThat(c.value()).isEqualTo(0.5);
    }

    @Test
    void shouldCreateAuxDef() {
        AuxDef aux = new AuxDef("Infection Rate", "Contact_Rate * Infectivity", "Person");
        assertThat(aux.name()).isEqualTo("Infection Rate");
        assertThat(aux.equation()).isEqualTo("Contact_Rate * Infectivity");
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
}
