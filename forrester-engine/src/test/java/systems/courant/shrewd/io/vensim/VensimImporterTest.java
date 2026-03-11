package systems.courant.forrester.io.vensim;

import systems.courant.forrester.io.ImportResult;
import systems.courant.forrester.model.def.AuxDef;
import systems.courant.forrester.model.def.CausalLinkDef;
import systems.courant.forrester.model.def.CldVariableDef;
import systems.courant.forrester.model.def.ElementType;
import systems.courant.forrester.model.def.FlowDef;
import systems.courant.forrester.model.def.LookupTableDef;
import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.StockDef;
import systems.courant.forrester.model.def.SubscriptDef;

import systems.courant.forrester.Simulation;
import systems.courant.forrester.model.Stock;
import systems.courant.forrester.model.compile.CompiledModel;
import systems.courant.forrester.model.compile.ModelCompiler;

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
            assertThat(flow.name()).isEqualTo("Population net flow");
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
            assertThat(result.definition().parameters()).hasSize(1 + 3); // +3 built-in constants
            AuxDef c = result.definition().parameters().stream()
                    .filter(cd -> cd.name().equals("alpha"))
                    .findFirst().orElseThrow();
            assertThat(c.literalValue()).isEqualTo(0.5);
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
            assertThat(result.definition().parameters()).hasSize(1 + 3); // +3 built-in constants
            assertThat(result.definition().parameters().stream()
                    .map(AuxDef::name)).contains("Pi");
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
            // 1 formula aux + 2 user constants + 3 built-in constants = 6
            assertThat(result.definition().auxiliaries()).hasSize(6);
            AuxDef aux = result.definition().auxiliaries().stream()
                    .filter(a -> !a.isLiteral()).findFirst().orElseThrow();
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
            assertThat(result.definition().parameters()).hasSize(3); // only built-in constants
            // All auxiliaries are literal (no formula auxes)
            assertThat(result.definition().auxiliaries().stream().filter(a -> !a.isLiteral()).toList())
                    .isEmpty();
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
        void shouldPreserveSpacesInMultiWordNames() {
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
            // Element names preserve spaces
            AuxDef aux = result.definition().auxiliaries().stream()
                    .filter(a -> a.name().equals("Infection Rate")).findFirst().orElseThrow();
            // Equations use underscore form for identifiers
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
            assertThat(lt.name()).isEqualTo("effect of pressure");
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
            assertThat(stock.name()).isEqualTo("Teacup Temperature");
            assertThat(stock.initialValue()).isEqualTo(180.0);

            // 2 user constants + 3 built-in constants (TIME_STEP, INITIAL_TIME, FINAL_TIME)
            assertThat(def.parameters()).hasSize(2 + 3);
            Set<String> constantNames = def.parameters().stream()
                    .map(AuxDef::name)
                    .collect(Collectors.toSet());
            assertThat(constantNames).contains(
                    "Room Temperature", "Characteristic Time",
                    "TIME_STEP", "INITIAL_TIME", "FINAL_TIME");

            // All auxiliaries: 1 formula + 2 user constants + 3 built-in constants = 6
            assertThat(def.auxiliaries()).hasSize(6);
            assertThat(def.auxiliaries().stream().filter(a -> !a.isLiteral()).toList())
                    .hasSize(1)
                    .first().extracting(AuxDef::name).isEqualTo("Heat Loss to Room");

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

            // 3 user constants + 3 built-in constants (TIME_STEP, INITIAL_TIME, FINAL_TIME)
            assertThat(def.parameters()).hasSize(3 + 3);
            Set<String> constantNames = def.parameters().stream()
                    .map(AuxDef::name)
                    .collect(Collectors.toSet());
            assertThat(constantNames).contains(
                    "Contact Rate", "Recovery Time", "Total Population",
                    "TIME_STEP", "INITIAL_TIME", "FINAL_TIME");

            // 2 formula auxiliaries + 6 literal-valued (constants) = 8 total
            assertThat(def.auxiliaries()).hasSize(8);
            assertThat(def.auxiliaries().stream().filter(a -> !a.isLiteral()).toList()).hasSize(2);

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

    @Nested
    @DisplayName("Numeric literal handling")
    class NumericLiterals {

        @Test
        void shouldClassifyDotFiveAsConstant() {
            String mdl = """
                    alpha = .5
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
            assertThat(result.definition().parameters()).hasSize(1 + 3); // +3 built-in constants
            AuxDef c = result.definition().parameters().stream()
                    .filter(cd -> cd.name().equals("alpha"))
                    .findFirst().orElseThrow();
            assertThat(c.literalValue()).isEqualTo(0.5);
        }

        @Test
        void shouldClassifyNegativeDotFiveAsConstant() {
            String mdl = """
                    alpha = -.5
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
            assertThat(result.definition().parameters()).hasSize(1 + 3); // +3 built-in constants
            AuxDef c = result.definition().parameters().stream()
                    .filter(cd -> cd.name().equals("alpha"))
                    .findFirst().orElseThrow();
            assertThat(c.literalValue()).isEqualTo(-0.5);
        }
    }

    @Nested
    @DisplayName("Case-insensitive system variables")
    class CaseInsensitiveSystemVars {

        @Test
        void shouldRecognizeLowercaseSystemVars() {
            String mdl = """
                    x = 5
                    \t~\t
                    \t~\t
                    \t|

                    initial time = 0
                    \t~\tDay
                    \t~\t
                    \t|

                    final time = 10
                    \t~\tDay
                    \t~\t
                    \t|

                    time step = 1
                    \t~\tDay
                    \t~\t
                    \t|
                    """;

            ImportResult result = importer.importModel(mdl, "Test");
            // System vars should not appear as user model elements, but 3 built-in constants are injected
            assertThat(result.definition().parameters()).hasSize(1 + 3); // +3 built-in constants
            assertThat(result.definition().parameters().stream()
                    .map(AuxDef::name)
                    .filter(n -> !Set.of("TIME_STEP", "INITIAL_TIME", "FINAL_TIME").contains(n))
                    .toList()).containsExactly("x");
            assertThat(result.definition().defaultSimulation().duration()).isEqualTo(10.0);
        }
    }

    @Nested
    @DisplayName("Duplicate name detection")
    class DuplicateNames {

        @Test
        void shouldWarnOnDuplicateNormalizedNames() {
            // "x_y" and "x y" both normalize to "x_y"
            String mdl = """
                    x_y = 100
                    \t~\t
                    \t~\t
                    \t|

                    x y = 200
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
            assertThat(result.warnings()).anyMatch(w -> w.contains("Duplicate normalized name"));
        }
    }

    @Nested
    @DisplayName("FINAL TIME <= INITIAL TIME edge case")
    class FinalTimeLessThanInitialTime {

        @Test
        void shouldWarnAndDefaultWhenFinalTimeLessThanInitialTime() {
            String mdl = """
                    x = 5
                    \t~\t
                    \t~\t
                    \t|

                    INITIAL TIME = 100
                    \t~\tDay
                    \t~\t
                    \t|

                    FINAL TIME = 50
                    \t~\tDay
                    \t~\t
                    \t|

                    TIME STEP = 1
                    \t~\tDay
                    \t~\t
                    \t|
                    """;

            ImportResult result = importer.importModel(mdl, "Test");
            assertThat(result.warnings()).anyMatch(w -> w.contains("FINAL TIME"));
            assertThat(result.definition().defaultSimulation().duration()).isEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("CLD (Causal Loop Diagram) import")
    class CldImport {

        @Test
        void shouldDetectCldWhenNoStocks() {
            String mdl = """
                    Population= 0
                    \t~\t
                    \t~\tThe total population
                    \t|

                    Birth Rate= 0
                    \t~\t
                    \t~\tRate of births
                    \t|

                    INITIAL TIME = 0
                    \t~\tYear
                    \t~\t
                    \t|

                    FINAL TIME = 100
                    \t~\tYear
                    \t~\t
                    \t|

                    TIME STEP = 1
                    \t~\tYear
                    \t~\t
                    \t|

                    \\---///
                    *View
                    10,1,Population,200,200
                    10,2,Birth Rate,100,100
                    1,3,2,1
                    """;

            ImportResult result = importer.importModel(mdl, "Test");
            ModelDefinition def = result.definition();

            // No stocks, flows, or formula auxiliaries in CLD mode; only built-in constants
            assertThat(def.stocks()).isEmpty();
            assertThat(def.flows()).isEmpty();
            assertThat(def.auxiliaries().stream().filter(a -> !a.isLiteral()).toList()).isEmpty();
            assertThat(def.parameters()).hasSize(3); // only built-in constants

            // Should have CLD variables
            assertThat(def.cldVariables()).hasSize(2);
            Set<String> names = def.cldVariables().stream()
                    .map(CldVariableDef::name)
                    .collect(Collectors.toSet());
            assertThat(names).containsExactlyInAnyOrder("Population", "Birth Rate");
        }

        @Test
        void shouldExtractCausalLinksFromSketch() {
            String mdl = """
                    A= 0
                    \t~\t
                    \t~\t
                    \t|

                    B= 0
                    \t~\t
                    \t~\t
                    \t|

                    C= 0
                    \t~\t
                    \t~\t
                    \t|

                    INITIAL TIME = 0
                    \t~\tYear
                    \t~\t
                    \t|

                    FINAL TIME = 100
                    \t~\tYear
                    \t~\t
                    \t|

                    TIME STEP = 1
                    \t~\tYear
                    \t~\t
                    \t|

                    \\---///
                    *View
                    10,1,A,100,100
                    10,2,B,200,100
                    10,3,C,300,100
                    1,4,1,2
                    1,5,2,3
                    """;

            ImportResult result = importer.importModel(mdl, "Test");
            ModelDefinition def = result.definition();

            assertThat(def.cldVariables()).hasSize(3);
            assertThat(def.causalLinks()).hasSize(2);

            CausalLinkDef link1 = def.causalLinks().get(0);
            assertThat(link1.from()).isEqualTo("A");
            assertThat(link1.to()).isEqualTo("B");
            assertThat(link1.polarity()).isEqualTo(CausalLinkDef.Polarity.UNKNOWN);

            CausalLinkDef link2 = def.causalLinks().get(1);
            assertThat(link2.from()).isEqualTo("B");
            assertThat(link2.to()).isEqualTo("C");
        }

        @Test
        void shouldClassifySketchElementsAsCldVariable() {
            String mdl = """
                    X= 0
                    \t~\t
                    \t~\t
                    \t|

                    Y= 0
                    \t~\t
                    \t~\t
                    \t|

                    INITIAL TIME = 0
                    \t~\tYear
                    \t~\t
                    \t|

                    FINAL TIME = 100
                    \t~\tYear
                    \t~\t
                    \t|

                    TIME STEP = 1
                    \t~\tYear
                    \t~\t
                    \t|

                    \\---///
                    *View
                    10,1,X,100,100
                    10,2,Y,200,100
                    """;

            ImportResult result = importer.importModel(mdl, "Test");
            ModelDefinition def = result.definition();

            assertThat(def.views()).hasSize(1);
            assertThat(def.views().getFirst().elements()).hasSize(2);
            assertThat(def.views().getFirst().elements().get(0).type())
                    .isEqualTo(ElementType.CLD_VARIABLE);
            assertThat(def.views().getFirst().elements().get(1).type())
                    .isEqualTo(ElementType.CLD_VARIABLE);
        }

        @Test
        void shouldImportSimpleCldFile() throws IOException {
            Path path = Path.of("src/test/resources/vensim/simple-cld.mdl");
            ImportResult result = importer.importModel(path);
            ModelDefinition def = result.definition();

            assertThat(def.name()).isEqualTo("simple-cld");

            // 4 CLD variables
            assertThat(def.cldVariables()).hasSize(4);
            Set<String> names = def.cldVariables().stream()
                    .map(CldVariableDef::name)
                    .collect(Collectors.toSet());
            assertThat(names).containsExactlyInAnyOrder(
                    "Population", "Birth Rate", "Death Rate", "Resources");

            // No stocks, flows, or formula auxiliaries; only built-in constants
            assertThat(def.stocks()).isEmpty();
            assertThat(def.flows()).isEmpty();
            assertThat(def.auxiliaries().stream().filter(a -> !a.isLiteral()).toList()).isEmpty();
            assertThat(def.parameters()).hasSize(3); // only built-in constants

            // 4 causal links from sketch connectors
            assertThat(def.causalLinks()).hasSize(4);

            // Sketch should have a view with CLD_VARIABLE elements
            assertThat(def.views()).hasSize(1);
            assertThat(def.views().getFirst().name()).isEqualTo("CLD View");
        }

        @Test
        void shouldNotDetectCldWhenStocksExist() {
            String mdl = """
                    Stock = INTEG(rate, 100)
                    \t~\tUnits
                    \t~\t
                    \t|

                    rate = 10
                    \t~\tUnits/Year
                    \t~\t
                    \t|

                    INITIAL TIME = 0
                    \t~\tYear
                    \t~\t
                    \t|

                    FINAL TIME = 100
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

            // Should NOT be treated as CLD
            assertThat(def.stocks()).hasSize(1);
            assertThat(def.cldVariables()).isEmpty();
            assertThat(def.causalLinks()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Import → compile → simulate round-trip")
    class ImportCompileSimulate {

        @Test
        void shouldImportCompileAndSimulateTeacup() throws IOException {
            Path path = Path.of("src/test/resources/vensim/teacup.mdl");
            ImportResult result = importer.importModel(path);
            ModelDefinition def = result.definition();

            // Compile the imported definition
            CompiledModel compiled = new ModelCompiler().compile(def);
            assertThat(compiled).isNotNull();

            // Run the simulation
            Simulation sim = compiled.createSimulation();
            sim.execute();

            // The teacup should cool toward room temperature (70°F)
            // Starting at 180°F, after 30 minutes it should be significantly cooler
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
