package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.THING;

/**
 * Models a car dealership's inventory system with perception, response, and delivery delays.
 *
 * <p>Inspired by the inventory example in <em>Thinking in Systems</em>. A Cars-on-Lot stock is
 * drained by sales and replenished by factory deliveries. Three delays — perception,
 * response, and delivery — cause the dealer to overshoot and oscillate before
 * settling, demonstrating how delays in feedback loops amplify fluctuations.
 */
public class InventoryModelDemo {

    private static final Unit CARS = THING;

    public static void main(String[] args) {
        double initialCarsOnLot = 200;
        double initialPerceivedSales = 20;
        double baseDemand = 20;         // cars/day before step change
        double stepDemand = 22;         // cars/day after step change
        int demandStepChangeDay = 25;   // day when demand shifts
        double perceptionDelay = 5;     // days
        double responseDelay = 3;       // days
        double deliveryDelay = 5;       // days
        double desiredInventoryMultiplier = 10;  // times perceived sales
        double durationDays = 100;

        new InventoryModelDemo().run(initialCarsOnLot, initialPerceivedSales,
                baseDemand, stepDemand, demandStepChangeDay,
                perceptionDelay, responseDelay, deliveryDelay,
                desiredInventoryMultiplier, durationDays);
    }

    public void run(double initialCarsOnLot, double initialPerceivedSales,
                    double baseDemand, double stepDemand, int demandStepChangeDay,
                    double perceptionDelay, double responseDelay, double deliveryDelay,
                    double desiredInventoryMultiplier, double durationDays) {

        Model model = new Model("Inventory Model");
        Simulation run = new Simulation(model, DAY, DAY, durationDays);

        Stock carsOnLot = new Stock("Cars on Lot", initialCarsOnLot, CARS);

        Variable demand = new Variable("Customer Demand", CARS,
                () -> run.getCurrentStep() <= demandStepChangeDay ? baseDemand : stepDemand);

        Flow sales = Flow.create("Sales", DAY, () ->
                new Quantity(Math.min(carsOnLot.getValue(), demand.getValue()), CARS));

        Stock perceivedSales = new Stock("Perceived Sales", initialPerceivedSales, CARS);

        Flow perceptionAdjustment = Flow.create("Perception Adjustment", DAY, () -> {
            double adjustment = (sales.flowPerTimeUnit(DAY).getValue() - perceivedSales.getValue())
                    / perceptionDelay;
            return new Quantity(adjustment, CARS);
        });

        perceivedSales.addInflow(perceptionAdjustment);

        Variable desiredInventory = new Variable("Desired Inventory", CARS,
                () -> perceivedSales.getValue() * desiredInventoryMultiplier);

        Variable inventoryGap = new Variable("Gap between desired and actual inventory", CARS,
                () -> carsOnLot.getValue() - desiredInventory.getValue());

        Variable ordersToFactory = new Variable("Orders to Factory", CARS,
                () -> Math.max(perceivedSales.getValue() + inventoryGap.getValue(), 0));

        int totalDelay = Math.toIntExact(Math.round(responseDelay + deliveryDelay));

        Flow deliveries = Flow.create("Deliveries", DAY, () -> {
            if (run.getCurrentStep() <= totalDelay) {
                return new Quantity(initialPerceivedSales, CARS);
            }
            int priorStep = run.getCurrentStep() - totalDelay;
            return new Quantity(ordersToFactory.getHistoryAtTimeStep(priorStep), CARS);
        });

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
