package systems.courant.sd.measure.units.length;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Lengths")
class LengthsTest {

    @Test
    void shouldBeFinalClass() {
        assertThat(Modifier.isFinal(Lengths.class.getModifiers())).isTrue();
    }

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<Lengths> ctor = Lengths.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(ctor.getModifiers())).isTrue();
    }

    @Test
    void shouldCreateFeetQuantity() {
        assertThat(Lengths.feet(10).getValue()).isEqualTo(10);
    }

    @Test
    void shouldCreateInchesQuantity() {
        assertThat(Lengths.inches(24).getValue()).isEqualTo(24);
    }

    @Test
    void shouldCreateMetersQuantity() {
        assertThat(Lengths.meters(100).getValue()).isEqualTo(100);
    }

    @Test
    void shouldCreateMilesQuantity() {
        assertThat(Lengths.miles(5).getValue()).isEqualTo(5);
    }

    @Test
    void shouldCreateNauticalMilesQuantity() {
        assertThat(Lengths.nauticalMiles(3).getValue()).isEqualTo(3);
    }
}
