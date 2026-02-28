package com.deathrayresearch.forrester.demo.waterfall;


import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.dimensionless.DimensionlessUnits;
import com.deathrayresearch.forrester.measure.units.item.ItemUnits;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Formula;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Module;
import com.deathrayresearch.forrester.model.Variable;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.demo.waterfall.WaterfallSoftwareDevelopmentDemo.DAILY_RESOURCES_FOR_TRAINING;
import static com.deathrayresearch.forrester.demo.waterfall.WaterfallSoftwareDevelopmentDemo.DESIRED_WORKFORCE;
import static com.deathrayresearch.forrester.demo.waterfall.WaterfallSoftwareDevelopmentDemo.EXPERIENCED;
import static com.deathrayresearch.forrester.demo.waterfall.WaterfallSoftwareDevelopmentDemo.EXPERIENCED_WORKFORCE_FTE;
import static com.deathrayresearch.forrester.demo.waterfall.WaterfallSoftwareDevelopmentDemo.FRACTION_OF_WORKFORCE_WITH_EXPERIENCE;
import static com.deathrayresearch.forrester.demo.waterfall.WaterfallSoftwareDevelopmentDemo.NEW_HIRE_CAP;
import static com.deathrayresearch.forrester.demo.waterfall.WaterfallSoftwareDevelopmentDemo.NEWLY_HIRED;
import static com.deathrayresearch.forrester.demo.waterfall.WaterfallSoftwareDevelopmentDemo.TOTAL_WORKFORCE;
import static com.deathrayresearch.forrester.demo.waterfall.WaterfallSoftwareDevelopmentDemo.TOTAL_WORKFORCE_CAP;
import static com.deathrayresearch.forrester.demo.waterfall.WaterfallSoftwareDevelopmentDemo.WORKFORCE;
import static com.deathrayresearch.forrester.demo.waterfall.WaterfallSoftwareDevelopmentDemo.WORKFORCE_FTE;
import static com.deathrayresearch.forrester.demo.waterfall.WaterfallSoftwareDevelopmentDemo.WORKFORCE_GAP;
import static com.deathrayresearch.forrester.demo.waterfall.WaterfallSoftwareDevelopmentDemo.WORKFORCE_NEED;

/**
 * Workforce subsystem for the waterfall software project model.
 *
 * <p>Models hiring, assimilation, and resignation dynamics. New hires are recruited at a rate
 * proportional to the workforce gap (desired minus actual), delayed by an 8-week hiring lag.
 * New hires assimilate into experienced workers over a 16-week period. The module tracks
 * total workforce, the fraction with experience, full-time equivalent capacity, and the
 * daily training overhead imposed by new hires on experienced staff.
 */
class Workforce {

    static final Constant AVERAGE_DAILY_MAN_POWER_PER_STAFF = new Constant("ADMPPPS", ItemUnits.THING, 1);
    private static final Unit PEOPLE = ItemUnits.PEOPLE;
    private static final DimensionlessUnits DIMENSIONLESS_UNIT = DimensionlessUnits.DIMENSIONLESS;

    static Module getWorkforce() {

        Module workforce = new Module(WORKFORCE);

        Stock newlyHiredWorkforce = new Stock(NEWLY_HIRED, 2.0, ItemUnits.PEOPLE);
        Stock experiencedWorkforce = new Stock(EXPERIENCED, 4.0, ItemUnits.PEOPLE);
       // Stock cumulativeOverheadForTraining = new Stock(CUM_MP_FOR_TRAINING, 0.0, DIMENSIONLESS_UNIT);


        Variable totalWorkforce = new Variable(TOTAL_WORKFORCE, PEOPLE, () ->
                newlyHiredWorkforce.getQuantity().getValue()
                        + experiencedWorkforce.getQuantity().getValue());

        Variable fullTimeEquivalentWorkforce =
                new Variable(WORKFORCE_FTE, DIMENSIONLESS_UNIT, () ->
                        AVERAGE_DAILY_MAN_POWER_PER_STAFF.getValue()
                                * totalWorkforce.getValue());

        Variable fullTimeEquivalentExperiencedWorkforce = new Variable(
                EXPERIENCED_WORKFORCE_FTE, PEOPLE, () -> experiencedWorkforce.getQuantity().getValue()
                        * AVERAGE_DAILY_MAN_POWER_PER_STAFF.getValue());

        Variable workforceNeed = new Variable(WORKFORCE_NEED, PEOPLE, () -> 30.0);

        Constant maxNewHiresPerExperiencedStaff =
                new Constant("Max New Hires per Experienced Staff", ItemUnits.PEOPLE, 3.0);

        Variable newHireCap = new Variable(NEW_HIRE_CAP, PEOPLE, () ->
                maxNewHiresPerExperiencedStaff.getValue()
                        * fullTimeEquivalentExperiencedWorkforce.getValue());

        Variable totalWorkforceCap = new Variable(TOTAL_WORKFORCE_CAP, PEOPLE, () ->
                newHireCap.getValue() + experiencedWorkforce.getQuantity().getValue());

        Variable workforceLevelSought = new Variable(DESIRED_WORKFORCE, PEOPLE, () ->
                Math.min(workforceNeed.getValue(), totalWorkforceCap.getValue()));

        Variable workforceGap = new Variable(WORKFORCE_GAP, PEOPLE, () ->
                workforceLevelSought.getValue() - totalWorkforce.getValue());

        Constant trainersPerNewHire = new Constant("Trainers per New Hire", DIMENSIONLESS_UNIT, 0.2);

        Variable dailyManPowerForTraining = new Variable(DAILY_RESOURCES_FOR_TRAINING, PEOPLE, new Formula() {
            @Override
            public double getCurrentValue() {
                return trainersPerNewHire.getValue() * newlyHiredWorkforce.getQuantity().getValue();
            }
        });

        Variable fractionExperiencedWorkforce =
                new Variable(FRACTION_OF_WORKFORCE_WITH_EXPERIENCE,
                        DIMENSIONLESS_UNIT, () ->
                        experiencedWorkforce.getQuantity().getValue() / totalWorkforce.getValue());


        Flow hireFlow = getNewHireFlow(workforceGap);
        Flow assimilationFlow = getAssimilationFlow(newlyHiredWorkforce);
        Flow resignationFlow = getResignationFlow(experiencedWorkforce);

        workforce.addStock(newlyHiredWorkforce);
        workforce.addStock(experiencedWorkforce);

        newlyHiredWorkforce.addInflow(hireFlow);
        newlyHiredWorkforce.addOutflow(assimilationFlow);
        experiencedWorkforce.addInflow(assimilationFlow);
        //experiencedWorkforce.addOutflow(resignationFlow);

        workforce.addFlow(hireFlow);
        workforce.addFlow(assimilationFlow);
        workforce.addFlow(resignationFlow);

        workforce.addVariable(dailyManPowerForTraining);
        workforce.addVariable(workforceGap);
        workforce.addVariable(totalWorkforce);
        workforce.addVariable(fractionExperiencedWorkforce);
        workforce.addVariable(workforceLevelSought);
        workforce.addVariable(workforceNeed);
        workforce.addVariable(totalWorkforceCap);
        workforce.addVariable(newHireCap);
        workforce.addVariable(fullTimeEquivalentWorkforce);

        return workforce;
    }

    private static Flow getNewHireFlow(Variable workforceGap) {
        final double hiringDelayInDays = 8.0 * 7;

        return Flow.create("Hired", DAY, () -> {
            double gap = workforceGap.getValue();
            double result = gap / hiringDelayInDays;
            double maxAmount = Math.max(result, 0.0);
            return new Quantity(maxAmount, ItemUnits.PEOPLE);
        });
    }

    private static Flow getResignationFlow(Stock experiencedWorkforce) {
        double averageEmploymentInDays = 673.0;
        return Flow.create("Resigned", DAY, () ->
                new Quantity(experiencedWorkforce.getQuantity().getValue()
                        / averageEmploymentInDays, ItemUnits.PEOPLE));
    }

    private static Flow getAssimilationFlow(Stock newHires) {
        final double assimilationDelayInDays = 16.0 * 7;

        return Flow.create("Assimilated hires", DAY, () ->
                newHires.getQuantity().divide(assimilationDelayInDays));
    }
}
