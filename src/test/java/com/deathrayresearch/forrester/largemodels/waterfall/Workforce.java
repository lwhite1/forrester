package com.deathrayresearch.forrester.largemodels.waterfall;


import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.dimensionless.DimensionlessUnit;
import com.deathrayresearch.forrester.measure.units.item.People;
import com.deathrayresearch.forrester.measure.units.item.Thing;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Formula;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Module;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.rate.Rate;
import com.deathrayresearch.forrester.rate.RatePerDay;

import static com.deathrayresearch.forrester.largemodels.waterfall.WaterfallSoftwareDevelopment.*;

/**
 *
 */
class Workforce {

    static final Constant AVERAGE_DAILY_MAN_POWER_PER_STAFF = new Constant("ADMPPPS", Thing.getInstance(), 1);
    private static final People PEOPLE = People.getInstance();
    private static final DimensionlessUnit DIMENSIONLESS_UNIT = DimensionlessUnit.getInstance();

    static Module getWorkforce() {

        Module workforce = new Module(WORKFORCE);

        Stock newlyHiredWorkforce = new Stock(NEWLY_HIRED, 2.0, People.getInstance());
        Stock experiencedWorkforce = new Stock(EXPERIENCED, 4.0, People.getInstance());
       // Stock cumulativeOverheadForTraining = new Stock(CUM_MP_FOR_TRAINING, 0.0, DIMENSIONLESS_UNIT);


        Variable totalWorkforce = new Variable(TOTAL_WORKFORCE, PEOPLE, () ->
                newlyHiredWorkforce.getCurrentValue().getValue()
                        + experiencedWorkforce.getCurrentValue().getValue());

        Variable fullTimeEquivalentWorkforce =
                new Variable(WORKFORCE_FTE, DIMENSIONLESS_UNIT, () ->
                        AVERAGE_DAILY_MAN_POWER_PER_STAFF.getCurrentValue()
                                * totalWorkforce.getCurrentValue());

        Variable fullTimeEquivalentExperiencedWorkforce = new Variable(
                EXPERIENCED_WORKFORCE_FTE, PEOPLE, () -> experiencedWorkforce.getCurrentValue().getValue()
                        * AVERAGE_DAILY_MAN_POWER_PER_STAFF.getCurrentValue());

        Variable workforceNeed = new Variable(WORKFORCE_NEED, PEOPLE, () -> 30.0);

        Constant maxNewHiresPerExperiencedStaff =
                new Constant("Max New Hires per Experienced Staff", People.getInstance(), 3.0);

        Variable newHireCap = new Variable(NEW_HIRE_CAP, PEOPLE, () ->
                maxNewHiresPerExperiencedStaff.getCurrentValue()
                        * fullTimeEquivalentExperiencedWorkforce.getCurrentValue());

        Variable totalWorkforceCap = new Variable(TOTAL_WORKFORCE_CAP, PEOPLE, () ->
                newHireCap.getCurrentValue() + experiencedWorkforce.getCurrentValue().getValue());

        Variable workforceLevelSought = new Variable(DESIRED_WORKFORCE, PEOPLE, () ->
                Math.min(workforceNeed.getCurrentValue(), totalWorkforceCap.getCurrentValue()));

        Variable workforceGap = new Variable(WORKFORCE_GAP, PEOPLE, () ->
                workforceLevelSought.getCurrentValue() - totalWorkforce.getCurrentValue());

        Constant trainersPerNewHire = new Constant("Trainers per New Hire", DIMENSIONLESS_UNIT, 0.2);

        Variable dailyManPowerForTraining = new Variable(DAILY_RESOURCES_FOR_TRAINING, PEOPLE, new Formula() {
            @Override
            public double getCurrentValue() {
                return trainersPerNewHire.getCurrentValue() * newlyHiredWorkforce.getCurrentValue().getValue();
            }
        });

        Variable fractionExperiencedWorkforce =
                new Variable(FRACTION_OF_WORKFORCE_WITH_EXPERIENCE,
                        DIMENSIONLESS_UNIT, () ->
                        experiencedWorkforce.getCurrentValue().getValue() / totalWorkforce.getCurrentValue());

        Flow newHireFlow = getNewHireFlow(workforceGap);

        Flow assimilationFlow = getAssimilationFlow(newlyHiredWorkforce);

        Flow resignationFlow = getResignationFlow(experiencedWorkforce);

        workforce.addStock(newlyHiredWorkforce);
        workforce.addStock(experiencedWorkforce);

        newlyHiredWorkforce.addInflow(newHireFlow);
        newlyHiredWorkforce.addOutflow(assimilationFlow);
        experiencedWorkforce.addInflow(assimilationFlow);
        //experiencedWorkforce.addOutflow(resignationFlow);

        workforce.addFlow(newHireFlow);
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

        Rate hiringRate = new RatePerDay("Hiring Rate") {

            @Override
            protected Quantity quantityPerDay() {
                double gap = workforceGap.getCurrentValue();
                double result = gap / hiringDelayInDays;
                double maxAmount = Math.max(result, 0.0);

                return new Quantity(maxAmount, People.getInstance());
            }
        };

        return new Flow(WaterfallSoftwareDevelopment.NEW_HIRES, hiringRate);
    }

    private static Flow getResignationFlow(Stock experiencedWorkforce) {
        double averageEmploymentInDays = 673.0;
        Rate quitRate = new RatePerDay("Quit rate") {
            @Override
            protected Quantity quantityPerDay() {
                return new Quantity(experiencedWorkforce.getCurrentValue().getValue()
                        / averageEmploymentInDays, People.getInstance());
            }
        };
        return new Flow("Employees quiting", quitRate);
    }

    private static Flow getAssimilationFlow(Stock newHires) {
        final double assimilationDelayInDays = 16.0 * 7;

        Rate assimilationRate = new RatePerDay("Assimilation rate") {
            @Override
            protected Quantity quantityPerDay() {
                return newHires.getCurrentValue().divide(assimilationDelayInDays);
            }
        };

        return new Flow("Assimilated hires", assimilationRate);
    }
}
