package systems.courant.sd.io.xmile;

import systems.courant.sd.io.ImportResult;
import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.LookupTableDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;
import systems.courant.sd.model.def.ModuleInstanceDef;
import systems.courant.sd.model.def.StockDef;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("XmileExporter")
class XmileExporterTest {

    @Nested
    @DisplayName("Basic export")
    class BasicExport {

        @Test
        void shouldExportMinimalModel() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .constant("rate", 0.5, "1/Day")
                    .build();

            String xml = XmileExporter.toXmile(def);
            assertThat(xml).contains("xmlns=\"http://docs.oasis-open.org/xmile/ns/XMILE/v1.0\"");
            assertThat(xml).contains("<name>Test</name>");
            assertThat(xml).contains("time_units=\"day\"");
            assertThat(xml).contains("<stop>100</stop>");
        }

        @Test
        void shouldExportStockWithInflowOutflow() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .stock("Tank", 50.0, "Gallons")
                    .flow(new FlowDef("fill", "10", "Day", null, "Tank"))
                    .flow(new FlowDef("drain", "5", "Day", "Tank", null))
                    .build();

            String xml = XmileExporter.toXmile(def);
            assertThat(xml).contains("<stock");
            assertThat(xml).contains("name=\"Tank\"");
            assertThat(xml).contains("<inflow>fill</inflow>");
            assertThat(xml).contains("<outflow>drain</outflow>");
            assertThat(xml).contains("<eqn>50</eqn>");
        }

        @Test
        void shouldExportFlowWithTranslatedExpression() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .stock("S", 100.0, null)
                    .flow(new FlowDef("f", "IF(S > 50, 10, 0)", "Day", "S", null))
                    .build();

            String xml = XmileExporter.toXmile(def);
            assertThat(xml).contains("IF_THEN_ELSE");
        }

        @Test
        void shouldExportConstantAsAux() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .constant("rate", 0.5, "1/Day")
                    .build();

            String xml = XmileExporter.toXmile(def);
            assertThat(xml).contains("<aux");
            assertThat(xml).contains("name=\"rate\"");
            assertThat(xml).contains("<eqn>0.5</eqn>");
        }

        @Test
        void shouldExportStockInitialExpression() {
            StockDef stock = new StockDef("Water", null, 0.0,
                    "Capacity * 0.5", "Gallons", null, java.util.List.of());
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .stock(stock)
                    .constant("Capacity", 200, "Gallons")
                    .build();

            String xml = XmileExporter.toXmile(def);
            assertThat(xml).contains("Capacity");
            assertThat(xml).contains("0.5");
            // Must NOT fall back to the numeric initialValue of 0
            assertThat(xml).doesNotContain("<eqn>0</eqn>");
        }

        @Test
        void shouldFallBackToNumericWhenNoInitialExpression() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .stock("Tank", 42.0, "Gallons")
                    .build();

            String xml = XmileExporter.toXmile(def);
            assertThat(xml).contains("<eqn>42</eqn>");
        }

        @Test
        void shouldAlwaysWriteEqnForAuxWithLookup() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 10, "Day")
                    .lookupTable("effect",
                            new double[]{0, 1, 2}, new double[]{0, 0.5, 1.0}, "LINEAR")
                    .variable("result", "LOOKUP(effect, Time)", null)
                    .build();

            String xml = XmileExporter.toXmile(def);
            assertThat(xml).contains("<gf>");
            assertThat(xml).contains("<eqn>");
        }

        @Test
        void shouldExportCorrectYScaleForLookupTable() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .lookupTable("effect",
                            new double[]{0, 1, 2, 3},
                            new double[]{5.0, 10.0, 3.0, 8.0},
                            "LINEAR")
                    .build();

            String xml = XmileExporter.toXmile(def);
            assertThat(xml).contains("<gf>");
            // yscale min should be 3, max should be 10
            assertThat(xml).contains("min=\"3\"");
            assertThat(xml).contains("max=\"10\"");
        }

        @Test
        void shouldExportLookupTable() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .lookupTable("effect",
                            new double[]{0, 1, 2, 3, 4},
                            new double[]{0, 0.5, 1.0, 0.5, 0},
                            "LINEAR")
                    .build();

            String xml = XmileExporter.toXmile(def);
            assertThat(xml).contains("<gf>");
            assertThat(xml).contains("<xpts>0,1,2,3,4</xpts>");
            assertThat(xml).contains("<ypts>0,0.5,1,0.5,0</ypts>");
        }
    }

    @Nested
    @DisplayName("Export → import round-trip")
    class RoundTrip {

        @Test
        void shouldRoundTripStocksAndFlows() {
            ModelDefinition original = new ModelDefinitionBuilder()
                    .name("RoundTrip")
                    .defaultSimulation("Day", 50, "Day")
                    .stock("Population", 1000.0, "People")
                    .flow(new FlowDef("births", "Population * rate", "Day", null, "Population"))
                    .constant("rate", 0.02, "1/Day")
                    .build();

            String xml = XmileExporter.toXmile(original);
            ImportResult result = new XmileImporter().importModel(xml, "RoundTrip");
            ModelDefinition imported = result.definition();

            assertThat(imported.name()).isEqualTo("RoundTrip");
            assertThat(imported.stocks()).hasSize(1);
            assertThat(imported.stocks().get(0).name()).isEqualTo("Population");
            assertThat(imported.stocks().get(0).initialValue()).isEqualTo(1000.0);

            assertThat(imported.flows()).hasSize(1);
            assertThat(imported.flows().get(0).name()).isEqualTo("births");
            assertThat(imported.flows().get(0).sink()).isEqualTo("Population");

            assertThat(imported.parameters()).hasSize(1);
            assertThat(imported.parameters().get(0).name()).isEqualTo("rate");
            assertThat(imported.parameters().get(0).literalValue()).isEqualTo(0.02);
        }

        @Test
        void shouldRoundTripStockWithInitialExpression() {
            StockDef stock = new StockDef("Water", null, 0.0,
                    "Capacity * 0.5", "Gallons", null, java.util.List.of());
            ModelDefinition original = new ModelDefinitionBuilder()
                    .name("ExprInit")
                    .defaultSimulation("Day", 50, "Day")
                    .stock(stock)
                    .constant("Capacity", 200, "Gallons")
                    .build();

            String xml = XmileExporter.toXmile(original);
            ImportResult result = new XmileImporter().importModel(xml, "ExprInit");
            ModelDefinition imported = result.definition();

            assertThat(imported.stocks()).hasSize(1);
            StockDef importedStock = imported.stocks().get(0);
            assertThat(importedStock.name()).isEqualTo("Water");
            // The expression should survive the round-trip (either as initialExpression
            // or parsed back into the equation)
            assertThat(xml).contains("Capacity");
        }

        @Test
        void shouldRoundTripSimulationSettings() {
            ModelDefinition original = new ModelDefinitionBuilder()
                    .name("SimTest")
                    .defaultSimulation("Month", 240, "Month")
                    .constant("x", 1, null)
                    .build();

            String xml = XmileExporter.toXmile(original);
            ImportResult result = new XmileImporter().importModel(xml, "SimTest");

            var sim = result.definition().defaultSimulation();
            assertThat(sim.timeStep()).isEqualTo("Month");
            assertThat(sim.duration()).isEqualTo(240.0);
        }

        @Test
        void shouldRoundTripLookupTable() {
            double[] xVals = {0, 1, 2, 3, 4};
            double[] yVals = {0, 0.25, 1.0, 0.25, 0};

            ModelDefinition original = new ModelDefinitionBuilder()
                    .name("LookupTest")
                    .defaultSimulation("Day", 10, "Day")
                    .lookupTable("my_table", xVals, yVals, "LINEAR")
                    .build();

            String xml = XmileExporter.toXmile(original);
            ImportResult result = new XmileImporter().importModel(xml, "LookupTest");

            // Standalone lookups get exported as <aux> with <gf>, imported back as aux + lookup
            // The lookup data should be preserved
            ModelDefinition imported = result.definition();
            assertThat(imported.lookupTables()).isNotEmpty();
        }

        @Test
        void shouldRoundTripMultipleStocks() {
            ModelDefinition original = new ModelDefinitionBuilder()
                    .name("SIR")
                    .defaultSimulation("Day", 200, "Day")
                    .stock("Susceptible", 9999.0, "People")
                    .stock("Infected", 1.0, "People")
                    .stock("Recovered", 0.0, "People")
                    .flow(new FlowDef("infection", "Susceptible * Infected * rate / N",
                            "Day", "Susceptible", "Infected"))
                    .flow(new FlowDef("recovery", "Infected / recovery_time",
                            "Day", "Infected", "Recovered"))
                    .constant("rate", 0.3, "1/Day")
                    .constant("recovery_time", 5.0, "Day")
                    .constant("N", 10000.0, "People")
                    .build();

            String xml = XmileExporter.toXmile(original);
            ImportResult result = new XmileImporter().importModel(xml, "SIR");
            ModelDefinition imported = result.definition();

            assertThat(imported.stocks()).hasSize(3);
            Set<String> stockNames = imported.stocks().stream()
                    .map(StockDef::name).collect(Collectors.toSet());
            assertThat(stockNames).containsExactlyInAnyOrder(
                    "Susceptible", "Infected", "Recovered");

            assertThat(imported.flows()).hasSize(2);
            assertThat(imported.parameters()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Module export")
    class ModuleExport {

        @Test
        void shouldExportModuleAsNamedModel() {
            ModelDefinition inner = new ModelDefinitionBuilder()
                    .name("InnerModule")
                    .stock("Tank", 100, "Thing")
                    .flow(new FlowDef("drain", "Tank * rate", "Day", "Tank", null))
                    .build();

            ModelDefinition outer = new ModelDefinitionBuilder()
                    .name("Outer")
                    .defaultSimulation("Day", 10, "Day")
                    .constant("MyRate", 0.1, "1/Day")
                    .module(new ModuleInstanceDef("InnerModule", inner,
                            Map.of("rate", "MyRate"),
                            Map.of("drain", "drain_output")))
                    .build();

            String xml = XmileExporter.toXmile(outer);
            assertThat(xml).contains("name=\"InnerModule\"");
            assertThat(xml).contains("<module");
            assertThat(xml).contains("<connect");
        }

        @Test
        void shouldThrowWhenModuleNestingExceedsDepthLimit() {
            // Build a chain of 52 nested modules (exceeds limit of 50)
            ModelDefinition current = new ModelDefinitionBuilder()
                    .name("Leaf")
                    .constant("x", 1, null)
                    .build();
            for (int i = 51; i >= 0; i--) {
                current = new ModelDefinitionBuilder()
                        .name("Level_" + i)
                        .defaultSimulation("Day", 10, "Day")
                        .module(new ModuleInstanceDef("mod_" + (i + 1), current,
                                Map.of(), Map.of()))
                        .build();
            }

            ModelDefinition root = current;
            assertThatThrownBy(() -> XmileExporter.toXmile(root))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Module nesting depth exceeds maximum of 50");
        }

        @Test
        void shouldRoundTripModule() {
            ModelDefinition inner = new ModelDefinitionBuilder()
                    .name("Counter")
                    .stock("Count", 0, "Thing")
                    .flow(new FlowDef("inc", "step_size", "Day", null, "Count"))
                    .build();

            ModelDefinition original = new ModelDefinitionBuilder()
                    .name("Host")
                    .defaultSimulation("Day", 10, "Day")
                    .constant("Size", 5, "Thing")
                    .module(new ModuleInstanceDef("Counter", inner,
                            Map.of("step_size", "Size"),
                            Map.of()))
                    .build();

            String xml = XmileExporter.toXmile(original);
            ImportResult result = new XmileImporter().importModel(xml, "Host");
            ModelDefinition imported = result.definition();

            assertThat(imported.modules()).hasSize(1);
            ModuleInstanceDef mod = imported.modules().get(0);
            assertThat(mod.instanceName()).isEqualTo("Counter");
            assertThat(mod.inputBindings()).containsEntry("step_size", "Size");
            assertThat(mod.definition().stocks()).hasSize(1);
            assertThat(mod.definition().stocks().get(0).name()).isEqualTo("Count");
        }
    }

    @Nested
    @DisplayName("Lookup extraction helpers")
    class LookupHelpers {

        @Test
        void shouldExtractLookupReference() {
            assertThat(XmileExporter.extractLookupReference("LOOKUP(my_table, x + 1)"))
                    .hasValue("my_table");
        }

        @Test
        void shouldExtractLookupInput() {
            assertThat(XmileExporter.extractLookupInput("LOOKUP(my_table, x + 1)"))
                    .hasValue("x + 1");
        }

        @Test
        void shouldReturnEmptyForNonLookup() {
            assertThat(XmileExporter.extractLookupReference("a + b")).isEmpty();
            assertThat(XmileExporter.extractLookupInput("a + b")).isEmpty();
        }

        @Test
        void shouldExtractInputWithTrailingExpression() {
            // Issue #631: lastIndexOf(')') would grab ') + foo(y' instead of just 'x'
            assertThat(XmileExporter.extractLookupInput("LOOKUP(name, x) + foo(y)"))
                    .hasValue("x");
        }

        @Test
        void shouldExtractInputWithNestedParens() {
            assertThat(XmileExporter.extractLookupInput("LOOKUP(my_table, max(a, b))"))
                    .hasValue("max(a, b)");
        }

        @Test
        void shouldReturnEmptyForUnmatchedParen() {
            assertThat(XmileExporter.extractLookupInput("LOOKUP(name, x")).isEmpty();
        }
    }
}
