package systems.courant.sd.io.xmile;

import systems.courant.sd.Simulation;
import systems.courant.sd.io.ImportResult;
import systems.courant.sd.model.Stock;
import systems.courant.sd.model.compile.CompiledModel;
import systems.courant.sd.model.compile.ModelCompiler;
import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.LookupTableDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModuleInstanceDef;
import systems.courant.sd.model.def.StockDef;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
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
        void shouldParseStockWithNonLiteralInitialExpression() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <stock name="Inventory">
                          <eqn>capacity * 0.8</eqn>
                        </stock>
                        <aux name="capacity"><eqn>500</eqn></aux>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            ModelDefinition def = result.definition();

            assertThat(def.stocks()).hasSize(1);
            StockDef stock = def.stocks().get(0);
            assertThat(stock.name()).isEqualTo("Inventory");
            assertThat(stock.initialExpression()).isEqualTo("capacity * 0.8");
            assertThat(result.warnings()).noneMatch(w -> w.contains("Non-literal"));
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
            assertThat(result.definition().parameters()).hasSize(1);
            VariableDef c = result.definition().parameters().get(0);
            assertThat(c.name()).isEqualTo("rate");
            assertThat(c.literalValue()).isEqualTo(0.05);
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
            assertThat(result.definition().variables()).hasSize(1);
            VariableDef v = result.definition().variables().get(0);
            assertThat(v.name()).isEqualTo("rate");
            assertThat(v.equation()).isEqualTo("a + b");
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

            // Should also create a variable that references the lookup
            assertThat(result.definition().variables()).hasSize(1);
            assertThat(result.definition().variables().get(0).equation())
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
            assertThat(sim.dt()).isEqualTo(0.5);
        }

        @Test
        void shouldPreserveNonZeroInitialTime() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="year">
                        <start>1900</start><stop>2000</stop><dt>1</dt>
                      </sim_specs>
                      <model><variables>
                        <aux name="x"><eqn>1</eqn></aux>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            var sim = result.definition().defaultSimulation();
            assertThat(sim.initialTime()).isEqualTo(1900.0);
            assertThat(sim.duration()).isEqualTo(100.0);
        }

        @Test
        void shouldDefaultInitialTimeToZero() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day">
                        <start>0</start><stop>10</stop><dt>1</dt>
                      </sim_specs>
                      <model><variables>
                        <aux name="x"><eqn>1</eqn></aux>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            var sim = result.definition().defaultSimulation();
            assertThat(sim.initialTime()).isEqualTo(0.0);
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
            assertThat(def.parameters()).hasSize(2);
            Set<String> constantNames = def.parameters().stream()
                    .map(VariableDef::name)
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
            assertThat(def.parameters()).hasSize(3);
            Set<String> constantNames = def.parameters().stream()
                    .map(VariableDef::name)
                    .collect(Collectors.toSet());
            assertThat(constantNames).containsExactlyInAnyOrder(
                    "Contact_Rate", "Recovery_Time", "Total_Population");

            // 1 formula variable: Infection_Rate (3 constants are also in variables)
            assertThat(def.variables()).hasSize(4);
            assertThat(def.variables().stream().filter(a -> !a.isLiteral()).toList())
                    .hasSize(1)
                    .extracting(VariableDef::name)
                    .containsExactly("Infection_Rate");

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
    @DisplayName("Unsupported element warnings")
    class UnsupportedElementWarnings {

        @Test
        void shouldWarnAboutConveyorStock() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <stock name="Pipeline" conveyor="true">
                          <eqn>0</eqn>
                        </stock>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            assertThat(result.warnings()).anyMatch(w -> w.contains("conveyor"));
            assertThat(result.definition().stocks()).hasSize(1);
        }

        @Test
        void shouldWarnAboutQueueStock() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <stock name="WaitList" queue="true">
                          <eqn>0</eqn>
                        </stock>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            assertThat(result.warnings()).anyMatch(w -> w.contains("queue"));
        }

        @Test
        void shouldWarnAboutOvenStock() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <stock name="Batch" oven="true">
                          <eqn>0</eqn>
                        </stock>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            assertThat(result.warnings()).anyMatch(w -> w.contains("oven"));
        }

        @Test
        void shouldWarnAboutRangeOnStock() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <stock name="Level">
                          <eqn>50</eqn>
                          <range min="0" max="100"/>
                        </stock>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            assertThat(result.warnings()).anyMatch(w -> w.contains("Range") && w.contains("Level"));
        }

        @Test
        void shouldWarnAboutRangeOnAux() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <aux name="rate">
                          <eqn>a + b</eqn>
                          <range min="0" max="1"/>
                        </aux>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            assertThat(result.warnings()).anyMatch(w -> w.contains("Range") && w.contains("rate"));
        }

        @Test
        void shouldWarnAboutBiflow() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <stock name="Tank"><eqn>100</eqn><outflow>drain</outflow></stock>
                        <flow name="drain">
                          <eqn>5</eqn>
                        </flow>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            assertThat(result.warnings()).anyMatch(w -> w.contains("biflow"));
        }

        @Test
        void shouldNotWarnAboutUniflow() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <stock name="Tank"><eqn>100</eqn><outflow>drain</outflow></stock>
                        <flow name="drain">
                          <eqn>5</eqn>
                          <non_negative/>
                        </flow>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            assertThat(result.warnings()).noneMatch(w -> w.contains("biflow"));
        }

        @Test
        void shouldWarnAboutNonLinearInterpolation() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <aux name="effect">
                          <eqn>x</eqn>
                          <gf type="extrapolate">
                            <xpts>0,1,2,3,4</xpts>
                            <ypts>0,0.5,1.0,0.5,0</ypts>
                          </gf>
                        </aux>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            assertThat(result.warnings()).anyMatch(
                    w -> w.contains("interpolation") && w.contains("extrapolate"));
        }

        @Test
        void shouldNotWarnAboutContinuousInterpolation() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <aux name="effect">
                          <eqn>x</eqn>
                          <gf type="continuous">
                            <xpts>0,1,2,3,4</xpts>
                            <ypts>0,0.5,1.0,0.5,0</ypts>
                          </gf>
                        </aux>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            assertThat(result.warnings()).noneMatch(w -> w.contains("interpolation"));
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
    @DisplayName("Module import")
    class ModuleImport {

        @Test
        void shouldImportModuleWithInputBindings() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model name="Inner">
                        <variables>
                          <stock name="Tank"><eqn>0</eqn><inflow>fill</inflow></stock>
                          <flow name="fill"><eqn>rate_input</eqn></flow>
                        </variables>
                      </model>
                      <model>
                        <variables>
                          <aux name="MyRate"><eqn>5</eqn></aux>
                          <module name="Inner">
                            <connect to="rate_input" from="MyRate"/>
                          </module>
                        </variables>
                      </model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            ModelDefinition def = result.definition();

            assertThat(def.modules()).hasSize(1);
            ModuleInstanceDef mod = def.modules().get(0);
            assertThat(mod.instanceName()).isEqualTo("Inner");
            assertThat(mod.inputBindings()).containsEntry("rate_input", "MyRate");
            assertThat(mod.definition().stocks()).hasSize(1);
            assertThat(mod.definition().flows()).hasSize(1);
        }

        @Test
        void shouldImportModuleWithOutputBindings() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model name="Producer">
                        <variables>
                          <stock name="Value"><eqn>42</eqn></stock>
                          <aux name="output"><eqn>Value * 2</eqn></aux>
                        </variables>
                      </model>
                      <model>
                        <variables>
                          <module name="Producer">
                            <connect to=".produced_value" from="output"/>
                          </module>
                        </variables>
                      </model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            ModelDefinition def = result.definition();

            assertThat(def.modules()).hasSize(1);
            ModuleInstanceDef mod = def.modules().get(0);
            assertThat(mod.outputBindings()).containsEntry("output", "produced_value");
        }

        @Test
        void shouldWarnOnUnknownModuleReference() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model>
                        <variables>
                          <module name="NonExistent">
                            <connect to="x" from="y"/>
                          </module>
                        </variables>
                      </model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            assertThat(result.warnings()).anyMatch(
                    w -> w.contains("NonExistent") && w.contains("unknown model"));
            assertThat(result.definition().modules()).isEmpty();
        }

        @Test
        void shouldResolveModuleWhenInstanceNameDiffersFromModelName() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model name="Producer">
                        <variables>
                          <stock name="inventory"><eqn>100</eqn></stock>
                        </variables>
                      </model>
                      <model>
                        <variables>
                          <module name="producer">
                            <connect to="target" from="demand"/>
                          </module>
                        </variables>
                      </model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            assertThat(result.warnings()).noneMatch(w -> w.contains("unknown model"));
            assertThat(result.definition().modules()).hasSize(1);
            assertThat(result.definition().modules().get(0).instanceName()).isEqualTo("producer");
            assertThat(result.definition().modules().get(0).definition().name()).isEqualTo("Producer");
        }

        @Test
        void shouldImportModularSirModel() throws IOException {
            Path path = Path.of("src/test/resources/xmile/modular_sir.xmile");
            ImportResult result = importer.importModel(path);
            ModelDefinition def = result.definition();

            assertThat(def.name()).isEqualTo("Modular SIR");

            // Main model has 1 stock (Susceptible), 1 flow, 1 constant
            assertThat(def.stocks()).hasSize(1);
            assertThat(def.stocks().get(0).name()).isEqualTo("Susceptible");

            // 1 module instance
            assertThat(def.modules()).hasSize(1);
            ModuleInstanceDef mod = def.modules().get(0);
            assertThat(mod.instanceName()).isEqualTo("Disease");

            // Module has 2 input bindings
            assertThat(mod.inputBindings()).containsEntry("susceptible_pop", "Susceptible");
            assertThat(mod.inputBindings()).containsEntry("total_pop", "Total_Population");

            // Module has 1 output binding
            assertThat(mod.outputBindings()).containsEntry("current_infected", "disease_infected");

            // Inner definition has 2 stocks, 2 flows, 2 constants, 1 variable
            ModelDefinition inner = mod.definition();
            assertThat(inner.stocks()).hasSize(2);
            assertThat(inner.flows()).hasSize(2);
        }

        @Test
        void shouldCompileAndSimulateModularModel() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model name="Drainer">
                        <variables>
                          <stock name="Tank"><eqn>100</eqn><outflow>drain</outflow><units>Thing</units></stock>
                          <flow name="drain"><eqn>Tank * drain_pct</eqn><units>Thing/Day</units></flow>
                        </variables>
                      </model>
                      <model>
                        <variables>
                          <aux name="Rate"><eqn>0.1</eqn><units>1/Day</units></aux>
                          <module name="Drainer">
                            <connect to="drain_pct" from="Rate"/>
                          </module>
                        </variables>
                      </model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            CompiledModel compiled = new ModelCompiler().compile(result.definition());
            Simulation sim = compiled.createSimulation();
            sim.execute();

            Stock tank = compiled.getModel().getStocks().stream()
                    .filter(s -> s.getName().equals("Tank"))
                    .findFirst().orElseThrow();
            assertThat(tank.getValue()).as("Tank should have drained").isLessThan(100);
            assertThat(tank.getValue()).as("Tank should not be fully drained").isGreaterThan(30);
        }
    }

    @Nested
    @DisplayName("No-equation variables")
    class NoEquationVariables {

        @Test
        void shouldTreatNoEquationAuxAsConstantZeroViaStandardPath() {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Test</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <aux name="placeholder">
                          <doc>Awaiting calibration</doc>
                        </aux>
                      </variables></model>
                    </xmile>
                    """;

            ImportResult result = importer.importModel(xmile, "Test");
            // Should be created via builder.variable(), preserving comment
            assertThat(result.definition().variables()).hasSize(1);
            VariableDef v = result.definition().variables().get(0);
            assertThat(v.name()).isEqualTo("placeholder");
            assertThat(v.equation()).isEqualTo("0");
            assertThat(v.comment()).isEqualTo("Awaiting calibration");
            assertThat(result.warnings()).anyMatch(w -> w.contains("placeholder") && w.contains("constant 0"));
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

    @Nested
    @DisplayName("Charset fallback")
    class CharsetFallback {

        @TempDir
        Path tempDir;

        @Test
        void shouldFallBackToWindows1252WhenUtf8Fails() throws IOException {
            // \u00e9 (e-acute) is 0xE9 in windows-1252 but invalid as a standalone byte in UTF-8
            String xmile = """
                    <?xml version="1.0" encoding="windows-1252"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Caf\u00e9</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <stock name="Temp\u00e9rature">
                          <eqn>100</eqn>
                        </stock>
                      </variables></model>
                    </xmile>
                    """;
            Path file = tempDir.resolve("test.xmile");
            Files.write(file, xmile.getBytes(Charset.forName("windows-1252")));

            ImportResult result = importer.importModel(file);

            assertThat(result.definition().stocks()).hasSize(1);
            assertThat(result.definition().stocks().get(0).name()).isEqualTo("Temp\u00e9rature");
        }

        @Test
        void shouldStillReadUtf8FilesSuccessfully() throws IOException {
            String xmile = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <xmile xmlns="http://docs.oasis-open.org/xmile/ns/XMILE/v1.0" version="1.0">
                      <header><name>Caf\u00e9</name></header>
                      <sim_specs time_units="day"><start>0</start><stop>10</stop><dt>1</dt></sim_specs>
                      <model><variables>
                        <stock name="Temp\u00e9rature">
                          <eqn>100</eqn>
                        </stock>
                      </variables></model>
                    </xmile>
                    """;
            Path file = tempDir.resolve("test.xmile");
            Files.writeString(file, xmile);

            ImportResult result = importer.importModel(file);

            assertThat(result.definition().stocks()).hasSize(1);
            assertThat(result.definition().stocks().get(0).name()).isEqualTo("Temp\u00e9rature");
        }
    }

    @Nested
    @DisplayName("capitalizeFirst")
    class CapitalizeFirst {

        @Test
        @DisplayName("should capitalize first letter and preserve rest")
        void shouldCapitalizeFirstAndPreserveRest() {
            assertThat(XmileImporter.capitalizeFirst("day")).isEqualTo("Day");
        }

        @Test
        @DisplayName("should preserve mixed case after first character")
        void shouldPreserveMixedCase() {
            assertThat(XmileImporter.capitalizeFirst("kWh")).isEqualTo("KWh");
        }

        @Test
        @DisplayName("should handle single character")
        void shouldHandleSingleCharacter() {
            assertThat(XmileImporter.capitalizeFirst("d")).isEqualTo("D");
        }

        @Test
        @DisplayName("should return null for null input")
        void shouldReturnNullForNull() {
            assertThat(XmileImporter.capitalizeFirst(null)).isNull();
        }

        @Test
        @DisplayName("should return empty for empty input")
        void shouldReturnEmptyForEmpty() {
            assertThat(XmileImporter.capitalizeFirst("")).isEmpty();
        }

        @Test
        @DisplayName("should handle already capitalized input")
        void shouldHandleAlreadyCapitalized() {
            assertThat(XmileImporter.capitalizeFirst("Month")).isEqualTo("Month");
        }
    }
}
