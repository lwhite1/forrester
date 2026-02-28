package com.deathrayresearch.forrester.demo.waterfall;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.dimensionless.DimensionlessUnits;
import com.deathrayresearch.forrester.measure.units.item.ItemUnit;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Module;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;

import static com.deathrayresearch.forrester.measure.Units.DAY;

/**
 * Development subsystem for the waterfall software project model.
 *
 * <p>Models task completion as a function of development staffing (from
 * {@link StaffAllocation}) and productivity. Productivity depends on the workforce
 * experience mix — experienced workers are twice as productive as new hires. The daily
 * task completion rate is staffing multiplied by development productivity.
 */
public class Development {

    private static final Unit TASKS = new ItemUnit("Task");
    private static final DimensionlessUnits DIMENSIONLESS_UNIT = DimensionlessUnits.DIMENSIONLESS;
    private static final Unit TASKS_PER_PERSON_DAY = new ItemUnit("Tasks per person day");

    private final Module module;

    public Development(Variable dailyResourcesForProduction, Variable fractionExperienced) {
        module = new Module("Development");

        Stock actualFractionOfPersonDayOnProject =
                new Stock("Actual fraction of person day on project", 1.0, DIMENSIONLESS_UNIT);

        Stock tasksDeveloped = new Stock("Tasks Developed", 0.0, TASKS);

        Variable communicationOverhead =
                new Variable("Communication Overhead", DIMENSIONLESS_UNIT, () -> 0);

        Constant nominalPotentialProductivityOfExperiencedEmployee =
                new Constant("Nominal potential productivity of experienced employee",
                        TASKS_PER_PERSON_DAY, 1.0);

        Constant nominalPotentialProductivityOfNewEmployee =
                new Constant("Nominal potential productivity of new employee",
                        TASKS_PER_PERSON_DAY, 0.5);

        Variable averageNominalPotentialProductivity =
                new Variable("Average nominal potential productivity",
                        TASKS_PER_PERSON_DAY,
                        () -> {
                            double fe = fractionExperienced.getValue();
                            return (nominalPotentialProductivityOfExperiencedEmployee.getValue() * fe)
                                    + ((1 - fe) * nominalPotentialProductivityOfNewEmployee.getValue());
                        });

        Variable potentialProductivity =
                new Variable("Potential productivity", TASKS_PER_PERSON_DAY,
                        averageNominalPotentialProductivity::getValue);

        Variable developmentProductivity =
                new Variable("Development Productivity", TASKS_PER_PERSON_DAY,
                        potentialProductivity::getValue);

        Variable developmentStaffing = new Variable("Development Staffing",
                new ItemUnit("Person days per day"),
                dailyResourcesForProduction::getValue);

        module.addVariable(communicationOverhead);
        module.addVariable(averageNominalPotentialProductivity);
        module.addVariable(potentialProductivity);
        module.addVariable(developmentProductivity);
        module.addVariable(developmentStaffing);

        Flow developmentFlow = Flow.create("Tasks completed", DAY, () -> {
            double staffing = developmentStaffing.getValue();
            double productivity = developmentProductivity.getValue();
            return new Quantity(staffing * productivity, TASKS);
        });
        module.addFlow(developmentFlow);

        tasksDeveloped.addInflow(developmentFlow);

        module.addStock(tasksDeveloped);
        module.addStock(actualFractionOfPersonDayOnProject);
    }

    public Module getModule() {
        return module;
    }
}
