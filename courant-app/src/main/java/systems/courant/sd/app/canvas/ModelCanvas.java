package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ConnectorRoute;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.ValidationIssue;
import systems.courant.sd.model.def.ViewDef;
import systems.courant.sd.model.graph.CldLoopInfo;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import systems.courant.sd.app.canvas.controllers.CanvasContextMenuController;
import systems.courant.sd.app.canvas.controllers.CausalLinkCreationController;
import systems.courant.sd.app.canvas.controllers.ConnectionRerouteController;
import systems.courant.sd.app.canvas.controllers.CopyPasteController;
import systems.courant.sd.app.canvas.controllers.DragController;
import systems.courant.sd.app.canvas.controllers.FlowCreationController;
import systems.courant.sd.app.canvas.controllers.InfoLinkCreationController;
import systems.courant.sd.app.canvas.controllers.InlineEditController;
import systems.courant.sd.app.canvas.controllers.MarqueeController;
import systems.courant.sd.app.canvas.controllers.ReattachController;
import systems.courant.sd.app.canvas.controllers.ResizeController;
import systems.courant.sd.app.canvas.controllers.SelectionController;
import systems.courant.sd.app.canvas.controllers.TooltipController;
import systems.courant.sd.app.canvas.renderers.CanvasRenderer;

/**
 * Canvas component that renders a model using the Layered Flow Diagram visual language.
 * Supports pan, zoom, click-to-select, drag-to-move, element creation, two-click flow
 * connection, inline editing, and element deletion.
 *
 * <p>Delegates to extracted facades: {@link CanvasAnalysisFacade} (analysis),
 * {@link CanvasNavigationFacade} (module navigation), {@link CanvasUndoFacade} (undo/redo),
 * and {@link CanvasElementController} (element CRUD).
 */
public class ModelCanvas extends Canvas {

    // --- Mutable model state (package-private for facade access) ---
    ModelEditor editor;
    List<ConnectorRoute> connectors = List.of();
    UndoManager undoManager;
    CanvasToolBar.Tool activeTool = CanvasToolBar.Tool.SELECT;
    CanvasToolBar toolBar;

    // Coalescing flags
    private boolean resizeRedrawScheduled;
    private boolean connectorRegenerationScheduled;

    // Core canvas state
    private final Viewport viewport = new Viewport();
    private final CanvasState canvasState = new CanvasState();
    private final CanvasRenderer renderer = new CanvasRenderer(canvasState, viewport);

    // Interaction controllers (package-private for facade access)
    private final DragController dragController = new DragController();
    private final MarqueeController marqueeController = new MarqueeController();
    private final ResizeController resizeController = new ResizeController();
    private final ReattachController reattachController = new ReattachController();
    final FlowCreationController flowCreation = new FlowCreationController();
    final CausalLinkCreationController causalLinkCreation = new CausalLinkCreationController();
    final InfoLinkCreationController infoLinkCreation = new InfoLinkCreationController();
    private final ConnectionRerouteController rerouteController = new ConnectionRerouteController();
    final InlineEditController inlineEdit = new InlineEditController();
    final SelectionController selectionController;
    final InputDispatcher inputDispatcher;
    private final TooltipController tooltipController = new TooltipController();
    final CanvasContextMenuController contextMenuController;

    // Callbacks
    private Runnable onStatusChanged;
    Consumer<Set<String>> onPasteWarning;

    // Connection selection (package-private for facade access)
    ConnectionId selectedConnection;
    boolean selectedIsCausalLink;

    // Extracted facades
    private final CanvasAnalysisFacade analysis;
    private final CanvasNavigationFacade navigation;
    private final CanvasUndoFacade undoFacade;
    private final CanvasElementController elements;

    // Sparkline data from last simulation run
    private CanvasRenderer.SparklineData sparklineData;

    // View modes
    private boolean hideVariables;
    private boolean showDelayBadges;
    private boolean hideInfoLinks;

    // Unified callbacks for inline edit and context menu controllers
    final CanvasCallbacks callbacks = new CanvasCallbacks(this);

    public ModelCanvas(Clipboard clipboard) {
        var copyPaste = new CopyPasteController(clipboard);
        this.selectionController = new SelectionController(copyPaste);
        this.navigation = new CanvasNavigationFacade(this);
        this.contextMenuController = new CanvasContextMenuController(navigation.navController());
        this.analysis = new CanvasAnalysisFacade(
                canvasState,
                () -> editor != null ? editor.toModelDefinition(canvasState.toViewDef()) : null,
                this::redraw,
                this::fireStatusChanged);
        this.undoFacade = new CanvasUndoFacade(this);
        this.elements = new CanvasElementController(this);
        this.inputDispatcher = new InputDispatcher(
                dragController, marqueeController, resizeController,
                reattachController, flowCreation, causalLinkCreation,
                infoLinkCreation, rerouteController, inlineEdit);

        setFocusTraversable(true);

        widthProperty().addListener(observable -> scheduleResizeRedraw());
        heightProperty().addListener(observable -> scheduleResizeRedraw());

        setOnScroll(event -> inputDispatcher.handleScroll(event, viewport, () -> {
            redraw();
            fireStatusChanged();
        }));
        setOnMousePressed(event -> inputDispatcher.handleMousePressed(event, this));
        setOnMouseDragged(event -> inputDispatcher.handleMouseDragged(event, this));
        setOnMouseReleased(event -> inputDispatcher.handleMouseReleased(event, this));
        setOnMouseMoved(event -> inputDispatcher.handleMouseMoved(event, this));
        setOnMouseExited(event -> inputDispatcher.handleMouseExited(event, this));
        setOnKeyPressed(event -> inputDispatcher.handleKeyPressed(event, this));
        setOnKeyReleased(event -> {
            inputDispatcher.handleKeyReleased(event);
            inputDispatcher.updateCursor(this);
        });
    }

    private void scheduleResizeRedraw() {
        if (!resizeRedrawScheduled) {
            resizeRedrawScheduled = true;
            Platform.runLater(() -> {
                resizeRedrawScheduled = false;
                redraw();
            });
        }
    }

    public boolean isResizeRedrawScheduled() {
        return resizeRedrawScheduled;
    }

    // --- Model lifecycle ---

    public void setModel(ModelEditor editor, ViewDef view) {
        this.editor = editor;
        canvasState.loadFrom(view);
        canvasState.setCldLoopInfo(
                CldLoopInfo.compute(editor.getCldVariables(), editor.getCausalLinks()));
        this.connectors = editor.generateConnectors();
        invalidateAnalysis();
        redraw();
        fireStatusChanged();
    }

    public ViewDef toViewDef() {
        return canvasState.toViewDef();
    }

    public ModelEditor getEditor() {
        return editor;
    }

    public boolean isModelLoaded() {
        return editor != null;
    }

    // --- Facade accessors ---

    public CanvasAnalysisFacade analysis() {
        return analysis;
    }

    public CanvasNavigationFacade navigation() {
        return navigation;
    }

    public CanvasUndoFacade undo() {
        return undoFacade;
    }

    public CanvasElementController elements() {
        return elements;
    }

    // --- Callbacks ---

    public void setOnStatusChanged(Runnable callback) {
        this.onStatusChanged = callback;
    }

    public void setOnPasteWarning(Consumer<Set<String>> callback) {
        this.onPasteWarning = callback;
    }

    void fireStatusChanged() {
        if (onStatusChanged != null) {
            onStatusChanged.run();
        }
    }

    // --- Undo forwarders (used by CanvasCallbacks / InputDispatcher) ---

    void saveUndoState(String label) {
        undoFacade.saveUndoState(label);
    }

    void saveUndoStateTentative(String label) {
        undoFacade.saveUndoStateTentative(label);
    }

    // --- Sparklines ---

    public void setSparklineData(CanvasRenderer.SparklineData data) {
        this.sparklineData = data;
        redraw();
    }

    public void markSparklinesStale() {
        if (sparklineData != null && !sparklineData.stale()) {
            sparklineData = new CanvasRenderer.SparklineData(
                    sparklineData.stockSeries(), true);
            redraw();
        }
    }

    public void clearSparklines() {
        sparklineData = null;
    }

    public static Map<String, double[]> extractStockSeries(
            SimulationRunner.SimulationResult result) {
        List<String> columns = result.columnNames();
        List<double[]> rows = result.rows();
        Map<String, double[]> series = new java.util.LinkedHashMap<>();

        for (int col = 1; col < columns.size(); col++) {
            String name = columns.get(col);
            double[] values = new double[rows.size()];
            for (int row = 0; row < rows.size(); row++) {
                values[row] = rows.get(row)[col];
            }
            series.put(name, values);
        }
        return series;
    }

    // --- View modes ---

    public boolean isHideVariables() {
        return hideVariables;
    }

    public void setHideVariables(boolean hide) {
        if (this.hideVariables != hide) {
            this.hideVariables = hide;
            redraw();
        }
    }

    public boolean isShowDelayBadges() {
        return showDelayBadges;
    }

    public void setShowDelayBadges(boolean show) {
        if (this.showDelayBadges != show) {
            this.showDelayBadges = show;
            redraw();
        }
    }

    public boolean isHideInfoLinks() {
        return hideInfoLinks;
    }

    public void setHideInfoLinks(boolean hide) {
        if (this.hideInfoLinks != hide) {
            this.hideInfoLinks = hide;
            redraw();
        }
    }

    void invalidateAnalysis() {
        analysis.invalidate();
    }

    // --- Selection state (read-only) ---

    public int getSelectionCount() {
        return canvasState.getSelection().size();
    }

    public Set<String> getSelectedElementNames() {
        return canvasState.getSelection();
    }

    public ConnectionId getSelectedConnection() {
        return selectedConnection;
    }

    public boolean isSelectedConnectionCausalLink() {
        return selectedIsCausalLink;
    }

    public ElementType getSelectedElementType(String name) {
        return canvasState.getType(name).orElse(null);
    }

    // --- Canvas state accessors ---

    public CanvasState canvasState() {
        return canvasState;
    }

    public Viewport viewport() {
        return viewport;
    }

    CanvasToolBar.Tool getActiveTool() {
        return activeTool;
    }

    public List<ConnectorRoute> getConnectors() {
        return connectors;
    }

    public CanvasState getCanvasState() {
        return canvasState;
    }

    public double getZoomScale() {
        return viewport.getScale();
    }

    // --- Connector management ---

    public void requestRedraw() {
        redraw();
    }

    public void regenerateConnectors() {
        if (editor == null) {
            return;
        }
        connectors = editor.generateConnectors();
        invalidateAnalysis();
    }

    public void scheduleRegenerateConnectors() {
        if (!connectorRegenerationScheduled) {
            connectorRegenerationScheduled = true;
            Platform.runLater(() -> {
                connectorRegenerationScheduled = false;
                regenerateConnectors();
                redraw();
                fireStatusChanged();
            });
        }
    }

    public boolean isConnectorRegenerationScheduled() {
        return connectorRegenerationScheduled;
    }

    void regenerateAndRedraw() {
        if (editor == null) {
            return;
        }
        connectors = editor.generateConnectors();
        invalidateAnalysis();
        redraw();
        fireStatusChanged();
    }

    public void applyMutation(Runnable mutation) {
        mutation.run();
        regenerateAndRedraw();
    }

    // --- Connection selection ---

    public void setSelectedConnection(ConnectionId connection, boolean isCausal) {
        this.selectedConnection = connection;
        this.selectedIsCausalLink = isCausal;
    }

    public void clearSelectedConnection() {
        this.selectedConnection = null;
        this.selectedIsCausalLink = false;
    }

    // --- Tooltip ---

    public void updateTooltip(String elementName, MouseEvent event) {
        List<ValidationIssue> issues = elementName != null
                ? analysis.getValidationIssues(elementName)
                : List.of();
        tooltipController.update(elementName, event, this, canvasState, editor, issues);
    }

    public void updateCloudTooltip(FlowEndpointCalculator.CloudHit cloudHit, MouseEvent event) {
        tooltipController.updateCloud(cloudHit, event, this);
    }

    // --- Zoom ---

    public void zoomIn() {
        viewport.zoomAt(getWidth() / 2, getHeight() / 2, Viewport.ZOOM_FACTOR);
        redraw();
        inputDispatcher.updateCursor(this);
        fireStatusChanged();
    }

    public void zoomOut() {
        viewport.zoomAt(getWidth() / 2, getHeight() / 2, 1.0 / Viewport.ZOOM_FACTOR);
        redraw();
        inputDispatcher.updateCursor(this);
        fireStatusChanged();
    }

    public void resetZoom() {
        viewport.reset();
        redraw();
        inputDispatcher.updateCursor(this);
        fireStatusChanged();
    }

    public void zoomToFit() {
        if (editor == null || canvasState.getDrawOrder().isEmpty()) {
            resetZoom();
            return;
        }

        ExportBounds.Bounds bounds = ExportBounds.compute(canvasState, editor);
        double canvasW = getWidth();
        double canvasH = getHeight();

        if (canvasW <= 0 || canvasH <= 0 || bounds.width() <= 0 || bounds.height() <= 0) {
            return;
        }

        double fitScale = Math.min(canvasW / bounds.width(), canvasH / bounds.height());
        fitScale = Math.min(fitScale, 1.0);

        double worldCenterX = bounds.minX() + bounds.width() / 2;
        double worldCenterY = bounds.minY() + bounds.height() / 2;

        double translateX = canvasW / 2 - worldCenterX * fitScale;
        double translateY = canvasH / 2 - worldCenterY * fitScale;

        viewport.restoreState(translateX, translateY, fitScale);
        redraw();
        inputDispatcher.updateCursor(this);
        fireStatusChanged();
    }

    // --- Tool state ---

    public void setActiveTool(CanvasToolBar.Tool tool) {
        if (flowCreation.isPending()) {
            flowCreation.cancel();
        }
        if (causalLinkCreation.isPending()) {
            causalLinkCreation.cancel();
        }
        if (infoLinkCreation.isPending()) {
            infoLinkCreation.cancel();
        }
        this.activeTool = tool;
        inputDispatcher.updateCursor(this);
    }

    public void setToolBar(CanvasToolBar toolBar) {
        this.toolBar = toolBar;
    }

    public void setOverlayPane(Pane overlayPane) {
        inlineEdit.setOverlayPane(overlayPane);
    }

    public void switchTool(CanvasToolBar.Tool tool) {
        if (toolBar != null) {
            toolBar.selectTool(tool);
        } else {
            setActiveTool(tool);
        }
        fireStatusChanged();
    }

    public void resetToolToSelect() {
        if (toolBar != null) {
            toolBar.resetToSelect();
        } else {
            activeTool = CanvasToolBar.Tool.SELECT;
        }
    }

    // --- Canvas overrides ---

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public double prefWidth(double height) {
        return getWidth();
    }

    @Override
    public double prefHeight(double width) {
        return getHeight();
    }

    // --- Rendering ---

    private void redraw() {
        renderer.render(getGraphicsContext2D(), getWidth(), getHeight(),
                new CanvasRenderer.RenderContext(
                        editor, connectors, flowCreation.getState(),
                        causalLinkCreation.getState(),
                        infoLinkCreation.getState(),
                        reattachController.toRenderState(),
                        rerouteRenderState(),
                        marqueeController.toRenderState(),
                        analysis.getActiveLoopAnalysis(),
                        analysis.getTraceAnalysis(),
                        analysis.elementIssues(),
                        sparklineData,
                        inputDispatcher.getHoveredElement(),
                        inputDispatcher.getHoveredConnection(),
                        selectedConnection,
                        hideVariables,
                        showDelayBadges,
                        hideInfoLinks,
                        analysis.maturityAnalysis()));
    }

    private CanvasRenderer.RerouteState rerouteRenderState() {
        if (!rerouteController.isActive()) {
            return CanvasRenderer.RerouteState.IDLE;
        }
        return new CanvasRenderer.RerouteState(
                true,
                rerouteController.getAnchorX(),
                rerouteController.getAnchorY(),
                rerouteController.getRubberBandX(),
                rerouteController.getRubberBandY());
    }

    void updateCursorViaDispatcher() {
        inputDispatcher.updateCursor(this);
    }
}
