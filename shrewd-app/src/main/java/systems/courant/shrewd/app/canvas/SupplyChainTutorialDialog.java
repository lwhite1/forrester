package systems.courant.shrewd.app.canvas;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

/**
 * A step-by-step tutorial dialog that walks the user through building a
 * supply chain / inventory model. Introduces delays, oscillation from
 * delayed feedback, and the bullwhip effect.
 */
public class SupplyChainTutorialDialog extends Stage {

    public SupplyChainTutorialDialog() {
        setTitle("Tutorial — Supply Chain Model");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabs.getTabs().addAll(
                createTab("1. The Idea", ideaTab()),
                createTab("2. Stocks", stocksTab()),
                createTab("3. Flows", flowsTab()),
                createTab("4. Parameters", parametersTab()),
                createTab("5. Simulate", simulateTab()),
                createTab("6. Experiment", experimentTab()),
                createTab("7. Key Takeaways", takeawaysTab())
        );

        Scene scene = new Scene(tabs, 660, 540);
        setScene(scene);
    }

    private Tab createTab(String title, TextFlow content) {
        content.setPadding(new Insets(16));
        content.setLineSpacing(4);
        content.setMaxWidth(610);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);

        return new Tab(title, scroll);
    }

    private TextFlow ideaTab() {
        return new TextFlow(
                bold("Discover how delays cause oscillation\n\n"),
                plain("You manage a warehouse. Customers order products, you ship them "
                        + "from inventory, and you place orders with a supplier to replenish "
                        + "stock. But the supplier takes time to deliver.\n\n"),
                plain("This model has two key features:\n\n"),
                plain("  1. "),
                bold("Balancing feedback"),
                plain(" \u2014 when inventory drops below target, you order more. "
                        + "When it rises above, you order less.\n\n"),
                plain("  2. "),
                bold("Delay"),
                plain(" \u2014 orders take time to arrive. You're making decisions "
                        + "based on current inventory, but the results won't arrive "
                        + "for several days.\n\n"),
                plain("The delay creates a trap: you keep ordering because inventory "
                        + "hasn't recovered yet, then all the orders arrive at once, "
                        + "overshooting your target. This produces "),
                bold("oscillation"),
                plain(" \u2014 inventory swings above and below the target.\n\n"),
                plain("This is a simplified version of the "),
                bold("bullwhip effect"),
                plain(", one of the most studied phenomena in supply chain management.\n\n"),
                plain("By the end, you'll answer: "),
                italic("How does delivery delay affect inventory stability?")
        );
    }

    private TextFlow stocksTab() {
        return new TextFlow(
                bold("Place two Stocks\n\n"),
                plain("Press "),
                mono("2"),
                plain(" to switch to the Stock tool. Place two stocks on the canvas:\n\n"),
                plain("  1. "),
                bold("Inventory"),
                plain(" \u2014 initial value "),
                mono("200"),
                plain(" (units in the warehouse)\n"),
                plain("  2. "),
                bold("Supply Line"),
                plain(" \u2014 initial value "),
                mono("0"),
                plain(" (units on order but not yet delivered)\n\n"),
                plain("The Supply Line stock tracks orders that are in transit. This is "
                        + "important: you've placed orders but they haven't arrived yet. "
                        + "Ignoring the supply line is a classic cause of over-ordering.\n\n"),
                plain("Place Inventory on the left and Supply Line on the right.")
        );
    }

    private TextFlow flowsTab() {
        return new TextFlow(
                bold("Create the Shipments flow (outflow from Inventory)\n\n"),
                plain("Press "),
                mono("3"),
                plain(" for the Flow tool.\n\n"),
                plain("1. Click "),
                bold("Inventory"),
                plain(" (source)\n"),
                plain("2. Click an empty area to the left (cloud \u2014 products go to customers)\n\n"),
                plain("Name it "),
                mono("Shipments"),
                plain(". Set the equation to:\n\n"),
                mono("  Customer_Demand\n\n"),
                plain("We assume demand is met immediately from inventory.\n\n"),

                bold("Create the Order Placement flow (inflow to Supply Line)\n\n"),
                plain("1. Click an empty area to the right of Supply Line (cloud \u2014 supplier)\n"),
                plain("2. Click "),
                bold("Supply Line"),
                plain(" (sink)\n\n"),
                plain("Name it "),
                mono("Order Placement"),
                plain(". Set the equation to:\n\n"),
                mono("  MAX(0, Customer_Demand"
                        + " + (Target_Inventory - Inventory) / Adjustment_Time"
                        + " - Supply_Line / Delivery_Delay)\n\n"),
                plain("This order rule has three parts: replace what was sold, correct "
                        + "the inventory gap, and account for what's already on order.\n\n"),

                bold("Create the Delivery flow (from Supply Line to Inventory)\n\n"),
                plain("1. Click "),
                bold("Supply Line"),
                plain(" (source)\n"),
                plain("2. Click "),
                bold("Inventory"),
                plain(" (sink)\n\n"),
                plain("Name it "),
                mono("Delivery"),
                plain(". Set the equation to:\n\n"),
                mono("  Supply_Line / Delivery_Delay\n\n"),
                plain("Orders in the supply line are delivered after the delay period.")
        );
    }

    private TextFlow parametersTab() {
        return new TextFlow(
                bold("Place four Parameters\n\n"),
                plain("Press "),
                mono("4"),
                plain(" to switch to the Variable tool. Create:\n\n"),
                plain("  1. "),
                mono("Customer_Demand"),
                plain(" = "),
                mono("20"),
                plain(" (units per day)\n"),
                plain("  2. "),
                mono("Target_Inventory"),
                plain(" = "),
                mono("200"),
                plain(" (units \u2014 same as initial inventory)\n"),
                plain("  3. "),
                mono("Adjustment_Time"),
                plain(" = "),
                mono("4"),
                plain(" (days to correct an inventory gap)\n"),
                plain("  4. "),
                mono("Delivery_Delay"),
                plain(" = "),
                mono("5"),
                plain(" (days from order to receipt)\n\n"),
                plain("The system starts in equilibrium: inventory equals target, demand "
                        + "is steady, and orders equal demand.\n\n"),

                bold("Validate\n\n"),
                plain("Press "),
                mono("Ctrl+B"),
                plain(" to check for errors before running.")
        );
    }

    private TextFlow simulateTab() {
        return new TextFlow(
                bold("Configure simulation settings\n\n"),
                plain("Go to "),
                bold("Simulate \u2192 Simulation Settings"),
                plain(" and enter:\n\n"),
                plain("  Time step:      "),
                mono("Day"),
                plain("\n"),
                plain("  Duration:       "),
                mono("100"),
                plain("\n"),
                plain("  Duration unit:  "),
                mono("Day"),
                plain("\n\n"),

                bold("Introduce a demand shock\n\n"),
                plain("To see oscillation, we need to disturb the equilibrium. "
                        + "Change Customer_Demand from a constant to a step function:\n\n"),
                plain("In the Properties panel, change Customer_Demand's equation to:\n\n"),
                mono("  20 + STEP(5, 10)\n\n"),
                plain("This means demand is 20 for the first 10 days, then jumps to 25 "
                        + "permanently. The STEP function adds 5 units starting at day 10.\n\n"),

                bold("Run\n\n"),
                plain("Press "),
                mono("Ctrl+R"),
                plain(". Watch the Inventory curve in the Chart tab.\n\n"),

                bold("What you should see\n\n"),
                plain("After the demand shock at day 10:\n\n"),
                plain("  \u2022 Inventory drops as demand exceeds deliveries\n"),
                plain("  \u2022 The ordering rule increases orders to compensate\n"),
                plain("  \u2022 But deliveries are delayed, so inventory continues to fall\n"),
                plain("  \u2022 When the delayed orders finally arrive, inventory overshoots the target\n"),
                plain("  \u2022 The cycle repeats with decreasing amplitude\n\n"),
                plain("This is "),
                bold("damped oscillation"),
                plain(" \u2014 the hallmark of delayed balancing feedback.")
        );
    }

    private TextFlow experimentTab() {
        return new TextFlow(
                bold("Change the delivery delay\n\n"),
                plain("Set Delivery_Delay to "),
                mono("10"),
                plain(" and re-run. The oscillations should be larger and slower. "
                        + "Longer delays mean more over-correction.\n\n"),
                plain("Now try "),
                mono("2"),
                plain(". The oscillations nearly vanish. Short delays allow the "
                        + "system to correct quickly.\n\n"),

                bold("Run a parameter sweep\n\n"),
                plain("  1. Go to "),
                bold("Simulate \u2192 Parameter Sweep"),
                plain("\n"),
                plain("  2. Select "),
                mono("Delivery_Delay"),
                plain(" as the parameter\n"),
                plain("  3. Set Start = "),
                mono("1"),
                plain(", End = "),
                mono("10"),
                plain(", Step = "),
                mono("1"),
                plain("\n"),
                plain("  4. Click OK\n\n"),
                plain("The chart reveals how delay length controls oscillation. "
                        + "At short delays (1\u20132 days), inventory barely wobbles. "
                        + "At long delays (8\u201310 days), inventory swings wildly.\n\n"),

                bold("Change the Adjustment Time\n\n"),
                plain("Reset Delivery_Delay to 5. Now sweep Adjustment_Time "
                        + "from 1 to 10.\n\n"),
                plain("Short adjustment times (aggressive correction) make oscillations "
                        + "worse. Longer adjustment times (patient correction) are more "
                        + "stable. This is counterintuitive: "),
                italic("reacting more cautiously produces better results"),
                plain(" when feedback is delayed.")
        );
    }

    private TextFlow takeawaysTab() {
        return new TextFlow(
                bold("What you learned\n\n"),
                plain("  \u2022 "),
                bold("Delays"),
                plain(" \u2014 orders take time to arrive, creating a gap between "
                        + "decisions and their effects\n"),
                plain("  \u2022 "),
                bold("Oscillation"),
                plain(" \u2014 delayed balancing feedback causes overshoot and "
                        + "undershoot, producing cycles\n"),
                plain("  \u2022 "),
                bold("Bullwhip effect"),
                plain(" \u2014 small demand changes are amplified through the "
                        + "supply chain\n"),
                plain("  \u2022 "),
                bold("Supply line"),
                plain(" \u2014 tracking orders in transit prevents over-ordering\n"),
                plain("  \u2022 "),
                bold("Adjustment time"),
                plain(" \u2014 aggressive correction under delay makes things "
                        + "worse, not better\n\n"),

                bold("Behavior modes seen\n\n"),
                plain("  \u2022 Damped oscillation (inventory cycles)\n"),
                plain("  \u2022 Overshoot (inventory exceeding target)\n"),
                plain("  \u2022 Goal-seeking with delay (inventory eventually settling)\n\n"),

                bold("Key insight\n\n"),
                plain("People systematically underestimate the effect of delays "
                        + "(Sterman, 1989). The instinct to \"do more\" when things "
                        + "aren't improving is counterproductive when the feedback "
                        + "is delayed \u2014 your earlier actions haven't arrived yet.\n\n"),

                bold("Try next\n\n"),
                plain("  \u2022 Replace the constant Delivery_Delay with "),
                mono("DELAY FIXED"),
                plain(" for a pure pipeline delay\n"),
                plain("  \u2022 Add a second warehouse (retailer \u2192 distributor) "
                        + "to see the bullwhip amplify\n"),
                plain("  \u2022 Explore the SIR Tutorial (Help menu) if you haven't "
                        + "already \u2014 it covers reinforcing feedback")
        );
    }

    private Text bold(String content) {
        Text text = new Text(content);
        text.setStyle("-fx-font-weight: bold;");
        return text;
    }

    private Text plain(String content) {
        return new Text(content);
    }

    private Text mono(String content) {
        Text text = new Text(content);
        text.setStyle("-fx-font-family: monospace; -fx-font-weight: bold;");
        return text;
    }

    private Text italic(String content) {
        Text text = new Text(content);
        text.setStyle("-fx-font-style: italic;");
        return text;
    }
}
