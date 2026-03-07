package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.io.CsvSubscriber;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.ModelMetadata;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.HOUR;
import static com.deathrayresearch.forrester.measure.Units.THING;

/**
 * Demonstrates a third-order material delay as a chain of three first-order delays.
 *
 * <p>Items enter Step 1, flow to Step 2, then Step 3, each with its own average activity time.
 * A Total WIP variable tracks combined inventory across all stages. The cascaded stages smooth
 * output more than a single first-order delay, producing a bell-shaped throughput response to
 * a step input.
 */
public class ThirdOrderMaterialDelayDemo {

    public static void main(String[] args) {
        double initialStep1 = 100;
        double initialStep2 = 0;
        double initialStep3 = 0;
        double processDemandPerDay = 48;
        double step1DelayHours = 7.0;
        double step2DelayHours = 6.3;
        double step3DelayHours = 3.2;
        double durationHours = 48;

        new ThirdOrderMaterialDelayDemo().run(initialStep1, initialStep2, initialStep3,
                processDemandPerDay, step1DelayHours, step2DelayHours, step3DelayHours,
                durationHours);
    }

    public void run(double initialStep1, double initialStep2, double initialStep3,
                    double processDemandPerDay, double step1DelayHours, double step2DelayHours,
                    double step3DelayHours, double durationHours) {

        Model model = new Model("Third order material delay");
        model.setMetadata(ModelMetadata.builder()
                .license("CC-BY-SA-4.0")
                .build());

        Stock step1 = new Stock("Step 1", initialStep1, THING);
        Stock step2 = new Stock("Step 2", initialStep2, THING);
        Stock step3 = new Stock("Step 3", initialStep3, THING);

        Flow demand = Flow.create("Process Demand", DAY,
                () -> new Quantity(processDemandPerDay, THING));

        Flow step1Delay = Flow.create("Step 1 delay", HOUR, () ->
                new Quantity(Math.min(step1.getValue(),
                        step1.getValue() / step1DelayHours), THING));

        Flow step2Delay = Flow.create("Step 2 delay", HOUR, () ->
                new Quantity(Math.min(step2.getValue(),
                        step2.getValue() / step2DelayHours), THING));

        Flow step3Delay = Flow.create("Step 3 delay", HOUR, () ->
                new Quantity(Math.min(step3.getValue(),
                        step3.getValue() / step3DelayHours), THING));

        step1.addInflow(demand);
        step1.addOutflow(step1Delay);

        step2.addInflow(step1Delay);
        step2.addOutflow(step2Delay);

        step3.addInflow(step2Delay);
        step3.addOutflow(step3Delay);

        Variable totalWIP = new Variable("Total WIP", THING,
                () -> step1.getValue() + step2.getValue() + step3.getValue());

        Variable pipelineOutput = new Variable("Pipeline Output Rate", THING,
                () -> step3Delay.flowPerTimeUnit(HOUR).getValue());

        model.addStock(step1);
        model.addStock(step2);
        model.addStock(step3);
        model.addVariable(totalWIP);
        model.addVariable(pipelineOutput);

        Simulation run = new Simulation(model, HOUR, Times.hours(durationHours));
        run.addEventHandler(new StockLevelChartViewer());
        run.addEventHandler(new CsvSubscriber("3rd order.csv"));
        run.execute();
    }
}
