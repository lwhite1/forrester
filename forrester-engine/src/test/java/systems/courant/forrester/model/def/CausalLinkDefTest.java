package systems.courant.forrester.model.def;

import systems.courant.forrester.model.def.CausalLinkDef.Polarity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CausalLinkDef")
class CausalLinkDefTest {

    @Test
    void shouldCreateWithPolarity() {
        CausalLinkDef link = new CausalLinkDef("Workload", "Burnout", Polarity.POSITIVE);
        assertThat(link.from()).isEqualTo("Workload");
        assertThat(link.to()).isEqualTo("Burnout");
        assertThat(link.polarity()).isEqualTo(Polarity.POSITIVE);
        assertThat(link.comment()).isNull();
    }

    @Test
    void shouldDefaultToUnknownPolarity() {
        CausalLinkDef link = new CausalLinkDef("A", "B");
        assertThat(link.polarity()).isEqualTo(Polarity.UNKNOWN);
    }

    @Test
    void shouldDefaultNullPolarityToUnknown() {
        CausalLinkDef link = new CausalLinkDef("A", "B", null, "some comment");
        assertThat(link.polarity()).isEqualTo(Polarity.UNKNOWN);
    }

    @Test
    void shouldRejectBlankEndpoints() {
        assertThatThrownBy(() -> new CausalLinkDef("", "B"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CausalLinkDef("A", ""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CausalLinkDef(null, "B"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldParsePolaritySymbols() {
        assertThat(Polarity.fromSymbol("+")).isEqualTo(Polarity.POSITIVE);
        assertThat(Polarity.fromSymbol("-")).isEqualTo(Polarity.NEGATIVE);
        assertThat(Polarity.fromSymbol("?")).isEqualTo(Polarity.UNKNOWN);
        assertThat(Polarity.fromSymbol(null)).isEqualTo(Polarity.UNKNOWN);
    }

    @Test
    void shouldProvidePolaritySymbols() {
        assertThat(Polarity.POSITIVE.symbol()).isEqualTo("+");
        assertThat(Polarity.NEGATIVE.symbol()).isEqualTo("-");
        assertThat(Polarity.UNKNOWN.symbol()).isEqualTo("?");
    }
}
