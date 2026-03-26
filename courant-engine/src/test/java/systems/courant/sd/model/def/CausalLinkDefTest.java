package systems.courant.sd.model.def;

import systems.courant.sd.model.def.CausalLinkDef.Polarity;
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

    @Test
    void shouldDefaultBiasToZero() {
        assertThat(new CausalLinkDef("A", "B").bias()).isEqualTo(0.0);
        assertThat(new CausalLinkDef("A", "B", Polarity.POSITIVE).bias()).isEqualTo(0.0);
        assertThat(new CausalLinkDef("A", "B", Polarity.POSITIVE, "note").bias()).isEqualTo(0.0);
        assertThat(new CausalLinkDef("A", "B", Polarity.POSITIVE, null, 50.0).bias()).isEqualTo(0.0);
    }

    @Test
    void shouldReportHasBias() {
        assertThat(new CausalLinkDef("A", "B").hasBias()).isFalse();
        assertThat(new CausalLinkDef("A", "B", Polarity.UNKNOWN, null, Double.NaN, 10.0).hasBias()).isTrue();
        assertThat(new CausalLinkDef("A", "B", Polarity.UNKNOWN, null, Double.NaN, 0.0).hasBias()).isFalse();
    }

    @Test
    void shouldReturnCopyWithBias() {
        CausalLinkDef link = new CausalLinkDef("A", "B", Polarity.POSITIVE, "note", 50.0);
        CausalLinkDef withBias = link.withBias(30.0);

        assertThat(withBias.bias()).isEqualTo(30.0);
        assertThat(withBias.strength()).isEqualTo(50.0);
        assertThat(withBias.polarity()).isEqualTo(Polarity.POSITIVE);
        assertThat(withBias.comment()).isEqualTo("note");
    }

    @Test
    void shouldReturnCopyWithStrengthAndBias() {
        CausalLinkDef link = new CausalLinkDef("A", "B");
        CausalLinkDef updated = link.withStrengthAndBias(80.0, -20.0);

        assertThat(updated.strength()).isEqualTo(80.0);
        assertThat(updated.bias()).isEqualTo(-20.0);
    }

    @Test
    void shouldPreserveBiasInWithStrength() {
        CausalLinkDef link = new CausalLinkDef("A", "B", Polarity.UNKNOWN, null, 50.0, 25.0);
        CausalLinkDef updated = link.withStrength(100.0);

        assertThat(updated.strength()).isEqualTo(100.0);
        assertThat(updated.bias()).isEqualTo(25.0);
    }
}
