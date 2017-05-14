package com.deathrayresearch.forrester.largemodels.waterfall;


import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.item.People;
import com.deathrayresearch.forrester.measure.units.item.Thing;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.SubSystem;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.rate.Rate;

/**
 *
 */
class Workforce {

    static final Constant AVERAGE_DAILY_MAN_POWER_PER_STAFF = new Constant("ADMPPPS", Thing.getInstance(), 1);

    static SubSystem getWorkforce() {

        SubSystem workforce = new SubSystem(WaterfallSoftwareDevelopment.WORKFORCE);

        Stock newlyHiredWorkforce = new Stock(WaterfallSoftwareDevelopment.NEWLY_HIRED, 2.0, People.getInstance());
        Stock experiencedWorkforce = new Stock(WaterfallSoftwareDevelopment.EXPERIENCED, 4.0, People.getInstance());

        Variable totalWorkforce = new Variable(WaterfallSoftwareDevelopment.TOTAL_WORKFORCE, () ->
                newlyHiredWorkforce.getCurrentValue().getValue()
                        + experiencedWorkforce.getCurrentValue().getValue());

        Variable fullTimeEquivalentExperiencedWorkforce =
                new Variable(WaterfallSoftwareDevelopment.WORKFORCE_FTE, () ->
                        AVERAGE_DAILY_MAN_POWER_PER_STAFF.getCurrentValue()
                                * totalWorkforce.getCurrentValue());

        Variable workforceNeed = new Variable(WaterfallSoftwareDevelopment.WORKFORCE_NEED, () -> 30.0);

        Constant maxNewHiresPerExperiencedStaff =
                new Constant("Max New Hires per Experienced Staff", People.getInstance(), 3.0);

        Variable newHireCap = new Variable(WaterfallSoftwareDevelopment.NEW_HIRE_CAP, () ->
                maxNewHiresPerExperiencedStaff.getCurrentValue()
                        * fullTimeEquivalentExperiencedWorkforce.getCurrentValue());

        Variable totalWorkforceCap = new Variable(WaterfallSoftwareDevelopment.TOTAL_WORKFORCE_CAP, () ->
                newHireCap.getCurrentValue() + experiencedWorkforce.getCurrentValue().getValue());

        Variable workforceLevelSought = new Variable(WaterfallSoftwareDevelopment.DESIRED_WORKFORCE, () ->
                Math.min(workforceNeed.getCurrentValue(), totalWorkforceCap.getCurrentValue()));

        Variable workforceGap = new Variable(WaterfallSoftwareDevelopment.WORKFORCE_GAP, () ->
                workforceLevelSought.getCurrentValue() - totalWorkforce.getCurrentValue());

        Variable fractionExperiencedWorkforce = new Variable(WaterfallSoftwareDevelopment.FRACTION_OF_WORKFORCE_WITH_EXPERIENCE, () ->
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

    private static Flow getNewHireFlow(Variable workforceGap) {
        final double hiringDelayInDays = 8.0 * 7;
        Rate hiringRate = timeUnit -> {
            double gap = workforceGap.getCurrentValue();
            double result = gap / hiringDelayInDays;
            double maxAmount = Math.max(result, 0.0);

            return new Quantity(maxAmount, People.getInstance());
        };

        return new Flow(WaterfallSoftwareDevelopment.NEW_HIRES, hiringRate);
    }

    private static Flow getResignationFlow(Stock experiencedWorkforce) {
        double averageEmploymentInDays = 673.0;
        Rate quitRate = timeUnit ->
                new Quantity(experiencedWorkforce.getCurrentValue().getValue()
                        / averageEmploymentInDays, People.getInstance());
        return new Flow("Employees quiting", quitRate);
    }

    private static Flow getAssimilationFlow(Stock newHires) {
        final double assimilationDelayInDays = 16.0 * 7;

        Rate assimilationRate = timeUnit -> newHires.getCurrentValue().divide(assimilationDelayInDays);

        return new Flow("Assimilated hires", assimilationRate);
    }

}
