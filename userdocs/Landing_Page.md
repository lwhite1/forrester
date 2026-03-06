# Forrester — Free System Dynamics Modeling

Forrester is an open-source System Dynamics tool for building, simulating, and analyzing feedback-driven models. It combines a visual diagram editor with a programmable Java engine, so you can sketch causal loop diagrams on a canvas, formalize them into stock-and-flow simulations, run Monte Carlo analysis — and do it all from code if you prefer.

**Free. No license keys. No feature tiers. No signup.**

---

## Visual Editor

Build models interactively on a JavaFX canvas. Place stocks, flows, auxiliaries, constants, lookup tables, and CLD variables with toolbar buttons or keyboard shortcuts (1–9). Connect elements with click-and-drag. Edit names, equations, and values inline with double-click.

### What you can do in the editor

- **Causal loop diagrams** — sketch qualitative causal structure with CLD variables and polarity-labeled links (+/−/?). The engine automatically detects feedback loops and classifies them as reinforcing (R) or balancing (B). When you're ready to simulate, right-click any CLD variable and classify it as a stock, flow, auxiliary, or constant — it morphs in place on the same canvas.

- **Stock-and-flow models** — rounded-rectangle stocks, diamond flow indicators, dashed info links, cloud endpoints. Two-click flow connection protocol with rubber-band preview and stock hover highlighting. Flow endpoints can be reattached by dragging.

- **Equation autocomplete** — type in a flow or auxiliary equation and get suggestions for element names and built-in functions (SMOOTH, DELAY3, STEP, RAMP, IF, MIN, MAX, etc.). Case-insensitive prefix matching, keyboard navigation, auto-paren insertion.

- **Lookup tables** — place a lookup table, open the inline editor, and see a live chart preview of the curve as you edit x/y data points. Choose linear or cubic spline interpolation.

- **Modules** — encapsulate parts of a model into reusable sub-modules with input/output port bindings. Double-click to drill into a module; navigate back to the parent.

- **Feedback loop highlighting** — toggle the Loops button to see which elements and edges participate in feedback cycles, with colored borders and thickened lines.

- **Copy/paste** — Ctrl+C/V/X duplicates elements with auto-generated names, offset positioning, reconnected flow endpoints, and remapped equation tokens. Works across windows.

- **Undo/redo** — 100-level snapshot stack (Ctrl+Z / Ctrl+Shift+Z).

- **Pan and zoom** — Space+drag or middle-drag to pan, scroll wheel to zoom at cursor, Ctrl+Plus/Minus/0 for keyboard zoom.

- **Diagram export** — File > Export Diagram (Ctrl+E) produces PNG, JPEG, or SVG. Raster formats render at 2x for crisp output. SVG is lossless vector XML.

- **Validation** — Ctrl+B checks for undefined equations, disconnected flows, missing units, algebraic loops, and unused elements. Click any row to select the problem element.

### Analysis from the GUI

Run analysis directly from the Simulate menu — no code required. Results appear in a tabbed dashboard panel with interactive charts and right-click CSV export.

| Analysis | What it does |
|---|---|
| **Simulation** (Ctrl+R) | Compile and run the model. Results in a sortable table and interactive line chart with per-series toggles. |
| **Parameter Sweep** | Sweep one parameter across a range (start/end/step). Summary + time-series charts. |
| **Multi-Parameter Sweep** | Sweep N parameters in a combinatorial grid. Dynamic parameter rows with live combination count. |
| **Monte Carlo** | Sample parameters from probability distributions (Normal, Uniform, Triangular, etc.) with Latin Hypercube or random sampling. Percentile envelope charts. |
| **Optimization** | Find parameter values that minimize an objective (SSE fit-to-data, minimize/maximize a stock, target a value). Nelder-Mead, BOBYQA, or CMA-ES. |

### File operations

- **New / Open / Save / Save As** — JSON format with full view layout preservation.
- **Open Example** — 8 bundled models across 5 categories (introductory, ecology, epidemiology, population, supply chain).
- **Multi-window** — each model opens in its own window with independent state. Shared clipboard for cross-window copy/paste.
- **Import** — open Vensim .mdl files or XMILE files directly. The importer translates equations, stocks, flows, lookup tables, subscripts, simulation settings, and sketch layout.

---

## Programmable Engine

The same engine that powers the visual editor is available as a Java library. Add it as a dependency and build models in code, run headless simulations, sweep parameters, or embed SD in a larger application.

### Build a model in code

```java
Model model = new Model("SIR Epidemic");

Stock susceptible = new Stock("Susceptible", 990, PEOPLE);
Stock infectious  = new Stock("Infectious",   10, PEOPLE);
Stock recovered   = new Stock("Recovered",      0, PEOPLE);

Constant contactRate = new Constant("Contact Rate", DIMENSIONLESS, 8.0);
Constant infectivity = new Constant("Infectivity",  DIMENSIONLESS, 0.10);
Constant recoveryRate = new Constant("Recovery Rate", DIMENSIONLESS, 0.05);

Flow infection = Flow.create("Infection", DAY, () ->
    new Quantity(susceptible.getValue() * infectious.getValue()
        / (susceptible.getValue() + infectious.getValue() + recovered.getValue())
        * contactRate.getValue() * infectivity.getValue(), PEOPLE));

Flow recovery = Flow.create("Recovery", DAY, () ->
    new Quantity(infectious.getValue() * recoveryRate.getValue(), PEOPLE));

susceptible.addOutflow(infection);
infectious.addInflow(infection);
infectious.addOutflow(recovery);
recovered.addInflow(recovery);

model.addStock(susceptible);
model.addStock(infectious);
model.addStock(recovered);

Simulation sim = new Simulation(model, DAY, Times.days(120));
sim.addEventHandler(new CsvSubscriber("sir_output.csv"));
sim.execute();
```

### Or define as data and compile

```java
ModelDefinition def = new ModelDefinitionBuilder()
    .name("SIR Epidemic")
    .stock("Susceptible", 990, "Person")
    .stock("Infectious", 10, "Person")
    .stock("Recovered", 0, "Person")
    .flow("Infection",
          "Susceptible * Infectious / (Susceptible + Infectious + Recovered)"
          + " * contact_rate * infectivity",
          "Day", "Susceptible", "Infectious")
    .flow("Recovery", "Infectious * recovery_rate", "Day", "Infectious", "Recovered")
    .constant("contact_rate", 8.0, "1/Day")
    .constant("infectivity", 0.10, "Dimensionless")
    .constant("recovery_rate", 0.05, "1/Day")
    .defaultSimulation("Day", 120, "Day")
    .build();

// Validate
List<String> errors = DefinitionValidator.validate(def);

// Serialize to JSON (and load later, or open in the visual editor)
new ModelDefinitionSerializer().toFile(def, Path.of("sir.json"));

// Compile and run
CompiledModel compiled = new ModelCompiler().compile(def);
compiled.createSimulation().execute();
```

### Causal loop diagrams in code

```java
ModelDefinition cld = new ModelDefinitionBuilder()
    .name("Climate Feedback")
    .cldVariable("CO2 Emissions")
    .cldVariable("Temperature")
    .cldVariable("Ice Coverage")
    .cldVariable("Albedo")
    .causalLink("CO2 Emissions", "Temperature", POSITIVE)
    .causalLink("Temperature", "Ice Coverage", NEGATIVE)
    .causalLink("Ice Coverage", "Albedo", POSITIVE)
    .causalLink("Albedo", "Temperature", NEGATIVE)
    .build();

// Detect and classify feedback loops
FeedbackAnalysis analysis = FeedbackAnalysis.analyze(cld);
for (FeedbackAnalysis.CausalLoop loop : analysis.causalLoops()) {
    System.out.println(loop.label() + " (" + loop.type() + "): " + loop.members());
}
```

### Parameter sweep

```java
SweepResult result = ParameterSweep.builder()
    .parameterName("Contact Rate")
    .parameterValues(ParameterSweep.linspace(2.0, 14.0, 2.0))
    .modelFactory(rate -> buildSirModel(rate))
    .timeStep(DAY)
    .duration(Times.days(120))
    .build()
    .execute();

result.writeTimeSeriesCsv("sweep-timeseries.csv");
result.writeSummaryCsv("sweep-summary.csv");
```

### Monte Carlo

```java
MonteCarloResult result = MonteCarlo.builder()
    .parameter("Contact Rate", new NormalDistribution(8, 2))
    .parameter("Infectivity", new UniformRealDistribution(0.05, 0.15))
    .modelFactory(params -> buildSirModel(
        params.get("Contact Rate"), params.get("Infectivity")))
    .iterations(500)
    .sampling(SamplingMethod.LATIN_HYPERCUBE)
    .seed(42L)
    .timeStep(DAY)
    .duration(Times.days(120))
    .build()
    .execute();

result.writePercentileCsv("monte_carlo.csv", "Infectious", 2.5, 25, 50, 75, 97.5);
```

### Optimization / calibration

```java
OptimizationResult result = Optimizer.builder()
    .parameter("Contact Rate", 1.0, 20.0)
    .parameter("Infectivity", 0.01, 0.50)
    .modelFactory(params -> buildSirModel(
        params.get("Contact Rate"), params.get("Infectivity")))
    .objective(Objectives.fitToTimeSeries("Infectious", observedData))
    .algorithm(OptimizationAlgorithm.NELDER_MEAD)
    .maxEvaluations(500)
    .timeStep(DAY)
    .duration(Times.days(120))
    .build()
    .execute();

System.out.println("Best fit: " + result.getBestParameters());
System.out.println("SSE: " + result.getBestObjectiveValue());
```

### Subscripts and arrays

```java
Subscript region = new Subscript("Region", "North", "South", "East");

ArrayedStock population = new ArrayedStock("Population", region,
    new double[]{1000, 2000, 500}, PEOPLE);

ArrayedFlow births = ArrayedFlow.create("Births", DAY, region,
    i -> new Quantity(population.getValue(i) * 0.04, PEOPLE));
population.addInflow(births);

// Access by label
double south = population.getValue("South");
double total = population.sum();
```

### Import from Vensim or Stella

```java
// Vensim .mdl
ImportResult vensim = new VensimImporter().importModel(Path.of("model.mdl"));
vensim.warnings().forEach(System.out::println);
CompiledModel compiled = new ModelCompiler().compile(vensim.definition());
compiled.createSimulation().execute();

// XMILE (Stella/iThink)
ImportResult xmile = new XmileImporter().importModel(Path.of("model.xmile"));
new ModelCompiler().compile(xmile.definition()).createSimulation().execute();

// Export to XMILE
XmileExporter.toFile(vensim.definition(), Path.of("converted.xmile"));
```

---

## What's included

| Category | Capabilities |
|---|---|
| **Element types** | Stocks, flows, auxiliaries, constants, lookup tables (linear + spline), CLD variables, causal links, modules |
| **Subscripts** | Single-dimension arrays, multi-dimensional arrays (Region x AgeGroup), intelligent broadcasting (IndexedValue) |
| **SD functions** | SMOOTH, DELAY3, STEP, RAMP, LOOKUP, MIN, MAX, ABS, SQRT, EXP, LN, LOG10, SIN, COS, IF, TIME, DT |
| **Analysis** | Parameter sweep, multi-parameter grid sweep, Monte Carlo (random + LHS), optimization (Nelder-Mead, BOBYQA, CMA-ES) |
| **Units** | 8 dimensions, 40+ predefined units (time, length, mass, volume, temperature, money, items, dimensionless), runtime enforcement |
| **Import/export** | Vensim .mdl import, XMILE import/export, JSON serialization |
| **Visual editor** | Canvas-based diagram editor with CLD + S&F support, inline editing, autocomplete, loop highlighting, analysis dashboard, undo/redo, multi-window, diagram export |
| **Output** | CSV export, interactive line charts, fan charts, sortable result tables |

## Requirements

- **Java 21+** (OpenJDK or Oracle JDK)
- **JavaFX 21** (included via OpenJFX dependency for the visual editor)
- No other runtime dependencies beyond the Maven-managed libraries

## Getting started

```bash
# Clone and build
git clone https://github.com/lwhite1/forrester.git
cd forrester
mvn clean package -DskipTests

# Launch the visual editor
java -jar forrester-app/target/forrester-app-*.jar

# Run a demo from the engine module
mvn -pl forrester-engine exec:java \
    -Dexec.mainClass="com.deathrayresearch.forrester.demo.SirInfectiousDiseaseDemo"
```

Open the editor, go to **File > Open Example**, and pick a model to explore. Or start from scratch: press **8** to place CLD variables, **9** to draw causal links, and sketch your system's causal structure before formalizing it into a simulation.
