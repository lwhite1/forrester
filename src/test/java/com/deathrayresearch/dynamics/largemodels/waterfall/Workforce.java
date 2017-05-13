package com.deathrayresearch.dynamics.largemodels.waterfall;

import com.deathrayresearch.dynamics.measure.Quantity;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Item;
import com.deathrayresearch.dynamics.measure.dimension.Time;
import com.deathrayresearch.dynamics.model.*;
import com.deathrayresearch.dynamics.rate.Rate;

import static com.deathrayresearch.dynamics.largemodels.waterfall.WaterfallSoftwareDevelopment.*;

/**
 *
 */
class Workforce {

    private static final Constant averageDailyManPowerPerStaff = new Constant("ADMPPPS", 1);

    static SubSystem getWorkforce() {

        SubSystem workforce = new SubSystem(WORKFORCE);

        Stock<Item> newlyHiredWorkforce = new Stock<>(NEWLY_HIRED, 2.0, PEOPLE);
        Stock<Item> experiencedWorkforce = new Stock<>(EXPERIENCED, 4.0, PEOPLE);

        Variable totalWorkforce = new Variable(TOTAL_WORKFORCE, () ->
                newlyHiredWorkforce.getCurrentValue().getValue()
                        + experiencedWorkforce.getCurrentValue().getValue());

        Variable fullTimeEquivalentExperiencedWorkforce =
                new Variable(WORKFORCE_FTE, () ->
                        averageDailyManPowerPerStaff.getCurrentValue()
                                * totalWorkforce.getCurrentValue());

        Variable workforceNeed = new Variable(WORKFORCE_NEED, () -> 30.0);

        Constant maxNewHiresPerExperiencedStaff =
                new Constant("Max New Hires per Experienced Staff", 3.0);

        Variable newHireCap = new Variable(NEW_HIRE_CAP, () ->
                maxNewHiresPerExperiencedStaff.getCurrentValue()
                        * fullTimeEquivalentExperiencedWorkforce.getCurrentValue());

        Variable totalWorkforceCap = new Variable(TOTAL_WORKFORCE_CAP, () ->
                newHireCap.getCurrentValue() + experiencedWorkforce.getCurrentValue().getValue());

        Variable workforceLevelSought = new Variable(DESIRED_WORKFORCE, () ->
                Math.min(workforceNeed.getCurrentValue(), totalWorkforceCap.getCurrentValue()));

        Variable workforceGap = new Variable(WORKFORCE_GAP, () ->
                workforceLevelSought.getCurrentValue() - totalWorkforce.getCurrentValue());

        Variable fractionExperiencedWorkforce = new Variable(FRACTION_OF_WORKFORCE_WITH_EXPERIENCE, () ->
                experiencedWorkforce.getCurrentValue().getValue() / totalWorkforce.getCurrentValue());

        Flow<Item> newHireFlow = getNewHireFlow(workforceGap);

        Flow<Item> assimilationFlow = getAssimilationFlow(newlyHiredWorkforce);

        Flow<Item> resignationFlow = getResignationFlow(experiencedWorkforce);

        workforce.addStock(newlyHiredWorkforce);
        workforce.addStock(experiencedWorkforce);

        newlyHiredWorkforce.addInflow(newHireFlow);
        newlyHiredWorkforce.addOutflow(assimilationFlow);
        experiencedWorkforce.addInflow(assimilationFlow);
        //experiencedWorkforce.addOutflow(resignationFlow);

        workforce.addFlow(newHireFlow);
        workforce.addFlow(assimilationFlow);
        workforce.addFlow(resignationFlow);

        workforce.addVariable(workforceGap);
        workforce.addVariable(totalWorkforce);
        workforce.addVariable(fractionExperiencedWorkforce);
        workforce.addVariable(workforceLevelSought);
        workforce.addVariable(workforceNeed);
        workforce.addVariable(totalWorkforceCap);
        workforce.addVariable(newHireCap);
        workforce.addVariable(fullTimeEquivalentExperiencedWorkforce);

        return workforce;
    }

    private static Flow<Item> getNewHireFlow(Variable workforceGap) {
        final double hiringDelayInDays = 8.0 * 7;
        Rate<Item> hiringRate = new Rate<Item>() {
            @Override
            public Quantity<Item> flowPerTimeUnit(Unit<Time> timeUnit) {
                double gap = workforceGap.getCurrentValue();
                double result = gap / hiringDelayInDays;
                double maxAmount = Math.max(result, 0.0);

                return new Quantity<>(maxAmount, PEOPLE);
            }
        };

        return new Flow<>(NEW_HIRES, hiringRate);
    }

    private static Flow<Item> getResignationFlow(Stock<Item> experiencedWorkforce) {
        double averageEmploymentInDays = 673.0;
        Rate<Item> quitRate = timeUnit ->
                new Quantity<>(experiencedWorkforce.getCurrentValue().getValue()
                        / averageEmploymentInDays, PEOPLE);
        return new Flow<>("Employees quiting", quitRate);
    }

    private static Flow<Item> getAssimilationFlow(Stock<Item> newHires) {
        final double assimilationDelayInDays = 16.0 * 7;

        Rate<Item> assimilationRate = new Rate<Item>() {
            @Override
            public Quantity<Item> flowPerTimeUnit(Unit<Time> timeUnit) {
                return newHires.getCurrentValue().divide(assimilationDelayInDays);
            }
        };

        return new Flow<>("Assimilated hires", assimilationRate);
    }

}
