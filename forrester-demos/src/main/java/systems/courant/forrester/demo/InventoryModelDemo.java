/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.forrester.demo;

import systems.courant.forrester.Simulation;
import systems.courant.forrester.measure.Quantity;
import systems.courant.forrester.model.Flow;
import systems.courant.forrester.model.Model;
import systems.courant.forrester.model.ModelMetadata;
import systems.courant.forrester.model.Stock;
import systems.courant.forrester.model.Variable;
import systems.courant.forrester.ui.StockLevelChartViewer;

import static systems.courant.forrester.measure.Units.DAY;
import static systems.courant.forrester.measure.Units.THING;

/**
 * Models a car dealership's inventory system with perception, response, and delivery delays.
 *
 * <p>Inspired by the inventory example in <em>Thinking in Systems</em>. A Cars-on-Lot stock is
 * drained by sales and replenished by factory deliveries. Three delays — perception,
 * response, and delivery — cause the dealer to overshoot and oscillate before
 * settling, demonstrating how delays in feedback loops amplify fluctuations.
 */
public class InventoryModelDemo {

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
        model.setMetadata(ModelMetadata.builder()
                .source("Thinking in Systems, Donella Meadows (2008)")
                .license("CC-BY-SA-4.0")
                .build());
        Simulation run = new Simulation(model, DAY, DAY, durationDays);

        Stock carsOnLot = new Stock("Cars on Lot", initialCarsOnLot, THING);

        Variable demand = new Variable("Customer Demand", THING,
                () -> run.getCurrentStep() <= demandStepChangeDay ? baseDemand : stepDemand);

        Flow sales = Flow.create("Sales", DAY, () ->
                new Quantity(Math.min(carsOnLot.getValue(), demand.getValue()), THING));

        Stock perceivedSales = new Stock("Perceived Sales", initialPerceivedSales, THING);

        Flow perceptionAdjustment = Flow.create("Perception Adjustment", DAY, () -> {
            double adjustment = (sales.flowPerTimeUnit(DAY).getValue() - perceivedSales.getValue())
                    / perceptionDelay;
            return new Quantity(adjustment, THING);
        });

        perceivedSales.addInflow(perceptionAdjustment);

        Variable desiredInventory = new Variable("Desired Inventory", THING,
                () -> perceivedSales.getValue() * desiredInventoryMultiplier);

        Variable inventoryGap = new Variable("Gap between desired and actual inventory", THING,
                () -> desiredInventory.getValue() - carsOnLot.getValue());

        Variable ordersToFactory = new Variable("Orders to Factory", THING,
                () -> Math.max(perceivedSales.getValue()
                        + inventoryGap.getValue() / responseDelay, 0));

        int totalDelay = Math.toIntExact(Math.round(deliveryDelay));

        Flow deliveries = Flow.create("Deliveries", DAY, () -> {
            if (run.getCurrentStep() <= totalDelay) {
                return new Quantity(initialPerceivedSales, THING);
            }
            int priorStep = run.getCurrentStep() - totalDelay;
            return new Quantity(ordersToFactory.getHistoryAtTimeStep(priorStep), THING);
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
