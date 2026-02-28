package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Formula;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.model.flows.FlowPerDay;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.THING;

public class InventoryModelDemo {

    private static final Unit CARS = THING;

    // delays
    private static final double PERCEPTION_DELAY = 5;  // days to perceive demand changes
    private static final double RESPONSE_DELAY = 3;     // days to process and send order
    private static final double DELIVERY_DELAY = 5;     // days for factory to ship cars

    public static void main(String[] args) {
        new InventoryModelDemo().run();
    }

    public void run() {

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
            protected Quantity quantityPerTimeUnit() {
                return new Quantity(
                        Math.min(carsOnLot.getValue(), demand.getValue()),
                        CARS);
            }
        };

        Stock perceivedSales = new Stock("Perceived Sales", 20, CARS);

        Flow perceptionAdjustment = new FlowPerDay("Perception Adjustment") {
            @Override
            protected Quantity quantityPerTimeUnit() {
                double adjustment = (sales.flowPerTimeUnit(DAY).getValue() - perceivedSales.getValue())
                        / PERCEPTION_DELAY;
                return new Quantity(adjustment, CARS);
            }
        };

        perceivedSales.addInflow(perceptionAdjustment);

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

        int totalDelay = Math.toIntExact(Math.round(RESPONSE_DELAY + DELIVERY_DELAY));

        Flow deliveries = new FlowPerDay("Deliveries") {
            @Override
            protected Quantity quantityPerTimeUnit() {
                if (run.getCurrentStep() <= totalDelay) {
                    return new Quantity(20, CARS);
                }
                int priorStep = run.getCurrentStep() - totalDelay;
                return new Quantity(ordersToFactory.getHistoryAtTimeStep(priorStep), CARS);
            }
        };

        carsOnLot.addInflow(deliveries);
        carsOnLot.addOutflow(sales);

        model.addStock(carsOnLot);
        model.addStock(perceivedSales);
        model.addVariable(demand);
        model.addVariable(desiredInventory);
        model.addVariable(inventoryGap);
        model.addVariable(ordersToFactory);

        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}
