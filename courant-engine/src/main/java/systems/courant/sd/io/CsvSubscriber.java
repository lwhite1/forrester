package systems.courant.sd.io;

import systems.courant.sd.event.EventHandler;
import systems.courant.sd.event.SimulationEndEvent;
import systems.courant.sd.event.SimulationStartEvent;
import systems.courant.sd.event.TimeStepEvent;
import systems.courant.sd.model.Model;

import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * An {@link EventHandler} that writes simulation output to a CSV file.
 * Each row contains the step number, timestamp, stock values, and variable values.
 *
 * <p>Implements {@link Closeable} so callers can ensure the writer is closed even if
 * the simulation throws before firing {@link SimulationEndEvent}.
 */
public class CsvSubscriber implements EventHandler, Closeable {

    private static final Logger logger = LoggerFactory.getLogger(CsvSubscriber.class);

    private final String filePath;
    private CSVWriter csvWriter;
    private boolean closed;

    /**
     * Creates a new CSV subscriber that writes to the specified file path.
     * Parent directories are created if they do not exist. The file itself is
     * not opened until the first simulation event is received (lazy initialization),
     * so no file handle is leaked if the subscriber is never used.
     *
     * @param fileName the path of the CSV file to write
     */
    public CsvSubscriber(String fileName) {
        this.filePath = fileName;
        File file = Paths.get(fileName).toFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.mkdirs() && !parent.isDirectory()) {
            throw new CsvOutputException("Failed to create directory: " + parent.getAbsolutePath());
        }
    }

    private void ensureWriter() {
        if (csvWriter == null && !closed) {
            try {
                csvWriter = new CSVWriter(new OutputStreamWriter(
                        Files.newOutputStream(Paths.get(filePath)), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new CsvOutputException("Failed to open CSV file: " + filePath, e);
            }
        }
    }

    /**
     * Writes a data row for the current time step, including step number, timestamp,
     * stock values, and variable values.
     */
    @Override
    public void handleTimeStepEvent(TimeStepEvent event) {
        ensureWriter();
        if (csvWriter == null) {
            return;
        }
        Model model = event.getModel();

        List<Double> stockValues = model.getStockValues();
        List<Double> variableValues = model.getVariableValues();

        List<String> values = new ArrayList<>(2 + stockValues.size() + variableValues.size());
        values.add(String.valueOf(event.getStep()));
        values.add(event.getCurrentTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        for (Double stockValue : stockValues) {
            values.add(String.valueOf(stockValue));
        }
        for (Double variableValue : variableValues) {
            values.add(String.valueOf(variableValue));
        }
        csvWriter.writeNext(values.toArray(new String[0]));
        if (csvWriter.checkError()) {
            logger.warn("Failed to write CSV data row at step {}: {}",
                    event.getStep(), csvWriter.getException());
        }
    }

    /**
     * Writes the CSV header row with column names derived from the model's stocks and variables.
     */
    @Override
    public void handleSimulationStartEvent(SimulationStartEvent event) {
        ensureWriter();
        if (csvWriter == null) {
            return;
        }
        logger.info("Starting simulation: {}", event.getModel().getName());

        Model model = event.getModel();
        List<String> stockNames = model.getStockNames();
        List<String> variableNames = model.getVariableNames();

        List<String> values = new ArrayList<>(2 + stockNames.size() + variableNames.size());
        values.add("Step");
        values.add("Date time");
        values.addAll(stockNames);
        values.addAll(variableNames);
        csvWriter.writeNext(values.toArray(new String[0]));
        if (csvWriter.checkError()) {
            logger.warn("Failed to write CSV header: {}", csvWriter.getException());
        }
    }

    /**
     * Flushes and closes the CSV writer when the simulation ends.
     */
    @Override
    public void handleSimulationEndEvent(SimulationEndEvent event) {
        close();
        logger.info("Ending simulation");
    }

    /**
     * Closes the underlying CSV writer, flushing any buffered data.
     */
    @Override
    public void close() {
        closed = true;
        if (csvWriter != null) {
            try {
                csvWriter.flush();
                csvWriter.close();
            } catch (IOException e) {
                logger.error("Failed to close CSV writer", e);
            }
            csvWriter = null;
        }
    }
}
