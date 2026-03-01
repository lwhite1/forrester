package com.deathrayresearch.forrester.io;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.deathrayresearch.forrester.measure.Units.MINUTE;
import static com.deathrayresearch.forrester.measure.Units.THING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CsvSubscriberTest {

    @TempDir
    Path tempDir;

    @Test
    public void shouldWriteHeaderAndDataRows() throws IOException {
        Model model = new Model("Test");
        Stock stock = new Stock("Inventory", 100, THING);
        model.addStock(stock);

        String csvPath = tempDir.resolve("output.csv").toString();
        CsvSubscriber csv = new CsvSubscriber(csvPath);

        Simulation sim = new Simulation(model, MINUTE, MINUTE, 2);
        sim.addEventHandler(csv);
        sim.execute();

        List<String> lines = Files.readAllLines(tempDir.resolve("output.csv"));
        // Header + 3 data rows (steps 0, 1, 2)
        assertEquals(4, lines.size());
        // Header should contain stock name
        assertTrue(lines.get(0).contains("Inventory"));
        // Header should contain Step and Date time
        assertTrue(lines.get(0).contains("Step"));
    }

    @Test
    public void shouldIncludeVariableColumnsInHeader() throws IOException {
        Model model = new Model("VarTest");
        Stock stock = new Stock("S", 100, THING);
        model.addStock(stock);
        Variable var = new Variable("Level", THING, stock::getValue);
        model.addVariable(var);

        String csvPath = tempDir.resolve("var_output.csv").toString();
        CsvSubscriber csv = new CsvSubscriber(csvPath);

        Simulation sim = new Simulation(model, MINUTE, MINUTE, 1);
        sim.addEventHandler(csv);
        sim.execute();

        List<String> lines = Files.readAllLines(tempDir.resolve("var_output.csv"));
        assertTrue(lines.get(0).contains("Level"));
    }

    @Test
    public void shouldWriteCorrectStepNumbers() throws IOException {
        Model model = new Model("Steps");
        Stock stock = new Stock("S", 50, THING);
        model.addStock(stock);

        String csvPath = tempDir.resolve("steps.csv").toString();
        CsvSubscriber csv = new CsvSubscriber(csvPath);

        Simulation sim = new Simulation(model, MINUTE, MINUTE, 3);
        sim.addEventHandler(csv);
        sim.execute();

        List<String> lines = Files.readAllLines(tempDir.resolve("steps.csv"));
        // Data rows should start with step numbers 0, 1, 2, 3
        assertTrue(lines.get(1).startsWith("\"0\""));
        assertTrue(lines.get(2).startsWith("\"1\""));
        assertTrue(lines.get(3).startsWith("\"2\""));
        assertTrue(lines.get(4).startsWith("\"3\""));
    }

    @Test
    public void shouldBeCloseable() throws IOException {
        String csvPath = tempDir.resolve("closeable.csv").toString();
        CsvSubscriber csv = new CsvSubscriber(csvPath);
        csv.close(); // Should not throw
        csv.close(); // Double-close should be safe
    }

    @Test
    public void shouldCreateParentDirectories() throws IOException {
        Path nested = tempDir.resolve("a/b/c/output.csv");
        CsvSubscriber csv = new CsvSubscriber(nested.toString());
        csv.close();
        assertTrue(Files.exists(nested.getParent()));
    }

    @Test
    public void shouldRecordStockValuesWithFlowChanges() throws IOException {
        Model model = new Model("Flow");
        Stock tank = new Stock("Tank", 100, THING);
        Flow drain = Flow.create("Drain", MINUTE, () -> new Quantity(10, THING));
        tank.addOutflow(drain);
        model.addStock(tank);

        String csvPath = tempDir.resolve("flow.csv").toString();
        CsvSubscriber csv = new CsvSubscriber(csvPath);

        Simulation sim = new Simulation(model, MINUTE, MINUTE, 2);
        sim.addEventHandler(csv);
        sim.execute();

        List<String> lines = Files.readAllLines(tempDir.resolve("flow.csv"));
        // Should have header + 3 data rows
        assertEquals(4, lines.size());
    }
}
