package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.item.ItemUnit;
import com.deathrayresearch.forrester.measure.units.time.TimeUnits;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.ModelMetadata;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.WEEK;

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
