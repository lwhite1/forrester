/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.sd.demo.waterfall;

import systems.courant.sd.measure.Quantity;
import systems.courant.sd.measure.units.dimensionless.DimensionlessUnits;
import systems.courant.sd.measure.units.item.ItemUnits;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Module;
import systems.courant.sd.model.Stock;
import systems.courant.sd.model.Variable;

import static systems.courant.sd.measure.Units.DAY;

/**
 * Workforce subsystem for the waterfall software project model.
 *
 * <p>Models hiring, assimilation, and resignation dynamics. New hires are recruited at a rate
 * proportional to the workforce gap (desired minus actual), delayed by a hiring lag.
 * New hires assimilate into experienced workers over an assimilation period. The module tracks
 * total workforce, the fraction with experience, full-time equivalent capacity, the
 * daily training overhead imposed by new hires on experienced staff, and communication
 * overhead from Brooks's Law (overhead grows as n*(n-1) with team size).
 *
 * <h3>Expected behavior with default parameters</h3>
 *
 * <p>The team starts at 6 (2 new hires + 4 experienced) and grows toward the need of 30. The
 * 8-week hiring delay and 16-week assimilation delay create a ramp: the workforce reaches ~19
 * by day 80 and ~28 by day 175. Fraction experienced starts at 0.67, dips to ~0.68 during
 * rapid hiring, then recovers above 0.88 as new hires assimilate. Communication overhead
 * starts at 0.09 (6-person team) and grows to ~2.4 (28-person team), progressively reducing
 * effective capacity. Resignation drains experienced workers at ~1/673 per day — a slow leak
 * that prevents the experienced pool from growing without bound.
 */
public class Workforce {

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

        Variable averageDailyManPowerPerStaff =
                new Variable("ADMPPPS", ItemUnits.THING, () -> 1);

        Stock newlyHiredWorkforce = new Stock("Newly hired", initialNewHires, ItemUnits.PEOPLE);
        Stock experiencedWorkforce =
                new Stock("Experienced workers", initialExperienced, ItemUnits.PEOPLE);

        totalWorkforce = new Variable("Total Workforce", ItemUnits.PEOPLE, () ->
                newlyHiredWorkforce.getQuantity().getValue()
                        + experiencedWorkforce.getQuantity().getValue());

        workforceFte = new Variable("Full Time Equivalent workforce", DimensionlessUnits.DIMENSIONLESS, () ->
                averageDailyManPowerPerStaff.getValue() * totalWorkforce.getValue());

        Variable fullTimeEquivalentExperiencedWorkforce = new Variable(
                "Full Time Equivalent Experienced Workforce", ItemUnits.PEOPLE, () ->
                experiencedWorkforce.getQuantity().getValue()
                        * averageDailyManPowerPerStaff.getValue());

        Variable workforceNeed = new Variable("Workforce need", ItemUnits.PEOPLE,
                () -> workforceNeedValue);

        Variable maxNewHiresPerExperiencedStaff =
                new Variable("Max New Hires per Experienced Staff", ItemUnits.PEOPLE, () -> 3.0);

        Variable newHireCap = new Variable("New Hire Cap", ItemUnits.PEOPLE, () ->
                maxNewHiresPerExperiencedStaff.getValue()
                        * fullTimeEquivalentExperiencedWorkforce.getValue());

        Variable totalWorkforceCap = new Variable("Total Workforce Cap", ItemUnits.PEOPLE, () ->
                newHireCap.getValue() + experiencedWorkforce.getQuantity().getValue());

        Variable workforceLevelSought = new Variable("Desired Workforce", ItemUnits.PEOPLE, () ->
                Math.min(workforceNeed.getValue(), totalWorkforceCap.getValue()));

        Variable workforceGap = new Variable("Workforce Gap", ItemUnits.PEOPLE, () ->
                workforceLevelSought.getValue() - totalWorkforce.getValue());

        Variable trainersPerNewHire =
                new Variable("Trainers per New Hire", DimensionlessUnits.DIMENSIONLESS,
                        () -> trainersPerNewHireValue);

        dailyTrainingOverhead = new Variable("Daily overhead for training", ItemUnits.PEOPLE, () ->
                trainersPerNewHire.getValue() * newlyHiredWorkforce.getQuantity().getValue());

        communicationOverhead = new Variable("Communication overhead", DimensionlessUnits.DIMENSIONLESS, () -> {
            double teamSize = totalWorkforce.getValue();
            return teamSize * (teamSize - 1) * communicationOverheadPerPair;
        });

        fractionExperienced = new Variable("Fraction of Workforce with Experience",
                DimensionlessUnits.DIMENSIONLESS, () -> {
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
