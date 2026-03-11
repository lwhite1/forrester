package systems.courant.shrewd.model.def;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CldVariableDef")
class CldVariableDefTest {

    @Test
    void shouldCreateWithNameOnly() {
        CldVariableDef var = new CldVariableDef("Workload");
        assertThat(var.name()).isEqualTo("Workload");
        assertThat(var.comment()).isNull();
    }

    @Test
    void shouldCreateWithNameAndComment() {
        CldVariableDef var = new CldVariableDef("Burnout", "Accumulated fatigue");
        assertThat(var.name()).isEqualTo("Burnout");
        assertThat(var.comment()).isEqualTo("Accumulated fatigue");
    }

    @Test
    void shouldRejectBlankName() {
        assertThatThrownBy(() -> new CldVariableDef(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CldVariableDef(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
