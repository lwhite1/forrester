package com.deathrayresearch.forrester.demo.waterfall;

import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.dimensionless.DimensionlessUnits;
import com.deathrayresearch.forrester.measure.units.item.ItemUnit;
import com.deathrayresearch.forrester.measure.units.item.ItemUnits;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Module;
import com.deathrayresearch.forrester.model.Variable;

/**
 * Staff allocation subsystem for the waterfall software project model.
 *
 * <p>Divides the available workforce between software production and QA based on a planned
 * QA fraction. Daily resources are first reduced by training overhead (from {@link Workforce})
 * and communication overhead (Brooks's Law), then split between development and quality
 * assurance. An overhead loss constant further reduces the effective capacity.
 */
public class StaffAllocation {

    private static final Unit PERSON_DAYS_PER_DAY = new ItemUnit("Person days per day");
    private static final DimensionlessUnits DIMENSIONLESS_UNIT = DimensionlessUnits.DIMENSIONLESS;

    private final Module module;
    private final Variable dailyResourcesForProduction;
    private final Variable dailyResourcesForQA;

    public StaffAllocation(Variable totalWorkforce, Variable dailyTrainingOverhead,
                           Variable communicationOverhead,
                           double plannedFractionForQA, double overheadLoss) {
        module = new Module("Staff Allocation");

        Constant averageDailyManPowerPerStaff =
                new Constant("ADMPPPS", ItemUnits.THING, 1);

        Variable totalDailyStaffing = new Variable("Total daily dev. resources",
                ItemUnits.PEOPLE,
                () -> totalWorkforce.getValue() * averageDailyManPowerPerStaff.getValue());

        Constant plannedFractionOfStaffForQA =
                new Constant("Planned fraction of resources for QA", DIMENSIONLESS_UNIT,
                        plannedFractionForQA);

        Variable dailyResourcesAvailable =
                new Variable("Daily resources available after overhead",
                        PERSON_DAYS_PER_DAY,
                        () -> Math.max(0, totalDailyStaffing.getValue()
                                - dailyTrainingOverhead.getValue()
                                - communicationOverhead.getValue()));

        Variable actualFractionOfStaffForQA =
                new Variable("Actual fraction of resources for QA", DIMENSIONLESS_UNIT,
                        plannedFractionOfStaffForQA::getValue);

        Constant lossFromOverhead =
                new Constant("Loss from overhead", DIMENSIONLESS_UNIT, overheadLoss);

        dailyResourcesForQA = new Variable("Daily resources performing QA",
                PERSON_DAYS_PER_DAY,
                () -> Math.min(
                        actualFractionOfStaffForQA.getValue() * totalDailyStaffing.getValue(),
                        (1.0 - lossFromOverhead.getValue())
                                * dailyResourcesAvailable.getValue()));

        dailyResourcesForProduction = new Variable("Daily resources for software production",
                PERSON_DAYS_PER_DAY,
                () -> dailyResourcesAvailable.getValue()
                        - dailyResourcesForQA.getValue());

        module.addVariable(dailyResourcesAvailable);
        module.addVariable(actualFractionOfStaffForQA);
        module.addVariable(totalDailyStaffing);
        module.addVariable(dailyResourcesForQA);
        module.addVariable(dailyResourcesForProduction);
    }

    public StaffAllocation(Variable totalWorkforce, Variable dailyTrainingOverhead,
                           Variable communicationOverhead) {
        this(totalWorkforce, dailyTrainingOverhead, communicationOverhead, 0.15, 0.10);
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
