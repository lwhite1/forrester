package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ElementType;
import com.deathrayresearch.forrester.model.def.ModuleInstanceDef;

import javafx.scene.canvas.Canvas;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import java.util.List;
import java.util.Optional;
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
     * Shows a context menu for the given module element at the specified screen coordinates.
     */
    void showContextMenu(Canvas canvas, String elementName, CanvasState canvasState,
                         double screenX, double screenY,
                         Consumer<String> drillAction,
                         Consumer<String> bindingsAction,
                         Consumer<String> renameAction) {
        if (contextMenu != null) {
            contextMenu.hide();
        }

        ElementType type = canvasState.getType(elementName);
        if (type != ElementType.MODULE) {
            return;
        }

        contextMenu = new ContextMenu();

        MenuItem drillItem = new MenuItem("Drill Into");
        drillItem.setOnAction(e -> drillAction.accept(elementName));

        MenuItem bindingsItem = new MenuItem("Configure Bindings...");
        bindingsItem.setOnAction(e -> bindingsAction.accept(elementName));

        MenuItem renameItem = new MenuItem("Rename");
        renameItem.setOnAction(e -> renameAction.accept(elementName));

        contextMenu.getItems().addAll(drillItem, bindingsItem,
                new SeparatorMenuItem(), renameItem);
        contextMenu.show(canvas, screenX, screenY);
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
