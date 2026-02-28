package com.deathrayresearch.forrester.largemodels;

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
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import static com.deathrayresearch.forrester.measure.Units.HOUR;
import static com.deathrayresearch.forrester.measure.Units.WEEK;

public class FlowTimeTest {

    private static final ItemUnits TEST = ItemUnits.THING;

    private final Constant TATGoal = new Constant("TAT Target", HOUR, 336);
    private final Constant Capacity = new Constant("Capacity", TEST,190);

    private final Stock WIP = new Stock("WIP", 1000, TEST);

    private final Stock TAT = new Stock("TAT", TATGoal.getValue(), HOUR);

    private final Variable discrepancy = new Variable("Discrepancy", HOUR,
            () -> TATGoal.getValue() - TAT.getValue());

    public static void main(String[] args) {
        FlowTimeTest tat = new FlowTimeTest();
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

        WIP.addInflow(Demand);
        WIP.addOutflow(Throughput);

        tatModel.addStock(WIP);
        tatModel.addStock(TAT);
        tatModel.addConstant(TATGoal);

        sim.addEventHandler(new StockLevelChartViewer());
        sim.execute();
    }
}
