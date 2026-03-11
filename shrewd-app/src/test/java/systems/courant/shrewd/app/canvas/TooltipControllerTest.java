package systems.courant.shrewd.app.canvas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import systems.courant.shrewd.model.def.ValidationIssue;
import systems.courant.shrewd.model.def.ValidationIssue.Severity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TooltipController")
class TooltipControllerTest {

    @Test
    void shouldBuildSourceCloudTooltip() {
        var hit = new FlowEndpointCalculator.CloudHit(
                "Births", FlowEndpointCalculator.FlowEnd.SOURCE, 0, 0);

        String text = TooltipController.buildCloudTooltipText(hit);

        assertThat(text).startsWith("Source cloud (Births)");
        assertThat(text).contains("Material flows into the model from outside");
    }

    @Test
    void shouldBuildSinkCloudTooltip() {
        var hit = new FlowEndpointCalculator.CloudHit(
                "Deaths", FlowEndpointCalculator.FlowEnd.SINK, 0, 0);

        String text = TooltipController.buildCloudTooltipText(hit);

        assertThat(text).startsWith("Sink cloud (Deaths)");
        assertThat(text).contains("Material flows out of the model to outside");
    }

    @Nested
    @DisplayName("Validation messages in tooltips")
    class ValidationMessages {

        @Test
        void shouldAppendWarningMessage() {
            StringBuilder sb = new StringBuilder("MyVar");
            List<ValidationIssue> issues = List.of(
                    new ValidationIssue(Severity.WARNING, "MyVar",
                            "CLD variable is not connected by any causal link"));

            TooltipController.appendValidationMessages(sb, issues);

            String text = sb.toString();
            assertThat(text).contains("\u26A0 CLD variable is not connected by any causal link");
        }

        @Test
        void shouldAppendErrorMessage() {
            StringBuilder sb = new StringBuilder("BadFlow");
            List<ValidationIssue> issues = List.of(
                    new ValidationIssue(Severity.ERROR, "BadFlow",
                            "Flow has no equation defined"));

            TooltipController.appendValidationMessages(sb, issues);

            String text = sb.toString();
            assertThat(text).contains("\u2716 Flow has no equation defined");
        }

        @Test
        void shouldAppendMultipleMessages() {
            StringBuilder sb = new StringBuilder("Element");
            List<ValidationIssue> issues = List.of(
                    new ValidationIssue(Severity.ERROR, "Element", "Error one"),
                    new ValidationIssue(Severity.WARNING, "Element", "Warning two"));

            TooltipController.appendValidationMessages(sb, issues);

            String text = sb.toString();
            assertThat(text).contains("\u2716 Error one");
            assertThat(text).contains("\u26A0 Warning two");
        }

        @Test
        void shouldNotModifyTextWhenNoIssues() {
            StringBuilder sb = new StringBuilder("Clean");

            TooltipController.appendValidationMessages(sb, List.of());

            assertThat(sb.toString()).isEqualTo("Clean");
        }

        @Test
        void shouldHandleNullIssuesList() {
            StringBuilder sb = new StringBuilder("Null");

            TooltipController.appendValidationMessages(sb, null);

            assertThat(sb.toString()).isEqualTo("Null");
        }
    }
}
