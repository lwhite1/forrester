/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.forrester.demo.agile;

import systems.courant.forrester.Simulation;
import systems.courant.forrester.measure.Quantity;
import systems.courant.forrester.measure.Unit;
import systems.courant.forrester.measure.units.item.ItemUnit;
import systems.courant.forrester.measure.units.time.TimeUnits;
import systems.courant.forrester.model.Flow;
import systems.courant.forrester.model.Model;
import systems.courant.forrester.model.ModelMetadata;
import systems.courant.forrester.model.Stock;
import systems.courant.forrester.ui.StockLevelChartViewer;

import static systems.courant.forrester.measure.Units.WEEK;

/**
 * Models an agile software project with backlogs, defect creation, and defect resolution.
 *
 * <p>Stocks include a product backlog, release backlog, sprint backlog, completed work, latent
 * defects, and known defects. Work flows from sprint backlog to completion at a bounded
 * productivity rate, generating defects at a fraction of the completion rate. Defects are
 * discovered and fixed through separate feedback loops, illustrating rework dynamics in
 * iterative development.
 */
public class AgileSoftwareDevelopmentDemo {

    private static final Unit WORK = new ItemUnit("task");
    private static final Unit DEFECT = new ItemUnit("defect");

    // model parameters
    double projectSize = 100_000;  // tasks
    double releaseSize = 25_000;
    double sprintSize = 2_500;
    double nominalProductivityPerPersonWeek = 200; // tasks

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
        model.setMetadata(ModelMetadata.builder()
                .license("CC-BY-SA-4.0")
                .build());

        Stock productBacklog = new Stock("product backlog", projectSize, WORK);
        Stock releaseBacklog = new Stock("release backlog", releaseSize, WORK);
        Stock sprintBacklog = new Stock("sprint backlog", sprintSize, WORK);

        Stock completedWork = new Stock("completed tasks", 0, WORK);
        Stock latentDefects = new Stock("latent defects", 0, DEFECT);
        Stock knownDefects = new Stock("known defects", 0, DEFECT);

        Flow completionRate = Flow.create("Completed Work", WEEK, () -> {
            if (sprintBacklog.getQuantity().getValue() <= 0) {
                return new Quantity(0, WORK);
            }
            return new Quantity(Math.min(sprintBacklog.getQuantity().getValue(), nominalProductivityPerPersonWeek), WORK);
        });

        Flow defectCreationRate = Flow.create("Created defects", WEEK, () ->
                completionRate.flowPerTimeUnit(TimeUnits.DAY)
                    .multiply(1.0 - nominalFractionCorrectAndComplete));

        Flow sprintDefectFindRate = Flow.create("Found defects", WEEK, () ->
                latentDefects.getQuantity().multiply(0.4));

        Flow defectFixRate = Flow.create("Fixed defects", WEEK, () ->
                knownDefects.getQuantity().multiply(0.5));

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
