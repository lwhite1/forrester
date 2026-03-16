package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ElementType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import systems.courant.sd.app.canvas.dialogs.BindingConfigDialog;
import systems.courant.sd.app.canvas.dialogs.DefinePortsDialog;
import systems.courant.sd.app.canvas.dialogs.ExpressionLanguageDialog;
import systems.courant.sd.app.canvas.dialogs.MonteCarloDialog;
import systems.courant.sd.app.canvas.dialogs.MultiParameterSweepDialog;
import systems.courant.sd.app.canvas.dialogs.OptimizerDialog;
import systems.courant.sd.app.canvas.dialogs.ParameterSweepDialog;
import systems.courant.sd.app.canvas.dialogs.SimulationSettingsDialog;

@DisplayName("HelpContextResolver")
class HelpContextResolverTest {

    // Convenience: resolve with all flags false and no selection/tool
    private static HelpTopic resolveDefault() {
        return HelpContextResolver.resolve(false, null, CanvasToolBar.Tool.SELECT,
                false, false, false, false);
    }

    @Nested
    @DisplayName("Priority chain")
    class PriorityChain {

        @Test
        void shouldReturnOverviewWhenNothingActive() {
            assertThat(resolveDefault()).isEqualTo(HelpTopic.OVERVIEW);
        }

        @Test
        void shouldReturnExpressionLanguageWhenEquationFocused() {
            HelpTopic result = HelpContextResolver.resolve(
                    true,                           // equation focused
                    ElementType.STOCK,              // also has selection
                    CanvasToolBar.Tool.PLACE_FLOW,  // also has tool
                    true,                           // also loop active
                    true,                           // also trace active
                    true,                           // also inside module
                    true);                          // also dashboard selected
            assertThat(result).isEqualTo(HelpTopic.EXPRESSION_LANGUAGE);
        }

        @Test
        void shouldReturnElementTypeWhenSingleElementSelected() {
            HelpTopic result = HelpContextResolver.resolve(
                    false, ElementType.STOCK, CanvasToolBar.Tool.SELECT,
                    false, false, false, false);
            assertThat(result).isEqualTo(HelpTopic.STOCK);
        }

        @Test
        void shouldPrioritizeSelectionOverTool() {
            HelpTopic result = HelpContextResolver.resolve(
                    false, ElementType.FLOW, CanvasToolBar.Tool.PLACE_STOCK,
                    false, false, false, false);
            assertThat(result).isEqualTo(HelpTopic.FLOW);
        }

        @Test
        void shouldReturnToolTopicWhenToolActiveAndNoSelection() {
            HelpTopic result = HelpContextResolver.resolve(
                    false, null, CanvasToolBar.Tool.PLACE_STOCK,
                    false, false, false, false);
            assertThat(result).isEqualTo(HelpTopic.STOCK);
        }

        @Test
        void shouldReturnFeedbackLoopsWhenLoopHighlightActive() {
            HelpTopic result = HelpContextResolver.resolve(
                    false, null, CanvasToolBar.Tool.SELECT,
                    true, false, false, false);
            assertThat(result).isEqualTo(HelpTopic.FEEDBACK_LOOPS);
        }

        @Test
        void shouldPrioritizeLoopHighlightOverCausalTrace() {
            HelpTopic result = HelpContextResolver.resolve(
                    false, null, CanvasToolBar.Tool.SELECT,
                    true, true, false, false);
            assertThat(result).isEqualTo(HelpTopic.FEEDBACK_LOOPS);
        }

        @Test
        void shouldReturnCausalTraceWhenTraceActive() {
            HelpTopic result = HelpContextResolver.resolve(
                    false, null, CanvasToolBar.Tool.SELECT,
                    false, true, false, false);
            assertThat(result).isEqualTo(HelpTopic.CAUSAL_TRACE);
        }

        @Test
        void shouldReturnModuleWhenInsideModule() {
            HelpTopic result = HelpContextResolver.resolve(
                    false, null, CanvasToolBar.Tool.SELECT,
                    false, false, true, false);
            assertThat(result).isEqualTo(HelpTopic.MODULE);
        }

        @Test
        void shouldReturnSimulationResultsWhenDashboardSelected() {
            HelpTopic result = HelpContextResolver.resolve(
                    false, null, CanvasToolBar.Tool.SELECT,
                    false, false, false, true);
            assertThat(result).isEqualTo(HelpTopic.SIMULATION_RESULTS);
        }

        @Test
        void shouldPrioritizeInsideModuleOverDashboard() {
            HelpTopic result = HelpContextResolver.resolve(
                    false, null, CanvasToolBar.Tool.SELECT,
                    false, false, true, true);
            assertThat(result).isEqualTo(HelpTopic.MODULE);
        }
    }

    @Nested
    @DisplayName("Element type mapping")
    class ElementTypeMapping {

        @Test
        void shouldMapStockToStockTopic() {
            assertThat(HelpContextResolver.topicForElementType(ElementType.STOCK))
                    .isEqualTo(HelpTopic.STOCK);
        }

        @Test
        void shouldMapFlowToFlowTopic() {
            assertThat(HelpContextResolver.topicForElementType(ElementType.FLOW))
                    .isEqualTo(HelpTopic.FLOW);
        }

        @Test
        void shouldMapAuxToVariableTopic() {
            assertThat(HelpContextResolver.topicForElementType(ElementType.AUX))
                    .isEqualTo(HelpTopic.VARIABLE);
        }

        @Test
        void shouldMapLookupToLookupTopic() {
            assertThat(HelpContextResolver.topicForElementType(ElementType.LOOKUP))
                    .isEqualTo(HelpTopic.LOOKUP);
        }

        @Test
        void shouldMapModuleToModuleTopic() {
            assertThat(HelpContextResolver.topicForElementType(ElementType.MODULE))
                    .isEqualTo(HelpTopic.MODULE);
        }

        @Test
        void shouldMapCldVariableToCldVariableTopic() {
            assertThat(HelpContextResolver.topicForElementType(ElementType.CLD_VARIABLE))
                    .isEqualTo(HelpTopic.CLD_VARIABLE);
        }

        @Test
        void shouldMapCommentToCommentTopic() {
            assertThat(HelpContextResolver.topicForElementType(ElementType.COMMENT))
                    .isEqualTo(HelpTopic.COMMENT);
        }

        @Test
        void shouldReturnNullForNullType() {
            assertThat(HelpContextResolver.topicForElementType(null)).isNull();
        }
    }

    @Nested
    @DisplayName("Tool mapping")
    class ToolMapping {

        @Test
        void shouldReturnNullForSelectTool() {
            assertThat(HelpContextResolver.topicForTool(CanvasToolBar.Tool.SELECT)).isNull();
        }

        @Test
        void shouldReturnNullForNullTool() {
            assertThat(HelpContextResolver.topicForTool(null)).isNull();
        }

        @Test
        void shouldMapPlaceStockToStockTopic() {
            assertThat(HelpContextResolver.topicForTool(CanvasToolBar.Tool.PLACE_STOCK))
                    .isEqualTo(HelpTopic.STOCK);
        }

        @Test
        void shouldMapPlaceFlowToFlowTopic() {
            assertThat(HelpContextResolver.topicForTool(CanvasToolBar.Tool.PLACE_FLOW))
                    .isEqualTo(HelpTopic.FLOW);
        }

        @Test
        void shouldMapPlaceAuxToVariableTopic() {
            assertThat(HelpContextResolver.topicForTool(CanvasToolBar.Tool.PLACE_VARIABLE))
                    .isEqualTo(HelpTopic.VARIABLE);
        }

        @Test
        void shouldMapPlaceCausalLinkToCausalLoopsTopic() {
            assertThat(HelpContextResolver.topicForTool(CanvasToolBar.Tool.PLACE_CAUSAL_LINK))
                    .isEqualTo(HelpTopic.CAUSAL_LOOPS);
        }

        @Test
        void shouldMapPlaceCommentToCommentTopic() {
            assertThat(HelpContextResolver.topicForTool(CanvasToolBar.Tool.PLACE_COMMENT))
                    .isEqualTo(HelpTopic.COMMENT);
        }

        @Test
        void shouldMapAllPlaceToolsToNonNullTopic() {
            for (CanvasToolBar.Tool tool : CanvasToolBar.Tool.values()) {
                if (tool != CanvasToolBar.Tool.SELECT) {
                    assertThat(HelpContextResolver.topicForTool(tool))
                            .as("Tool " + tool + " should map to a topic")
                            .isNotNull();
                }
            }
        }
    }

    @Nested
    @DisplayName("Dialog mapping")
    class DialogMapping {

        @Test
        void shouldMapSimulationSettingsDialog() {
            assertThat(HelpContextResolver.topicForDialog("SimulationSettingsDialog"))
                    .isEqualTo(HelpTopic.SIMULATION_SETTINGS);
        }

        @Test
        void shouldMapMonteCarloDialog() {
            assertThat(HelpContextResolver.topicForDialog("MonteCarloDialog"))
                    .isEqualTo(HelpTopic.MONTE_CARLO);
        }

        @Test
        void shouldMapOptimizerDialog() {
            assertThat(HelpContextResolver.topicForDialog("OptimizerDialog"))
                    .isEqualTo(HelpTopic.OPTIMIZATION);
        }

        @Test
        void shouldMapParameterSweepDialog() {
            assertThat(HelpContextResolver.topicForDialog("ParameterSweepDialog"))
                    .isEqualTo(HelpTopic.PARAMETER_SWEEP);
        }

        @Test
        void shouldMapMultiParameterSweepDialog() {
            assertThat(HelpContextResolver.topicForDialog("MultiParameterSweepDialog"))
                    .isEqualTo(HelpTopic.MULTI_SWEEP);
        }

        @Test
        void shouldMapDefinePortsDialog() {
            assertThat(HelpContextResolver.topicForDialog("DefinePortsDialog"))
                    .isEqualTo(HelpTopic.MODULE_PORTS);
        }

        @Test
        void shouldMapBindingConfigDialog() {
            assertThat(HelpContextResolver.topicForDialog("BindingConfigDialog"))
                    .isEqualTo(HelpTopic.MODULE_PORTS);
        }

        @Test
        void shouldMapExpressionLanguageDialog() {
            assertThat(HelpContextResolver.topicForDialog("ExpressionLanguageDialog"))
                    .isEqualTo(HelpTopic.EXPRESSION_LANGUAGE);
        }

        @Test
        void shouldMapCalibrateDialog() {
            assertThat(HelpContextResolver.topicForDialog("CalibrateDialog"))
                    .isEqualTo(HelpTopic.CALIBRATION);
        }

        @Test
        void shouldMapValidationDialog() {
            assertThat(HelpContextResolver.topicForDialog("ValidationDialog"))
                    .isEqualTo(HelpTopic.VALIDATION);
        }

        @Test
        void shouldMapExtremeConditionDialog() {
            assertThat(HelpContextResolver.topicForDialog("ExtremeConditionDialog"))
                    .isEqualTo(HelpTopic.EXTREME_CONDITION);
        }

        @Test
        void shouldMapColumnMappingDialog() {
            assertThat(HelpContextResolver.topicForDialog("ColumnMappingDialog"))
                    .isEqualTo(HelpTopic.COLUMN_MAPPING);
        }

        @Test
        void shouldReturnOverviewForUnknownDialog() {
            assertThat(HelpContextResolver.topicForDialog("UnknownDialog"))
                    .isEqualTo(HelpTopic.OVERVIEW);
        }
    }
}
