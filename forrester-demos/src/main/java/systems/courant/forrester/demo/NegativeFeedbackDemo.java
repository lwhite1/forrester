/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.forrester.demo;

import systems.courant.forrester.Simulation;
import systems.courant.forrester.measure.Quantity;
import systems.courant.forrester.measure.Unit;
import systems.courant.forrester.measure.units.item.ItemUnit;
import systems.courant.forrester.measure.units.time.TimeUnits;
import systems.courant.forrester.model.Flow;
import systems.courant.forrester.model.Model;
import systems.courant.forrester.model.ModelMetadata;
import systems.courant.forrester.model.Stock;
import systems.courant.forrester.ui.StockLevelChartViewer;

import static systems.courant.forrester.measure.Units.DAY;
import static systems.courant.forrester.measure.Units.WEEK;

/**
 * Demonstrates negative (balancing) feedback driving a stock toward a goal.
 *
 * <p>An Inventory stock adjusts toward a target through a production inflow proportional to
 * the gap divided by an adjustment time. The stock approaches its goal asymptotically —
 * the classic goal-seeking archetype in system dynamics.
 */
public class NegativeFeedbackDemo {

    private static final Unit INVENTORY = new ItemUnit("Units");

    public static void main(String[] args) {
        double initialInventory = 100;
        double goalInventory = 860;
        double adjustmentTimeDays = 8;
        double durationWeeks = 12;

        new NegativeFeedbackDemo().run(initialInventory, goalInventory, adjustmentTimeDays,
                durationWeeks);
    }

    public void run(double initialInventory, double goalInventory, double adjustmentTimeDays,
                    double durationWeeks) {
        Model model = new Model("Negative feedback with goal");
        model.setMetadata(ModelMetadata.builder()
                .license("CC-BY-SA-4.0")
                .build());

        Stock inventoryOnHand = new Stock("Inventory on-hand", initialInventory, INVENTORY);

        Quantity goal = new Quantity(goalInventory, INVENTORY);

        Flow production = Flow.create("Production", DAY, () -> {
            Quantity delta = goal.subtract(inventoryOnHand.getQuantity());
            return delta.divide(adjustmentTimeDays);
        });

        inventoryOnHand.addInflow(production);

        model.addStock(inventoryOnHand);

        Simulation run = new Simulation(model, TimeUnits.DAY, WEEK, durationWeeks);
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }
}
