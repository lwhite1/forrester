package com.deathrayresearch.forrester.largemodels.agile;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Dimension;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.dimension.Item;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.rate.Rate;
import com.deathrayresearch.forrester.rate.RatePerWeek;
import com.deathrayresearch.forrester.ui.ChartViewer;
import org.junit.Test;

import static com.deathrayresearch.forrester.measure.Units.WEEK;

/**
 *
 */
public class AgileSoftwareDevelopment {

    // model parameters
    double projectSize = 100_000;  // tasks
    double releaseSize = 25_000;
    double sprintSize = 2_500;
    double inexperiencedStaff = 10;
    double experiencedStaff = 10;

    double nominalProductivityPerPersonWeek = 200; // tasks
    double relativeProductivityOfNewStaff = .20;

    double nominalFractionCorrectAndComplete = .80;

    @Test
    public void testRun1() {

        Simulation run = new Simulation(getModel(), Day.getInstance(), WEEK,52);
        run.addEventHandler(new ChartViewer());
        run.execute();
    }

    public Model getModel() {
        Model model = new Model("Agile software development");

        Stock productBacklog = new Stock("product backlog", projectSize, Work.getInstance());
        Stock releaseBacklog = new Stock("release backlog", releaseSize, Work.getInstance());
        Stock sprintBacklog = new Stock("sprint backlog", sprintSize, Work.getInstance());

        Stock completedWork = new Stock("completed tasks", 0, Work.getInstance());
        Stock latentDefects = new Stock("latent defects", 0, Defect.getInstance());
        Stock knownDefects = new Stock("known defects", 0, Defect.getInstance());

        Rate completionRate = new RatePerWeek() {
            @Override
            protected Quantity quantityPerWeek() {
                if (sprintBacklog.getCurrentValue().getValue() <= 0) {
                    return new Quantity("Completed Work",0, Work.getInstance());
                }
                return new Quantity("Completed Work", Math.min(sprintBacklog.getCurrentValue().getValue(), nominalProductivityPerPersonWeek), Work.getInstance());
            }
        };

        Flow workCompletion = new Flow(completionRate);

        Rate defectCreationRate = new RatePerWeek() {
            @Override
            protected Quantity quantityPerWeek() {
                return workCompletion.getRate().flowPerTimeUnit(Day.getInstance())
                    .multiply("Created defects", 1.0 - nominalFractionCorrectAndComplete);
            }
        };

        Flow createdDefects = new Flow(defectCreationRate);

        Rate sprintDefectFindRate = new RatePerWeek() {
            @Override
            protected Quantity quantityPerWeek() {
                return latentDefects.getCurrentValue().multiply("Found defects", 0.4);
            }
        };

        Flow foundDefects = new Flow(sprintDefectFindRate);

        completedWork.addInflow(workCompletion);
        sprintBacklog.addOutflow(workCompletion);

        latentDefects.addInflow(createdDefects);
        latentDefects.addOutflow(foundDefects);

        knownDefects.addInflow(foundDefects);

        model.addStock(productBacklog);
        model.addStock(releaseBacklog);
        model.addStock(sprintBacklog);
        model.addStock(completedWork);
        model.addStock(latentDefects);
        model.addStock(knownDefects);
        return model;
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
