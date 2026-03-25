package systems.courant.sd.app;

import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HelpWindowManager (TestFX)")
@ExtendWith(ApplicationExtension.class)
class HelpWindowManagerFxTest {

    private Stage owner;
    private HelpWindowManager manager;

    @Start
    void start(Stage stage) {
        this.owner = stage;
        this.manager = new HelpWindowManager(stage);
    }

    @Test
    @DisplayName("showOrBring by class sets owner on new window")
    void shouldSetOwnerOnNewWindow() {
        AtomicReference<Stage> windowRef = new AtomicReference<>();
        WaitForAsyncUtils.waitForAsyncFx(5000, () -> {
            windowRef.set(manager.showOrBring(Stage.class, Stage::new));
        });
        Stage window = windowRef.get();
        assertThat(window.getOwner()).isSameAs(owner);
        WaitForAsyncUtils.waitForAsyncFx(5000, window::close);
    }

    @Test
    @DisplayName("showOrBring by class skips initOwner when already set")
    void shouldSkipInitOwnerWhenAlreadySet() {
        AtomicReference<Stage> windowRef = new AtomicReference<>();
        WaitForAsyncUtils.waitForAsyncFx(5000, () -> {
            windowRef.set(manager.showOrBring(Stage.class, () -> {
                Stage s = new Stage();
                s.initOwner(owner);
                return s;
            }));
        });
        Stage window = windowRef.get();
        assertThat(window.getOwner()).isSameAs(owner);
        WaitForAsyncUtils.waitForAsyncFx(5000, window::close);
    }

    @Test
    @DisplayName("showOrBring by key sets owner on new window")
    void shouldSetOwnerByKey() {
        AtomicReference<Stage> windowRef = new AtomicReference<>();
        WaitForAsyncUtils.waitForAsyncFx(5000, () -> {
            windowRef.set(manager.showOrBring("test-key", Stage::new));
        });
        Stage window = windowRef.get();
        assertThat(window.getOwner()).isSameAs(owner);
        WaitForAsyncUtils.waitForAsyncFx(5000, window::close);
    }

    @Test
    @DisplayName("showOrBring by key skips initOwner when already set")
    void shouldSkipInitOwnerByKeyWhenAlreadySet() {
        AtomicReference<Stage> windowRef = new AtomicReference<>();
        WaitForAsyncUtils.waitForAsyncFx(5000, () -> {
            windowRef.set(manager.showOrBring("test-key", () -> {
                Stage s = new Stage();
                s.initOwner(owner);
                return s;
            }));
        });
        Stage window = windowRef.get();
        assertThat(window.getOwner()).isSameAs(owner);
        WaitForAsyncUtils.waitForAsyncFx(5000, window::close);
    }
}
