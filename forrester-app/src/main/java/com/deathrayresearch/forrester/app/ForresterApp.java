package com.deathrayresearch.forrester.app;

import com.deathrayresearch.forrester.app.canvas.CanvasToolBar;
import com.deathrayresearch.forrester.app.canvas.ModelCanvas;
import com.deathrayresearch.forrester.app.canvas.ModelEditor;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;
import com.deathrayresearch.forrester.model.def.ViewDef;
import com.deathrayresearch.forrester.model.graph.AutoLayout;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/**
 * JavaFX entry point for the Forrester Design 4 application.
 * Phase 3: interactive editor with element creation/deletion via a toolbar.
 */
public class ForresterApp extends Application {

    @Override
    public void start(Stage stage) {
        ModelDefinition sir = buildSirModel();
        ViewDef view = AutoLayout.layout(sir);

        ModelEditor editor = new ModelEditor();
        editor.loadFrom(sir);

        ModelCanvas canvas = new ModelCanvas();

        CanvasToolBar toolBar = new CanvasToolBar();
        toolBar.setOnToolChanged(canvas::setActiveTool);
        canvas.setToolBar(toolBar);

        canvas.setModel(editor, view);

        // Wrap canvas in a Pane so it can bind to available space in the center region
        Pane canvasPane = new Pane(canvas);
        canvas.widthProperty().bind(canvasPane.widthProperty());
        canvas.heightProperty().bind(canvasPane.heightProperty());

        BorderPane root = new BorderPane();
        root.setTop(toolBar);
        root.setCenter(canvasPane);

        Scene scene = new Scene(root, 1200, 800);
        stage.setTitle("Forrester — SIR Model");
        stage.setScene(scene);
        stage.show();

        // Ensure canvas gets focus for keyboard events
        canvas.requestFocus();
    }

    private static ModelDefinition buildSirModel() {
        return new ModelDefinitionBuilder()
                .name("SIR Infectious Disease")
                .stock("Susceptible", 1000, "people")
                .stock("Infectious", 10, "people")
                .stock("Recovered", 0, "people")
                .constant("Contact Rate", 8, "contacts/person/day")
                .constant("Infectivity", 0.10, "dimensionless")
                .constant("Recovery Fraction", 0.20, "1/day")
                .flow("Infection",
                        "Contact_Rate * Infectivity * Infectious"
                                + " / (Susceptible + Infectious + Recovered) * Susceptible",
                        "day", "Susceptible", "Infectious")
                .flow("Recovery",
                        "Infectious * Recovery_Fraction",
                        "day", "Infectious", "Recovered")
                .build();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
