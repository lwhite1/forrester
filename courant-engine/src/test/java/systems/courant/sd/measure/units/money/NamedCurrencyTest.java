package systems.courant.sd.measure.units.money;

import systems.courant.sd.measure.Dimension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NamedCurrency")
class NamedCurrencyTest {

    @Test
    void shouldHaveMoneyDimension() {
        NamedCurrency eur = new NamedCurrency("EUR");
        assertThat(eur.getDimension()).isSameAs(Dimension.MONEY);
    }

    @Test
    void shouldReturnRatioOfOne() {
        NamedCurrency gbp = new NamedCurrency("GBP");
        assertThat(gbp.ratioToBaseUnit()).isEqualTo(1.0);
    }

    @Test
    void shouldPreserveName() {
        NamedCurrency jpy = new NamedCurrency("JPY");
        assertThat(jpy.getName()).isEqualTo("JPY");
        assertThat(jpy.toString()).isEqualTo("JPY");
    }

    @Test
    void shouldBeEqualForSameName() {
        NamedCurrency a = new NamedCurrency("EUR");
        NamedCurrency b = new NamedCurrency("EUR");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void shouldNotBeEqualForDifferentName() {
        NamedCurrency eur = new NamedCurrency("EUR");
        NamedCurrency gbp = new NamedCurrency("GBP");
        assertThat(eur).isNotEqualTo(gbp);
    }

    @Test
    void shouldNotBeEqualToNull() {
        NamedCurrency eur = new NamedCurrency("EUR");
        assertThat(eur).isNotEqualTo(null);
    }
}
