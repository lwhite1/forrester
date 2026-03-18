package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModuleInstanceDef;
import systems.courant.sd.model.def.ViewDef;
import systems.courant.sd.model.graph.AutoLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import systems.courant.sd.app.canvas.controllers.ModuleNavigationController;

/**
 * Facade encapsulating module navigation responsibilities: drill-into,
 * navigate-back, breadcrumb path, and model definition composition.
 * Extracted from {@link ModelCanvas} to isolate navigation concerns.
 */
public final class CanvasNavigationFacade {

    private final ModelCanvas canvas;
    private final ModuleNavigationController navController = new ModuleNavigationController();

    CanvasNavigationFacade(ModelCanvas canvas) {
        this.canvas = canvas;
    }

    ModuleNavigationController navController() {
        return navController;
    }

    public void setOnNavigationChanged(Runnable callback) {
        navController.setOnNavigationChanged(callback);
    }

    public ModelDefinition toModelDefinition() {
        if (canvas.editor == null) {
            throw new IllegalStateException("No model loaded");
        }
        if (!navController.isInsideModule()) {
            return canvas.editor.toModelDefinition(canvas.canvasState().toViewDef());
        }

        ModelDefinition childDef = canvas.editor.toModelDefinition(canvas.canvasState().toViewDef());

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

    public void drillInto(String moduleName) {
        if (canvas.editor == null) {
            return;
        }
        Optional<ModuleInstanceDef> moduleOpt = canvas.editor.getModuleByName(moduleName);
        if (moduleOpt.isEmpty()) {
            return;
        }
        ModuleInstanceDef module = moduleOpt.get();
        int moduleIndex = canvas.editor.getModuleIndex(moduleName);

        navController.push(new NavigationStack.Frame(
                moduleName, moduleIndex, canvas.editor, canvas.canvasState().toViewDef(),
                canvas.viewport().getTranslateX(), canvas.viewport().getTranslateY(),
                canvas.viewport().getScale(), canvas.undoManager, canvas.activeTool));

        ModelEditor moduleEditor = new ModelEditor();
        moduleEditor.loadFrom(module.definition());

        ViewDef moduleView;
        if (!module.definition().views().isEmpty()) {
            moduleView = module.definition().views().getFirst();
        } else {
            var sizeOverrides = LayoutMetrics.computeSizeOverrides(module.definition());
            moduleView = AutoLayout.layout(module.definition(), sizeOverrides);
        }

        canvas.undoManager = new UndoManager();
        canvas.setModel(moduleEditor, moduleView);
        canvas.viewport().reset();

        if (canvas.toolBar != null) {
            canvas.toolBar.resetToSelect();
        } else {
            canvas.activeTool = CanvasToolBar.Tool.SELECT;
        }

        navController.fireNavigationChanged();
        canvas.fireStatusChanged();
    }

    public void navigateBack() {
        if (!navController.isInsideModule() || canvas.editor == null) {
            return;
        }

        popNavigationLevel(true);

        canvas.connectors = canvas.editor.generateConnectors();
        canvas.invalidateAnalysis();
        canvas.requestRedraw();

        navController.fireNavigationChanged();
        canvas.fireStatusChanged();
    }

    public void navigateToDepth(int targetDepth) {
        int levelsToNavigate = navController.depth() - targetDepth;
        if (levelsToNavigate <= 0 || canvas.editor == null) {
            return;
        }

        if (levelsToNavigate == 1) {
            navigateBack();
            return;
        }

        popNavigationLevel(false);
        for (int i = 1; i < levelsToNavigate; i++) {
            popNavigationLevel(i == levelsToNavigate - 1);
        }

        canvas.connectors = canvas.editor.generateConnectors();
        canvas.invalidateAnalysis();
        canvas.requestRedraw();

        navController.fireNavigationChanged();
        canvas.fireStatusChanged();
    }

    private void popNavigationLevel(boolean saveUndo) {
        ModelDefinition childDef = canvas.editor.toModelDefinition(canvas.canvasState().toViewDef());
        NavigationStack.Frame frame = navController.pop();

        canvas.undoManager.close();
        canvas.editor = frame.editor();
        canvas.undoManager = frame.undoManager();

        if (saveUndo) {
            canvas.saveUndoState("Edit module " + frame.moduleName());
        }
        canvas.editor.updateModuleDefinition(frame.moduleIndex(), childDef);

        canvas.canvasState().loadFrom(frame.viewSnapshot());
        canvas.viewport().restoreState(frame.viewportTranslateX(),
                frame.viewportTranslateY(), frame.viewportScale());

        if (canvas.toolBar != null) {
            canvas.toolBar.selectTool(frame.activeTool());
        } else {
            canvas.activeTool = frame.activeTool();
        }
    }

    public boolean isInsideModule() {
        return navController.isInsideModule();
    }

    public List<String> getNavigationPath() {
        String rootName = navController.isInsideModule()
                ? navController.frames().getFirst().editor().getModelName()
                : canvas.editor.getModelName();
        return navController.getPath(rootName);
    }

    public String getCurrentModuleName() {
        return navController.getCurrentModuleName();
    }

    public void clearNavigation() {
        if (navController.isInsideModule()) {
            canvas.undoManager.close();
            List<NavigationStack.Frame> frames = navController.frames();
            canvas.undoManager = frames.getFirst().undoManager();
            for (int i = 1; i < frames.size(); i++) {
                frames.get(i).undoManager().close();
            }
        }
        navController.clear();
    }

    void openDefinePortsDialog(String moduleName) {
        if (canvas.editor == null) {
            return;
        }
        navController.openDefinePortsDialog(moduleName, canvas.editor,
                () -> canvas.saveUndoState("Define " + moduleName + " ports"),
                canvas::fireStatusChanged);
    }

    void openBindingsDialog(String moduleName) {
        if (canvas.editor == null) {
            return;
        }
        navController.openBindingsDialog(moduleName, canvas.editor,
                () -> canvas.saveUndoState("Edit " + moduleName + " bindings"),
                canvas::fireStatusChanged);
    }
}
