package com.deathrayresearch.dynamics.largemodels.waterfall;

import com.deathrayresearch.dynamics.Simulation;
import com.deathrayresearch.dynamics.measure.Quantity;
import com.deathrayresearch.dynamics.measure.dimension.Time;
import com.deathrayresearch.dynamics.measure.units.time.Day;
import com.deathrayresearch.dynamics.measure.units.time.Year;
import com.deathrayresearch.dynamics.model.Model;
import com.deathrayresearch.dynamics.model.SubSystem;
import com.deathrayresearch.dynamics.ui.ChartViewer;
import org.junit.Test;

/**
 *
 */
public class WaterfallSoftwareDevelopment {

    static final People PEOPLE = People.getInstance();
    static final Tasks TASKS = Tasks.getInstance();

    // subsystem constants
    static final String WORKFORCE = "Workforce";
    static final String DEVELOPMENT = "Development";
    static final String TESTING_AND_REWORK = "Testing and Rework";

    // Workforce constants
    static final String WORKFORCE_FTE = "Full time equivilent workforce";

    static final String NEW_HIRES = "New hires";
    static final String EXPERIENCED = "Experienced workers";
    static final String TOTAL_WORKFORCE = "Total Workforce";
    static final String FRACTION_OF_WORKFORCE_WITH_EXPERIENCE = "Fraction of Workforce with Experience";

    static final String WORKFORCE_NEED = "Workforce need";
    static final String DESIRED_WORKFORCE = "Desired Workforce";
    static final String NEW_HIRE_CAP = "New Hire Cap";
    static final String TOTAL_WORKFORCE_CAP = "Total Workforce Cap";

    static final String NEWLY_HIRED = "Newly hired";
    static final String WORKFORCE_GAP = "Workforce Gap";

    // development constants
    static final String TASKS_DEVELOPED = "Tasks Developed";


    private SubSystem workforce = Workforce.getWorkforce();
    private SubSystem development = Development.getDevelopmentSubSystem();
    private SubSystem testAndRework = TestAndRework.getTestAndReworkSubSystem();

    @Test
    public void testRun1() {

        Model model = new Model("Waterfall");

        model.addStock(workforce.getStock(NEWLY_HIRED));
        model.addStock(workforce.getStock(EXPERIENCED));
        model.addVariable(workforce.getVariable(DESIRED_WORKFORCE));
        model.addVariable(workforce.getVariable(WORKFORCE_GAP));
        model.addVariable(workforce.getVariable(TOTAL_WORKFORCE));
        model.addVariable(workforce.getVariable(FRACTION_OF_WORKFORCE_WITH_EXPERIENCE));
        model.addVariable(workforce.getVariable(WORKFORCE_FTE));
        //model.addVariable(workforce.getVariable(TOTAL_WORKFORCE_CAP));
        model.addVariable(workforce.getVariable(WORKFORCE_NEED));
        //model.addVariable(workforce.getVariable(NEW_HIRE_CAP));

        model.addStock(development.getStock(TASKS_DEVELOPED));

        model.addSubSystem(workforce);
        model.addSubSystem(development);
        model.addSubSystem(testAndRework);

        Quantity<Time> duration = new Quantity<>(1, Year.getInstance());
        Simulation simulation = new Simulation(model, Day.getInstance(), duration);
        simulation.addEventHandler(ChartViewer.newInstance(simulation.getEventBus()));

        simulation.executeSubSystems();
    }

}
