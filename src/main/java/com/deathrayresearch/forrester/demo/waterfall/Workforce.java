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
 * proportional to the workforce gap (desired minus actual), delayed by a hiring lag.
 * New hires assimilate into experienced workers over an assimilation period. The module tracks
 * total workforce, the fraction with experience, full-time equivalent capacity, the
 * daily training overhead imposed by new hires on experienced staff, and communication
 * overhead from Brooks's Law (overhead grows as n*(n-1) with team size).
 */
public class Workforce {

    private static final DimensionlessUnits DIMENSIONLESS_UNIT = DimensionlessUnits.DIMENSIONLESS;

    private final Module module;
    private final Variable totalWorkforce;
    private final Variable fractionExperienced;
    private final Variable dailyTrainingOverhead;
    private final Variable communicationOverhead;
    private final Variable workforceFte;

    public Workforce(double initialNewHires, double initialExperienced, double workforceNeedValue,
                     double hiringDelayWeeks, double assimilationDelayWeeks,
                     double trainersPerNewHireValue, double avgEmploymentDays,
                     double communicationOverheadPerPair) {
        module = new Module("Workforce");

        Constant averageDailyManPowerPerStaff =
                new Constant("ADMPPPS", ItemUnits.THING, 1);

        Stock newlyHiredWorkforce = new Stock("Newly hired", initialNewHires, ItemUnits.PEOPLE);
        Stock experiencedWorkforce =
                new Stock("Experienced workers", initialExperienced, ItemUnits.PEOPLE);

        totalWorkforce = new Variable("Total Workforce", ItemUnits.PEOPLE, () ->
                newlyHiredWorkforce.getQuantity().getValue()
                        + experiencedWorkforce.getQuantity().getValue());

        workforceFte = new Variable("Full Time Equivalent workforce", DIMENSIONLESS_UNIT, () ->
                averageDailyManPowerPerStaff.getValue() * totalWorkforce.getValue());

        Variable fullTimeEquivalentExperiencedWorkforce = new Variable(
                "Full Time Equivalent Experienced Workforce", ItemUnits.PEOPLE, () ->
                experiencedWorkforce.getQuantity().getValue()
                        * averageDailyManPowerPerStaff.getValue());

        Variable workforceNeed = new Variable("Workforce need", ItemUnits.PEOPLE,
                () -> workforceNeedValue);

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

        Constant trainersPerNewHire =
                new Constant("Trainers per New Hire", DIMENSIONLESS_UNIT, trainersPerNewHireValue);

        dailyTrainingOverhead = new Variable("Daily overhead for training", ItemUnits.PEOPLE, () ->
                trainersPerNewHire.getValue() * newlyHiredWorkforce.getQuantity().getValue());

        communicationOverhead = new Variable("Communication overhead", DIMENSIONLESS_UNIT, () -> {
            double teamSize = totalWorkforce.getValue();
            return teamSize * (teamSize - 1) * communicationOverheadPerPair;
        });

        fractionExperienced = new Variable("Fraction of Workforce with Experience",
                DIMENSIONLESS_UNIT, () -> {
                    double total = totalWorkforce.getValue();
                    if (total <= 0) {
                        return 0.0;
                    }
                    return experiencedWorkforce.getQuantity().getValue() / total;
                });

        double hiringDelayInDays = hiringDelayWeeks * 7;
        double assimilationDelayInDays = assimilationDelayWeeks * 7;

        Flow hireFlow = Flow.create("Hired", DAY, () -> {
            double gap = workforceGap.getValue();
            double result = gap / hiringDelayInDays;
            return new Quantity(Math.max(result, 0.0), ItemUnits.PEOPLE);
        });

        Flow assimilationFlow = Flow.create("Assimilated hires", DAY, () ->
                newlyHiredWorkforce.getQuantity().divide(assimilationDelayInDays));

        Flow resignationFlow = Flow.create("Resigned", DAY, () ->
                new Quantity(experiencedWorkforce.getQuantity().getValue()
                        / avgEmploymentDays, ItemUnits.PEOPLE));

        module.addStock(newlyHiredWorkforce);
        module.addStock(experiencedWorkforce);

        newlyHiredWorkforce.addInflow(hireFlow);
        newlyHiredWorkforce.addOutflow(assimilationFlow);
        experiencedWorkforce.addInflow(assimilationFlow);
        experiencedWorkforce.addOutflow(resignationFlow);

        module.addFlow(hireFlow);
        module.addFlow(assimilationFlow);
        module.addFlow(resignationFlow);

        module.addVariable(dailyTrainingOverhead);
        module.addVariable(communicationOverhead);
        module.addVariable(workforceGap);
        module.addVariable(totalWorkforce);
        module.addVariable(fractionExperienced);
        module.addVariable(workforceLevelSought);
        module.addVariable(workforceNeed);
        module.addVariable(totalWorkforceCap);
        module.addVariable(newHireCap);
        module.addVariable(workforceFte);
    }

    public Workforce() {
        this(2, 4, 30, 8, 16, 0.2, 673, 0.003);
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

    public Variable getCommunicationOverhead() {
        return communicationOverhead;
    }

    public Variable getWorkforceFte() {
        return workforceFte;
    }
}
