package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.def.ModelDefinition;
import systems.courant.shrewd.model.def.ModelDefinitionBuilder;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EquationAutoComplete")
class EquationAutoCompleteTest {

    @Nested
    @DisplayName("extractToken")
    class ExtractToken {

        @Test
        void shouldExtractTokenAtEndOfExpression() {
            var token = EquationAutoComplete.extractToken("Stock_A * Bi", 12);
            assertThat(token).isNotNull();
            assertThat(token.prefix()).isEqualTo("Bi");
            assertThat(token.start()).isEqualTo(10);
            assertThat(token.end()).isEqualTo(12);
        }

        @Test
        void shouldReturnNullWhenCaretAfterOperator() {
            var token = EquationAutoComplete.extractToken("Stock_A * ", 10);
            assertThat(token).isNull();
        }

        @Test
        void shouldReturnNullWhenCaretAfterParen() {
            var token = EquationAutoComplete.extractToken("SMOOTH(", 7);
            assertThat(token).isNull();
        }

        @Test
        void shouldReturnNullForEmptyText() {
            var token = EquationAutoComplete.extractToken("", 0);
            assertThat(token).isNull();
        }

        @Test
        void shouldExtractTokenFromStart() {
            var token = EquationAutoComplete.extractToken("Pop", 3);
            assertThat(token).isNotNull();
            assertThat(token.prefix()).isEqualTo("Pop");
            assertThat(token.start()).isEqualTo(0);
            assertThat(token.end()).isEqualTo(3);
        }

        @Test
        void shouldExtractTokenAfterOperator() {
            var token = EquationAutoComplete.extractToken("A + B", 5);
            assertThat(token).isNotNull();
            assertThat(token.prefix()).isEqualTo("B");
            assertThat(token.start()).isEqualTo(4);
            assertThat(token.end()).isEqualTo(5);
        }

        @Test
        void shouldReturnNullForNullText() {
            var token = EquationAutoComplete.extractToken(null, 0);
            assertThat(token).isNull();
        }

        @Test
        void shouldHandleUnderscoresInIdentifiers() {
            var token = EquationAutoComplete.extractToken("Birth_Ra", 8);
            assertThat(token).isNotNull();
            assertThat(token.prefix()).isEqualTo("Birth_Ra");
            assertThat(token.start()).isEqualTo(0);
            assertThat(token.end()).isEqualTo(8);
        }
    }

    @Nested
    @DisplayName("filterSuggestions")
    class FilterSuggestions {

        private final List<String> suggestions = List.of(
                "Birth_Rate", "Stock_A", "STEP", "SMOOTH", "SUM");

        @Test
        void shouldMatchCaseInsensitivePrefix() {
            assertThat(EquationAutoComplete.filterSuggestions(suggestions, "St"))
                    .containsExactly("Stock_A", "STEP");
        }

        @Test
        void shouldMatchLowerCasePrefix() {
            assertThat(EquationAutoComplete.filterSuggestions(suggestions, "st"))
                    .containsExactly("Stock_A", "STEP");
        }

        @Test
        void shouldReturnEmptyForNoMatch() {
            assertThat(EquationAutoComplete.filterSuggestions(suggestions, "xyz"))
                    .isEmpty();
        }

        @Test
        void shouldReturnEmptyForEmptyPrefix() {
            assertThat(EquationAutoComplete.filterSuggestions(suggestions, ""))
                    .isEmpty();
        }

        @Test
        void shouldReturnEmptyForNullPrefix() {
            assertThat(EquationAutoComplete.filterSuggestions(suggestions, null))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("getSuggestions")
    class GetSuggestions {

        private ModelEditor editor;

        @BeforeEach
        void setUp() {
            editor = new ModelEditor();
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 1000, "people")
                    .flow("Birth Rate", "Population * 0.03", "year", null, "Population")
                    .aux("Contact Rate", "5", "contacts/day")
                    .constant("Growth Factor", 1.5, "dimensionless")
                    .build();
            editor.loadFrom(def);
        }

        @Test
        void shouldExcludeSelfReference() {
            List<String> suggestions = EquationAutoComplete.getSuggestions(
                    editor, "Birth Rate");
            assertThat(suggestions).doesNotContain("Birth_Rate");
            assertThat(suggestions).contains("Population", "Contact_Rate", "Growth_Factor");
        }

        @Test
        void shouldIncludeBuiltInFunctions() {
            List<String> suggestions = EquationAutoComplete.getSuggestions(editor, null);
            assertThat(suggestions).containsAll(EquationAutoComplete.BUILT_IN_FUNCTIONS);
        }

        @Test
        void shouldReplaceSpacesWithUnderscores() {
            List<String> suggestions = EquationAutoComplete.getSuggestions(editor, null);
            assertThat(suggestions).contains("Birth_Rate", "Contact_Rate", "Growth_Factor");
            assertThat(suggestions).doesNotContain("Birth Rate", "Contact Rate", "Growth Factor");
        }

        @Test
        void shouldListElementsBeforeFunctions() {
            List<String> suggestions = EquationAutoComplete.getSuggestions(editor, null);
            int lastElementIdx = suggestions.indexOf("Population");
            int firstFunctionIdx = suggestions.indexOf(
                    EquationAutoComplete.BUILT_IN_FUNCTIONS.stream()
                            .sorted().findFirst().orElseThrow());
            assertThat(lastElementIdx).isLessThan(firstFunctionIdx);
        }

        @Test
        void shouldReflectNewlyAddedElements() {
            List<String> before = EquationAutoComplete.getSuggestions(editor, null);
            assertThat(before).doesNotContain("Death_Rate");

            // Reload with an additional flow to simulate model change
            ModelDefinition def2 = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 1000, "people")
                    .flow("Birth Rate", "Population * 0.03", "year", null, "Population")
                    .flow("Death Rate", "Population * 0.01", "year", "Population", null)
                    .aux("Contact Rate", "5", "contacts/day")
                    .constant("Growth Factor", 1.5, "dimensionless")
                    .build();
            editor.loadFrom(def2);

            List<String> after = EquationAutoComplete.getSuggestions(editor, null);
            assertThat(after).contains("Death_Rate");
        }

        @Test
        void shouldReflectRemovedElements() {
            List<String> before = EquationAutoComplete.getSuggestions(editor, null);
            assertThat(before).contains("Contact_Rate");

            // Reload without Contact Rate
            ModelDefinition def2 = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 1000, "people")
                    .flow("Birth Rate", "Population * 0.03", "year", null, "Population")
                    .constant("Growth Factor", 1.5, "dimensionless")
                    .build();
            editor.loadFrom(def2);

            List<String> after = EquationAutoComplete.getSuggestions(editor, null);
            assertThat(after).doesNotContain("Contact_Rate");
        }
    }

    @Nested
    @DisplayName("detectFunctionContext")
    class DetectFunctionContext {

        @Test
        void shouldDetectFirstParamRightAfterOpenParen() {
            // STEP(|
            var ctx = EquationAutoComplete.detectFunctionContext("STEP(", 5);
            assertThat(ctx).isNotNull();
            assertThat(ctx.functionName()).isEqualTo("STEP");
            assertThat(ctx.paramIndex()).isEqualTo(0);
        }

        @Test
        void shouldDetectFirstParamWhileTyping() {
            // STEP(10|
            var ctx = EquationAutoComplete.detectFunctionContext("STEP(10", 7);
            assertThat(ctx).isNotNull();
            assertThat(ctx.functionName()).isEqualTo("STEP");
            assertThat(ctx.paramIndex()).isEqualTo(0);
        }

        @Test
        void shouldDetectSecondParamAfterComma() {
            // STEP(10, |
            var ctx = EquationAutoComplete.detectFunctionContext("STEP(10, ", 9);
            assertThat(ctx).isNotNull();
            assertThat(ctx.functionName()).isEqualTo("STEP");
            assertThat(ctx.paramIndex()).isEqualTo(1);
        }

        @Test
        void shouldDetectSecondParamWhileTyping() {
            // STEP(10, 5|
            var ctx = EquationAutoComplete.detectFunctionContext("STEP(10, 5", 10);
            assertThat(ctx).isNotNull();
            assertThat(ctx.functionName()).isEqualTo("STEP");
            assertThat(ctx.paramIndex()).isEqualTo(1);
        }

        @Test
        void shouldHandleNestedFunctionCalls() {
            // SMOOTH(STEP(10, 5), |
            var ctx = EquationAutoComplete.detectFunctionContext("SMOOTH(STEP(10, 5), ", 20);
            assertThat(ctx).isNotNull();
            assertThat(ctx.functionName()).isEqualTo("SMOOTH");
            assertThat(ctx.paramIndex()).isEqualTo(1);
        }

        @Test
        void shouldDetectInnerFunctionContext() {
            // SMOOTH(STEP(10, |), 4)
            var ctx = EquationAutoComplete.detectFunctionContext("SMOOTH(STEP(10, ), 4)", 16);
            assertThat(ctx).isNotNull();
            assertThat(ctx.functionName()).isEqualTo("STEP");
            assertThat(ctx.paramIndex()).isEqualTo(1);
        }

        @Test
        void shouldReturnNullOutsideParens() {
            var ctx = EquationAutoComplete.detectFunctionContext("Population * ", 13);
            assertThat(ctx).isNull();
        }

        @Test
        void shouldReturnNullForUnknownFunction() {
            // FOOBAR(|
            var ctx = EquationAutoComplete.detectFunctionContext("FOOBAR(", 7);
            assertThat(ctx).isNull();
        }

        @Test
        void shouldReturnNullForBareParens() {
            // (10 + |
            var ctx = EquationAutoComplete.detectFunctionContext("(10 + ", 6);
            assertThat(ctx).isNull();
        }

        @Test
        void shouldReturnNullForNullText() {
            assertThat(EquationAutoComplete.detectFunctionContext(null, 0)).isNull();
        }

        @Test
        void shouldReturnNullForEmptyText() {
            assertThat(EquationAutoComplete.detectFunctionContext("", 0)).isNull();
        }

        @Test
        void shouldHandleCaseInsensitiveFunctionName() {
            // smooth(x, |
            var ctx = EquationAutoComplete.detectFunctionContext("smooth(x, ", 10);
            assertThat(ctx).isNotNull();
            assertThat(ctx.functionName()).isEqualTo("SMOOTH");
            assertThat(ctx.paramIndex()).isEqualTo(1);
        }

        @Test
        void shouldHandleThreeArgFunction() {
            // DELAY_FIXED(input, 3, |
            var ctx = EquationAutoComplete.detectFunctionContext("DELAY_FIXED(input, 3, ", 22);
            assertThat(ctx).isNotNull();
            assertThat(ctx.functionName()).isEqualTo("DELAY_FIXED");
            assertThat(ctx.paramIndex()).isEqualTo(2);
        }

        @Test
        void shouldReturnNullAfterClosingParen() {
            // STEP(10, 5)|
            var ctx = EquationAutoComplete.detectFunctionContext("STEP(10, 5)", 11);
            assertThat(ctx).isNull();
        }
    }

    @Nested
    @DisplayName("No stale suggestion cache (#438)")
    class NoStaleSuggestionCache {

        @Test
        void shouldNotHaveCachedAllSuggestionsFieldInState() {
            // Guard against reintroducing the allSuggestions cache field
            boolean hasCachedField = Arrays.stream(
                            EquationAutoComplete.class.getDeclaredClasses())
                    .filter(c -> c.getSimpleName().equals("State"))
                    .flatMap(c -> Arrays.stream(c.getDeclaredFields()))
                    .anyMatch(f -> f.getName().equals("allSuggestions"));
            assertThat(hasCachedField)
                    .as("State should not cache allSuggestions — see #438")
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("isBuiltInFunction")
    class IsBuiltInFunction {

        @Test
        void shouldReturnTrueForBuiltIn() {
            assertThat(EquationAutoComplete.isBuiltInFunction("SMOOTH")).isTrue();
            assertThat(EquationAutoComplete.isBuiltInFunction("TIME")).isTrue();
        }

        @Test
        void shouldReturnFalseForNonBuiltIn() {
            assertThat(EquationAutoComplete.isBuiltInFunction("Population")).isFalse();
        }
    }
}
