package systems.courant.forrester.app.canvas;

import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import systems.courant.forrester.model.def.ElementType;

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
                CanvasState canvasState, ModelEditor editor) {
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

        String text = buildTooltipText(elementName, type, editor);
        elementTooltip.setText(text);
        Tooltip.install(owner, elementTooltip);
    }

    private String buildTooltipText(String name, ElementType type, ModelEditor editor) {
        StringBuilder sb = new StringBuilder(name);
        switch (type) {
            case FLOW -> editor.getFlowEquation(name)
                    .filter(ElementRenderer::isDisplayableEquation)
                    .ifPresent(eq -> sb.append("\n= ").append(eq));
            case AUX -> editor.getAuxEquation(name)
                    .filter(ElementRenderer::isDisplayableEquation)
                    .ifPresent(eq -> sb.append("\n= ").append(eq));
            case CONSTANT -> editor.getConstantByName(name)
                    .ifPresent(cd -> sb.append("\n= ").append(ElementRenderer.formatValue(cd.value())));
            case STOCK -> editor.getStockUnit(name)
                    .filter(unit -> !unit.isBlank())
                    .ifPresent(unit -> sb.append("\nUnit: ").append(unit));
            default -> { }
        }
        return sb.toString();
    }
}
