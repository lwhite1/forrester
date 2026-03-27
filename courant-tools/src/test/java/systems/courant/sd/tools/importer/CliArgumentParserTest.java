package systems.courant.sd.tools.importer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CliArgumentParser")
class CliArgumentParserTest {

    @Test
    void shouldReturnValueFollowingFlag() {
        String[] args = {"--file", "model.mdl", "--class", "Demo"};
        assertThat(CliArgumentParser.requireValue(args, 0)).isEqualTo("model.mdl");
        assertThat(CliArgumentParser.requireValue(args, 2)).isEqualTo("Demo");
    }

    @Test
    void shouldThrowWhenNoValueFollowsFlag() {
        String[] args = {"--file"};
        assertThatThrownBy(() -> CliArgumentParser.requireValue(args, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--file")
                .hasMessageContaining("requires a value");
    }

    @Test
    void shouldThrowWhenFlagIsLastElement() {
        String[] args = {"--output-dir", "/tmp", "--verbose"};
        assertThatThrownBy(() -> CliArgumentParser.requireValue(args, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--verbose");
    }

    @Test
    void shouldReturnValueAtMiddleOfArray() {
        String[] args = {"--a", "1", "--b", "2", "--c", "3"};
        assertThat(CliArgumentParser.requireValue(args, 2)).isEqualTo("2");
    }
}
