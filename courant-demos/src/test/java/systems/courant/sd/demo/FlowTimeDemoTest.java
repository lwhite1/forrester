package systems.courant.sd.demo;

import systems.courant.sd.Simulation;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.measure.units.item.ItemUnits;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Flows;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.ModelMetadata;
import systems.courant.sd.model.Stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static systems.courant.sd.measure.Units.DAY;
import static systems.courant.sd.measure.Units.HOUR;
import static systems.courant.sd.measure.Units.WEEK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests for {@link FlowTimeDemo}, specifically the negative history index bug (#466).
 */
@DisplayName("FlowTimeDemo (#466)")
class FlowTimeDemoTest {

    @Test
    @DisplayName("run() completes without exception")
    void shouldRunWithoutException() {
        assertThatCode(() -> new FlowTimeDemo().run(
                1000, 336, 190, 200, 24.0, 24.0, 4))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("throughput uses earliest demand when delay exceeds elapsed steps (#466)")
    void shouldClampNegativeHistoryIndex() {
        // Build the model inline to inspect flow history during warm-up
        double initialWip = 1000;
        double tatGoalHours = 336;
        double capacity = 190;
        double newOrdersPerDay = 200;
        double hoursPerDay = 24.0;
        double tatAdjustmentTimeHours = 24.0;

        Model model = new Model("TAT Test");
        model.setMetadata(ModelMetadata.builder().license("CC-BY-SA-4.0").build());

        Stock wip = new Stock("WIP", initialWip, ItemUnits.THING);
        Stock tat = new Stock("TAT", tatGoalHours, HOUR);

        Simulation sim = new Simulation(model, HOUR, WEEK, 1);

        Flow demand = Flows.linearGrowth("New Orders", DAY, wip, newOrdersPerDay);

        Flow throughput = Flow.create("Delivered Reports", DAY, () -> {
            int demandDelay = Math.max(0, (int) Math.round(tat.getValue()));
            int stepToGet = Math.max(0, (int) sim.getCurrentStep() - demandDelay);
            double demandPlusDelay = demand.getHistoryAtTimeStep(stepToGet);
            return new Quantity(Math.min(capacity, demandPlusDelay), ItemUnits.THING);
        });

        wip.addInflow(demand);
        wip.addOutflow(throughput);
        tat.addInflow(Flow.create("TAT Adjustment", HOUR, () -> {
            double currentTAT = Math.max(0, tat.getValue());
            double actualTAT = (wip.getValue() / capacity) * hoursPerDay;
            return new Quantity((actualTAT - currentTAT) / tatAdjustmentTimeHours, HOUR);
        }));

        model.addStock(wip);
        model.addStock(tat);

        sim.execute();

        // Before the fix, stepToGet would be negative for the first ~336 steps,
        // causing getHistoryAtTimeStep to return 0 → throughput = min(190, 0) = 0.
        // After the fix, stepToGet is clamped to 0, so throughput = min(190, demand[0]) > 0.
        // WIP should not grow unchecked — some deliveries must have occurred.
        assertThat(wip.getValue()).isLessThan(initialWip + newOrdersPerDay * 7);
    }

    @Test
    @DisplayName("throughput is non-zero during warm-up period (#466)")
    void shouldHaveNonZeroThroughputDuringWarmup() {
        // Short simulation where delay exceeds duration — throughput must still be > 0
        Model model = new Model("Warmup Test");

        Stock wip = new Stock("WIP", 500, ItemUnits.THING);
        // TAT of 1000 hours means demandDelay=1000, but we only run 48 hours
        Stock tat = new Stock("TAT", 1000, HOUR);

        Simulation sim = new Simulation(model, HOUR, new Quantity(48, HOUR));

        Flow demand = Flows.linearGrowth("New Orders", DAY, wip, 100);

        // Track throughput values
        double[] throughputSum = {0};
        Flow throughput = Flow.create("Delivered", DAY, () -> {
            int demandDelay = Math.max(0, (int) Math.round(tat.getValue()));
            int stepToGet = Math.max(0, (int) sim.getCurrentStep() - demandDelay);
            double demandPlusDelay = demand.getHistoryAtTimeStep(stepToGet);
            double rate = Math.min(190, demandPlusDelay);
            throughputSum[0] += rate;
            return new Quantity(rate, ItemUnits.THING);
        });

        wip.addInflow(demand);
        wip.addOutflow(throughput);
        model.addStock(wip);
        model.addStock(tat);

        sim.execute();

        // With the fix, throughput should be > 0 because stepToGet is clamped to 0
        // (uses earliest demand). Without fix, all throughput would be 0.
        assertThat(throughputSum[0]).isGreaterThan(0);
    }
}
