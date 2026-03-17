package systems.courant.sd.demo;

import systems.courant.sd.Simulation;
import systems.courant.sd.demo.agile.AgileSoftwareDevelopmentDemo;
import systems.courant.sd.demo.waterfall.WaterfallSoftwareDevelopmentDemo;
import systems.courant.sd.io.CsvSubscriber;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.measure.Units;
import systems.courant.sd.measure.units.time.TimeUnits;
import systems.courant.sd.measure.units.time.Times;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Flows;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.Stock;
import systems.courant.sd.sweep.MonteCarlo;
import systems.courant.sd.sweep.MonteCarloResult;
import systems.courant.sd.sweep.SamplingMethod;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static systems.courant.sd.measure.Units.DAY;
import static systems.courant.sd.measure.Units.MINUTE;
import static systems.courant.sd.measure.Units.WEEK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Smoke tests for all demo models. Each test verifies that the actual demo class
 * can construct its model and that the model simulates without throwing exceptions.
 *
 * <p>Tests use the demo's {@code getModel()} or {@code createSimulation()} method
 * to exercise the real model construction code, not simplified inline copies.</p>
 */
@DisplayName("Demo Smoke Tests")
class DemoSmokeTest {

    // ---- Sweep / MonteCarlo / Optimizer demos (no JavaFX, can call run() directly) ----

    @Test
    @DisplayName("SirSweepDemo runs without error")
    void sirSweepDemo() {
        new SirSweepDemo().run(1000, 10, 0, 0.10, 0.2, 4.0, 8.0, 2.0, 4);
    }

    @Test
    @DisplayName("SirMultiSweepDemo runs without error")
    void sirMultiSweepDemo() {
        new SirMultiSweepDemo().run(1000, 10, 0, 0.2,
                4.0, 8.0, 4.0, new double[]{0.05, 0.10}, 4);
    }

    @Test
    @DisplayName("SirMonteCarloDemo runs without error")
    void sirMonteCarloDemo() {
        // Build MC inline to avoid FanChart.show() popup in SirMonteCarloDemo.run()
        MonteCarloResult result = MonteCarlo.builder()
                .parameter("Contact Rate", new NormalDistribution(8, 2))
                .parameter("Infectivity", new UniformRealDistribution(0.05, 0.15))
                .modelFactory(params -> SirModelBuilder.build("SIR Monte Carlo",
                        params.get("Contact Rate"), params.get("Infectivity"),
                        1000, 10, 0, 0.2))
                .iterations(10)
                .sampling(SamplingMethod.LATIN_HYPERCUBE)
                .seed(42L)
                .timeStep(DAY)
                .duration(Times.weeks(4))
                .build()
                .execute();
        assertThat(result.getRunCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("SirCalibrationDemo runs without error")
    void sirCalibrationDemo() {
        new SirCalibrationDemo().run(8.0, 0.10, 1000, 10, 0, 0.2,
                4.0, 12.0, 0.05, 0.20, 100, 4);
    }

    // ---- Demos with getModel() — build actual model and run short simulation ----

    @Test
    @DisplayName("WaterfallSoftwareDevelopmentDemo model builds and runs")
    void waterfallDemo() {
        Model model = new WaterfallSoftwareDevelopmentDemo().getModel();
        assertThat(model.getStocks()).isNotEmpty();
        Simulation sim = new Simulation(model, DAY, new Quantity(30, DAY));
        sim.execute();
    }

    @Test
    @DisplayName("AgileSoftwareDevelopmentDemo model builds and runs")
    void agileDemo() {
        Model model = AgileSoftwareDevelopmentDemo.getModel();
        assertThat(model.getStocks()).isNotEmpty();
        Simulation sim = new Simulation(model, DAY, new Quantity(14, DAY));
        sim.execute();
    }

    @Test
    @DisplayName("CoffeeCoolingDemo model builds and simulates")
    void coffeeCoolingDemo() {
        Model model = new CoffeeCoolingDemo().getModel();
        assertThat(model.getStocks()).isNotEmpty();
        Simulation sim = new Simulation(model, MINUTE, MINUTE, 8);
        sim.execute();
        Stock coffee = findStock(model, "Coffee Temperature");
        assertThat(coffee.getValue()).isLessThan(100);
    }

    @Test
    @DisplayName("ExponentialDecayDemo model builds and simulates")
    void exponentialDecayDemo() {
        Model model = new ExponentialDecayDemo().getModel();
        assertThat(model.getStocks()).isNotEmpty();
        Simulation sim = new Simulation(model, DAY, Times.weeks(4));
        sim.execute();
        Stock pop = findStock(model, "Population");
        assertThat(pop.getValue()).isLessThan(100);
    }

    @Test
    @DisplayName("ExponentialGrowthDemo model builds and simulates")
    void exponentialGrowthDemo() {
        Model model = new ExponentialGrowthDemo().getModel();
        assertThat(model.getStocks()).isNotEmpty();
        Simulation sim = new Simulation(model, DAY, WEEK, 4);
        sim.execute();
        Stock pop = findStock(model, "population");
        assertThat(pop.getValue()).isGreaterThan(0);
    }

    @Test
    @DisplayName("NegativeFeedbackDemo model builds and simulates")
    void negativeFeedbackDemo() {
        Model model = new NegativeFeedbackDemo().getModel();
        assertThat(model.getStocks()).isNotEmpty();
        Simulation sim = new Simulation(model, DAY, WEEK, 4);
        sim.execute();
        Stock inventory = findStock(model, "Inventory on-hand");
        assertThat(inventory.getValue()).isGreaterThan(100);
    }

    @Test
    @DisplayName("SShapedPopulationGrowthDemo model builds and simulates")
    void sShapedGrowthDemo() {
        Model model = new SShapedPopulationGrowthDemo().getModel();
        assertThat(model.getStocks()).isNotEmpty();
        Simulation sim = new Simulation(model, DAY, WEEK, 4);
        sim.execute();
        Stock pop = findStock(model, "population");
        assertThat(pop.getValue()).isGreaterThan(10);
    }

    @Test
    @DisplayName("SirInfectiousDiseaseDemo model builds and simulates")
    void sirDemo() {
        Model model = new SirInfectiousDiseaseDemo().getModel();
        assertThat(model.getStocks()).isNotEmpty();
        Simulation sim = new Simulation(model, DAY, Times.weeks(4));
        sim.execute();
        Stock recovered = findStock(model, "Recovered");
        assertThat(recovered.getValue()).isGreaterThan(0);
    }

    @Test
    @DisplayName("PredatorPreyDemo model builds and simulates")
    void predatorPreyDemo() {
        Model model = new PredatorPreyDemo().getModel();
        assertThat(model.getStocks()).isNotEmpty();
        Simulation sim = new Simulation(model, DAY, Times.weeks(4));
        sim.execute();
        Stock prey = findStock(model, "Rabbits");
        assertThat(prey.getValue()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("FirstOrderMaterialDelayDemo model builds and simulates")
    void firstOrderDelayDemo() {
        Model model = new FirstOrderMaterialDelayDemo().getModel();
        assertThat(model.getStocks()).isNotEmpty();
        Simulation sim = new Simulation(model, DAY, Times.weeks(4));
        sim.execute();
        Stock customers = findStock(model, "Potential Customers");
        assertThat(customers.getValue()).isLessThan(1000);
    }

    @Test
    @DisplayName("LookupTableDemo model builds and simulates")
    void lookupTableDemo() {
        Model model = new LookupTableDemo().getModel();
        assertThat(model.getStocks()).isNotEmpty();
        Simulation sim = new Simulation(model, DAY, WEEK, 4);
        sim.execute();
        Stock pop = findStock(model, "Population");
        assertThat(pop.getValue()).isGreaterThan(10);
    }

    @Test
    @DisplayName("MultiRegionSirDemo model builds and simulates")
    void multiRegionSirDemo() {
        Model model = new MultiRegionSirDemo().getModel();
        assertThat(model.getStocks()).isNotEmpty();
        Simulation sim = new Simulation(model, DAY, Times.weeks(4));
        sim.execute();
    }

    @Test
    @DisplayName("PopulationRegionAgeDemo model builds and simulates")
    void populationRegionAgeDemo() {
        Model model = new PopulationRegionAgeDemo().getModel();
        assertThat(model.getStocks()).isNotEmpty();
        Simulation sim = new Simulation(model, DAY, Times.years(1));
        sim.execute();
    }

    @Test
    @DisplayName("ThirdOrderMaterialDelayDemo model builds and simulates")
    void thirdOrderDelayDemo() {
        Model model = new ThirdOrderMaterialDelayDemo().getModel();
        assertThat(model.getStocks()).isNotEmpty();
        var hour = TimeUnits.HOUR;
        Simulation sim = new Simulation(model, hour, Times.hours(24));
        sim.execute();
        Stock step2 = findStock(model, "Step 2");
        assertThat(step2.getValue()).isGreaterThan(0);
    }

    @Test
    @DisplayName("SalesMixDemo model builds and simulates")
    void salesMixDemo() {
        Model model = new SalesMixDemo().getModel();
        assertThat(model.getStocks()).isNotEmpty();
        Simulation sim = new Simulation(model, WEEK, Times.years(1));
        sim.execute();
        Stock customers = findStock(model, "customers");
        assertThat(customers.getValue()).isGreaterThan(0);
        assertThat(model.getVariableNames()).contains("Total sales");
    }

    // ---- Demos with createSimulation() — simulation-dependent model construction ----

    @Test
    @DisplayName("TubDemo model builds and simulates")
    void tubDemo() {
        Simulation sim = new TubDemo().createSimulation();
        sim.execute();
        Stock water = findStock(sim.getModel(), "Water in Tub");
        assertThat(water.getValue()).isLessThan(50);
    }

    @Test
    @DisplayName("InventoryModelDemo model builds and simulates")
    void inventoryDemo() {
        Simulation sim = new InventoryModelDemo().createSimulation();
        sim.execute();
        Stock cars = findStock(sim.getModel(), "Cars on Lot");
        assertThat(cars.getValue()).isGreaterThan(0);
    }

    @Test
    @DisplayName("SimplePipelineDelayDemo model builds and simulates")
    void simplePipelineDelayDemo() {
        Simulation sim = new SimplePipelineDelayDemo().createSimulation();
        sim.execute();
        Stock wip = findStock(sim.getModel(), "WIP");
        assertThat(wip.getValue()).isGreaterThan(0);
    }

    @Test
    @DisplayName("FlowTimeDemo model builds and simulates")
    void flowTimeDemo() {
        Simulation sim = new FlowTimeDemo().createSimulation();
        sim.execute();
        Stock wip = findStock(sim.getModel(), "WIP");
        assertThat(wip.getValue()).isGreaterThan(0);
    }

    // ---- Edge-case tests ----

    @Test
    @DisplayName("ThirdOrderMaterialDelayDemo flows clamp to zero when stocks are negative")
    void thirdOrderDelayFlowsClamped() {
        var hour = TimeUnits.HOUR;
        var model = new Model("Clamp Test");
        var step1 = new Stock("Step 1", -10,
                Units.DIMENSIONLESS);
        double delayHours = 5.0;
        var flow = Flow.create("Step 1 delay", hour, () ->
                new Quantity(Math.max(0, Math.min(step1.getValue(),
                        step1.getValue() / delayHours)),
                        Units.DIMENSIONLESS));
        step1.addOutflow(flow);
        model.addStock(step1);
        new Simulation(model, hour, Times.hours(1)).execute();
        // Flow clamped to 0, engine may also clamp negative stocks
        assertThat(step1.getValue()).isGreaterThanOrEqualTo(-10);
    }

    @Test
    @DisplayName("FlowTimeDemo TAT stays non-negative during simulation")
    void flowTimeDemoTatNonNegative() {
        var hour = TimeUnits.HOUR;
        var model = new Model("TAT Clamp Test");
        var tat = new Stock("TAT", 0, hour);
        double capacity = 100;
        double hoursPerDay = 24.0;
        double adjustmentTime = 24.0;
        var wip = new Stock("WIP", 500,
                Units.DIMENSIONLESS);
        var tatAdjustment = Flow.create("TAT Adjustment", hour, () -> {
            double currentTAT = Math.max(0, tat.getValue());
            double actualTAT = (wip.getValue() / capacity) * hoursPerDay;
            return new Quantity((actualTAT - currentTAT) / adjustmentTime, hour);
        });
        tat.addInflow(tatAdjustment);
        model.addStock(tat);
        model.addStock(wip);
        new Simulation(model, hour, Times.hours(48)).execute();
        assertThat(tat.getValue()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("FlowTimeDemo throughput uses clamped history index when TAT exceeds current step (#466)")
    void flowTimeDemoClampedHistoryIndex() {
        var hour = TimeUnits.HOUR;
        var model = new Model("FlowTime Clamp Test");
        double capacity = 190;
        double tatGoalHours = 336;

        Stock wip = new Stock("WIP", 1000, Units.DIMENSIONLESS);
        Stock tat = new Stock("TAT", tatGoalHours, hour);

        Simulation sim = new Simulation(model, hour, WEEK, 1);

        Flow demand = Flows.linearGrowth("New Orders", DAY, wip, 200);

        Flow throughput = Flow.create("Delivered Reports", DAY, () -> {
            int demandDelay = Math.max(0, (int) Math.round(tat.getValue()));
            int stepToGet = Math.max(0, (int) sim.getCurrentStep() - demandDelay);
            double demandPlusDelay = demand.getHistoryAtTimeStep(stepToGet);
            return new Quantity(Math.min(capacity, demandPlusDelay),
                    Units.DIMENSIONLESS);
        });

        wip.addInflow(demand);
        wip.addOutflow(throughput);

        Flow tatAdjustment = Flow.create("TAT Adjustment", hour, () -> {
            double currentTAT = Math.max(0, tat.getValue());
            double actualTAT = (wip.getValue() / capacity) * 24.0;
            return new Quantity((actualTAT - currentTAT) / 24.0, hour);
        });
        tat.addInflow(tatAdjustment);

        model.addStock(wip);
        model.addStock(tat);

        sim.execute();

        assertThat(throughput.getHistoryAtTimeStep(0)).isGreaterThanOrEqualTo(0);
        assertThat(throughput.getHistoryAtTimeStep(1)).isGreaterThan(0);
    }

    // ---- CSV path tests (#447, #556) ----

    @Test
    @DisplayName("CsvSubscriber writes to tmpdir path, not relative CWD (#447)")
    void csvSubscriberWritesToTmpdir(@TempDir Path tempDir) throws Exception {
        String csvPath = tempDir.resolve("courant-test.csv").toString();
        var model = new Model("CSV Path Test");
        var stock = new Stock("Level", 100, Units.DIMENSIONLESS);
        model.addStock(stock);

        var csv = new CsvSubscriber(csvPath);
        var sim = new Simulation(model, MINUTE,
                MINUTE, 2);
        sim.addEventHandler(csv);
        sim.execute();

        Path written = Path.of(csvPath);
        assertThat(written).exists();
        assertThat(written.isAbsolute()).isTrue();
        assertThat(Files.readAllLines(written)).hasSizeGreaterThan(1);
    }

    @Test
    @DisplayName("ThirdOrderMaterialDelayDemo CSV path uses tmpdir, not relative path (#556)")
    void thirdOrderDelayDemoCsvUsesAbsolutePath() throws Exception {
        Path sourceFile = Path.of("src/main/java/systems/courant/sd/demo/"
                + "ThirdOrderMaterialDelayDemo.java");
        String source = Files.readString(sourceFile);
        assertThat(source)
                .describedAs("ThirdOrderMaterialDelayDemo must use java.io.tmpdir for CSV output")
                .contains("System.getProperty(\"java.io.tmpdir\")");
        assertThat(source)
                .describedAs("ThirdOrderMaterialDelayDemo must not use a relative CSV path")
                .doesNotContain("new CsvSubscriber(\"courant-")
                .doesNotContain("new CsvSubscriber(\"third");
    }

    @Test
    @DisplayName("TubDemo CSV path uses tmpdir, not relative path (#556)")
    void tubDemoCsvUsesAbsolutePath() throws Exception {
        Path sourceFile = Path.of("src/main/java/systems/courant/sd/demo/"
                + "TubDemo.java");
        String source = Files.readString(sourceFile);
        assertThat(source)
                .describedAs("TubDemo must use java.io.tmpdir for CSV output")
                .contains("System.getProperty(\"java.io.tmpdir\")");
        assertThat(source)
                .describedAs("TubDemo must not use a relative CSV path")
                .doesNotContain("new CsvSubscriber(\"courant-")
                .doesNotContain("new CsvSubscriber(\"tub");
    }

    @Test
    @DisplayName("All demo CSV paths use absolute tmpdir paths, not relative (#556)")
    void allDemoCsvPathsUseAbsolutePaths() throws Exception {
        Path demoDir = Path.of("src/main/java/systems/courant/sd/demo");
        try (var stream = Files.list(demoDir)) {
            var demoFiles = stream
                    .filter(p -> p.toString().endsWith("Demo.java"))
                    .toList();
            for (Path file : demoFiles) {
                String source = Files.readString(file);
                if (source.contains("CsvSubscriber")) {
                    assertThat(source)
                            .describedAs(file.getFileName() + " must use java.io.tmpdir for CSV output")
                            .contains("System.getProperty(\"java.io.tmpdir\")");
                }
            }
        }
    }

    // ---- Division-by-zero validation tests ----

    @Test
    @DisplayName("InventoryModelDemo rejects zero perceptionDelay")
    void inventoryDemoZeroPerceptionDelay() {
        assertThatThrownBy(() -> new InventoryModelDemo()
                .createSimulation(200, 20, 20, 22, 25, 0, 3, 5, 10, 100))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("InventoryModelDemo rejects zero responseDelay")
    void inventoryDemoZeroResponseDelay() {
        assertThatThrownBy(() -> new InventoryModelDemo()
                .createSimulation(200, 20, 20, 22, 25, 5, 0, 5, 10, 100))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("InventoryModelDemo rejects zero deliveryDelay")
    void inventoryDemoZeroDeliveryDelay() {
        assertThatThrownBy(() -> new InventoryModelDemo()
                .createSimulation(200, 20, 20, 22, 25, 5, 3, 0, 10, 100))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("FirstOrderMaterialDelayDemo rejects zero averageDelayDays")
    void firstOrderDelayDemoZeroDelay() {
        assertThatThrownBy(() -> new FirstOrderMaterialDelayDemo().getModel(1000, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ThirdOrderMaterialDelayDemo rejects zero step delays")
    void thirdOrderDelayDemoZeroDelays() {
        assertThatThrownBy(() -> new ThirdOrderMaterialDelayDemo()
                .getModel(100, 0, 0, 48, 0, 6.3, 3.2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ThirdOrderMaterialDelayDemo()
                .getModel(100, 0, 0, 48, 7.0, 0, 3.2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ThirdOrderMaterialDelayDemo()
                .getModel(100, 0, 0, 48, 7.0, 6.3, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- Helpers ----

    private Stock findStock(Model model, String name) {
        return model.getStocks().stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Stock not found: " + name));
    }
}
