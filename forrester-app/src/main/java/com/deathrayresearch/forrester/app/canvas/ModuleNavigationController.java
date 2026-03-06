package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ElementType;
import com.deathrayresearch.forrester.model.def.ModuleInstanceDef;

import javafx.scene.canvas.Canvas;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Manages module navigation (drill-in/out), the module context menu,
 * and the bindings configuration dialog. Wraps the {@link NavigationStack}
 * and provides accessor methods for the navigation state.
 */
final class ModuleNavigationController {

    private final NavigationStack navigationStack = new NavigationStack();
    private Runnable onNavigationChanged;
    private ContextMenu contextMenu;

    void setOnNavigationChanged(Runnable callback) {
        this.onNavigationChanged = callback;
    }

    void fireNavigationChanged() {
        if (onNavigationChanged != null) {
            onNavigationChanged.run();
        }
    }

    boolean isInsideModule() {
        return !navigationStack.isEmpty();
    }

    int depth() {
        return navigationStack.depth();
    }

    List<NavigationStack.Frame> frames() {
        return navigationStack.frames();
    }

    NavigationStack.Frame peek() {
        return navigationStack.peek();
    }

    void push(NavigationStack.Frame frame) {
        navigationStack.push(frame);
    }

    NavigationStack.Frame pop() {
        return navigationStack.pop();
    }

    void clear() {
        navigationStack.clear();
        fireNavigationChanged();
    }

    List<String> getPath(String rootName) {
        return navigationStack.getPath(rootName);
    }

    String getCurrentModuleName() {
        if (navigationStack.isEmpty()) {
            return null;
        }
        return navigationStack.peek().moduleName();
    }

    /**
     * Shows a context menu for the given element at the specified screen coordinates.
     * Supports MODULE elements (drill-in, bindings, rename) and CLD_VARIABLE elements
     * (classify as stock/flow/aux/constant, rename).
     */
    void showContextMenu(Canvas canvas, String elementName, CanvasState canvasState,
                         double screenX, double screenY,
                         Consumer<String> drillAction,
                         Consumer<String> bindingsAction,
                         Consumer<String> renameAction,
                         BiConsumer<String, ElementType> classifyAction) {
        if (contextMenu != null) {
            contextMenu.hide();
        }

        ElementType type = canvasState.getType(elementName);

        if (type == ElementType.MODULE) {
            contextMenu = buildModuleContextMenu(elementName, drillAction,
                    bindingsAction, renameAction);
        } else if (type == ElementType.CLD_VARIABLE) {
            contextMenu = buildCldVariableContextMenu(elementName, renameAction,
                    classifyAction);
        } else {
            return;
        }

        contextMenu.show(canvas, screenX, screenY);
    }

    private ContextMenu buildModuleContextMenu(String elementName,
            Consumer<String> drillAction, Consumer<String> bindingsAction,
            Consumer<String> renameAction) {
        ContextMenu menu = new ContextMenu();

        MenuItem drillItem = new MenuItem("Drill Into");
        drillItem.setOnAction(e -> drillAction.accept(elementName));

        MenuItem bindingsItem = new MenuItem("Configure Bindings...");
        bindingsItem.setOnAction(e -> bindingsAction.accept(elementName));

        MenuItem renameItem = new MenuItem("Rename");
        renameItem.setOnAction(e -> renameAction.accept(elementName));

        menu.getItems().addAll(drillItem, bindingsItem,
                new SeparatorMenuItem(), renameItem);
        return menu;
    }

    private ContextMenu buildCldVariableContextMenu(String elementName,
            Consumer<String> renameAction,
            BiConsumer<String, ElementType> classifyAction) {
        ContextMenu menu = new ContextMenu();

        Menu classifyMenu = new Menu("Classify as...");

        MenuItem asStock = new MenuItem("Stock");
        asStock.setOnAction(e -> classifyAction.accept(elementName, ElementType.STOCK));

        MenuItem asFlow = new MenuItem("Flow");
        asFlow.setOnAction(e -> classifyAction.accept(elementName, ElementType.FLOW));

        MenuItem asAux = new MenuItem("Auxiliary");
        asAux.setOnAction(e -> classifyAction.accept(elementName, ElementType.AUX));

        MenuItem asConstant = new MenuItem("Constant");
        asConstant.setOnAction(e -> classifyAction.accept(elementName, ElementType.CONSTANT));

        classifyMenu.getItems().addAll(asStock, asFlow, asAux, asConstant);

        MenuItem renameItem = new MenuItem("Rename");
        renameItem.setOnAction(e -> renameAction.accept(elementName));

        menu.getItems().addAll(classifyMenu, new SeparatorMenuItem(), renameItem);
        return menu;
    }

    /**
     * Opens the bindings configuration dialog for the named module.
     */
    void openBindingsDialog(String moduleName, ModelEditor editor,
                            Runnable saveUndo, Runnable fireStatus) {
        ModuleInstanceDef module = editor.getModuleByName(moduleName);
        if (module == null) {
            return;
        }

        BindingConfigDialog dialog = new BindingConfigDialog(module);
        Optional<BindingConfigDialog.BindingResult> result = dialog.showAndWait();
        result.ifPresent(bindings -> {
            saveUndo.run();
            editor.updateModuleBindings(moduleName,
                    bindings.inputBindings(), bindings.outputBindings());
            fireStatus.run();
        });
    }
}
