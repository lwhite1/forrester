package com.deathrayresearch.forrester.demo.waterfall;

import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.dimensionless.DimensionlessUnits;
import com.deathrayresearch.forrester.measure.units.item.ItemUnit;
import com.deathrayresearch.forrester.measure.units.item.ItemUnits;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Module;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;

/**
 * Staff allocation subsystem for the waterfall software project model.
 *
 * <p>Divides the available workforce between software production and QA based on a planned
 * QA fraction. Daily resources are first reduced by the training overhead from the
 * {@link Workforce} module, then split between development and quality assurance. An
 * overhead loss constant further reduces the effective capacity available for productive work.
 */
public class StaffAllocation {

    private static final Unit PERSON_DAYS_PER_DAY = new ItemUnit("Person days per day");
    private static final DimensionlessUnits DIMENSIONLESS_UNIT = DimensionlessUnits.DIMENSIONLESS;

    private final Module module;
    private final Variable dailyResourcesForProduction;
    private final Variable dailyResourcesForQA;

    public StaffAllocation(Variable totalWorkforce, Variable dailyTrainingOverhead) {
        module = new Module("Staff Allocation");

        Constant averageDailyManPowerPerStaff =
                new Constant("ADMPPPS", ItemUnits.THING, 1);

        Variable totalDailyStaffing = new Variable("Total daily dev. resources",
                ItemUnits.PEOPLE,
                () -> totalWorkforce.getValue() * averageDailyManPowerPerStaff.getValue());

        Constant plannedFractionOfStaffForQA =
                new Constant("Planned fraction of resources for QA", DIMENSIONLESS_UNIT, .15);

        Variable dailyResourcesAvailableAfterTrainingOverhead =
                new Variable("Daily resources available after training overhead",
                        PERSON_DAYS_PER_DAY,
                        () -> totalDailyStaffing.getValue() - dailyTrainingOverhead.getValue());

        Variable actualFractionOfStaffForQA =
                new Variable("Actual fraction of resources for QA", DimensionlessUnits.DIMENSIONLESS,
                        plannedFractionOfStaffForQA::getValue);

        Constant lossFromOverhead = new Constant("Loss from overhead", DimensionlessUnits.DIMENSIONLESS, .1);

        dailyResourcesForQA = new Variable("Daily resources performing QA",
                PERSON_DAYS_PER_DAY,
                () -> Math.min(
                        actualFractionOfStaffForQA.getValue() * totalDailyStaffing.getValue(),
                        (1.0 - lossFromOverhead.getValue())
                                * dailyResourcesAvailableAfterTrainingOverhead.getValue()));

        dailyResourcesForProduction = new Variable("Daily resources for software production",
                PERSON_DAYS_PER_DAY,
                () -> dailyResourcesAvailableAfterTrainingOverhead.getValue()
                        - dailyResourcesForQA.getValue());

        Stock cumulativeManDaysExpended =
                new Stock("Cumulative Person-Days Expended", 0.0001, new ItemUnit("Person day"));

        module.addStock(cumulativeManDaysExpended);
        module.addVariable(dailyResourcesAvailableAfterTrainingOverhead);
        module.addVariable(actualFractionOfStaffForQA);
        module.addVariable(totalDailyStaffing);
        module.addVariable(dailyResourcesForQA);
        module.addVariable(dailyResourcesForProduction);
    }

    public Module getModule() {
        return module;
    }

    public Variable getDailyResourcesForProduction() {
        return dailyResourcesForProduction;
    }

    public Variable getDailyResourcesForQA() {
        return dailyResourcesForQA;
    }
}
