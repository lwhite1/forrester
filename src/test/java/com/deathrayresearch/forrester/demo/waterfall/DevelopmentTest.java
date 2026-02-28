package com.deathrayresearch.forrester.demo.waterfall;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.dimensionless.DimensionlessUnits;
import com.deathrayresearch.forrester.measure.units.item.ItemUnits;
import com.deathrayresearch.forrester.measure.units.time.TimeUnits;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DevelopmentTest {

    @Test
    void shouldCompleteTasks() {
        Variable staffing = new Variable("Staffing", ItemUnits.PEOPLE, () -> 5.0);
        Variable fractionExperienced = new Variable("Fraction Experienced",
                DimensionlessUnits.DIMENSIONLESS, () -> 0.8);

        Development development = new Development(staffing, fractionExperienced);

        Model model = new Model("Dev Test");
        model.addModule(development.getModule());

        Simulation sim = new Simulation(model, TimeUnits.DAY,
                new Quantity(30, TimeUnits.DAY));
        sim.execute();

        Stock tasksDeveloped = development.getModule().getStock("Tasks Developed");
        assertTrue(tasksDeveloped.getValue() > 0,
                "Tasks developed should be positive after running development");
    }

    @Test
    void shouldReflectExperienceInProductivity() {
        Variable staffing = new Variable("Staffing", ItemUnits.PEOPLE, () -> 10.0);
        Variable allExperienced = new Variable("Experienced",
                DimensionlessUnits.DIMENSIONLESS, () -> 1.0);
        Variable noExperience = new Variable("Inexperienced",
                DimensionlessUnits.DIMENSIONLESS, () -> 0.0);

        Development expDev = new Development(staffing, allExperienced);
        Development newDev = new Development(staffing, noExperience);

        Model expModel = new Model("Exp Dev");
        expModel.addModule(expDev.getModule());
        Simulation expSim = new Simulation(expModel, TimeUnits.DAY,
                new Quantity(30, TimeUnits.DAY));
        expSim.execute();

        Model newModel = new Model("New Dev");
        newModel.addModule(newDev.getModule());
        Simulation newSim = new Simulation(newModel, TimeUnits.DAY,
                new Quantity(30, TimeUnits.DAY));
        newSim.execute();

        double expTasks = expDev.getModule().getStock("Tasks Developed").getValue();
        double newTasks = newDev.getModule().getStock("Tasks Developed").getValue();

        assertTrue(expTasks > newTasks,
                "Experienced team should complete more tasks than inexperienced team");
    }
}
