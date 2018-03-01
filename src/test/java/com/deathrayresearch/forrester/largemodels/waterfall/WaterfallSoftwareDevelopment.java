package com.deathrayresearch.forrester.largemodels.waterfall;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.largemodels.waterfall.units.Tasks;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.item.People;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.measure.units.time.Year;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Module;
import com.deathrayresearch.forrester.ui.ChartViewer;
import org.junit.Test;

/**
 *
 */
public class WaterfallSoftwareDevelopment {

    private Model model = new Model("Waterfall");

    // new units
    static final People PEOPLE = People.getInstance();
    static final Tasks TASKS = Tasks.getInstance();

    // subsystem constants
    static final String WORKFORCE = "Workforce";
    static final String DEVELOPMENT = "Development";
    static final String TESTING_AND_REWORK = "Testing and Rework";
    static final String STAFF_ALLOCATION = "Staff Allocation";

    // Workforce constants
    static final String WORKFORCE_FTE = "Full Time Equivalent workforce";
    static final String EXPERIENCED_WORKFORCE_FTE = "Full Time Equivalent Experienced Workforce";

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
    static final String DAILY_RESOURCES_FOR_TRAINING = "Daily overhead for training";
   // static final String CUM_MP_FOR_TRAINING = "Cumulative overhead for training";

    // development constants
    static final String TASKS_DEVELOPED = "Tasks Developed";
    static final String POTENTIAL_PRODUCTIVITY = "Potential productivity";


    // staff allocation constants
    static final String DAILY_RESOURCES_FOR_SOFTWARE_PRODUCTION = "Daily resources for software production";
    static final String DAILY_RESOURCES_PERFORMING_QA = "Daily resources performing QA";

    // model subsystems
    private Module staffAllocation = StaffAllocation.getStaffAllocationModule(model);
    private Module workforce = Workforce.getWorkforce();
    private Module testAndRework = TestAndRework.getTestAndReworkSubSystem();

    private Module development = Development.getDevelopmentSubSystem(model);

    @Test
    public void testRun1() {

        Quantity duration = new Quantity("Simulation duration", 1, Year.getInstance());
        Simulation simulation = new Simulation(model, Day.getInstance(), duration);
        simulation.addEventHandler(ChartViewer.newInstance(simulation.getEventBus()));

        simulation.execute();
    }

    public Model getModel() {
        model.addStock(workforce.getStock(NEWLY_HIRED));
        model.addStock(workforce.getStock(EXPERIENCED));
        // model.addStock(workforce.getStock(CUM_MP_FOR_TRAINING));

        model.addVariable(workforce.getVariable(DESIRED_WORKFORCE));
        model.addVariable(workforce.getVariable(WORKFORCE_GAP));
        model.addVariable(workforce.getVariable(TOTAL_WORKFORCE));
        model.addVariable(workforce.getVariable(FRACTION_OF_WORKFORCE_WITH_EXPERIENCE));
        model.addVariable(workforce.getVariable(WORKFORCE_FTE));
        model.addVariable(workforce.getVariable(DAILY_RESOURCES_FOR_TRAINING));
        //model.addVariable(workforce.getVariable(TOTAL_WORKFORCE_CAP));
        model.addVariable(workforce.getVariable(WORKFORCE_NEED));
        //model.addVariable(workforce.getVariable(NEW_HIRE_CAP));

        model.addVariable(staffAllocation.getVariable(DAILY_RESOURCES_FOR_SOFTWARE_PRODUCTION));
        model.addVariable(staffAllocation.getVariable(DAILY_RESOURCES_PERFORMING_QA));

        model.addStock(development.getStock(TASKS_DEVELOPED));
        model.addVariable(development.getVariable(POTENTIAL_PRODUCTIVITY));

        model.addModule(workforce);
        model.addModule(staffAllocation);
        model.addModule(development);
        model.addModule(testAndRework);

        return model;
    }

}
