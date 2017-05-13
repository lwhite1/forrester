package com.deathrayresearch.dynamics.largemodels.waterfall;

import com.deathrayresearch.dynamics.measure.Quantity;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Item;
import com.deathrayresearch.dynamics.measure.dimension.Time;
import com.deathrayresearch.dynamics.model.Flow;
import com.deathrayresearch.dynamics.model.Formula;
import com.deathrayresearch.dynamics.model.Stock;
import com.deathrayresearch.dynamics.model.SubSystem;
import com.deathrayresearch.dynamics.model.Variable;
import com.deathrayresearch.dynamics.rate.Rate;

import static com.deathrayresearch.dynamics.largemodels.waterfall.WaterfallSoftwareDevelopment.*;

/**
 */
class Development {

    static SubSystem getDevelopmentSubSystem() {

        SubSystem subSystem = new SubSystem(DEVELOPMENT);
        Stock<Item> tasksDeveloped = new Stock<>(TASKS_DEVELOPED, 0.0, TASKS);

        Flow<Item> developmentFlow = getDevelopmentFlow();

        Variable developmentProductivity = new Variable("Development Productivity", new Formula() {
            @Override
            public double getCurrentValue() {
                return 1;
            }
        });

        Variable developmentStaffing = new Variable("Development Staffing", new Formula() {
            @Override
            public double getCurrentValue() {
                return 100;
            }
        });

        tasksDeveloped.addInflow(developmentFlow);

        subSystem.addFlow(developmentFlow);
        subSystem.addStock(tasksDeveloped);

        subSystem.addVariable(developmentProductivity);
        subSystem.addVariable(developmentStaffing);
        return subSystem;

    }

    private static Flow<Item> getDevelopmentFlow() {
        Rate<Item> softwareDevelopmentRate = new Rate<Item>() {
            @Override
            public Quantity<Item> flowPerTimeUnit(Unit<Time> timeUnit) {
                return new Quantity<>(1, TASKS);
            }
        };

        return new Flow<>("Software Development", softwareDevelopmentRate);
    }
}
