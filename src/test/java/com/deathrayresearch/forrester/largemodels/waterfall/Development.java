package com.deathrayresearch.forrester.largemodels.waterfall;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.item.People;
import com.deathrayresearch.forrester.measure.units.item.Thing;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Formula;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Module;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.rate.Rate;
import com.deathrayresearch.forrester.rate.RatePerDay;

/**
 */
class Development {

    private static final Tasks TASKS = Tasks.getInstance();
    private static final People PEOPLE = People.getInstance();

    static Module getDevelopmentSubSystem(Model model) {

        Module module = new Module(WaterfallSoftwareDevelopment.DEVELOPMENT);
        Stock tasksDeveloped = new Stock(WaterfallSoftwareDevelopment.TASKS_DEVELOPED,0.0, TASKS);

        Flow developmentFlow = getDevelopmentFlow();

        Variable developmentProductivity = new Variable("Development Productivity", TASKS, (Formula) () -> 1);

        Variable developmentStaffing = new Variable("Development Staffing", PEOPLE, () -> 10);

        Variable totalDailyStaffing = new Variable("Total daily staffing", PEOPLE,
                () -> model.getVariable(WaterfallSoftwareDevelopment.TOTAL_WORKFORCE).getCurrentValue()
                * Workforce.AVERAGE_DAILY_MAN_POWER_PER_STAFF.getCurrentValue());

        Stock cumulativeManDaysExpended =
                new Stock("Cumulative Person-Days Expended", 0.0001, PersonDays.getInstance());

        Constant qualityObjective = new Constant("Quality Objective", Thing.getInstance(), 0);

        tasksDeveloped.addInflow(developmentFlow);

        module.addFlow(developmentFlow);
        module.addStock(tasksDeveloped);

        module.addVariable(developmentProductivity);
        module.addVariable(developmentStaffing);
        return module;

    }

    private static Flow getDevelopmentFlow() {
        Rate softwareDevelopmentRate = new RatePerDay() {
            @Override
            protected Quantity quantityPerDay() {
                return new Quantity(1, WaterfallSoftwareDevelopment.TASKS);
            }
        };

        return new Flow("Software Development", softwareDevelopmentRate);
    }
}
