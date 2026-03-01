package com.deathrayresearch.forrester.demo.waterfall;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.TimeUnits;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

/**
 * Models a waterfall software project using the Abdel-Hamid &amp; Madnick (1991) structure.
 *
 * <p>Three modules — Workforce, Staff Allocation, and Software Production — are composed into
 * a single model. The core rework cycle in Software Production drives the classic waterfall
 * pathology: deferred integration causes errors to stay hidden until late in the project,
 * when rework becomes expensive and floods the schedule.
 *
 * <p>Key dynamics demonstrated:
 * <ul>
 *   <li>Brooks's Law — communication overhead grows quadratically with team size</li>
 *   <li>Rework cycle — FCC splits work into correct and erroneous; errors accumulate hidden</li>
 *   <li>Waterfall integration tax — rework discovery and cost increase late in the project</li>
 * </ul>
 */
public class WaterfallSoftwareDevelopmentDemo {

    public static void main(String[] args) {
        new WaterfallSoftwareDevelopmentDemo().run(
                // Workforce parameters
                2, 4, 30, 8, 16, 0.2, 673, 0.003,
                // StaffAllocation parameters
                0.15, 0.10,
                // SoftwareProduction parameters
                500, 0.80, 1.0, 0.5, 0.05, 0.40, 1.5,
                // Simulation duration in days (1.5 years ≈ 548 days)
                548
        );
    }

    public void run(double initialNewHires, double initialExperienced, double workforceNeed,
                    double hiringDelayWeeks, double assimilationDelayWeeks,
                    double trainersPerNewHire, double avgEmploymentDays,
                    double communicationOverheadPerPair,
                    double plannedFractionForQA, double overheadLoss,
                    double projectSize, double baseFCC,
                    double nominalProductivityExp, double nominalProductivityNew,
                    double baseReworkDiscoveryFraction, double testingReworkDiscoveryFraction,
                    double integrationCoefficient,
                    int durationDays) {

        Model model = getModel(
                initialNewHires, initialExperienced, workforceNeed,
                hiringDelayWeeks, assimilationDelayWeeks,
                trainersPerNewHire, avgEmploymentDays, communicationOverheadPerPair,
                plannedFractionForQA, overheadLoss,
                projectSize, baseFCC,
                nominalProductivityExp, nominalProductivityNew,
                baseReworkDiscoveryFraction, testingReworkDiscoveryFraction,
                integrationCoefficient);

        Quantity duration = new Quantity(durationDays, TimeUnits.DAY);
        Simulation simulation = new Simulation(model, TimeUnits.DAY, duration);
        simulation.addEventHandler(new StockLevelChartViewer());
        simulation.execute();
    }

    public Model getModel() {
        return getModel(2, 4, 30, 8, 16, 0.2, 673, 0.003,
                0.15, 0.10,
                500, 0.80, 1.0, 0.5, 0.05, 0.40, 1.5);
    }

    public Model getModel(double initialNewHires, double initialExperienced, double workforceNeed,
                           double hiringDelayWeeks, double assimilationDelayWeeks,
                           double trainersPerNewHire, double avgEmploymentDays,
                           double communicationOverheadPerPair,
                           double plannedFractionForQA, double overheadLoss,
                           double projectSize, double baseFCC,
                           double nominalProductivityExp, double nominalProductivityNew,
                           double baseReworkDiscoveryFraction,
                           double testingReworkDiscoveryFraction,
                           double integrationCoefficient) {

        Workforce workforce = new Workforce(
                initialNewHires, initialExperienced, workforceNeed,
                hiringDelayWeeks, assimilationDelayWeeks,
                trainersPerNewHire, avgEmploymentDays, communicationOverheadPerPair);

        StaffAllocation staffAllocation = new StaffAllocation(
                workforce.getTotalWorkforce(),
                workforce.getDailyTrainingOverhead(),
                workforce.getCommunicationOverhead(),
                plannedFractionForQA, overheadLoss);

        SoftwareProduction softwareProduction = new SoftwareProduction(
                staffAllocation.getDailyResourcesForProduction(),
                staffAllocation.getDailyResourcesForQA(),
                workforce.getFractionExperienced(),
                projectSize, baseFCC,
                nominalProductivityExp, nominalProductivityNew,
                baseReworkDiscoveryFraction, testingReworkDiscoveryFraction,
                integrationCoefficient);

        Model model = new Model("Waterfall");
        model.addModule(workforce.getModule());
        model.addModule(staffAllocation.getModule());
        model.addModule(softwareProduction.getModule());
        return model;
    }
}
