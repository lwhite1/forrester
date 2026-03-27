package systems.courant.sd.app.canvas.controllers;

import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.ElementType;

import javafx.scene.canvas.Canvas;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import systems.courant.sd.app.canvas.CanvasState;
import systems.courant.sd.app.canvas.CanvasToolBar;
import systems.courant.sd.app.canvas.ConnectionId;
import systems.courant.sd.app.canvas.ModelCanvas;
import systems.courant.sd.app.canvas.ModelEditor;


/**
 * Builds and shows right-click context menus for elements, connections,
 * and empty canvas space. Delegates module/CLD-variable menus to
 * {@link ModuleNavigationController}.
 */
public final class CanvasContextMenuController {

    /**
     * Callbacks that the context menu controller uses to invoke canvas operations.
     * Avoids a direct dependency on {@link ModelCanvas}.
     */
    public interface Callbacks {
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
        void convertVariableToComment(String variableName);
        void setElementColor(String elementName, String hexColor);
        void setCausalLinkColor(ConnectionId link, String hexColor);
    }

    private final ModuleNavigationController navController;

    public CanvasContextMenuController(ModuleNavigationController navController) {
        this.navController = navController;
    }

    /**
     * Shows a context menu appropriate for the element type (module/CLD handled by
     * {@link ModuleNavigationController}, others by {@link #showGeneralElementContextMenu}).
     */
    public void showElementContextMenu(Canvas canvas, String elementName,
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
    public void showGeneralElementContextMenu(Canvas canvas, String elementName,
                                       CanvasState canvasState, double screenX, double screenY,
                                       Callbacks callbacks) {
        ContextMenu menu = new ContextMenu();
        ElementType type = canvasState.getType(elementName).orElse(null);

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

        menu.getItems().add(editItem);

        if (type != ElementType.COMMENT) {
            MenuItem traceUpItem = new MenuItem("Trace Upstream");
            traceUpItem.setOnAction(e -> callbacks.traceUpstream(elementName));

            MenuItem traceDownItem = new MenuItem("Trace Downstream");
            traceDownItem.setOnAction(e -> callbacks.traceDownstream(elementName));

            MenuItem whereUsedItem = new MenuItem("Where Used");
            whereUsedItem.setOnAction(e -> callbacks.showWhereUsed(elementName));

            MenuItem usesItem = new MenuItem("Uses");
            usesItem.setOnAction(e -> callbacks.showUses(elementName));

            menu.getItems().addAll(new SeparatorMenuItem(),
                    traceUpItem, traceDownItem, whereUsedItem, usesItem);
        }

        if (type == ElementType.AUX) {
            MenuItem convertItem = new MenuItem("Convert to Comment");
            convertItem.setOnAction(e -> {
                callbacks.saveUndoState("Convert to comment");
                callbacks.convertVariableToComment(elementName);
                callbacks.redraw();
                callbacks.fireStatusChanged();
            });
            menu.getItems().addAll(new SeparatorMenuItem(), convertItem);
        }

        // Color submenu
        String currentHex = canvasState.getColor(elementName).orElse(null);
        Menu colorMenu = buildColorMenu(currentHex, hex -> {
            callbacks.saveUndoState("Change color");
            callbacks.setElementColor(elementName, hex);
            callbacks.redraw();
            callbacks.fireStatusChanged();
        });
        menu.getItems().addAll(new SeparatorMenuItem(), colorMenu);

        menu.getItems().addAll(new SeparatorMenuItem(),
                cutItem, copyItem, new SeparatorMenuItem(), deleteItem);
        menu.show(canvas, screenX, screenY);
    }

    /**
     * Shows a context menu for a causal link with polarity setting and delete options.
     */
    public void showCausalLinkContextMenu(Canvas canvas, ConnectionId link,
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

        // Color submenu
        String currentLinkColor = editor.getCausalLinks().stream()
                .filter(cl -> cl.from().equals(link.from()) && cl.to().equals(link.to()))
                .findFirst().map(cl -> cl.hasColor() ? cl.color() : null).orElse(null);
        Menu colorMenu = buildColorMenu(currentLinkColor, hex -> {
            callbacks.saveUndoState("Change link color");
            callbacks.setCausalLinkColor(link, hex);
            callbacks.redraw();
        });
        menu.getItems().add(colorMenu);

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
    public void showInfoLinkContextMenu(Canvas canvas, ConnectionId link,
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

    // ── Color menu helpers ─────────────────────────────────────────────

    private static final String[][] COLOR_SWATCHES = {
            {"Black",  "#2C3E50"},
            {"Red",    "#E74C3C"},
            {"Blue",   "#2980B9"},
            {"Green",  "#27AE60"},
            {"Orange", "#E67E22"},
            {"Purple", "#8E44AD"},
    };

    /**
     * Builds a "Color" submenu with preset swatches, a "Custom..." option, and "Default".
     *
     * @param currentHex the currently applied hex color, or null for default
     * @param onColor    callback receiving the chosen hex string, or null for "Default"
     */
    private static Menu buildColorMenu(String currentHex, java.util.function.Consumer<String> onColor) {
        Menu menu = new Menu("Color");
        for (String[] swatch : COLOR_SWATCHES) {
            MenuItem item = new MenuItem(swatch[0]);
            Rectangle rect = new Rectangle(12, 12, Color.web(swatch[1]));
            item.setGraphic(rect);
            String hex = swatch[1];
            item.setOnAction(e -> onColor.accept(hex));
            menu.getItems().add(item);
        }
        menu.getItems().add(new SeparatorMenuItem());

        MenuItem customItem = new MenuItem("Custom\u2026");
        customItem.setOnAction(e -> {
            Color initial = currentHex != null ? Color.web(currentHex) : Color.web("#2C3E50");
            Dialog<Color> dialog = new Dialog<>();
            dialog.setTitle("Choose Color");
            dialog.setHeaderText(null);
            ColorPicker picker = new ColorPicker(initial);
            dialog.getDialogPane().setContent(picker);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.setResultConverter(btn -> btn == ButtonType.OK ? picker.getValue() : null);
            dialog.showAndWait().ifPresent(color -> {
                String hex = String.format("#%02X%02X%02X",
                        (int) (color.getRed() * 255),
                        (int) (color.getGreen() * 255),
                        (int) (color.getBlue() * 255));
                onColor.accept(hex);
            });
        });
        menu.getItems().add(customItem);

        menu.getItems().add(new SeparatorMenuItem());
        MenuItem defaultItem = new MenuItem("Default");
        defaultItem.setDisable(currentHex == null);
        defaultItem.setOnAction(e -> onColor.accept(null));
        menu.getItems().add(defaultItem);

        return menu;
    }

    /**
     * Shows a context menu on empty canvas space with paste and element creation options.
     */
    public void showCanvasContextMenu(Canvas canvas, double worldX, double worldY,
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
