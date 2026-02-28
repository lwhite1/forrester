package com.deathrayresearch.forrester.demo;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.archetypes.SimpleLinearChange;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.item.ItemUnits;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.model.flows.FlowPerDay;
import com.deathrayresearch.forrester.model.flows.FlowPerHour;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import static com.deathrayresearch.forrester.measure.Units.HOUR;
import static com.deathrayresearch.forrester.measure.Units.WEEK;

/**
 * Models turnaround time (TAT) for a work-in-process queue with demand and capacity constraints.
 *
 * <p>Two stocks — WIP (work items) and TAT (hours) — interact through flows defined at
 * different time units (per-day demand, per-hour TAT adjustment). Throughput is bounded by
 * capacity and delayed by the current TAT, demonstrating how flow rates expressed in different
 * time units are automatically converted by the simulation engine.
 */
public class FlowTimeDemo {

    private static final ItemUnits TEST = ItemUnits.THING;

    private final Constant TATGoal = new Constant("TAT Target", HOUR, 336);
    private final Constant Capacity = new Constant("Capacity", TEST,190);

    private final Stock WIP = new Stock("WIP", 1000, TEST);

    private final Stock TAT = new Stock("TAT", TATGoal.getValue(), HOUR);

    private final Variable discrepancy = new Variable("Discrepancy", HOUR,
            () -> TATGoal.getValue() - TAT.getValue());

    public static void main(String[] args) {
        FlowTimeDemo tat = new FlowTimeDemo();
        tat.tatModel();
    }

    private void tatModel() {

        Model tatModel = new Model("TAT Model");

        Simulation sim = new Simulation(tatModel, HOUR, WEEK, 4);

        Flow Demand =
            new FlowPerDay("New Orders") {
                @Override
                protected Quantity quantityPerTimeUnit() {
                    return SimpleLinearChange.from( WIP, 200);
                }
            };

        Flow Throughput =
            new FlowPerDay("Delivered Reports") {
                @Override
                protected Quantity quantityPerTimeUnit() {

                    int demandDelay = Math.toIntExact(Math.round(TAT.getValue()));
                    int stepToGet = sim.getCurrentStep() - demandDelay;

                    double demandPlusDelay = Demand.getHistoryAtTimeStep(stepToGet);

                    double throughput = Math.min(Capacity.getValue(), demandPlusDelay);
                    return new Quantity(throughput, TEST);
                }
            };

        double hoursPerDay = 24.0;
        double adjustmentTime = 24.0; // hours

        Flow tatAdjustment = new FlowPerHour("TAT Adjustment") {
            @Override
            protected Quantity quantityPerTimeUnit() {
                double actualTAT = (WIP.getValue() / Capacity.getValue()) * hoursPerDay;
                return new Quantity((actualTAT - TAT.getValue()) / adjustmentTime, HOUR);
            }
        };

        WIP.addInflow(Demand);
        WIP.addOutflow(Throughput);
        TAT.addInflow(tatAdjustment);

        tatModel.addStock(WIP);
        tatModel.addStock(TAT);
        tatModel.addConstant(TATGoal);
        tatModel.addVariable(discrepancy);

        sim.addEventHandler(new StockLevelChartViewer());
        sim.execute();
    }
}
