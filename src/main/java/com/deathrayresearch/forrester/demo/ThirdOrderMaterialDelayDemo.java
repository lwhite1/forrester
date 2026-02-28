package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.io.CsvSubscriber;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Formula;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.HOUR;
import static com.deathrayresearch.forrester.measure.Units.THING;

/**
 * Demonstrates a third-order material delay as a chain of three first-order delays.
 *
 * <p>Items enter Step 1, flow to Step 2, then Step 3, each with its own average activity time
 * (7 h, 6.3 h, 3.2 h). A Total WIP variable tracks combined inventory across all stages. The
 * cascaded stages smooth output more than a single first-order delay, producing a bell-shaped
 * throughput response to a step input.
 */
public class ThirdOrderMaterialDelayDemo {

    public static void main(String[] args) {
        new ThirdOrderMaterialDelayDemo().run();
    }

    public void run() {

        Model model = new Model("Third order material delay");
        model.setComment("A 3rd order material delay is a chain of three first order material delays. Each of the " +
                "individual first order delays is based on " +
                "the assumption that the stock is completely mixed, like the water in a tub, so FIFO is not possible. " +
                "The outflow of the first delay is the input to the second delay, and so on");

        Stock step1 = new Stock("Step 1", 100, THING);
        Stock step2 = new Stock("Step 2", 0, THING);
        Stock step3 = new Stock("Step 3", 0, THING);

        Flow demand = Flow.create("Process Demand", DAY, () -> new Quantity(48, THING));

        Flow step1Delay = Flow.create("Step 1 delay", HOUR, () -> {
            double averageDelay = 7; // 7 hour average activity time for this step
            return new Quantity(
                    Math.min(step1.getValue(), step1.getValue()/averageDelay), THING);
        });

        Flow step2Delay = Flow.create("Step 2 delay", HOUR, () -> {
            double averageDelay = 6.3; // 6.3 hour average activity time for this step
            return new Quantity(
                    Math.min(step2.getValue(), step2.getValue()/ averageDelay), THING);
        });

        Flow step3Delay = Flow.create("Step 3 delay", HOUR, () -> {
            double averageDelay = 3.2; // 3.2 hour average activity time for this step
            return new Quantity(
                    Math.min(step3.getValue(), step3.getValue()/ averageDelay), THING);
        });

        step1.addInflow(demand);
        step1.addOutflow(step1Delay);

        step2.addInflow(step1Delay);
        step2.addOutflow(step2Delay);

        step3.addInflow(step2Delay);
        step3.addOutflow(step3Delay);

        Variable totalWIP = new Variable("Total WIP", THING, new Formula() {
            @Override
            public double getCurrentValue() {
                return step1.getValue() + step2.getValue() + step3.getValue();
            }
        });

        Variable pipelineOutput = new Variable("Pipeline Output Rate", THING, new Formula() {
            @Override
            public double getCurrentValue() {
                return step3Delay.flowPerTimeUnit(HOUR).getValue();
            }
        });

        model.addStock(step1);
        model.addStock(step2);
        model.addStock(step3);

        model.addVariable(totalWIP);
        model.addVariable(pipelineOutput);

        Simulation run = new Simulation(model, HOUR, Times.hours( 48));
        run.addEventHandler(new StockLevelChartViewer());
        run.addEventHandler(new CsvSubscriber("3rd order.csv"));
        run.execute();
    }
}
