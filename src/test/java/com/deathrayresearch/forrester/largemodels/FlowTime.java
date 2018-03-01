package com.deathrayresearch.forrester.largemodels;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.archetypes.SimpleLinearChange;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.item.Thing;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.rate.Flow;
import com.deathrayresearch.forrester.rate.FlowPerDay;
import com.deathrayresearch.forrester.ui.ChartViewer;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.WEEK;

public class FlowTime {

    private static final Thing TEST = Thing.getInstance();

    private Constant TATGoal = new Constant("TAT Target", DAY, 14);
    private Constant Capacity = new Constant("Capacity", TEST,190);

    private Stock WIP = new Stock("WIP", 1000, TEST);
    private Stock Variance = new Stock("TAT Variance", 0, DAY);


    public static void main(String[] args) {
        FlowTime tat = new FlowTime();
        tat.tatModel();
    }

    private void tatModel() {

        Model tatModel = new Model("TAT Model");

        Flow Demand =
            new FlowPerDay("New Orders") {
                @Override
                protected Quantity quantityPerDay() {
                    return SimpleLinearChange.from("New Orders", WIP, 200);
                    //return new Quantity("New Orders", 200, TEST);
                }
            };

        Flow Throughput =
            new FlowPerDay("Delivered Reports") {
                @Override
                protected Quantity quantityPerDay() {
                    double throughput = Math.min(Capacity.getCurrentValue(), Demand.flowPerTimeUnit(DAY).getValue());
                    return new Quantity("Delivered Reports", throughput, TEST);
                }
            };

        WIP.addInflow(Demand);
        WIP.addOutflow(Throughput);

        tatModel.addStock(WIP);
        tatModel.addConstant(TATGoal);
        tatModel.addStock(Variance);

        Simulation sim = new Simulation(tatModel, Day.getInstance(), WEEK, 12);
        sim.addEventHandler(new ChartViewer());
        sim.execute();
    }
}
