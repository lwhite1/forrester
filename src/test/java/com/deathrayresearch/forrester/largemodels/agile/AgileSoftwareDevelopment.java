package com.deathrayresearch.forrester.largemodels.agile;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Item;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.measure.units.time.Times;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.rate.Rate;
import com.deathrayresearch.forrester.ui.ChartViewer;
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

    double nominalProductivityPerPersonWeek = 200; // tasks
    double relativeProductivityOfNewStaff = .20;

    double nominalFractionCorrectAndComplete = .80;

    @Test
    public void testRun1() {

        Model model = new Model("Software development");

        Stock productBacklog = new Stock("product backlog", projectSize, Work.getInstance());
        Stock releaseBacklog = new Stock("release backlog", releaseSize, Work.getInstance());
        Stock sprintBacklog = new Stock("sprint backlog", sprintSize, Work.getInstance());

        Stock completedWork = new Stock("completed tasks", 0, Work.getInstance());
        Stock latentDefects = new Stock("latent defects", 0, Defect.getInstance());
        Stock knownDefects = new Stock("known defects", 0, Defect.getInstance());

        Rate completionRate = timeUnit -> {
            if (sprintBacklog.getCurrentValue().getValue() <= 0) {
                return new Quantity(0, Work.getInstance());
            }
            return new Quantity(Math.min(sprintBacklog.getCurrentValue().getValue(), nominalProductivityPerPersonWeek), Work.getInstance());
        };
        Flow workCompletion = new Flow("Completed work", completionRate);

        Rate defectCreationRate = timeUnit ->
                workCompletion.getRate().flowPerTimeUnit(Day.getInstance())
                        .multiply(1.0 - nominalFractionCorrectAndComplete);

        Flow createdDefects = new Flow("Created defects", defectCreationRate);

        Rate sprintDefectFindRate = timeUnit -> latentDefects.getCurrentValue().multiply(0.4);

        Flow foundDefects = new Flow("Found defects", sprintDefectFindRate);

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

    private static class Work implements Unit {

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

    private static class Defect implements Unit {

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
