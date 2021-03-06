package com.deathrayresearch.forrester.largemodels.waterfall;

import com.deathrayresearch.forrester.largemodels.waterfall.units.PersonDaysPerDay;
import com.deathrayresearch.forrester.largemodels.waterfall.units.Tasks;
import com.deathrayresearch.forrester.largemodels.waterfall.units.TasksPerPersonDay;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.dimensionless.DimensionlessUnit;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Formula;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Module;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.flows.FlowPerDay;

import static com.deathrayresearch.forrester.largemodels.waterfall.WaterfallSoftwareDevelopment.*;

/**
 *
 */
class Development {

    private static final Tasks TASKS = Tasks.getInstance();
    private static final DimensionlessUnit DIMENSIONLESS_UNIT = DimensionlessUnit.getInstance();
    private static final TasksPerPersonDay TASKS_PER_PERSON_DAY = TasksPerPersonDay.getInstance();

    static Module getDevelopmentSubSystem(Model model) {

        Module module = new Module(DEVELOPMENT);

        Stock actualFractionOfPersonDayOnProject =
                new Stock("Actual fraction of person day on project",
                        1.0, DIMENSIONLESS_UNIT
                );

        Stock tasksDeveloped = new Stock(TASKS_DEVELOPED,0.0, TASKS);

        Variable communicationOverhead =
                new Variable("Communication Overhead", DIMENSIONLESS_UNIT, new Formula() {
                    @Override
                    public double getCurrentValue() {
                        return 0;
                    }
                });

        Constant nominalPotentialProductivityOfExperiencedEmployee =
                new Constant("Nominal potential productivity of experienced employee",
                        TASKS_PER_PERSON_DAY,
                        1.0);

        Constant nominalPotentialProductivityOfNewEmployee =
                new Constant("Nominal potential productivity of new employee",
                        TASKS_PER_PERSON_DAY,
                        0.5);

        Variable averageNominalPotentialProductivity =
                new Variable("Average nominal potential productivity",
                        TASKS_PER_PERSON_DAY,
                        new Formula() {
                            @Override
                            public double getCurrentValue() {
                                double fractionExperienced =
                                        model.getVariable(FRACTION_OF_WORKFORCE_WITH_EXPERIENCE).getValue();
                                return
                                        (nominalPotentialProductivityOfExperiencedEmployee.getValue()
                                            * fractionExperienced)
                                        + ((1 - fractionExperienced)
                                                * nominalPotentialProductivityOfNewEmployee.getValue())
                                        ;}});

        Variable potentialProductivity =
                new Variable(POTENTIAL_PRODUCTIVITY,
                        TASKS_PER_PERSON_DAY,
                        new Formula() {
                            @Override
                            public double getCurrentValue() {
                                return averageNominalPotentialProductivity.getValue();
                            }
                        });


        Variable developmentProductivity =
                new Variable("Development Productivity",
                    TASKS_PER_PERSON_DAY,
                        potentialProductivity::getValue);

        Variable developmentStaffing = new Variable("Development Staffing", PersonDaysPerDay.getInstance(),
                new Formula() {
                    @Override
                    public double getCurrentValue() {
                        return module.getVariable(DAILY_RESOURCES_FOR_SOFTWARE_PRODUCTION).getValue();
                    }
                });

        module.addVariable(communicationOverhead);
        module.addVariable(averageNominalPotentialProductivity);
        module.addVariable(potentialProductivity);
        module.addVariable(developmentProductivity);
        module.addVariable(developmentStaffing);

        Flow developmentFlow = getDevelopmentFlow(module);
        module.addFlow(developmentFlow);

        tasksDeveloped.addInflow(developmentFlow);

        module.addStock(tasksDeveloped);
        module.addStock(actualFractionOfPersonDayOnProject);
        return module;
    }

    private static Flow getDevelopmentFlow(Module module) {
        return new FlowPerDay("Tasks completed") {
            @Override
            protected Quantity quantityPerDay() {
                double staffing = module.getVariable("Development Staffing").getValue();
                double productivity = module.getVariable("Development Productivity").getValue();
                double value = staffing * productivity;
                return new Quantity(value, TASKS);
            }
        };
    }
}
