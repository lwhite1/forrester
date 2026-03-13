package systems.courant.sd.app.canvas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ActivityLogEntry")
class ActivityLogEntryTest {

    @Test
    @DisplayName("Valid entry stores all fields")
    void validEntry() {
        LocalDateTime now = LocalDateTime.now();
        ActivityLogEntry entry = new ActivityLogEntry(now, "edit", "Added stock");
        assertThat(entry.timestamp()).isEqualTo(now);
        assertThat(entry.type()).isEqualTo("edit");
        assertThat(entry.message()).isEqualTo("Added stock");
    }

    @Test
    @DisplayName("Null timestamp throws IllegalArgumentException")
    void nullTimestampThrows() {
        assertThatThrownBy(() -> new ActivityLogEntry(null, "edit", "msg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timestamp");
    }

    @Test
    @DisplayName("Null type throws IllegalArgumentException")
    void nullTypeThrows() {
        assertThatThrownBy(() -> new ActivityLogEntry(LocalDateTime.now(), null, "msg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type");
    }

    @Test
    @DisplayName("Blank type throws IllegalArgumentException")
    void blankTypeThrows() {
        assertThatThrownBy(() -> new ActivityLogEntry(LocalDateTime.now(), "  ", "msg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type");
    }

    @Test
    @DisplayName("Null message throws IllegalArgumentException")
    void nullMessageThrows() {
        assertThatThrownBy(() -> new ActivityLogEntry(LocalDateTime.now(), "edit", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("message");
    }

    @Test
    @DisplayName("Blank message throws IllegalArgumentException")
    void blankMessageThrows() {
        assertThatThrownBy(() -> new ActivityLogEntry(LocalDateTime.now(), "edit", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("message");
    }
}
