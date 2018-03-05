package com.deathrayresearch.forrester;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.item.Thing;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Formula;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.model.flows.FlowPerDay;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;
import org.junit.Test;

import static com.deathrayresearch.forrester.measure.Units.*;

public class InventoryModel {

    private static final Thing CARS = THING;

    // delays
    private static final double PERCEPTION_DELAY = 0;
    private static final double RESPONSE_DELAY = 0;
    private static final double DELIVERY_DELAY = 0;

    @Test
    public void testRun1() {

        Model model = new Model("Inventory Model");
        model.setComment("From 'Thinking in Systems': Illustrates the effects of delays. ");

        Simulation run = new Simulation(model, DAY, DAY, 100);

        Stock carsOnLot = new Stock("Cars on Lot", 200, CARS);

        Variable demand = new Variable("Customer Demand", CARS,
                new Formula() {
                    @Override
                    public double getCurrentValue() {
                        if (run.getCurrentStep() <= 25) {
                            return 20;
                        }
                        return 22;
                    }
                });

        Flow sales = new FlowPerDay("Sales") {
            @Override
            protected Quantity quantityPerDay() {
                return new Quantity(
                        Math.min(carsOnLot.getValue(), demand.getValue()),
                        CARS);
            }
        };

        Variable perceivedSales = new Variable("Perceived Sales", CARS,
                () -> sales.flowPerTimeUnit(DAY).getValue());

        Variable desiredInventory = new Variable("Desired Inventory", CARS, new Formula() {
            @Override
            public double getCurrentValue() {
                return perceivedSales.getValue() * 10;
            }
        });

        Variable inventoryGap = new Variable("Gap between desired and actual inventory", CARS,
                () -> carsOnLot.getValue() - desiredInventory.getValue());

        Variable ordersToFactory = new Variable("Orders to Factory", CARS,
                () -> Math.max(perceivedSales.getValue() + inventoryGap.getValue(), 0));

        Flow deliveries = new FlowPerDay("Deliveries") {
            @Override
            protected Quantity quantityPerDay() {
                if (run.getCurrentStep() <= 5) {
                    return new Quantity(
                            20,
                            CARS);
                }
                int priorStep = run.getCurrentStep() - Math.toIntExact(Math.round(DELIVERY_DELAY));
                return new Quantity(ordersToFactory.getHistoryAtTimeStep(priorStep), CARS);
            }
        };

        carsOnLot.addInflow(deliveries);
        carsOnLot.addOutflow(sales);

        model.addStock(carsOnLot);

        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}
