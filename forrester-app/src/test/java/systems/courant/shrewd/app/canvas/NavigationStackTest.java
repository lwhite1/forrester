package systems.courant.forrester.app.canvas;

import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.ModelDefinitionBuilder;
import systems.courant.forrester.model.def.ViewDef;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NavigationStack")
class NavigationStackTest {

    private NavigationStack stack;

    @BeforeEach
    void setUp() {
        stack = new NavigationStack();
    }

    private NavigationStack.Frame makeFrame(String moduleName, int moduleIndex) {
        ModelEditor editor = new ModelEditor();
        editor.loadFrom(new ModelDefinitionBuilder().name("Parent").build());
        ViewDef view = new ViewDef("Main", List.of(), List.of(), List.of());
        return new NavigationStack.Frame(
                moduleName, moduleIndex, editor, view,
                0, 0, 1.0, new UndoManager(), CanvasToolBar.Tool.SELECT);
    }

    @Nested
    @DisplayName("empty stack")
    class EmptyStack {

        @Test
        void shouldBeEmptyInitially() {
            assertThat(stack.isEmpty()).isTrue();
            assertThat(stack.depth()).isZero();
        }

        @Test
        void shouldReturnNullOnPop() {
            assertThat(stack.pop()).isNull();
        }

        @Test
        void shouldReturnNullOnPeek() {
            assertThat(stack.peek()).isNull();
        }

        @Test
        void shouldReturnRootOnlyPath() {
            List<String> path = stack.getPath("Root");
            assertThat(path).containsExactly("Root");
        }
    }

    @Nested
    @DisplayName("push and pop")
    class PushAndPop {

        @Test
        void shouldPushOneFrame() {
            stack.push(makeFrame("Module A", 0));

            assertThat(stack.isEmpty()).isFalse();
            assertThat(stack.depth()).isEqualTo(1);
        }

        @Test
        void shouldPopFrame() {
            NavigationStack.Frame frame = makeFrame("Module A", 0);
            stack.push(frame);

            NavigationStack.Frame popped = stack.pop();

            assertThat(popped).isNotNull();
            assertThat(popped.moduleName()).isEqualTo("Module A");
            assertThat(stack.isEmpty()).isTrue();
        }

        @Test
        void shouldPeekWithoutRemoving() {
            stack.push(makeFrame("Module A", 0));

            NavigationStack.Frame peeked = stack.peek();

            assertThat(peeked).isNotNull();
            assertThat(peeked.moduleName()).isEqualTo("Module A");
            assertThat(stack.depth()).isEqualTo(1);
        }

        @Test
        void shouldMaintainLIFOOrder() {
            stack.push(makeFrame("Module A", 0));
            stack.push(makeFrame("Module B", 1));

            assertThat(stack.depth()).isEqualTo(2);
            assertThat(stack.pop().moduleName()).isEqualTo("Module B");
            assertThat(stack.pop().moduleName()).isEqualTo("Module A");
        }
    }

    @Nested
    @DisplayName("depth")
    class Depth {

        @Test
        void shouldTrackDepthCorrectly() {
            assertThat(stack.depth()).isZero();

            stack.push(makeFrame("A", 0));
            assertThat(stack.depth()).isEqualTo(1);

            stack.push(makeFrame("B", 1));
            assertThat(stack.depth()).isEqualTo(2);

            stack.push(makeFrame("C", 0));
            assertThat(stack.depth()).isEqualTo(3);

            stack.pop();
            assertThat(stack.depth()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("getPath")
    class GetPath {

        @Test
        void shouldReturnRootOnlyWhenEmpty() {
            List<String> path = stack.getPath("MyModel");
            assertThat(path).containsExactly("MyModel");
        }

        @Test
        void shouldReturnTwoSegmentsAtDepthOne() {
            stack.push(makeFrame("Sub Module", 0));

            List<String> path = stack.getPath("Root Model");
            assertThat(path).containsExactly("Root Model", "Sub Module");
        }

        @Test
        void shouldReturnFullPathAtDepthTwo() {
            stack.push(makeFrame("Level 1", 0));
            stack.push(makeFrame("Level 2", 0));

            List<String> path = stack.getPath("Root");
            assertThat(path).containsExactly("Root", "Level 1", "Level 2");
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        void shouldClearAllFrames() {
            stack.push(makeFrame("A", 0));
            stack.push(makeFrame("B", 1));

            stack.clear();

            assertThat(stack.isEmpty()).isTrue();
            assertThat(stack.depth()).isZero();
        }
    }

    @Nested
    @DisplayName("frames")
    class Frames {

        @Test
        void shouldReturnFramesBottomToTop() {
            stack.push(makeFrame("A", 0));
            stack.push(makeFrame("B", 1));
            stack.push(makeFrame("C", 2));

            List<NavigationStack.Frame> frames = stack.frames();

            assertThat(frames).hasSize(3);
            assertThat(frames.get(0).moduleName()).isEqualTo("A");
            assertThat(frames.get(1).moduleName()).isEqualTo("B");
            assertThat(frames.get(2).moduleName()).isEqualTo("C");
        }

        @Test
        void shouldReturnEmptyListWhenEmpty() {
            assertThat(stack.frames()).isEmpty();
        }
    }
}
