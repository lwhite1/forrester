package systems.courant.sd.app.canvas.dialogs;

import static systems.courant.sd.app.canvas.dialogs.StyledText.*;

import javafx.scene.control.Tab;
import javafx.scene.text.TextFlow;

import java.util.List;

/**
 * A step-by-step quickstart tutorial dialog that walks a new user through
 * building a coffee cooling model. Mirrors the content of userdocs/Quickstart.md
 * in an in-app help window.
 */
public class QuickstartDialog extends AbstractTutorialDialog {

    public QuickstartDialog() {
        super("Getting Started — Build Your First Model", 660, 540, 610);
    }

    @Override
    protected List<Tab> buildTabs() {
        return List.of(
                createTab("1. The Idea", ideaTab()),
                createTab("2. Place Elements", placeElementsTab()),
                createTab("3. Connect & Equate", connectTab()),
                createTab("4. Simulate", simulateTab()),
                createTab("5. Experiment", experimentTab()),
                createTab("6. Next Steps", nextStepsTab())
        );
    }

    private TextFlow ideaTab() {
        return new TextFlow(
                bold("Build a coffee cooling model in 10 minutes\n\n"),
                plain("You're going to simulate a cup of coffee cooling down to room temperature. "
                        + "Along the way you'll learn the four building blocks of System Dynamics:\n\n"),
                plain("  \u2022 "),
                bold("Stock"),
                plain(" \u2014 a container that accumulates (the coffee's heat)\n"),
                plain("  \u2022 "),
                bold("Flow"),
                plain(" \u2014 a rate that changes a stock (cooling)\n"),
                plain("  \u2022 "),
                bold("Variable"),
                plain(" \u2014 a computed value (the temperature gap)\n"),
                plain("  \u2022 "),
                bold("Parameter"),
                plain(" \u2014 a variable with a fixed value (room temperature, cooling rate)\n\n"),
                plain("The model uses Newton's law of cooling: the hotter the coffee relative to "
                        + "the room, the faster it cools. As the coffee approaches room temperature, "
                        + "cooling slows down. This is "),
                bold("negative feedback"),
                plain(" \u2014 the system self-corrects toward equilibrium.\n\n"),
                plain("By the end, you'll answer: "),
                italic("How long does it take to reach drinkable temperature?")
        );
    }

    private TextFlow placeElementsTab() {
        return new TextFlow(
                bold("Name your model\n\n"),
                plain("Before adding elements, give the model a name and description. "
                        + "Go to "),
                bold("File \u2192 Model Properties"),
                plain(" and enter:\n\n"),
                plain("  Name:        "),
                mono("Coffee Cooling"),
                plain("\n"),
                plain("  Description: "),
                mono("Newton's law of cooling \u2014 coffee approaching room temperature"),
                plain("\n\n"),
                plain("This helps identify your model later in file listings and reports.\n\n"),

                bold("Place a Stock\n\n"),
                plain("Press "),
                mono("2"),
                plain(" (or click the Stock button in the toolbar), then click on the canvas.\n"),
                plain("Double-click the stock to select it. In the "),
                bold("Properties panel"),
                plain(" on the right, name it: "),
                mono("Coffee Temperature"),
                plain("\n"),
                plain("Set the initial value to "),
                mono("100"),
                plain(" (degrees Celsius).\n\n"),

                bold("Place two Variables as parameters\n\n"),
                plain("Press "),
                mono("4"),
                plain(" to switch to the Variable tool.\n\n"),
                plain("Click the canvas to the right of the stock. Name it "),
                mono("Room Temperature"),
                plain(", value "),
                mono("18"),
                plain(".\n"),
                plain("Click again below. Name it "),
                mono("Cooling Rate"),
                plain(", value "),
                mono("0.10"),
                plain(".\n\n"),
                plain("The cooling rate means the coffee loses 10% of the temperature "
                        + "difference each minute.\n\n"),

                bold("Place a Variable\n\n"),
                plain("Press "),
                mono("4"),
                plain(". Click between the stock and the constants. Name it "),
                mono("Discrepancy"),
                plain(".\n"),
                plain("When prompted for the equation, type:\n\n"),
                mono("  Coffee_Temperature - Room_Temperature\n\n"),
                plain("The autocomplete dropdown suggests names as you type \u2014 press "),
                mono("Tab"),
                plain(" to accept.\n\n"),
                plain("This variable continuously recalculates: when the coffee is 100\u00b0C, "
                        + "the discrepancy is 82\u00b0C. As the coffee cools, it shrinks toward zero.")
        );
    }

    private TextFlow connectTab() {
        return new TextFlow(
                bold("Create a Flow\n\n"),
                plain("Press "),
                mono("3"),
                plain(" to switch to the Flow tool.\n\n"),
                plain("1. Click the "),
                bold("Coffee Temperature"),
                plain(" stock \u2014 this is the source\n"),
                plain("2. Click an empty area nearby \u2014 a cloud appears (heat dissipates)\n\n"),
                plain("A flow arrow connects the stock to the cloud. Double-click the diamond "
                        + "flow indicator and name it "),
                mono("Cooling"),
                plain(".\n\n"),
                plain("In the "),
                bold("Properties panel"),
                plain(" on the right, set the equation to:\n\n"),
                mono("  Discrepancy * Cooling_Rate\n\n"),
                plain("In the Properties panel, set the "),
                bold("Time Unit"),
                plain(" for this flow to "),
                mono("Minute"),
                plain(". This tells the simulation that the cooling rate is per minute "
                        + "\u2014 matching the simulation time step you will set in the "
                        + "next tab. Without this, the model will produce incorrect results.\n\n"),
                plain("This is the key feedback equation. When the coffee is hot, the "
                        + "discrepancy is large, so cooling is fast. As the coffee approaches "
                        + "room temperature, the discrepancy shrinks, and cooling slows. "
                        + "The system regulates itself.\n\n"),

                bold("Check your work\n\n"),
                plain("Press "),
                mono("Ctrl+B"),
                plain(" to validate. If everything is connected correctly, "
                        + "you'll see no errors. If something is missing, click the "
                        + "error to jump to the problem element.\n\n"),
                plain("Your model:\n"),
                plain("  \u2022 Stock: Coffee Temperature (100)\n"),
                plain("  \u2022 Flow: Cooling = Discrepancy \u00d7 Cooling_Rate\n"),
                plain("  \u2022 Variable: Discrepancy = Coffee_Temperature \u2212 Room_Temperature\n"),
                plain("  \u2022 Parameters: Room Temperature (18), Cooling Rate (0.10)")
        );
    }

    private TextFlow simulateTab() {
        return new TextFlow(
                bold("Configure simulation settings\n\n"),
                plain("Go to "),
                bold("Simulate \u2192 Simulation Settings"),
                plain(" and enter:\n\n"),
                plain("  Time step:      "),
                mono("Minute"),
                plain("\n"),
                plain("  Duration:       "),
                mono("60"),
                plain("\n"),
                plain("  Duration unit:  "),
                mono("Minute"),
                plain("\n\n"),
                plain("This simulates one hour of cooling, one minute at a time.\n\n"),

                bold("Run\n\n"),
                plain("Press "),
                mono("Ctrl+R"),
                plain(" (or Simulate \u2192 Run Simulation).\n\n"),
                plain("The dashboard opens at the bottom with two tabs:\n\n"),
                plain("  \u2022 "),
                bold("Table"),
                plain(" \u2014 a sortable grid with Coffee Temperature at each step\n"),
                plain("  \u2022 "),
                bold("Chart"),
                plain(" \u2014 a line chart plotting the temperature curve\n\n"),

                bold("What you should see\n\n"),
                plain("An exponential decay curve: the coffee drops quickly at first "
                        + "(losing ~8\u00b0C in the first minute), then slows as it "
                        + "approaches room temperature. After 60 minutes, it's around "
                        + "20\u00b0C.\n\n"),
                plain("Right-click the chart and select "),
                bold("Export CSV"),
                plain(" to save the data.")
        );
    }

    private TextFlow experimentTab() {
        return new TextFlow(
                bold("Change a parameter\n\n"),
                plain("Click "),
                bold("Cooling Rate"),
                plain(" on the canvas. In the Properties panel on the right, change its "
                        + "value from 0.10 to 0.05. Press "),
                mono("Ctrl+R"),
                plain(" again.\n\n"),
                plain("The curve is flatter \u2014 the coffee cools more slowly. "
                        + "A thicker mug, perhaps.\n\n"),

                bold("Run a parameter sweep\n\n"),
                plain("Instead of changing values one at a time, sweep the whole range:\n\n"),
                plain("  1. Go to "),
                bold("Simulate \u2192 Parameter Sweep"),
                plain("\n"),
                plain("  2. Select "),
                mono("Cooling Rate"),
                plain(" as the parameter\n"),
                plain("  3. Set Start = "),
                mono("0.02"),
                plain(", End = "),
                mono("0.20"),
                plain(", Step = "),
                mono("0.02"),
                plain("\n"),
                plain("  4. Click OK\n\n"),
                plain("The dashboard shows a family of curves \u2014 one per cooling rate. "
                        + "Toggle individual series with the checkboxes on the right.\n\n"),
                plain("You can see exactly which rate produces the behavior you want. "
                        + "What cooling rate reaches 60\u00b0C (drinkable) in 10 minutes?\n\n"),

                bold("Multi-parameter sweep\n\n"),
                plain("Try "),
                bold("Simulate \u2192 Multi-Parameter Sweep"),
                plain(" to vary both Cooling Rate and Room Temperature simultaneously. "
                        + "Each combination runs as an independent simulation.")
        );
    }

    private TextFlow nextStepsTab() {
        return new TextFlow(
                bold("What you learned\n\n"),
                plain("  \u2022 "),
                bold("Stock"),
                plain(" \u2014 accumulates a quantity (Coffee Temperature)\n"),
                plain("  \u2022 "),
                bold("Flow"),
                plain(" \u2014 changes a stock each time step (Cooling)\n"),
                plain("  \u2022 "),
                bold("Variable"),
                plain(" \u2014 computes a derived value (Discrepancy)\n"),
                plain("  \u2022 "),
                bold("Parameter"),
                plain(" \u2014 a variable with a fixed value (Room Temperature, Cooling Rate)\n"),
                plain("  \u2022 "),
                bold("Negative feedback"),
                plain(" \u2014 the system self-corrects toward equilibrium\n\n"),
                plain("These five ideas can model surprisingly complex systems \u2014 from "
                        + "disease epidemics to supply chains to climate dynamics.\n\n"),

                bold("Try next\n\n"),
                plain("  \u2022 "),
                bold("SIR Epidemic Tutorial"),
                plain(" \u2014 Help \u2192 Tutorial: SIR Epidemic. Build a multi-stock "
                        + "epidemic model with reinforcing feedback, S-shaped growth, "
                        + "and parameter sweeps\n\n"),
                plain("  \u2022 "),
                bold("Supply Chain Tutorial"),
                plain(" \u2014 Help \u2192 Tutorial: Supply Chain. Discover how delays "
                        + "cause oscillation and the bullwhip effect\n\n"),
                plain("  \u2022 "),
                bold("Explore examples"),
                plain(" \u2014 File \u2192 Open Example has models for population growth, "
                        + "epidemiology, ecology, and supply chains\n\n"),
                plain("  \u2022 "),
                bold("Sketch a causal loop diagram"),
                plain(" \u2014 press "),
                mono("7"),
                plain(" for CLD variables and "),
                mono("8"),
                plain(" for causal links. Map out your system's feedback "
                        + "structure before formalizing it\n\n"),
                plain("  \u2022 "),
                bold("Expression language"),
                plain(" \u2014 go to Help \u2192 Expression Language for the full "
                        + "function reference (SMOOTH, DELAY3, STEP, RAMP, LOOKUP, IF, etc.)\n\n"),
                plain("  \u2022 "),
                bold("Import a model"),
                plain(" \u2014 File \u2192 Open supports Vensim .mdl and XMILE files")
        );
    }

}
