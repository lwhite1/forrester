package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Where Used and Uses queries (#410)")
@ExtendWith(ApplicationExtension.class)
class WhereUsedFxTest {

    private ModelCanvas canvas;

    @Start
    void start(Stage stage) {
        canvas = new ModelCanvas(new Clipboard());

        ModelDefinition def = new ModelDefinitionBuilder()
                .name("SIR")
                .stock("Susceptible", 1000, "Person")
                .stock("Infectious", 10, "Person")
                .stock("Recovered", 0, "Person")
                .constant("Contact Rate", 8, "Dimensionless unit")
                .constant("Duration", 5, "Day")
                .flow("Infection",
                        "Contact_Rate * Infectious * Susceptible"
                                + " / (Susceptible + Infectious + Recovered)",
                        "Day", "Susceptible", "Infectious")
                .flow("Recovery", "Infectious / Duration",
                        "Day", "Infectious", "Recovered")
                .build();

        ModelEditor editor = new ModelEditor();
        editor.loadFrom(def);

        CanvasState state = new CanvasState();
        state.addElement("Susceptible", ElementType.STOCK, 100, 200);
        state.addElement("Infectious", ElementType.STOCK, 300, 200);
        state.addElement("Recovered", ElementType.STOCK, 500, 200);
        state.addElement("Contact Rate", ElementType.AUX, 200, 50);
        state.addElement("Duration", ElementType.AUX, 400, 50);
        state.addElement("Infection", ElementType.FLOW, 200, 200);
        state.addElement("Recovery", ElementType.FLOW, 400, 200);

        canvas.setModel(editor, state.toViewDef());
        stage.setScene(new Scene(new StackPane(canvas), 800, 600));
        stage.show();
    }

    @Test
    @DisplayName("whereUsed returns elements that reference the given element")
    void shouldReturnDependentsForWhereUsed() {
        Set<String> whereUsed = canvas.analysis().whereUsed("Contact Rate");
        assertThat(whereUsed).contains("Infection");
    }

    @Test
    @DisplayName("whereUsed returns multiple dependents for a shared variable")
    void shouldReturnMultipleDependentsForSharedVariable() {
        Set<String> whereUsed = canvas.analysis().whereUsed("Infectious");
        assertThat(whereUsed).contains("Infection", "Recovery");
    }

    @Test
    @DisplayName("uses returns elements referenced by the given element's equation")
    void shouldReturnDependenciesForUses() {
        Set<String> uses = canvas.analysis().uses("Recovery");
        assertThat(uses).contains("Infectious", "Duration");
    }

    @Test
    @DisplayName("uses returns empty set for a constant")
    void shouldReturnEmptyUsesForConstant() {
        Set<String> uses = canvas.analysis().uses("Contact Rate");
        assertThat(uses).isEmpty();
    }

    @Test
    @DisplayName("showWhereUsed selects all dependent elements")
    void shouldSelectDependentsOnShowWhereUsed() {
        canvas.analysis().showWhereUsed("Duration");

        Set<String> selected = canvas.getSelectedElementNames();
        assertThat(selected).contains("Recovery");
    }

    @Test
    @DisplayName("showUses selects all dependency elements")
    void shouldSelectDependenciesOnShowUses() {
        canvas.analysis().showUses("Recovery");

        Set<String> selected = canvas.getSelectedElementNames();
        assertThat(selected).contains("Infectious", "Duration");
    }
}
