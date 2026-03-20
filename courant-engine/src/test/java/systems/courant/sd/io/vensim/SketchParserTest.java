package systems.courant.sd.io.vensim;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import systems.courant.sd.model.def.CommentDef;
import systems.courant.sd.model.def.ElementPlacement;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.ViewDef;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SketchParser")
class SketchParserTest {

    @Nested
    @DisplayName("Text annotations")
    class TextAnnotations {

        @Test
        void shouldParseTextAnnotationAsComment() {
            List<String> lines = List.of(
                    "*Main",
                    "12,26,0,618,433,50,30,8,7,0,8,-1,0,0,0,0-0-0,0-0-0,|8||0-0-0",
                    "Hello world"
            );

            SketchParser.ParseResult result = SketchParser.parseWithComments(
                    lines, Set.of(), Set.of(), Set.of(), Set.of());

            assertThat(result.comments()).hasSize(1);
            CommentDef comment = result.comments().getFirst();
            assertThat(comment.name()).isEqualTo("Comment 1");
            assertThat(comment.text()).isEqualTo("Hello world");
        }

        @Test
        void shouldPlaceCommentElementInView() {
            List<String> lines = List.of(
                    "*Main",
                    "12,26,0,618,433,50,30,8,7,0,8,-1,0,0,0",
                    "Annotation text"
            );

            SketchParser.ParseResult result = SketchParser.parseWithComments(
                    lines, Set.of(), Set.of(), Set.of(), Set.of());

            ViewDef view = result.views().getFirst();
            assertThat(view.elements()).hasSize(1);
            ElementPlacement ep = view.elements().getFirst();
            assertThat(ep.type()).isEqualTo(ElementType.COMMENT);
            assertThat(ep.x()).isEqualTo(618);
            assertThat(ep.y()).isEqualTo(433);
            assertThat(ep.width()).isEqualTo(100);
            assertThat(ep.height()).isEqualTo(60);
        }

        @Test
        void shouldParseMultipleAnnotations() {
            List<String> lines = List.of(
                    "*Main",
                    "12,26,0,100,200,40,16,8,7,0,24,-1,0,0,0",
                    "First note",
                    "12,27,0,300,400,37,16,8,7,0,24,-1,0,0,0",
                    "Second note"
            );

            SketchParser.ParseResult result = SketchParser.parseWithComments(
                    lines, Set.of(), Set.of(), Set.of(), Set.of());

            assertThat(result.comments()).hasSize(2);
            assertThat(result.comments().get(0).text()).isEqualTo("First note");
            assertThat(result.comments().get(0).name()).isEqualTo("Comment 1");
            assertThat(result.comments().get(1).text()).isEqualTo("Second note");
            assertThat(result.comments().get(1).name()).isEqualTo("Comment 2");
        }

        @Test
        void shouldSkipCloudLines() {
            List<String> lines = List.of(
                    "*Main",
                    "12,4,48,105,546,10,8,0,3,0,0,-1,0,0,0",
                    "12,24,2,608,447,23,23,5,3,0,0,-1,0,0,0"
            );

            SketchParser.ParseResult result = SketchParser.parseWithComments(
                    lines, Set.of(), Set.of(), Set.of(), Set.of());

            assertThat(result.comments()).isEmpty();
            assertThat(result.views().getFirst().elements()).isEmpty();
        }

        @Test
        void shouldMixAnnotationsWithModelElements() {
            List<String> lines = List.of(
                    "*Main",
                    "10,1,Population,200,300,40,20,3,3,0,0,0,0,0,0",
                    "12,26,0,400,500,30,15,8,7,0,8,-1,0,0,0",
                    "Note about population",
                    "12,4,48,105,546,10,8,0,3,0,0,-1,0,0,0"
            );

            SketchParser.ParseResult result = SketchParser.parseWithComments(
                    lines, Set.of("Population"), Set.of(), Set.of(), Set.of());

            ViewDef view = result.views().getFirst();
            assertThat(view.elements()).hasSize(2);
            assertThat(view.elements().get(0).type()).isEqualTo(ElementType.STOCK);
            assertThat(view.elements().get(1).type()).isEqualTo(ElementType.COMMENT);
            assertThat(result.comments()).hasSize(1);
        }

        @Test
        void shouldReturnEmptyCommentsWhenNoAnnotations() {
            List<String> lines = List.of(
                    "*Main",
                    "10,1,Stock1,200,300,40,20,3,3,0,0,0,0,0,0"
            );

            SketchParser.ParseResult result = SketchParser.parseWithComments(
                    lines, Set.of("Stock1"), Set.of(), Set.of(), Set.of());

            assertThat(result.comments()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Backward compatibility")
    class BackwardCompatibility {

        @Test
        void shouldReturnViewsFromLegacyParseMethod() {
            List<String> lines = List.of(
                    "*Main",
                    "10,1,Var1,100,200,40,20,3,3,0,0,0,0,0,0",
                    "12,26,0,300,400,50,30,8,7,0,8,-1,0,0,0",
                    "Some annotation"
            );

            List<ViewDef> views = SketchParser.parse(
                    lines, Set.of(), Set.of(), Set.of(), Set.of());

            assertThat(views).hasSize(1);
            // Legacy parse returns views with comment placements included
            assertThat(views.getFirst().elements()).hasSize(2);
        }
    }
}
