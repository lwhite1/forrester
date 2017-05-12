package com.deathrayresearch.dynamics.largemodels;

import com.deathrayresearch.dynamics.Simulation;
import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Quantity;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Item;
import com.deathrayresearch.dynamics.measure.dimension.Time;
import com.deathrayresearch.dynamics.measure.units.time.Day;
import com.deathrayresearch.dynamics.measure.units.time.Year;
import com.deathrayresearch.dynamics.model.*;
import com.deathrayresearch.dynamics.rate.Rate;
import com.deathrayresearch.dynamics.ui.ChartViewer;
import org.junit.Test;

/**
 *
 */
public class WaterfallSoftwareDevelopment {

    private static final People PEOPLE = People.getInstance();

    private static final String FRACTION_OF_WORKFORCE_WITH_EXPERIENCE = "Fraction of Workforce with Experience";
    private static final String DEVELOPMENT = "Development";
    private static final String TESTING_AND_REWORK = "Testing and Rework";
    private static final String NEW_HIRES = "New hires";
    private static final String TOTAL_WORKFORCE = "Total Workforce";
    private static final String DESIRED_WORKFORCE = "Desired Workforce";
    private static final String WORKFORCE = "Workforce";
    private static final String EXPERIENCED = "Experienced";
    private static final String NEWLY_HIRED = "Newly hired";
    private static final String WORKFORCE_GAP = "Workforce Gap";
    public static final String NEW_HIRE_CAP = "New Hire Cap";
    public static final String WORKFORCE_NEED = "Workforce need";
    public static final String TOTAL_WORKFORCE_CAP = "Total Workforce Cap";

    private SubSystem workforce = getWorkforce();
    private SubSystem development = getDevelopment();
    private SubSystem testAndRework = getTestAndRework();

    private Constant averageDailyManPowerPerStaff = new Constant("ADMPPPS", 1);

    @Test
    public void testRun1() {

        Model model = new Model("Waterfall");

        model.addStock(workforce.getStock(NEWLY_HIRED));
        model.addStock(workforce.getStock(EXPERIENCED));
        model.addVariable(workforce.getVariable(WORKFORCE_GAP));
        model.addVariable(workforce.getVariable(TOTAL_WORKFORCE));
        model.addVariable(workforce.getVariable(FRACTION_OF_WORKFORCE_WITH_EXPERIENCE));
        model.addVariable(workforce.getVariable(TOTAL_WORKFORCE_CAP));
        model.addVariable(workforce.getVariable(WORKFORCE_NEED));
        model.addVariable(workforce.getVariable(NEW_HIRE_CAP));

        model.addSubSystem(workforce);
    //    model.addSubSystem(development);
    //    model.addSubSystem(testAndRework);

        Quantity<Time> duration = new Quantity<>(3, Year.getInstance());
        Simulation simulation = new Simulation(model, Day.getInstance(), duration);
        simulation.addEventHandler(ChartViewer.newInstance(simulation.getEventBus()));

        simulation.execute();
    }

    private SubSystem getTestAndRework() {
        SubSystem subSystem = new SubSystem(TESTING_AND_REWORK);

        return subSystem;
    }

    private SubSystem getDevelopment() {
        SubSystem subSystem = new SubSystem(DEVELOPMENT);

        return subSystem;
    }

    private SubSystem getWorkforce() {

        SubSystem workforce = new SubSystem(WORKFORCE);

        Stock<Item> newlyHiredWorkforce = new Stock<>(NEWLY_HIRED, 2.0, PEOPLE);
        Stock<Item> experiencedWorkforce = new Stock<>(EXPERIENCED, 4.0, PEOPLE);

        Variable fullTimeEquivalentExperiencedWorkforce =
                new Variable("Full time experienced workforce",
                        new Formula() {
                            @Override
                            public double getCurrentValue() {
                                return averageDailyManPowerPerStaff.getCurrentValue()
                                        * experiencedWorkforce.getCurrentValue().getValue();
                            }
                        }
                );

        Variable totalWorkforce = new Variable(TOTAL_WORKFORCE,
                () -> newlyHiredWorkforce.getCurrentValue().getValue()
                        + experiencedWorkforce.getCurrentValue().getValue());

        Variable workforceNeed = new Variable(WORKFORCE_NEED, () -> 30.0);


        Constant maxNewHiresPerExperiencedStaff = new Constant("Max New Hires per Experienced Staff", 3.0);

        Variable newHireCap = new Variable(NEW_HIRE_CAP, new Formula() {
            @Override
            public double getCurrentValue() {
                return maxNewHiresPerExperiencedStaff.getCurrentValue()
                        * fullTimeEquivalentExperiencedWorkforce.getCurrentValue();
            }
        });

        Variable totalWorkforceCap = new Variable(TOTAL_WORKFORCE_CAP, new Formula() {
            @Override
            public double getCurrentValue() {
                return newHireCap.getCurrentValue() + experiencedWorkforce.getCurrentValue().getValue();
            }
        });

        Variable workforceLevelSought = new Variable(DESIRED_WORKFORCE, () ->
                Math.min(workforceNeed.getCurrentValue(), totalWorkforceCap.getCurrentValue()));

        Variable workforceGap = new Variable(WORKFORCE_GAP,
                () -> workforceLevelSought.getCurrentValue() - totalWorkforce.getCurrentValue());

        Variable fractionExperiencedWorkforce = new Variable(FRACTION_OF_WORKFORCE_WITH_EXPERIENCE,
                () -> experiencedWorkforce.getCurrentValue().getValue() / totalWorkforce.getCurrentValue());

        Flow<Item> newHireFlow = getItemFlow(workforceGap);

        Flow<Item> assimilationFlow = getAssimilationFlow(newlyHiredWorkforce);

        Flow<Item> resignationFlow = getItemFlow(experiencedWorkforce);

        workforce.addStock(newlyHiredWorkforce);
        workforce.addStock(experiencedWorkforce);

        newlyHiredWorkforce.addInflow(newHireFlow);
        newlyHiredWorkforce.addOutflow(assimilationFlow);
        experiencedWorkforce.addInflow(assimilationFlow);
        experiencedWorkforce.addOutflow(resignationFlow);

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

        return workforce;
    }

    private Flow<Item> getItemFlow(Variable workforceGap) {
        final int hiringDelayInDays = 40;
        Rate<Item> hiringRate = timeUnit ->
                new Quantity<>(
                        Math.max(0.0, workforceGap.getCurrentValue() / hiringDelayInDays), PEOPLE);

        return new Flow<>(NEW_HIRES, hiringRate);
    }

    private Flow<Item> getItemFlow(Stock<Item> experiencedWorkforce) {
        int averageEmploymentInDays = 673;
        Rate<Item> quitRate = timeUnit ->
                new Quantity<>(experiencedWorkforce.getCurrentValue().getValue()
                        / averageEmploymentInDays, PEOPLE);
        return new Flow<>("Employees quiting", quitRate);
    }

    private Flow<Item> getAssimilationFlow(Stock<Item> newHires) {
        final int assimilationDelayInDays = 80;
        Rate<Item> assimilationRate = timeUnit ->
                new Quantity<>(newHires.getCurrentValue().getValue() / assimilationDelayInDays, PEOPLE);
        return new Flow<>("Assimilated hires", assimilationRate);
    }

    private static class People implements Unit<Item> {

        private static final People instance = new People();

        @Override
        public String getName() {
            return "Person";
        }

        @Override
        public Dimension getDimension() {
            return Item.getInstance();
        }

        @Override
        public double ratioToBaseUnit() {
            return 1.0;
        }

        static People getInstance() {
            return instance;
        }
    }
}
