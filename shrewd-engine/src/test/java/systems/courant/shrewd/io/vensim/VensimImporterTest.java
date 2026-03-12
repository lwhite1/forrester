package systems.courant.shrewd.io.vensim;

import systems.courant.shrewd.io.ImportResult;
import systems.courant.shrewd.model.def.VariableDef;
import systems.courant.shrewd.model.def.CausalLinkDef;
import systems.courant.shrewd.model.def.CldVariableDef;
import systems.courant.shrewd.model.def.ElementType;
import systems.courant.shrewd.model.def.FlowDef;
import systems.courant.shrewd.model.def.LookupTableDef;
import systems.courant.shrewd.model.def.ModelDefinition;
import systems.courant.shrewd.model.def.StockDef;
import systems.courant.shrewd.model.def.SubscriptDef;

import systems.courant.shrewd.Simulation;
import systems.courant.shrewd.model.Stock;
import systems.courant.shrewd.model.compile.CompiledModel;
import systems.courant.shrewd.model.compile.ModelCompiler;

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
            VariableDef c = result.definition().parameters().stream()
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
                    .map(VariableDef::name)).contains("Pi");
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
            // 1 formula variable + 2 user constants + 3 built-in constants = 6
            assertThat(result.definition().variables()).hasSize(6);
            VariableDef v = result.definition().variables().stream()
                    .filter(a -> !a.isLiteral()).findFirst().orElseThrow();
            assertThat(v.name()).isEqualTo("rate");
            assertThat(v.equation()).isEqualTo("alpha * beta");
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
            assertThat(result.warnings()).anyMatch(w -> w.contains("imported as constant 0"));
            // Data variable creates a placeholder variable (value 0) plus 3 built-in constants
            assertThat(result.definition().variables()).hasSize(4);
            // All variables are literal (no formula auxes)
            assertThat(result.definition().variables().stream().filter(a -> !a.isLiteral()).toList())
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

        @Test
        void shouldNotWarnWhenSubscriptNameMatchesVariableName() {
            String mdl = """
                    Region : North, South, Total
                    \t~\t
                    \t~\t
                    \t|

                    Region = North + South
                    \t~\tPeople
                    \t~\t
                    \t|

                    North = 100
                    \t~\tPeople
                    \t~\t
                    \t|

                    South = 200
                    \t~\tPeople
                    \t~\t
                    \t|

                    Total = North + South
                    \t~\tPeople
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
            // The subscript range name "Region" should not produce a false duplicate warning
            // with the variable named "Region"
            assertThat(result.warnings().stream()
                    .filter(w -> w.contains("Duplicate") && w.contains("Region"))
                    .toList()).isEmpty();
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
            assertThat(sim.dt()).isEqualTo(0.5);
        }

        @Test
        void shouldPreserveDefaultDtWhenTimeStepIsOne() {
            String mdl = """
                    x = 1
                    \t~\t
                    \t~\t
                    \t|

                    INITIAL TIME = 0
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
            var sim = result.definition().defaultSimulation();
            assertThat(sim).isNotNull();
            assertThat(sim.dt()).isEqualTo(1.0);
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
            VariableDef v = result.definition().variables().stream()
                    .filter(a -> a.name().equals("Infection Rate")).findFirst().orElseThrow();
            // Equations use underscore form for identifiers
            assertThat(v.equation()).isEqualTo("Contact_Rate * Infected");
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
                    .map(VariableDef::name)
                    .collect(Collectors.toSet());
            assertThat(constantNames).contains(
                    "Room Temperature", "Characteristic Time",
                    "TIME_STEP", "INITIAL_TIME", "FINAL_TIME");

            // All variables: 1 formula + 2 user constants + 3 built-in constants = 6
            assertThat(def.variables()).hasSize(6);
            assertThat(def.variables().stream().filter(a -> !a.isLiteral()).toList())
                    .hasSize(1)
                    .first().extracting(VariableDef::name).isEqualTo("Heat Loss to Room");

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
                    .map(VariableDef::name)
                    .collect(Collectors.toSet());
            assertThat(constantNames).contains(
                    "Contact Rate", "Recovery Time", "Total Population",
                    "TIME_STEP", "INITIAL_TIME", "FINAL_TIME");

            // 2 formula variables + 6 literal-valued (constants) = 8 total
            assertThat(def.variables()).hasSize(8);
            assertThat(def.variables().stream().filter(a -> !a.isLiteral()).toList()).hasSize(2);

            // 4 flows: Susceptible/Recovered each get a net flow,
            // Infected decomposes INTEG(Infection Rate - Recovery Rate) into 2 individual flows
            assertThat(def.flows()).hasSize(4);

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
            VariableDef c = result.definition().parameters().stream()
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
            VariableDef c = result.definition().parameters().stream()
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
                    .map(VariableDef::name)
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

            // No stocks, flows, or formula variables in CLD mode; only built-in constants
            assertThat(def.stocks()).isEmpty();
            assertThat(def.flows()).isEmpty();
            assertThat(def.variables().stream().filter(a -> !a.isLiteral()).toList()).isEmpty();
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

            // No stocks, flows, or formula variables; only built-in constants
            assertThat(def.stocks()).isEmpty();
            assertThat(def.flows()).isEmpty();
            assertThat(def.variables().stream().filter(a -> !a.isLiteral()).toList()).isEmpty();
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

    @Nested
    @DisplayName("Double UTF-8 header handling (#491)")
    class DoubleUtf8Header {

        @Test
        void shouldHandleDoubleUtf8Header() {
            // Many Vensim files (e.g., RABFOX.MDL, DELIV.MDL) have two {UTF-8} headers.
            // The second must be stripped or it corrupts the first variable name.
            String mdl = """
                    {UTF-8}
                    {UTF-8}

                    fox births = Fox Population * fox birth rate
                    \t~\tFox/Year
                    \t~\t
                    \t|

                    Fox Population = INTEG(fox births, 30)
                    \t~\tFox
                    \t~\t
                    \t|

                    fox birth rate = 0.25
                    \t~\t1/Year
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

            // The first variable should have a clean name, not "{UTF-8}..." prefix
            VariableDef v = def.variables().stream()
                    .filter(a -> a.name().equals("fox births"))
                    .findFirst().orElseThrow();
            // Multi-word name replacement should work in the equation
            assertThat(v.equation()).isEqualTo("Fox_Population * fox_birth_rate");
        }

        @Test
        void shouldCompileModelWithDoubleUtf8Header() {
            // Verifies the full pipeline works with double headers
            String mdl = """
                    {UTF-8}
                    {UTF-8}

                    new customers = shipments / products per customer
                    \t~\tPerson/Week
                    \t~\t
                    \t|

                    Customers = INTEG(new customers, 100)
                    \t~\tPerson
                    \t~\t
                    \t|

                    products per customer = 1
                    \t~\tMachine/Person
                    \t~\t
                    \t|

                    shipments = 50
                    \t~\tMachine/Week
                    \t~\t
                    \t|

                    INITIAL TIME = 0
                    \t~\tWeek
                    \t~\t
                    \t|

                    FINAL TIME = 100
                    \t~\tWeek
                    \t~\t
                    \t|

                    TIME STEP = 1
                    \t~\tWeek
                    \t~\t
                    \t|
                    """;

            ImportResult result = importer.importModel(mdl, "Test");
            ModelDefinition def = result.definition();

            // Should compile without "Unexpected character" errors
            CompiledModel compiled = new ModelCompiler().compile(def);
            assertThat(compiled).isNotNull();

            // Verify the flow equation uses normalized names
            FlowDef flow = def.flows().stream()
                    .filter(f -> f.name().contains("Customers"))
                    .findFirst().orElseThrow();
            assertThat(flow.equation()).isEqualTo("new_customers");
        }
    }

    @Nested
    @DisplayName("Flat CSV lookup tables (#490)")
    class FlatCsvLookupImport {

        @Test
        void shouldImportFlatCsvLookupTable() {
            // Format used by BURNOUT.MDL and WORLD.MDL
            String mdl = """
                    effect = effect lookup(input)
                    \t~\tDimensionless
                    \t~\t
                    \t|

                    effect lookup(
                    \t0,0.2,0.4,0.6,0.8,1,
                    \t0,0.2,0.4,0.6,0.8,1)
                    \t~\tDimensionless
                    \t~\t
                    \t|

                    input = 0.5
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
            ModelDefinition def = result.definition();

            // Lookup table should be parsed successfully
            assertThat(def.lookupTables()).hasSize(1);
            LookupTableDef lt = def.lookupTables().get(0);
            assertThat(lt.name()).isEqualTo("effect lookup");
            assertThat(lt.xValues()).hasSize(6);
            assertThat(lt.yValues()).hasSize(6);

            // No warnings about failing to parse lookup data
            assertThat(result.warnings().stream()
                    .filter(w -> w.contains("Could not parse lookup"))
                    .toList()).isEmpty();
        }

        @Test
        void shouldCompileModelWithFlatCsvLookup() {
            String mdl = """
                    effect = effect lookup(input)
                    \t~\tDimensionless
                    \t~\t
                    \t|

                    effect lookup(
                    \t0,0.5,1,
                    \t0,0.5,1)
                    \t~\tDimensionless
                    \t~\t
                    \t|

                    input = 0.5
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

            // Should compile without "references unknown element" errors
            CompiledModel compiled = new ModelCompiler().compile(result.definition());
            assertThat(compiled).isNotNull();
        }
    }

    @Nested
    @DisplayName("INITIAL function reference resolution (#494)")
    class InitialFunctionReference {

        @Test
        void shouldCompileModelWithInitialFunction() {
            // INITIAL(expr) should be recognized as a built-in function,
            // not flagged as "references unknown element: INITIAL"
            String mdl = """
                    target = 100
                    \t~\tWidgets
                    \t~\t
                    \t|

                    baseline = INITIAL(target)
                    \t~\tWidgets
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
            ModelDefinition def = result.definition();

            // Should compile without "references unknown element: INITIAL"
            CompiledModel compiled = new ModelCompiler().compile(def);
            assertThat(compiled).isNotNull();
        }

        @Test
        void shouldCompileModelWithInitialInStockInit() {
            // Pattern from WFKAL.MDL: INITIAL used in stock initial value expression
            String mdl = """
                    desired = 50
                    \t~\tPerson
                    \t~\t
                    \t|

                    initial level = INITIAL(desired)
                    \t~\tPerson
                    \t~\t
                    \t|

                    Stock = INTEG(inflow, initial level)
                    \t~\tPerson
                    \t~\t
                    \t|

                    inflow = 10
                    \t~\tPerson/Day
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
            ModelDefinition def = result.definition();

            CompiledModel compiled = new ModelCompiler().compile(def);
            assertThat(compiled).isNotNull();
        }
    }

    @Nested
    @DisplayName("Data variable placeholder import (#494)")
    class DataVariablePlaceholder {

        @Test
        void shouldCreatePlaceholderForDataVariable() {
            // Vensim := operator marks data variables (external data source).
            // The importer should create a constant 0 placeholder so references resolve.
            String mdl = """
                    external input := 0
                    \t~\tWidgets/Day
                    \t~\t
                    \t|

                    output = external input * 2
                    \t~\tWidgets/Day
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
            ModelDefinition def = result.definition();

            // Data variable should create a placeholder variable
            assertThat(def.variables().stream()
                    .anyMatch(a -> a.name().equals("external input"))).isTrue();

            // Should compile without "references unknown element"
            CompiledModel compiled = new ModelCompiler().compile(def);
            assertThat(compiled).isNotNull();
        }

        @Test
        void shouldCreatePlaceholderForBareNameVariable() {
            // Some Vensim variables have no equation at all (just a name, unit, comment).
            // Pattern from WFKAL.MDL: "sales" with no equation.
            String mdl = """
                    sales
                    \t~\tWidgets/Month
                    \t~\t
                    \t|

                    revenue = sales * 10
                    \t~\tDollars/Month
                    \t~\t
                    \t|

                    INITIAL TIME = 0
                    \t~\tMonth
                    \t~\t
                    \t|

                    FINAL TIME = 10
                    \t~\tMonth
                    \t~\t
                    \t|

                    TIME STEP = 1
                    \t~\tMonth
                    \t~\t
                    \t|
                    """;

            ImportResult result = importer.importModel(mdl, "Test");
            ModelDefinition def = result.definition();

            // Bare variable should create a placeholder variable
            assertThat(def.variables().stream()
                    .anyMatch(a -> a.name().equals("sales"))).isTrue();

            // Should compile without "references unknown element: sales"
            CompiledModel compiled = new ModelCompiler().compile(def);
            assertThat(compiled).isNotNull();
        }

        @Test
        void shouldWarnAboutDataVariableImport() {
            String mdl = """
                    data input := 0
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
            assertThat(result.warnings()).anyMatch(w -> w.contains("imported as constant 0"));
        }
    }

    @Nested
    @DisplayName("Not-equal operator in equations (#492)")
    class NotEqualOperator {

        @Test
        void shouldCompileModelWithNotEqualOperator() {
            // Vensim uses <> for not-equal; Shrewd uses !=
            String mdl = """
                    trigger = IF THEN ELSE(value <> 0, 1, 0)
                    \t~\tDimensionless
                    \t~\t
                    \t|

                    value = 5
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
            ModelDefinition def = result.definition();

            // Should compile without parse errors
            CompiledModel compiled = new ModelCompiler().compile(def);
            assertThat(compiled).isNotNull();
        }

        @Test
        void shouldCompileNotEqualInStockRate() {
            // Pattern from change.mdl: <> used in INTEG rate expression
            String mdl = """
                    counter = INTEG(IF THEN ELSE(level <> 0, 1 / TIME STEP, 0), 0)
                    \t~\tDimensionless
                    \t~\t
                    \t|

                    level = 5
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
            ModelDefinition def = result.definition();

            CompiledModel compiled = new ModelCompiler().compile(def);
            assertThat(compiled).isNotNull();
        }
    }

    @Nested
    @DisplayName("MESSAGE and SIMULTANEOUS no-op handling (#498)")
    class MessageAndSimultaneousImport {

        @Test
        void shouldCompileModelWithMessageFunction() {
            String mdl = """
                    msg = IF THEN ELSE(x > 5, MESSAGE(alert, x), 0)
                    \t~\tDimensionless
                    \t~\t
                    \t|

                    x = 10
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
            ModelDefinition def = result.definition();

            // MESSAGE should be stripped, allowing compilation
            CompiledModel compiled = new ModelCompiler().compile(def);
            assertThat(compiled).isNotNull();
        }

        @Test
        void shouldCompileModelWithSimultaneousFunction() {
            String mdl = """
                    solver_hint = SIMULTANEOUS(0, 2)
                    \t~\tDimensionless
                    \t~\t
                    \t|

                    x = 5
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
            ModelDefinition def = result.definition();

            CompiledModel compiled = new ModelCompiler().compile(def);
            assertThat(compiled).isNotNull();
        }
    }

    @Nested
    @DisplayName("Subscript expansion (#495)")
    class SubscriptExpansion {

        @Test
        void shouldExpandSubscriptedConstant() {
            // Subscripted variable with comma-separated constant values
            String mdl = """
                    Region : North, South
                    \t~\t
                    \t~\t
                    \t|

                    Population[Region] = 100, 200
                    \t~\tPeople
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
            ModelDefinition def = result.definition();

            // Should expand into Population_North = 100 and Population_South = 200
            assertThat(def.variables().stream()
                    .filter(a -> a.name().equals("Population North"))
                    .findFirst()).isPresent();
            assertThat(def.variables().stream()
                    .filter(a -> a.name().equals("Population South"))
                    .findFirst()).isPresent();
        }

        @Test
        void shouldExpandSubscriptedFormula() {
            // Subscripted variable with formula referencing the dimension
            String mdl = """
                    tub : low tub, high tub
                    \t~\t
                    \t~\t
                    \t|

                    capacity[tub] = 100
                    \t~\tLiters
                    \t~\t
                    \t|

                    fill rate[tub] = capacity[tub] * 0.1
                    \t~\tLiters/Day
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
            ModelDefinition def = result.definition();

            // fill rate[tub] should be expanded into fill_rate_low_tub and fill_rate_high_tub
            // with capacity[tub] → capacity_low_tub / capacity_high_tub
            VariableDef lowFill = def.variables().stream()
                    .filter(a -> a.name().equals("fill rate low tub"))
                    .findFirst().orElseThrow();
            assertThat(lowFill.equation()).isEqualTo("capacity_low_tub * 0.1");

            VariableDef highFill = def.variables().stream()
                    .filter(a -> a.name().equals("fill rate high tub"))
                    .findFirst().orElseThrow();
            assertThat(highFill.equation()).isEqualTo("capacity_high_tub * 0.1");
        }

        @Test
        void shouldCompileExpandedSubscriptModel() {
            // End-to-end: subscripted model should compile and simulate
            String mdl = """
                    Region : North, South
                    \t~\t
                    \t~\t
                    \t|

                    initial pop[Region] = 100, 200
                    \t~\tPeople
                    \t~\t
                    \t|

                    growth rate[Region] = 0.05, 0.03
                    \t~\t1/Year
                    \t~\t
                    \t|

                    Population[Region] = INTEG(births[Region], initial pop[Region])
                    \t~\tPeople
                    \t~\t
                    \t|

                    births[Region] = Population[Region] * growth rate[Region]
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

            // Should have 2 stocks (Population_North, Population_South)
            assertThat(def.stocks()).hasSize(2);

            // Should compile and simulate
            CompiledModel compiled = new ModelCompiler().compile(def);
            assertThat(compiled).isNotNull();

            Simulation sim = compiled.createSimulation();
            sim.execute();

            // Both populations should have grown
            for (Stock stock : compiled.getModel().getStocks()) {
                assertThat(stock.getValue()).isGreaterThan(100.0);
            }
        }

        @Test
        void shouldExpandSubscriptedStockWithPerLabelInit() {
            String mdl = """
                    Color : red, blue
                    \t~\t
                    \t~\t
                    \t|

                    Level[Color] = INTEG(rate[Color], 10)
                    \t~\tUnits
                    \t~\t
                    \t|

                    rate[Color] = 1
                    \t~\tUnits/Day
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
            ModelDefinition def = result.definition();

            assertThat(def.stocks()).hasSize(2);
            assertThat(def.stocks().stream().map(StockDef::name))
                    .containsExactlyInAnyOrder("Level red", "Level blue");

            CompiledModel compiled = new ModelCompiler().compile(def);
            assertThat(compiled).isNotNull();
        }
    }

    @Nested
    @DisplayName("Rate term decomposition")
    class RateTermDecomposition {

        @Test
        void shouldSplitSimpleAdditionAndSubtraction() {
            var terms = VensimImporter.splitRateTerms("births - deaths");
            assertThat(terms).hasSize(2);
            assertThat(terms.get(0).expr()).isEqualTo("births");
            assertThat(terms.get(0).positive()).isTrue();
            assertThat(terms.get(1).expr()).isEqualTo("deaths");
            assertThat(terms.get(1).positive()).isFalse();
        }

        @Test
        void shouldSplitMultipleTerms() {
            var terms = VensimImporter.splitRateTerms("a + b - c + d");
            assertThat(terms).hasSize(4);
            assertThat(terms.get(0).positive()).isTrue();
            assertThat(terms.get(1).positive()).isTrue();
            assertThat(terms.get(2).positive()).isFalse();
            assertThat(terms.get(3).positive()).isTrue();
        }

        @Test
        void shouldHandleLeadingMinus() {
            var terms = VensimImporter.splitRateTerms("-a + b");
            assertThat(terms).hasSize(2);
            assertThat(terms.get(0).expr()).isEqualTo("a");
            assertThat(terms.get(0).positive()).isFalse();
            assertThat(terms.get(1).expr()).isEqualTo("b");
            assertThat(terms.get(1).positive()).isTrue();
        }

        @Test
        void shouldReturnNullForSingleTerm() {
            assertThat(VensimImporter.splitRateTerms("births")).isNull();
        }

        @Test
        void shouldReturnNullForNull() {
            assertThat(VensimImporter.splitRateTerms(null)).isNull();
        }

        @Test
        void shouldReturnNullForBlank() {
            assertThat(VensimImporter.splitRateTerms("  ")).isNull();
        }

        @Test
        void shouldNotSplitInsideParens() {
            var terms = VensimImporter.splitRateTerms("f(a + b) - c");
            assertThat(terms).hasSize(2);
            assertThat(terms.get(0).expr()).isEqualTo("f(a + b)");
            assertThat(terms.get(1).expr()).isEqualTo("c");
        }
    }

    @Nested
    @DisplayName("Non-constant SMOOTHI initial value (#514)")
    class NonConstantSmoothIInitial {

        @Test
        void shouldCompileModelWithSmoothIReferencingAux() {
            String mdl = """
                    smoothed = SMOOTHI(input, 5, normal price)
                    \t~\tDollars
                    \t~\t
                    \t|

                    input = 100
                    \t~\tDollars
                    \t~\t
                    \t|

                    normal price = 50
                    \t~\tDollars
                    \t~\t
                    \t|
                    """;
            ImportResult result = importer.importModel(mdl, "test");
            CompiledModel compiled = new ModelCompiler().compile(result.definition());
            Simulation sim = compiled.createSimulation();
            sim.execute();
            assertThat(compiled).isNotNull();
        }
    }

    @Nested
    @DisplayName("SAMPLE IF TRUE and FIND ZERO (#512)")
    class SampleIfTrueAndFindZeroImport {

        @Test
        void shouldCompileModelWithSampleIfTrue() {
            String mdl = """
                    sensor = SAMPLE IF TRUE(switch > 0, input, 0)
                    \t~\tUnits
                    \t~\t
                    \t|

                    switch = 1
                    \t~\tDimensionless
                    \t~\t
                    \t|

                    input = 42
                    \t~\tUnits
                    \t~\t
                    \t|
                    """;
            ImportResult result = importer.importModel(mdl, "test");
            assertThat(result.warnings()).noneMatch(w -> w.contains("Unsupported"));

            CompiledModel compiled = new ModelCompiler().compile(result.definition());
            Simulation sim = compiled.createSimulation();
            sim.execute();
            assertThat(compiled).isNotNull();
        }

        @Test
        void shouldCompileModelWithFindZero() {
            String mdl = """
                    root = FIND ZERO(x - 5, x, 0, 10)
                    \t~\tUnits
                    \t~\t
                    \t|

                    x = 0
                    \t~\tUnits
                    \t~\t
                    \t|
                    """;
            ImportResult result = importer.importModel(mdl, "test");
            assertThat(result.warnings()).noneMatch(w -> w.contains("Unsupported"));

            CompiledModel compiled = new ModelCompiler().compile(result.definition());
            Simulation sim = compiled.createSimulation();
            sim.execute();

            // FIND ZERO should find x=5 where x-5=0
            var rootVar = compiled.getModel().getVariables().stream()
                    .filter(v -> v.getName().contains("root"))
                    .findFirst()
                    .orElseThrow();
            assertThat(rootVar.getValue()).isCloseTo(5.0, org.assertj.core.data.Offset.offset(0.01));
        }
    }

    @Nested
    @DisplayName("ACTIVE INITIAL handling (#513)")
    class ActiveInitialImport {

        @Test
        void shouldCompileModelWithActiveInitial() {
            String mdl = """
                    total market= ACTIVE INITIAL(
                    Potential Customers + Customers, Potential Customers)
                    \t~\tPeople
                    \t~\t
                    \t|

                    Potential Customers = 1000
                    \t~\tPeople
                    \t~\t
                    \t|

                    Customers = 500
                    \t~\tPeople
                    \t~\t
                    \t|
                    """;
            ImportResult result = importer.importModel(mdl, "test");
            ModelDefinition def = result.definition();

            // ACTIVE INITIAL should resolve to first arg: Potential Customers + Customers
            var v = def.variables().stream()
                    .filter(a -> a.name().contains("total market"))
                    .findFirst();
            assertThat(v).isPresent();
            assertThat(v.get().equation()).isEqualTo("Potential_Customers + Customers");

            // Should compile without errors
            CompiledModel compiled = new ModelCompiler().compile(def);
            assertThat(compiled).isNotNull();
        }

        @Test
        void shouldCompileModelWithActiveInitialAndIfThenElse() {
            String mdl = """
                    proactive replacements = ACTIVE INITIAL(IF THEN ELSE(avail > 0, avail, 0), 0)
                    \t~\tUnits
                    \t~\t
                    \t|

                    avail = 50
                    \t~\tUnits
                    \t~\t
                    \t|
                    """;
            ImportResult result = importer.importModel(mdl, "test");
            ModelDefinition def = result.definition();

            var v = def.variables().stream()
                    .filter(a -> a.name().contains("proactive"))
                    .findFirst();
            assertThat(v).isPresent();
            assertThat(v.get().equation()).isEqualTo("IF(avail > 0, avail, 0)");
        }
    }

    @Nested
    @DisplayName("Non-monotonic lookup table handling (#511)")
    class NonMonotonicLookup {

        @Test
        void shouldSortNonMonotonicLookupXValues() {
            String mdl = """
                    my table(
                    [(0,0)-(10,10)],(0,1),(3,5),(2,3),(5,8),(4,6))
                    \t~\t
                    \t~\t
                    \t|

                    result = my table(Time)
                    \t~\t
                    \t~\t
                    \t|
                    """;
            ImportResult result = importer.importModel(mdl, "test");
            ModelDefinition def = result.definition();

            assertThat(def.lookupTables()).hasSize(1);
            LookupTableDef lookup = def.lookupTables().get(0);
            // x-values should be sorted: 0, 2, 3, 4, 5
            assertThat(lookup.xValues()).containsExactly(0, 2, 3, 4, 5);
            assertThat(lookup.yValues()).containsExactly(1, 3, 5, 6, 8);
            assertThat(result.warnings()).anyMatch(w -> w.contains("sorted non-monotonic"));
        }

        @Test
        void shouldSortAndDeduplicateLookupXValues() {
            String mdl = """
                    my table(
                    [(0,0)-(10,10)],(0,1),(5.5,7),(3,5),(5.0,6),(5.5,8))
                    \t~\t
                    \t~\t
                    \t|

                    result = my table(Time)
                    \t~\t
                    \t~\t
                    \t|
                    """;
            ImportResult result = importer.importModel(mdl, "test");
            ModelDefinition def = result.definition();

            assertThat(def.lookupTables()).hasSize(1);
            LookupTableDef lookup = def.lookupTables().get(0);
            // Sorted: 0, 3, 5.0, 5.5, 5.5 → deduplicated: 0, 3, 5.0, 5.5
            assertThat(lookup.xValues()).containsExactly(0, 3, 5.0, 5.5);
            // Last y for x=5.5 is 8 (from the second 5.5 entry)
            assertThat(lookup.yValues()).containsExactly(1, 5, 6, 8);
            assertThat(result.warnings()).anyMatch(w -> w.contains("sorted non-monotonic")
                    && w.contains("duplicate"));
        }

        @Test
        void shouldCompileModelWithNonMonotonicLookup() {
            String mdl = """
                    my table(
                    [(0,0)-(10,10)],(0,0),(5,10),(3,6),(10,20))
                    \t~\t
                    \t~\t
                    \t|

                    output = my table(Time)
                    \t~\t
                    \t~\t
                    \t|
                    """;
            ImportResult result = importer.importModel(mdl, "test");
            CompiledModel compiled = new ModelCompiler().compile(result.definition());
            Simulation sim = compiled.createSimulation();
            sim.execute();
            // Model should compile and run without errors
            assertThat(compiled).isNotNull();
        }
    }
}
