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
import com.deathrayresearch.forrester.model.flows.FlowPerDay;
import com.deathrayresearch.forrester.model.flows.FlowPerHour;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import static com.deathrayresearch.forrester.measure.Units.HOUR;
import static com.deathrayresearch.forrester.measure.Units.THING;

/**
 *
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

        Flow demand = new FlowPerDay("Process Demand") {
            @Override
            protected Quantity quantityPerTimeUnit() {
                return new Quantity(48, THING);
            }
        };

        Flow step1Delay = new FlowPerHour("Step 1 delay") {
            @Override
            protected Quantity quantityPerTimeUnit() {
                double averageDelay = 7; // 7 hour average activity time for this step
                return new Quantity(
                        Math.min(step1.getValue(), step1.getValue()/averageDelay), THING);
                //return step1.getQuantity().divide(averageDelay);
            }
        };

        Flow step2Delay = new FlowPerHour("Step 2 delay") {
            @Override
            protected Quantity quantityPerTimeUnit() {
                double averageDelay = 6.3; // 6.3 hour average activity time for this step
                return new Quantity(
                        Math.min(step2.getValue(), step2.getValue()/ averageDelay), THING);
            }
        };

        Flow step3Delay = new FlowPerHour("Step 3 delay") {
            @Override
            protected Quantity quantityPerTimeUnit() {
                double averageDelay = 3.2; // 3.2 hour average activity time for this step
                return new Quantity(
                        Math.min(step3.getValue(), step3.getValue()/ averageDelay), THING);
            }
        };

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
