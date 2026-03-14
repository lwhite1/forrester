package systems.courant.sd.app.canvas;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ParameterRowBase (#68)")
@ExtendWith(ApplicationExtension.class)
class ParameterRowBaseTest {

    @Start
    void start(Stage stage) {
        stage.show();
    }

    /**
     * Concrete test subclass of ParameterRowBase for verifying base behavior.
     */
    static class TestRow extends ParameterRowBase {
        private final TextField valueField;
        private boolean valid = true;

        TestRow(List<String> names, String defaultName, Runnable onChange) {
            super(names, defaultName, onChange);
            valueField = new TextField("test");
            wireFieldChange(valueField);
            Button removeBtn = createRemoveButton(() -> {});
            buildPane(removeBtn, new Label("Val:"), valueField);
        }

        @Override
        boolean isValid() {
            return valid;
        }

        void setValid(boolean valid) {
            this.valid = valid;
        }

        TextField getValueField() {
            return valueField;
        }
    }

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("selects default name when provided")
        void shouldSelectDefaultName() {
            TestRow row = new TestRow(List.of("alpha", "beta", "gamma"), "beta", () -> {});
            assertThat(row.getSelectedName()).isEqualTo("beta");
        }

        @Test
        @DisplayName("selects first name when no default provided")
        void shouldSelectFirstWhenNoDefault() {
            TestRow row = new TestRow(List.of("alpha", "beta"), null, () -> {});
            assertThat(row.getSelectedName()).isEqualTo("alpha");
        }

        @Test
        @DisplayName("handles empty name list")
        void shouldHandleEmptyNameList() {
            TestRow row = new TestRow(List.of(), null, () -> {});
            assertThat(row.isNameSelected()).isFalse();
            assertThat(row.getSelectedName()).isNull();
        }
    }

    @Nested
    @DisplayName("wireFieldChange")
    class WireFieldChange {

        @Test
        @DisplayName("triggers onChange callback when field text changes")
        void shouldTriggerOnChange() {
            AtomicInteger counter = new AtomicInteger(0);
            TestRow row = new TestRow(List.of("x"), null, counter::incrementAndGet);
            row.getValueField().setText("new value");
            assertThat(counter.get()).isGreaterThanOrEqualTo(1);
        }
    }

    @Nested
    @DisplayName("buildPane")
    class BuildPane {

        @Test
        @DisplayName("pane contains nameCombo and custom fields")
        void shouldBuildPaneWithFields() {
            TestRow row = new TestRow(List.of("param1"), null, () -> {});
            // nameCombo + Label("Val:") + valueField + removeBtn = 4 children
            assertThat(row.getPane().getChildren()).hasSize(4);
        }
    }

    @Nested
    @DisplayName("isNameSelected and getSelectedName")
    class NameSelection {

        @Test
        @DisplayName("isNameSelected returns true when a name is selected")
        void shouldReturnTrueWhenSelected() {
            TestRow row = new TestRow(List.of("x"), null, () -> {});
            assertThat(row.isNameSelected()).isTrue();
        }

        @Test
        @DisplayName("getSelectedName returns the selected name")
        void shouldReturnSelectedName() {
            TestRow row = new TestRow(List.of("myParam"), null, () -> {});
            assertThat(row.getSelectedName()).isEqualTo("myParam");
        }
    }

    @Nested
    @DisplayName("createRemoveButton")
    class RemoveButton {

        @Test
        @DisplayName("remove button calls callback on action")
        void shouldCallRemoveCallback() {
            AtomicInteger counter = new AtomicInteger(0);
            ParameterRowBase row = new ParameterRowBase(List.of("a"), null, () -> {}) {
                @Override
                boolean isValid() {
                    return true;
                }
            };
            Button btn = row.createRemoveButton(counter::incrementAndGet);
            btn.fire();
            assertThat(counter.get()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("dialog ParameterRow subclasses extend ParameterRowBase")
    class InheritanceCheck {

        @Test
        @DisplayName("MultiParameterSweepDialog.ParameterRow is a ParameterRowBase")
        void multiSweepRowExtendsBase() {
            // Verify class hierarchy via reflection
            assertThat(ParameterRowBase.class).isAssignableFrom(
                    findInnerClass(MultiParameterSweepDialog.class, "ParameterRow"));
        }

        @Test
        @DisplayName("MonteCarloDialog.ParameterRow is a ParameterRowBase")
        void monteCarloRowExtendsBase() {
            assertThat(ParameterRowBase.class).isAssignableFrom(
                    findInnerClass(MonteCarloDialog.class, "ParameterRow"));
        }

        @Test
        @DisplayName("OptimizerDialog.ParamRow is a ParameterRowBase")
        void optimizerRowExtendsBase() {
            assertThat(ParameterRowBase.class).isAssignableFrom(
                    findInnerClass(OptimizerDialog.class, "ParamRow"));
        }

        private Class<?> findInnerClass(Class<?> outer, String name) {
            for (Class<?> inner : outer.getDeclaredClasses()) {
                if (inner.getSimpleName().equals(name)) {
                    return inner;
                }
            }
            throw new AssertionError("Inner class " + name + " not found in " + outer.getSimpleName());
        }
    }
}
