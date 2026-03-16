package systems.courant.sd.model.compile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FunctionDocRegistry")
class FunctionDocRegistryTest {

    @Test
    void shouldContainAllCoreFunctions() {
        List<String> names = FunctionDocRegistry.allNames();
        assertThat(names).contains(
                "TIME", "DT", "IF",
                "ABS", "SQRT", "LN", "EXP", "LOG", "SIN", "COS", "TAN",
                "ARCSIN", "ARCCOS", "ARCTAN", "SIGN", "PI",
                "INT", "ROUND", "MIN", "MAX", "MODULO", "QUANTUM", "POWER",
                "SUM", "MEAN", "VMIN", "VMAX", "PROD",
                "XIDZ", "ZIDZ", "INITIAL",
                "STEP", "RAMP", "PULSE", "PULSE_TRAIN",
                "SMOOTH", "DELAY1I", "DELAY3", "DELAY3I", "DELAY_FIXED",
                "TREND", "FORECAST", "NPV",
                "LOOKUP", "RANDOM_NORMAL", "RANDOM_UNIFORM",
                "SAMPLE_IF_TRUE", "FIND_ZERO", "LOOKUP_AREA");
    }

    @Test
    void shouldReturnDocByName() {
        assertThat(FunctionDocRegistry.get("STEP")).isPresent();
        assertThat(FunctionDocRegistry.get("step")).isPresent();
        assertThat(FunctionDocRegistry.get("NONEXISTENT")).isEmpty();
    }

    @Test
    void shouldHaveCompleteDocForEachFunction() {
        for (FunctionDoc doc : FunctionDocRegistry.all()) {
            assertThat(doc.name()).as("name for %s", doc).isNotBlank();
            assertThat(doc.signature()).as("signature for %s", doc.name()).isNotBlank();
            assertThat(doc.oneLiner()).as("oneLiner for %s", doc.name()).isNotBlank();
            assertThat(doc.category()).as("category for %s", doc.name()).isIn("SD", "Math", "Special");
            assertThat(doc.behavior()).as("behavior for %s", doc.name()).isNotBlank();
            assertThat(doc.example()).as("example for %s", doc.name()).isNotBlank();
            assertThat(doc.parameters()).as("parameters for %s", doc.name()).isNotNull();
            assertThat(doc.related()).as("related for %s", doc.name()).isNotNull();
        }
    }

    @Test
    void shouldFilterByCategory() {
        List<FunctionDoc> sd = FunctionDocRegistry.byCategory("SD");
        assertThat(sd).isNotEmpty();
        assertThat(sd).allMatch(d -> "SD".equals(d.category()));

        List<FunctionDoc> math = FunctionDocRegistry.byCategory("Math");
        assertThat(math).isNotEmpty();
        assertThat(math).allMatch(d -> "Math".equals(d.category()));
    }

    @Test
    void shouldHaveUniqueNames() {
        List<String> names = FunctionDocRegistry.allNames();
        Set<String> unique = names.stream().collect(Collectors.toSet());
        assertThat(unique).hasSameSizeAs(names);
    }

    @Test
    void shouldHaveParameterDocsForFunctionsWithArgs() {
        FunctionDoc step = FunctionDocRegistry.get("STEP").orElseThrow();
        assertThat(step.parameters()).hasSize(2);
        assertThat(step.parameters().get(0).name()).isEqualTo("height");
        assertThat(step.parameters().get(1).name()).isEqualTo("step_time");
    }

    @Test
    void shouldDocumentIFWithFunctionCallSyntax() {
        FunctionDoc doc = FunctionDocRegistry.get("IF").orElseThrow();
        assertThat(doc.signature()).isEqualTo("IF(condition, a, b)");
        assertThat(doc.example()).isEqualTo("IF(Population > 1000, Growth_Rate, 0)");
    }

    @Test
    void shouldDocumentRandomNormalWith5Parameters() {
        FunctionDoc doc = FunctionDocRegistry.get("RANDOM_NORMAL").orElseThrow();
        assertThat(doc.parameters()).hasSize(5);
        assertThat(doc.parameters().get(0).name()).isEqualTo("min");
        assertThat(doc.parameters().get(1).name()).isEqualTo("max");
        assertThat(doc.parameters().get(2).name()).isEqualTo("mean");
        assertThat(doc.parameters().get(3).name()).isEqualTo("std_dev");
        assertThat(doc.parameters().get(4).name()).isEqualTo("seed");
        assertThat(doc.signature()).contains("[, seed]");
    }
}
