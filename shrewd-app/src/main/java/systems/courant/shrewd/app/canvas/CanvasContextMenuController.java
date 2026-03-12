package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.def.CausalLinkDef;
import systems.courant.shrewd.model.def.ElementType;

import javafx.scene.canvas.Canvas;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;


/**
 * Builds and shows right-click context menus for elements, connections,
 * and empty canvas space. Delegates module/CLD-variable menus to
 * {@link ModuleNavigationController}.
 */
final class CanvasContextMenuController {

    /**
     * Callbacks that the context menu controller uses to invoke canvas operations.
     * Avoids a direct dependency on {@link ModelCanvas}.
     */
    interface Callbacks {
        void startInlineEdit(String elementName);
        void deleteSelectedElements();
        void cutSelection();
        void copySelection();
        void pasteClipboard();
        void selectAll();
        void switchTool(CanvasToolBar.Tool tool);
        void saveUndoState(String label);
        void regenerateConnectors();
        void redraw();
        void fireStatusChanged();
        void clearSelectedConnection();
        void updateCursor();
        String createElementAt(double worldX, double worldY, CanvasToolBar.Tool tool);
        boolean deleteConnection(ConnectionId connection, boolean isCausal);
        boolean canPaste();
        void classifyCldVariable(String name, ElementType targetType);
        void drillInto(String moduleName);
        void openDefinePortsDialog(String moduleName);
        void openBindingsDialog(String moduleName);
        void traceUpstream(String elementName);
        void traceDownstream(String elementName);
        void showWhereUsed(String elementName);
        void showUses(String elementName);
    }

    private final ModuleNavigationController navController;

    CanvasContextMenuController(ModuleNavigationController navController) {
        this.navController = navController;
    }

    /**
     * Shows a context menu appropriate for the element type (module/CLD handled by
     * {@link ModuleNavigationController}, others by {@link #showGeneralElementContextMenu}).
     */
    void showElementContextMenu(Canvas canvas, String elementName,
                                CanvasState canvasState, double screenX, double screenY,
                                Callbacks callbacks) {
        navController.showContextMenu(canvas, elementName, canvasState, screenX, screenY,
                callbacks::drillInto, callbacks::openDefinePortsDialog,
                callbacks::openBindingsDialog,
                callbacks::startInlineEdit, callbacks::classifyCldVariable);
    }

    /**
     * Shows a context menu for a general element (stock, flow, variable, constant, lookup).
     */
    void showGeneralElementContextMenu(Canvas canvas, String elementName,
                                       CanvasState canvasState, double screenX, double screenY,
                                       Callbacks callbacks) {
        ContextMenu menu = new ContextMenu();

        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(e -> callbacks.startInlineEdit(elementName));

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            canvasState.clearSelection();
            canvasState.select(elementName);
            callbacks.deleteSelectedElements();
            callbacks.fireStatusChanged();
        });

        MenuItem cutItem = new MenuItem("Cut");
        cutItem.setOnAction(e -> {
            canvasState.clearSelection();
            canvasState.select(elementName);
            callbacks.cutSelection();
            callbacks.fireStatusChanged();
        });

        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setOnAction(e -> {
            canvasState.clearSelection();
            canvasState.select(elementName);
            callbacks.copySelection();
        });

        MenuItem traceUpItem = new MenuItem("Trace Upstream");
        traceUpItem.setOnAction(e -> callbacks.traceUpstream(elementName));

        MenuItem traceDownItem = new MenuItem("Trace Downstream");
        traceDownItem.setOnAction(e -> callbacks.traceDownstream(elementName));

        MenuItem whereUsedItem = new MenuItem("Where Used");
        whereUsedItem.setOnAction(e -> callbacks.showWhereUsed(elementName));

        MenuItem usesItem = new MenuItem("Uses");
        usesItem.setOnAction(e -> callbacks.showUses(elementName));

        menu.getItems().addAll(editItem, new SeparatorMenuItem(),
                traceUpItem, traceDownItem,
                whereUsedItem, usesItem, new SeparatorMenuItem(),
                cutItem, copyItem, new SeparatorMenuItem(), deleteItem);
        menu.show(canvas, screenX, screenY);
    }

    /**
     * Shows a context menu for a causal link with polarity setting and delete options.
     */
    void showCausalLinkContextMenu(Canvas canvas, ConnectionId link,
                                   ModelEditor editor, double screenX, double screenY,
                                   Callbacks callbacks) {
        ContextMenu menu = new ContextMenu();

        Menu polarityMenu = new Menu("Set Polarity");
        for (CausalLinkDef.Polarity p : CausalLinkDef.Polarity.values()) {
            MenuItem item = new MenuItem(p.symbol() + " (" + p.name().toLowerCase() + ")");
            item.setOnAction(e -> {
                callbacks.saveUndoState("Set " + link.from() + " \u2192 " + link.to() + " polarity");
                editor.setCausalLinkPolarity(link.from(), link.to(), p);
                callbacks.regenerateConnectors();
                callbacks.redraw();
            });
            polarityMenu.getItems().add(item);
        }
        menu.getItems().add(polarityMenu);

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            callbacks.saveUndoState("Delete " + link.from() + " \u2192 " + link.to() + " causal link");
            editor.removeCausalLink(link.from(), link.to());
            callbacks.clearSelectedConnection();
            callbacks.regenerateConnectors();
            callbacks.redraw();
            callbacks.fireStatusChanged();
        });
        menu.getItems().add(deleteItem);

        menu.show(canvas, screenX, screenY);
    }

    /**
     * Shows a context menu for an info link (dependency connection between elements).
     */
    void showInfoLinkContextMenu(Canvas canvas, ConnectionId link,
                                 double screenX, double screenY,
                                 Callbacks callbacks) {
        ContextMenu menu = new ContextMenu();

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            if (callbacks.deleteConnection(link, false)) {
                callbacks.regenerateConnectors();
                callbacks.clearSelectedConnection();
                callbacks.redraw();
                callbacks.updateCursor();
            }
        });
        menu.getItems().add(deleteItem);

        menu.show(canvas, screenX, screenY);
    }

    /**
     * Shows a context menu on empty canvas space with paste and element creation options.
     */
    void showCanvasContextMenu(Canvas canvas, double worldX, double worldY,
                               double screenX, double screenY,
                               Callbacks callbacks) {
        ContextMenu menu = new ContextMenu();

        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setOnAction(e -> callbacks.pasteClipboard());
        pasteItem.setDisable(!callbacks.canPaste());

        menu.getItems().add(pasteItem);
        menu.getItems().add(new SeparatorMenuItem());

        MenuItem addStock = new MenuItem("Add Stock");
        addStock.setOnAction(e -> {
            String name = callbacks.createElementAt(worldX, worldY,
                    CanvasToolBar.Tool.PLACE_STOCK);
            if (name != null) {
                callbacks.fireStatusChanged();
            }
        });

        MenuItem addFlow = new MenuItem("Add Flow");
        addFlow.setOnAction(e -> callbacks.switchTool(CanvasToolBar.Tool.PLACE_FLOW));

        MenuItem addVariable = new MenuItem("Add Variable");
        addVariable.setOnAction(e -> {
            String name = callbacks.createElementAt(worldX, worldY,
                    CanvasToolBar.Tool.PLACE_VARIABLE);
            if (name != null) {
                callbacks.fireStatusChanged();
            }
        });

        MenuItem addComment = new MenuItem("Add Comment");
        addComment.setOnAction(e -> {
            String name = callbacks.createElementAt(worldX, worldY,
                    CanvasToolBar.Tool.PLACE_COMMENT);
            if (name != null) {
                callbacks.fireStatusChanged();
            }
        });

        menu.getItems().addAll(addStock, addFlow, addVariable, new SeparatorMenuItem(), addComment);
        menu.getItems().add(new SeparatorMenuItem());

        MenuItem selectAllItem = new MenuItem("Select All");
        selectAllItem.setOnAction(e -> {
            callbacks.selectAll();
            callbacks.fireStatusChanged();
        });
        menu.getItems().add(selectAllItem);

        menu.show(canvas, screenX, screenY);
    }
}
