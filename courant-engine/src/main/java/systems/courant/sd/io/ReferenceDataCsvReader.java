package systems.courant.sd.io;

import systems.courant.sd.model.def.ReferenceDataset;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads a CSV file into a {@link ReferenceDataset}. The expected format is:
 *
 * <pre>
 * Time,Variable1,Variable2,...
 * 0,100.5,200.3
 * 1,102.1,201.5
 * </pre>
 *
 * <p>The first column is treated as the time axis. Remaining columns become
 * named data series. Empty cells are treated as {@code NaN}.
 */
public final class ReferenceDataCsvReader {

    private ReferenceDataCsvReader() {
    }

    /**
     * Reads reference data from a CSV file.
     *
     * @param path the CSV file path
     * @param name display name for the dataset
     * @return the parsed reference dataset
     * @throws IOException if the file cannot be read or is malformed
     */
    public static ReferenceDataset read(Path path, String name) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return read(reader, name);
        }
    }

    /**
     * Reads reference data from a character stream.
     *
     * @param reader the input reader
     * @param name   display name for the dataset
     * @return the parsed reference dataset
     * @throws IOException if the data cannot be read or is malformed
     */
    public static ReferenceDataset read(Reader reader, String name) throws IOException {
        try (CSVReader csv = new CSVReader(reader)) {
            String[] header = csv.readNext();
            if (header == null || header.length < 2) {
                throw new IOException("CSV must have at least two columns (time + one variable)");
            }

            // Trim headers and check for duplicates
            String[] columnNames = new String[header.length - 1];
            Set<String> seen = new HashSet<>();
            for (int i = 1; i < header.length; i++) {
                columnNames[i - 1] = header[i].trim();
                if (!seen.add(columnNames[i - 1])) {
                    throw new IOException(
                            "Duplicate column header: '" + columnNames[i - 1] + "'");
                }
            }

            List<double[]> rawRows = new ArrayList<>();
            String[] line;
            int lineNumber = 1;
            while ((line = csv.readNext()) != null) {
                lineNumber++;
                if (line.length == 0 || (line.length == 1 && line[0].isBlank())) {
                    continue; // skip empty lines
                }
                double[] row = new double[header.length];
                for (int i = 0; i < header.length; i++) {
                    if (i < line.length && !line[i].isBlank()) {
                        try {
                            row[i] = Double.parseDouble(line[i].trim());
                        } catch (NumberFormatException e) {
                            throw new IOException(
                                    "Invalid number at line " + lineNumber + ", column " + (i + 1)
                                            + ": '" + line[i].trim() + "'");
                        }
                    } else {
                        row[i] = Double.NaN;
                    }
                }
                if (Double.isNaN(row[0])) {
                    throw new IOException(
                            "Missing or blank time value at line " + lineNumber);
                }
                rawRows.add(row);
            }

            if (rawRows.isEmpty()) {
                throw new IOException("CSV contains no data rows");
            }

            // Extract time values and column data
            double[] timeValues = new double[rawRows.size()];
            Map<String, double[]> columns = new LinkedHashMap<>();
            for (String colName : columnNames) {
                columns.put(colName, new double[rawRows.size()]);
            }

            for (int r = 0; r < rawRows.size(); r++) {
                double[] row = rawRows.get(r);
                timeValues[r] = row[0];
                for (int c = 0; c < columnNames.length; c++) {
                    columns.get(columnNames[c])[r] = row[c + 1];
                }
            }

            return new ReferenceDataset(name, timeValues, columns);

        } catch (CsvValidationException e) {
            throw new IOException("Invalid CSV format: " + e.getMessage(), e);
        }
    }
}
