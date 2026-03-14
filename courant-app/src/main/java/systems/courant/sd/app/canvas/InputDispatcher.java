package systems.courant.sd.app.canvas;

import javafx.scene.Cursor;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import systems.courant.sd.model.def.ElementType;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Routes mouse, keyboard, and scroll events to the appropriate interaction
 * controllers. Manages pan state, cursor updates, and hover tracking.
 */
final class InputDispatcher {

    private static final double ZOOM_FACTOR = 1.1;
    private static final double MIN_REATTACH_DRAG_SQ = 5 * 5;

    private final DragController dragController;
    private final MarqueeController marqueeController;
    private final ResizeController resizeController;
    private final ReattachController reattachController;
    private final FlowCreationController flowCreation;
    private final CausalLinkCreationController causalLinkCreation;
    private final InfoLinkCreationController infoLinkCreation;
    private final ConnectionRerouteController rerouteController;
    private final InlineEditController inlineEdit;

    // Pan state
    private boolean panning;
    private boolean panMoved;
    private boolean spaceDown;
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
                    FlowCreationController flowCreation,
                    CausalLinkCreationController causalLinkCreation,
                    InfoLinkCreationController infoLinkCreation,
                    ConnectionRerouteController rerouteController,
                    InlineEditController inlineEdit) {
        this.dragController = dragController;
        this.marqueeController = marqueeController;
        this.resizeController = resizeController;
        this.reattachController = reattachController;
        this.flowCreation = flowCreation;
        this.causalLinkCreation = causalLinkCreation;
        this.infoLinkCreation = infoLinkCreation;
        this.rerouteController = rerouteController;
        this.inlineEdit = inlineEdit;
    }

    String getHoveredElement() {
        return hoveredElement;
    }

    ConnectionId getHoveredConnection() {
        return hoveredConnection;
    }

    boolean isSpaceDown() {
        return spaceDown;
    }

    boolean isPanning() {
        return panning;
    }

    // --- Scroll ---

    void handleScroll(ScrollEvent event, Viewport viewport, Runnable redraw) {
        double factor = event.getDeltaY() > 0 ? ZOOM_FACTOR : 1.0 / ZOOM_FACTOR;
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
        CanvasState canvasState = canvas.canvasState();

        if (flowCreation.isPending()) {
            flowCreation.updateRubberBand(
                    viewport.toWorldX(event.getX()),
                    viewport.toWorldY(event.getY()));
            canvas.requestRedraw();
            event.consume();
        }

        if (causalLinkCreation.isPending()) {
            causalLinkCreation.updateRubberBand(
                    viewport.toWorldX(event.getX()),
                    viewport.toWorldY(event.getY()));
            canvas.requestRedraw();
            event.consume();
        }

        if (infoLinkCreation.isPending()) {
            double wx = viewport.toWorldX(event.getX());
            double wy = viewport.toWorldY(event.getY());
            infoLinkCreation.updateRubberBand(wx, wy);
            infoLinkCreation.updateHoveredPort(wx, wy, canvasState, canvas.getEditor());
            canvas.requestRedraw();
            event.consume();
        }

        // Update hovered port for highlight even when not pending
        if (canvas.getActiveTool() == CanvasToolBar.Tool.PLACE_INFO_LINK
                && !infoLinkCreation.isPending()) {
            double wx = viewport.toWorldX(event.getX());
            double wy = viewport.toWorldY(event.getY());
            infoLinkCreation.updateHoveredPort(wx, wy, canvasState, canvas.getEditor());
        }

        // Update hover highlight
        double worldX = viewport.toWorldX(event.getX());
        double worldY = viewport.toWorldY(event.getY());
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

            // Cloud tooltip when no element is hovered
            if (hit == null && canvas.getEditor() != null) {
                FlowEndpointCalculator.CloudHit cloudHit =
                        FlowEndpointCalculator.hitTestClouds(
                                worldX, worldY, canvasState, canvas.getEditor());
                canvas.updateCloudTooltip(cloudHit, event);
            }
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
        if (event.getButton() == MouseButton.MIDDLE
                || event.getButton() == MouseButton.SECONDARY
                || (event.getButton() == MouseButton.PRIMARY && spaceDown)) {
            panning = true;
            panMoved = false;
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
        CanvasState canvasState = canvas.canvasState();
        ModelEditor editor = canvas.getEditor();
        CanvasToolBar.Tool activeTool = canvas.getActiveTool();
        boolean hideAux = canvas.isHideVariables();

        double worldX = viewport.toWorldX(event.getX());
        double worldY = viewport.toWorldY(event.getY());

        // Double-click: drill into module or start inline editing
        if (event.getClickCount() == 2
                && activeTool == CanvasToolBar.Tool.SELECT
                && !flowCreation.isPending()) {
            String hit = HitTester.hitTest(canvasState, worldX, worldY, hideAux);
            if (hit != null) {
                if (canvasState.getType(hit).orElse(null) == ElementType.MODULE) {
                    canvas.drillInto(hit);
                } else {
                    canvas.startInlineEdit(hit);
                }
                event.consume();
                return;
            }
        }

        // Flow endpoint reattachment: check cloud + connected endpoints (SELECT mode only)
        if (activeTool == CanvasToolBar.Tool.SELECT && !flowCreation.isPending()) {
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
                return;
            }
        }

        // Resize handle check: takes priority over move drag
        if (activeTool == CanvasToolBar.Tool.SELECT && !flowCreation.isPending()) {
            ResizeHandle.HandleHit handleHit = ResizeHandle.hitTest(canvasState, worldX, worldY);
            if (handleHit != null) {
                resizeController.start(handleHit, canvasState);
                updateCursor(canvas);
                event.consume();
                return;
            }
        }

        // Connection reroute: clicking near an endpoint of the selected connection
        ConnectionId selectedConnection = canvas.getSelectedConnection();
        if (activeTool == CanvasToolBar.Tool.SELECT && !flowCreation.isPending()
                && selectedConnection != null) {
            ConnectionRerouteController.RerouteHit rerouteHit =
                    ConnectionRerouteController.hitTestEndpoint(
                            selectedConnection, canvasState, canvas.getConnectors(),
                            worldX, worldY);
            if (rerouteHit != null) {
                rerouteController.prepare(rerouteHit);
                updateCursor(canvas);
                event.consume();
                return;
            }
        }

        // PLACE_FLOW: two-click protocol
        if (activeTool == CanvasToolBar.Tool.PLACE_FLOW) {
            canvas.handleFlowClick(worldX, worldY);
            updateCursor(canvas);
            event.consume();
            return;
        }

        // PLACE_CAUSAL_LINK: two-click protocol
        if (activeTool == CanvasToolBar.Tool.PLACE_CAUSAL_LINK) {
            canvas.handleCausalLinkClick(worldX, worldY);
            updateCursor(canvas);
            event.consume();
            return;
        }

        // PLACE_INFO_LINK: two-click protocol
        if (activeTool == CanvasToolBar.Tool.PLACE_INFO_LINK) {
            canvas.handleInfoLinkClick(worldX, worldY);
            updateCursor(canvas);
            event.consume();
            return;
        }

        // Placement mode: other PLACE_* tools — click on empty space to create
        if (activeTool != CanvasToolBar.Tool.SELECT) {
            String hit = HitTester.hitTest(canvasState, worldX, worldY, hideAux);
            if (hit == null) {
                canvas.createElementAt(worldX, worldY);
                updateCursor(canvas);
                event.consume();
                return;
            }
        }

        // Select mode
        handleSelectClick(worldX, worldY, event, canvas);
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
            dragController.start(hit, event.getX(), event.getY(), canvasState);
        } else {
            // No element hit — check for connection click (info links then causal links)
            ConnectionId connHit = HitTester.hitTestInfoLink(
                    canvasState, canvas.getConnectors(), worldX, worldY, hideAux);
            boolean isCausal = false;
            if (connHit == null) {
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

        if (panning) {
            panMoved = true;
            double screenDx = event.getX() - dragStartX;
            double screenDy = event.getY() - dragStartY;
            viewport.pan(screenDx, screenDy);
            dragStartX = event.getX();
            dragStartY = event.getY();
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
                reattachController.complete(
                        viewport.toWorldX(event.getX()),
                        viewport.toWorldY(event.getY()),
                        canvasState, editor, () -> canvas.saveUndoState(
                                "Reconnect " + reattachController.flowName()));
                canvas.scheduleRegenerateConnectors();
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
        if (panning && !panMoved && event.getButton() == MouseButton.SECONDARY) {
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
        panning = false;
        panMoved = false;
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

        panning = false;
        panMoved = false;

        // 1. Element hit
        String hit = HitTester.hitTest(canvasState, worldX, worldY, hideAux);
        if (hit != null) {
            ElementType hitType = canvasState.getType(hit).orElse(null);
            if (hitType == ElementType.MODULE || hitType == ElementType.CLD_VARIABLE) {
                canvas.showElementContextMenu(hit, sx, sy);
            } else {
                canvas.showGeneralElementContextMenu(hit, sx, sy);
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
            canvas.showCausalLinkContextMenu(causalHit, sx, sy);
            updateCursor(canvas);
            return true;
        }

        // 3. Info link hit
        ConnectionId infoHit = HitTester.hitTestInfoLink(canvasState,
                canvas.getConnectors(), worldX, worldY, hideAux);
        if (infoHit != null) {
            canvas.showInfoLinkContextMenu(infoHit, sx, sy);
            updateCursor(canvas);
            return true;
        }

        // 4. Empty canvas
        canvas.showCanvasContextMenu(worldX, worldY, sx, sy);
        updateCursor(canvas);
        return true;
    }

    // --- Key events ---

    void handleKeyPressed(KeyEvent event, ModelCanvas canvas) {
        if (inlineEdit.isActive()) {
            return;
        }

        if (event.getCode() == KeyCode.ESCAPE) {
            handleEscape(canvas);
            event.consume();
        } else if (event.getCode() == KeyCode.ENTER && isConnectionTool(canvas.getActiveTool())) {
            handleConnectionEnter(canvas);
            event.consume();
        } else if (event.getCode() == KeyCode.TAB && isConnectionTool(canvas.getActiveTool())) {
            handleConnectionTab(canvas, event.isShiftDown());
            event.consume();
        } else if (event.getCode() == KeyCode.SPACE) {
            spaceDown = true;
            updateCursor(canvas);
            event.consume();
        } else if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
            canvas.deleteSelectedOrConnection();
            event.consume();
        } else if (event.isShortcutDown() && event.getCode() == KeyCode.A) {
            canvas.selectAll();
            event.consume();
        } else if (event.isShortcutDown() && event.getCode() == KeyCode.C) {
            canvas.copySelection();
            event.consume();
        } else if (event.isShortcutDown() && event.getCode() == KeyCode.X) {
            canvas.cutSelection();
            event.consume();
        } else if (event.isShortcutDown() && event.getCode() == KeyCode.V) {
            canvas.pasteClipboard();
            event.consume();
        } else if (event.isShortcutDown()
                && (event.getCode() == KeyCode.PLUS || event.getCode() == KeyCode.EQUALS
                        || event.getCode() == KeyCode.ADD)) {
            canvas.zoomIn();
            event.consume();
        } else if (event.isShortcutDown()
                && (event.getCode() == KeyCode.MINUS || event.getCode() == KeyCode.SUBTRACT)) {
            canvas.zoomOut();
            event.consume();
        } else if (event.isShortcutDown() && event.getCode() == KeyCode.DIGIT0) {
            canvas.resetZoom();
            event.consume();
        } else if (!event.isShortcutDown() && !event.isShiftDown() && !event.isAltDown()) {
            switch (event.getCode()) {
                case DIGIT1 -> { canvas.switchTool(CanvasToolBar.Tool.SELECT); event.consume(); }
                case DIGIT2 -> { canvas.switchTool(CanvasToolBar.Tool.PLACE_STOCK); event.consume(); }
                case DIGIT3 -> { canvas.switchTool(CanvasToolBar.Tool.PLACE_FLOW); event.consume(); }
                case DIGIT4 -> { canvas.switchTool(CanvasToolBar.Tool.PLACE_VARIABLE); event.consume(); }
                case DIGIT5 -> { canvas.switchTool(CanvasToolBar.Tool.PLACE_MODULE); event.consume(); }
                case DIGIT6 -> { canvas.switchTool(CanvasToolBar.Tool.PLACE_LOOKUP); event.consume(); }
                case DIGIT7 -> { canvas.switchTool(CanvasToolBar.Tool.PLACE_CLD_VARIABLE); event.consume(); }
                case DIGIT8 -> { canvas.switchTool(CanvasToolBar.Tool.PLACE_CAUSAL_LINK); event.consume(); }
                case DIGIT9 -> { canvas.switchTool(CanvasToolBar.Tool.PLACE_COMMENT); event.consume(); }
                case DIGIT0 -> { canvas.switchTool(CanvasToolBar.Tool.PLACE_INFO_LINK); event.consume(); }
                case OPEN_BRACKET -> {
                    if (canvas.isLoopHighlightActive()) {
                        canvas.stepLoopBack();
                        event.consume();
                    }
                }
                case CLOSE_BRACKET -> {
                    if (canvas.isLoopHighlightActive()) {
                        canvas.stepLoopForward();
                        event.consume();
                    }
                }
                default -> { }
            }
        }
    }

    void handleKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.SPACE) {
            spaceDown = false;
        }
    }

    private static boolean isConnectionTool(CanvasToolBar.Tool tool) {
        return tool == CanvasToolBar.Tool.PLACE_FLOW
                || tool == CanvasToolBar.Tool.PLACE_CAUSAL_LINK
                || tool == CanvasToolBar.Tool.PLACE_INFO_LINK;
    }

    /**
     * Performs a connection click at the selected element's center, enabling
     * fully keyboard-driven connection creation (Enter to confirm source/target).
     */
    private void handleConnectionEnter(ModelCanvas canvas) {
        CanvasState canvasState = canvas.canvasState();
        Set<String> selection = canvasState.getSelection();

        if (selection.isEmpty()) {
            return;
        }

        String selected = selection.iterator().next();
        double worldX = canvasState.getX(selected);
        double worldY = canvasState.getY(selected);

        if (Double.isNaN(worldX) || Double.isNaN(worldY)) {
            return;
        }

        CanvasToolBar.Tool activeTool = canvas.getActiveTool();
        if (activeTool == CanvasToolBar.Tool.PLACE_FLOW) {
            canvas.handleFlowClick(worldX, worldY);
        } else if (activeTool == CanvasToolBar.Tool.PLACE_CAUSAL_LINK) {
            canvas.handleCausalLinkClick(worldX, worldY);
        } else if (activeTool == CanvasToolBar.Tool.PLACE_INFO_LINK) {
            canvas.handleInfoLinkClick(worldX, worldY);
        }
        updateCursor(canvas);
    }

    /**
     * Cycles element selection forward (Tab) or backward (Shift+Tab),
     * enabling keyboard navigation to choose connection endpoints.
     */
    private void handleConnectionTab(ModelCanvas canvas, boolean reverse) {
        CanvasState canvasState = canvas.canvasState();
        List<String> drawOrder = canvasState.getDrawOrder();
        if (drawOrder.isEmpty()) {
            return;
        }

        Set<String> selection = canvasState.getSelection();
        String current = selection.isEmpty() ? null : selection.iterator().next();

        int currentIndex = current != null ? drawOrder.indexOf(current) : -1;
        int nextIndex;
        if (reverse) {
            nextIndex = currentIndex <= 0 ? drawOrder.size() - 1 : currentIndex - 1;
        } else {
            nextIndex = currentIndex < 0 || currentIndex >= drawOrder.size() - 1
                    ? 0 : currentIndex + 1;
        }

        String next = drawOrder.get(nextIndex);
        canvasState.select(next);

        // Update rubber-band to point at the newly selected element
        double worldX = canvasState.getX(next);
        double worldY = canvasState.getY(next);
        if (!Double.isNaN(worldX) && !Double.isNaN(worldY)) {
            flowCreation.updateRubberBand(worldX, worldY);
            causalLinkCreation.updateRubberBand(worldX, worldY);
            infoLinkCreation.updateRubberBand(worldX, worldY);
        }

        canvas.requestRedraw();
        canvas.fireStatusChanged();
    }

    private void handleEscape(ModelCanvas canvas) {
        if (canvas.isTraceActive()) {
            canvas.clearTrace();
            canvas.requestRedraw();
        } else if (resizeController.isActive()) {
            resizeController.cancel(canvas::performUndo);
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
        } else if (flowCreation.isPending()) {
            flowCreation.cancel();
            canvas.requestRedraw();
        } else if (causalLinkCreation.isPending()) {
            causalLinkCreation.cancel();
            canvas.requestRedraw();
        } else if (infoLinkCreation.isPending()) {
            infoLinkCreation.cancel();
            canvas.requestRedraw();
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
        } else if (canvas.isInsideModule()) {
            canvas.navigateBack();
        }
        updateCursor(canvas);
    }

    // --- Cursor ---

    void updateCursor(ModelCanvas canvas) {
        if (inlineEdit.isActive()) {
            return;
        }

        Viewport viewport = canvas.viewport();
        CanvasState canvasState = canvas.canvasState();
        ModelEditor editor = canvas.getEditor();
        CanvasToolBar.Tool activeTool = canvas.getActiveTool();

        Cursor cursor;

        if (resizeController.isActive()) {
            cursor = ResizeController.cursorFor(resizeController.getHandle());
        } else if (reattachController.isActive() || rerouteController.isActive()
                || panning || dragController.isDragging()) {
            cursor = Cursor.CLOSED_HAND;
        } else if (marqueeController.isActive()) {
            cursor = Cursor.CROSSHAIR;
        } else if (spaceDown) {
            cursor = Cursor.MOVE;
        } else if (flowCreation.isPending() || infoLinkCreation.isPending()
                || activeTool != CanvasToolBar.Tool.SELECT) {
            cursor = Cursor.CROSSHAIR;
        } else if (editor != null) {
            cursor = computeSelectCursor(viewport, canvasState, editor, canvas.isHideVariables());
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
