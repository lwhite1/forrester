package systems.courant.sd.integration;

import systems.courant.sd.Simulation;
import systems.courant.sd.io.ImportResult;
import systems.courant.sd.io.vensim.VensimImporter;
import systems.courant.sd.model.Stock;
import systems.courant.sd.model.compile.CompiledModel;
import systems.courant.sd.model.compile.ModelCompiler;
import systems.courant.sd.model.def.ModelDefinition;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;


/**
 * Batch import test: scans Vensim Sample, UserGuide, and Delft directories,
 * imports every .mdl file, attempts compile and simulate, and writes
 * detailed reports to devdocs/.
 */
@DisplayName("Vensim batch import (Sample + UserGuide + Delft)")
class VensimBatchImportTest {

    private static final Path MODELS_ROOT = Path.of("D:/projects/forrester/models");
    private static final Path VENSIM_ROOT = MODELS_ROOT.resolve("Vensim");
    private static final Path DELFT_ROOT = MODELS_ROOT.resolve("Delft");
    private static final Path DEVDOCS = Path.of("D:/projects/forrester/devdocs");

    private final VensimImporter importer = new VensimImporter();
    private final ModelCompiler compiler = new ModelCompiler();

    record ModelResult(
            String relativePath,
            String source,
            boolean importOk,
            List<String> warnings,
            boolean compileOk,
            String compileError,
            boolean simulateOk,
            String simulateError,
            int stocks,
            int flows,
            int auxes
    ) {}

    record SourceDir(Path root, String label) {}

    @Test
    @DisplayName("should import all Sample, UserGuide, and Delft models and generate reports")
    void batchImportAndReport() throws IOException {
        List<SourceDir> sources = List.of(
                new SourceDir(VENSIM_ROOT.resolve("Sample"), "Sample"),
                new SourceDir(VENSIM_ROOT.resolve("UserGuide"), "UserGuide"),
                new SourceDir(DELFT_ROOT, "Delft")
        );

        List<ModelResult> results = new ArrayList<>();

        for (SourceDir src : sources) {
            if (!Files.isDirectory(src.root)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(src.root)) {
                List<Path> mdlFiles = walk
                        .filter(p -> p.toString().toLowerCase().endsWith(".mdl"))
                        .sorted()
                        .toList();

                for (Path mdlFile : mdlFiles) {
                    results.add(processModel(mdlFile, src.root, src.label));
                }
            }
        }

        Assumptions.assumeFalse(results.isEmpty(),
                "No .mdl model files found — skipping (model directories are not present in CI)");

        Files.createDirectories(DEVDOCS);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        writeImportLog(results, DEVDOCS.resolve("vensim-import-log-" + timestamp + ".md"));
        writeCompatibilityMatrix(results, DEVDOCS.resolve("vensim-compatibility-matrix-" + timestamp + ".csv"));
        writeSummary(results, DEVDOCS.resolve("vensim-import-summary-" + timestamp + ".md"));

        long importFail = results.stream().filter(r -> !r.importOk).count();
        System.out.printf("%n=== BATCH IMPORT RESULTS ===%n");
        System.out.printf("Total models: %d%n", results.size());
        System.out.printf("Import success: %d%n", results.stream().filter(r -> r.importOk).count());
        System.out.printf("Compile success: %d%n", results.stream().filter(r -> r.compileOk).count());
        System.out.printf("Simulate success: %d%n", results.stream().filter(r -> r.simulateOk).count());
    }

    private ModelResult processModel(Path mdlFile, Path sourceRoot, String sourceLabel) {
        String relativePath = sourceRoot.relativize(mdlFile).toString().replace('/', '\\');

        boolean importOk = false;
        List<String> warnings = List.of();
        boolean compileOk = false;
        String compileError = null;
        boolean simulateOk = false;
        String simulateError = null;
        int stocks = 0;
        int flows = 0;
        int auxes = 0;

        // Import
        ModelDefinition def = null;
        try {
            ImportResult result = importer.importModel(mdlFile);
            importOk = true;
            warnings = result.warnings();
            def = result.definition();

            stocks = def.stocks().size();
            flows = def.flows().size();
            auxes = def.variables().size() + def.parameters().size();
        } catch (Exception e) {
            compileError = e.getClass().getSimpleName() + ": " + truncate(e.getMessage(), 200);
            return new ModelResult(relativePath, sourceLabel, importOk, warnings,
                    compileOk, compileError, simulateOk, simulateError,
                    stocks, flows, auxes);
        }

        // Compile
        CompiledModel compiled = null;
        try {
            compiled = compiler.compile(def);
            compileOk = true;
        } catch (Exception e) {
            compileError = e.getClass().getSimpleName() + ": " + truncate(e.getMessage(), 300);
        }

        // Simulate
        if (compiled != null) {
            try {
                Simulation sim = compiled.createSimulation();
                sim.execute();
                simulateOk = true;

                // Check for NaN/Infinity in stocks
                for (Stock s : compiled.getModel().getStocks()) {
                    if (!Double.isFinite(s.getValue())) {
                        simulateOk = false;
                        simulateError = "Non-finite stock value: " + s.getName() + " = " + s.getValue();
                        break;
                    }
                }
            } catch (Exception e) {
                simulateError = e.getClass().getSimpleName() + ": " + truncate(e.getMessage(), 200);
            }
        }

        return new ModelResult(relativePath, sourceLabel, importOk, warnings,
                compileOk, compileError, simulateOk, simulateError,
                stocks, flows, auxes);
    }

    private void writeImportLog(List<ModelResult> results, Path path) throws IOException {
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8))) {
            w.printf("# Vensim Import Log%n%n");
            w.printf("Generated: %s%n%n", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            long importOk = results.stream().filter(r -> r.importOk).count();
            long compileOk = results.stream().filter(r -> r.compileOk).count();
            long simOk = results.stream().filter(r -> r.simulateOk).count();

            w.printf("## Summary%n%n");
            w.printf("| Metric | Count |%n|---|---|%n");
            w.printf("| Total models | %d |%n", results.size());
            w.printf("| Import success | %d |%n", importOk);
            w.printf("| Compile success | %d |%n", compileOk);
            w.printf("| Simulate success | %d |%n%n", simOk);

            w.printf("## Per-Model Results%n%n");

            for (ModelResult r : results) {
                w.printf("### %s%n%n", r.relativePath);
                w.printf("| Phase | Status | Details |%n|---|---|---|%n");

                w.printf("| Import | %s | %s |%n",
                        r.importOk ? "OK" : "FAILED",
                        r.importOk && !r.warnings.isEmpty() ? r.warnings.size() + " warnings" : r.importOk ? "" : r.compileError);

                if (r.importOk) {
                    w.printf("| Compile | %s | %s |%n",
                            r.compileOk ? "OK" : "FAILED",
                            r.compileOk ? "" : r.compileError);
                    w.printf("| Simulate | %s | %s |%n",
                            r.simulateOk ? "OK" : "FAILED",
                            r.simulateOk ? "" : (r.simulateError != null ? r.simulateError : ""));
                    w.printf("| Structure | %d stocks, %d flows, %d auxes |  |%n",
                            r.stocks, r.flows, r.auxes);
                }

                if (!r.warnings.isEmpty()) {
                    w.printf("%n**Warnings:**%n");
                    for (String warn : r.warnings) {
                        w.printf("- %s%n", warn);
                    }
                }

                w.printf("%n---%n%n");
            }
        }
    }

    private void writeCompatibilityMatrix(List<ModelResult> results, Path path) throws IOException {
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8))) {
            w.println("Model,Source,Import,Compile,Simulate,Stocks,Flows,Auxes,Warnings,Error Category,Error Detail");

            for (ModelResult r : results) {
                String errorCategory = "";
                String errorDetail = "";
                if (r.importOk && !r.compileOk && r.compileError != null) {
                    errorCategory = categorizeError(r.compileError);
                    errorDetail = truncate(r.compileError, 150);
                } else if (r.compileOk && !r.simulateOk && r.simulateError != null) {
                    errorCategory = "Simulation error";
                    errorDetail = truncate(r.simulateError, 150);
                }
                w.printf("%s,%s,%s,%s,%s,%d,%d,%d,%d,%s,\"%s\"%n",
                        r.relativePath,
                        r.source,
                        r.importOk ? "OK" : "FAIL",
                        r.compileOk ? "OK" : "FAIL",
                        r.simulateOk ? "OK" : "FAIL",
                        r.stocks, r.flows, r.auxes,
                        r.warnings.size(),
                        errorCategory,
                        errorDetail.replace("\"", "'"));
            }
        }
    }

    private static String categorizeError(String error) {
        if (error.contains("references unknown element")) {
            return "Unresolved reference";
        }
        if (error.contains("invalid equation")) {
            return "Parse error";
        }
        if (error.contains("requires") && error.contains("arguments")) {
            return "Wrong arity";
        }
        if (error.contains("must be a constant")) {
            return "Non-constant expression";
        }
        if (error.contains("subscript") || error.contains("Subscript")) {
            return "Subscript";
        }
        return "Other";
    }

    private void writeSummary(List<ModelResult> results, Path path) throws IOException {
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8))) {
            writeSummaryHeader(w);
            writeOverallResults(w, results);
            writeResultsBySource(w, results);
            writeCompileFailures(w, results);
            writeImportFailures(w, results);
            writeSimulationFailures(w, results);
            writeSuccessfulModels(w, results);
            writeCommonWarnings(w, results);
            writeFullLogsLinks(w);
        }
    }

    private void writeSummaryHeader(PrintWriter w) {
        w.printf("# Vensim Import Compatibility Summary%n%n");
        w.printf("**Date:** %s%n", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        w.printf("**Source:** `models/Vensim/Sample`, `models/Vensim/UserGuide`, and `models/Delft`%n");
        w.printf("**Tool:** `VensimBatchImportTest.java`%n%n");
    }

    private void writeOverallResults(PrintWriter w, List<ModelResult> results) {
        long total = results.size();
        long importOk = results.stream().filter(r -> r.importOk).count();
        long compileOk = results.stream().filter(r -> r.compileOk).count();
        long simOk = results.stream().filter(r -> r.simulateOk).count();

        w.printf("## Results at a Glance%n%n");
        w.printf("| Phase | Pass | Fail | Rate |%n");
        w.printf("|---|---|---|---|%n");
        w.printf("| Import (parse .mdl) | %d | %d | %d%% |%n", importOk, total - importOk, pct(importOk, total));
        w.printf("| Compile | %d | %d | %d%% |%n", compileOk, total - compileOk, pct(compileOk, total));
        w.printf("| Simulate | %d | %d | %d%% |%n%n", simOk, total - simOk, pct(simOk, total));
    }

    private void writeResultsBySource(PrintWriter w, List<ModelResult> results) {
        w.printf("## Results by Source Directory%n%n");

        Map<String, List<ModelResult>> byTopDir = new LinkedHashMap<>();
        for (ModelResult r : results) {
            byTopDir.computeIfAbsent(r.source, k -> new ArrayList<>()).add(r);
        }

        for (var entry : byTopDir.entrySet()) {
            List<ModelResult> group = entry.getValue();
            long gTotal = group.size();
            long gImport = group.stream().filter(r -> r.importOk).count();
            long gCompile = group.stream().filter(r -> r.compileOk).count();
            long gSim = group.stream().filter(r -> r.simulateOk).count();

            w.printf("### %s (%d models)%n%n", entry.getKey(), gTotal);
            w.printf("| Phase | Pass | Fail | Rate |%n|---|---|---|---|%n");
            w.printf("| Import | %d | %d | %d%% |%n", gImport, gTotal - gImport, pct(gImport, gTotal));
            w.printf("| Compile | %d | %d | %d%% |%n", gCompile, gTotal - gCompile, pct(gCompile, gTotal));
            w.printf("| Simulate | %d | %d | %d%% |%n%n", gSim, gTotal - gSim, pct(gSim, gTotal));
        }
    }

    private void writeCompileFailures(PrintWriter w, List<ModelResult> results) {
        List<ModelResult> failures = results.stream().filter(r -> r.importOk && !r.compileOk).toList();
        if (failures.isEmpty()) {
            return;
        }
        w.printf("## Compile Failures (%d models)%n%n", failures.size());
        w.printf("| Model | Error Summary |%n|---|---|%n");
        for (ModelResult r : failures) {
            w.printf("| %s | %s |%n", r.relativePath, truncate(r.compileError, 120));
        }
        w.println();
    }

    private void writeImportFailures(PrintWriter w, List<ModelResult> results) {
        List<ModelResult> importFailures = results.stream().filter(r -> !r.importOk).toList();
        if (importFailures.isEmpty()) {
            return;
        }
        w.printf("## Import Failures (%d models)%n%n", importFailures.size());
        w.printf("| Model | Error Summary |%n|---|---|%n");
        for (ModelResult r : importFailures) {
            w.printf("| %s | %s |%n", r.relativePath, truncate(r.compileError, 120));
        }
        w.println();
    }

    private void writeSimulationFailures(PrintWriter w, List<ModelResult> results) {
        List<ModelResult> simFailures = results.stream()
                .filter(r -> r.compileOk && !r.simulateOk)
                .toList();
        if (simFailures.isEmpty()) {
            return;
        }
        w.printf("## Simulation Failures (%d models)%n%n", simFailures.size());
        w.printf("| Model | Error Summary |%n|---|---|%n");
        for (ModelResult r : simFailures) {
            w.printf("| %s | %s |%n", r.relativePath,
                    r.simulateError != null ? truncate(r.simulateError, 120) : "Unknown");
        }
        w.println();
    }

    private void writeSuccessfulModels(PrintWriter w, List<ModelResult> results) {
        List<ModelResult> successes = results.stream().filter(r -> r.simulateOk).toList();
        w.printf("## Successfully Simulated Models (%d)%n%n", successes.size());
        w.printf("| Model | Stocks | Flows | Auxes | Warnings |%n|---|---|---|---|---|%n");
        for (ModelResult r : successes) {
            w.printf("| %s | %d | %d | %d | %d |%n",
                    r.relativePath, r.stocks, r.flows, r.auxes, r.warnings.size());
        }
        w.println();
    }

    private void writeCommonWarnings(PrintWriter w, List<ModelResult> results) {
        Map<String, Integer> warningCounts = new TreeMap<>();
        for (ModelResult r : results) {
            for (String warn : r.warnings) {
                String key = warn.length() > 80 ? warn.substring(0, 80) + "..." : warn;
                warningCounts.merge(key, 1, Integer::sum);
            }
        }
        if (warningCounts.isEmpty()) {
            return;
        }
        w.printf("## Common Warnings%n%n");
        w.printf("| Warning | Count |%n|---|---|%n");
        warningCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> w.printf("| %s | %d |%n", e.getKey(), e.getValue()));
        w.println();
    }

    private void writeFullLogsLinks(PrintWriter w) {
        w.printf("## Full Logs%n%n");
        w.printf("- **Per-model details:** `devdocs/vensim-import-log-%s.md`%n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        w.printf("- **Compatibility matrix:** `devdocs/vensim-compatibility-matrix-%s.csv`%n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }

    private static long pct(long num, long denom) {
        return denom == 0 ? 0 : (num * 100) / denom;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
