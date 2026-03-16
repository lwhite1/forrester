package systems.courant.sd.model;

import systems.courant.sd.measure.Quantity;

import org.junit.jupiter.api.Test;

import static systems.courant.sd.measure.Units.DAY;
import static systems.courant.sd.measure.Units.THING;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FlowsTest {

    // constant

    @Test
    public void constantShouldReturnFixedQuantity() {
        Quantity q = new Quantity(5, THING);
        Flow flow = Flows.constant("arrivals", DAY, q);
        assertEquals(5, flow.flowPerTimeUnit(DAY).getValue(), 0.001);
    }

    @Test
    public void constantShouldReturnSameValueOnRepeatedCalls() {
        Quantity q = new Quantity(3, THING);
        Flow flow = Flows.constant("arrivals", DAY, q);
        assertEquals(3, flow.flowPerTimeUnit(DAY).getValue(), 0.001);
        assertEquals(3, flow.flowPerTimeUnit(DAY).getValue(), 0.001);
    }

    // linearGrowth

    @Test
    public void linearGrowthShouldReturnConstantAmount() {
        Stock stock = new Stock("items", 100, THING);
        Flow flow = Flows.linearGrowth("growth", DAY, stock, 10);
        assertEquals(10, flow.flowPerTimeUnit(DAY).getValue(), 0.001);
    }

    @Test
    public void linearGrowthShouldNotDependOnStockValue() {
        Stock stock = new Stock("items", 100, THING);
        Flow flow = Flows.linearGrowth("growth", DAY, stock, 10);
        assertEquals(10, flow.flowPerTimeUnit(DAY).getValue(), 0.001);
        stock.setValue(500);
        assertEquals(10, flow.flowPerTimeUnit(DAY).getValue(), 0.001);
    }

    // exponentialGrowth

    @Test
    public void exponentialGrowthShouldMultiplyStockByRate() {
        Stock stock = new Stock("population", 100, THING);
        Flow flow = Flows.exponentialGrowth("births", DAY, stock, 0.04);
        assertEquals(4.0, flow.flowPerTimeUnit(DAY).getValue(), 0.001);
    }

    @Test
    public void exponentialGrowthShouldTrackStockChanges() {
        Stock stock = new Stock("population", 100, THING);
        Flow flow = Flows.exponentialGrowth("births", DAY, stock, 0.04);
        assertEquals(4.0, flow.flowPerTimeUnit(DAY).getValue(), 0.001);
        stock.setValue(200);
        assertEquals(8.0, flow.flowPerTimeUnit(DAY).getValue(), 0.001);
    }

    // exponentialGrowthWithLimit

    @Test
    public void exponentialGrowthWithLimitShouldApplyLogisticFormula() {
        Stock stock = new Stock("population", 100, THING);
        Flow flow = Flows.exponentialGrowthWithLimit("births", DAY, stock, 0.04, 1000);
        // 100 * 0.04 * (1 - 100/1000) = 4 * 0.9 = 3.6
        assertEquals(3.6, flow.flowPerTimeUnit(DAY).getValue(), 0.001);
    }

    @Test
    public void exponentialGrowthWithLimitShouldApproachZeroAtLimit() {
        Stock stock = new Stock("population", 999, THING);
        Flow flow = Flows.exponentialGrowthWithLimit("births", DAY, stock, 0.04, 1000);
        // 999 * 0.04 * (1 - 999/1000) = 39.96 * 0.001 = 0.03996
        assertEquals(0.03996, flow.flowPerTimeUnit(DAY).getValue(), 0.001);
    }

    @Test
    public void exponentialGrowthWithLimitShouldBeZeroAtExactLimit() {
        Stock stock = new Stock("population", 1000, THING);
        Flow flow = Flows.exponentialGrowthWithLimit("births", DAY, stock, 0.04, 1000);
        assertEquals(0.0, flow.flowPerTimeUnit(DAY).getValue(), 0.001);
    }

    // pipelineDelay

    @Test
    public void pipelineDelayShouldReturnZeroBeforeDelayElapsed() {
        Stock wip = new Stock("WIP", 0, THING);
        Flow arrivals = Flows.constant("arrivals", DAY, new Quantity(5, THING));
        wip.addInflow(arrivals);

        int[] step = {0};
        Flow departures = Flows.pipelineDelay("departures", DAY, arrivals, () -> step[0], 3);

        // At step 0 with delay 3, reference step is -3, so history returns 0
        assertEquals(0.0, departures.flowPerTimeUnit(DAY).getValue(), 0.001);
    }

    @Test
    public void pipelineDelayShouldReplayHistoryAfterDelay() {
        Stock wip = new Stock("WIP", 0, THING);
        Flow arrivals = Flows.constant("arrivals", DAY, new Quantity(5, THING));
        wip.addInflow(arrivals);

        // Record some history on the arrivals flow
        arrivals.recordValue(new Quantity(5, THING)); // step 0
        arrivals.recordValue(new Quantity(5, THING)); // step 1
        arrivals.recordValue(new Quantity(5, THING)); // step 2

        int[] step = {3};
        Flow departures = Flows.pipelineDelay("departures", DAY, arrivals, () -> step[0], 3);

        // At step 3 with delay 3, reference step is 0 — should return 5.0
        assertEquals(5.0, departures.flowPerTimeUnit(DAY).getValue(), 0.001);
    }

    @Test
    public void pipelineDelayShouldHandleLongStepWithoutTruncation() {
        Stock wip = new Stock("WIP", 0, THING);
        Flow arrivals = Flows.constant("arrivals", DAY, new Quantity(5, THING));
        wip.addInflow(arrivals);

        // Step exceeds Integer.MAX_VALUE — reference step also exceeds int range
        long bigStep = Integer.MAX_VALUE + 10L;
        Flow departures = Flows.pipelineDelay("departures", DAY, arrivals, () -> bigStep, 3);

        // Reference step is beyond any recorded history, so should return 0 (not wrap negative)
        assertEquals(0.0, departures.flowPerTimeUnit(DAY).getValue(), 0.001);
    }

    // Flow name and time unit

    @Test
    public void factoryMethodsShouldPreserveFlowName() {
        Flow flow = Flows.constant("myFlow", DAY, new Quantity(1, THING));
        assertEquals("myFlow", flow.getName());
    }

    @Test
    public void factoryMethodsShouldPreserveTimeUnit() {
        Flow flow = Flows.exponentialGrowth("growth", DAY, new Stock("s", 10, THING), 0.1);
        assertEquals(DAY, flow.getTimeUnit());
    }
}
