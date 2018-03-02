package com.deathrayresearch.forrester.largemodels.waterfall;


import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.dimensionless.DimensionlessUnit;
import com.deathrayresearch.forrester.measure.units.item.People;
import com.deathrayresearch.forrester.measure.units.item.Thing;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Formula;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Module;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.rate.Flow;
import com.deathrayresearch.forrester.rate.FlowPerDay;

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
                new Constant("Max New Hires per Experienced Staff", People.getInstance(), 3.0);

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


        workforce.addStock(newlyHiredWorkforce);
        workforce.addStock(experiencedWorkforce);

        newlyHiredWorkforce.addInflow(getNewHireFlow(workforceGap));
        newlyHiredWorkforce.addOutflow(getAssimilationFlow(newlyHiredWorkforce));
        experiencedWorkforce.addInflow(getAssimilationFlow(newlyHiredWorkforce));
        //experiencedWorkforce.addOutflow(resignationFlow);

        workforce.addFlow(getNewHireFlow(workforceGap));
        workforce.addFlow(getAssimilationFlow(newlyHiredWorkforce));
        workforce.addFlow(getResignationFlow(experiencedWorkforce));

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

        return new FlowPerDay("Hired") {

            @Override
            protected Quantity quantityPerDay() {
                double gap = workforceGap.getValue();
                double result = gap / hiringDelayInDays;
                double maxAmount = Math.max(result, 0.0);

                return new Quantity(maxAmount, People.getInstance());
            }
        };
    }

    private static Flow getResignationFlow(Stock experiencedWorkforce) {
        double averageEmploymentInDays = 673.0;
        return new FlowPerDay("Resigned") {
            @Override
            protected Quantity quantityPerDay() {
                return new Quantity(experiencedWorkforce.getQuantity().getValue()
                        / averageEmploymentInDays, People.getInstance());
            }
        };
    }

    private static Flow getAssimilationFlow(Stock newHires) {
        final double assimilationDelayInDays = 16.0 * 7;

        return new FlowPerDay("Assimilated hires") {
            @Override
            protected Quantity quantityPerDay() {
                return newHires.getQuantity().divide(assimilationDelayInDays);
            }
        };
    }
}
