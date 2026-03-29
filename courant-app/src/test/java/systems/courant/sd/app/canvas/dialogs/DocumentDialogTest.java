package systems.courant.sd.app.canvas.dialogs;

import systems.courant.sd.app.canvas.ModelEditor;
import systems.courant.sd.app.canvas.dialogs.DocumentDialog.ElementRow;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;

import javafx.application.Platform;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DocumentDialog")
@ExtendWith(ApplicationExtension.class)
class DocumentDialogTest {

    @Start
    void start(Stage stage) {
        // No-op — tests create their own DocumentDialog instances
    }

    private ModelEditor editorWith(ModelDefinition def) {
        ModelEditor editor = new ModelEditor();
        editor.loadFrom(def);
        return editor;
    }

    private ModelDefinition sampleModel() {
        ModelDefinition innerDef = new ModelDefinitionBuilder()
                .name("Inner")
                .variable("x", "1", "")
                .build();

        return new ModelDefinitionBuilder()
                .name("Test Model")
                .stock("Population", 1000, "people")
                .stock("Pollution", 0, "tons")
                .flow("Birth Rate", "Population * birth_fraction", "year",
                        null, "Population")
                .variable("birth_fraction", "0.03", "1/year")
                .variable("net_growth", "Birth Rate - death_rate", "people/year")
                .lookupTable("effect_of_pollution",
                        new double[]{0, 1, 2, 3}, new double[]{1.0, 0.8, 0.5, 0.1},
                        "LINEAR")
                .module("Inner Module", innerDef, Map.of(), Map.of())
                .build();
    }

    @Nested
    @DisplayName("buildRows")
    class BuildRowsTests {

        @Test
        @DisplayName("should include all element types")
        void shouldIncludeAllElementTypes() {
            ModelEditor editor = editorWith(sampleModel());
            List<ElementRow> rows = DocumentDialog.buildRows(editor);

            assertThat(rows).extracting(ElementRow::type)
                    .contains("Stock", "Flow", "Constant", "Variable", "Lookup Table", "Module");
        }

        @Test
        @DisplayName("should return rows sorted by name (case-insensitive)")
        void shouldSortByName() {
            ModelEditor editor = editorWith(sampleModel());
            List<ElementRow> rows = DocumentDialog.buildRows(editor);

            List<String> names = rows.stream().map(ElementRow::name).toList();
            List<String> sorted = names.stream()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
            assertThat(names).isEqualTo(sorted);
        }

        @Test
        @DisplayName("should show stock initial value as equation")
        void shouldShowStockInitialValue() {
            ModelEditor editor = editorWith(sampleModel());
            List<ElementRow> rows = DocumentDialog.buildRows(editor);

            ElementRow pop = rows.stream()
                    .filter(r -> r.name().equals("Population"))
                    .findFirst().orElseThrow();
            assertThat(pop.type()).isEqualTo("Stock");
            assertThat(pop.equation()).isEqualTo("1000.0");
        }

        @Test
        @DisplayName("should show flow equation")
        void shouldShowFlowEquation() {
            ModelEditor editor = editorWith(sampleModel());
            List<ElementRow> rows = DocumentDialog.buildRows(editor);

            ElementRow flow = rows.stream()
                    .filter(r -> r.name().equals("Birth Rate"))
                    .findFirst().orElseThrow();
            assertThat(flow.type()).isEqualTo("Flow");
            assertThat(flow.equation()).isEqualTo("Population * birth_fraction");
        }

        @Test
        @DisplayName("should classify literal variables as Constant")
        void shouldClassifyLiteralAsConstant() {
            ModelEditor editor = editorWith(sampleModel());
            List<ElementRow> rows = DocumentDialog.buildRows(editor);

            ElementRow constant = rows.stream()
                    .filter(r -> r.name().equals("birth_fraction"))
                    .findFirst().orElseThrow();
            assertThat(constant.type()).isEqualTo("Constant");
        }

        @Test
        @DisplayName("should classify formula variables as Variable")
        void shouldClassifyFormulaAsVariable() {
            ModelEditor editor = editorWith(sampleModel());
            List<ElementRow> rows = DocumentDialog.buildRows(editor);

            ElementRow variable = rows.stream()
                    .filter(r -> r.name().equals("net_growth"))
                    .findFirst().orElseThrow();
            assertThat(variable.type()).isEqualTo("Variable");
        }

        @Test
        @DisplayName("should show lookup table summary")
        void shouldShowLookupSummary() {
            ModelEditor editor = editorWith(sampleModel());
            List<ElementRow> rows = DocumentDialog.buildRows(editor);

            ElementRow lookup = rows.stream()
                    .filter(r -> r.name().equals("effect_of_pollution"))
                    .findFirst().orElseThrow();
            assertThat(lookup.type()).isEqualTo("Lookup Table");
            assertThat(lookup.equation()).contains("4 points").contains("LINEAR");
        }

        @Test
        @DisplayName("should show module instance")
        void shouldShowModuleInstance() {
            ModelEditor editor = editorWith(sampleModel());
            List<ElementRow> rows = DocumentDialog.buildRows(editor);

            ElementRow module = rows.stream()
                    .filter(r -> r.name().equals("Inner Module"))
                    .findFirst().orElseThrow();
            assertThat(module.type()).isEqualTo("Module");
            assertThat(module.equation()).isEqualTo("Inner");
        }

        @Test
        @DisplayName("should return empty list for empty model")
        void shouldReturnEmptyForEmptyModel() {
            ModelDefinition empty = new ModelDefinitionBuilder()
                    .name("Empty")
                    .build();
            ModelEditor editor = editorWith(empty);
            List<ElementRow> rows = DocumentDialog.buildRows(editor);

            assertThat(rows).isEmpty();
        }
    }

    @Nested
    @DisplayName("Dialog lifecycle")
    class LifecycleTests {

        @Test
        @DisplayName("showOrUpdate creates a new dialog when none is open")
        void shouldCreateNewDialog() {
            assertThat(DocumentDialog.getOpenInstance(null)).isNull();

            ModelEditor editor = editorWith(sampleModel());
            Platform.runLater(() ->
                    DocumentDialog.showOrUpdate(editor, name -> {}, null));
            WaitForAsyncUtils.waitForFxEvents();

            DocumentDialog instance = DocumentDialog.getOpenInstance(null);
            assertThat(instance).isNotNull();
            assertThat(instance.isShowing()).isTrue();

            Platform.runLater(() -> instance.close());
            WaitForAsyncUtils.waitForFxEvents();
        }

        @Test
        @DisplayName("showOrUpdate reuses existing dialog")
        void shouldReuseExistingDialog() {
            ModelEditor editor = editorWith(sampleModel());
            Platform.runLater(() ->
                    DocumentDialog.showOrUpdate(editor, name -> {}, null));
            WaitForAsyncUtils.waitForFxEvents();

            DocumentDialog first = DocumentDialog.getOpenInstance(null);

            Platform.runLater(() ->
                    DocumentDialog.showOrUpdate(editor, name -> {}, null));
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(DocumentDialog.getOpenInstance(null)).isSameAs(first);

            Platform.runLater(() -> first.close());
            WaitForAsyncUtils.waitForFxEvents();
        }

        @Test
        @DisplayName("closing clears the open instance")
        void shouldClearOnClose() {
            ModelEditor editor = editorWith(sampleModel());
            Platform.runLater(() ->
                    DocumentDialog.showOrUpdate(editor, name -> {}, null));
            WaitForAsyncUtils.waitForFxEvents();

            DocumentDialog instance = DocumentDialog.getOpenInstance(null);
            Platform.runLater(() -> instance.close());
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(DocumentDialog.getOpenInstance(null)).isNull();
        }
    }

    @Nested
    @DisplayName("Filtering")
    class FilteringTests {

        @Test
        @DisplayName("should filter by name search text")
        void shouldFilterByName() {
            ModelEditor editor = editorWith(sampleModel());
            final DocumentDialog[] holder = new DocumentDialog[1];

            Platform.runLater(() -> {
                holder[0] = new DocumentDialog(editor, name -> {}, null);
                holder[0].show();
            });
            WaitForAsyncUtils.waitForFxEvents();

            DocumentDialog dialog = holder[0];

            Platform.runLater(() -> dialog.getSearchField().setText("birth"));
            WaitForAsyncUtils.waitForFxEvents();

            List<ElementRow> visible = dialog.getVisibleRows();
            assertThat(visible).allSatisfy(row ->
                    assertThat(row.name().toLowerCase()).contains("birth"));

            Platform.runLater(() -> dialog.close());
            WaitForAsyncUtils.waitForFxEvents();
        }

        @Test
        @DisplayName("should filter by type")
        void shouldFilterByType() {
            ModelEditor editor = editorWith(sampleModel());
            final DocumentDialog[] holder = new DocumentDialog[1];

            Platform.runLater(() -> {
                holder[0] = new DocumentDialog(editor, name -> {}, null);
                holder[0].show();
            });
            WaitForAsyncUtils.waitForFxEvents();

            DocumentDialog dialog = holder[0];

            Platform.runLater(() -> dialog.getTypeFilter().setValue("Stock"));
            WaitForAsyncUtils.waitForFxEvents();

            List<ElementRow> visible = dialog.getVisibleRows();
            assertThat(visible).allSatisfy(row ->
                    assertThat(row.type()).isEqualTo("Stock"));
            assertThat(visible).hasSize(2); // Population, Pollution

            Platform.runLater(() -> dialog.close());
            WaitForAsyncUtils.waitForFxEvents();
        }

        @Test
        @DisplayName("should combine name and type filters")
        void shouldCombineFilters() {
            ModelEditor editor = editorWith(sampleModel());
            final DocumentDialog[] holder = new DocumentDialog[1];

            Platform.runLater(() -> {
                holder[0] = new DocumentDialog(editor, name -> {}, null);
                holder[0].show();
            });
            WaitForAsyncUtils.waitForFxEvents();

            DocumentDialog dialog = holder[0];

            Platform.runLater(() -> {
                dialog.getSearchField().setText("pop");
                dialog.getTypeFilter().setValue("Stock");
            });
            WaitForAsyncUtils.waitForFxEvents();

            List<ElementRow> visible = dialog.getVisibleRows();
            assertThat(visible).hasSize(1);
            assertThat(visible.getFirst().name()).isEqualTo("Population");

            Platform.runLater(() -> dialog.close());
            WaitForAsyncUtils.waitForFxEvents();
        }

        @Test
        @DisplayName("clearing filters should show all elements")
        void shouldShowAllWhenFiltersCleared() {
            ModelEditor editor = editorWith(sampleModel());
            final DocumentDialog[] holder = new DocumentDialog[1];

            Platform.runLater(() -> {
                holder[0] = new DocumentDialog(editor, name -> {}, null);
                holder[0].show();
            });
            WaitForAsyncUtils.waitForFxEvents();

            DocumentDialog dialog = holder[0];
            int totalRows = dialog.getVisibleRows().size();

            Platform.runLater(() -> {
                dialog.getSearchField().setText("Population");
                dialog.getTypeFilter().setValue("Stock");
            });
            WaitForAsyncUtils.waitForFxEvents();
            assertThat(dialog.getVisibleRows().size()).isLessThan(totalRows);

            Platform.runLater(() -> {
                dialog.getSearchField().clear();
                dialog.getTypeFilter().setValue("All Types");
            });
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(dialog.getVisibleRows()).hasSize(totalRows);

            Platform.runLater(() -> dialog.close());
            WaitForAsyncUtils.waitForFxEvents();
        }
    }

    @Nested
    @DisplayName("Element selection")
    class SelectionTests {

        @Test
        @DisplayName("should invoke callback when row is selected")
        void shouldInvokeCallbackOnSelect() {
            ModelEditor editor = editorWith(sampleModel());
            String[] selected = {null};
            final DocumentDialog[] holder = new DocumentDialog[1];

            Platform.runLater(() -> {
                holder[0] = new DocumentDialog(editor, name -> selected[0] = name, null);
                holder[0].show();
            });
            WaitForAsyncUtils.waitForFxEvents();

            DocumentDialog dialog = holder[0];

            Platform.runLater(() -> dialog.getTable().getSelectionModel().select(0));
            WaitForAsyncUtils.waitForFxEvents();

            assertThat(selected[0]).isNotNull();

            Platform.runLater(() -> dialog.close());
            WaitForAsyncUtils.waitForFxEvents();
        }
    }
}
