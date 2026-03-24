package systems.courant.sd.app;

import systems.courant.sd.app.canvas.AnalysisRunner;
import systems.courant.sd.app.canvas.ChartUtils;
import systems.courant.sd.app.canvas.DashboardPanel;
import systems.courant.sd.app.canvas.ModelCanvas;
import systems.courant.sd.app.canvas.ModelDefinitionFactory;
import systems.courant.sd.app.canvas.ModelDefinitionFactory.SimulationTiming;
import systems.courant.sd.app.canvas.ModelEditListener;
import systems.courant.sd.app.canvas.ModelEditor;
import systems.courant.sd.app.canvas.dialogs.MonteCarloDialog;
import systems.courant.sd.app.canvas.dialogs.MultiParameterSweepDialog;
import systems.courant.sd.app.canvas.dialogs.CalibrateDialog;
import systems.courant.sd.app.canvas.dialogs.OptimizerDialog;
import systems.courant.sd.app.canvas.dialogs.ParameterSweepDialog;
import systems.courant.sd.app.canvas.charts.SensitivityPane;
import systems.courant.sd.app.canvas.SimulationRunner;
import systems.courant.sd.app.canvas.dialogs.SimulationSettingsDialog;
import systems.courant.sd.app.canvas.StatusBar;
import systems.courant.sd.app.canvas.dialogs.ExtremeConditionDialog;
import systems.courant.sd.app.canvas.dialogs.ValidationDialog;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelValidator;
import systems.courant.sd.model.def.SimulationSettings;
import systems.courant.sd.model.def.StockDef;
import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.graph.FeedbackAnalysis;
import systems.courant.sd.model.graph.LoopDominanceAnalysis;
import systems.courant.sd.sweep.ExtremeConditionTest;
import systems.courant.sd.sweep.MonteCarlo;
import systems.courant.sd.sweep.Objectives;
import systems.courant.sd.sweep.ObjectiveFunction;
import systems.courant.sd.sweep.OptimizationAlgorithm;
import systems.courant.sd.sweep.Optimizer;
import systems.courant.sd.sweep.MultiParameterSweep;
import systems.courant.sd.sweep.ParameterSweep;
import systems.courant.sd.sweep.SamplingMethod;
import systems.courant.sd.sweep.SensitivitySummary;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Handles simulation, parameter sweeps, Monte Carlo, and optimization for a {@link ModelWindow}.
 */
final class SimulationController {

    private final ModelCanvas canvas;
    private AnalysisRunner analysisRunner;
    private final DashboardPanel dashboardPanel;
    private final Runnable switchToDashboard;
    private final StatusBar statusBar;
    private final Consumer<String> showError;
    private final Consumer<Consumer<ModelEditListener>> fireLogEvent;
    private ParameterSweepDialog.Config lastSweepConfig;
    private MonteCarloDialog.Config lastMonteCarloConfig;

    SimulationController(ModelCanvas canvas,
                         AnalysisRunner analysisRunner,
                         DashboardPanel dashboardPanel,
                         Runnable switchToDashboard,
                         StatusBar statusBar,
                         Consumer<String> showError,
                         Consumer<Consumer<ModelEditListener>> fireLogEvent) {
        this.canvas = canvas;
        this.analysisRunner = analysisRunner;
        this.dashboardPanel = dashboardPanel;
        this.switchToDashboard = switchToDashboard;
        this.statusBar = statusBar;
        this.showError = showError;
        this.fireLogEvent = fireLogEvent;
    }

    void setAnalysisRunner(AnalysisRunner runner) {
        this.analysisRunner = runner;
    }

    void openSimulationSettings() {
        ModelEditor editor = canvas.getEditor();
        SimulationSettingsDialog dialog = new SimulationSettingsDialog(
                editor.getSimulationSettings());
        Optional<SimulationSettings> result = dialog.showAndWait();
        result.ifPresent(editor::setSimulationSettings);
    }

    void runSimulation() {
        SimulationSettings settings = ensureSettings();
        if (settings == null) {
            return;
        }

        ModelDefinition def = canvas.navigation().toModelDefinition();
        SimulationSettings finalSettings = settings;
        dashboardPanel.setTimeStepLabel(settings.timeStep());

        // Snapshot parameter values for ghost run labeling
        Map<String, Double> paramSnapshot = new LinkedHashMap<>();
        for (var param : def.parameters()) {
            paramSnapshot.put(param.name(), param.literalValue());
        }

        analysisRunner.run("Simulating...",
                () -> new SimulationRunner().run(def, finalSettings),
                result -> {
                    dashboardPanel.showSimulationResult(result, paramSnapshot, def.flows(),
                            def.referenceDatasets());
                    canvas.setSparklineData(new systems.courant.sd.app.canvas.renderers.CanvasRenderer.SparklineData(
                            ModelCanvas.extractStockSeries(result), false));
                    computeLoopDominance(result);
                    switchToDashboard.run();
                    fireLogEvent.accept(ModelEditListener::onSimulationRun);
                },
                "Simulation Error");
    }

    void runParameterSweep() {
        SimulationSettings settings = ensureSettings();
        if (settings == null) {
            return;
        }
        dashboardPanel.setTimeStepLabel(settings.timeStep());

        ModelEditor activeEditor = canvas.getEditor();
        List<String> parameterNames = activeEditor.getParameterNames();
        List<String> trackableNames = new ArrayList<>();
        activeEditor.getStocks().forEach(s -> trackableNames.add(s.name()));
        activeEditor.getFlows().forEach(f -> trackableNames.add(f.name()));
        activeEditor.getVariables().stream()
                .filter(a -> !ChartUtils.isSimulationSetting(a.name()))
                .forEach(a -> trackableNames.add(a.name()));

        if (parameterNames.isEmpty()) {
            showError.accept("Model has no parameters to sweep.");
            return;
        }

        ParameterSweepDialog dialog = new ParameterSweepDialog(
                parameterNames, trackableNames, lastSweepConfig);
        Optional<ParameterSweepDialog.Config> configOpt = dialog.showAndWait();
        if (configOpt.isEmpty()) {
            return;
        }

        ParameterSweepDialog.Config config = configOpt.get();
        lastSweepConfig = config;
        ModelDefinition def = canvas.navigation().toModelDefinition();
        SimulationSettings finalSettings = settings;

        analysisRunner.run("Running sweep...",
                () -> {
                    SimulationTiming timing = ModelDefinitionFactory.resolveTiming(finalSettings);

                    return ParameterSweep.builder()
                            .parameterName(config.parameterName())
                            .parameterValues(ParameterSweep.linspace(
                                    config.start(), config.end(), config.step()))
                            .compiledModelFactory(
                                    ModelDefinitionFactory.createSingleParamFactory(
                                            def, finalSettings, config.parameterName()))
                            .timeStep(timing.timeStep())
                            .duration(timing.duration())
                            .build()
                            .execute();
                },
                result -> {
                    dashboardPanel.showSweepResult(result, config.parameterName());
                    switchToDashboard.run();
                    fireLogEvent.accept(l -> l.onAnalysisRun("Parameter Sweep",
                            config.parameterName() + " [" + config.start() + ".." + config.end() + "]"));
                },
                "Sweep Error");
    }

    void runMultiParameterSweep() {
        SimulationSettings settings = ensureSettings();
        if (settings == null) {
            return;
        }
        dashboardPanel.setTimeStepLabel(settings.timeStep());

        ModelEditor activeEditor = canvas.getEditor();
        List<String> parameterNames = activeEditor.getParameterNames();

        if (parameterNames.size() < 2) {
            showError.accept("Model needs at least 2 parameters to sweep.");
            return;
        }

        MultiParameterSweepDialog dialog = new MultiParameterSweepDialog(parameterNames);
        Optional<MultiParameterSweepDialog.Config> configOpt = dialog.showAndWait();
        if (configOpt.isEmpty()) {
            return;
        }

        MultiParameterSweepDialog.Config config = configOpt.get();
        ModelDefinition def = canvas.navigation().toModelDefinition();
        SimulationSettings finalSettings = settings;

        analysisRunner.run("Running multi-parameter sweep...",
                () -> {
                    SimulationTiming timing = ModelDefinitionFactory.resolveTiming(finalSettings);

                    MultiParameterSweep.Builder builder = MultiParameterSweep.builder()
                            .compiledModelFactory(
                                    ModelDefinitionFactory.createFactory(def, finalSettings))
                            .timeStep(timing.timeStep())
                            .duration(timing.duration());

                    for (MultiParameterSweepDialog.ParamConfig p : config.parameters()) {
                        builder.parameter(p.name(),
                                ParameterSweep.linspace(p.start(), p.end(), p.step()));
                    }

                    return builder.build().execute();
                },
                result -> {
                    dashboardPanel.showMultiSweepResult(result);
                    showMultiSweepSensitivity(result);
                    switchToDashboard.run();
                    String paramSummary = config.parameters().stream()
                            .map(MultiParameterSweepDialog.ParamConfig::name)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("");
                    fireLogEvent.accept(l -> l.onAnalysisRun("Multi-Parameter Sweep", paramSummary));
                },
                "Multi-Sweep Error");
    }

    void runMonteCarlo() {
        SimulationSettings settings = ensureSettings();
        if (settings == null) {
            return;
        }
        dashboardPanel.setTimeStepLabel(settings.timeStep());

        ModelEditor activeEditor = canvas.getEditor();
        List<String> parameterNames = activeEditor.getParameterNames();

        if (parameterNames.isEmpty()) {
            showError.accept("Model has no parameters to vary.");
            return;
        }

        MonteCarloDialog dialog = new MonteCarloDialog(parameterNames, lastMonteCarloConfig);
        Optional<MonteCarloDialog.Config> configOpt = dialog.showAndWait();
        if (configOpt.isEmpty()) {
            return;
        }

        MonteCarloDialog.Config config = configOpt.get();
        lastMonteCarloConfig = config;

        String validationError = validateDistributionParameters(config.parameters());
        if (!validationError.isEmpty()) {
            showError.accept(validationError);
            return;
        }

        ModelDefinition def = canvas.navigation().toModelDefinition();
        SimulationSettings finalSettings = settings;

        analysisRunner.run(
                "Running Monte Carlo (" + config.iterations() + " iterations)...",
                () -> {
                    SimulationTiming timing = ModelDefinitionFactory.resolveTiming(finalSettings);

                    MonteCarlo.Builder builder = MonteCarlo.builder()
                            .compiledModelFactory(
                                    ModelDefinitionFactory.createFactory(def, finalSettings))
                            .iterations(config.iterations())
                            .sampling("RANDOM".equals(config.samplingMethod())
                                    ? SamplingMethod.RANDOM : SamplingMethod.LATIN_HYPERCUBE)
                            .seed(config.seed())
                            .timeStep(timing.timeStep())
                            .duration(timing.duration());

                    for (MonteCarloDialog.ParameterConfig p : config.parameters()) {
                        if (p.distribution() == MonteCarloDialog.DistributionType.NORMAL) {
                            builder.parameter(p.name(), new NormalDistribution(p.param1(), p.param2()));
                        } else {
                            builder.parameter(p.name(), new UniformRealDistribution(p.param1(), p.param2()));
                        }
                    }

                    return builder.build().execute();
                },
                result -> {
                    dashboardPanel.showMonteCarloResult(result);
                    showMonteCarloSensitivity(result);
                    switchToDashboard.run();
                    fireLogEvent.accept(l -> l.onAnalysisRun("Monte Carlo",
                            config.iterations() + " iterations, " + config.parameters().size() + " params"));
                },
                "Monte Carlo Error");
    }

    void runOptimization() {
        SimulationSettings settings = ensureSettings();
        if (settings == null) {
            return;
        }
        dashboardPanel.setTimeStepLabel(settings.timeStep());

        ModelEditor activeEditor = canvas.getEditor();
        List<String> parameterNames = activeEditor.getParameterNames();
        List<String> stockNames = activeEditor.getStocks().stream()
                .map(StockDef::name).toList();

        if (parameterNames.isEmpty()) {
            showError.accept("Model has no parameters to optimize.");
            return;
        }
        if (stockNames.isEmpty()) {
            showError.accept("Model has no stocks for objective evaluation.");
            return;
        }

        OptimizerDialog dialog = new OptimizerDialog(parameterNames, stockNames);
        Optional<OptimizerDialog.Config> configOpt = dialog.showAndWait();
        if (configOpt.isEmpty()) {
            return;
        }

        OptimizerDialog.Config config = configOpt.get();
        ModelDefinition def = canvas.navigation().toModelDefinition();
        SimulationSettings finalSettings = settings;

        analysisRunner.run(
                "Optimizing (" + config.maxEvaluations() + " max evals)...",
                () -> {
                    SimulationTiming timing = ModelDefinitionFactory.resolveTiming(finalSettings);

                    ObjectiveFunction objective = switch (config.objectiveType()) {
                        case MINIMIZE -> Objectives.minimize(config.targetVariable());
                        case MAXIMIZE -> Objectives.maximize(config.targetVariable());
                        case TARGET -> Objectives.target(config.targetVariable(), config.targetValue());
                        case MINIMIZE_PEAK -> Objectives.minimizePeak(config.targetVariable());
                    };

                    OptimizationAlgorithm algorithm = switch (config.algorithm()) {
                        case "BOBYQA" -> OptimizationAlgorithm.BOBYQA;
                        case "CMAES" -> OptimizationAlgorithm.CMAES;
                        default -> OptimizationAlgorithm.NELDER_MEAD;
                    };

                    Optimizer.Builder builder = Optimizer.builder()
                            .compiledModelFactory(
                                    ModelDefinitionFactory.createFactory(def, finalSettings))
                            .objective(objective)
                            .algorithm(algorithm)
                            .maxEvaluations(config.maxEvaluations())
                            .timeStep(timing.timeStep())
                            .duration(timing.duration());

                    for (OptimizerDialog.ParamConfig p : config.parameters()) {
                        if (Double.isNaN(p.initialGuess())) {
                            builder.parameter(p.name(), p.lower(), p.upper());
                        } else {
                            builder.parameter(p.name(), p.lower(), p.upper(), p.initialGuess());
                        }
                    }

                    return builder.build().execute();
                },
                result -> {
                    dashboardPanel.showOptimizationResult(result);
                    switchToDashboard.run();
                    fireLogEvent.accept(l -> l.onAnalysisRun("Optimization",
                            config.algorithm() + ", " + config.parameters().size() + " params"));
                },
                "Optimization Error");
    }

    void runCalibration() {
        SimulationSettings settings = ensureSettings();
        if (settings == null) {
            return;
        }
        dashboardPanel.setTimeStepLabel(settings.timeStep());

        ModelEditor activeEditor = canvas.getEditor();
        List<String> parameterNames = activeEditor.getParameterNames();
        List<String> stockNames = activeEditor.getStocks().stream()
                .map(StockDef::name).toList();

        if (parameterNames.isEmpty()) {
            showError.accept("Model has no parameters to calibrate.");
            return;
        }
        if (stockNames.isEmpty()) {
            showError.accept("Model has no stocks to fit against observed data.");
            return;
        }

        CalibrateDialog dialog = new CalibrateDialog(parameterNames, stockNames);
        Optional<CalibrateDialog.Config> configOpt = dialog.showAndWait();
        if (configOpt.isEmpty()) {
            return;
        }

        CalibrateDialog.Config config = configOpt.get();
        ModelDefinition def = canvas.navigation().toModelDefinition();
        SimulationSettings finalSettings = settings;

        analysisRunner.run(
                "Calibrating (" + config.maxEvaluations() + " max evals)...",
                () -> {
                    SimulationTiming timing = ModelDefinitionFactory.resolveTiming(finalSettings);

                    // Build composite objective: sum SSE across all fit targets
                    List<CalibrateDialog.FitTarget> targets = config.fitTargets();
                    List<ObjectiveFunction> perTarget = targets.stream()
                            .map(t -> Objectives.fitToTimeSeries(t.stockName(), t.observedData()))
                            .toList();
                    ObjectiveFunction objective = runResult -> {
                        double totalSse = 0.0;
                        for (ObjectiveFunction fn : perTarget) {
                            totalSse += fn.evaluate(runResult);
                        }
                        return totalSse;
                    };

                    OptimizationAlgorithm algorithm = switch (config.algorithm()) {
                        case "BOBYQA" -> OptimizationAlgorithm.BOBYQA;
                        case "CMAES" -> OptimizationAlgorithm.CMAES;
                        default -> OptimizationAlgorithm.NELDER_MEAD;
                    };

                    Optimizer.Builder builder = Optimizer.builder()
                            .compiledModelFactory(
                                    ModelDefinitionFactory.createFactory(def, finalSettings))
                            .objective(objective)
                            .algorithm(algorithm)
                            .maxEvaluations(config.maxEvaluations())
                            .timeStep(timing.timeStep())
                            .duration(timing.duration());

                    for (CalibrateDialog.ParamConfig p : config.parameters()) {
                        if (Double.isNaN(p.initialGuess())) {
                            builder.parameter(p.name(), p.lower(), p.upper());
                        } else {
                            builder.parameter(p.name(), p.lower(), p.upper(), p.initialGuess());
                        }
                    }

                    return builder.build().execute();
                },
                result -> {
                    dashboardPanel.showCalibrationResult(result, config.fitTargets());
                    switchToDashboard.run();
                    fireLogEvent.accept(l -> l.onAnalysisRun("Calibration",
                            config.algorithm() + ", " + config.parameters().size() + " params, "
                                    + config.fitTargets().size() + " targets"));
                },
                "Calibration Error");
    }

    void validateModel() {
        ModelDefinition def = canvas.navigation().toModelDefinition();

        analysisRunner.run(
                () -> ModelValidator.validate(def),
                result -> {
                    statusBar.updateValidation(result.errorCount(), result.warningCount());
                    ValidationDialog.showOrUpdate(result, canvas.elements()::selectElement);
                    fireLogEvent.accept(l -> l.onValidation(result.errorCount(), result.warningCount()));
                },
                "Validation Error");
    }

    void runExtremeConditionTest() {
        SimulationSettings settings = ensureSettings();
        if (settings == null) {
            return;
        }

        ModelDefinition def = canvas.navigation().toModelDefinition();
        List<VariableDef> params = def.parameters();

        if (params.isEmpty()) {
            showError.accept("Model has no parameters to test.");
            return;
        }

        int totalRuns = params.size() * 3;
        analysisRunner.run(
                "Running extreme condition tests (" + totalRuns + " runs)...",
                () -> {
                    SimulationTiming timing = ModelDefinitionFactory.resolveTiming(settings);

                    ExtremeConditionTest.Builder builder = ExtremeConditionTest.builder()
                            .compiledModelFactory(
                                    ModelDefinitionFactory.createFactory(def, settings))
                            .timeStep(timing.timeStep())
                            .duration(timing.duration());

                    for (VariableDef param : params) {
                        builder.parameter(param.name(), param.literalValue());
                    }

                    return builder.build().execute();
                },
                result -> {
                    ExtremeConditionDialog.showOrUpdate(result);
                    fireLogEvent.accept(l -> l.onAnalysisRun("Extreme Condition Test",
                            params.size() + " parameters, " + result.findings().size() + " findings"));
                },
                "Extreme Condition Test Error");
    }

    /**
     * Validates distribution parameters for Monte Carlo configurations.
     * Returns an empty string if all parameters are valid, or a descriptive
     * error message for the first invalid parameter found.
     */
    static String validateDistributionParameters(List<MonteCarloDialog.ParameterConfig> parameters) {
        for (MonteCarloDialog.ParameterConfig p : parameters) {
            if (p.distribution() == MonteCarloDialog.DistributionType.NORMAL) {
                if (p.param2() <= 0) {
                    return "Parameter '" + p.name()
                            + "': Normal distribution requires a positive standard deviation, got "
                            + p.param2() + ".";
                }
            } else {
                if (p.param1() >= p.param2()) {
                    return "Parameter '" + p.name()
                            + "': Uniform distribution requires min < max, got min="
                            + p.param1() + ", max=" + p.param2() + ".";
                }
            }
        }
        return "";
    }

    private void showMultiSweepSensitivity(
            systems.courant.sd.sweep.MultiSweepResult multiResult) {
        List<String> trackable = collectTrackableNames(multiResult.getStockNames(),
                multiResult.getVariableNames());
        if (trackable.isEmpty()) {
            return;
        }
        SensitivityPane pane = new SensitivityPane(trackable,
                (target, unused) -> SensitivitySummary.fromMultiSweep(multiResult, target),
                trackable.getFirst());
        dashboardPanel.showSensitivity(pane);
    }

    private void showMonteCarloSensitivity(
            systems.courant.sd.sweep.MonteCarloResult mcResult) {
        List<String> trackable = collectTrackableNames(mcResult.getStockNames(),
                mcResult.getVariableNames());
        if (trackable.isEmpty()) {
            return;
        }
        SensitivityPane pane = new SensitivityPane(trackable,
                (target, unused) -> SensitivitySummary.fromMonteCarlo(mcResult, target),
                trackable.getFirst());
        dashboardPanel.showSensitivity(pane);

        // Store sensitivity impacts for report export (using first trackable variable)
        List<SensitivitySummary.ParameterImpact> impacts =
                SensitivitySummary.fromMonteCarlo(mcResult, trackable.getFirst());
        dashboardPanel.storeSensitivityImpacts(impacts);
    }

    private static List<String> collectTrackableNames(List<String> stocks, List<String> variables) {
        List<String> names = new ArrayList<>(ChartUtils.filterSimulationSettings(stocks));
        names.addAll(ChartUtils.filterSimulationSettings(variables));
        return names;
    }

    private void computeLoopDominance(SimulationRunner.SimulationResult result) {
        if (!canvas.analysis().isLoopHighlightActive()) {
            return;
        }
        FeedbackAnalysis analysis = canvas.analysis().getLoopAnalysis();
        if (analysis == null || analysis.loopCount() == 0) {
            return;
        }
        LoopDominanceAnalysis dominance = LoopDominanceAnalysis.compute(
                result.columnNames(), result.rows(), analysis);
        if (dominance != null) {
            dashboardPanel.showLoopDominance(dominance);
        }
    }

    private SimulationSettings ensureSettings() {
        ModelEditor editor = canvas.getEditor();
        SimulationSettings settings = editor.getSimulationSettings();
        if (settings == null) {
            SimulationSettingsDialog dialog = new SimulationSettingsDialog(null);
            Optional<SimulationSettings> result = dialog.showAndWait();
            if (result.isEmpty()) {
                return null;
            }
            settings = result.get();
            editor.setSimulationSettings(settings);
        }
        return settings;
    }
}
