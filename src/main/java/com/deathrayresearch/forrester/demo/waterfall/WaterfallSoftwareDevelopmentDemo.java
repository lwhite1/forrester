package com.deathrayresearch.forrester.demo.waterfall;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.TimeUnits;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.ui.StockLevelChartViewer;

/**
 * Models a waterfall software project with modular subsystems for workforce, development,
 * testing, rework, and staff allocation.
 *
 * <p>Four {@link com.deathrayresearch.forrester.model.Module Modules} — Workforce, Development,
 * Staff Allocation, and Test &amp; Rework — are composed into a single model. Hiring delays,
 * training overhead, defect injection, and rework cycles interact to show how phased development
 * with late testing can lead to schedule overruns and staffing oscillations.
 */
public class WaterfallSoftwareDevelopmentDemo {

    public static void main(String[] args) {
        new WaterfallSoftwareDevelopmentDemo().run();
    }

    public void run() {
        Model model = getModel();

        Quantity duration = new Quantity(1, TimeUnits.YEAR);
        Simulation simulation = new Simulation(model, TimeUnits.DAY, duration);
        simulation.addEventHandler(new StockLevelChartViewer());

        simulation.execute();
    }

    public Model getModel() {
        Workforce workforce = new Workforce();
        StaffAllocation staffAllocation = new StaffAllocation(
                workforce.getTotalWorkforce(),
                workforce.getDailyTrainingOverhead());
        Development development = new Development(
                staffAllocation.getDailyResourcesForProduction(),
                workforce.getFractionExperienced());
        TestAndRework testAndRework = new TestAndRework();

        Model model = new Model("Waterfall");
        model.addModule(workforce.getModule());
        model.addModule(staffAllocation.getModule());
        model.addModule(development.getModule());
        model.addModule(testAndRework.getModule());
        return model;
    }
}
