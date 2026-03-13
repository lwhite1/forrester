package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ViewDef;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Maintains a stack of navigation frames for module drill-down.
 * Each frame captures the state of the parent level so it can be restored
 * when navigating back.
 */
public class NavigationStack {

    /**
     * A snapshot of one navigation level's state.
     *
     * @param moduleName the name of the module that was drilled into
     * @param moduleIndex the index of that module in the parent editor's module list
     * @param editor the parent level's ModelEditor
     * @param viewSnapshot the parent level's ViewDef at the time of drill-in
     * @param viewportTranslateX the parent viewport X translation
     * @param viewportTranslateY the parent viewport Y translation
     * @param viewportScale the parent viewport scale
     * @param undoManager the parent level's UndoManager
     * @param activeTool the active tool at the time of drill-in
     */
    public record Frame(
            String moduleName,
            int moduleIndex,
            ModelEditor editor,
            ViewDef viewSnapshot,
            double viewportTranslateX,
            double viewportTranslateY,
            double viewportScale,
            UndoManager undoManager,
            CanvasToolBar.Tool activeTool
    ) {}

    private final Deque<Frame> stack = new ArrayDeque<>();

    /**
     * Pushes a frame onto the navigation stack.
     */
    public void push(Frame frame) {
        stack.push(frame);
    }

    /**
     * Pops and returns the top frame, or null if empty.
     */
    public Frame pop() {
        return stack.isEmpty() ? null : stack.pop();
    }

    /**
     * Returns the top frame without removing it, or null if empty.
     */
    public Frame peek() {
        return stack.peek();
    }

    /**
     * Returns true if the stack is empty (at root level).
     */
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    /**
     * Returns the current navigation depth (0 = root).
     */
    public int depth() {
        return stack.size();
    }

    /**
     * Builds the breadcrumb path from root to the current level.
     *
     * @param rootName the name to display for the root level
     * @return ordered list of path segments from root to current
     */
    public List<String> getPath(String rootName) {
        List<String> path = new ArrayList<>();
        path.add(rootName);

        List<Frame> frames = new ArrayList<>(stack);
        Collections.reverse(frames);
        for (Frame frame : frames) {
            path.add(frame.moduleName());
        }

        return path;
    }

    /**
     * Clears the entire navigation stack.
     */
    public void clear() {
        stack.clear();
    }

    /**
     * Returns an unmodifiable view of the frames from bottom to top.
     */
    public List<Frame> frames() {
        List<Frame> list = new ArrayList<>(stack);
        Collections.reverse(list);
        return Collections.unmodifiableList(list);
    }
}
