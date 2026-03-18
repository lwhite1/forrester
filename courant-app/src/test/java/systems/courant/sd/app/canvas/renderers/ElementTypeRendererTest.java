package systems.courant.sd.app.canvas.renderers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import systems.courant.sd.model.def.ElementType;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ElementTypeRenderer strategy pattern (#509)")
class ElementTypeRendererTest {

    @Nested
    @DisplayName("ELEMENT_RENDERERS map")
    class RendererMap {

        @SuppressWarnings("unchecked")
        private Map<ElementType, ElementTypeRenderer> getRendererMap() throws Exception {
            Field field = ElementPass.class.getDeclaredField("ELEMENT_RENDERERS");
            field.setAccessible(true);
            return (Map<ElementType, ElementTypeRenderer>) field.get(null);
        }

        @Test
        @DisplayName("should have a renderer for every element type")
        void shouldCoverAllElementTypes() throws Exception {
            Map<ElementType, ElementTypeRenderer> map = getRendererMap();
            for (ElementType type : ElementType.values()) {
                assertThat(map).as("Missing renderer for " + type).containsKey(type);
            }
        }

        @Test
        @DisplayName("should map STOCK to StockRenderer")
        void shouldMapStock() throws Exception {
            assertThat(getRendererMap().get(ElementType.STOCK)).isInstanceOf(StockRenderer.class);
        }

        @Test
        @DisplayName("should map FLOW to FlowRenderer")
        void shouldMapFlow() throws Exception {
            assertThat(getRendererMap().get(ElementType.FLOW)).isInstanceOf(FlowRenderer.class);
        }

        @Test
        @DisplayName("should map AUX to AuxRenderer")
        void shouldMapAux() throws Exception {
            assertThat(getRendererMap().get(ElementType.AUX)).isInstanceOf(AuxRenderer.class);
        }

        @Test
        @DisplayName("should map MODULE to ModuleRenderer")
        void shouldMapModule() throws Exception {
            assertThat(getRendererMap().get(ElementType.MODULE)).isInstanceOf(ModuleRenderer.class);
        }

        @Test
        @DisplayName("should map LOOKUP to LookupRenderer")
        void shouldMapLookup() throws Exception {
            assertThat(getRendererMap().get(ElementType.LOOKUP)).isInstanceOf(LookupRenderer.class);
        }

        @Test
        @DisplayName("should map CLD_VARIABLE to CldVariableRenderer")
        void shouldMapCldVariable() throws Exception {
            assertThat(getRendererMap().get(ElementType.CLD_VARIABLE))
                    .isInstanceOf(CldVariableRenderer.class);
        }

        @Test
        @DisplayName("should map COMMENT to CommentRenderer")
        void shouldMapComment() throws Exception {
            assertThat(getRendererMap().get(ElementType.COMMENT))
                    .isInstanceOf(CommentRenderer.class);
        }
    }

    @Nested
    @DisplayName("Strategy interface")
    class StrategyInterface {

        @Test
        @DisplayName("ElementTypeRenderer should be a public interface")
        void shouldBePublicInterface() {
            assertThat(ElementTypeRenderer.class.isInterface()).isTrue();
            assertThat(java.lang.reflect.Modifier.isPublic(
                    ElementTypeRenderer.class.getModifiers())).isTrue();
        }

        @Test
        @DisplayName("render method should have correct parameter types")
        void shouldHaveRenderMethod() throws NoSuchMethodException {
            var method = ElementTypeRenderer.class.getDeclaredMethod("render",
                    javafx.scene.canvas.GraphicsContext.class,
                    String.class, double.class, double.class,
                    systems.courant.sd.app.canvas.CanvasState.class,
                    systems.courant.sd.app.canvas.ModelEditor.class,
                    boolean.class);
            assertThat(method.getReturnType()).isEqualTo(void.class);
        }
    }

    @Nested
    @DisplayName("Strategy implementations")
    class Implementations {

        @Test
        @DisplayName("all implementations should be package-private")
        void shouldBePackagePrivate() {
            Class<?>[] renderers = {
                    StockRenderer.class, FlowRenderer.class, AuxRenderer.class,
                    ModuleRenderer.class, LookupRenderer.class,
                    CldVariableRenderer.class, CommentRenderer.class
            };
            for (Class<?> cls : renderers) {
                int mods = cls.getModifiers();
                assertThat(java.lang.reflect.Modifier.isPublic(mods))
                        .as(cls.getSimpleName() + " should not be public").isFalse();
                assertThat(java.lang.reflect.Modifier.isPrivate(mods))
                        .as(cls.getSimpleName() + " should not be private").isFalse();
            }
        }

        @Test
        @DisplayName("all implementations should be final")
        void shouldBeFinal() {
            Class<?>[] renderers = {
                    StockRenderer.class, FlowRenderer.class, AuxRenderer.class,
                    ModuleRenderer.class, LookupRenderer.class,
                    CldVariableRenderer.class, CommentRenderer.class
            };
            for (Class<?> cls : renderers) {
                assertThat(java.lang.reflect.Modifier.isFinal(cls.getModifiers()))
                        .as(cls.getSimpleName() + " should be final").isTrue();
            }
        }

        @Test
        @DisplayName("all implementations should implement ElementTypeRenderer")
        void shouldImplementInterface() {
            Class<?>[] renderers = {
                    StockRenderer.class, FlowRenderer.class, AuxRenderer.class,
                    ModuleRenderer.class, LookupRenderer.class,
                    CldVariableRenderer.class, CommentRenderer.class
            };
            for (Class<?> cls : renderers) {
                assertThat(ElementTypeRenderer.class.isAssignableFrom(cls))
                        .as(cls.getSimpleName() + " should implement ElementTypeRenderer")
                        .isTrue();
            }
        }
    }
}
