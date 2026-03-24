package systems.courant.sd.app.canvas.dialogs;

import static systems.courant.sd.app.canvas.dialogs.StyledText.*;

import javafx.scene.control.Tab;
import javafx.scene.text.TextFlow;

import java.util.List;

/**
 * A step-by-step tutorial dialog that walks the user through building a
 * Causal Loop Diagram (CLD) of urban traffic congestion. Introduces CLD
 * variables, causal links, polarity, reinforcing and balancing loops,
 * and the transition from qualitative to quantitative modeling.
 */
public class CldTutorialDialog extends AbstractTutorialDialog {

    public CldTutorialDialog() {
        super("Tutorial \u2014 Causal Loop Diagrams", 660, 540, 610);
    }

    @Override
    protected List<Tab> buildTabs() {
        return List.of(
                createTab("1. The Idea", ideaTab()),
                createTab("2. Variables", variablesTab()),
                createTab("3. Causal Links", causalLinksTab()),
                createTab("4. Polarity", polarityTab()),
                createTab("5. Feedback Loops", feedbackLoopsTab()),
                createTab("6. Explore", exploreTab()),
                createTab("7. Key Takeaways", takeawaysTab())
        );
    }

    private TextFlow ideaTab() {
        return new TextFlow(
                bold("Map the causal structure of urban traffic congestion\n\n"),
                plain("Before building a quantitative simulation, system dynamics "
                        + "practitioners sketch a "),
                bold("Causal Loop Diagram (CLD)"),
                plain(" \u2014 a map of cause-and-effect relationships that reveals "
                        + "the feedback structure driving system behavior.\n\n"),
                plain("In this tutorial you will diagram a familiar problem: "),
                bold("traffic congestion"),
                plain(". The story goes like this:\n\n"),
                plain("  \u2022 More "),
                bold("Cars on Road"),
                plain(" cause greater "),
                bold("Congestion"),
                plain("\n"),
                plain("  \u2022 Greater Congestion increases "),
                bold("Travel Time"),
                plain("\n"),
                plain("  \u2022 Longer Travel Time makes people choose "),
                bold("Public Transit"),
                plain("\n"),
                plain("  \u2022 More Public Transit means fewer Cars on Road\n\n"),
                plain("But there is a second loop: when congestion rises, the city "
                        + "builds more roads, which "),
                italic("initially"),
                plain(" reduces congestion \u2014 but eventually attracts more drivers.\n\n"),
                plain("By the end, you'll see how two interlocking feedback loops "
                        + "explain why building more roads does not solve congestion.")
        );
    }

    private TextFlow variablesTab() {
        return new TextFlow(
                bold("Place CLD Variables\n\n"),
                plain("Click the "),
                bold("Causal Variable"),
                plain(" button in the toolbar to switch to the CLD Variable tool.\n\n"),
                plain("Click on the canvas to place six variables. Arrange them "
                        + "roughly in a circle so there is room for links between them. "
                        + "Double-click each one to name it:\n\n"),
                plain("  1. "),
                mono("Cars on Road"),
                plain("\n"),
                plain("  2. "),
                mono("Congestion"),
                plain("\n"),
                plain("  3. "),
                mono("Travel Time"),
                plain("\n"),
                plain("  4. "),
                mono("Public Transit Use"),
                plain("\n"),
                plain("  5. "),
                mono("Road Construction"),
                plain("\n"),
                plain("  6. "),
                mono("Road Capacity"),
                plain("\n\n"),
                bold("Tip: "),
                plain("CLD variables are qualitative \u2014 they have no equations or "
                        + "units. Their purpose is to capture the causal structure of the "
                        + "system before committing to numbers.")
        );
    }

    private TextFlow causalLinksTab() {
        return new TextFlow(
                bold("Draw Causal Links\n\n"),
                plain("Click the "),
                bold("Causal Link"),
                plain(" button in the toolbar to switch to the Causal Link tool.\n\n"),
                plain("Drawing a link is a two-click operation: click the "),
                bold("cause"),
                plain(" variable, then click the "),
                bold("effect"),
                plain(" variable.\n\n"),
                plain("Draw these six links:\n\n"),
                plain("  1. "),
                mono("Cars on Road"),
                plain(" \u2192 "),
                mono("Congestion"),
                plain("\n"),
                plain("  2. "),
                mono("Congestion"),
                plain(" \u2192 "),
                mono("Travel Time"),
                plain("\n"),
                plain("  3. "),
                mono("Travel Time"),
                plain(" \u2192 "),
                mono("Public Transit Use"),
                plain("\n"),
                plain("  4. "),
                mono("Public Transit Use"),
                plain(" \u2192 "),
                mono("Cars on Road"),
                plain("\n"),
                plain("  5. "),
                mono("Congestion"),
                plain(" \u2192 "),
                mono("Road Construction"),
                plain("\n"),
                plain("  6. "),
                mono("Road Capacity"),
                plain(" \u2192 "),
                mono("Congestion"),
                plain("\n\n"),
                bold("Tip: "),
                plain("If you make a mistake, press "),
                mono("1"),
                plain(" to switch back to the Select tool, right-click the link, "
                        + "and choose Delete.")
        );
    }

    private TextFlow polarityTab() {
        return new TextFlow(
                bold("Set Link Polarity\n\n"),
                plain("Every causal link has a "),
                bold("polarity"),
                plain(" that describes the direction of influence:\n\n"),
                plain("  \u2022 "),
                bold("+"),
                plain(" (positive) \u2014 when the cause increases, the effect increases "
                        + "(all else being equal)\n"),
                plain("  \u2022 "),
                bold("\u2212"),
                plain(" (negative) \u2014 when the cause increases, the effect decreases\n"),
                plain("  \u2022 "),
                bold("?"),
                plain(" (unknown) \u2014 direction not yet determined\n\n"),
                plain("Press "),
                mono("1"),
                plain(" to switch to the Select tool. Right-click each link and "
                        + "choose "),
                bold("Set Polarity"),
                plain(":\n\n"),
                plain("  1. Cars on Road \u2192 Congestion: "),
                bold("+"),
                plain("  (more cars \u2192 more congestion)\n"),
                plain("  2. Congestion \u2192 Travel Time: "),
                bold("+"),
                plain("  (more congestion \u2192 longer trips)\n"),
                plain("  3. Travel Time \u2192 Public Transit Use: "),
                bold("+"),
                plain("  (longer trips \u2192 more people switch)\n"),
                plain("  4. Public Transit Use \u2192 Cars on Road: "),
                bold("\u2212"),
                plain("  (more transit \u2192 fewer cars)\n"),
                plain("  5. Congestion \u2192 Road Construction: "),
                bold("+"),
                plain("  (more congestion \u2192 more road building)\n"),
                plain("  6. Road Capacity \u2192 Congestion: "),
                bold("\u2212"),
                plain("  (more capacity \u2192 less congestion)\n\n"),
                bold("Important: "),
                plain("Polarity describes the "),
                italic("direction"),
                plain(" of the relationship, not its sign in an equation. "
                        + "\"More A leads to less B\" means the polarity is negative.")
        );
    }

    private TextFlow feedbackLoopsTab() {
        return new TextFlow(
                bold("Identify Feedback Loops\n\n"),
                plain("Click the "),
                bold("Loops"),
                plain(" toggle button in the toolbar to activate loop highlighting. "
                        + "The loop navigator bar appears at the bottom.\n\n"),
                plain("Your diagram contains two feedback loops:\n\n"),
                bold("Loop 1 \u2014 Balancing (B): The Transit Loop\n"),
                plain("  Cars on Road "),
                mono("+"),
                plain("\u2192 Congestion "),
                mono("+"),
                plain("\u2192 Travel Time "),
                mono("+"),
                plain("\u2192 Public Transit Use "),
                mono("\u2212"),
                plain("\u2192 Cars on Road\n\n"),
                plain("Count the negative links: there is "),
                bold("one"),
                plain(" negative link, making this a "),
                bold("balancing loop (B)"),
                plain(". Balancing loops are goal-seeking \u2014 this loop works to "
                        + "reduce congestion by pushing people to transit.\n\n"),
                bold("Loop 2 \u2014 Balancing (B): The Road Building Loop\n"),
                plain("  Congestion "),
                mono("+"),
                plain("\u2192 Road Construction \u2192 Road Capacity "),
                mono("\u2212"),
                plain("\u2192 Congestion\n\n"),
                plain("(Note: we have not drawn the Road Construction \u2192 Road Capacity "
                        + "link yet. Add it now with polarity "),
                bold("+"),
                plain(" \u2014 more construction leads to more capacity.)\n\n"),
                plain("This loop also has one negative link \u2014 another "),
                bold("balancing loop"),
                plain(". But here is the key insight: this loop has a "),
                italic("long delay"),
                plain(" between Road Construction and Road Capacity. "
                        + "And once capacity appears, it attracts new drivers "
                        + "\u2014 a phenomenon called "),
                bold("induced demand"),
                plain(".\n\n"),
                plain("Use the "),
                bold("\u25c0"),
                plain(" and "),
                bold("\u25b6"),
                plain(" buttons (or press "),
                mono("["),
                plain(" and "),
                mono("]"),
                plain(") to step through each loop and see it highlighted on the diagram.")
        );
    }

    private TextFlow exploreTab() {
        return new TextFlow(
                bold("Extend the Diagram\n\n"),
                plain("A CLD is a thinking tool. Try extending your diagram with "
                        + "additional factors:\n\n"),
                plain("  \u2022 Add a variable "),
                mono("Fuel Cost"),
                plain(" and link it to Cars on Road with negative polarity \u2014 higher "
                        + "fuel cost means fewer drivers\n"),
                plain("  \u2022 Add "),
                mono("Air Quality"),
                plain(" linked from Congestion (negative) \u2014 more congestion degrades "
                        + "air quality\n"),
                plain("  \u2022 Link Air Quality to Public Transit Use (negative) \u2014 worse "
                        + "air quality motivates people to use transit\n\n"),
                plain("Each addition may create new feedback loops. Use the loop "
                        + "navigator to discover them.\n\n"),

                bold("From CLD to Stock-and-Flow\n\n"),
                plain("Once you are satisfied with the causal structure, you can convert "
                        + "individual CLD variables into formal stock-and-flow elements:\n\n"),
                plain("  1. Press "),
                mono("1"),
                plain(" to switch to the Select tool\n"),
                plain("  2. Right-click a CLD variable\n"),
                plain("  3. Choose "),
                bold("Classify as \u2192 Stock"),
                plain(", "),
                bold("Variable"),
                plain(", or another element type\n\n"),
                plain("This preserves the variable's position and name, and lets "
                        + "you add equations and units to build a runnable simulation.")
        );
    }

    private TextFlow takeawaysTab() {
        return new TextFlow(
                bold("What you learned\n\n"),
                plain("  \u2022 "),
                bold("CLD variables"),
                plain(" \u2014 qualitative placeholders that capture causal "
                        + "relationships without equations or units\n"),
                plain("  \u2022 "),
                bold("Causal links"),
                plain(" \u2014 directed arrows showing cause-and-effect\n"),
                plain("  \u2022 "),
                bold("Polarity"),
                plain(" \u2014 whether a cause increases (+) or decreases "
                        + "(\u2212) its effect\n"),
                plain("  \u2022 "),
                bold("Balancing loops (B)"),
                plain(" \u2014 odd number of negative links; "
                        + "goal-seeking behavior\n"),
                plain("  \u2022 "),
                bold("Reinforcing loops (R)"),
                plain(" \u2014 even number of negative links (or all positive); "
                        + "self-amplifying behavior\n"),
                plain("  \u2022 "),
                bold("Induced demand"),
                plain(" \u2014 how building capacity can attract new usage, "
                        + "undermining the intended fix\n\n"),

                bold("When to use a CLD\n\n"),
                plain("  \u2022 At the start of a project, to map out the system "
                        + "before formalizing equations\n"),
                plain("  \u2022 For stakeholder communication \u2014 CLDs are "
                        + "accessible to non-modelers\n"),
                plain("  \u2022 To identify counter-intuitive feedback before "
                        + "investing in simulation detail\n\n"),

                bold("Try next\n\n"),
                plain("  \u2022 Convert your CLD into a stock-and-flow model and "
                        + "run a simulation\n"),
                plain("  \u2022 Try the "),
                bold("Getting Started"),
                plain(" tutorial (Help menu) to build a quantitative model from scratch\n"),
                plain("  \u2022 Try the "),
                bold("SIR Epidemic Tutorial"),
                plain(" (Help menu) to see reinforcing and balancing feedback in action")
        );
    }

}
