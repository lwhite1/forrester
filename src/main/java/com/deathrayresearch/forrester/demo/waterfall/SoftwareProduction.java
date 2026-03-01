package com.deathrayresearch.forrester.demo.waterfall;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.dimensionless.DimensionlessUnits;
import com.deathrayresearch.forrester.measure.units.item.ItemUnit;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Module;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;

import static com.deathrayresearch.forrester.measure.Units.DAY;

/**
 * Software production subsystem implementing the Abdel-Hamid &amp; Madnick (1991) rework cycle.
 *
 * <p>Four stocks model the flow of work through the development process:
 * <ol>
 *   <li><b>Tasks Remaining</b> — work not yet attempted, drains via development</li>
 *   <li><b>Tasks Completed</b> — correctly finished work</li>
 *   <li><b>Undiscovered Rework</b> — tasks with hidden errors, injected when FCC &lt; 1</li>
 *   <li><b>Rework to Do</b> — discovered errors awaiting fixing</li>
 * </ol>
 *
 * <p>Key dynamics:
 * <ul>
 *   <li><b>Fraction Correct and Complete (FCC)</b> — proportion of work done right the first time;
 *       degrades with inexperienced staff</li>
 *   <li><b>Rework Discovery Fraction</b> — interpolates from low (development phase) to high
 *       (integration/testing phase) as completion fraction increases</li>
 *   <li><b>Integration Effort Multiplier</b> — makes rework progressively more expensive as
 *       the project nears completion, modeling the waterfall integration tax</li>
 * </ul>
 *
 * <h3>Expected behavior with default parameters</h3>
 *
 * <p><b>Development phase (days 0–84):</b> Tasks Remaining drains from 500 to zero. FCC hovers
 * around 0.69–0.80, meaning roughly 20–30% of completed work carries hidden errors. Undiscovered
 * Rework peaks at ~13 tasks around day 30, then declines as the Rework Discovery Fraction rises
 * from 0.05 toward 0.33. Rework to Do accumulates steadily to ~86 tasks by the time development
 * ends. The Integration Effort Multiplier reaches ~1.9x.
 *
 * <p><b>Rework phase (days 84–175):</b> With no tasks remaining, all development resources are
 * idle; only QA staff work through the rework backlog. The Integration Effort Multiplier (2.0–
 * 2.4x) makes each fix expensive, and rework itself injects new errors (FCC &lt; 1), creating a
 * long tail. Rework to Do drains to zero around day 175. Final Tasks Completed stabilizes at
 * ~479 out of 500 — the ~21-task deficit reflects tasks permanently lost to the compounding
 * error-on-error cycle.
 */
public class SoftwareProduction {

    private static final Unit TASKS = new ItemUnit("Task");
    private static final Unit TASKS_PER_PERSON_DAY = new ItemUnit("Tasks per person day");
    private static final DimensionlessUnits DIMENSIONLESS = DimensionlessUnits.DIMENSIONLESS;

    private final Module module;

    public SoftwareProduction(Variable dailyResourcesForProduction,
                              Variable dailyResourcesForQA,
                              Variable fractionExperienced,
                              double projectSize,
                              double baseFCC,
                              double nominalProductivityExp,
                              double nominalProductivityNew,
                              double baseReworkDiscoveryFraction,
                              double testingReworkDiscoveryFraction,
                              double integrationCoefficient) {
        module = new Module("Software Production");

        // --- Stocks ---
        Stock tasksRemaining = new Stock("Tasks Remaining", projectSize, TASKS);
        Stock tasksCompleted = new Stock("Tasks Completed", 0.0, TASKS);
        Stock undiscoveredRework = new Stock("Undiscovered Rework", 0.0, TASKS);
        Stock reworkToDo = new Stock("Rework to Do", 0.0, TASKS);

        // --- Variables ---

        // Completion fraction: how far along the project is (used to drive discovery and integration)
        Variable completionFraction = new Variable("Completion Fraction", DIMENSIONLESS, () -> {
            double completed = tasksCompleted.getValue();
            double undiscovered = undiscoveredRework.getValue();
            double total = completed + undiscovered;
            if (projectSize <= 0) {
                return 0.0;
            }
            return Math.min(total / projectSize, 1.0);
        });

        // FCC: affected by experience mix
        // Lookup: fractionExperienced 0.0 → multiplier 0.6, 1.0 → multiplier 1.2 (linear)
        Variable fractionCorrectAndComplete = new Variable("Fraction Correct and Complete",
                DIMENSIONLESS, () -> {
                    double fe = fractionExperienced.getValue();
                    double experienceMultiplier = 0.6 + 0.6 * fe; // 0.6 at fe=0, 1.2 at fe=1
                    return Math.min(baseFCC * experienceMultiplier, 1.0);
                });

        // Rework discovery fraction: interpolates from base to testing as completion rises
        Variable reworkDiscoveryFraction = new Variable("Rework Discovery Fraction",
                DIMENSIONLESS, () -> {
                    double cf = completionFraction.getValue();
                    return baseReworkDiscoveryFraction
                            + (testingReworkDiscoveryFraction - baseReworkDiscoveryFraction) * cf;
                });

        // Integration effort multiplier: makes rework more expensive late in the project
        Variable integrationEffortMultiplier = new Variable("Integration Effort Multiplier",
                DIMENSIONLESS, () -> {
                    double cf = completionFraction.getValue();
                    return 1.0 + integrationCoefficient * cf * cf;
                });

        // Development productivity: weighted average of experienced/new productivity
        Variable developmentProductivity = new Variable("Development Productivity",
                TASKS_PER_PERSON_DAY, () -> {
                    double fe = fractionExperienced.getValue();
                    return (nominalProductivityExp * fe)
                            + (nominalProductivityNew * (1 - fe));
                });

        // --- Flows ---

        // Development rate: tasks attempted per day
        // Splits by FCC into Tasks Completed and Undiscovered Rework
        Variable developmentRate = new Variable("Development Rate", TASKS, () -> {
            double staffing = dailyResourcesForProduction.getValue();
            double productivity = developmentProductivity.getValue();
            double potentialRate = staffing * productivity;
            return Math.min(potentialRate, tasksRemaining.getValue());
        });

        // Flow: Tasks Remaining → (split)
        Flow developmentOutflow = Flow.create("Development", DAY, () ->
                new Quantity(developmentRate.getValue(), TASKS));

        // Flow: FCC portion → Tasks Completed (from development)
        Flow correctDevelopment = Flow.create("Correct Development", DAY, () -> {
            double rate = developmentRate.getValue();
            double fcc = fractionCorrectAndComplete.getValue();
            return new Quantity(rate * fcc, TASKS);
        });

        // Flow: (1-FCC) portion → Undiscovered Rework (from development)
        Flow errorInjection = Flow.create("Error Injection", DAY, () -> {
            double rate = developmentRate.getValue();
            double fcc = fractionCorrectAndComplete.getValue();
            return new Quantity(rate * (1 - fcc), TASKS);
        });

        // Flow: Undiscovered Rework → Rework to Do (discovery)
        Flow reworkDiscovery = Flow.create("Rework Discovery", DAY, () -> {
            double undiscovered = undiscoveredRework.getValue();
            double discoveryFrac = reworkDiscoveryFraction.getValue();
            return new Quantity(undiscovered * discoveryFrac, TASKS);
        });

        // Rework rate: limited by QA staffing and integration multiplier
        Variable reworkRate = new Variable("Rework Rate", TASKS, () -> {
            double qaStaffing = dailyResourcesForQA.getValue();
            double productivity = developmentProductivity.getValue();
            double multiplier = integrationEffortMultiplier.getValue();
            // Each rework task costs (multiplier) times as much as a new task
            double effectiveProductivity = productivity / multiplier;
            double potentialRate = qaStaffing * effectiveProductivity;
            return Math.min(potentialRate, reworkToDo.getValue());
        });

        // Flow: Rework to Do → (split)
        Flow reworkOutflow = Flow.create("Rework", DAY, () ->
                new Quantity(reworkRate.getValue(), TASKS));

        // Flow: FCC portion of rework → Tasks Completed
        Flow correctRework = Flow.create("Correct Rework", DAY, () -> {
            double rate = reworkRate.getValue();
            double fcc = fractionCorrectAndComplete.getValue();
            return new Quantity(rate * fcc, TASKS);
        });

        // Flow: (1-FCC) portion of rework → back to Undiscovered Rework
        Flow reworkErrors = Flow.create("Rework Errors", DAY, () -> {
            double rate = reworkRate.getValue();
            double fcc = fractionCorrectAndComplete.getValue();
            return new Quantity(rate * (1 - fcc), TASKS);
        });

        // --- Wire stocks and flows ---

        // Tasks Remaining drains via development
        tasksRemaining.addOutflow(developmentOutflow);

        // Tasks Completed receives correctly done work from both development and rework
        tasksCompleted.addInflow(correctDevelopment);
        tasksCompleted.addInflow(correctRework);

        // Undiscovered Rework receives errors from development and rework, drains via discovery
        undiscoveredRework.addInflow(errorInjection);
        undiscoveredRework.addInflow(reworkErrors);
        undiscoveredRework.addOutflow(reworkDiscovery);

        // Rework to Do receives discovered errors, drains via rework
        reworkToDo.addInflow(reworkDiscovery);
        reworkToDo.addOutflow(reworkOutflow);

        // --- Register with module ---
        module.addStock(tasksRemaining);
        module.addStock(tasksCompleted);
        module.addStock(undiscoveredRework);
        module.addStock(reworkToDo);

        module.addFlow(developmentOutflow);
        module.addFlow(correctDevelopment);
        module.addFlow(errorInjection);
        module.addFlow(reworkDiscovery);
        module.addFlow(reworkOutflow);
        module.addFlow(correctRework);
        module.addFlow(reworkErrors);

        module.addVariable(completionFraction);
        module.addVariable(fractionCorrectAndComplete);
        module.addVariable(reworkDiscoveryFraction);
        module.addVariable(integrationEffortMultiplier);
        module.addVariable(developmentProductivity);
        module.addVariable(developmentRate);
        module.addVariable(reworkRate);
    }

    public SoftwareProduction(Variable dailyResourcesForProduction,
                              Variable dailyResourcesForQA,
                              Variable fractionExperienced) {
        this(dailyResourcesForProduction, dailyResourcesForQA, fractionExperienced,
                500, 0.80, 1.0, 0.5, 0.05, 0.40, 1.5);
    }

    public Module getModule() {
        return module;
    }
}
