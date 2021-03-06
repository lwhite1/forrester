package com.deathrayresearch.forrester.largemodels.waterfall;

import com.deathrayresearch.forrester.largemodels.waterfall.units.PersonDays;
import com.deathrayresearch.forrester.largemodels.waterfall.units.PersonDaysPerDay;
import com.deathrayresearch.forrester.measure.units.dimensionless.DimensionlessUnit;
import com.deathrayresearch.forrester.measure.units.item.Thing;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Formula;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Module;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;

import static com.deathrayresearch.forrester.largemodels.waterfall.WaterfallSoftwareDevelopment.*;

/**
 */
class StaffAllocation {

    private static final PersonDaysPerDay PERSON_DAYS_PER_DAY = PersonDaysPerDay.getInstance();
    private static final DimensionlessUnit DIMENSIONLESS_UNIT = DimensionlessUnit.getInstance();

    static Module getStaffAllocationModule(Model model) {

        Module module = new Module(WaterfallSoftwareDevelopment.STAFF_ALLOCATION);

        Variable totalDailyStaffing = new Variable("Total daily dev. resources",
                PEOPLE,
                () -> model.getVariable(WaterfallSoftwareDevelopment.TOTAL_WORKFORCE).getValue()
                        * Workforce.AVERAGE_DAILY_MAN_POWER_PER_STAFF.getValue());


        Constant plannedFractionOfStaffForQA = 
                new Constant("Planned fraction of resources for QA", DIMENSIONLESS_UNIT, .15);

        Variable dailyResourcesAvailableAfterTrainingOverhead = 
                new Variable("Daily resources available after training overhead",
                        PERSON_DAYS_PER_DAY,
                        new Formula() {
                            @Override
                            public double getCurrentValue() {
                                return totalDailyStaffing.getValue()
                                        - model.getVariable(DAILY_RESOURCES_FOR_TRAINING).getValue();
                            }
                        });
        
        Variable actualFractionOfStaffForQA = 
                new Variable("Actual fraction of resources for QA", DimensionlessUnit.getInstance(), new Formula() {
                    @Override
                    public double getCurrentValue() {
                        return plannedFractionOfStaffForQA.getValue();
                    }
                });

        Constant lossFromOverhead = new Constant("Loss from overhead", DimensionlessUnit.getInstance(), .1);
        
        Variable dailyResourcesForQA =
                new Variable(DAILY_RESOURCES_PERFORMING_QA,
                        PERSON_DAYS_PER_DAY,
                        new Formula() {
                            @Override
                            public double getCurrentValue() {
                                return Math.min(
                                        actualFractionOfStaffForQA.getValue() * totalDailyStaffing.getValue(),
                                        (1.0 - lossFromOverhead.getValue())
                                                * dailyResourcesAvailableAfterTrainingOverhead.getValue());
                            }
                        }
                );
        
        Variable dailyResourcesForSoftwareProduction = 
                new Variable(DAILY_RESOURCES_FOR_SOFTWARE_PRODUCTION,
                        PERSON_DAYS_PER_DAY,
                        new Formula() {
                            @Override
                            public double getCurrentValue() {
                                return dailyResourcesAvailableAfterTrainingOverhead.getValue()
                                        - dailyResourcesForQA.getValue();
                            }
                        });
        
        Constant qualityObjective = new Constant("Quality Objective", Thing.getInstance(), 0);

        Stock cumulativeManDaysExpended =
                new Stock("Cumulative Person-Days Expended", 0.0001, PersonDays.getInstance());

        module.addStock(cumulativeManDaysExpended);
        module.addVariable(dailyResourcesAvailableAfterTrainingOverhead);
        module.addVariable(actualFractionOfStaffForQA);
        module.addVariable(totalDailyStaffing);
        module.addVariable(dailyResourcesForQA);
        module.addVariable(dailyResourcesForSoftwareProduction);

        return module;
    }
}
