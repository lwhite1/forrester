/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.sd.demo;

import systems.courant.sd.Simulation;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.measure.Unit;
import systems.courant.sd.measure.units.item.ItemUnit;
import systems.courant.sd.measure.units.time.TimeUnits;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.ModelMetadata;
import systems.courant.sd.model.Stock;
import systems.courant.sd.ui.StockLevelChartViewer;

import static systems.courant.sd.measure.Units.DAY;
import static systems.courant.sd.measure.Units.WEEK;

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
        Model model = getModel(initialInventory, goalInventory, adjustmentTimeDays);

        Simulation run = new Simulation(model, TimeUnits.DAY, WEEK, durationWeeks);
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }

    public Model getModel() {
        return getModel(100, 860, 8);
    }

    public Model getModel(double initialInventory, double goalInventory,
                           double adjustmentTimeDays) {
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

        return model;
    }
}
