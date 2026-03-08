package systems.courant.forrester.app.canvas;

import systems.courant.forrester.model.def.CausalLinkDef;
import systems.courant.forrester.model.def.ConnectorRoute;
import systems.courant.forrester.model.def.ElementType;
import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.ModelValidator;
import systems.courant.forrester.model.def.ModuleInstanceDef;
import systems.courant.forrester.model.def.ValidationIssue;
import systems.courant.forrester.model.def.ValidationIssue.Severity;
import systems.courant.forrester.model.def.ValidationResult;
import systems.courant.forrester.model.def.ViewDef;
import systems.courant.forrester.model.graph.AutoLayout;
import systems.courant.forrester.model.graph.FeedbackAnalysis;

import javafx.scene.canvas.Canvas;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
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

    // Undo/redo
    private UndoManager undoManager;

    // Status change callback
    private Runnable onStatusChanged;
    private Consumer<Set<String>> onPasteWarning;

    // Connection selection
    private ConnectionId selectedConnection;
    private boolean selectedIsCausalLink;

    // Feedback loop highlighting
    private boolean loopHighlightActive;
    private FeedbackAnalysis loopAnalysis;
    /** Active loop index for step-through mode. -1 = show all loops. */
    private int activeLoopIndex = -1;

    // Validation issue indicators (element name → highest severity)
    private Map<String, Severity> elementIssues = Map.of();

    // Sparkline data from last simulation run
    private CanvasRenderer.SparklineData sparklineData;

    // Inline edit callbacks
    private final InlineEditController.Callbacks inlineCallbacks =
            new InlineEditController.Callbacks() {
                @Override
                public void applyRename(String oldName, String newName) {
                    ModelCanvas.this.applyRename(oldName, newName);
                }

                @Override
                public void saveAndSetConstantValue(String name, double value) {
                    saveUndoState("Edit constant");
                    editor.setConstantValue(name, value);
                    recomputeValidation();
                    redraw();
                }

                @Override
                public void saveAndSetFlowEquation(String name, String equation) {
                    saveUndoState("Edit equation");
                    editor.setFlowEquation(name, equation);
                    regenerateAndRedraw();
                }

                @Override
                public void saveAndSetAuxEquation(String name, String equation) {
                    saveUndoState("Edit equation");
                    editor.setAuxEquation(name, equation);
                    regenerateAndRedraw();
                }

                @Override
                public void postEdit() {
                    requestFocus();
                    inputDispatcher.updateCursor(ModelCanvas.this);
                }
            };

    public ModelCanvas(Clipboard clipboard) {
        this.copyPaste = new CopyPasteController(clipboard);
        this.selectionController = new SelectionController(copyPaste);
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

    private void fireStatusChanged() {
        if (onStatusChanged != null) {
            onStatusChanged.run();
        }
    }

    // --- Loop analysis ---

    public void setLoopHighlightActive(boolean active) {
        this.loopHighlightActive = active;
        this.activeLoopIndex = -1;
        if (active && editor != null) {
            recomputeLoopAnalysis();
        } else {
            this.loopAnalysis = null;
        }
        redraw();
    }

    public boolean isLoopHighlightActive() {
        return loopHighlightActive;
    }

    public FeedbackAnalysis getLoopAnalysis() {
        return loopAnalysis;
    }

    /**
     * Returns the active loop index for step-through mode.
     * -1 means all loops are shown.
     */
    public int getActiveLoopIndex() {
        return activeLoopIndex;
    }

    /**
     * Sets the active loop index. -1 = show all loops.
     */
    public void setActiveLoopIndex(int index) {
        if (loopAnalysis == null) {
            return;
        }
        int count = loopAnalysis.loopCount();
        if (index < -1 || index >= count) {
            index = -1;
        }
        this.activeLoopIndex = index;
        redraw();
        fireStatusChanged();
    }

    /**
     * Steps to the next loop, wrapping around. If showing all, goes to first loop.
     */
    public void stepLoopForward() {
        if (loopAnalysis == null || loopAnalysis.loopCount() == 0) {
            return;
        }
        int count = loopAnalysis.loopCount();
        if (activeLoopIndex < 0) {
            setActiveLoopIndex(0);
        } else {
            setActiveLoopIndex((activeLoopIndex + 1) % count);
        }
    }

    /**
     * Steps to the previous loop, wrapping around. If showing all, goes to last loop.
     */
    public void stepLoopBack() {
        if (loopAnalysis == null || loopAnalysis.loopCount() == 0) {
            return;
        }
        int count = loopAnalysis.loopCount();
        if (activeLoopIndex < 0) {
            setActiveLoopIndex(count - 1);
        } else {
            setActiveLoopIndex((activeLoopIndex - 1 + count) % count);
        }
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

    private void recomputeLoopAnalysis() {
        if (editor == null) {
            loopAnalysis = null;
            activeLoopIndex = -1;
            return;
        }
        loopAnalysis = FeedbackAnalysis.analyze(
                editor.toModelDefinition(canvasState.toViewDef()));
        // Clamp index if loops were removed
        if (activeLoopIndex >= loopAnalysis.loopCount()) {
            activeLoopIndex = -1;
        }
    }

    private void invalidateLoopAnalysis() {
        if (loopHighlightActive && editor != null) {
            recomputeLoopAnalysis();
        }
    }

    // --- Validation indicators ---

    /**
     * Recomputes validation issues for all elements in the model.
     * Called from {@link #invalidateAnalysis()} on every structural mutation.
     */
    private void recomputeValidation() {
        if (editor == null) {
            elementIssues = Map.of();
            return;
        }
        ValidationResult result = ModelValidator.validate(
                editor.toModelDefinition(canvasState.toViewDef()));
        Map<String, Severity> issues = new LinkedHashMap<>();
        for (ValidationIssue issue : result.issues()) {
            if (issue.elementName() != null) {
                issues.merge(issue.elementName(), issue.severity(),
                        (existing, incoming) ->
                                existing == Severity.ERROR ? existing : incoming);
            }
        }
        elementIssues = issues;
    }

    /**
     * Invalidates all cached analysis (loop highlighting, validation indicators).
     * Must be called after any structural model mutation.
     */
    private void invalidateAnalysis() {
        invalidateLoopAnalysis();
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

    private void restoreSnapshot(UndoManager.Snapshot snapshot) {
        editor.loadFrom(snapshot.model());
        canvasState.loadFrom(snapshot.view());
        connectors = editor.generateConnectors();
        invalidateAnalysis();
        redraw();
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
        selectionController.deleteSelected(editor, canvasState, () -> saveUndoState("Delete"));
        regenerateConnectors();
        redraw();
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
    }

    public void selectElement(String name) {
        selectionController.selectAndCenter(name, canvasState, viewport, getWidth(), getHeight());
        redraw();
    }

    public void copySelection() {
        selectionController.copy(editor, canvasState);
    }

    public void cutSelection() {
        selectionController.cut(editor, canvasState, () -> saveUndoState("Delete"));
        regenerateConnectors();
        redraw();
        inputDispatcher.updateCursor(this);
    }

    public Set<String> pasteClipboard() {
        Set<String> replaced = selectionController.paste(
                editor, canvasState, () -> saveUndoState("Paste"));
        if (replaced == null) {
            return Set.of();
        }
        regenerateConnectors();
        redraw();
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
                    () -> saveUndoState("Delete connection"))) {
                if (!selectedIsCausalLink) {
                    connectors = editor.generateConnectors();
                }
                clearSelectedConnection();
                invalidateAnalysis();
                redraw();
                inputDispatcher.updateCursor(this);
            }
        } else {
            deleteSelectedElements();
        }
    }

    void handleFlowClick(double worldX, double worldY) {
        if (flowCreation.isPending()) {
            saveUndoState("Add flow");
        }
        FlowCreationController.FlowResult result = flowCreation.handleClick(
                worldX, worldY, canvasState, editor);
        if (result.isCreated()) {
            regenerateConnectors();
            canvasState.clearSelection();
            canvasState.select(result.flowName());
        }
        redraw();
    }

    void handleCausalLinkClick(double worldX, double worldY) {
        if (causalLinkCreation.isPending()) {
            saveUndoState("Add causal link");
        }
        CausalLinkCreationController.LinkResult result = causalLinkCreation.handleClick(
                worldX, worldY, canvasState, editor);
        if (result.isCreated()) {
            regenerateConnectors();
        }
        redraw();
    }

    void createElementAt(double worldX, double worldY) {
        String name = selectionController.createElementAt(
                worldX, worldY, activeTool, editor, canvasState,
                () -> saveUndoState("Add element"));
        if (name != null) {
            regenerateConnectors();
            redraw();
        }
    }

    void startInlineEdit(String elementName) {
        inlineEdit.startEdit(elementName, canvasState, editor, viewport, inlineCallbacks);
    }

    void updateTooltip(String elementName, MouseEvent event) {
        tooltipController.update(elementName, event, this, canvasState, editor);
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
        if (!loopHighlightActive || loopAnalysis == null) {
            return null;
        }
        if (activeLoopIndex >= 0) {
            return loopAnalysis.filterToLoop(activeLoopIndex);
        }
        return loopAnalysis;
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
                        elementIssues,
                        sparklineData,
                        inputDispatcher.getHoveredElement(),
                        inputDispatcher.getHoveredConnection(),
                        selectedConnection));
        fireStatusChanged();
    }

    // --- Rename ---

    private void applyRename(String oldName, String newName) {
        if (selectionController.applyRename(oldName, newName, editor, canvasState,
                () -> saveUndoState("Rename"))) {
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

        saveUndoState("Edit module");
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

    // --- Context menus ---

    void showElementContextMenu(String elementName, double screenX, double screenY) {
        navController.showContextMenu(this, elementName, canvasState, screenX, screenY,
                this::drillInto, this::openBindingsDialog, this::startInlineEdit,
                this::classifyCldVariable);
    }

    private void classifyCldVariable(String name, ElementType targetType) {
        if (selectionController.classifyCldVariable(name, targetType, editor, canvasState,
                () -> saveUndoState("Classify variable"))) {
            regenerateAndRedraw();
            fireStatusChanged();
        }
    }

    /**
     * Shows a context menu for a general element (stock, flow, auxiliary, constant, lookup).
     * Module and CLD variable elements are handled by {@link #showElementContextMenu}.
     */
    void showGeneralElementContextMenu(String elementName,
                                       double screenX, double screenY) {
        ContextMenu menu = new ContextMenu();

        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(e -> startInlineEdit(elementName));

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            canvasState.clearSelection();
            canvasState.select(elementName);
            deleteSelectedElements();
            fireStatusChanged();
        });

        MenuItem cutItem = new MenuItem("Cut");
        cutItem.setOnAction(e -> {
            canvasState.clearSelection();
            canvasState.select(elementName);
            cutSelection();
            fireStatusChanged();
        });

        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setOnAction(e -> {
            canvasState.clearSelection();
            canvasState.select(elementName);
            copySelection();
        });

        menu.getItems().addAll(editItem, new SeparatorMenuItem(),
                cutItem, copyItem, new SeparatorMenuItem(), deleteItem);
        menu.show(this, screenX, screenY);
    }

    void showCausalLinkContextMenu(ConnectionId link,
                                   double screenX, double screenY) {
        ContextMenu menu = new ContextMenu();

        Menu polarityMenu = new Menu("Set Polarity");
        for (CausalLinkDef.Polarity p : CausalLinkDef.Polarity.values()) {
            MenuItem item = new MenuItem(p.symbol() + " (" + p.name().toLowerCase() + ")");
            item.setOnAction(e -> {
                saveUndoState("Set polarity");
                editor.setCausalLinkPolarity(link.from(), link.to(), p);
                invalidateAnalysis();
                redraw();
            });
            polarityMenu.getItems().add(item);
        }
        menu.getItems().add(polarityMenu);

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            saveUndoState("Delete causal link");
            editor.removeCausalLink(link.from(), link.to());
            selectedConnection = null;
            selectedIsCausalLink = false;
            invalidateAnalysis();
            redraw();
        });
        menu.getItems().add(deleteItem);

        menu.show(this, screenX, screenY);
    }

    /**
     * Shows a context menu for an info link (dependency connection between elements).
     */
    void showInfoLinkContextMenu(ConnectionId link,
                                 double screenX, double screenY) {
        ContextMenu menu = new ContextMenu();

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            if (selectionController.deleteConnection(
                    link, false, editor,
                    () -> saveUndoState("Delete connection"))) {
                connectors = editor.generateConnectors();
                clearSelectedConnection();
                redraw();
                inputDispatcher.updateCursor(this);
            }
        });
        menu.getItems().add(deleteItem);

        menu.show(this, screenX, screenY);
    }

    /**
     * Shows a context menu on empty canvas space.
     */
    void showCanvasContextMenu(double worldX, double worldY,
                               double screenX, double screenY) {
        ContextMenu menu = new ContextMenu();

        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setOnAction(e -> pasteClipboard());
        pasteItem.setDisable(!selectionController.canPaste());

        menu.getItems().add(pasteItem);
        menu.getItems().add(new SeparatorMenuItem());

        MenuItem addStock = new MenuItem("Add Stock");
        addStock.setOnAction(e -> {
            String name = selectionController.createElementAt(
                    worldX, worldY, CanvasToolBar.Tool.PLACE_STOCK, editor, canvasState,
                    () -> saveUndoState("Add element"));
            if (name != null) {
                regenerateConnectors();
                redraw();
                fireStatusChanged();
            }
        });

        MenuItem addFlow = new MenuItem("Add Flow");
        addFlow.setOnAction(e -> switchTool(CanvasToolBar.Tool.PLACE_FLOW));

        MenuItem addAux = new MenuItem("Add Auxiliary");
        addAux.setOnAction(e -> {
            String name = selectionController.createElementAt(
                    worldX, worldY, CanvasToolBar.Tool.PLACE_AUX, editor, canvasState,
                    () -> saveUndoState("Add element"));
            if (name != null) {
                regenerateConnectors();
                redraw();
                fireStatusChanged();
            }
        });

        MenuItem addConstant = new MenuItem("Add Constant");
        addConstant.setOnAction(e -> {
            String name = selectionController.createElementAt(
                    worldX, worldY, CanvasToolBar.Tool.PLACE_CONSTANT, editor, canvasState,
                    () -> saveUndoState("Add element"));
            if (name != null) {
                regenerateConnectors();
                redraw();
                fireStatusChanged();
            }
        });

        menu.getItems().addAll(addStock, addFlow, addAux, addConstant);
        menu.getItems().add(new SeparatorMenuItem());

        MenuItem selectAllItem = new MenuItem("Select All");
        selectAllItem.setOnAction(e -> {
            selectAll();
            fireStatusChanged();
        });
        menu.getItems().add(selectAllItem);

        menu.show(this, screenX, screenY);
    }

    private void openBindingsDialog(String moduleName) {
        navController.openBindingsDialog(moduleName, editor,
                () -> saveUndoState("Edit bindings"), this::fireStatusChanged);
    }
}
