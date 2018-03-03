package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.flows.FlowPerMinute;
import com.deathrayresearch.forrester.ui.FlowChartViewer;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;
import org.junit.Test;

import static com.deathrayresearch.forrester.measure.Units.*;

/**
 *
 */
public class CoffeeCoolingModel {

    @Test
    public void testRun1() {

        Model model = new Model("Coffee Cooling");
        model.setComment("Illustrates decay to a target value for the stock. The rate of decay is based on " +
                "the difference between " +
                "the target and the current value of the stock");

        Stock coffeeTemperature = new Stock("Coffee Temperature", 100, CENTIGRADE);

        Constant roomTemperature = new Constant("Room Temperature", CENTIGRADE, 18);

        Variable discrepancy = new Variable("Discrepancy", CENTIGRADE,
                () -> coffeeTemperature.getValue() - roomTemperature.getValue());

        Flow cooling = new FlowPerMinute("Cooling") {

            double coolingRate = 0.10;

            @Override
            protected Quantity quantityPerMinute() {
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
