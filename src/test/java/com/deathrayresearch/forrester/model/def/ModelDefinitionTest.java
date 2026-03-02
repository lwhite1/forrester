package com.deathrayresearch.forrester.model.def;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ModelDefinition records")
class ModelDefinitionTest {

    @Test
    void shouldCreateMinimalDefinition() {
        ModelDefinition def = new ModelDefinition(
                "Test", null, null,
                null, null, null, null, null, null, null, null, null);
        assertEquals("Test", def.name());
        assertTrue(def.stocks().isEmpty());
        assertTrue(def.flows().isEmpty());
    }

    @Test
    void shouldRejectBlankName() {
        assertThrows(IllegalArgumentException.class, () ->
                new ModelDefinition("", null, null,
                        null, null, null, null, null, null, null, null, null));
    }

    @Test
    void shouldReturnImmutableLists() {
        ModelDefinition def = new ModelDefinition(
                "Test", null, null,
                List.of(new StockDef("S", 100, "Person")),
                null, null, null, null, null, null, null, null);
        assertThrows(UnsupportedOperationException.class, () ->
                def.stocks().add(new StockDef("S2", 50, "Person")));
    }

    @Test
    void shouldSupportRecordEquality() {
        StockDef s1 = new StockDef("S", 100, "Person");
        StockDef s2 = new StockDef("S", 100, "Person");
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    void shouldCreateFlowDef() {
        FlowDef flow = new FlowDef("Infection", "rate * S", "Day", "Susceptible", "Infectious");
        assertEquals("Infection", flow.name());
        assertEquals("rate * S", flow.equation());
        assertEquals("Day", flow.timeUnit());
        assertEquals("Susceptible", flow.source());
        assertEquals("Infectious", flow.sink());
    }

    @Test
    void shouldCreateConstantDef() {
        ConstantDef c = new ConstantDef("Rate", 0.5, "Dimensionless");
        assertEquals("Rate", c.name());
        assertEquals(0.5, c.value());
    }

    @Test
    void shouldCreateAuxDef() {
        AuxDef aux = new AuxDef("Infection Rate", "Contact_Rate * Infectivity", "Person");
        assertEquals("Infection Rate", aux.name());
        assertEquals("Contact_Rate * Infectivity", aux.equation());
    }

    @Test
    void shouldCloneLookupTableArrays() {
        double[] x = {0, 1, 2};
        double[] y = {0, 0.5, 1};
        LookupTableDef table = new LookupTableDef("Effect", x, y, "LINEAR");
        x[0] = 999; // mutate original
        assertEquals(0, table.xValues()[0], "Array should be cloned");
    }

    @Test
    void shouldCreateModuleInterface() {
        ModuleInterface iface = new ModuleInterface(
                List.of(new PortDef("in1", "Person")),
                List.of(new PortDef("out1", "Person")));
        assertEquals(1, iface.inputs().size());
        assertEquals(1, iface.outputs().size());
    }
}
