package systems.courant.forrester.app.canvas;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

/**
 * Floating zoom controls anchored to the bottom-left of the canvas pane.
 * Provides zoom-in, zoom-out, zoom-to-fit, and a zoom percentage display.
 */
public class ZoomOverlay extends HBox {

    private static final String BUTTON_STYLE =
            "-fx-font-size: 13; -fx-padding: 2 7; -fx-min-width: 28; -fx-min-height: 26; "
                    + "-fx-background-radius: 0; -fx-border-radius: 0; -fx-cursor: hand;";
    private static final String CONTAINER_STYLE =
            "-fx-background-color: rgba(255,255,255,0.92); "
                    + "-fx-border-color: #ccc; -fx-border-width: 1; "
                    + "-fx-border-radius: 4; -fx-background-radius: 4; "
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 4, 0, 0, 1);";

    private final Label zoomLabel = new Label("100%");

    public ZoomOverlay(ModelCanvas canvas) {
        setId("zoomOverlay");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(0);
        setPadding(new Insets(0));
        setStyle(CONTAINER_STYLE);
        setPickOnBounds(false);

        Button zoomOutBtn = new Button("\u2212"); // minus sign
        zoomOutBtn.setStyle(BUTTON_STYLE);
        zoomOutBtn.setFocusTraversable(false);
        zoomOutBtn.setTooltip(new Tooltip("Zoom out"));
        zoomOutBtn.setOnAction(e -> canvas.zoomOut());

        zoomLabel.setStyle("-fx-font-size: 11; -fx-padding: 0 4; -fx-min-width: 38; "
                + "-fx-alignment: center;");

        Button zoomInBtn = new Button("+");
        zoomInBtn.setStyle(BUTTON_STYLE);
        zoomInBtn.setFocusTraversable(false);
        zoomInBtn.setTooltip(new Tooltip("Zoom in"));
        zoomInBtn.setOnAction(e -> canvas.zoomIn());

        Label sep = new Label("|");
        sep.setStyle("-fx-text-fill: #ccc; -fx-padding: 0 2;");

        Button fitBtn = new Button("\u29C9"); // squared four corners
        fitBtn.setStyle(BUTTON_STYLE);
        fitBtn.setFocusTraversable(false);
        fitBtn.setTooltip(new Tooltip("Zoom to fit (Ctrl+Shift+F)"));
        fitBtn.setOnAction(e -> canvas.zoomToFit());

        getChildren().addAll(zoomOutBtn, zoomLabel, zoomInBtn, sep, fitBtn);
    }

    /**
     * Updates the zoom percentage display.
     */
    public void updateZoom(double scale) {
        zoomLabel.setText(Math.round(scale * 100) + "%");
    }

    /**
     * Anchors this overlay to the bottom-left of the given parent pane.
     */
    public void anchorTo(Pane parent) {
        parent.getChildren().add(this);
        // Position: 12px from left, 12px from bottom
        layoutXProperty().bind(parent.widthProperty().multiply(0).add(12));
        layoutYProperty().bind(parent.heightProperty().subtract(heightProperty()).subtract(12));
        // Ensure overlay is on top
        toFront();
    }
}
