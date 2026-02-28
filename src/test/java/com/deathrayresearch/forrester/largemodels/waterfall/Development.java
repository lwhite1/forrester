package com.deathrayresearch.forrester.largemodels.waterfall;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.item.ItemUnit;
import com.deathrayresearch.forrester.measure.units.dimensionless.DimensionlessUnits;
import com.deathrayresearch.forrester.model.Constant;
import com.deathrayresearch.forrester.model.Formula;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Module;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.flows.FlowPerDay;

import static com.deathrayresearch.forrester.largemodels.waterfall.WaterfallSoftwareDevelopment.DAILY_RESOURCES_FOR_SOFTWARE_PRODUCTION;
import static com.deathrayresearch.forrester.largemodels.waterfall.WaterfallSoftwareDevelopment.DEVELOPMENT;
import static com.deathrayresearch.forrester.largemodels.waterfall.WaterfallSoftwareDevelopment.FRACTION_OF_WORKFORCE_WITH_EXPERIENCE;
import static com.deathrayresearch.forrester.largemodels.waterfall.WaterfallSoftwareDevelopment.POTENTIAL_PRODUCTIVITY;
import static com.deathrayresearch.forrester.largemodels.waterfall.WaterfallSoftwareDevelopment.TASKS_DEVELOPED;

/**
 *
 */
class Development {

    private static final Unit TASKS = new ItemUnit("Task");
    private static final DimensionlessUnits DIMENSIONLESS_UNIT = DimensionlessUnits.DIMENSIONLESS;
    private static final Unit TASKS_PER_PERSON_DAY = new ItemUnit("Tasks per person day");

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

        Variable developmentStaffing = new Variable("Development Staffing", new ItemUnit("Person days per day"),
                new Formula() {
                    @Override
                    public double getCurrentValue() {
                        return model.getVariable(DAILY_RESOURCES_FOR_SOFTWARE_PRODUCTION).getValue();
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
            protected Quantity quantityPerTimeUnit() {
                double staffing = module.getVariable("Development Staffing").getValue();
                double productivity = module.getVariable("Development Productivity").getValue();
                double value = staffing * productivity;
                return new Quantity(value, TASKS);
            }
        };
    }
}
