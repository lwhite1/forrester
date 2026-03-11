package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.def.AuxDef;
import systems.courant.shrewd.model.def.FlowDef;
import systems.courant.shrewd.model.def.ModelDefinition;
import systems.courant.shrewd.model.def.ModelDefinitionBuilder;
import systems.courant.shrewd.model.def.StockDef;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EquationValidator")
class EquationValidatorTest {

    private ModelEditor editor;

    @BeforeEach
    void setUp() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("Population", 100, "People")
                .constant("Birth Rate", 0.05, "1/Year")
                .flow("Births", "Population * Birth_Rate", "Year",
                        null, "Population")
                .build();
        editor = new ModelEditor();
        editor.loadFrom(def);
    }

    @Nested
    @DisplayName("syntax checking")
    class SyntaxChecking {

        @Test
        @DisplayName("valid equation returns OK")
        void validEquation() {
            EquationValidator.Result result =
                    EquationValidator.validate("Population * Birth_Rate", editor, "Births");
            assertThat(result.valid()).isTrue();
            assertThat(result.message()).isNull();
        }

        @Test
        @DisplayName("blank equation returns OK")
        void blankEquation() {
            EquationValidator.Result result =
                    EquationValidator.validate("", editor, "Births");
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("null equation returns OK")
        void nullEquation() {
            EquationValidator.Result result =
                    EquationValidator.validate(null, editor, "Births");
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("syntax error is reported")
        void syntaxError() {
            EquationValidator.Result result =
                    EquationValidator.validate("Population *", editor, "Births");
            assertThat(result.valid()).isFalse();
            assertThat(result.message()).isNotBlank();
        }

        @Test
        @DisplayName("unmatched parenthesis is reported")
        void unmatchedParen() {
            EquationValidator.Result result =
                    EquationValidator.validate("(Population + 1", editor, "Births");
            assertThat(result.valid()).isFalse();
        }
    }

    @Nested
    @DisplayName("reference checking")
    class ReferenceChecking {

        @Test
        @DisplayName("unknown variable is reported")
        void unknownVariable() {
            EquationValidator.Result result =
                    EquationValidator.validate("Popultion * Birth_Rate", editor, "Births");
            assertThat(result.valid()).isFalse();
            assertThat(result.message()).contains("Unknown variable 'Popultion'");
        }

        @Test
        @DisplayName("close match suggests correction")
        void closeSuggestion() {
            EquationValidator.Result result =
                    EquationValidator.validate("Popultion * Birth_Rate", editor, "Births");
            assertThat(result.valid()).isFalse();
            assertThat(result.message()).contains("did you mean 'Population'");
        }

        @Test
        @DisplayName("multiple unknowns are listed")
        void multipleUnknowns() {
            EquationValidator.Result result =
                    EquationValidator.validate("Foo + Bar", editor, "Births");
            assertThat(result.valid()).isFalse();
            assertThat(result.message()).contains("Foo").contains("Bar");
        }

        @Test
        @DisplayName("builtin names TIME and DT are accepted")
        void builtinNames() {
            EquationValidator.Result result =
                    EquationValidator.validate("Population * DT", editor, "Births");
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("underscore-to-space resolution works")
        void underscoreResolution() {
            EquationValidator.Result result =
                    EquationValidator.validate("Birth_Rate * 2", editor, "Births");
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("CLD variable names are recognized")
        void cldVariableRecognized() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("CLD Test")
                    .stock("S", 100, "u")
                    .cldVariable("Pressure")
                    .aux("A", "Pressure * 2", "u")
                    .flow("F", "S * 0.1", "day", null, "S")
                    .build();
            ModelEditor cldEditor = new ModelEditor();
            cldEditor.loadFrom(def);

            EquationValidator.Result result =
                    EquationValidator.validate("Pressure * 2", cldEditor, "A");
            assertThat(result.valid()).isTrue();
        }
    }

    @Nested
    @DisplayName("levenshtein")
    class Levenshtein {

        @Test
        @DisplayName("identical strings have distance 0")
        void identical() {
            assertThat(EquationValidator.levenshtein("abc", "abc")).isEqualTo(0);
        }

        @Test
        @DisplayName("single character difference")
        void singleDiff() {
            assertThat(EquationValidator.levenshtein("cat", "bat")).isEqualTo(1);
        }

        @Test
        @DisplayName("empty vs non-empty")
        void emptyVsNonEmpty() {
            assertThat(EquationValidator.levenshtein("", "abc")).isEqualTo(3);
        }
    }
}
