package systems.courant.sd.app.canvas.renderers;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.ModelEditor;
import systems.courant.sd.model.def.CommentDef;

/**
 * Renders comment elements: sticky-note style box with word-wrapped text.
 * Auto-computes size on first render if no custom size is set.
 */
final class CommentRenderer implements ElementTypeRenderer {

    @Override
    public void render(GraphicsContext gc, String name, double cx, double cy,
                       CanvasState canvasState, ModelEditor editor, boolean showDelay) {
        CommentDef commentDef = editor.getCommentByName(name);
        String text = commentDef != null ? commentDef.text() : "";
        double w;
        double h;
        if (canvasState.hasCustomSize(name)) {
            w = canvasState.getWidth(name);
            h = canvasState.getHeight(name);
        } else {
            double[] auto = ElementRenderer.computeCommentSize(text);
            w = auto[0];
            h = auto[1];
            canvasState.setSize(name, w, h);
        }
        Color customColor = canvasState.getColor(name).map(Color::web).orElse(null);
        ElementRenderer.drawComment(gc, text, cx - w / 2, cy - h / 2, w, h, customColor);
    }
}
