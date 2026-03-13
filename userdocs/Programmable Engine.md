# Programmable Engine

The Courant engine provides two code-first APIs for building and running System Dynamics models.

## Lambda-based API (Programmatic)

Build models directly in Java using stocks, flows, and lambda formulas:

```java
Model model = new Model("My Model");

Stock population = new Stock("Population", 1000, ItemUnits.PEOPLE);
model.addStock(population);

Constant birthRate = new Constant("Birth Rate", DimensionlessUnits.DIMENSIONLESS, 0.03);
Flow births = Flows.exponentialGrowth("Births", YEAR, population, birthRate.getValue());
population.addInflow(births);

Simulation sim = new Simulation(model, TimeUnits.DAY, Units.YEAR, 1);
sim.addEventHandler(new CsvSubscriber("output.csv"));
sim.execute();
```

## Definition-based API (Data-Driven)

Define models as pure data, serialize to JSON, and compile to runnable simulations:

```java
ModelDefinition def = new ModelDefinitionBuilder()
    .name("Population Model")
    .stock("Population", 1000, "Person")
    .flow("Births", "Population * birth_rate", "Year", null, "Population")
    .constant("birth_rate", 0.03, "1/Year")
    .defaultSimulation("Day", 365, "Day")
    .build();

List<String> errors = DefinitionValidator.validate(def);
CompiledModel compiled = new ModelCompiler().compile(def);
compiled.createSimulation().execute();
```

Both APIs produce the same simulation output. The lambda-based API is best for models built and run within a single Java program. The definition-based API is best when models need to be saved, shared, or composed from reusable modules.

## Simulation Engine

The engine manages execution over a specified duration with configurable time steps:

1. Post `SimulationStartEvent`
2. For each time step: post `TimeStepEvent`, update stocks, record values
3. Post `SimulationEndEvent`

Supported time steps: Millisecond, Second, Minute, Hour, Day, Week, Month, Year.

### Event System

Uses Google Guava's `EventBus` for decoupled communication. Any `EventHandler` can subscribe to `SimulationStartEvent`, `TimeStepEvent`, and `SimulationEndEvent`.

## Flow Creation

Flows are created via `Flow.create()` or the `Flows` utility class:

```java
// Custom formula
Flow cooling = Flow.create("Cooling", MINUTE, () ->
        new Quantity(discrepancy.getValue() * coolingRate, CELSIUS));

// Factory methods
Flows.constant(name, timeUnit, quantity)
Flows.linearGrowth(name, timeUnit, stock, amount)
Flows.exponentialGrowth(name, timeUnit, stock, rate)
Flows.exponentialGrowthWithLimit(name, timeUnit, stock, rate, limit)
Flows.pipelineDelay(name, timeUnit, inflow, stepSupplier, delay)
```

## Lookup Tables

Piecewise interpolation for nonlinear relationships. Out-of-range inputs clamp to the nearest endpoint.

```java
LookupTable effect = LookupTable.builder()
    .at(0.0, 1.2).at(0.5, 1.0).at(1.0, 0.5).at(1.5, 0.1).at(2.0, 0.0)
    .buildLinear(() -> population.getValue() / carryingCapacity);
```

Supports `linear` (2+ points) and `spline` (3+ points, cubic) interpolation.

## Negative-Stock Guardrails

| Policy | Behavior |
|---|---|
| `CLAMP_TO_ZERO` (default) | Silently clamp to zero |
| `ALLOW` | Permit negative values |
| `THROW` | Throw `IllegalArgumentException` |

## Subscripts / Arrays

Single-dimension arrays expand elements to one instance per label:

```java
Subscript region = new Subscript("Region", "North", "South", "East");
ArrayedStock population = new ArrayedStock("Population", region, 1000, PEOPLE);
ArrayedFlow births = ArrayedFlow.create("Births", DAY, region,
    i -> new Quantity(population.getValue(i) * 0.04, PEOPLE));
```

### Multi-Dimensional Subscripts

`SubscriptRange` composes dimensions for cross-tabulated elements:

```java
SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));  // 3×3 = 9 elements
MultiArrayedStock pop = new MultiArrayedStock("Population", range, 1000, PEOPLE);
```

Supports coordinate access, `sumOver()`, and `slice()`.

### Intelligent Arrays (IndexedValue)

Immutable multi-dimensional values with automatic broadcasting arithmetic:

```java
IndexedValue byRegion = IndexedValue.of(region, 1000, 2000, 500);
IndexedValue byAge = IndexedValue.of(ageGroup, 0.1, 0.2, 0.3);
IndexedValue product = byRegion.multiply(byAge);  // 9-element outer product
```

Shared dimensions align by name; non-shared dimensions expand via outer product.

## Standard SD Functions

| Function | Type | Behavior |
|---|---|---|
| `Step.of(height, stepTime, currentStep)` | Input | Returns 0 before step time, constant height after |
| `Ramp.of(slope, startStep, [endStep], currentStep)` | Input | Linearly increasing from start |
| `Smooth.of(input, smoothingTime, [initialValue], currentStep)` | Delay | First-order exponential smoothing |
| `Delay3.of(input, delayTime, [initialValue], currentStep)` | Delay | Third-order material delay |

## Expression AST

Sealed `Expr` hierarchy for representing formulas as data:

```java
Expr expr = ExprParser.parse("Population * birth_rate + Immigration");
Set<String> deps = ExprDependencies.extract(expr);
String text = ExprStringifier.stringify(expr);
```

Supports arithmetic, comparisons, logical operators, `IF()`, `TIME`, `DT`, and built-in functions: `SMOOTH`, `DELAY3`, `STEP`, `RAMP`, `LOOKUP`, `MIN`, `MAX`, `ABS`, `SQRT`, `EXP`, `LN`, `LOG10`, `SIN`, `COS`.

## Model Compiler

Bridges definitions and executable simulations via two-pass compilation:

```java
ModelCompiler compiler = new ModelCompiler();
CompiledModel compiled = compiler.compile(def);
Simulation sim = compiled.createSimulation();
sim.execute();
```

Handles forward references and nested module compilation with parent-chain name resolution.

## Model Definitions

Immutable Java records for pure-data model representation:

```java
ModelDefinition def = new ModelDefinitionBuilder()
    .name("SIR Model")
    .stock("Susceptible", 990, "Person")
    .flow("Infection", "Susceptible * Infected * contact_rate", "Day", "Susceptible", "Infected")
    .constant("contact_rate", 0.005, "1/Day")
    .lookupTable("effect", new double[]{0, 1}, new double[]{1, 0}, "LINEAR")
    .moduleInstance("workforce", workforceDef, inputBindings, outputBindings)
    .defaultSimulation("Day", 100, "Day")
    .build();
```

### Causal Loop Diagrams

```java
ModelDefinition def = new ModelDefinitionBuilder()
    .name("Climate CLD")
    .cldVariable("CO2 Emissions")
    .cldVariable("Temperature")
    .causalLink("CO2 Emissions", "Temperature", CausalLinkDef.Polarity.POSITIVE)
    .causalLink("Temperature", "Ice Coverage", CausalLinkDef.Polarity.NEGATIVE)
    .build();

FeedbackAnalysis analysis = FeedbackAnalysis.analyze(def);
for (FeedbackAnalysis.CausalLoop loop : analysis.causalLoops()) {
    System.out.println(loop.label() + " (" + loop.type() + "): " + loop.members());
}
```

## JSON Serialization

Round-trip JSON persistence via Jackson:

```java
ModelDefinitionSerializer serializer = new ModelDefinitionSerializer();
serializer.toFile(def, Paths.get("model.json"));
ModelDefinition loaded = serializer.fromFile(Paths.get("model.json"));
```

## Dependency Graph & Auto-Layout

```java
DependencyGraph graph = DependencyGraph.fromDefinition(def);
Set<String> deps = graph.dependenciesOf("Infection");
List<ConnectorRoute> connectors = ConnectorGenerator.generate(def);
ViewDef view = AutoLayout.layout(def);
```

## Measurement System

40+ predefined units across 8 dimensions (Time, Mass, Length, Volume, Money, Item, Temperature, Dimensionless). Unit conversion is automatic; incompatible dimensions are rejected. `UnitRegistry` maps name strings to `Unit` objects.

## Modules

Hierarchical model composition with sub-modules and port bindings:

```java
Module workforce = new Module("Workforce");
workforce.addStock(staff);
workforce.addFlow(hiring);

Module project = new Module("Project");
project.addSubModule(workforce);
```

## Parameter Sweep

```java
SweepResult result = ParameterSweep.builder()
    .parameterName("Contact Rate")
    .parameterValues(ParameterSweep.linspace(2.0, 14.0, 2.0))
    .modelFactory(this::buildSirModel)
    .timeStep(DAY).duration(Times.weeks(8))
    .build().execute();

result.writeTimeSeriesCsv("sweep-timeseries.csv");
```

### Multi-Parameter Sweep

Computes the Cartesian product of N parameter arrays:

```java
MultiSweepResult result = MultiParameterSweep.builder()
    .parameter("Contact Rate", ParameterSweep.linspace(2.0, 14.0, 4.0))
    .parameter("Infectivity", new double[]{0.05, 0.10, 0.15})
    .modelFactory(params -> buildModel(params.get("Contact Rate"), params.get("Infectivity")))
    .timeStep(DAY).duration(Times.weeks(8))
    .build().execute();
```

## Monte Carlo Simulation

```java
MonteCarloResult result = MonteCarlo.builder()
    .parameter("Contact Rate", new NormalDistribution(8, 2))
    .parameter("Infectivity", new UniformRealDistribution(0.05, 0.15))
    .modelFactory(params -> buildModel(params.get("Contact Rate"), params.get("Infectivity")))
    .iterations(200)
    .sampling(SamplingMethod.LATIN_HYPERCUBE)
    .seed(42L)
    .timeStep(DAY).duration(Times.weeks(8))
    .build().execute();

result.writePercentileCsv("output.csv", "Infectious", 2.5, 25, 50, 75, 97.5);
FanChart.show(result, "Infectious");
```

## Optimization / Calibration

```java
OptimizationResult result = Optimizer.builder()
    .parameter("Contact Rate", 1.0, 20.0)
    .parameter("Infectivity", 0.01, 0.50)
    .modelFactory(params -> buildSirModel(
            params.get("Contact Rate"), params.get("Infectivity")))
    .objective(Objectives.fitToTimeSeries("Infectious", observedData))
    .algorithm(OptimizationAlgorithm.NELDER_MEAD)
    .maxEvaluations(500)
    .timeStep(DAY).duration(Times.weeks(8))
    .build().execute();

Map<String, Double> best = result.getBestParameters();
```

Algorithms: Nelder-Mead, BOBYQA, CMA-ES. Built-in objectives: `fitToTimeSeries`, `minimize`, `maximize`, `target`, `minimizePeak`.

## Output & Visualization

- **CsvSubscriber** — writes simulation results to CSV
- **StockLevelChartViewer** — JavaFX real-time line chart of stock/variable values
- **FlowChartViewer** — JavaFX real-time line chart of flow rates
- **ModelReport** — generates structural reports of model composition
