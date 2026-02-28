package com.deathrayresearch.forrester.demo.waterfall;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.dimensionless.DimensionlessUnits;
import com.deathrayresearch.forrester.measure.units.item.ItemUnits;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Module;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;

import static com.deathrayresearch.forrester.measure.Units.DAY;

/**
 * Workforce subsystem for the waterfall software project model.
 *
 * <p>Models hiring, assimilation, and resignation dynamics. New hires are recruited at a rate
 * proportional to the workforce gap (desired minus actual), delayed by an 8-week hiring lag.
 * New hires assimilate into experienced workers over a 16-week period. The module tracks
 * total workforce, the fraction with experience, full-time equivalent capacity, and the
 * daily training overhead imposed by new hires on experienced staff.
 */
public class Workforce {

    private static final DimensionlessUnits DIMENSIONLESS_UNIT = DimensionlessUnits.DIMENSIONLESS;

    private final Module module;
    private final Variable totalWorkforce;
    private final Variable fractionExperienced;
    private final Variable dailyTrainingOverhead;
    private final Variable workforceFte;

    public Workforce() {
        module = new Module("Workforce");

        Constant averageDailyManPowerPerStaff =
                new Constant("ADMPPPS", ItemUnits.THING, 1);

        Stock newlyHiredWorkforce = new Stock("Newly hired", 2.0, ItemUnits.PEOPLE);
        Stock experiencedWorkforce = new Stock("Experienced workers", 4.0, ItemUnits.PEOPLE);

        totalWorkforce = new Variable("Total Workforce", ItemUnits.PEOPLE, () ->
                newlyHiredWorkforce.getQuantity().getValue()
                        + experiencedWorkforce.getQuantity().getValue());

        workforceFte = new Variable("Full Time Equivalent workforce", DIMENSIONLESS_UNIT, () ->
                averageDailyManPowerPerStaff.getValue() * totalWorkforce.getValue());

        Variable fullTimeEquivalentExperiencedWorkforce = new Variable(
                "Full Time Equivalent Experienced Workforce", ItemUnits.PEOPLE, () ->
                experiencedWorkforce.getQuantity().getValue()
                        * averageDailyManPowerPerStaff.getValue());

        Variable workforceNeed = new Variable("Workforce need", ItemUnits.PEOPLE, () -> 30.0);

        Constant maxNewHiresPerExperiencedStaff =
                new Constant("Max New Hires per Experienced Staff", ItemUnits.PEOPLE, 3.0);

        Variable newHireCap = new Variable("New Hire Cap", ItemUnits.PEOPLE, () ->
                maxNewHiresPerExperiencedStaff.getValue()
                        * fullTimeEquivalentExperiencedWorkforce.getValue());

        Variable totalWorkforceCap = new Variable("Total Workforce Cap", ItemUnits.PEOPLE, () ->
                newHireCap.getValue() + experiencedWorkforce.getQuantity().getValue());

        Variable workforceLevelSought = new Variable("Desired Workforce", ItemUnits.PEOPLE, () ->
                Math.min(workforceNeed.getValue(), totalWorkforceCap.getValue()));

        Variable workforceGap = new Variable("Workforce Gap", ItemUnits.PEOPLE, () ->
                workforceLevelSought.getValue() - totalWorkforce.getValue());

        Constant trainersPerNewHire = new Constant("Trainers per New Hire", DIMENSIONLESS_UNIT, 0.2);

        dailyTrainingOverhead = new Variable("Daily overhead for training", ItemUnits.PEOPLE, () ->
                trainersPerNewHire.getValue() * newlyHiredWorkforce.getQuantity().getValue());

        fractionExperienced = new Variable("Fraction of Workforce with Experience",
                DIMENSIONLESS_UNIT, () ->
                experiencedWorkforce.getQuantity().getValue() / totalWorkforce.getValue());

        Flow hireFlow = createNewHireFlow(workforceGap);
        Flow assimilationFlow = createAssimilationFlow(newlyHiredWorkforce);
        Flow resignationFlow = createResignationFlow(experiencedWorkforce);

        module.addStock(newlyHiredWorkforce);
        module.addStock(experiencedWorkforce);

        newlyHiredWorkforce.addInflow(hireFlow);
        newlyHiredWorkforce.addOutflow(assimilationFlow);
        experiencedWorkforce.addInflow(assimilationFlow);

        module.addFlow(hireFlow);
        module.addFlow(assimilationFlow);
        module.addFlow(resignationFlow);

        module.addVariable(dailyTrainingOverhead);
        module.addVariable(workforceGap);
        module.addVariable(totalWorkforce);
        module.addVariable(fractionExperienced);
        module.addVariable(workforceLevelSought);
        module.addVariable(workforceNeed);
        module.addVariable(totalWorkforceCap);
        module.addVariable(newHireCap);
        module.addVariable(workforceFte);
    }

    public Module getModule() {
        return module;
    }

    public Variable getTotalWorkforce() {
        return totalWorkforce;
    }

    public Variable getFractionExperienced() {
        return fractionExperienced;
    }

    public Variable getDailyTrainingOverhead() {
        return dailyTrainingOverhead;
    }

    public Variable getWorkforceFte() {
        return workforceFte;
    }

    private static Flow createNewHireFlow(Variable workforceGap) {
        final double hiringDelayInDays = 8.0 * 7;
        return Flow.create("Hired", DAY, () -> {
            double gap = workforceGap.getValue();
            double result = gap / hiringDelayInDays;
            double maxAmount = Math.max(result, 0.0);
            return new Quantity(maxAmount, ItemUnits.PEOPLE);
        });
    }

    private static Flow createResignationFlow(Stock experiencedWorkforce) {
        double averageEmploymentInDays = 673.0;
        return Flow.create("Resigned", DAY, () ->
                new Quantity(experiencedWorkforce.getQuantity().getValue()
                        / averageEmploymentInDays, ItemUnits.PEOPLE));
    }

    private static Flow createAssimilationFlow(Stock newHires) {
        final double assimilationDelayInDays = 16.0 * 7;
        return Flow.create("Assimilated hires", DAY, () ->
                newHires.getQuantity().divide(assimilationDelayInDays));
    }
}
