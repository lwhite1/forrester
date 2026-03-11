/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.shrewd.demo;

import systems.courant.shrewd.Simulation;
import systems.courant.shrewd.measure.Quantity;
import systems.courant.shrewd.measure.units.item.ItemUnits;
import systems.courant.shrewd.model.Flow;
import systems.courant.shrewd.model.Flows;
import systems.courant.shrewd.model.Model;
import systems.courant.shrewd.model.ModelMetadata;
import systems.courant.shrewd.model.Stock;
import systems.courant.shrewd.model.Variable;
import systems.courant.shrewd.ui.StockLevelChartViewer;

import static systems.courant.shrewd.measure.Units.DAY;
import static systems.courant.shrewd.measure.Units.HOUR;
import static systems.courant.shrewd.measure.Units.WEEK;

/**
 * Models turnaround time (TAT) for a work-in-process queue with demand and capacity constraints.
 *
 * <p>Two stocks — WIP (work items) and TAT (hours) — interact through flows defined at
 * different time units (per-day demand, per-hour TAT adjustment). Throughput is bounded by
 * capacity and delayed by the current TAT, demonstrating how flow rates expressed in different
 * time units are automatically converted by the simulation engine.
 */
public class FlowTimeDemo {

    public static void main(String[] args) {
        double initialWip = 1000;
        double tatGoalHours = 336;
        double capacity = 190;
        double newOrdersPerDay = 200;
        double hoursPerDay = 24.0;
        double tatAdjustmentTimeHours = 24.0;
        double durationWeeks = 4;

        new FlowTimeDemo().run(initialWip, tatGoalHours, capacity, newOrdersPerDay,
                hoursPerDay, tatAdjustmentTimeHours, durationWeeks);
    }

    public void run(double initialWip, double tatGoalHours, double capacity,
                    double newOrdersPerDay, double hoursPerDay, double tatAdjustmentTimeHours,
                    double durationWeeks) {
        Model tatModel = new Model("TAT Model");
        tatModel.setMetadata(ModelMetadata.builder()
                .license("CC-BY-SA-4.0")
                .build());

        Stock wip = new Stock("WIP", initialWip, ItemUnits.THING);
        Stock tat = new Stock("TAT", tatGoalHours, HOUR);

        Variable discrepancy = new Variable("Discrepancy", HOUR,
                () -> tatGoalHours - tat.getValue());

        Simulation sim = new Simulation(tatModel, HOUR, WEEK, durationWeeks);

        Flow demand = Flows.linearGrowth("New Orders", DAY, wip, newOrdersPerDay);

        Flow throughput = Flow.create("Delivered Reports", DAY, () -> {
            int demandDelay = Math.max(0, (int) Math.round(tat.getValue()));
            int stepToGet = (int) sim.getCurrentStep() - demandDelay;
            double demandPlusDelay = demand.getHistoryAtTimeStep(stepToGet);
            return new Quantity(Math.min(capacity, demandPlusDelay), ItemUnits.THING);
        });

        Flow tatAdjustment = Flow.create("TAT Adjustment", HOUR, () -> {
            double currentTAT = Math.max(0, tat.getValue());
            double actualTAT = (wip.getValue() / capacity) * hoursPerDay;
            return new Quantity((actualTAT - currentTAT) / tatAdjustmentTimeHours, HOUR);
        });

        wip.addInflow(demand);
        wip.addOutflow(throughput);
        tat.addInflow(tatAdjustment);

        tatModel.addStock(wip);
        tatModel.addStock(tat);
        tatModel.addVariable(discrepancy);

        sim.addEventHandler(new StockLevelChartViewer());
        sim.execute();
    }
}
