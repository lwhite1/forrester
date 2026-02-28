package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.flows.FlowPerMinute;
import com.deathrayresearch.forrester.ui.FlowChartViewer;

import static com.deathrayresearch.forrester.measure.Units.CENTIGRADE;
import static com.deathrayresearch.forrester.measure.Units.MINUTE;

/**
 * Simulates Newton's law of cooling applied to a cup of coffee.
 *
 * <p>A Coffee Temperature stock (initially 100 °C) cools toward a Room Temperature constant
 * (18 °C) via a negative-feedback outflow proportional to the temperature discrepancy. The
 * cooling rate decelerates as the gap narrows, producing the classic goal-seeking decay curve.
 */
public class CoffeeCoolingDemo {

    public static void main(String[] args) {
        new CoffeeCoolingDemo().run();
    }

    public void run() {

        Model model = new Model("Coffee Cooling");
        model.setComment("Illustrates decay to a target value for the stock. The rate of decay is based on " +
                "the difference between " +
                "the target and the current value of the stock");

        Stock coffeeTemperature = new Stock("Coffee Temperature", 100, CENTIGRADE);

        Constant roomTemperature = new Constant("Room Temperature", CENTIGRADE, 18);

        Variable discrepancy = new Variable("Discrepancy", CENTIGRADE,
                () -> coffeeTemperature.getValue() - roomTemperature.getValue());

        Flow cooling = new FlowPerMinute("Cooling") {

            final double coolingRate = 0.10;

            @Override
            protected Quantity quantityPerTimeUnit() {
                return new Quantity(
                        discrepancy.getValue() * coolingRate,
                        CENTIGRADE);
            }
        };

        coffeeTemperature.addOutflow(cooling);

        model.addStock(coffeeTemperature);

        Simulation run = new Simulation(model, MINUTE, MINUTE, 8);
        // run.addEventHandler(new StockLevelChartViewer());
        run.addEventHandler(new FlowChartViewer(cooling));
        run.execute();
    }
}
