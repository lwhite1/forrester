package systems.courant.sd.app.canvas.dialogs;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import systems.courant.sd.app.canvas.HelpContent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A tutorial dialog whose content is loaded from Markdown step files
 * via {@link TutorialContentLoader}, rather than hardcoded in Java.
 *
 * <p>Each step becomes a tab whose content is rendered from Markdown
 * using {@link MarkdownToTextFlow}. Glossary terms in the rendered
 * text are annotated with tooltips (and optional navigation links)
 * via {@link HelpContent#annotateGlossaryTerms}.
 *
 * <p>When the tutorial's {@link TutorialContent#model()} field is
 * non-null and a model opener callback has been registered, an
 * "Open Example Model" button is displayed at the top of the first
 * tab so the user can load the companion model directly.
 */
public class ContentTutorialDialog extends AbstractTutorialDialog {

    private static final double CONTENT_MAX_WIDTH = 610;

    private static Consumer<String> glossaryNavigator;
    private static Consumer<String> modelOpener;

    /**
     * Creates a dialog for the given tutorial content.
     */
    public ContentTutorialDialog(TutorialContent content) {
        super("Tutorial \u2014 " + content.title(),
                660, 540, CONTENT_MAX_WIDTH,
                content.id(),
                buildStepTabs(content));
    }

    /**
     * Sets the callback invoked when the user clicks an annotated glossary
     * term. Pass {@code null} to retain tooltip-only annotations without
     * navigation.
     */
    public static void setGlossaryNavigator(Consumer<String> navigator) {
        glossaryNavigator = navigator;
    }

    /**
     * Sets the callback invoked when the user clicks the "Open Example
     * Model" button. The callback receives the model resource path from
     * {@link TutorialContent#model()}.
     */
    public static void setModelOpener(Consumer<String> opener) {
        modelOpener = opener;
    }

    @Override
    protected List<Tab> buildTabs() {
        return List.of();
    }

    private static List<Tab> buildStepTabs(TutorialContent content) {
        List<Tab> tabs = new ArrayList<>();
        List<TutorialContent.Step> steps = content.steps();
        for (int i = 0; i < steps.size(); i++) {
            TutorialContent.Step step = steps.get(i);
            String tabTitle = (i + 1) + ". " + step.title();
            TextFlow rendered = MarkdownToTextFlow.convert(step.markdown());
            HelpContent.annotateGlossaryTerms(rendered, glossaryNavigator);

            if (i == 0 && content.model() != null && modelOpener != null) {
                tabs.add(createModelTab(tabTitle, rendered, content.model()));
            } else {
                tabs.add(createStyledTab(tabTitle, rendered, CONTENT_MAX_WIDTH));
            }
        }
        return tabs;
    }

    /**
     * Creates a tab whose content is followed by an "Open Example Model"
     * button at the bottom. The button invokes the registered
     * {@link #modelOpener} callback with the given model resource path.
     */
    private static Tab createModelTab(String title, TextFlow rendered,
                                       String modelPath) {
        Text hint = new Text("We recommend working through the tutorial yourself,"
                + " but if you get stuck you can open the finished model:");
        hint.setStyle("-fx-font-size: 12px; -fx-fill: #555;");
        TextFlow hintFlow = new TextFlow(hint);
        hintFlow.setMaxWidth(CONTENT_MAX_WIDTH);

        Button openModelBtn = new Button("Open Example Model");
        openModelBtn.setStyle(
                "-fx-background-color: #E8F0FE; -fx-text-fill: #1A73E8;"
                + " -fx-font-size: 12px; -fx-padding: 6 16;"
                + " -fx-background-radius: 4; -fx-cursor: hand;");
        openModelBtn.setOnAction(e -> modelOpener.accept(modelPath));

        rendered.setPadding(Insets.EMPTY);
        rendered.setLineSpacing(4);
        rendered.setMaxWidth(CONTENT_MAX_WIDTH);
        makeTextCopyable(rendered);

        VBox wrapper = new VBox(8, rendered, hintFlow, openModelBtn);
        wrapper.setPadding(new Insets(16));

        ScrollPane scroll = new ScrollPane(wrapper);
        scroll.setFitToWidth(true);
        return new Tab(title, scroll);
    }
}
