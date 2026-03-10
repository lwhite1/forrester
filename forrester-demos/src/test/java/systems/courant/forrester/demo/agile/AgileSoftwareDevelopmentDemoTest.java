package systems.courant.forrester.demo.agile;

import systems.courant.forrester.Simulation;
import systems.courant.forrester.measure.units.time.TimeUnits;
import systems.courant.forrester.model.Model;
import systems.courant.forrester.sweep.RunResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static systems.courant.forrester.measure.Units.WEEK;

@DisplayName("AgileSoftwareDevelopmentDemo")
class AgileSoftwareDevelopmentDemoTest {

    private static final double PROJECT_SIZE = 500;
    private static final double TEAM_SIZE = 5;
    private static final double PRODUCTIVITY = 20;
    private static final double FRACTION_CORRECT = 0.80;
    private static final double SPRINT_PULL = 0.10;
    private static final double DEFECT_DISCOVERY = 0.40;
    private static final double DEFECT_FIX = 0.50;

    private Model model;

    @BeforeEach
    void setUp() {
        model = AgileSoftwareDevelopmentDemo.getModel(
                PROJECT_SIZE, TEAM_SIZE, PRODUCTIVITY,
                FRACTION_CORRECT, SPRINT_PULL, DEFECT_DISCOVERY, DEFECT_FIX);
    }

    @Nested
    @DisplayName("Model structure")
    class ModelStructure {

        @Test
        void shouldContainAllFiveStocks() {
            assertThat(model.getStockNames()).containsExactlyInAnyOrder(
                    "Product Backlog", "Sprint Backlog", "Completed Tasks",
                    "Latent Defects", "Known Defects");
        }

        @Test
        void shouldHaveCorrectInitialValues() {
            assertThat(stockValue("Product Backlog")).isEqualTo(PROJECT_SIZE);
            assertThat(stockValue("Sprint Backlog")).isEqualTo(0);
            assertThat(stockValue("Completed Tasks")).isEqualTo(0);
            assertThat(stockValue("Latent Defects")).isEqualTo(0);
            assertThat(stockValue("Known Defects")).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Simulation behavior")
    class SimulationBehavior {

        @Test
        void shouldDrainProductBacklogOverTime() {
            runSimulation(52);
            assertThat(stockValue("Product Backlog"))
                    .as("Product backlog should drain significantly")
                    .isLessThan(PROJECT_SIZE * 0.05);
        }

        @Test
        void shouldAccumulateCompletedWork() {
            runSimulation(52);
            assertThat(stockValue("Completed Tasks"))
                    .as("Completed tasks should accumulate")
                    .isGreaterThan(PROJECT_SIZE * 0.50);
        }

        @Test
        void shouldFlowWorkThroughSprintBacklog() {
            runSimulation(5);
            assertThat(stockValue("Sprint Backlog"))
                    .as("Sprint backlog should have received work from product backlog")
                    .isGreaterThan(0);
        }

        @Test
        void shouldGenerateDefectsDuringDevelopment() {
            runSimulation(10);
            double latent = stockValue("Latent Defects");
            double known = stockValue("Known Defects");
            assertThat(latent + known)
                    .as("Defects should be generated as work completes")
                    .isGreaterThan(0);
        }

        @Test
        void shouldDiscoverLatentDefects() {
            runSimulation(20);
            assertThat(stockValue("Known Defects"))
                    .as("Some latent defects should have been discovered")
                    .isGreaterThan(0);
        }

        @Test
        void shouldResolveDefectsAfterDevelopmentEnds() {
            // Run long enough for backlog to drain and defects to decay
            runSimulation(104);
            assertThat(stockValue("Known Defects"))
                    .as("Known defects should be mostly resolved after 2 years")
                    .isLessThan(1.0);
            assertThat(stockValue("Latent Defects"))
                    .as("Latent defects should be mostly resolved after 2 years")
                    .isLessThan(1.0);
        }

        @Test
        void shouldConserveTotalWork() {
            // Work is conserved: product backlog + sprint backlog + completed = project size
            // (defects are separate — they don't consume work units)
            runSimulation(52);
            double total = stockValue("Product Backlog")
                    + stockValue("Sprint Backlog")
                    + stockValue("Completed Tasks");
            assertThat(total).isCloseTo(PROJECT_SIZE, org.assertj.core.data.Offset.offset(0.1));
        }
    }

    @Nested
    @DisplayName("Team size effects")
    class TeamSizeEffects {

        @Test
        void shouldCompleteWorkFasterWithLargerTeam() {
            // Use high pull fraction so sprint backlog exceeds small team capacity
            double highPull = 0.50;
            Model smallTeam = AgileSoftwareDevelopmentDemo.getModel(
                    PROJECT_SIZE, 3, PRODUCTIVITY, FRACTION_CORRECT,
                    highPull, DEFECT_DISCOVERY, DEFECT_FIX);
            Model largeTeam = AgileSoftwareDevelopmentDemo.getModel(
                    PROJECT_SIZE, 10, PRODUCTIVITY, FRACTION_CORRECT,
                    highPull, DEFECT_DISCOVERY, DEFECT_FIX);

            runModel(smallTeam, 10);
            runModel(largeTeam, 10);

            double smallCompleted = getStockValue(smallTeam, "Completed Tasks");
            double largeCompleted = getStockValue(largeTeam, "Completed Tasks");
            assertThat(largeCompleted)
                    .as("Larger team should complete more work")
                    .isGreaterThan(smallCompleted);
        }
    }

    @Nested
    @DisplayName("Quality effects")
    class QualityEffects {

        @Test
        void shouldProduceFewerDefectsWithHigherQuality() {
            Model lowQuality = AgileSoftwareDevelopmentDemo.getModel(
                    PROJECT_SIZE, TEAM_SIZE, PRODUCTIVITY, 0.60,
                    SPRINT_PULL, DEFECT_DISCOVERY, DEFECT_FIX);
            Model highQuality = AgileSoftwareDevelopmentDemo.getModel(
                    PROJECT_SIZE, TEAM_SIZE, PRODUCTIVITY, 0.95,
                    SPRINT_PULL, DEFECT_DISCOVERY, DEFECT_FIX);

            runModel(lowQuality, 20);
            runModel(highQuality, 20);

            double lowDefects = getStockValue(lowQuality, "Latent Defects")
                    + getStockValue(lowQuality, "Known Defects");
            double highDefects = getStockValue(highQuality, "Latent Defects")
                    + getStockValue(highQuality, "Known Defects");
            assertThat(highDefects)
                    .as("Higher quality should produce fewer defects")
                    .isLessThan(lowDefects);
        }
    }

    @Nested
    @DisplayName("RunResult integration")
    class RunResultIntegration {

        @Test
        void shouldCaptureStockTimeSeries() {
            RunResult result = new RunResult(1.0);
            Simulation sim = new Simulation(model, TimeUnits.DAY, WEEK, 10);
            sim.addEventHandler(result);
            sim.execute();

            assertThat(result.getStepCount()).isGreaterThan(0);
            assertThat(result.getStockNames()).contains("Product Backlog", "Completed Tasks");
            assertThat(result.getFinalStockValue("Product Backlog"))
                    .isLessThan(PROJECT_SIZE);
        }
    }

    private void runSimulation(int weeks) {
        Simulation sim = new Simulation(model, TimeUnits.DAY, WEEK, weeks);
        sim.execute();
    }

    private double stockValue(String name) {
        return getStockValue(model, name);
    }

    private static void runModel(Model m, int weeks) {
        Simulation sim = new Simulation(m, TimeUnits.DAY, WEEK, weeks);
        sim.execute();
    }

    private static double getStockValue(Model m, String name) {
        return m.getStocks().stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Stock not found: " + name))
                .getValue();
    }
}
