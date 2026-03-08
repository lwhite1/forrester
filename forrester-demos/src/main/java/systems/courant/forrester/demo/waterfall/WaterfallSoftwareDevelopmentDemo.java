package systems.courant.forrester.demo.waterfall;

import systems.courant.forrester.Simulation;
import systems.courant.forrester.measure.Quantity;
import systems.courant.forrester.measure.units.time.TimeUnits;
import systems.courant.forrester.model.Model;
import systems.courant.forrester.model.ModelMetadata;
import systems.courant.forrester.ui.StockLevelChartViewer;

/**
 * Models a waterfall software project using the Abdel-Hamid &amp; Madnick (1991) structure.
 *
 * <p>Three modules — {@link Workforce}, {@link StaffAllocation}, and {@link SoftwareProduction} —
 * are composed into a single model. The core rework cycle in Software Production drives the
 * classic waterfall pathology: deferred integration causes errors to stay hidden until late in the
 * project, when rework becomes expensive and floods the schedule.
 *
 * <h3>Expected behavior with default parameters (200 days)</h3>
 *
 * <p><b>Days 0–80 (development phase):</b> The workforce ramps from 6 to ~19 people. Development
 * proceeds steadily, draining Tasks Remaining from 500 toward zero. Tasks Completed climbs to
 * ~380. Undiscovered Rework peaks around day 30 (~13 tasks) then declines as the rising Rework
 * Discovery Fraction catches hidden errors. Rework to Do accumulates from 0 to ~85 tasks. The
 * project appears ~76% complete, but the growing rework backlog is the hidden debt.
 *
 * <p><b>Day ~84 (development ends):</b> Tasks Remaining reaches zero — all 500 original tasks
 * have been attempted. However, only ~392 are correctly completed. The remaining ~86 sit in
 * Rework to Do, awaiting expensive fixes.
 *
 * <p><b>Days 84–175 (rework grind):</b> No new development occurs. The QA team (~3–4 people)
 * works through the rework backlog, but the Integration Effort Multiplier (now ~2.0–2.4x) makes
 * each fix costly, and rework itself generates new errors (FCC &lt; 1). Rework to Do drains
 * gradually to zero. Final Tasks Completed stabilizes at ~479 — roughly 21 tasks are "lost" to
 * the compounding error-on-error dynamics of the rework cycle.
 *
 * <p><b>Day 175+:</b> All stocks are stable. The project is complete.
 *
 * <p>The waterfall pathology is visible in the gap between apparent progress (Tasks Remaining
 * hits zero at day 84) and actual completion (rework doesn't clear until day ~175). A naive
 * schedule based on development rate alone would predict completion around day 84; the true
 * schedule is more than double that.
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
                // Simulation duration in days (work finishes ~day 175; 200 gives margin)
                200
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
        model.setMetadata(ModelMetadata.builder()
                .source("Abdel-Hamid & Madnick, Software Project Dynamics (1991)")
                .license("CC-BY-SA-4.0")
                .build());
        model.addModule(workforce.getModule());
        model.addModule(staffAllocation.getModule());
        model.addModule(softwareProduction.getModule());
        return model;
    }
}
