package com.deathrayresearch.forrester.demo.agile;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.item.ItemUnit;
import com.deathrayresearch.forrester.measure.units.time.TimeUnits;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.flows.FlowPerWeek;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

import static com.deathrayresearch.forrester.measure.Units.WEEK;

/**
 *
 */
public class AgileSoftwareDevelopmentDemo {

    private static final Unit WORK = new ItemUnit("task");
    private static final Unit DEFECT = new ItemUnit("defect");

    // model parameters
    double projectSize = 100_000;  // tasks
    double releaseSize = 25_000;
    double sprintSize = 2_500;
    double inexperiencedStaff = 10;
    double experiencedStaff = 10;

    double nominalProductivityPerPersonWeek = 200; // tasks
    double relativeProductivityOfNewStaff = .20;

    double nominalFractionCorrectAndComplete = .80;

    public static void main(String[] args) {
        new AgileSoftwareDevelopmentDemo().run();
    }

    public void run() {

        Simulation run = new Simulation(getModel(), TimeUnits.DAY, WEEK,52);
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }

    public Model getModel() {
        Model model = new Model("Agile software development");

        Stock productBacklog = new Stock("product backlog", projectSize, WORK);
        Stock releaseBacklog = new Stock("release backlog", releaseSize, WORK);
        Stock sprintBacklog = new Stock("sprint backlog", sprintSize, WORK);

        Stock completedWork = new Stock("completed tasks", 0, WORK);
        Stock latentDefects = new Stock("latent defects", 0, DEFECT);
        Stock knownDefects = new Stock("known defects", 0, DEFECT);

        Flow completionRate = new FlowPerWeek("Completed Work") {
            @Override
            protected Quantity quantityPerTimeUnit() {
                if (sprintBacklog.getQuantity().getValue() <= 0) {
                    return new Quantity(0, WORK);
                }
                return new Quantity(Math.min(sprintBacklog.getQuantity().getValue(), nominalProductivityPerPersonWeek), WORK);
            }
        };

        Flow defectCreationRate = new FlowPerWeek("Created defects") {
            @Override
            protected Quantity quantityPerTimeUnit() {
                return completionRate.flowPerTimeUnit(TimeUnits.DAY)
                    .multiply(1.0 - nominalFractionCorrectAndComplete);
            }
        };

        Flow sprintDefectFindRate = new FlowPerWeek("Found defects") {
            @Override
            protected Quantity quantityPerTimeUnit() {
                return latentDefects.getQuantity().multiply(0.4);
            }
        };

        Flow defectFixRate = new FlowPerWeek("Fixed defects") {
            @Override
            protected Quantity quantityPerTimeUnit() {
                return knownDefects.getQuantity().multiply(0.5);
            }
        };

        completedWork.addInflow(completionRate);
        sprintBacklog.addOutflow(completionRate);

        latentDefects.addInflow(defectCreationRate);
        latentDefects.addOutflow(sprintDefectFindRate);

        knownDefects.addInflow(sprintDefectFindRate);
        knownDefects.addOutflow(defectFixRate);

        model.addStock(productBacklog);
        model.addStock(releaseBacklog);
        model.addStock(sprintBacklog);
        model.addStock(completedWork);
        model.addStock(latentDefects);
        model.addStock(knownDefects);
        return model;
    }
}
