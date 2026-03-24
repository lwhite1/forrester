package systems.courant.sd.app.canvas.dialogs;

import javafx.scene.control.Tab;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;

/**
 * A tutorial dialog whose content is loaded from Markdown step files
 * via {@link TutorialContentLoader}, rather than hardcoded in Java.
 *
 * <p>Each step becomes a tab whose content is rendered from Markdown
 * using {@link MarkdownToTextFlow}.
 */
public class ContentTutorialDialog extends AbstractTutorialDialog {

    private static final double CONTENT_MAX_WIDTH = 610;

    /**
     * Creates a dialog for the given tutorial content.
     */
    public ContentTutorialDialog(TutorialContent content) {
        super("Tutorial \u2014 " + content.title(),
                660, 540, CONTENT_MAX_WIDTH,
                content.id(),
                buildStepTabs(content));
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
            tabs.add(createStyledTab(tabTitle, rendered, CONTENT_MAX_WIDTH));
        }
        return tabs;
    }
}
