package systems.courant.shrewd.app.canvas;

import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import systems.courant.shrewd.model.def.ElementType;
import systems.courant.shrewd.model.def.ValidationIssue;

import java.util.List;
import java.util.Optional;

/**
 * Manages element hover tooltips on the canvas.
 * Shows element name, equation, or unit information on hover.
 */
final class TooltipController {

    private final Tooltip elementTooltip;

    TooltipController() {
        elementTooltip = new Tooltip();
        elementTooltip.setWrapText(true);
        elementTooltip.setMaxWidth(350);
        elementTooltip.setShowDelay(Duration.millis(400));
        elementTooltip.setHideDelay(Duration.millis(200));
    }

    /**
     * Updates the tooltip for the given element, or hides it if elementName is null.
     */
    void update(String elementName, MouseEvent event, Node owner,
                CanvasState canvasState, ModelEditor editor,
                List<ValidationIssue> validationIssues) {
        if (elementName == null || editor == null) {
            Tooltip.uninstall(owner, elementTooltip);
            elementTooltip.hide();
            return;
        }

        Optional<ElementType> typeOpt = canvasState.getType(elementName);
        if (typeOpt.isEmpty()) {
            Tooltip.uninstall(owner, elementTooltip);
            elementTooltip.hide();
            return;
        }
        ElementType type = typeOpt.get();

        String text = buildTooltipText(elementName, type, editor, validationIssues);
        elementTooltip.setText(text);
        Tooltip.install(owner, elementTooltip);
    }

    /**
     * Shows a tooltip for a cloud (external source or sink) on a flow.
     */
    void updateCloud(FlowEndpointCalculator.CloudHit cloudHit, MouseEvent event, Node owner) {
        if (cloudHit == null) {
            Tooltip.uninstall(owner, elementTooltip);
            elementTooltip.hide();
            return;
        }
        elementTooltip.setText(buildCloudTooltipText(cloudHit));
        Tooltip.install(owner, elementTooltip);
    }

    static String buildCloudTooltipText(FlowEndpointCalculator.CloudHit cloudHit) {
        String role = cloudHit.end() == FlowEndpointCalculator.FlowEnd.SOURCE
                ? "Source" : "Sink";
        String explanation = cloudHit.end() == FlowEndpointCalculator.FlowEnd.SOURCE
                ? "Material flows into the model from outside"
                : "Material flows out of the model to outside";
        return role + " cloud (" + cloudHit.flowName() + ")\n" + explanation;
    }

    private String buildTooltipText(String name, ElementType type, ModelEditor editor,
                                    List<ValidationIssue> validationIssues) {
        StringBuilder sb = new StringBuilder(name);
        switch (type) {
            case FLOW -> editor.getFlowEquation(name)
                    .filter(ElementRenderer::isDisplayableEquation)
                    .ifPresent(eq -> sb.append("\n= ").append(eq));
            case AUX -> editor.getAuxEquation(name)
                    .filter(ElementRenderer::isDisplayableEquation)
                    .ifPresent(eq -> sb.append("\n= ").append(eq));
            case STOCK -> editor.getStockUnit(name)
                    .filter(unit -> !unit.isBlank())
                    .ifPresent(unit -> sb.append("\nUnit: ").append(unit));
            default -> { }
        }
        appendValidationMessages(sb, validationIssues);
        return sb.toString();
    }

    static void appendValidationMessages(StringBuilder sb, List<ValidationIssue> issues) {
        if (issues != null && !issues.isEmpty()) {
            sb.append("\n");
            for (ValidationIssue issue : issues) {
                String icon = issue.severity() == ValidationIssue.Severity.ERROR
                        ? "\u2716 " : "\u26A0 ";
                sb.append("\n").append(icon).append(issue.message());
            }
        }
    }
}
