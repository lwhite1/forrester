package com.deathrayresearch.forrester.largemodels;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.archetypes.SimpleLinearChange;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.item.Thing;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.model.flows.FlowPerDay;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import static com.deathrayresearch.forrester.measure.Units.*;

public class FlowTime {

    private static final Thing TEST = Thing.getInstance();

    private Constant TATGoal = new Constant("TAT Target", HOUR, 336);
    private Constant Capacity = new Constant("Capacity", TEST,190);

    private Stock WIP = new Stock("WIP", 1000, TEST);

    private Stock TAT = new Stock("TAT", TATGoal.getValue(), HOUR);

    private Variable discrepancy = new Variable("Discrepancy", HOUR,
            () -> TATGoal.getValue() - TAT.getValue());

    public static void main(String[] args) {
        FlowTime tat = new FlowTime();
        tat.tatModel();
    }

    private void tatModel() {

        Model tatModel = new Model("TAT Model");

        Simulation sim = new Simulation(tatModel, HOUR, WEEK, 4);

        Flow Demand =
            new FlowPerDay("New Orders") {
                @Override
                protected Quantity quantityPerDay() {
                    return SimpleLinearChange.from( WIP, 200);
                }
            };

        Flow Throughput =
            new FlowPerDay("Delivered Reports") {
                @Override
                protected Quantity quantityPerDay() {

                    double demandDelay = TAT.getValue();
                    int stepToGet = sim.getCurrentStep() - Math.toIntExact(Math.round(demandDelay));

                    double demandPlusDelay = Demand.getHistoryAtTimeStep(stepToGet);

                    double throughput = Math.min(Capacity.getValue(), demandPlusDelay);
                    return new Quantity(throughput, TEST);
                }
            };

        WIP.addInflow(Demand);
        WIP.addOutflow(Throughput);

        tatModel.addStock(WIP);
        tatModel.addStock(TAT);
        tatModel.addConstant(TATGoal);

        sim.addEventHandler(new StockLevelChartViewer());
        sim.execute();
    }
}
