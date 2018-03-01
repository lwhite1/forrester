package com.deathrayresearch.forrester.largemodels.f1cdxSales;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.item.Thing;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.rate.RatePerDay;
import com.deathrayresearch.forrester.ui.ChartViewer;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.WEEK;

public class TurnaroundTime {

    static final Thing TEST = Thing.getInstance();

    Constant TATGoal = new Constant("TAT Target", DAY, 14);
    Constant Capacity = new Constant("Capacity", TEST,190);

    Stock WIP = new Stock("WIP", 1000, TEST);
    Stock Variance = new Stock("TAT Variance", 0, DAY);


    public static void main(String[] args) {
        TurnaroundTime tat = new TurnaroundTime();
        tat.tatModel();
    }

    public Model tatModel() {

        Model tatModel = new Model("TAT Model");

        Flow Demand = new Flow(
            new RatePerDay() {
                @Override
                protected Quantity quantityPerDay() {
                    return new Quantity("New Orders", 200, TEST);
                }
            }
        );

        Flow Throughput = new Flow(
            new RatePerDay() {
                @Override
                protected Quantity quantityPerDay() {
                    double throughput = Math.min(Capacity.getCurrentValue(), Demand.getRate().flowPerTimeUnit(DAY).getValue());
                    return new Quantity("Delivered Reports", throughput, TEST);
                }
            }
        );

        WIP.addInflow(Demand);
        WIP.addOutflow(Throughput);

        tatModel.addStock(WIP);
        tatModel.addConstant(TATGoal);
        tatModel.addStock(Variance);

        Simulation sim = new Simulation(tatModel, Day.getInstance(), WEEK, 12);
        sim.addEventHandler(new ChartViewer());
        sim.execute();
        return tatModel;
    }
}
