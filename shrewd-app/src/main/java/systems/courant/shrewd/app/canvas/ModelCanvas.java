package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.def.ConnectorRoute;
import systems.courant.shrewd.model.def.ElementType;
import systems.courant.shrewd.model.def.ModelDefinition;
import systems.courant.shrewd.model.def.ModelValidator;
import systems.courant.shrewd.model.def.ModuleInstanceDef;
import systems.courant.shrewd.model.def.ValidationIssue;
import systems.courant.shrewd.model.def.ValidationIssue.Severity;
import systems.courant.shrewd.model.def.ValidationResult;
import systems.courant.shrewd.model.def.ViewDef;
import systems.courant.shrewd.model.graph.AutoLayout;
import systems.courant.shrewd.model.graph.CausalTraceAnalysis;
import systems.courant.shrewd.model.graph.DependencyGraph;
import systems.courant.shrewd.model.graph.FeedbackAnalysis;

import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Canvas component that renders a model using the Layered Flow Diagram visual language.
 * Supports pan (Space+drag, middle/right drag), zoom (scroll wheel),
 * click-to-select, drag-to-move, element creation (toolbar placement mode),
 * two-click flow connection, inline name/value editing, and element deletion.
 *
 * <p>Delegates input handling to {@link InputDispatcher}, selection/mutation
 * operations to {@link SelectionController}, and tooltips to
 * {@link TooltipController}.
 */
public class ModelCanvas extends Canvas {

    private static final double ZOOM_FACTOR = 1.1;

    private ModelEditor editor;
    private List<ConnectorRoute> connectors = List.of();

    private final Viewport viewport = new Viewport();
    private final CanvasState canvasState = new CanvasState();
    private final CanvasRenderer renderer = new CanvasRenderer(canvasState, viewport);

    // Tool state
    private CanvasToolBar.Tool activeTool = CanvasToolBar.Tool.SELECT;
    private CanvasToolBar toolBar;

    // Interaction controllers
    private final DragController dragController = new DragController();
    private final MarqueeController marqueeController = new MarqueeController();
    private final ResizeController resizeController = new ResizeController();
    private final ReattachController reattachController = new ReattachController();
    private final FlowCreationController flowCreation = new FlowCreationController();
    private final CausalLinkCreationController causalLinkCreation = new CausalLinkCreationController();
    private final CopyPasteController copyPaste;
    private final ConnectionRerouteController rerouteController = new ConnectionRerouteController();
    private final InlineEditController inlineEdit = new InlineEditController();
    private final ModuleNavigationController navController = new ModuleNavigationController();

    // Extracted controllers
    private final SelectionController selectionController;
    private final InputDispatcher inputDispatcher;
    private final TooltipController tooltipController = new TooltipController();
    private final CanvasContextMenuController contextMenuController;

    // Undo/redo
    private UndoManager undoManager;

    // Status change callback
    private Runnable onStatusChanged;
    private Consumer<Set<String>> onPasteWarning;

    // Connection selection
    private ConnectionId selectedConnection;
    private boolean selectedIsCausalLink;

    // Feedback loop highlighting
    private final LoopHighlightController loopController = new LoopHighlightController();

    // Causal tracing
    private final CausalTraceController traceController = new CausalTraceController();

    // Validation issue indicators (element name → highest severity)
    private Map<String, Severity> elementIssues = Map.of();

    // Full validation issues per element (for tooltips and dialog)
    private Map<String, List<ValidationIssue>> elementIssueDetails = Map.of();

    // Last validation result (for dialog access)
    private ValidationResult lastValidationResult = new ValidationResult(List.of());

    // Callback when validation counts change
    private Consumer<ValidationResult> onValidationChanged;

    // Sparkline data from last simulation run
    private CanvasRenderer.SparklineData sparklineData;

    // View mode: hide auxiliary variables
    private boolean hideAuxiliaries;

    // View mode: show delay indicator badges
    private boolean showDelayBadges;

    // View mode: hide info links
    private boolean hideInfoLinks;

    // Inline edit callbacks
    private final InlineEditController.Callbacks inlineCallbacks =
            new InlineEditController.Callbacks() {
                @Override
                public void applyRename(String oldName, String newName) {
                    ModelCanvas.this.applyRename(oldName, newName);
                }

                @Override
                public void saveAndSetFlowEquation(String name, String equation) {
                    saveUndoState("Edit " + name + " equation");
                    editor.setFlowEquation(name, equation);
                    regenerateAndRedraw();
                }

                @Override
                public void saveAndSetAuxEquation(String name, String equation) {
                    saveUndoState("Edit " + name + " equation");
                    editor.setAuxEquation(name, equation);
                    regenerateAndRedraw();
                }

                @Override
                public void saveAndSetCommentText(String name, String text) {
                    saveUndoState("Edit " + name + " text");
                    editor.setCommentText(name, text);
                    regenerateAndRedraw();
                }

                @Override
                public void postEdit() {
                    requestFocus();
                    inputDispatcher.updateCursor(ModelCanvas.this);
                }
            };

    private final CanvasContextMenuController.Callbacks contextMenuCallbacks =
            new CanvasContextMenuController.Callbacks() {
                @Override public void startInlineEdit(String name) { ModelCanvas.this.startInlineEdit(name); }
                @Override public void deleteSelectedElements() { ModelCanvas.this.deleteSelectedElements(); }
                @Override public void cutSelection() { ModelCanvas.this.cutSelection(); }
                @Override public void copySelection() { ModelCanvas.this.copySelection(); }
                @Override public void pasteClipboard() { ModelCanvas.this.pasteClipboard(); }
                @Override public void selectAll() { ModelCanvas.this.selectAll(); }
                @Override public void switchTool(CanvasToolBar.Tool tool) { ModelCanvas.this.switchTool(tool); }
                @Override public void saveUndoState(String label) { ModelCanvas.this.saveUndoState(label); }
                @Override public void regenerateConnectors() { ModelCanvas.this.regenerateConnectors(); }
                @Override public void redraw() { ModelCanvas.this.redraw(); }
                @Override public void fireStatusChanged() { ModelCanvas.this.fireStatusChanged(); }
                @Override public void clearSelectedConnection() { ModelCanvas.this.clearSelectedConnection(); }
                @Override public void updateCursor() { inputDispatcher.updateCursor(ModelCanvas.this); }
                @Override public String createElementAt(double wx, double wy, CanvasToolBar.Tool tool) {
                    String name = selectionController.createElementAt(
                            wx, wy, tool, editor, canvasState,
                            () -> ModelCanvas.this.saveUndoState("Add " + tool.label()));
                    if (name != null) {
                        ModelCanvas.this.regenerateConnectors();
                        ModelCanvas.this.redraw();
                        ModelCanvas.this.fireStatusChanged();
                    }
                    return name;
                }
                @Override public boolean deleteConnection(ConnectionId conn, boolean isCausal) {
                    return selectionController.deleteConnection(conn, isCausal, editor,
                            () -> ModelCanvas.this.saveUndoState(
                                    "Delete " + conn.from() + " \u2192 " + conn.to() + " connection"));
                }
                @Override public boolean canPaste() { return selectionController.canPaste(); }
                @Override public void classifyCldVariable(String name, ElementType type) {
                    ModelCanvas.this.classifyCldVariable(name, type);
                }
                @Override public void drillInto(String moduleName) { ModelCanvas.this.drillInto(moduleName); }
                @Override public void openDefinePortsDialog(String moduleName) {
                    ModelCanvas.this.openDefinePortsDialog(moduleName);
                }
                @Override public void openBindingsDialog(String moduleName) {
                    ModelCanvas.this.openBindingsDialog(moduleName);
                }
                @Override public void traceUpstream(String name) {
                    ModelCanvas.this.traceUpstream(name);
                }
                @Override public void traceDownstream(String name) {
                    ModelCanvas.this.traceDownstream(name);
                }
                @Override public void showWhereUsed(String name) {
                    ModelCanvas.this.showWhereUsed(name);
                }
                @Override public void showUses(String name) {
                    ModelCanvas.this.showUses(name);
                }
            };

    public ModelCanvas(Clipboard clipboard) {
        this.copyPaste = new CopyPasteController(clipboard);
        this.selectionController = new SelectionController(copyPaste);
        this.contextMenuController = new CanvasContextMenuController(navController);
        this.inputDispatcher = new InputDispatcher(
                dragController, marqueeController, resizeController,
                reattachController, flowCreation, causalLinkCreation,
                rerouteController, inlineEdit);

        setFocusTraversable(true);

        widthProperty().addListener(observable -> redraw());
        heightProperty().addListener(observable -> redraw());

        setOnScroll(event -> inputDispatcher.handleScroll(event, viewport, () -> {
            redraw();
            fireStatusChanged();
        }));
        setOnMousePressed(event -> inputDispatcher.handleMousePressed(event, this));
        setOnMouseDragged(event -> inputDispatcher.handleMouseDragged(event, this));
        setOnMouseReleased(event -> inputDispatcher.handleMouseReleased(event, this));
        setOnMouseMoved(event -> inputDispatcher.handleMouseMoved(event, this));
        setOnKeyPressed(event -> inputDispatcher.handleKeyPressed(event, this));
        setOnKeyReleased(event -> {
            inputDispatcher.handleKeyReleased(event);
            inputDispatcher.updateCursor(this);
        });
    }

    // --- Model lifecycle ---

    public void setModel(ModelEditor editor, ViewDef view) {
        this.editor = editor;
        canvasState.loadFrom(view);
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

    public ModelDefinition toModelDefinition() {
        if (!navController.isInsideModule()) {
            return editor.toModelDefinition(canvasState.toViewDef());
        }

        ModelDefinition childDef = editor.toModelDefinition(canvasState.toViewDef());

        List<NavigationStack.Frame> frames = new ArrayList<>(navController.frames());
        for (int i = frames.size() - 1; i >= 0; i--) {
            NavigationStack.Frame frame = frames.get(i);
            ModelEditor parentEditor = new ModelEditor();
            parentEditor.loadFrom(frame.editor().toModelDefinition(frame.viewSnapshot()));
            parentEditor.updateModuleDefinition(frame.moduleIndex(), childDef);
            childDef = parentEditor.toModelDefinition(frame.viewSnapshot());
        }

        return childDef;
    }

    // --- Callbacks ---

    public void setOnStatusChanged(Runnable callback) {
        this.onStatusChanged = callback;
    }

    public void setOnPasteWarning(Consumer<Set<String>> callback) {
        this.onPasteWarning = callback;
    }

    public void setOnValidationChanged(Consumer<ValidationResult> callback) {
        this.onValidationChanged = callback;
    }

    /**
     * Returns the most recent validation result from live validation.
     */
    public ValidationResult getLastValidationResult() {
        return lastValidationResult;
    }

    /**
     * Returns the validation issues for a specific element, or an empty list.
     */
    public List<ValidationIssue> getValidationIssues(String elementName) {
        return elementIssueDetails.getOrDefault(elementName, List.of());
    }

    void fireStatusChanged() {
        if (onStatusChanged != null) {
            onStatusChanged.run();
        }
    }

    // --- Loop analysis (delegated to LoopHighlightController) ---

    public void setLoopHighlightActive(boolean active) {
        loopController.setActive(active,
                () -> editor != null ? editor.toModelDefinition(canvasState.toViewDef()) : null);
        redraw();
    }

    public boolean isLoopHighlightActive() {
        return loopController.isActive();
    }

    public FeedbackAnalysis getLoopAnalysis() {
        return loopController.getAnalysis();
    }

    public int getActiveLoopIndex() {
        return loopController.getActiveIndex();
    }

    public void setActiveLoopIndex(int index) {
        if (loopController.setActiveIndex(index)) {
            redraw();
            fireStatusChanged();
        }
    }

    public void stepLoopForward() {
        if (loopController.stepForward()) {
            redraw();
            fireStatusChanged();
        }
    }

    public void stepLoopBack() {
        if (loopController.stepBack()) {
            redraw();
            fireStatusChanged();
        }
    }

    public FeedbackAnalysis.LoopType getLoopTypeFilter() {
        return loopController.getTypeFilter();
    }

    public void setLoopTypeFilter(FeedbackAnalysis.LoopType filter) {
        if (loopController.setTypeFilter(filter)) {
            redraw();
            fireStatusChanged();
        }
    }

    public int getFilteredLoopCount() {
        return loopController.filteredLoopCount();
    }

    // --- Sparklines ---

    /**
     * Sets the sparkline data extracted from a simulation result.
     * Called by the simulation controller after a successful run.
     */
    public void setSparklineData(CanvasRenderer.SparklineData data) {
        this.sparklineData = data;
        redraw();
    }

    /**
     * Marks sparkline data as stale (model changed since last simulation).
     */
    public void markSparklinesStale() {
        if (sparklineData != null && !sparklineData.stale()) {
            sparklineData = new CanvasRenderer.SparklineData(
                    sparklineData.stockSeries(), true);
            redraw();
        }
    }

    /**
     * Clears sparkline data (e.g. when loading a new model).
     */
    public void clearSparklines() {
        sparklineData = null;
    }

    /**
     * Extracts stock time series from a simulation result into a sparkline-ready map.
     */
    public static Map<String, double[]> extractStockSeries(
            SimulationRunner.SimulationResult result) {
        List<String> columns = result.columnNames();
        List<double[]> rows = result.rows();
        Map<String, double[]> series = new java.util.LinkedHashMap<>();

        for (int col = 1; col < columns.size(); col++) {
            String name = columns.get(col);
            // Only extract stocks — they come first after "Step" in the column order.
            // We check by seeing if the unit map contains the name (all stocks are in units).
            // But simpler: just extract all columns; the renderer only draws for elements
            // that exist in canvasState with STOCK type (checked externally).
            double[] values = new double[rows.size()];
            for (int row = 0; row < rows.size(); row++) {
                values[row] = rows.get(row)[col];
            }
            series.put(name, values);
        }
        return series;
    }

    // --- Hide auxiliaries view mode ---

    public boolean isHideAuxiliaries() {
        return hideAuxiliaries;
    }

    public void setHideAuxiliaries(boolean hide) {
        if (this.hideAuxiliaries != hide) {
            this.hideAuxiliaries = hide;
            redraw();
        }
    }

    // --- Show delay badges view mode ---

    public boolean isShowDelayBadges() {
        return showDelayBadges;
    }

    public void setShowDelayBadges(boolean show) {
        if (this.showDelayBadges != show) {
            this.showDelayBadges = show;
            redraw();
        }
    }

    // --- Hide info links view mode ---

    public boolean isHideInfoLinks() {
        return hideInfoLinks;
    }

    public void setHideInfoLinks(boolean hide) {
        if (this.hideInfoLinks != hide) {
            this.hideInfoLinks = hide;
            redraw();
        }
    }

    private void invalidateLoopAnalysis() {
        loopController.invalidate(
                editor != null ? editor.toModelDefinition(canvasState.toViewDef()) : null);
    }

    // --- Validation indicators ---

    /**
     * Recomputes validation issues for all elements in the model.
     * Called from {@link #invalidateAnalysis()} on every structural mutation.
     */
    private void recomputeValidation() {
        if (editor == null) {
            elementIssues = Map.of();
            elementIssueDetails = Map.of();
            lastValidationResult = new ValidationResult(List.of());
            return;
        }
        ValidationResult result = ModelValidator.validate(
                editor.toModelDefinition(canvasState.toViewDef()));
        Map<String, Severity> issues = new LinkedHashMap<>();
        Map<String, List<ValidationIssue>> details = new LinkedHashMap<>();
        for (ValidationIssue issue : result.issues()) {
            if (issue.elementName() != null) {
                issues.merge(issue.elementName(), issue.severity(),
                        (existing, incoming) ->
                                existing == Severity.ERROR ? existing : incoming);
                details.computeIfAbsent(issue.elementName(), k -> new ArrayList<>()).add(issue);
            }
        }
        elementIssues = issues;
        elementIssueDetails = details;
        lastValidationResult = result;
        if (onValidationChanged != null) {
            onValidationChanged.accept(result);
        }
    }

    /**
     * Invalidates all cached analysis (loop highlighting, causal trace, validation indicators).
     * Must be called after any structural model mutation.
     */
    private void invalidateAnalysis() {
        invalidateLoopAnalysis();
        traceController.invalidate(
                editor != null ? editor.toModelDefinition(canvasState.toViewDef()) : null);
        recomputeValidation();
    }

    // --- Undo/redo ---

    public void setUndoManager(UndoManager undoManager) {
        this.undoManager = undoManager;
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    private UndoManager.Snapshot captureSnapshot() {
        return new UndoManager.Snapshot(
                editor.toModelDefinition(canvasState.toViewDef()),
                canvasState.toViewDef());
    }

    void saveUndoState(String label) {
        if (undoManager != null) {
            undoManager.pushUndo(captureSnapshot(), label);
        }
    }

    /**
     * Returns a short description of the current selection for undo labels.
     * Single element: its name. Multiple: "N elements". Empty: "elements".
     */
    private String describeSelection() {
        Set<String> sel = canvasState.getSelection();
        if (sel.isEmpty()) {
            return "elements";
        }
        if (sel.size() == 1) {
            return sel.iterator().next();
        }
        return sel.size() + " elements";
    }

    private void restoreSnapshot(UndoManager.Snapshot snapshot) {
        editor.loadFrom(snapshot.model());
        canvasState.loadFrom(snapshot.view());
        connectors = editor.generateConnectors();
        invalidateAnalysis();
        redraw();
        fireStatusChanged();
    }

    public void performUndo() {
        if (undoManager == null) {
            return;
        }
        undoManager.undo(captureSnapshot()).ifPresent(this::restoreSnapshot);
    }

    public void performRedo() {
        if (undoManager == null) {
            return;
        }
        undoManager.redo(captureSnapshot()).ifPresent(this::restoreSnapshot);
    }

    public void performUndoTo(int depth) {
        if (undoManager == null) {
            return;
        }
        undoManager.undoTo(captureSnapshot(), depth).ifPresent(this::restoreSnapshot);
    }

    // --- Selection state (public API) ---

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

    // --- Selection operations (public API, delegate to SelectionController) ---

    public void deleteSelectedElements() {
        selectionController.deleteSelected(editor, canvasState,
                () -> saveUndoState("Delete " + describeSelection()));
        regenerateConnectors();
        redraw();
        fireStatusChanged();
        inputDispatcher.updateCursor(this);
    }

    public void renameElement(String oldName, String newName) {
        applyRename(oldName, newName);
    }

    public void triggerBindingConfig(String moduleName) {
        openBindingsDialog(moduleName);
    }

    public void selectAll() {
        canvasState.selectAll();
        redraw();
        fireStatusChanged();
    }

    public void selectElement(String name) {
        selectionController.selectAndCenter(name, canvasState, viewport, getWidth(), getHeight());
        fireStatusChanged();
        redraw();
    }

    public void copySelection() {
        selectionController.copy(editor, canvasState);
    }

    public void cutSelection() {
        selectionController.cut(editor, canvasState,
                () -> saveUndoState("Cut " + describeSelection()));
        regenerateConnectors();
        redraw();
        fireStatusChanged();
        inputDispatcher.updateCursor(this);
    }

    public Set<String> pasteClipboard() {
        Set<String> replaced = selectionController.paste(
                editor, canvasState, () -> saveUndoState("Paste elements"));
        if (replaced == null) {
            return Set.of();
        }
        regenerateConnectors();
        redraw();
        fireStatusChanged();
        if (!replaced.isEmpty() && onPasteWarning != null) {
            onPasteWarning.accept(replaced);
        }
        return replaced;
    }

    // --- Package-private methods for InputDispatcher ---

    CanvasState canvasState() {
        return canvasState;
    }

    Viewport viewport() {
        return viewport;
    }

    CanvasToolBar.Tool getActiveTool() {
        return activeTool;
    }

    public List<ConnectorRoute> getConnectors() {
        return connectors;
    }

    void requestRedraw() {
        redraw();
    }

    void regenerateConnectors() {
        connectors = editor.generateConnectors();
        invalidateAnalysis();
    }

    void setSelectedConnection(ConnectionId connection, boolean isCausal) {
        this.selectedConnection = connection;
        this.selectedIsCausalLink = isCausal;
    }

    void clearSelectedConnection() {
        this.selectedConnection = null;
        this.selectedIsCausalLink = false;
    }

    void deleteSelectedOrConnection() {
        if (selectedConnection != null && canvasState.getSelection().isEmpty()) {
            if (selectionController.deleteConnection(
                    selectedConnection, selectedIsCausalLink, editor,
                    () -> saveUndoState("Delete " + selectedConnection.from()
                            + " \u2192 " + selectedConnection.to() + " connection"))) {
                if (!selectedIsCausalLink) {
                    connectors = editor.generateConnectors();
                }
                clearSelectedConnection();
                invalidateAnalysis();
                redraw();
                fireStatusChanged();
                inputDispatcher.updateCursor(this);
            }
        } else {
            deleteSelectedElements();
        }
    }

    void handleFlowClick(double worldX, double worldY) {
        FlowCreationController.FlowResult result = flowCreation.handleClick(
                worldX, worldY, canvasState, editor);
        if (result.isCreated()) {
            saveUndoState("Add flow");
            regenerateConnectors();
            canvasState.clearSelection();
            canvasState.select(result.flowName());
        }
        redraw();
        fireStatusChanged();
    }

    void handleCausalLinkClick(double worldX, double worldY) {
        CausalLinkCreationController.LinkResult result = causalLinkCreation.handleClick(
                worldX, worldY, canvasState, editor);
        if (result.isCreated()) {
            saveUndoState("Add causal link");
            regenerateConnectors();
        }
        redraw();
        fireStatusChanged();
    }

    void createElementAt(double worldX, double worldY) {
        String name = selectionController.createElementAt(
                worldX, worldY, activeTool, editor, canvasState,
                () -> saveUndoState("Add " + activeTool.label()));
        if (name != null) {
            regenerateConnectors();
            redraw();
            fireStatusChanged();
        }
    }

    void startInlineEdit(String elementName) {
        inlineEdit.startEdit(elementName, canvasState, editor, viewport, inlineCallbacks);
    }

    void updateTooltip(String elementName, MouseEvent event) {
        List<ValidationIssue> issues = elementName != null
                ? elementIssueDetails.getOrDefault(elementName, List.of())
                : List.of();
        tooltipController.update(elementName, event, this, canvasState, editor, issues);
    }

    void updateCloudTooltip(FlowEndpointCalculator.CloudHit cloudHit, MouseEvent event) {
        tooltipController.updateCloud(cloudHit, event, this);
    }

    void resetToolToSelect() {
        if (toolBar != null) {
            toolBar.resetToSelect();
        } else {
            activeTool = CanvasToolBar.Tool.SELECT;
        }
    }

    public void zoomIn() {
        viewport.zoomAt(getWidth() / 2, getHeight() / 2, ZOOM_FACTOR);
        redraw();
        inputDispatcher.updateCursor(this);
        fireStatusChanged();
    }

    public void zoomOut() {
        viewport.zoomAt(getWidth() / 2, getHeight() / 2, 1.0 / ZOOM_FACTOR);
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

    /**
     * Zooms and pans so that all model elements fit within the visible canvas
     * with a small margin. Does not zoom beyond 1.0 (100%) for small models.
     */
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
        fitScale = Math.min(fitScale, 1.0); // don't magnify beyond 100%

        double worldCenterX = bounds.minX() + bounds.width() / 2;
        double worldCenterY = bounds.minY() + bounds.height() / 2;

        double translateX = canvasW / 2 - worldCenterX * fitScale;
        double translateY = canvasH / 2 - worldCenterY * fitScale;

        viewport.restoreState(translateX, translateY, fitScale);
        redraw();
        inputDispatcher.updateCursor(this);
        fireStatusChanged();
    }

    // --- Rendering ---

    private void regenerateAndRedraw() {
        connectors = editor.generateConnectors();
        invalidateAnalysis();
        redraw();
        fireStatusChanged();
    }

    public void applyMutation(Runnable mutation) {
        mutation.run();
        regenerateAndRedraw(); // includes recomputeValidation()
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

    public CanvasState getCanvasState() {
        return canvasState;
    }

    public FeedbackAnalysis getActiveLoopAnalysis() {
        return loopController.getActiveAnalysis();
    }

    public double getZoomScale() {
        return viewport.getScale();
    }

    // --- Tool state ---

    public void setActiveTool(CanvasToolBar.Tool tool) {
        if (flowCreation.isPending()) {
            flowCreation.cancel();
        }
        if (causalLinkCreation.isPending()) {
            causalLinkCreation.cancel();
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
                        reattachController.toRenderState(),
                        rerouteRenderState(),
                        marqueeController.toRenderState(),
                        getActiveLoopAnalysis(),
                        traceController.getAnalysis(),
                        elementIssues,
                        sparklineData,
                        inputDispatcher.getHoveredElement(),
                        inputDispatcher.getHoveredConnection(),
                        selectedConnection,
                        hideAuxiliaries,
                        showDelayBadges,
                        hideInfoLinks));
    }

    // --- Rename ---

    private void applyRename(String oldName, String newName) {
        if (selectionController.applyRename(oldName, newName, editor, canvasState,
                () -> saveUndoState("Rename " + oldName + " \u2192 " + newName))) {
            regenerateAndRedraw();
        }
    }

    // --- Module navigation ---

    public void setOnNavigationChanged(Runnable callback) {
        navController.setOnNavigationChanged(callback);
    }

    public void drillInto(String moduleName) {
        if (editor == null) {
            return;
        }
        Optional<ModuleInstanceDef> moduleOpt = editor.getModuleByName(moduleName);
        if (moduleOpt.isEmpty()) {
            return;
        }
        ModuleInstanceDef module = moduleOpt.get();
        int moduleIndex = editor.getModuleIndex(moduleName);

        navController.push(new NavigationStack.Frame(
                moduleName, moduleIndex, editor, canvasState.toViewDef(),
                viewport.getTranslateX(), viewport.getTranslateY(),
                viewport.getScale(), undoManager, activeTool));

        ModelEditor moduleEditor = new ModelEditor();
        moduleEditor.loadFrom(module.definition());

        ViewDef moduleView;
        if (!module.definition().views().isEmpty()) {
            moduleView = module.definition().views().getFirst();
        } else {
            moduleView = AutoLayout.layout(module.definition());
        }

        this.editor = moduleEditor;
        this.undoManager = new UndoManager();
        setModel(editor, moduleView);
        viewport.reset();

        if (toolBar != null) {
            toolBar.resetToSelect();
        } else {
            activeTool = CanvasToolBar.Tool.SELECT;
        }

        navController.fireNavigationChanged();
        fireStatusChanged();
    }

    public void navigateBack() {
        if (!navController.isInsideModule()) {
            return;
        }

        ModelDefinition childDef = editor.toModelDefinition(canvasState.toViewDef());
        NavigationStack.Frame frame = navController.pop();

        this.undoManager.close();
        this.editor = frame.editor();
        this.undoManager = frame.undoManager();

        saveUndoState("Edit module " + frame.moduleName());
        editor.updateModuleDefinition(frame.moduleIndex(), childDef);

        canvasState.loadFrom(frame.viewSnapshot());
        viewport.restoreState(frame.viewportTranslateX(),
                frame.viewportTranslateY(), frame.viewportScale());

        if (toolBar != null) {
            toolBar.selectTool(frame.activeTool());
        } else {
            activeTool = frame.activeTool();
        }

        connectors = editor.generateConnectors();
        invalidateAnalysis();
        redraw();

        navController.fireNavigationChanged();
        fireStatusChanged();
    }

    public void navigateToDepth(int targetDepth) {
        while (navController.depth() > targetDepth) {
            navigateBack();
        }
    }

    public boolean isInsideModule() {
        return navController.isInsideModule();
    }

    public List<String> getNavigationPath() {
        String rootName = navController.isInsideModule()
                ? navController.frames().getFirst().editor().getModelName()
                : editor.getModelName();
        return navController.getPath(rootName);
    }

    public String getCurrentModuleName() {
        return navController.getCurrentModuleName();
    }

    public void clearNavigation() {
        navController.clear();
    }

    // --- Context menus (delegated to CanvasContextMenuController) ---

    void showElementContextMenu(String elementName, double screenX, double screenY) {
        contextMenuController.showElementContextMenu(
                this, elementName, canvasState, screenX, screenY, contextMenuCallbacks);
    }

    void showGeneralElementContextMenu(String elementName,
                                       double screenX, double screenY) {
        contextMenuController.showGeneralElementContextMenu(
                this, elementName, canvasState, screenX, screenY, contextMenuCallbacks);
    }

    void showCausalLinkContextMenu(ConnectionId link,
                                   double screenX, double screenY) {
        contextMenuController.showCausalLinkContextMenu(
                this, link, editor, screenX, screenY, contextMenuCallbacks);
    }

    void showInfoLinkContextMenu(ConnectionId link,
                                 double screenX, double screenY) {
        contextMenuController.showInfoLinkContextMenu(
                this, link, screenX, screenY, contextMenuCallbacks);
    }

    void showCanvasContextMenu(double worldX, double worldY,
                               double screenX, double screenY) {
        contextMenuController.showCanvasContextMenu(
                this, worldX, worldY, screenX, screenY, contextMenuCallbacks);
    }

    private void classifyCldVariable(String name, ElementType targetType) {
        if (selectionController.classifyCldVariable(name, targetType, editor, canvasState,
                () -> saveUndoState("Classify " + name + " as " + targetType.name().toLowerCase()))) {
            regenerateAndRedraw();
            fireStatusChanged();
        }
    }

    private void openDefinePortsDialog(String moduleName) {
        navController.openDefinePortsDialog(moduleName, editor,
                () -> saveUndoState("Define " + moduleName + " ports"), this::fireStatusChanged);
    }

    private void openBindingsDialog(String moduleName) {
        navController.openBindingsDialog(moduleName, editor,
                () -> saveUndoState("Edit " + moduleName + " bindings"), this::fireStatusChanged);
    }

    // --- Causal tracing ---

    void traceUpstream(String elementName) {
        traceController.startTrace(elementName,
                CausalTraceAnalysis.TraceDirection.UPSTREAM,
                editor.toModelDefinition(canvasState.toViewDef()));
        redraw();
    }

    void traceDownstream(String elementName) {
        traceController.startTrace(elementName,
                CausalTraceAnalysis.TraceDirection.DOWNSTREAM,
                editor.toModelDefinition(canvasState.toViewDef()));
        redraw();
    }

    boolean isTraceActive() {
        return traceController.isActive();
    }

    void clearTrace() {
        traceController.clearTrace();
    }

    /**
     * Returns the set of elements whose equations reference the named element
     * (direct dependents — "where used").
     */
    Set<String> whereUsed(String elementName) {
        DependencyGraph graph = DependencyGraph.fromDefinition(
                editor.toModelDefinition(canvasState.toViewDef()));
        return graph.dependentsOf(elementName);
    }

    /**
     * Returns the set of elements that the named element's equation references
     * (direct dependencies — "uses").
     */
    Set<String> uses(String elementName) {
        DependencyGraph graph = DependencyGraph.fromDefinition(
                editor.toModelDefinition(canvasState.toViewDef()));
        return graph.dependenciesOf(elementName);
    }

    void showWhereUsed(String elementName) {
        Set<String> dependents = whereUsed(elementName);
        canvasState.clearSelection();
        dependents.forEach(canvasState::addToSelection);
        fireStatusChanged();
        redraw();
    }

    void showUses(String elementName) {
        Set<String> dependencies = uses(elementName);
        canvasState.clearSelection();
        dependencies.forEach(canvasState::addToSelection);
        fireStatusChanged();
        redraw();
    }
}
