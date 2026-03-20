package systems.courant.sd.io;

import systems.courant.sd.Simulation;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.Stock;
import systems.courant.sd.model.Variable;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static systems.courant.sd.measure.Units.MINUTE;
import static systems.courant.sd.measure.Units.THING;
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
    public void shouldNotOpenFileUntilFirstSimulationEvent() throws IOException {
        Path csvFile = tempDir.resolve("lazy.csv");
        CsvSubscriber csv = new CsvSubscriber(csvFile.toString());
        // File should not exist yet — writer is lazily initialized
        assertTrue(!Files.exists(csvFile), "File should not be created until first write");
        csv.close();
        // After close without any simulation, file should still not exist
        assertTrue(!Files.exists(csvFile), "File should not be created when closed without use");
    }

    @Test
    public void shouldCreateParentDirectories() throws IOException {
        Path nested = tempDir.resolve("a/b/c/output.csv");
        CsvSubscriber csv = new CsvSubscriber(nested.toString());
        csv.close();
        assertTrue(Files.exists(nested.getParent()));
    }

    @Test
    public void shouldNotThrowWhenEventFiredAfterClose() throws IOException {
        Model model = new Model("AfterClose");
        Stock stock = new Stock("S", 50, THING);
        model.addStock(stock);

        String csvPath = tempDir.resolve("afterclose.csv").toString();
        CsvSubscriber csv = new CsvSubscriber(csvPath);

        // Run simulation normally (this closes the writer via handleSimulationEndEvent)
        Simulation sim = new Simulation(model, MINUTE, MINUTE, 2);
        sim.addEventHandler(csv);
        sim.execute();

        // Now run a second simulation with the same (already-closed) subscriber.
        // Before the fix, this would throw NPE in handleSimulationStartEvent/handleTimeStepEvent.
        Simulation sim2 = new Simulation(model, MINUTE, MINUTE, 1);
        sim2.addEventHandler(csv);
        sim2.execute(); // Should not throw
    }

    @Test
    public void shouldLogModelNameUsingParameterizedMessage() throws IOException {
        ch.qos.logback.classic.Logger csvLogger = (ch.qos.logback.classic.Logger)
                LoggerFactory.getLogger(CsvSubscriber.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        csvLogger.addAppender(appender);

        try {
            Model model = new Model("MyModel");
            Stock stock = new Stock("S", 10, THING);
            model.addStock(stock);

            String csvPath = tempDir.resolve("log_test.csv").toString();
            CsvSubscriber csv = new CsvSubscriber(csvPath);

            Simulation sim = new Simulation(model, MINUTE, MINUTE, 1);
            sim.addEventHandler(csv);
            sim.execute();

            List<ILoggingEvent> logs = appender.list;
            ILoggingEvent startEvent = logs.stream()
                    .filter(e -> e.getLevel() == Level.INFO)
                    .filter(e -> e.getFormattedMessage().contains("Starting simulation"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Expected 'Starting simulation' log message"));

            assertEquals("Starting simulation: {}", startEvent.getMessage(),
                    "Log should use parameterized format, not string concatenation");
            assertEquals("MyModel", startEvent.getArgumentArray()[0].toString());
        } finally {
            csvLogger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    public void shouldNotThrowWhenClosedConcurrentlyWithWrite() throws Exception {
        Model model = new Model("Concurrent");
        Stock stock = new Stock("S", 50, THING);
        model.addStock(stock);

        String csvPath = tempDir.resolve("concurrent.csv").toString();
        CsvSubscriber csv = new CsvSubscriber(csvPath);

        Simulation sim = new Simulation(model, MINUTE, MINUTE, 1);
        sim.addEventHandler(csv);

        // Start simulation to initialize the writer
        CountDownLatch started = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Thread closer = new Thread(() -> {
            try {
                started.await();
                csv.close();
            } catch (Throwable t) {
                error.set(t);
            }
        });
        closer.start();
        started.countDown();

        // Run simulation concurrently with close — should not throw NPE
        try {
            sim.execute();
        } catch (CsvOutputException ignored) {
            // Acceptable — writer was closed mid-write
        }
        closer.join(5000);
        if (error.get() != null) {
            throw new AssertionError("Close thread threw", error.get());
        }
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
