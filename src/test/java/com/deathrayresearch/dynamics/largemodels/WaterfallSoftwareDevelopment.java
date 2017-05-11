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


    SubSystem workforce = getWorkforce();
    SubSystem development = new SubSystem("Development");
    SubSystem testAndRework = new SubSystem("Testing and Rework");


    @Test
    public void testRun1() {

        Model model = new Model("Waterfall");

        model.addStock(workforce.getStock("Newly hired"));
        model.addStock(workforce.getStock("Experienced"));
        model.addVariable(workforce.getVariable("Workforce Gap"));
        model.addVariable(workforce.getVariable("Total Workforce"));

        Quantity<Time> duration = new Quantity<>(3, Year.getInstance());
        Simulation simulation = new Simulation(model, Day.getInstance(), duration);
        simulation.addEventHandler(ChartViewer.newInstance(simulation.getEventBus()));

        simulation.execute();
    }

    private SubSystem getWorkforce() {

        // initial variables and constants
        final int averageEmploymentInDays = 673;
        final int hiringDelayInDays = 40;
        final int assimilationDelayInDays = 80;

        SubSystem workforce = new SubSystem("Workforce");

        Quantity<Item> newHires = new Quantity<>(2.0, People.getInstance());
        Quantity<Item> experienced = new Quantity<>(4.0, People.getInstance());

        Stock<Item> newlyHiredWorkforce = new Stock<>("Newly hired", newHires);
        Stock<Item> experiencedWorkforce = new Stock<>("Experienced", experienced);


        Variable totalWorkforce = new Variable("Total Workforce",
                () -> newlyHiredWorkforce.getCurrentValue().getValue()
                        + experiencedWorkforce.getCurrentValue().getValue());

        Variable workforceLevelSought = new Variable("Desired Workforce", new Formula() {
            @Override
            public double getCurrentValue() {
                return 30.0;
            }
        });

        Variable workforceGap = new Variable("Workforce Gap",
                () -> workforceLevelSought.getCurrentValue() - totalWorkforce.getCurrentValue());


        Variable fractionExperiencedWorkforce = new Variable("Fraction of Workforce with Experience",
                () -> experiencedWorkforce.getCurrentValue().getValue() / totalWorkforce.getCurrentValue());


        Rate<Item> hiringRate = timeUnit -> new Quantity<>(workforceGap.getCurrentValue() / hiringDelayInDays, People.getInstance());

        Flow<Item> newHireFlow = new Flow<>("New hires", hiringRate);

        Rate<Item> assimilationRate = timeUnit ->
                new Quantity<>(newHires.getValue() / assimilationDelayInDays, People.getInstance());
        Flow<Item> assimilationFlow = new Flow<>("Assimilated hires", assimilationRate);

        Rate<Item> quitRate = timeUnit ->
                new Quantity<>(experiencedWorkforce.getCurrentValue().getValue()
                        / averageEmploymentInDays, People.getInstance());
        Flow<Item> resignationFlow = new Flow<>("Employees quiting", quitRate);

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

        return workforce;
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
