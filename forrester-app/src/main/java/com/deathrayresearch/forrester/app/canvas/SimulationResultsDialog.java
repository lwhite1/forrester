package com.deathrayresearch.forrester.app.canvas;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.List;

/**
 * A separate window that displays simulation results in a {@link TableView}.
 * Each column corresponds to "Step" or a stock/variable name, and each row
 * contains the values at that time step.
 */
public class SimulationResultsDialog extends Stage {

    public SimulationResultsDialog(SimulationRunner.SimulationResult result) {
        setTitle("Simulation Results");

        TableView<double[]> table = new TableView<>();
        List<String> columns = result.columnNames();

        for (int c = 0; c < columns.size(); c++) {
            final int colIndex = c;
            TableColumn<double[], String> col = new TableColumn<>(columns.get(c));
            col.setCellValueFactory(data -> {
                double[] row = data.getValue();
                if (colIndex < row.length) {
                    double val = row[colIndex];
                    if (colIndex == 0) {
                        // Step column — show as integer
                        return new SimpleStringProperty(String.valueOf((int) val));
                    }
                    return new SimpleStringProperty(formatNumber(val));
                }
                return new SimpleStringProperty("");
            });
            col.setPrefWidth(colIndex == 0 ? 60 : 120);
            table.getColumns().add(col);
        }

        table.setItems(FXCollections.observableArrayList(result.rows()));

        BorderPane root = new BorderPane(table);
        Scene scene = new Scene(root, 800, 500);
        setScene(scene);
    }

    private static String formatNumber(double value) {
        if (value == Math.floor(value) && Double.isFinite(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.4f", value);
    }
}
