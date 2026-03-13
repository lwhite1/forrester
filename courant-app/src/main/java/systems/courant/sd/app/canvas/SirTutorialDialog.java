package systems.courant.sd.app.canvas;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

/**
 * A step-by-step tutorial dialog that walks the user through building an
 * SIR epidemic model. Introduces reinforcing feedback, balancing feedback,
 * multi-stock systems, and S-shaped growth.
 */
public class SirTutorialDialog extends Stage {

    public SirTutorialDialog() {
        setTitle("Tutorial — SIR Epidemic Model");

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
                bold("Model an epidemic with reinforcing and balancing feedback\n\n"),
                plain("The SIR model divides a population into three groups:\n\n"),
                plain("  \u2022 "),
                bold("Susceptible"),
                plain(" \u2014 people who can catch the disease\n"),
                plain("  \u2022 "),
                bold("Infectious"),
                plain(" \u2014 people who have it and can spread it\n"),
                plain("  \u2022 "),
                bold("Recovered"),
                plain(" \u2014 people who have recovered and are immune\n\n"),
                plain("Two processes drive the epidemic:\n\n"),
                plain("  1. "),
                bold("Infection"),
                plain(" \u2014 a "),
                bold("reinforcing loop"),
                plain(". More infectious people infect more susceptible people, "
                        + "which creates even more infectious people. This drives "
                        + "exponential growth early on.\n\n"),
                plain("  2. "),
                bold("Recovery"),
                plain(" \u2014 a "),
                bold("balancing loop"),
                plain(". Infectious people recover over time, depleting the "
                        + "infectious pool. As the susceptible population shrinks, "
                        + "the infection rate slows.\n\n"),
                plain("The interplay between these two loops produces "),
                bold("S-shaped growth"),
                plain(" \u2014 the epidemic curve rises exponentially, peaks, "
                        + "then declines as susceptible people are depleted.\n\n"),
                plain("By the end, you'll answer: "),
                italic("How does the infection peak depend on the contact rate?")
        );
    }

    private TextFlow stocksTab() {
        return new TextFlow(
                bold("Place three Stocks\n\n"),
                plain("Press "),
                mono("2"),
                plain(" (or click the Stock button in the toolbar), then click on the canvas "
                        + "three times, arranging them left to right:\n\n"),
                plain("  1. "),
                bold("Susceptible"),
                plain(" \u2014 initial value "),
                mono("990"),
                plain("\n"),
                plain("  2. "),
                bold("Infectious"),
                plain(" \u2014 initial value "),
                mono("10"),
                plain("\n"),
                plain("  3. "),
                bold("Recovered"),
                plain(" \u2014 initial value "),
                mono("0"),
                plain("\n\n"),
                plain("Total population is 1000. We start with 10 infected people "
                        + "and everyone else susceptible.\n\n"),
                plain("Double-click each stock to name it and set the initial value "
                        + "in the Properties panel on the right.")
        );
    }

    private TextFlow flowsTab() {
        return new TextFlow(
                bold("Create the Infection flow\n\n"),
                plain("Press "),
                mono("3"),
                plain(" to switch to the Flow tool.\n\n"),
                plain("1. Click the "),
                bold("Susceptible"),
                plain(" stock (source)\n"),
                plain("2. Click the "),
                bold("Infectious"),
                plain(" stock (sink)\n\n"),
                plain("Double-click the flow indicator and name it "),
                mono("Infection"),
                plain(".\n"),
                plain("Set the equation to:\n\n"),
                mono("  Contact_Rate * Infectivity * Infectious"
                        + " * Susceptible / (Susceptible + Infectious + Recovered)\n\n"),
                plain("This is the standard mass-action infection rate. The fraction at "
                        + "the end is the probability a contact is with a susceptible person.\n\n"),

                bold("Create the Recovery flow\n\n"),
                plain("Press "),
                mono("3"),
                plain(" again.\n\n"),
                plain("1. Click the "),
                bold("Infectious"),
                plain(" stock (source)\n"),
                plain("2. Click the "),
                bold("Recovered"),
                plain(" stock (sink)\n\n"),
                plain("Name it "),
                mono("Recovery"),
                plain(". Set the equation to:\n\n"),
                mono("  Infectious * Recovery_Rate\n\n"),
                plain("People recover at a constant fractional rate.")
        );
    }

    private TextFlow parametersTab() {
        return new TextFlow(
                bold("Place three Parameters\n\n"),
                plain("Press "),
                mono("4"),
                plain(" to switch to the Variable tool. Create three constants:\n\n"),
                plain("  1. "),
                mono("Contact_Rate"),
                plain(" = "),
                mono("8"),
                plain(" (contacts per person per day)\n"),
                plain("  2. "),
                mono("Infectivity"),
                plain(" = "),
                mono("0.10"),
                plain(" (probability of transmission per contact)\n"),
                plain("  3. "),
                mono("Recovery_Rate"),
                plain(" = "),
                mono("0.20"),
                plain(" (20% of infected people recover each day)\n\n"),
                plain("A recovery rate of 0.20 means the average duration of infection is "
                        + "5 days (1/0.20).\n\n"),
                plain("The basic reproduction number R\u2080 = Contact_Rate \u00d7 Infectivity "
                        + "/ Recovery_Rate = 8 \u00d7 0.10 / 0.20 = "),
                bold("4"),
                plain(". Each infected person infects 4 others on average at the start "
                        + "of the epidemic, so the outbreak will grow rapidly.\n\n"),

                bold("Validate\n\n"),
                plain("Press "),
                mono("Ctrl+B"),
                plain(" to check for errors. Every variable should be resolved and "
                        + "every flow should have a valid equation.")
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
                mono("56"),
                plain("\n"),
                plain("  Duration unit:  "),
                mono("Day"),
                plain("\n\n"),
                plain("This simulates 8 weeks of the epidemic.\n\n"),

                bold("Run\n\n"),
                plain("Press "),
                mono("Ctrl+R"),
                plain(". Switch to the Chart tab in the dashboard.\n\n"),

                bold("What you should see\n\n"),
                plain("Three curves:\n\n"),
                plain("  \u2022 "),
                bold("Susceptible"),
                plain(" \u2014 starts high and drops in an S-shaped curve as people get infected\n"),
                plain("  \u2022 "),
                bold("Infectious"),
                plain(" \u2014 rises exponentially, peaks, then declines (the epidemic curve)\n"),
                plain("  \u2022 "),
                bold("Recovered"),
                plain(" \u2014 rises in an S-shaped curve as people recover\n\n"),
                plain("The infectious peak occurs when the susceptible population drops "
                        + "below the threshold needed to sustain exponential growth. This is "
                        + "the moment the reinforcing loop loses dominance to the balancing loop.")
        );
    }

    private TextFlow experimentTab() {
        return new TextFlow(
                bold("Flatten the curve\n\n"),
                plain("Reduce the Contact_Rate from 8 to 4 and re-run. The peak should "
                        + "be lower and later. This is the mechanism behind social distancing "
                        + "\u2014 reducing contacts slows the reinforcing loop.\n\n"),

                bold("Run a parameter sweep\n\n"),
                plain("  1. Go to "),
                bold("Simulate \u2192 Parameter Sweep"),
                plain("\n"),
                plain("  2. Select "),
                mono("Contact_Rate"),
                plain(" as the parameter\n"),
                plain("  3. Set Start = "),
                mono("2"),
                plain(", End = "),
                mono("12"),
                plain(", Step = "),
                mono("2"),
                plain("\n"),
                plain("  4. Click OK\n\n"),
                plain("The chart shows a family of epidemic curves. Notice how:\n\n"),
                plain("  \u2022 Lower contact rates produce smaller, later peaks\n"),
                plain("  \u2022 Higher contact rates produce taller, earlier peaks\n"),
                plain("  \u2022 The total number of recovered converges toward the full "
                        + "population regardless of rate (almost everyone eventually "
                        + "gets infected), but the peak height varies dramatically\n\n"),

                bold("Try it\n\n"),
                plain("What value of Contact_Rate keeps the infectious peak below 200 "
                        + "(20% of the population)?")
        );
    }

    private TextFlow takeawaysTab() {
        return new TextFlow(
                bold("What you learned\n\n"),
                plain("  \u2022 "),
                bold("Reinforcing feedback"),
                plain(" \u2014 infection spreads exponentially when susceptible "
                        + "people are plentiful\n"),
                plain("  \u2022 "),
                bold("Balancing feedback"),
                plain(" \u2014 recovery depletes the infectious pool, and "
                        + "depletion of susceptibles slows transmission\n"),
                plain("  \u2022 "),
                bold("S-shaped growth"),
                plain(" \u2014 the transition from reinforcing to balancing "
                        + "dominance produces a characteristic sigmoid curve\n"),
                plain("  \u2022 "),
                bold("Multi-stock models"),
                plain(" \u2014 material (people) flows between stocks "
                        + "conserving the total population\n"),
                plain("  \u2022 "),
                bold("R\u2080"),
                plain(" \u2014 the basic reproduction number determines whether "
                        + "the epidemic grows (R\u2080 > 1) or dies out (R\u2080 < 1)\n\n"),

                bold("Behavior modes seen\n\n"),
                plain("  \u2022 Exponential growth (early infection)\n"),
                plain("  \u2022 Goal-seeking (recovery approaching total population)\n"),
                plain("  \u2022 S-shaped growth (susceptible depletion curve)\n\n"),

                bold("Try next\n\n"),
                plain("  \u2022 Add a "),
                bold("vaccination flow"),
                plain(" from Susceptible directly to Recovered\n"),
                plain("  \u2022 Try the "),
                bold("Supply Chain Tutorial"),
                plain(" (Help menu) to explore delays and oscillation\n"),
                plain("  \u2022 Sketch a causal loop diagram of the SIR model "
                        + "using CLD variables ("),
                mono("7"),
                plain(") and causal links ("),
                mono("8"),
                plain(")")
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
