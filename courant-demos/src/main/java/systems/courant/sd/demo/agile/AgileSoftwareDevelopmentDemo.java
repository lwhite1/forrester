/*
 * Copyright (c) 2026 Courant Systems
 * Licensed under CC-BY-SA-4.0. See LICENSE in this module for details.
 */

package systems.courant.sd.demo.agile;

import systems.courant.sd.Simulation;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.measure.Unit;
import systems.courant.sd.measure.units.item.ItemUnit;
import systems.courant.sd.measure.units.time.TimeUnits;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.ModelMetadata;
import systems.courant.sd.model.Stock;
import systems.courant.sd.ui.StockLevelChartViewer;

import static systems.courant.sd.measure.Units.WEEK;

/**
 * Models an agile software project with a backlog pipeline, staffed completion, and rework.
 *
 * <p>Work flows from the product backlog through the sprint backlog into completed tasks.
 * A team of developers pulls work at a rate bounded by both available backlog and team
 * capacity. A fraction of completed work introduces latent defects, which are discovered
 * and then fixed through separate feedback loops.
 *
 * <h3>Pipeline</h3>
 * <pre>
 *   Product Backlog --[Sprint Pull]--> Sprint Backlog --[Completion]--> Completed Tasks
 *                                                             |
 *                                                             v
 *                                                      Latent Defects --[Discovery]--> Known Defects
 *                                                                                          |
 *                                                                                     [Fix Rate]
 * </pre>
 *
 * <h3>Default behavior (52 weeks)</h3>
 * <p>With 500 tasks and a 5-person team producing 20 tasks/person/week, the backlog drains
 * in roughly 5 weeks. Defects are created at a 20% rate, discovered at 40%/week, and fixed
 * at 50%/week, so the defect stocks rise then decay after development ends.
 */
public class AgileSoftwareDevelopmentDemo {

    private static final Unit WORK = new ItemUnit("task");
    private static final Unit DEFECT = new ItemUnit("defect");

    public static void main(String[] args) {
        Model model = getModel(500, 5, 20, 0.80, 0.10, 0.40, 0.50);
        Simulation run = new Simulation(model, TimeUnits.DAY, WEEK, 52);
        run.addEventHandler(new StockLevelChartViewer());
        run.execute();
    }

    /**
     * Builds the agile project model with default parameters.
     *
     * @return the configured model
     */
    public static Model getModel() {
        return getModel(500, 5, 20, 0.80, 0.10, 0.40, 0.50);
    }

    /**
     * Builds the agile project model with the given parameters.
     *
     * @param projectSize           total tasks in the product backlog
     * @param teamSize              number of developers
     * @param productivityPerPerson tasks completed per person per week
     * @param fractionCorrect       fraction of completed work that is defect-free (0..1)
     * @param sprintPullFraction    fraction of product backlog pulled into sprint each week
     * @param defectDiscoveryRate   fraction of latent defects discovered per week (0..1)
     * @param defectFixRate         fraction of known defects fixed per week (0..1)
     * @return the configured model
     */
    public static Model getModel(double projectSize, double teamSize,
                                  double productivityPerPerson, double fractionCorrect,
                                  double sprintPullFraction, double defectDiscoveryRate,
                                  double defectFixRate) {
        Model model = new Model("Agile software development");
        model.setMetadata(ModelMetadata.builder()
                .license("CC-BY-SA-4.0")
                .build());

        Stock productBacklog = new Stock("Product Backlog", projectSize, WORK);
        Stock sprintBacklog = new Stock("Sprint Backlog", 0, WORK);
        Stock completedWork = new Stock("Completed Tasks", 0, WORK);
        Stock latentDefects = new Stock("Latent Defects", 0, DEFECT);
        Stock knownDefects = new Stock("Known Defects", 0, DEFECT);

        // Pull work from product backlog into sprint backlog each week
        Flow sprintPull = Flow.create("Sprint Pull", WEEK, () -> {
            double available = productBacklog.getQuantity().getValue();
            return new Quantity(available * sprintPullFraction, WORK);
        });
        productBacklog.addOutflow(sprintPull);
        sprintBacklog.addInflow(sprintPull);

        // Team completes work from sprint backlog, bounded by capacity and available work
        Flow completion = Flow.create("Completion", WEEK, () -> {
            double available = sprintBacklog.getQuantity().getValue();
            double capacity = teamSize * productivityPerPerson;
            return new Quantity(Math.min(available, capacity), WORK);
        });
        sprintBacklog.addOutflow(completion);
        completedWork.addInflow(completion);

        // A fraction of completed work introduces latent defects
        Flow defectCreation = Flow.create("Defect Creation", WEEK, () ->
                completion.flowPerTimeUnit(TimeUnits.DAY)
                        .multiply(1.0 - fractionCorrect));
        latentDefects.addInflow(defectCreation);

        // Latent defects are discovered at a fractional rate
        Flow defectDiscovery = Flow.create("Defect Discovery", WEEK, () ->
                latentDefects.getQuantity().multiply(defectDiscoveryRate));
        latentDefects.addOutflow(defectDiscovery);
        knownDefects.addInflow(defectDiscovery);

        // Known defects are fixed at a fractional rate
        Flow defectFix = Flow.create("Defect Fix", WEEK, () ->
                knownDefects.getQuantity().multiply(defectFixRate));
        knownDefects.addOutflow(defectFix);

        model.addStock(productBacklog);
        model.addStock(sprintBacklog);
        model.addStock(completedWork);
        model.addStock(latentDefects);
        model.addStock(knownDefects);
        return model;
    }
}
