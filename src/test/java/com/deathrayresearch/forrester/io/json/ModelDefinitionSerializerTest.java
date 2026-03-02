package com.deathrayresearch.forrester.io.json;

import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.def.ElementPlacement;
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

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ModelDefinitionSerializer")
class ModelDefinitionSerializerTest {

    private final ModelDefinitionSerializer serializer = new ModelDefinitionSerializer();

    @Test
    void shouldSerializeAndDeserializeSIRModel() {
        ModelDefinition sir = buildSIR();
        String json = serializer.toJson(sir);

        assertNotNull(json);
        assertTrue(json.contains("SIR Model"));
        assertTrue(json.contains("Susceptible"));
        assertTrue(json.contains("Infection"));
        assertTrue(json.contains("Contact_Rate"));

        ModelDefinition deserialized = serializer.fromJson(json);
        assertEquals(sir.name(), deserialized.name());
        assertEquals(sir.stocks().size(), deserialized.stocks().size());
        assertEquals(sir.flows().size(), deserialized.flows().size());
        assertEquals(sir.constants().size(), deserialized.constants().size());
        assertEquals(sir.defaultSimulation().duration(),
                deserialized.defaultSimulation().duration());
    }

    @Test
    void shouldPreserveStockDetails() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("S", "A stock", 100, "Person", "ALLOW")
                .build();

        ModelDefinition roundTripped = serializer.fromJson(serializer.toJson(def));
        assertEquals("S", roundTripped.stocks().get(0).name());
        assertEquals("A stock", roundTripped.stocks().get(0).comment());
        assertEquals(100, roundTripped.stocks().get(0).initialValue());
        assertEquals("Person", roundTripped.stocks().get(0).unit());
        assertEquals("ALLOW", roundTripped.stocks().get(0).negativeValuePolicy());
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
        assertEquals("F", roundTripped.flows().get(0).name());
        assertEquals("A * 0.1", roundTripped.flows().get(0).equation());
        assertEquals("Day", roundTripped.flows().get(0).timeUnit());
        assertEquals("A", roundTripped.flows().get(0).source());
        assertEquals("B", roundTripped.flows().get(0).sink());
    }

    @Test
    void shouldHandleNullSourceAndSink() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .flow("F", "10", "Day", null, null)
                .build();

        ModelDefinition roundTripped = serializer.fromJson(serializer.toJson(def));
        assertNull(roundTripped.flows().get(0).source());
        assertNull(roundTripped.flows().get(0).sink());
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
        assertEquals("Effect", table.name());
        assertArrayEquals(new double[]{0, 0.5, 1.0}, table.xValues());
        assertArrayEquals(new double[]{1.0, 0.5, 0.0}, table.yValues());
        assertEquals("LINEAR", table.interpolation());
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
        assertEquals(1, roundTripped.modules().size());
        assertEquals("mod1", roundTripped.modules().get(0).instanceName());
        assertEquals("Inner", roundTripped.modules().get(0).definition().name());
        assertEquals("42", roundTripped.modules().get(0).inputBindings().get("in1"));
        assertEquals("result", roundTripped.modules().get(0).outputBindings().get("out1"));
    }

    @Test
    void shouldSerializeViews() {
        ViewDef view = new ViewDef("Main",
                List.of(new ElementPlacement("S", "stock", 100, 200)),
                List.of(new ConnectorRoute("A", "B")),
                List.of());

        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .view(view)
                .build();

        ModelDefinition roundTripped = serializer.fromJson(serializer.toJson(def));
        assertEquals(1, roundTripped.views().size());
        assertEquals("Main", roundTripped.views().get(0).name());
        assertEquals(1, roundTripped.views().get(0).elements().size());
        assertEquals("S", roundTripped.views().get(0).elements().get(0).name());
    }

    @Test
    void shouldThrowOnMalformedJson() {
        assertThrows(RuntimeException.class,
                () -> serializer.fromJson("not valid json{{{"));
    }

    @Test
    void shouldOmitNullFields() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Minimal")
                .stock("S", 100, "Thing")
                .build();

        String json = serializer.toJson(def);
        assertFalse(json.contains("\"comment\""));
        assertFalse(json.contains("\"flows\""));
        assertFalse(json.contains("\"auxiliaries\""));
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
