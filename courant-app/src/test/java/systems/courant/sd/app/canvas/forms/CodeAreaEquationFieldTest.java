package systems.courant.sd.app.canvas.forms;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CodeAreaEquationField")
@ExtendWith(ApplicationExtension.class)
class CodeAreaEquationFieldTest {

    private CodeAreaEquationField field;

    @Start
    void start(Stage stage) {
        field = new CodeAreaEquationField("Population * 0.03");
        VBox root = new VBox(field.node());
        stage.setScene(new Scene(root, 400, 200));
        stage.show();
    }

    @Test
    @DisplayName("should initialize with given text")
    void shouldInitializeWithText() {
        assertThat(field.getText()).isEqualTo("Population * 0.03");
    }

    @Test
    @DisplayName("getText and setText should round-trip")
    void shouldRoundTripText() {
        Platform.runLater(() -> field.setText("new_equation + 42"));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(field.getText()).isEqualTo("new_equation + 42");
    }

    @Test
    @DisplayName("textObservable should fire on programmatic setText")
    void shouldFireTextObservableOnSetText() {
        AtomicBoolean fired = new AtomicBoolean(false);
        field.textObservable().addListener((obs, old, newVal) -> fired.set(true));

        Platform.runLater(() -> field.setText("changed"));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(fired.get()).isTrue();
    }

    @Test
    @DisplayName("selectAll should not throw")
    void shouldSelectAll() {
        Platform.runLater(() -> field.selectAll());
        WaitForAsyncUtils.waitForFxEvents();
        // No exception means success
    }

    @Test
    @DisplayName("positionCaret should update caret position")
    void shouldPositionCaret() {
        Platform.runLater(() -> field.positionCaret(5));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(field.getCaretPosition()).isEqualTo(5);
    }

    @Test
    @DisplayName("setOnAction and getOnAction should round-trip")
    void shouldRoundTripOnAction() {
        AtomicBoolean called = new AtomicBoolean(false);
        field.setOnAction(e -> called.set(true));

        assertThat(field.getOnAction()).isNotNull();
        field.getOnAction().handle(new ActionEvent());

        assertThat(called.get()).isTrue();
    }

    @Test
    @DisplayName("setFieldStyle should not throw")
    void shouldSetFieldStyle() {
        Platform.runLater(() -> field.setFieldStyle("-fx-border-color: red;"));
        WaitForAsyncUtils.waitForFxEvents();
        // No exception means success
    }

    @Test
    @DisplayName("setFieldStyle should preserve monospace font")
    void shouldPreserveMonospaceFontInFieldStyle() {
        Platform.runLater(() -> field.setFieldStyle("-fx-border-color: red;"));
        WaitForAsyncUtils.waitForFxEvents();

        String style = field.node().getStyle();
        assertThat(style).contains("-fx-font-family: monospace");
        assertThat(style).contains("-fx-font-size: 13px");
        assertThat(style).contains("-fx-border-color: red");
    }

    @Test
    @DisplayName("setFieldStyle with null should retain base font style")
    void shouldRetainBaseFontStyleWhenNull() {
        Platform.runLater(() -> field.setFieldStyle(null));
        WaitForAsyncUtils.waitForFxEvents();

        String style = field.node().getStyle();
        assertThat(style).contains("-fx-font-family: monospace");
        assertThat(style).contains("-fx-font-size: 13px");
    }

    @Test
    @DisplayName("setFieldStyle with empty string should retain base font style")
    void shouldRetainBaseFontStyleWhenEmpty() {
        Platform.runLater(() -> field.setFieldStyle(""));
        WaitForAsyncUtils.waitForFxEvents();

        String style = field.node().getStyle();
        assertThat(style).contains("-fx-font-family: monospace");
        assertThat(style).contains("-fx-font-size: 13px");
    }

    @Test
    @DisplayName("node should return a non-null Node")
    void shouldReturnNode() {
        assertThat(field.node()).isNotNull();
    }

    @Test
    @DisplayName("focusedProperty should be observable")
    void shouldHaveFocusedProperty() {
        assertThat(field.focusedProperty()).isNotNull();
    }

    @Test
    @DisplayName("setText with empty string should clear text")
    void shouldClearTextWithEmptyString() {
        Platform.runLater(() -> field.setText(""));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(field.getText()).isEmpty();
    }

    @Test
    @DisplayName("setText with null should clear text")
    void shouldClearTextWithNull() {
        Platform.runLater(() -> field.setText(null));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(field.getText()).isEmpty();
    }
}
