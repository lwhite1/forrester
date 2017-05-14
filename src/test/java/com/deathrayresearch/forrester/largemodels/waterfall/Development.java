package com.deathrayresearch.forrester.largemodels.waterfall;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.measure.units.item.Thing;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Formula;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.SubSystem;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.rate.Rate;

/**
 */
class Development {

    static SubSystem getDevelopmentSubSystem(Model model) {

        SubSystem subSystem = new SubSystem(WaterfallSoftwareDevelopment.DEVELOPMENT);
        Stock tasksDeveloped = new Stock(WaterfallSoftwareDevelopment.TASKS_DEVELOPED, 0.0, WaterfallSoftwareDevelopment.TASKS);

        Flow developmentFlow = getDevelopmentFlow();

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


        Variable totalDailyStaffing = new Variable("Total daily staffing", new Formula() {
            @Override
            public double getCurrentValue() {
                return model.getVariable(WaterfallSoftwareDevelopment.TOTAL_WORKFORCE).getCurrentValue()
                    * Workforce.AVERAGE_DAILY_MAN_POWER_PER_STAFF.getCurrentValue();
            }
        });

        Stock cumulativeManDaysExpended =
                new Stock("Cumulative Person-Days Expended", 0.0001, PersonDays.getInstance());

        Constant qualityObjective = new Constant("Quality Objective", Thing.getInstance(), 0);

        tasksDeveloped.addInflow(developmentFlow);

        subSystem.addFlow(developmentFlow);
        subSystem.addStock(tasksDeveloped);

        subSystem.addVariable(developmentProductivity);
        subSystem.addVariable(developmentStaffing);
        return subSystem;

    }

    private static Flow getDevelopmentFlow() {
        Rate softwareDevelopmentRate = new Rate() {
            @Override
            public Quantity flowPerTimeUnit(TimeUnit timeUnit) {
                return new Quantity(1, WaterfallSoftwareDevelopment.TASKS);
            }
        };

        return new Flow("Software Development", softwareDevelopmentRate);
    }
}
