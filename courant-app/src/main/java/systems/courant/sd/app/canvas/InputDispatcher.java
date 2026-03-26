package systems.courant.sd.app.canvas;

import javafx.scene.Cursor;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import systems.courant.sd.model.def.ElementType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import systems.courant.sd.app.canvas.controllers.CausalLinkDragController;
import systems.courant.sd.app.canvas.controllers.ConnectionRerouteController;
import systems.courant.sd.app.canvas.controllers.DragController;
import systems.courant.sd.app.canvas.controllers.InlineEditController;
import systems.courant.sd.app.canvas.controllers.MarqueeController;
import systems.courant.sd.app.canvas.controllers.ReattachController;
import systems.courant.sd.app.canvas.controllers.ResizeController;

/**
 * Routes mouse, keyboard, and scroll events to the appropriate interaction
 * controllers. Manages pan state, cursor updates, and hover tracking.
 *
 * <p>Connection-tool behavior (flow, causal link, info link creation) is
 * delegated to {@link ConnectionToolHandler}. New connection tools can be
 * added there without modifying this class.</p>
 */
final class InputDispatcher {

    private static final double MIN_REATTACH_DRAG_SQ = 5 * 5;

    private static final Map<KeyCode, Consumer<ModelCanvas>> SHORTCUT_KEYS = Map.ofEntries(
            Map.entry(KeyCode.A, c -> c.elements().selectAll()),
            Map.entry(KeyCode.C, c -> c.elements().copySelection()),
            Map.entry(KeyCode.X, c -> c.elements().cutSelection()),
            Map.entry(KeyCode.V, c -> c.elements().pasteClipboard()),
            Map.entry(KeyCode.PLUS, ModelCanvas::zoomIn),
            Map.entry(KeyCode.EQUALS, ModelCanvas::zoomIn),
            Map.entry(KeyCode.ADD, ModelCanvas::zoomIn),
            Map.entry(KeyCode.MINUS, ModelCanvas::zoomOut),
            Map.entry(KeyCode.SUBTRACT, ModelCanvas::zoomOut),
            Map.entry(KeyCode.DIGIT0, ModelCanvas::resetZoom)
    );

    private static final Map<KeyCode, CanvasToolBar.Tool> DIGIT_TO_TOOL = Map.ofEntries(
            Map.entry(KeyCode.DIGIT1, CanvasToolBar.Tool.SELECT),
            Map.entry(KeyCode.DIGIT2, CanvasToolBar.Tool.PLACE_CLD_VARIABLE),
            Map.entry(KeyCode.DIGIT3, CanvasToolBar.Tool.PLACE_CAUSAL_LINK),
            Map.entry(KeyCode.DIGIT4, CanvasToolBar.Tool.PLACE_STOCK),
            Map.entry(KeyCode.DIGIT5, CanvasToolBar.Tool.PLACE_FLOW),
            Map.entry(KeyCode.DIGIT6, CanvasToolBar.Tool.PLACE_VARIABLE),
            Map.entry(KeyCode.DIGIT7, CanvasToolBar.Tool.PLACE_LOOKUP),
            Map.entry(KeyCode.DIGIT8, CanvasToolBar.Tool.PLACE_MODULE),
            Map.entry(KeyCode.DIGIT9, CanvasToolBar.Tool.PLACE_COMMENT)
    );

    private final DragController dragController;
    private final MarqueeController marqueeController;
    private final ResizeController resizeController;
    private final ReattachController reattachController;
    private final ConnectionRerouteController rerouteController;
    private final InlineEditController inlineEdit;
    private final ConnectionToolHandler connectionHandler;

    private final CausalLinkDragController causalLinkDrag = new CausalLinkDragController();
    private final PanController panController = new PanController();
    private double dragStartX;
    private double dragStartY;

    // Mouse position tracking for cursor updates
    private double lastMouseX;
    private double lastMouseY;

    // Hover highlighting
    private String hoveredElement;
    private ConnectionId hoveredConnection;

    // Pending narrow: when clicking an already-selected element in multi-selection,
    // narrow to that element on mouse-up only if no drag occurred
    private String pendingNarrowTarget;

    InputDispatcher(DragController dragController,
                    MarqueeController marqueeController,
                    ResizeController resizeController,
                    ReattachController reattachController,
                    ConnectionRerouteController rerouteController,
                    InlineEditController inlineEdit,
                    ConnectionToolHandler connectionHandler) {
        this.dragController = dragController;
        this.marqueeController = marqueeController;
        this.resizeController = resizeController;
        this.reattachController = reattachController;
        this.rerouteController = rerouteController;
        this.inlineEdit = inlineEdit;
        this.connectionHandler = connectionHandler;
    }

    String getHoveredElement() {
        return hoveredElement;
    }

    ConnectionId getHoveredConnection() {
        return hoveredConnection;
    }

    boolean isSpaceDown() {
        return panController.isSpaceDown();
    }

    boolean isPanning() {
        return panController.isPanning();
    }

    // --- Scroll ---

    void handleScroll(ScrollEvent event, Viewport viewport, Runnable redraw) {
        double factor = event.getDeltaY() > 0 ? Viewport.ZOOM_FACTOR : 1.0 / Viewport.ZOOM_FACTOR;
        viewport.zoomAt(event.getX(), event.getY(), factor);
        redraw.run();
        event.consume();
    }

    // --- Mouse exited ---

    void handleMouseExited(MouseEvent event, ModelCanvas canvas) {
        hoveredElement = null;
        hoveredConnection = null;
        canvas.updateTooltip(null, event);
        canvas.updateCloudTooltip(null, event);
        canvas.requestRedraw();
    }

    // --- Mouse moved ---

    void handleMouseMoved(MouseEvent event, ModelCanvas canvas) {
        lastMouseX = event.getX();
        lastMouseY = event.getY();

        Viewport viewport = canvas.viewport();
        double worldX = viewport.toWorldX(event.getX());
        double worldY = viewport.toWorldY(event.getY());

        // Delegate rubber-band updates to connection handler
        connectionHandler.updateRubberBands(worldX, worldY, canvas);

        // Update hovered port for info link highlight (when pending or just hovering)
        if (canvas.getActiveTool() == CanvasToolBar.Tool.PLACE_INFO_LINK) {
            canvas.getInfoLinkCreation().updateHoveredPort(
                    worldX, worldY, canvas.canvasState(), canvas.getEditor());
        }

        // Update hover highlight
        CanvasState canvasState = canvas.canvasState();
        boolean hideAux = canvas.isHideVariables();
        String hit = HitTester.hitTest(canvasState, worldX, worldY, hideAux);

        // Connection hover: only when no element is hovered
        ConnectionId connHit = null;
        if (hit == null) {
            connHit = HitTester.hitTestInfoLink(canvasState, canvas.getConnectors(),
                    worldX, worldY, hideAux);
            if (connHit == null && canvas.getEditor() != null) {
                connHit = HitTester.hitTestCausalLink(canvasState,
                        canvas.getEditor().getCausalLinks(), worldX, worldY, hideAux);
            }
        }

        boolean changed = !Objects.equals(hit, hoveredElement)
                || !Objects.equals(connHit, hoveredConnection);
        if (changed) {
            hoveredElement = hit;
            hoveredConnection = connHit;
            canvas.requestRedraw();
            canvas.updateTooltip(hit, event);
        }

        // Cloud tooltip — checked independently of element/connection hover
        // changes, since clouds don't affect hit/connHit state (#1191)
        if (hit == null && connHit == null && canvas.getEditor() != null) {
            FlowEndpointCalculator.CloudHit cloudHit =
                    FlowEndpointCalculator.hitTestClouds(
                            worldX, worldY, canvasState, canvas.getEditor());
            canvas.updateCloudTooltip(cloudHit, event);
        } else {
            canvas.updateCloudTooltip(null, event);
        }

        updateCursor(canvas);
    }

    // --- Mouse pressed ---

    void handleMousePressed(MouseEvent event, ModelCanvas canvas) {
        if (inlineEdit.isActive()) {
            return;
        }

        canvas.requestFocus();
        lastMouseX = event.getX();
        lastMouseY = event.getY();
        dragStartX = event.getX();
        dragStartY = event.getY();

        // Pan: middle-drag, right-drag, or Space+left-drag
        if (panController.shouldStartPan(event.getButton())) {
            panController.startPan(event.getX(), event.getY());
            updateCursor(canvas);
            event.consume();
            return;
        }

        if (event.getButton() == MouseButton.PRIMARY) {
            handlePrimaryPress(event, canvas);
        }
    }

    private void handlePrimaryPress(MouseEvent event, ModelCanvas canvas) {
        Viewport viewport = canvas.viewport();
        CanvasToolBar.Tool activeTool = canvas.getActiveTool();

        double worldX = viewport.toWorldX(event.getX());
        double worldY = viewport.toWorldY(event.getY());

        if (handleDoubleClick(event, canvas, activeTool, worldX, worldY)) {
            return;
        }
        if (handleSelectModeInteractions(event, canvas, activeTool, worldX, worldY)) {
            return;
        }
        if (connectionHandler.handleClick(activeTool, worldX, worldY, canvas)) {
            updateCursor(canvas);
            event.consume();
            return;
        }
        if (handlePlacementToolClick(event, canvas, activeTool, worldX, worldY)) {
            return;
        }

        handleSelectClick(worldX, worldY, event, canvas);
    }

    private boolean handleDoubleClick(MouseEvent event, ModelCanvas canvas,
                                      CanvasToolBar.Tool activeTool,
                                      double worldX, double worldY) {
        if (event.getClickCount() != 2
                || activeTool != CanvasToolBar.Tool.SELECT
                || connectionHandler.isAnyPending()) {
            return false;
        }
        CanvasState canvasState = canvas.canvasState();
        boolean hideAux = canvas.isHideVariables();
        String hit = HitTester.hitTest(canvasState, worldX, worldY, hideAux);
        if (hit == null) {
            return false;
        }
        if (canvasState.getType(hit).orElse(null) == ElementType.MODULE) {
            canvas.navigation().drillInto(hit);
        } else {
            canvas.elements().startInlineEdit(hit);
        }
        event.consume();
        return true;
    }

    private boolean handleSelectModeInteractions(MouseEvent event, ModelCanvas canvas,
                                                 CanvasToolBar.Tool activeTool,
                                                 double worldX, double worldY) {
        if (activeTool != CanvasToolBar.Tool.SELECT || connectionHandler.isAnyPending()) {
            return false;
        }
        CanvasState canvasState = canvas.canvasState();
        ModelEditor editor = canvas.getEditor();

        // Flow endpoint reattachment
        FlowEndpointCalculator.CloudHit cloudHit =
                FlowEndpointCalculator.hitTestClouds(worldX, worldY, canvasState, editor);
        if (cloudHit == null) {
            cloudHit = FlowEndpointCalculator.hitTestConnectedEndpoints(
                    worldX, worldY, canvasState, editor);
        }
        if (cloudHit != null) {
            reattachController.start(cloudHit, canvasState);
            canvas.requestRedraw();
            updateCursor(canvas);
            event.consume();
            return true;
        }

        // Resize handle
        ResizeHandle.HandleHit handleHit = ResizeHandle.hitTest(canvasState, worldX, worldY);
        if (handleHit != null) {
            resizeController.start(handleHit, canvasState);
            updateCursor(canvas);
            event.consume();
            return true;
        }

        // Connection reroute
        ConnectionId selectedConnection = canvas.getSelectedConnection();
        if (selectedConnection != null) {
            ConnectionRerouteController.RerouteHit rerouteHit =
                    ConnectionRerouteController.hitTestEndpoint(
                            selectedConnection, canvasState, canvas.getConnectors(),
                            worldX, worldY);
            if (rerouteHit != null) {
                rerouteController.prepare(rerouteHit);
                updateCursor(canvas);
                event.consume();
                return true;
            }

            // Causal link curvature handle drag
            if (canvas.isSelectedConnectionCausalLink()) {
                CausalLinkGeometry.LoopContext loopCtx = CausalLinkGeometry.loopContext(
                        canvasState, editor.getCldVariables());
                if (CausalLinkDragController.hitTestHandle(
                        worldX, worldY, selectedConnection, canvasState,
                        editor.getCausalLinks(), loopCtx)) {
                    causalLinkDrag.start(selectedConnection, canvasState,
                            editor.getCausalLinks(), loopCtx);
                    updateCursor(canvas);
                    event.consume();
                    return true;
                }
            }
        }

        return false;
    }

    private boolean handlePlacementToolClick(MouseEvent event, ModelCanvas canvas,
                                             CanvasToolBar.Tool activeTool,
                                             double worldX, double worldY) {
        if (activeTool == CanvasToolBar.Tool.SELECT
                || connectionHandler.isConnectionTool(activeTool)) {
            return false;
        }
        CanvasState canvasState = canvas.canvasState();
        boolean hideAux = canvas.isHideVariables();
        String hit = HitTester.hitTest(canvasState, worldX, worldY, hideAux);
        if (hit != null) {
            return false;
        }
        canvas.elements().createElementAt(worldX, worldY);
        updateCursor(canvas);
        event.consume();
        return true;
    }

    private void handleSelectClick(double worldX, double worldY,
                                   MouseEvent event, ModelCanvas canvas) {
        CanvasState canvasState = canvas.canvasState();
        ModelEditor editor = canvas.getEditor();
        boolean hideAux = canvas.isHideVariables();

        String hit = HitTester.hitTest(canvasState, worldX, worldY, hideAux);

        if (hit != null) {
            // Element click: clear connection selection
            canvas.clearSelectedConnection();

            if (event.isShiftDown()) {
                canvasState.toggleSelection(hit);
                pendingNarrowTarget = null;
            } else if (!canvasState.isSelected(hit)) {
                canvasState.select(hit);
                pendingNarrowTarget = null;
            } else if (canvasState.getSelection().size() > 1) {
                // Already selected in multi-selection: defer narrowing to mouse-up
                pendingNarrowTarget = hit;
            } else {
                pendingNarrowTarget = null;
            }

            // Start drag for all selected elements
            dragController.start(hit, event.getX(), event.getY(), canvasState, canvas.viewport());
        } else {
            // No element hit — check for connection click (info links then causal links)
            ConnectionId connHit = HitTester.hitTestInfoLink(
                    canvasState, canvas.getConnectors(), worldX, worldY, hideAux);
            boolean isCausal = false;
            if (connHit == null && editor != null) {
                connHit = HitTester.hitTestCausalLink(canvasState,
                        editor.getCausalLinks(), worldX, worldY, hideAux);
                isCausal = connHit != null;
            }
            if (connHit != null) {
                // Connection click: select connection, clear element selection
                canvas.setSelectedConnection(connHit, isCausal);
                canvasState.clearSelection();
            } else {
                // Empty space: clear connection selection, start marquee
                canvas.clearSelectedConnection();
                marqueeController.start(worldX, worldY, canvasState, event.isShiftDown(),
                        hideAux);
            }
        }

        canvas.requestRedraw();
        canvas.fireStatusChanged();
        updateCursor(canvas);
        event.consume();
    }

    // --- Mouse dragged ---

    void handleMouseDragged(MouseEvent event, ModelCanvas canvas) {
        hoveredElement = null;
        hoveredConnection = null;

        Viewport viewport = canvas.viewport();
        CanvasState canvasState = canvas.canvasState();

        if (marqueeController.isActive()) {
            marqueeController.drag(
                    viewport.toWorldX(event.getX()),
                    viewport.toWorldY(event.getY()), canvasState);
            canvas.requestRedraw();
            event.consume();
            return;
        }

        if (reattachController.isActive()) {
            reattachController.drag(
                    viewport.toWorldX(event.getX()),
                    viewport.toWorldY(event.getY()));
            canvas.requestRedraw();
            event.consume();
            return;
        }

        if (causalLinkDrag.isActive()) {
            CausalLinkDragController.DragResult result = causalLinkDrag.drag(
                    viewport.toWorldX(event.getX()),
                    viewport.toWorldY(event.getY()));
            ModelEditor editor = canvas.getEditor();
            if (editor != null) {
                canvas.saveUndoStateTentative("Adjust curve");
                editor.setCausalLinkStrengthAndBias(
                        causalLinkDrag.getFromName(),
                        causalLinkDrag.getToName(),
                        result.strength(), result.bias());
            }
            canvas.requestRedraw();
            event.consume();
            return;
        }

        if (rerouteController.isActive()) {
            rerouteController.drag(
                    viewport.toWorldX(event.getX()),
                    viewport.toWorldY(event.getY()));
            canvas.requestRedraw();
            event.consume();
            return;
        }

        if (resizeController.isActive()) {
            resizeController.drag(
                    viewport.toWorldX(event.getX()),
                    viewport.toWorldY(event.getY()),
                    canvasState, () -> canvas.saveUndoState("Resize " + resizeController.getTarget()));
            canvas.requestRedraw();
            event.consume();
            return;
        }

        if (panController.handleDrag(event.getX(), event.getY(), viewport)) {
            canvas.requestRedraw();
            event.consume();
            return;
        }

        if (dragController.isDragging()) {
            dragController.drag(event.getX(), event.getY(),
                    canvasState, viewport, () -> {
                        Set<String> sel = canvasState.getSelection();
                        String desc = sel.size() == 1 ? sel.iterator().next()
                                : sel.size() + " elements";
                        canvas.saveUndoState("Move " + desc);
                    });
            canvas.requestRedraw();
            event.consume();
        }
    }

    // --- Mouse released ---

    void handleMouseReleased(MouseEvent event, ModelCanvas canvas) {
        Viewport viewport = canvas.viewport();
        CanvasState canvasState = canvas.canvasState();
        ModelEditor editor = canvas.getEditor();

        if (causalLinkDrag.isActive()) {
            causalLinkDrag.cancel();
            canvas.requestRedraw();
            canvas.fireStatusChanged();
            updateCursor(canvas);
            event.consume();
            return;
        }

        if (marqueeController.isActive()) {
            marqueeController.end();
            canvas.requestRedraw();
            canvas.fireStatusChanged();
            updateCursor(canvas);
            event.consume();
            return;
        }

        if (reattachController.isActive()) {
            double dx = event.getX() - dragStartX;
            double dy = event.getY() - dragStartY;
            if (dx * dx + dy * dy < MIN_REATTACH_DRAG_SQ) {
                String flow = reattachController.flowName();
                reattachController.cancel();
                if (flow != null) {
                    canvasState.select(flow);
                }
            } else {
                String flowLabel = reattachController.flowName();
                UndoManager um = canvas.undo().getUndoManager();
                boolean reconnected = reattachController.complete(
                        viewport.toWorldX(event.getX()),
                        viewport.toWorldY(event.getY()),
                        canvasState, editor,
                        () -> canvas.saveUndoStateTentative("Reconnect " + flowLabel),
                        () -> {
                            if (um != null) {
                                um.discardLastUndo();
                            }
                        });
                if (!reconnected) {
                    if (um != null) {
                        um.discardLastUndo();
                    }
                } else {
                    if (um != null) {
                        um.confirmLastUndo();
                    }
                    canvas.scheduleRegenerateConnectors();
                }
            }
            canvas.requestRedraw();
            canvas.fireStatusChanged();
            updateCursor(canvas);
            event.consume();
            return;
        }

        if (rerouteController.isActive()) {
            boolean rerouted = rerouteController.complete(
                    viewport.toWorldX(event.getX()),
                    viewport.toWorldY(event.getY()),
                    canvasState, editor, () -> canvas.saveUndoState(
                            "Reroute " + rerouteController.getFromName()
                                    + " \u2192 " + rerouteController.getToName() + " connection"));
            if (rerouted) {
                canvas.clearSelectedConnection();
                canvas.scheduleRegenerateConnectors();
            }
            canvas.requestRedraw();
            canvas.fireStatusChanged();
            updateCursor(canvas);
            event.consume();
            return;
        }

        if (resizeController.isActive()) {
            resizeController.end();
            updateCursor(canvas);
            event.consume();
            return;
        }

        // Right-click release without drag: show context menu
        if (panController.isPanning() && !panController.hasPanMoved()
                && event.getButton() == MouseButton.SECONDARY) {
            double worldX = viewport.toWorldX(event.getX());
            double worldY = viewport.toWorldY(event.getY());
            if (handleRightClickRelease(worldX, worldY, event, canvas)) {
                event.consume();
                return;
            }
        }

        // Narrow multi-selection to single element on click-release without drag
        if (pendingNarrowTarget != null && !dragController.hasMoved()) {
            canvas.canvasState().select(pendingNarrowTarget);
            canvas.requestRedraw();
            canvas.fireStatusChanged();
        }
        pendingNarrowTarget = null;

        dragController.end();
        panController.endPan();
        updateCursor(canvas);
        event.consume();
    }

    private boolean handleRightClickRelease(double worldX, double worldY,
                                            MouseEvent event, ModelCanvas canvas) {
        CanvasState canvasState = canvas.canvasState();
        ModelEditor editor = canvas.getEditor();
        boolean hideAux = canvas.isHideVariables();
        double sx = event.getScreenX();
        double sy = event.getScreenY();

        panController.endPan();

        // 1. Element hit
        String hit = HitTester.hitTest(canvasState, worldX, worldY, hideAux);
        if (hit != null) {
            ElementType hitType = canvasState.getType(hit).orElse(null);
            if (hitType == ElementType.MODULE || hitType == ElementType.CLD_VARIABLE) {
                canvas.elements().showElementContextMenu(hit, sx, sy);
            } else {
                canvas.elements().showGeneralElementContextMenu(hit, sx, sy);
            }
            updateCursor(canvas);
            return true;
        }

        if (editor == null) {
            return false;
        }

        // 2. Causal link hit
        ConnectionId causalHit = HitTester.hitTestCausalLink(canvasState,
                editor.getCausalLinks(), worldX, worldY, hideAux);
        if (causalHit != null) {
            canvas.elements().showCausalLinkContextMenu(causalHit, sx, sy);
            updateCursor(canvas);
            return true;
        }

        // 3. Info link hit
        ConnectionId infoHit = HitTester.hitTestInfoLink(canvasState,
                canvas.getConnectors(), worldX, worldY, hideAux);
        if (infoHit != null) {
            canvas.elements().showInfoLinkContextMenu(infoHit, sx, sy);
            updateCursor(canvas);
            return true;
        }

        // 4. Empty canvas
        canvas.elements().showCanvasContextMenu(worldX, worldY, sx, sy);
        updateCursor(canvas);
        return true;
    }

    // --- Key events ---

    void handleKeyPressed(KeyEvent event, ModelCanvas canvas) {
        if (inlineEdit.isActive()) {
            return;
        }

        KeyCode code = event.getCode();

        // State-dependent keys
        if (code == KeyCode.ESCAPE) {
            handleEscape(canvas);
            event.consume();
        } else if (code == KeyCode.ENTER && connectionHandler.isConnectionTool(canvas.getActiveTool())) {
            connectionHandler.handleEnter(canvas.getActiveTool(), canvas);
            updateCursor(canvas);
            event.consume();
        } else if (code == KeyCode.TAB && connectionHandler.isConnectionTool(canvas.getActiveTool())) {
            connectionHandler.handleTab(canvas, event.isShiftDown());
            event.consume();
        } else if (code == KeyCode.SPACE) {
            panController.onSpacePressed();
            updateCursor(canvas);
            event.consume();
        } else if (code == KeyCode.DELETE || code == KeyCode.BACK_SPACE) {
            canvas.elements().deleteSelectedOrConnection();
            event.consume();
        } else if (event.isShortcutDown()) {
            Consumer<ModelCanvas> action = SHORTCUT_KEYS.get(code);
            if (action != null) {
                action.accept(canvas);
                event.consume();
            }
        } else if (!event.isShiftDown() && !event.isAltDown()) {
            CanvasToolBar.Tool tool = DIGIT_TO_TOOL.get(code);
            if (tool != null) {
                canvas.switchTool(tool);
                event.consume();
            } else if (code == KeyCode.OPEN_BRACKET) {
                if (canvas.analysis().isLoopHighlightActive()) {
                    canvas.analysis().stepLoopBack();
                    event.consume();
                }
            } else if (code == KeyCode.CLOSE_BRACKET) {
                if (canvas.analysis().isLoopHighlightActive()) {
                    canvas.analysis().stepLoopForward();
                    event.consume();
                }
            }
        }
    }

    void handleKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.SPACE) {
            panController.onSpaceReleased();
        }
    }

    private void handleEscape(ModelCanvas canvas) {
        if (canvas.analysis().isTraceActive()) {
            canvas.analysis().clearTrace();
            canvas.requestRedraw();
        } else if (resizeController.isActive()) {
            resizeController.cancel(canvas.undo()::performUndo);
            canvas.requestRedraw();
        } else if (marqueeController.isActive()) {
            marqueeController.cancel(canvas.canvasState());
            canvas.requestRedraw();
        } else if (reattachController.isActive()) {
            reattachController.cancel();
            canvas.requestRedraw();
        } else if (rerouteController.isActive()) {
            rerouteController.cancel();
            canvas.requestRedraw();
        } else if (connectionHandler.cancelPending(canvas)) {
            // handled by connectionHandler
        } else if (canvas.getActiveTool() != CanvasToolBar.Tool.SELECT) {
            canvas.resetToolToSelect();
        } else if (canvas.getSelectedConnection() != null) {
            canvas.clearSelectedConnection();
            canvas.requestRedraw();
            canvas.fireStatusChanged();
        } else if (!canvas.canvasState().getSelection().isEmpty()) {
            canvas.canvasState().clearSelection();
            canvas.requestRedraw();
            canvas.fireStatusChanged();
        } else if (canvas.navigation().isInsideModule()) {
            canvas.navigation().navigateBack();
        }
        updateCursor(canvas);
    }

    // --- Cursor ---

    void updateCursor(ModelCanvas canvas) {
        if (inlineEdit.isActive()) {
            return;
        }

        CanvasToolBar.Tool activeTool = canvas.getActiveTool();

        Cursor cursor;

        if (resizeController.isActive()) {
            cursor = ResizeController.cursorFor(resizeController.getHandle());
        } else if (reattachController.isActive() || rerouteController.isActive()
                || panController.isPanning() || dragController.isDragging()) {
            cursor = Cursor.CLOSED_HAND;
        } else if (marqueeController.isActive()) {
            cursor = Cursor.CROSSHAIR;
        } else if (panController.isSpaceDown()) {
            cursor = Cursor.MOVE;
        } else if (connectionHandler.isAnyPending()
                || activeTool != CanvasToolBar.Tool.SELECT) {
            cursor = Cursor.CROSSHAIR;
        } else if (canvas.getEditor() != null) {
            cursor = computeSelectCursor(canvas.viewport(), canvas.canvasState(),
                    canvas.getEditor(), canvas.isHideVariables());
        } else {
            cursor = Cursor.DEFAULT;
        }

        canvas.setCursor(cursor);
    }

    private Cursor computeSelectCursor(Viewport viewport, CanvasState canvasState,
                                       ModelEditor editor, boolean hideAux) {
        double worldX = viewport.toWorldX(lastMouseX);
        double worldY = viewport.toWorldY(lastMouseY);

        ResizeHandle.HandleHit handleHit = ResizeHandle.hitTest(canvasState, worldX, worldY);
        if (handleHit != null) {
            return ResizeController.cursorFor(handleHit.handle());
        }

        FlowEndpointCalculator.CloudHit cloudHit =
                FlowEndpointCalculator.hitTestClouds(worldX, worldY, canvasState, editor);
        if (cloudHit == null) {
            cloudHit = FlowEndpointCalculator.hitTestConnectedEndpoints(
                    worldX, worldY, canvasState, editor);
        }

        if (cloudHit != null) {
            return Cursor.HAND;
        }

        String hit = HitTester.hitTest(canvasState, worldX, worldY, hideAux);
        if (hit != null) {
            return Cursor.OPEN_HAND;
        } else if (hoveredConnection != null) {
            return Cursor.HAND;
        }
        return Cursor.DEFAULT;
    }
}
