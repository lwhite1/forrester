package systems.courant.sd.io.json;

import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.CldVariableDef;
import systems.courant.sd.model.def.ConnectorRoute;
import systems.courant.sd.model.def.ElementPlacement;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.LookupTableDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;
import systems.courant.sd.model.def.ModuleInterface;
import systems.courant.sd.model.def.ReferenceDataset;
import systems.courant.sd.model.def.PortDef;
import systems.courant.sd.model.def.ViewDef;
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
        assertThat(deserialized.parameters()).hasSameSizeAs(sir.parameters());
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
    void shouldPreserveFlowMaterialUnit() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("Pop", 100, "Person")
                .flow("Births", "Pop * 0.1", "Day", "Person", null, "Pop")
                .build();

        String json = serializer.toJson(def);
        assertThat(json).contains("materialUnit");
        assertThat(json).contains("Person");

        ModelDefinition roundTripped = serializer.fromJson(json);
        assertThat(roundTripped.flows().get(0).materialUnit()).isEqualTo("Person");
    }

    @Test
    void shouldHandleNullMaterialUnit() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .flow("F", "10", "Day", null, null)
                .build();

        ModelDefinition roundTripped = serializer.fromJson(serializer.toJson(def));
        assertThat(roundTripped.flows().get(0).materialUnit()).isNull();
    }

    @Test
    void shouldDeserializeOldJsonWithoutMaterialUnit() {
        // Simulate old-format JSON that doesn't have materialUnit field
        String oldJson = """
                {
                  "name": "Legacy",
                  "flows": [{
                    "name": "F",
                    "equation": "10",
                    "timeUnit": "Day"
                  }]
                }
                """;
        ModelDefinition def = serializer.fromJson(oldJson);
        assertThat(def.flows().get(0).materialUnit()).isNull();
        assertThat(def.flows().get(0).name()).isEqualTo("F");
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
        assertThat(roundTripped.variables()).isNullOrEmpty();
    }

    @Test
    void shouldRoundTripCldVariables() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("CLD Test")
                .cldVariable("Workload", "Hours per week")
                .cldVariable("Burnout")
                .causalLink("Workload", "Burnout", CausalLinkDef.Polarity.POSITIVE)
                .causalLink("Burnout", "Workload", CausalLinkDef.Polarity.NEGATIVE)
                .build();

        ModelDefinition roundTripped = serializer.fromJson(serializer.toJson(def));

        assertThat(roundTripped.cldVariables()).hasSize(2);
        assertThat(roundTripped.cldVariables().get(0).name()).isEqualTo("Workload");
        assertThat(roundTripped.cldVariables().get(0).comment()).isEqualTo("Hours per week");
        assertThat(roundTripped.cldVariables().get(1).name()).isEqualTo("Burnout");
        assertThat(roundTripped.cldVariables().get(1).comment()).isNull();

        assertThat(roundTripped.causalLinks()).hasSize(2);
        assertThat(roundTripped.causalLinks().get(0).from()).isEqualTo("Workload");
        assertThat(roundTripped.causalLinks().get(0).to()).isEqualTo("Burnout");
        assertThat(roundTripped.causalLinks().get(0).polarity()).isEqualTo(CausalLinkDef.Polarity.POSITIVE);
        assertThat(roundTripped.causalLinks().get(1).polarity()).isEqualTo(CausalLinkDef.Polarity.NEGATIVE);
    }

    @Test
    void shouldDeserializeModelWithoutCldFields() {
        // Old JSON without cldVariables or causalLinks should deserialize with empty lists
        String json = """
                {
                  "name": "Legacy",
                  "stocks": [
                    { "name": "S", "initialValue": 10, "unit": "u" }
                  ]
                }
                """;
        ModelDefinition def = serializer.fromJson(json);
        assertThat(def.cldVariables()).isEmpty();
        assertThat(def.causalLinks()).isEmpty();
    }

    @Test
    void shouldRoundTripCausalLinkWithComment() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .cldVariable("A")
                .cldVariable("B")
                .causalLink(new CausalLinkDef("A", "B",
                        CausalLinkDef.Polarity.POSITIVE, "after a delay"))
                .build();

        ModelDefinition roundTripped = serializer.fromJson(serializer.toJson(def));
        assertThat(roundTripped.causalLinks().get(0).comment()).isEqualTo("after a delay");
    }

    @Test
    void shouldRoundTripMixedSfAndCldModel() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Mixed")
                .stock("Population", 1000, "Person")
                .cldVariable("Quality of Life")
                .causalLink("Population", "Quality of Life", CausalLinkDef.Polarity.NEGATIVE)
                .build();

        ModelDefinition roundTripped = serializer.fromJson(serializer.toJson(def));
        assertThat(roundTripped.stocks()).hasSize(1);
        assertThat(roundTripped.cldVariables()).hasSize(1);
        assertThat(roundTripped.causalLinks()).hasSize(1);
    }

    @Test
    void shouldDefaultInvalidPolarityToUnknown() {
        String json = """
                {
                  "name": "Bad Polarity",
                  "cldVariables": [
                    { "name": "A" },
                    { "name": "B" }
                  ],
                  "causalLinks": [
                    { "from": "A", "to": "B", "polarity": "STRONG" }
                  ]
                }
                """;
        ModelDefinition def = serializer.fromJson(json);
        assertThat(def.causalLinks()).hasSize(1);
        assertThat(def.causalLinks().get(0).polarity()).isEqualTo(CausalLinkDef.Polarity.UNKNOWN);
    }

    @Test
    void shouldRoundTripReferenceDatasets() {
        ReferenceDataset refData = new ReferenceDataset(
                "Historical",
                new double[]{0, 1, 2, 3},
                Map.of("Population", new double[]{100, 110, 115, 118},
                        "Revenue", new double[]{50, 55, 60, 63})
        );
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("With Reference")
                .stock("Population", 100, "Person")
                .referenceDataset(refData)
                .build();

        String json = serializer.toJson(def);
        assertThat(json).contains("referenceDatasets");
        assertThat(json).contains("Historical");

        ModelDefinition roundTripped = serializer.fromJson(json);
        assertThat(roundTripped.referenceDatasets()).hasSize(1);
        ReferenceDataset rt = roundTripped.referenceDatasets().getFirst();
        assertThat(rt.name()).isEqualTo("Historical");
        assertThat(rt.timeValues()).containsExactly(0, 1, 2, 3);
        assertThat(rt.columns().get("Population")).containsExactly(100, 110, 115, 118);
        assertThat(rt.columns().get("Revenue")).containsExactly(50, 55, 60, 63);
    }

    @Test
    void shouldRoundTripComments() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Comment Test")
                .comment("Comment 1", "This is a note")
                .comment("Comment 2", "Another note")
                .build();

        ModelDefinition roundTripped = serializer.fromJson(serializer.toJson(def));

        assertThat(roundTripped.comments()).hasSize(2);
        assertThat(roundTripped.comments().get(0).name()).isEqualTo("Comment 1");
        assertThat(roundTripped.comments().get(0).text()).isEqualTo("This is a note");
        assertThat(roundTripped.comments().get(1).name()).isEqualTo("Comment 2");
        assertThat(roundTripped.comments().get(1).text()).isEqualTo("Another note");
    }

    @Test
    void shouldDeserializeModelWithoutComments() {
        String json = """
                {
                  "name": "Old Model",
                  "stocks": [{ "name": "S", "initialValue": 100, "unit": "x" }]
                }
                """;
        ModelDefinition def = serializer.fromJson(json);
        assertThat(def.comments()).isEmpty();
    }

    @Test
    void shouldDeserializeModelWithoutReferenceDatasets() {
        String json = """
                {
                  "name": "Old Model",
                  "stocks": [{ "name": "S", "initialValue": 100, "unit": "x" }]
                }
                """;
        ModelDefinition def = serializer.fromJson(json);
        assertThat(def.referenceDatasets()).isEmpty();
    }

    @Test
    @DisplayName("should round-trip subscript definitions")
    void shouldRoundTripSubscripts() {
        ModelDefinition original = new ModelDefinitionBuilder()
                .name("Subscripted")
                .subscript("Region", List.of("North", "South", "East"))
                .subscript("Age", List.of("Young", "Old"))
                .stock("Pop", 100, "people", List.of("Region", "Age"))
                .build();

        String json = serializer.toJson(original);
        ModelDefinition restored = serializer.fromJson(json);

        assertThat(restored.subscripts()).hasSize(2);
        assertThat(restored.subscripts().get(0).name()).isEqualTo("Region");
        assertThat(restored.subscripts().get(0).labels()).containsExactly("North", "South", "East");
        assertThat(restored.subscripts().get(1).name()).isEqualTo("Age");
        assertThat(restored.subscripts().get(1).labels()).containsExactly("Young", "Old");
        assertThat(restored.stocks().get(0).subscripts()).containsExactly("Region", "Age");
    }

    @Test
    @DisplayName("should deserialize model without subscripts field")
    void shouldDeserializeModelWithoutSubscriptsField() {
        String json = """
                {
                  "name": "Legacy",
                  "stocks": [{"name": "S", "initialValue": 0, "unit": "units"}]
                }
                """;
        ModelDefinition def = serializer.fromJson(json);
        assertThat(def.subscripts()).isEmpty();
    }

    @Test
    @DisplayName("should serialize variables under 'variables' JSON field and round-trip")
    void shouldSerializeVariablesFieldName() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Variable Test")
                .variable("Rate", "Stock_1 * 0.1", "1/Day")
                .constant("k", 0.5, "Dimensionless unit")
                .build();

        String json = serializer.toJson(def);

        // JSON must use "variables" (not the old "auxiliaries")
        assertThat(json).contains("\"variables\"");
        assertThat(json).doesNotContain("\"auxiliaries\"");

        ModelDefinition roundTripped = serializer.fromJson(json);
        assertThat(roundTripped.variables()).hasSize(2);
        assertThat(roundTripped.variables().get(0).name()).isEqualTo("Rate");
        assertThat(roundTripped.variables().get(0).equation()).isEqualTo("Stock_1 * 0.1");
        assertThat(roundTripped.variables().get(1).name()).isEqualTo("k");
        assertThat(roundTripped.variables().get(1).isLiteral()).isTrue();
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
