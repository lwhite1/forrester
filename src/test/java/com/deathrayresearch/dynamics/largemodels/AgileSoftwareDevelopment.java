package com.deathrayresearch.dynamics.largemodels;

import com.deathrayresearch.dynamics.Simulation;
import com.deathrayresearch.dynamics.measure.Dimension;
import com.deathrayresearch.dynamics.measure.Quantity;
import com.deathrayresearch.dynamics.measure.Unit;
import com.deathrayresearch.dynamics.measure.dimension.Item;
import com.deathrayresearch.dynamics.measure.units.time.Day;
import com.deathrayresearch.dynamics.measure.units.time.Times;
import com.deathrayresearch.dynamics.model.Flow;
import com.deathrayresearch.dynamics.model.Model;
import com.deathrayresearch.dynamics.model.Stock;
import com.deathrayresearch.dynamics.rate.Rate;
import com.deathrayresearch.dynamics.ui.ChartViewer;
import org.junit.Test;

/**
 *
 */
public class AgileSoftwareDevelopment {

    // model parameters
    double projectSize = 200_000;  // tasks
    double releaseSize = 40_000;
    double sprintSize = 10_000;
    double inexperiencedStaff = 10;
    double experiencedStaff = 10;

    double nominalProductivityPerPersonWeek = 200;
    double relativeProductivityOfNewStaff = .20;

    double nominalFractionCorrectAndComplete = .80;

    @Test
    public void testRun1() {

        Model model = new Model("Software development");

        Stock<Item> productBacklog = new Stock<>("product backlog", projectSize, Work.getInstance());
        Stock<Item> releaseBacklog = new Stock<>("release backlog", releaseSize, Work.getInstance());
        Stock<Item> sprintBacklog = new Stock<>("sprint backlog", sprintSize, Work.getInstance());

        Stock<Item> completedWork = new Stock<>("completed tasks", 0, Work.getInstance());
        Stock<Item> latentDefects = new Stock<>("latent defects", 0, Defect.getInstance());
        Stock<Item> knownDefects = new Stock<>("known defects", 0, Defect.getInstance());

        Rate<Item> completionRate = timeUnit -> {
            if (sprintBacklog.getCurrentValue().getValue() <= 0) {
                return new Quantity<>(0, Work.getInstance());
            }
            return new Quantity<>(Math.min(sprintBacklog.getCurrentValue().getValue(), nominalProductivityPerPersonWeek), Work.getInstance());
        };
        Flow<Item> workCompletion = new Flow<>("Completed work", completionRate);

        Rate<Item> defectCreationRate = timeUnit ->
                workCompletion.getRate().flowPerTimeUnit(Day.getInstance())
                        .multiply(1.0 - nominalFractionCorrectAndComplete);

        Flow<Item> createdDefects = new Flow<>("Created defects", defectCreationRate);

        Rate<Item> sprintDefectFindRate = timeUnit -> latentDefects.getCurrentValue().multiply(0.4);

        Flow<Item> foundDefects = new Flow<>("Found defects", sprintDefectFindRate);

        completedWork.addInflow(workCompletion);
        sprintBacklog.addOutflow(workCompletion);

        latentDefects.addInflow(createdDefects);
        knownDefects.addInflow(foundDefects);
        latentDefects.addOutflow(foundDefects);

        model.addStock(productBacklog);
        model.addStock(releaseBacklog);
        model.addStock(sprintBacklog);
        model.addStock(completedWork);
        model.addStock(latentDefects);
        model.addStock(knownDefects);

        Simulation run = new Simulation(model, Day.getInstance(), Times.weeks(52));
        run.addEventHandler(ChartViewer.newInstance(run.getEventBus()));
        run.execute();
    }

    private static class Work implements Unit<Item> {

        private static final Work instance = new Work();

        @Override
        public String getName() {
            return "task";
        }

        @Override
        public Dimension getDimension() {
            return Item.getInstance();
        }

        @Override
        public double ratioToBaseUnit() {
            return 1.0;
        }

        static Work getInstance() {
            return instance;
        }
    }

    private static class Defect implements Unit<Item> {

        private static final Defect instance = new Defect();

        @Override
        public String getName() {
            return "defect";
        }

        @Override
        public Dimension getDimension() {
            return Item.getInstance();
        }

        @Override
        public double ratioToBaseUnit() {
            return 1.0;
        }

        static Defect getInstance() {
            return instance;
        }
    }
}
