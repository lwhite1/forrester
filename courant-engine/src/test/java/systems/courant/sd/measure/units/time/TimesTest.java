package systems.courant.sd.measure.units.time;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Times")
class TimesTest {

    @Test
    void shouldBeFinalClass() {
        assertThat(Modifier.isFinal(Times.class.getModifiers())).isTrue();
    }

    @Test
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
        Constructor<Times> ctor = Times.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(ctor.getModifiers())).isTrue();
    }

    @Test
    void shouldCreateMillisecondQuantity() {
        assertThat(Times.milliseconds(500).getValue()).isEqualTo(500);
    }

    @Test
    void shouldCreateSecondQuantity() {
        assertThat(Times.seconds(30).getValue()).isEqualTo(30);
    }

    @Test
    void shouldCreateMinuteQuantity() {
        assertThat(Times.minutes(5).getValue()).isEqualTo(5);
    }

    @Test
    void shouldCreateHourQuantity() {
        assertThat(Times.hours(2).getValue()).isEqualTo(2);
    }

    @Test
    void shouldCreateDayQuantity() {
        assertThat(Times.days(7).getValue()).isEqualTo(7);
    }

    @Test
    void shouldCreateWeekQuantity() {
        assertThat(Times.weeks(4).getValue()).isEqualTo(4);
    }

    @Test
    void shouldCreateMonthQuantity() {
        assertThat(Times.months(12).getValue()).isEqualTo(12);
    }

    @Test
    void shouldCreateYearQuantity() {
        assertThat(Times.years(1).getValue()).isEqualTo(1);
    }
}
