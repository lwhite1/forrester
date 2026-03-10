package systems.courant.forrester.app.models;

import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.ModelDefinitionBuilder;
import systems.courant.forrester.model.graph.AutoLayout;

import java.util.List;

/**
 * Canonical example models for bundling as JSON resources.
 * Each method returns a {@link ModelDefinition} with expression-string equations
 * suitable for serialization.
 */
public final class CanonicalModels {

    private CanonicalModels() {
    }

    /** Simple exponential growth with births and deaths. */
    public static ModelDefinition exponentialGrowth() {
        return new ModelDefinitionBuilder()
                .name("Exponential Growth")
                .comment("Population growing with constant birth and death rates")
                .stock("Population", 100, "Person")
                .flow("Births", "Population * Birth_Rate", "Day", null, "Population")
                .flow("Deaths", "Population * Death_Rate", "Day", "Population", null)
                .constant("Birth_Rate", 0.04, "Dimensionless unit")
                .constant("Death_Rate", 0.03, "Dimensionless unit")
                .defaultSimulation("Day", 365, "Day")
                .build();
    }

    /** Bathtub with delayed inflow and capped outflow. */
    public static ModelDefinition bathtub() {
        return new ModelDefinitionBuilder()
                .name("Bathtub")
                .comment("Water drains at a fixed rate; inflow begins after 5 minutes")
                .stock("Water_in_Tub", 50, "Gallon")
                .flow("Outflow", "MIN(Outflow_Rate, Water_in_Tub)", "Minute",
                        "Water_in_Tub", null)
                .flow("Inflow", "STEP(Inflow_Rate, 5)", "Minute",
                        null, "Water_in_Tub")
                .constant("Outflow_Rate", 5, "Gallon per Minute")
                .constant("Inflow_Rate", 5, "Gallon per Minute")
                .defaultSimulation("Minute", 10, "Minute")
                .build();
    }

    /** Goal-seeking negative feedback loop. */
    public static ModelDefinition goalSeeking() {
        return new ModelDefinitionBuilder()
                .name("Goal Seeking")
                .comment("Inventory adjusts toward a goal via negative feedback")
                .stock("Inventory", 100, "Unit")
                .flow("Production", "(Goal - Inventory) / Adjustment_Time", "Day",
                        null, "Inventory")
                .constant("Goal", 860, "Unit")
                .constant("Adjustment_Time", 8, "Day")
                .defaultSimulation("Day", 84, "Day")
                .build();
    }

    /** Newton's law of cooling applied to a cup of coffee. */
    public static ModelDefinition coffeeCooling() {
        return new ModelDefinitionBuilder()
                .name("Coffee Cooling")
                .comment("Coffee temperature decays toward room temperature")
                .stock("Coffee_Temperature", 100, "Celsius")
                .aux("Discrepancy", "Coffee_Temperature - Room_Temperature", "Celsius")
                .flow("Cooling", "Discrepancy * Cooling_Rate", "Minute",
                        "Coffee_Temperature", null)
                .constant("Room_Temperature", 18, "Celsius")
                .constant("Cooling_Rate", 0.10, "Dimensionless unit")
                .defaultSimulation("Minute", 8, "Minute")
                .build();
    }

    /** Classic SIR compartmental epidemic model. */
    public static ModelDefinition sirEpidemic() {
        return new ModelDefinitionBuilder()
                .name("SIR Epidemic")
                .comment("Susceptible-Infectious-Recovered compartmental model")
                .stock("Susceptible", 1000, "Person")
                .stock("Infectious", 10, "Person")
                .stock("Recovered", 0, "Person")
                .flow("Infection",
                        "Contact_Rate * Infectious / (Susceptible + Infectious + Recovered) * Infectivity * Susceptible",
                        "Day", "Susceptible", "Infectious")
                .flow("Recovery", "Infectious * Recovery_Rate", "Day",
                        "Infectious", "Recovered")
                .constant("Contact_Rate", 8.0, "Dimensionless unit")
                .constant("Infectivity", 0.10, "Dimensionless unit")
                .constant("Recovery_Rate", 0.20, "Dimensionless unit")
                .defaultSimulation("Day", 56, "Day")
                .build();
    }

    /** Lotka-Volterra predator-prey oscillation model. */
    public static ModelDefinition predatorPrey() {
        return new ModelDefinitionBuilder()
                .name("Predator Prey")
                .comment("Lotka-Volterra model of rabbits and coyotes")
                .stock("Rabbits", 100, "Animal")
                .stock("Coyotes", 10, "Animal")
                .flow("Prey_Births", "Prey_Birth_Rate * Rabbits", "Year",
                        null, "Rabbits")
                .flow("Prey_Deaths", "Predation_Rate * Rabbits * Coyotes", "Year",
                        "Rabbits", null)
                .flow("Predator_Births",
                        "Predator_Efficiency * Predation_Rate * Rabbits * Coyotes", "Year",
                        null, "Coyotes")
                .flow("Predator_Deaths", "Predator_Death_Rate * Coyotes", "Year",
                        "Coyotes", null)
                .constant("Prey_Birth_Rate", 1.0, "Dimensionless unit")
                .constant("Predation_Rate", 0.01, "Dimensionless unit")
                .constant("Predator_Efficiency", 0.5, "Dimensionless unit")
                .constant("Predator_Death_Rate", 0.8, "Dimensionless unit")
                .defaultSimulation("Day", 7300, "Day")
                .build();
    }

    /** Logistic S-shaped population growth with carrying capacity. */
    public static ModelDefinition sShapedGrowth() {
        return new ModelDefinitionBuilder()
                .name("S-Shaped Growth")
                .comment("Logistic population growth limited by carrying capacity")
                .stock("Population", 10, "Person")
                .flow("Births",
                        "Population * Max_Birth_Rate * (1 - Population / Carrying_Capacity)",
                        "Day", null, "Population")
                .constant("Max_Birth_Rate", 0.04, "Dimensionless unit")
                .constant("Carrying_Capacity", 1000, "Person")
                .defaultSimulation("Day", 224, "Day")
                .build();
    }

    /** Inventory oscillation with perception and delivery delays. */
    public static ModelDefinition inventoryOscillation() {
        return new ModelDefinitionBuilder()
                .name("Inventory Oscillation")
                .comment("Car dealership inventory with perception and delivery delays")
                .stock("Cars_on_Lot", 200, "Car")
                .stock("Perceived_Sales", 20, "Car")
                .aux("Customer_Demand",
                        "IF(TIME > 25, Step_Demand, Base_Demand)", "Car")
                .aux("Desired_Inventory",
                        "Perceived_Sales * Desired_Inventory_Multiplier", "Car")
                .aux("Inventory_Gap",
                        "Desired_Inventory - Cars_on_Lot", "Car")
                .aux("Orders_to_Factory",
                        "MAX(Perceived_Sales + Inventory_Gap / Response_Delay, 0)", "Car")
                .flow("Sales", "MIN(Cars_on_Lot, Customer_Demand)", "Day",
                        "Cars_on_Lot", null)
                .flow("Perception_Adjustment",
                        "(Sales - Perceived_Sales) / Perception_Delay", "Day",
                        null, "Perceived_Sales")
                .flow("Deliveries", "DELAY3(Orders_to_Factory, Delivery_Delay)", "Day",
                        null, "Cars_on_Lot")
                .constant("Base_Demand", 20, "Car per Day")
                .constant("Step_Demand", 22, "Car per Day")
                .constant("Perception_Delay", 5, "Day")
                .constant("Response_Delay", 3, "Day")
                .constant("Delivery_Delay", 5, "Day")
                .constant("Desired_Inventory_Multiplier", 10, "Dimensionless unit")
                .defaultSimulation("Day", 100, "Day")
                .build();
    }

    /** Returns all canonical models in order. */
    public static List<ModelDefinition> all() {
        return List.of(
                exponentialGrowth(),
                bathtub(),
                goalSeeking(),
                coffeeCooling(),
                sirEpidemic(),
                predatorPrey(),
                sShapedGrowth(),
                inventoryOscillation()
        );
    }

    /** Returns a model definition with an auto-generated layout view added. */
    public static ModelDefinition addAutoLayout(ModelDefinition def) {
        var view = AutoLayout.layout(def);
        return def.toBuilder().clearViews().view(view).build();
    }
}
