/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.shrewd.demo;

import systems.courant.shrewd.Simulation;
import systems.courant.shrewd.io.CsvSubscriber;
import systems.courant.shrewd.measure.Quantity;
import systems.courant.shrewd.model.Flow;
import systems.courant.shrewd.model.Model;
import systems.courant.shrewd.model.ModelMetadata;
import systems.courant.shrewd.model.Stock;
import systems.courant.shrewd.ui.StockLevelChartViewer;

import java.time.Duration;

import static systems.courant.shrewd.measure.Units.GALLON_US;
import static systems.courant.shrewd.measure.Units.MINUTE;

/**
 * The classic bathtub model — the simplest stock-and-flow demonstration.
 *
 * <p>A Water-in-Tub stock drains at a constant rate from the start while the inflow is
 * delayed and then adds at a constant rate. The tub drains down, then stabilizes once
 * inflow begins, illustrating how a stock is the accumulation of the difference between its
 * inflow and outflow over time.
 */
public class TubDemo {

    public static void main(String[] args) {
        double initialWater = 50;        // gallons
        double outflowRate = 5;          // gallons per minute
        double inflowRate = 5;           // gallons per minute
        int inflowDelayMinutes = 5;      // minutes before inflow starts
        double durationMinutes = 10;

        new TubDemo().run(initialWater, outflowRate, inflowRate, inflowDelayMinutes,
                durationMinutes);
    }

    public void run(double initialWater, double outflowRate, double inflowRate,
                    int inflowDelayMinutes, double durationMinutes) {
        Model model = new Model("Tub model");
        model.setMetadata(ModelMetadata.builder()
                .license("CC-BY-SA-4.0")
                .build());
        Simulation run = new Simulation(model, MINUTE, MINUTE, durationMinutes);

        Stock tub = new Stock("Water in Tub", initialWater, GALLON_US);

        Flow outflow = Flow.create("Outflow", MINUTE, () ->
                new Quantity(
                        Math.min(outflowRate, tub.getQuantity().getValue()),
                        GALLON_US));

        Flow inflow = Flow.create("Inflow", MINUTE, () -> {
            if (run.getElapsedTime().compareTo(Duration.ofMinutes(inflowDelayMinutes)) < 0) {
                return new Quantity(0.0, GALLON_US);
            }
            return new Quantity(inflowRate, GALLON_US);
        });

        tub.addInflow(inflow);
        tub.addOutflow(outflow);

        model.addStock(tub);

        run.addEventHandler(new StockLevelChartViewer());
        run.addEventHandler(new CsvSubscriber("tub.csv"));
        run.execute();
    }
}
