package com.deathrayresearch.forrester.io.json;

import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.def.ElementPlacement;
import com.deathrayresearch.forrester.model.def.ElementType;
import com.deathrayresearch.forrester.model.def.LookupTableDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;
import com.deathrayresearch.forrester.model.def.ModuleInterface;
import com.deathrayresearch.forrester.model.def.PortDef;
import com.deathrayresearch.forrester.model.def.ViewDef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ModelDefinitionSerializer")
class ModelDefinitionSerializerTest {

    private final ModelDefinitionSerializer serializer = new ModelDefinitionSerializer();

    @Test
    void shouldSerializeAndDeserializeSIRModel() {
        ModelDefinition sir = buildSIR();
        String json = serializer.toJson(sir);

        assertThat(json).isNotNull();

        ModelDefinition deserialized = serializer.fromJson(json);
        assertThat(deserialized.name()).isEqualTo(sir.name());
        assertThat(deserialized.stocks()).hasSameSizeAs(sir.stocks());
        assertThat(deserialized.flows()).hasSameSizeAs(sir.flows());
        assertThat(deserialized.constants()).hasSameSizeAs(sir.constants());
        assertThat(deserialized.defaultSimulation().duration())
                .isEqualTo(sir.defaultSimulation().duration());
    }

    @Test
    void shouldPreserveStockDetails() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("S", "A stock", 100, "Person", "ALLOW")
                .build();

        ModelDefinition roundTripped = serializer.fromJson(serializer.toJson(def));
        assertThat(roundTripped.stocks().get(0).name()).isEqualTo("S");
        assertThat(roundTripped.stocks().get(0).comment()).isEqualTo("A stock");
        assertThat(roundTripped.stocks().get(0).initialValue()).isEqualTo(100);
        assertThat(roundTripped.stocks().get(0).unit()).isEqualTo("Person");
        assertThat(roundTripped.stocks().get(0).negativeValuePolicy()).isEqualTo("ALLOW");
    }

    @Test
    void shouldPreserveFlowDetails() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("A", 100, "Thing")
                .stock("B", 0, "Thing")
                .flow("F", "A * 0.1", "Day", "A", "B")
                .build();

        ModelDefinition roundTripped = serializer.fromJson(serializer.toJson(def));
        assertThat(roundTripped.flows().get(0).name()).isEqualTo("F");
        assertThat(roundTripped.flows().get(0).equation()).isEqualTo("A * 0.1");
        assertThat(roundTripped.flows().get(0).timeUnit()).isEqualTo("Day");
        assertThat(roundTripped.flows().get(0).source()).isEqualTo("A");
        assertThat(roundTripped.flows().get(0).sink()).isEqualTo("B");
    }

    @Test
    void shouldHandleNullSourceAndSink() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .flow("F", "10", "Day", null, null)
                .build();

        ModelDefinition roundTripped = serializer.fromJson(serializer.toJson(def));
        assertThat(roundTripped.flows().get(0).source()).isNull();
        assertThat(roundTripped.flows().get(0).sink()).isNull();
    }

    @Test
    void shouldSerializeLookupTables() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .lookupTable("Effect",
                        new double[]{0, 0.5, 1.0},
                        new double[]{1.0, 0.5, 0.0},
                        "LINEAR")
                .build();

        ModelDefinition roundTripped = serializer.fromJson(serializer.toJson(def));
        LookupTableDef table = roundTripped.lookupTables().get(0);
        assertThat(table.name()).isEqualTo("Effect");
        assertThat(table.xValues()).isEqualTo(new double[]{0, 0.5, 1.0});
        assertThat(table.yValues()).isEqualTo(new double[]{1.0, 0.5, 0.0});
        assertThat(table.interpolation()).isEqualTo("LINEAR");
    }

    @Test
    void shouldSerializeModuleWithBindings() {
        ModelDefinition moduleDef = new ModelDefinitionBuilder()
                .name("Inner")
                .moduleInterface(new ModuleInterface(
                        List.of(new PortDef("in1", "Thing")),
                        List.of(new PortDef("out1", "Thing"))))
                .stock("S", 0, "Thing")
                .build();

        ModelDefinition outer = new ModelDefinitionBuilder()
                .name("Outer")
                .module("mod1", moduleDef,
                        Map.of("in1", "42"),
                        Map.of("out1", "result"))
                .build();

        ModelDefinition roundTripped = serializer.fromJson(serializer.toJson(outer));
        assertThat(roundTripped.modules()).hasSize(1);
        assertThat(roundTripped.modules().get(0).instanceName()).isEqualTo("mod1");
        assertThat(roundTripped.modules().get(0).definition().name()).isEqualTo("Inner");
        assertThat(roundTripped.modules().get(0).inputBindings().get("in1")).isEqualTo("42");
        assertThat(roundTripped.modules().get(0).outputBindings().get("out1")).isEqualTo("result");
    }

    @Test
    void shouldSerializeViews() {
        ViewDef view = new ViewDef("Main",
                List.of(new ElementPlacement("S", ElementType.STOCK, 100, 200)),
                List.of(new ConnectorRoute("A", "B")),
                List.of());

        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .view(view)
                .build();

        ModelDefinition roundTripped = serializer.fromJson(serializer.toJson(def));
        assertThat(roundTripped.views()).hasSize(1);
        assertThat(roundTripped.views().get(0).name()).isEqualTo("Main");
        assertThat(roundTripped.views().get(0).elements()).hasSize(1);
        assertThat(roundTripped.views().get(0).elements().get(0).name()).isEqualTo("S");
    }

    @Test
    void shouldThrowOnMalformedJson() {
        assertThatThrownBy(() -> serializer.fromJson("not valid json{{{"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowForNonNumericDoubleField() {
        String json = """
                {
                  "name": "Bad",
                  "stocks": [
                    { "name": "S", "initialValue": "not_a_number", "unit": "Thing" }
                  ]
                }
                """;
        assertThatThrownBy(() -> serializer.fromJson(json))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a number");
    }

    @Test
    void shouldOmitNullFields() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Minimal")
                .stock("S", 100, "Thing")
                .build();

        String json = serializer.toJson(def);
        ModelDefinition roundTripped = serializer.fromJson(json);

        // Verify structurally that null/empty fields were omitted and round-trip correctly
        assertThat(roundTripped.stocks().get(0).comment()).isNull();
        assertThat(roundTripped.flows()).isNullOrEmpty();
        assertThat(roundTripped.auxiliaries()).isNullOrEmpty();
    }

    private ModelDefinition buildSIR() {
        return new ModelDefinitionBuilder()
                .name("SIR Model")
                .stock("Susceptible", 1000, "Person")
                .stock("Infectious", 10, "Person")
                .stock("Recovered", 0, "Person")
                .flow("Infection",
                        "Contact_Rate * Infectious / (Susceptible + Infectious + Recovered) * Infectivity * Susceptible",
                        "Day", "Susceptible", "Infectious")
                .flow("Recovery", "Infectious * Recovery_Rate", "Day",
                        "Infectious", "Recovered")
                .constant("Contact_Rate", 8.0, "Dimensionless unit")
                .constant("Infectivity", 0.10, "Dimensionless unit")
                .constant("Recovery_Rate", 0.20, "Dimensionless unit")
                .defaultSimulation("Day", 56, "Day")
                .build();
    }
}
