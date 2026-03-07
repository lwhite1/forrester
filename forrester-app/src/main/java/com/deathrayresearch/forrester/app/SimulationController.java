package com.deathrayresearch.forrester.app;

import com.deathrayresearch.forrester.app.canvas.AnalysisRunner;
import com.deathrayresearch.forrester.app.canvas.DashboardPanel;
import com.deathrayresearch.forrester.app.canvas.ModelCanvas;
import com.deathrayresearch.forrester.app.canvas.ModelDefinitionFactory;
import com.deathrayresearch.forrester.app.canvas.ModelEditListener;
import com.deathrayresearch.forrester.app.canvas.ModelEditor;
import com.deathrayresearch.forrester.app.canvas.MonteCarloDialog;
import com.deathrayresearch.forrester.app.canvas.MultiParameterSweepDialog;
import com.deathrayresearch.forrester.app.canvas.OptimizerDialog;
import com.deathrayresearch.forrester.app.canvas.ParameterSweepDialog;
import com.deathrayresearch.forrester.app.canvas.SimulationRunner;
import com.deathrayresearch.forrester.app.canvas.SimulationSettingsDialog;
import com.deathrayresearch.forrester.app.canvas.StatusBar;
import com.deathrayresearch.forrester.app.canvas.ValidationDialog;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelValidator;
import com.deathrayresearch.forrester.model.def.SimulationSettings;
import com.deathrayresearch.forrester.model.def.StockDef;
import com.deathrayresearch.forrester.sweep.MonteCarlo;
import com.deathrayresearch.forrester.sweep.Objectives;
import com.deathrayresearch.forrester.sweep.ObjectiveFunction;
import com.deathrayresearch.forrester.sweep.OptimizationAlgorithm;
import com.deathrayresearch.forrester.sweep.Optimizer;
import com.deathrayresearch.forrester.sweep.MultiParameterSweep;
import com.deathrayresearch.forrester.sweep.ParameterSweep;
import com.deathrayresearch.forrester.sweep.SamplingMethod;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Handles simulation, parameter sweeps, Monte Carlo, and optimization for a {@link ModelWindow}.
 */
final class SimulationController {

    private final ModelCanvas canvas;
    private final AnalysisRunner analysisRunner;
    private final DashboardPanel dashboardPanel;
    private final Runnable switchToDashboard;
    private final StatusBar statusBar;
    private final Consumer<String> showError;
    private final Consumer<Consumer<ModelEditListener>> fireLogEvent;

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

        ModelDefinition def = canvas.toModelDefinition();
        SimulationSettings finalSettings = settings;

        analysisRunner.run("Simulating...",
                () -> new SimulationRunner().run(def, finalSettings),
                result -> {
                    dashboardPanel.showSimulationResult(result);
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

        ModelEditor activeEditor = canvas.getEditor();
        List<String> constantNames = activeEditor.getConstants().stream()
                .map(ConstantDef::name).toList();
        List<String> trackableNames = new ArrayList<>();
        activeEditor.getStocks().forEach(s -> trackableNames.add(s.name()));
        activeEditor.getFlows().forEach(f -> trackableNames.add(f.name()));
        activeEditor.getAuxiliaries().forEach(a -> trackableNames.add(a.name()));

        if (constantNames.isEmpty()) {
            showError.accept("Model has no constants to sweep.");
            return;
        }

        ParameterSweepDialog dialog = new ParameterSweepDialog(constantNames, trackableNames);
        Optional<ParameterSweepDialog.Config> configOpt = dialog.showAndWait();
        if (configOpt.isEmpty()) {
            return;
        }

        ParameterSweepDialog.Config config = configOpt.get();
        ModelDefinition def = canvas.toModelDefinition();
        SimulationSettings finalSettings = settings;

        analysisRunner.run("Running sweep...",
                () -> {
                    TimeUnit timeStep = ModelDefinitionFactory.resolveTimeStep(finalSettings);
                    Quantity duration = ModelDefinitionFactory.resolveDuration(finalSettings);

                    return ParameterSweep.builder()
                            .parameterName(config.parameterName())
                            .parameterValues(ParameterSweep.linspace(
                                    config.start(), config.end(), config.step()))
                            .compiledModelFactory(
                                    ModelDefinitionFactory.createSingleParamFactory(
                                            def, finalSettings, config.parameterName()))
                            .timeStep(timeStep)
                            .duration(duration)
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

        ModelEditor activeEditor = canvas.getEditor();
        List<String> constantNames = activeEditor.getConstants().stream()
                .map(ConstantDef::name).toList();

        if (constantNames.size() < 2) {
            showError.accept("Model needs at least 2 constants to sweep.");
            return;
        }

        MultiParameterSweepDialog dialog = new MultiParameterSweepDialog(constantNames);
        Optional<MultiParameterSweepDialog.Config> configOpt = dialog.showAndWait();
        if (configOpt.isEmpty()) {
            return;
        }

        MultiParameterSweepDialog.Config config = configOpt.get();
        ModelDefinition def = canvas.toModelDefinition();
        SimulationSettings finalSettings = settings;

        analysisRunner.run("Running multi-parameter sweep...",
                () -> {
                    TimeUnit timeStep = ModelDefinitionFactory.resolveTimeStep(finalSettings);
                    Quantity duration = ModelDefinitionFactory.resolveDuration(finalSettings);

                    MultiParameterSweep.Builder builder = MultiParameterSweep.builder()
                            .compiledModelFactory(
                                    ModelDefinitionFactory.createFactory(def, finalSettings))
                            .timeStep(timeStep)
                            .duration(duration);

                    for (MultiParameterSweepDialog.ParamConfig p : config.parameters()) {
                        builder.parameter(p.name(),
                                ParameterSweep.linspace(p.start(), p.end(), p.step()));
                    }

                    return builder.build().execute();
                },
                result -> {
                    dashboardPanel.showMultiSweepResult(result);
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

        ModelEditor activeEditor = canvas.getEditor();
        List<String> constantNames = activeEditor.getConstants().stream()
                .map(ConstantDef::name).toList();

        if (constantNames.isEmpty()) {
            showError.accept("Model has no constants to vary.");
            return;
        }

        MonteCarloDialog dialog = new MonteCarloDialog(constantNames);
        Optional<MonteCarloDialog.Config> configOpt = dialog.showAndWait();
        if (configOpt.isEmpty()) {
            return;
        }

        MonteCarloDialog.Config config = configOpt.get();
        ModelDefinition def = canvas.toModelDefinition();
        SimulationSettings finalSettings = settings;

        analysisRunner.run(
                "Running Monte Carlo (" + config.iterations() + " iterations)...",
                () -> {
                    TimeUnit timeStep = ModelDefinitionFactory.resolveTimeStep(finalSettings);
                    Quantity duration = ModelDefinitionFactory.resolveDuration(finalSettings);

                    MonteCarlo.Builder builder = MonteCarlo.builder()
                            .compiledModelFactory(
                                    ModelDefinitionFactory.createFactory(def, finalSettings))
                            .iterations(config.iterations())
                            .sampling("RANDOM".equals(config.samplingMethod())
                                    ? SamplingMethod.RANDOM : SamplingMethod.LATIN_HYPERCUBE)
                            .seed(config.seed())
                            .timeStep(timeStep)
                            .duration(duration);

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

        ModelEditor activeEditor = canvas.getEditor();
        List<String> constantNames = activeEditor.getConstants().stream()
                .map(ConstantDef::name).toList();
        List<String> stockNames = activeEditor.getStocks().stream()
                .map(StockDef::name).toList();

        if (constantNames.isEmpty()) {
            showError.accept("Model has no constants to optimize.");
            return;
        }
        if (stockNames.isEmpty()) {
            showError.accept("Model has no stocks for objective evaluation.");
            return;
        }

        OptimizerDialog dialog = new OptimizerDialog(constantNames, stockNames);
        Optional<OptimizerDialog.Config> configOpt = dialog.showAndWait();
        if (configOpt.isEmpty()) {
            return;
        }

        OptimizerDialog.Config config = configOpt.get();
        ModelDefinition def = canvas.toModelDefinition();
        SimulationSettings finalSettings = settings;

        analysisRunner.run(
                "Optimizing (" + config.maxEvaluations() + " max evals)...",
                () -> {
                    TimeUnit timeStep = ModelDefinitionFactory.resolveTimeStep(finalSettings);
                    Quantity duration = ModelDefinitionFactory.resolveDuration(finalSettings);

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
                            .timeStep(timeStep)
                            .duration(duration);

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

    void validateModel() {
        ModelDefinition def = canvas.toModelDefinition();

        analysisRunner.run(
                () -> ModelValidator.validate(def),
                result -> {
                    statusBar.updateValidation(result.errorCount(), result.warningCount());
                    ValidationDialog dialog = new ValidationDialog(result, canvas::selectElement);
                    dialog.show();
                    fireLogEvent.accept(l -> l.onValidation(result.errorCount(), result.warningCount()));
                },
                "Validation Error");
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
