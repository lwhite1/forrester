package com.deathrayresearch.forrester.io.xmile;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.io.ImportResult;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.compile.CompiledModel;
import com.deathrayresearch.forrester.model.compile.ModelCompiler;
import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.LookupTableDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.StockDef;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("XmileImporter")
class XmileImporterTest {

    private final XmileImporter importer = new XmileImporter();

    @Nested
    @DisplayName("Element parsing")
    class ElementParsing {

        @Test
        void shouldParseStockWithInitialValue() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <stock name="Population">
                          <eqn>100</eqn>
                        </stock>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            ModelDefinition def = result.definition();

            assertThat(def.stocks()).hasSize(1);
            StockDef stock = def.stocks().get(0);
            assertThat(stock.name()).isEqualTo("Population");
            assertThat(stock.initialValue()).isEqualTo(100.0);
        }

        @Test
        void shouldParseFlowWithEquation() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <stock name="Population">
                          <eqn>100</eqn>
                          <inflow>births</inflow>
                        </stock>
                        <flow name="births">
                          <eqn>Population * birth_rate</eqn>
                        </flow>
                        <aux name="birth_rate">
                          <eqn>0.04</eqn>
                        </aux>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            ModelDefinition def = result.definition();

            assertThat(def.flows()).hasSize(1);
            FlowDef flow = def.flows().get(0);
            assertThat(flow.name()).isEqualTo("births");
            assertThat(flow.equation()).isEqualTo("Population * birth_rate");
            assertThat(flow.sink()).isEqualTo("Population");
            assertThat(flow.source()).isNull();
        }

        @Test
        void shouldParseNumericAuxAsConstant() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <aux name="rate">
                          <eqn>0.05</eqn>
                          <units>1/Day</units>
                        </aux>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            assertThat(result.definition().constants()).hasSize(1);
            ConstantDef c = result.definition().constants().get(0);
            assertThat(c.name()).isEqualTo("rate");
            assertThat(c.value()).isEqualTo(0.05);
        }

        @Test
        void shouldParseExpressionAuxAsAuxiliary() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <aux name="rate">
                          <eqn>a + b</eqn>
                        </aux>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            assertThat(result.definition().auxiliaries()).hasSize(1);
            AuxDef aux = result.definition().auxiliaries().get(0);
            assertThat(aux.name()).isEqualTo("rate");
            assertThat(aux.equation()).isEqualTo("a + b");
        }

        @Test
        void shouldParseEmbeddedGraphicalFunction() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <aux name="effect">
                          <eqn>density</eqn>
                          <gf>
                            <xscale min="0" max="2"/>
                            <yscale min="0" max="1"/>
                            <ypts>1,0.9,0.7,0.3,0.1,0</ypts>
                          </gf>
                        </aux>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            assertThat(result.definition().lookupTables()).hasSize(1);
            LookupTableDef lt = result.definition().lookupTables().get(0);
            assertThat(lt.name()).isEqualTo("effect_lookup");
            assertThat(lt.xValues()).hasSize(6);
            assertThat(lt.yValues()).hasSize(6);
            assertThat(lt.yValues()[0]).isEqualTo(1.0);
            assertThat(lt.yValues()[5]).isEqualTo(0.0);

            // Should also create an aux that references the lookup
            assertThat(result.definition().auxiliaries()).hasSize(1);
            assertThat(result.definition().auxiliaries().get(0).equation())
                    .contains("LOOKUP(effect_lookup");
        }

        @Test
        void shouldParseGfWithExplicitXpts() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <aux name="curve">
                          <eqn>x_input</eqn>
                          <gf>
                            <xpts>0,1,2,3,4</xpts>
                            <ypts>0,0.5,1.0,0.5,0</ypts>
                          </gf>
                        </aux>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            assertThat(result.definition().lookupTables()).hasSize(1);
            LookupTableDef lt = result.definition().lookupTables().get(0);
            assertThat(lt.xValues()).containsExactly(0, 1, 2, 3, 4);
            assertThat(lt.yValues()).containsExactly(0, 0.5, 1.0, 0.5, 0);
        }
    }

    @Nested
    @DisplayName("Stock-flow linkage")
    class StockFlowLinkage {

        @Test
        void shouldLinkInflowToSink() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <stock name="Tank">
                          <eqn>0</eqn>
                          <inflow>fill_rate</inflow>
                        </stock>
                        <flow name="fill_rate">
                          <eqn>10</eqn>
                        </flow>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            FlowDef flow = result.definition().flows().get(0);
            assertThat(flow.sink()).isEqualTo("Tank");
            assertThat(flow.source()).isNull();
        }

        @Test
        void shouldLinkOutflowToSource() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <stock name="Tank">
                          <eqn>100</eqn>
                          <outflow>drain_rate</outflow>
                        </stock>
                        <flow name="drain_rate">
                          <eqn>5</eqn>
                        </flow>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            FlowDef flow = result.definition().flows().get(0);
            assertThat(flow.source()).isEqualTo("Tank");
            assertThat(flow.sink()).isNull();
        }

        @Test
        void shouldLinkFlowBetweenTwoStocks() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <stock name="Source">
                          <eqn>100</eqn>
                          <outflow>transfer</outflow>
                        </stock>
                        <stock name="Dest">
                          <eqn>0</eqn>
                          <inflow>transfer</inflow>
                        </stock>
                        <flow name="transfer">
                          <eqn>10</eqn>
                        </flow>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            FlowDef flow = result.definition().flows().get(0);
            assertThat(flow.source()).isEqualTo("Source");
            assertThat(flow.sink()).isEqualTo("Dest");
        }
    }

    @Nested
    @DisplayName("Simulation settings")
    class SimulationSettingsTests {

        @Test
        void shouldExtractSimulationSettings() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="month">
                        <start>0</start><stop>120</stop><dt>0.5</dt>
                      </sim_specs>
                      <model><variables>
                        <aux name="x"><eqn>1</eqn></aux>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            var sim = result.definition().defaultSimulation();
            assertThat(sim).isNotNull();
            assertThat(sim.timeStep()).isEqualTo("Month");
            assertThat(sim.duration()).isEqualTo(120.0);
            assertThat(sim.durationUnit()).isEqualTo("Month");
        }

        @Test
        void shouldUseModelNameFromHeader() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>My Custom Model</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <aux name="x"><eqn>1</eqn></aux>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Fallback");
            assertThat(result.definition().name()).isEqualTo("My Custom Model");
        }
    }

    @Nested
    @DisplayName("Teacup model integration test")
    class TeacupModel {

        @Test
        void shouldImportTeacupModel() throws IOException {
            Path path = Path.of("src/test/resources/xmile/teacup.xmile");
            ImportResult result = importer.importModel(path);
            ModelDefinition def = result.definition();

            assertThat(def.name()).isEqualTo("Teacup");

            // 1 stock: Teacup_Temperature
            assertThat(def.stocks()).hasSize(1);
            StockDef stock = def.stocks().get(0);
            assertThat(stock.name()).isEqualTo("Teacup_Temperature");
            assertThat(stock.initialValue()).isEqualTo(180.0);

            // 2 constants: Room_Temperature, Characteristic_Time
            assertThat(def.constants()).hasSize(2);
            Set<String> constantNames = def.constants().stream()
                    .map(ConstantDef::name)
                    .collect(Collectors.toSet());
            assertThat(constantNames).containsExactlyInAnyOrder(
                    "Room_Temperature", "Characteristic_Time");

            // 1 flow
            assertThat(def.flows()).hasSize(1);
            FlowDef flow = def.flows().get(0);
            assertThat(flow.name()).isEqualTo("Heat_Loss_to_Room");
            assertThat(flow.source()).isEqualTo("Teacup_Temperature");

            // Simulation settings
            assertThat(def.defaultSimulation()).isNotNull();
            assertThat(def.defaultSimulation().timeStep()).isEqualTo("Minute");
            assertThat(def.defaultSimulation().duration()).isEqualTo(30.0);

            // Views
            assertThat(def.views()).isNotEmpty();
            assertThat(def.views().get(0).elements()).isNotEmpty();
            assertThat(def.views().get(0).connectors()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("SIR model integration test")
    class SirModel {

        @Test
        void shouldImportSirModel() throws IOException {
            Path path = Path.of("src/test/resources/xmile/sir.xmile");
            ImportResult result = importer.importModel(path);
            ModelDefinition def = result.definition();

            assertThat(def.name()).isEqualTo("SIR");

            // 3 stocks
            assertThat(def.stocks()).hasSize(3);
            Set<String> stockNames = def.stocks().stream()
                    .map(StockDef::name)
                    .collect(Collectors.toSet());
            assertThat(stockNames).containsExactlyInAnyOrder(
                    "Susceptible", "Infected", "Recovered");

            // 3 constants
            assertThat(def.constants()).hasSize(3);
            Set<String> constantNames = def.constants().stream()
                    .map(ConstantDef::name)
                    .collect(Collectors.toSet());
            assertThat(constantNames).containsExactlyInAnyOrder(
                    "Contact_Rate", "Recovery_Time", "Total_Population");

            // 1 auxiliary: Infection_Rate
            assertThat(def.auxiliaries()).hasSize(1);

            // 2 flows
            assertThat(def.flows()).hasSize(2);

            // Simulation settings: 200 days
            assertThat(def.defaultSimulation()).isNotNull();
            assertThat(def.defaultSimulation().timeStep()).isEqualTo("Day");
            assertThat(def.defaultSimulation().duration()).isEqualTo(200.0);

            // Views
            assertThat(def.views()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Expression translation in import")
    class ExpressionTranslation {

        @Test
        void shouldTranslateIfThenElseInFlowEquation() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <stock name="S"><eqn>100</eqn><outflow>f</outflow></stock>
                        <flow name="f">
                          <eqn>IF_THEN_ELSE(S > 50, 10, 0)</eqn>
                        </flow>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            FlowDef flow = result.definition().flows().get(0);
            assertThat(flow.equation()).isEqualTo("IF(S > 50, 10, 0)");
        }
    }

    @Nested
    @DisplayName("Import → compile → simulate round-trip")
    class ImportCompileSimulate {

        @Test
        void shouldImportCompileAndSimulateTeacup() throws IOException {
            Path path = Path.of("src/test/resources/xmile/teacup.xmile");
            ImportResult result = importer.importModel(path);
            ModelDefinition def = result.definition();

            // Compile the imported definition
            CompiledModel compiled = new ModelCompiler().compile(def);
            assertThat(compiled).isNotNull();

            // Run the simulation
            Simulation sim = compiled.createSimulation();
            sim.execute();

            // The teacup should cool toward room temperature (70)
            // Starting at 180, after 30 minutes it should be significantly cooler
            Stock tempStock = compiled.getModel().getStocks().stream()
                    .filter(s -> s.getName().contains("Teacup"))
                    .findFirst()
                    .orElseThrow();

            double finalTemp = tempStock.getValue();
            assertThat(finalTemp).isLessThan(180.0);
            assertThat(finalTemp).isGreaterThan(70.0);
        }
    }
}
