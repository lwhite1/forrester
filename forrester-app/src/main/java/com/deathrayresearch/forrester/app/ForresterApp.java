package com.deathrayresearch.forrester.app;

import com.deathrayresearch.forrester.app.canvas.ModelCanvas;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;
import com.deathrayresearch.forrester.model.def.ViewDef;
import com.deathrayresearch.forrester.model.graph.AutoLayout;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

/**
 * JavaFX entry point for the Forrester Design 4 application.
 * Phase 1: renders a static SIR model using the Layered Flow Diagram visual language.
 */
public class ForresterApp extends Application {

    @Override
    public void start(Stage stage) {
        ModelDefinition sir = buildSirModel();
        ViewDef view = AutoLayout.layout(sir);

        ModelCanvas canvas = new ModelCanvas();
        canvas.setModel(sir, view);

        Pane root = new Pane(canvas);
        canvas.widthProperty().bind(root.widthProperty());
        canvas.heightProperty().bind(root.heightProperty());

        Scene scene = new Scene(root, 1200, 800);
        stage.setTitle("Forrester — SIR Model");
        stage.setScene(scene);
        stage.show();
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
