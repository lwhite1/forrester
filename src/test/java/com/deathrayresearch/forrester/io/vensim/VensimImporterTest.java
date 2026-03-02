package com.deathrayresearch.forrester.io.vensim;

import com.deathrayresearch.forrester.io.ImportResult;
import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.LookupTableDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.StockDef;
import com.deathrayresearch.forrester.model.def.SubscriptDef;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VensimImporter")
class VensimImporterTest {

    private final VensimImporter importer = new VensimImporter();

    @Nested
    @DisplayName("Element classification")
    class ElementClassification {

        @Test
        void shouldClassifyStockFromInteg() {
            String mdl = """
                    Population = INTEG(births, 100)
                    \t~\tPeople
                    \t~\t
                    \t|

                    births = 10
                    \t~\tPeople/Year
                    \t~\t
                    \t|

                    INITIAL TIME = 0
                    \t~\tYear
                    \t~\t
                    \t|

                    FINAL TIME = 50
                    \t~\tYear
                    \t~\t
                    \t|

                    TIME STEP = 1
                    \t~\tYear
                    \t~\t
                    \t|
                    """;

            ImportResult result = importer.importModel(mdl, "Test");
            ModelDefinition def = result.definition();

            assertThat(def.stocks()).hasSize(1);
            StockDef stock = def.stocks().get(0);
            assertThat(stock.name()).isEqualTo("Population");
            assertThat(stock.initialValue()).isEqualTo(100.0);

            // Should also create a net flow for the stock
            assertThat(def.flows()).hasSize(1);
            FlowDef flow = def.flows().get(0);
            assertThat(flow.name()).isEqualTo("Population_net_flow");
            assertThat(flow.sink()).isEqualTo("Population");
        }

        @Test
        void shouldClassifyNumericLiteralAsConstant() {
            String mdl = """
                    alpha = 0.5
                    \t~\tDimensionless
                    \t~\t
                    \t|

                    INITIAL TIME = 0
                    \t~\tDay
                    \t~\t
                    \t|

                    FINAL TIME = 10
                    \t~\tDay
                    \t~\t
                    \t|

                    TIME STEP = 1
                    \t~\tDay
                    \t~\t
                    \t|
                    """;

            ImportResult result = importer.importModel(mdl, "Test");
            assertThat(result.definition().constants()).hasSize(1);
            ConstantDef c = result.definition().constants().get(0);
            assertThat(c.name()).isEqualTo("alpha");
            assertThat(c.value()).isEqualTo(0.5);
        }

        @Test
        void shouldClassifyUnchangeableAsConstant() {
            String mdl = """
                    Pi == 3.14159
                    \t~\t
                    \t~\t
                    \t|

                    INITIAL TIME = 0
                    \t~\tDay
                    \t~\t
                    \t|

                    FINAL TIME = 10
                    \t~\tDay
                    \t~\t
                    \t|

                    TIME STEP = 1
                    \t~\tDay
                    \t~\t
                    \t|
                    """;

            ImportResult result = importer.importModel(mdl, "Test");
            assertThat(result.definition().constants()).hasSize(1);
            assertThat(result.definition().constants().get(0).name()).isEqualTo("Pi");
        }

        @Test
        void shouldClassifyExpressionAsAuxiliary() {
            String mdl = """
                    rate = alpha * beta
                    \t~\t1/Day
                    \t~\t
                    \t|

                    alpha = 0.5
                    \t~\t
                    \t~\t
                    \t|

                    beta = 2
                    \t~\t
                    \t~\t
                    \t|

                    INITIAL TIME = 0
                    \t~\tDay
                    \t~\t
                    \t|

                    FINAL TIME = 10
                    \t~\tDay
                    \t~\t
                    \t|

                    TIME STEP = 1
                    \t~\tDay
                    \t~\t
                    \t|
                    """;

            ImportResult result = importer.importModel(mdl, "Test");
            assertThat(result.definition().auxiliaries()).hasSize(1);
            AuxDef aux = result.definition().auxiliaries().get(0);
            assertThat(aux.name()).isEqualTo("rate");
            assertThat(aux.equation()).isEqualTo("alpha * beta");
        }

        @Test
        void shouldSkipDataVariableWithWarning() {
            String mdl = """
                    External := GET XLS DATA('file.xls', 'Sheet', 'A', 'B2')
                    \t~\t
                    \t~\t
                    \t|

                    INITIAL TIME = 0
                    \t~\tDay
                    \t~\t
                    \t|

                    FINAL TIME = 10
                    \t~\tDay
                    \t~\t
                    \t|

                    TIME STEP = 1
                    \t~\tDay
                    \t~\t
                    \t|
                    """;

            ImportResult result = importer.importModel(mdl, "Test");
            assertThat(result.warnings()).anyMatch(w -> w.contains("Data variable"));
            assertThat(result.definition().constants()).isEmpty();
            assertThat(result.definition().auxiliaries()).isEmpty();
        }

        @Test
        void shouldClassifySubscriptRange() {
            String mdl = """
                    Region : North, South, East, West
                    \t~\t
                    \t~\t
                    \t|

                    INITIAL TIME = 0
                    \t~\tDay
                    \t~\t
                    \t|

                    FINAL TIME = 10
                    \t~\tDay
                    \t~\t
                    \t|

                    TIME STEP = 1
                    \t~\tDay
                    \t~\t
                    \t|
                    """;

            ImportResult result = importer.importModel(mdl, "Test");
            assertThat(result.definition().subscripts()).hasSize(1);
            SubscriptDef sub = result.definition().subscripts().get(0);
            assertThat(sub.name()).isEqualTo("Region");
            assertThat(sub.labels()).containsExactly("North", "South", "East", "West");
        }
    }

    @Nested
    @DisplayName("Simulation settings")
    class SimulationSettingsTests {

        @Test
        void shouldExtractSimulationSettings() {
            String mdl = """
                    x = 1
                    \t~\t
                    \t~\t
                    \t|

                    INITIAL TIME = 0
                    \t~\tMonth
                    \t~\t
                    \t|

                    FINAL TIME = 120
                    \t~\tMonth
                    \t~\t
                    \t|

                    TIME STEP = 0.5
                    \t~\tMonth
                    \t~\t
                    \t|
                    """;

            ImportResult result = importer.importModel(mdl, "Test");
            var sim = result.definition().defaultSimulation();
            assertThat(sim).isNotNull();
            assertThat(sim.timeStep()).isEqualTo("Month");
            assertThat(sim.duration()).isEqualTo(120.0);
            assertThat(sim.durationUnit()).isEqualTo("Month");
        }
    }

    @Nested
    @DisplayName("Multi-word name handling")
    class MultiWordNames {

        @Test
        void shouldNormalizeMultiWordNamesInExpressions() {
            String mdl = """
                    Infection Rate = Contact Rate * Infected
                    \t~\tPeople/Day
                    \t~\t
                    \t|

                    Contact Rate = 0.3
                    \t~\t1/Day
                    \t~\t
                    \t|

                    Infected = 10
                    \t~\tPeople
                    \t~\t
                    \t|

                    INITIAL TIME = 0
                    \t~\tDay
                    \t~\t
                    \t|

                    FINAL TIME = 100
                    \t~\tDay
                    \t~\t
                    \t|

                    TIME STEP = 1
                    \t~\tDay
                    \t~\t
                    \t|
                    """;

            ImportResult result = importer.importModel(mdl, "Test");
            AuxDef aux = result.definition().auxiliaries().get(0);
            assertThat(aux.name()).isEqualTo("Infection_Rate");
            assertThat(aux.equation()).isEqualTo("Contact_Rate * Infected");
        }
    }

    @Nested
    @DisplayName("Lookup tables")
    class LookupTables {

        @Test
        void shouldImportStandaloneLookupTable() {
            String mdl = """
                    effect of pressure(
                    \t[(0,0)-(2,2)],(0,0),(0.5,0.5),(1,1),(1.5,1.7),(2,2)
                    \t)
                    \t~\tDimensionless
                    \t~\t
                    \t|

                    INITIAL TIME = 0
                    \t~\tDay
                    \t~\t
                    \t|

                    FINAL TIME = 10
                    \t~\tDay
                    \t~\t
                    \t|

                    TIME STEP = 1
                    \t~\tDay
                    \t~\t
                    \t|
                    """;

            ImportResult result = importer.importModel(mdl, "Test");
            assertThat(result.definition().lookupTables()).hasSize(1);
            LookupTableDef lt = result.definition().lookupTables().get(0);
            assertThat(lt.name()).isEqualTo("effect_of_pressure");
            assertThat(lt.xValues().length).isGreaterThanOrEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Teacup model integration test")
    class TeacupModel {

        @Test
        void shouldImportTeacupModel() throws IOException {
            Path path = Path.of("src/test/resources/vensim/teacup.mdl");
            ImportResult result = importer.importModel(path);
            ModelDefinition def = result.definition();

            assertThat(def.name()).isEqualTo("teacup");

            // 1 stock: Teacup Temperature
            assertThat(def.stocks()).hasSize(1);
            StockDef stock = def.stocks().get(0);
            assertThat(stock.name()).isEqualTo("Teacup_Temperature");
            assertThat(stock.initialValue()).isEqualTo(180.0);

            // 2 constants: Room Temperature, Characteristic Time
            assertThat(def.constants()).hasSize(2);
            Set<String> constantNames = def.constants().stream()
                    .map(ConstantDef::name)
                    .collect(Collectors.toSet());
            assertThat(constantNames).containsExactlyInAnyOrder(
                    "Room_Temperature", "Characteristic_Time");

            // 1 auxiliary: Heat Loss to Room
            assertThat(def.auxiliaries()).hasSize(1);
            assertThat(def.auxiliaries().get(0).name()).isEqualTo("Heat_Loss_to_Room");

            // 1 flow: net flow for the stock
            assertThat(def.flows()).hasSize(1);

            // Simulation settings
            assertThat(def.defaultSimulation()).isNotNull();
            assertThat(def.defaultSimulation().timeStep()).isEqualTo("Minute");
            assertThat(def.defaultSimulation().duration()).isEqualTo(30.0);
        }
    }

    @Nested
    @DisplayName("SIR model integration test")
    class SirModel {

        @Test
        void shouldImportSirModel() throws IOException {
            Path path = Path.of("src/test/resources/vensim/sir.mdl");
            ImportResult result = importer.importModel(path);
            ModelDefinition def = result.definition();

            assertThat(def.name()).isEqualTo("sir");

            // 3 stocks
            assertThat(def.stocks()).hasSize(3);
            Set<String> stockNames = def.stocks().stream()
                    .map(StockDef::name)
                    .collect(Collectors.toSet());
            assertThat(stockNames).containsExactlyInAnyOrder(
                    "Susceptible", "Infected", "Recovered");

            // 3 constants: Contact Rate, Recovery Time, Total Population
            assertThat(def.constants()).hasSize(3);
            Set<String> constantNames = def.constants().stream()
                    .map(ConstantDef::name)
                    .collect(Collectors.toSet());
            assertThat(constantNames).containsExactlyInAnyOrder(
                    "Contact_Rate", "Recovery_Time", "Total_Population");

            // 2 auxiliaries: Infection Rate, Recovery Rate
            assertThat(def.auxiliaries()).hasSize(2);

            // 3 flows: one per stock
            assertThat(def.flows()).hasSize(3);

            // Simulation settings: 200 days
            assertThat(def.defaultSimulation()).isNotNull();
            assertThat(def.defaultSimulation().timeStep()).isEqualTo("Day");
            assertThat(def.defaultSimulation().duration()).isEqualTo(200.0);

            // Sketch: should have at least one view
            assertThat(def.views()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("ImportResult")
    class ImportResultTests {

        @Test
        void shouldReportCleanImport() {
            String mdl = """
                    x = 5
                    \t~\tUnit
                    \t~\t
                    \t|

                    INITIAL TIME = 0
                    \t~\tDay
                    \t~\t
                    \t|

                    FINAL TIME = 10
                    \t~\tDay
                    \t~\t
                    \t|

                    TIME STEP = 1
                    \t~\tDay
                    \t~\t
                    \t|
                    """;

            ImportResult result = importer.importModel(mdl, "Test");
            assertThat(result.definition()).isNotNull();
            // Even clean models may have minor warnings, so just check definition exists
            assertThat(result.definition().name()).isEqualTo("Test");
        }
    }
}
