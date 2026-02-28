# Forrester - System Dynamics Simulation Engine

## Overview

Forrester is a Java implementation of a System Dynamics simulation engine developed by Death Ray Research. System Dynamics is a methodology for modeling complex systems characterized by feedback loops, significant delays, and non-linear interactions. The framework provides the building blocks to construct, run, and visualize such models.

The engine is designed for creating training simulations, games, scenario testing, and planning models across domains including ecology, project management, software development, business strategy, operations, medicine, and international relations.

## Build & Configuration

- **Language:** Java 17+
- **Build system:** Maven
- **Artifact:** `com.deathrayresearch:dynamics:1.0-SNAPSHOT`

### Key Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| JUnit 5 | 5.11.4 | Testing |
| Google Guava | 33.4.0-jre | Event bus, collections |
| Apache Commons Math | 3.6.1 | Mathematical functions |
| OpenCSV | 5.9 | CSV output |
| JavaMoney Moneta | 1.4.4 | Currency handling |
| Logback | 1.5.16 | Logging |
| OpenJFX | 21 | Chart visualization |

## Core Concepts

System Dynamics models are built from four fundamental elements:

- **Stocks** - Accumulations that represent the state of a system (e.g., population, inventory, money). Stocks change only through inflows and outflows. By default, stocks clamp negative values to zero (configurable via `NegativeValuePolicy`).
- **Flows** - Rates of change that add to or drain from stocks (e.g., birth rate, sales rate, spending rate). Flows are defined per unit of time.
- **Variables** - Calculated quantities derived from formulas that may reference stocks, constants, or other variables.
- **Constants** - Fixed exogenous values that parameterize the model.
- **Lookup Tables** - Piecewise interpolation curves for modeling nonlinear effects (e.g., "effect of crowding on birth rate"). Supports linear and cubic spline interpolation.

These elements are connected into feedback loops that drive system behavior over time.

## Architecture

### Package Structure

```
com.deathrayresearch.forrester
├── Simulation.java              # Core simulation engine
├── model/                       # Model elements (Stock, Flow, Flows, Variable, Constant, Module)
│   └── flows/                   # Rate conversion utilities
├── measure/                     # Dimensional analysis and unit system
├── event/                       # Event-driven communication
├── sweep/                       # Parameter sweep runner and CSV output
├── io/                          # CSV export and reporting
└── ui/                          # JavaFX chart visualization
```

### Simulation Engine (`Simulation.java`)

The simulation engine manages execution of a model over a specified duration with configurable time steps. It follows this lifecycle:

1. Post `SimulationStartEvent`
2. For each time step:
   - Post `TimeStepEvent` (carrying current time, step number, model state)
   - Update all stock levels based on their inflows and outflows
   - Record variable and flow values
3. Post `SimulationEndEvent`

Supported time steps: Second, Minute, Hour, Day, Week.

### Event System

The engine uses Google Guava's `EventBus` for decoupled communication. Any class implementing `EventHandler` can subscribe to simulation events. This allows multiple observers (CSV export, UI charts) to operate without coupling to the simulation core.

Event types:
- `SimulationStartEvent` - fired once at the beginning
- `TimeStepEvent` - fired at each simulation step
- `SimulationEndEvent` - fired once at completion

### Measurement System (`measure/` package)

A dimension-aware quantity system ensures unit correctness:

- **Dimensions:** Time, Mass, Length, Volume, Money, Item, Temperature, Dimensionless
- **25+ predefined units** including Second, Minute, Hour, Day, Week, Year, People, Thing, Meter, Foot, Mile, Kilogram, Pound, USD, Liter, Gallon, Centigrade, and more
- **Unit conversion** is handled automatically; incompatible dimensions (e.g., adding miles to pounds) are rejected
- `Quantity` objects are fully immutable - all operations return new instances

### Flow Creation

Flows are created via the `Flow.create()` static factory or the higher-level `Flows` utility class. Both accept a name, time unit, and a lambda that computes the quantity per time step. The `RateConverter` automatically translates rates to match the simulation's time step.

**`Flow.create()`** — general-purpose factory for any custom flow formula:

```java
Flow cooling = Flow.create("Cooling", MINUTE, () ->
        new Quantity(discrepancy.getValue() * coolingRate, CENTIGRADE));
```

**`Flows`** — factory methods for common system dynamics patterns:

| Method | Behavior |
|---|---|
| `Flows.constant(name, timeUnit, quantity)` | Fixed quantity per time step |
| `Flows.linearGrowth(name, timeUnit, stock, amount)` | Constant amount added/removed per step |
| `Flows.exponentialGrowth(name, timeUnit, stock, rate)` | Growth or decay proportional to stock level |
| `Flows.exponentialGrowthWithLimit(name, timeUnit, stock, rate, limit)` | S-shaped growth that levels off at a carrying capacity |
| `Flows.pipelineDelay(name, timeUnit, inflow, stepSupplier, delay)` | FIFO material delay based on historical inflow values |

### Lookup Tables

`LookupTable` implements `Formula` and provides the standard SD "table function" for modeling nonlinear relationships. Input values are mapped to output values via interpolation between user-defined data points. Out-of-range inputs are clamped to the nearest endpoint (standard SD convention).

**Static factories:**

```java
LookupTable effect = LookupTable.linear(
    new double[]{0, 0.5, 1.0, 1.5, 2.0},
    new double[]{1.2, 1.0, 0.5, 0.1, 0.0},
    () -> population.getValue() / carryingCapacity);
```

**Fluent builder** (auto-sorts by x):

```java
LookupTable effect = LookupTable.builder()
    .at(0.0, 1.2).at(0.5, 1.0).at(1.0, 0.5).at(1.5, 0.1).at(2.0, 0.0)
    .buildLinear(() -> population.getValue() / carryingCapacity);
```

| Method | Min points | Behavior |
|---|---|---|
| `LookupTable.linear()` / `buildLinear()` | 2 | Straight-line segments between points |
| `LookupTable.spline()` / `buildSpline()` | 3 | Cubic spline for smooth curves (may overshoot with steep transitions) |

### Negative-Stock Guardrails

Stocks enforce a `NegativeValuePolicy` that controls what happens when a value would go negative. The default policy (`CLAMP_TO_ZERO`) prevents physical quantities from going negative — matching the behavior of SD tools like Vensim and Stella.

| Policy | Behavior |
|---|---|
| `CLAMP_TO_ZERO` (default) | Silently clamp to zero |
| `ALLOW` | Permit negative values (e.g., bank balances) |
| `THROW` | Throw `IllegalArgumentException` |

```java
// Default: negative values are clamped to zero
Stock inventory = new Stock("Inventory", 100, THING);

// Allow negative values via constructor
Stock balance = new Stock("Balance", -500, US_DOLLAR, NegativeValuePolicy.ALLOW);

// Change policy after construction
inventory.setNegativeValuePolicy(NegativeValuePolicy.THROW);
```

### Standard SD Functions

The framework provides built-in implementations of the four standard SD input and delay functions. All implement `Formula` and take an `IntSupplier` for the current simulation timestep.

**Input functions:**

```java
// Step: returns 0 before step 10, then 100
Variable shock = new Variable("Demand Shock", THING, Step.of(100, 10, sim::getCurrentStep));

// Ramp: increases by 5 per step starting at step 10 (optionally bounded)
Variable ramp = new Variable("Demand Ramp", THING, Ramp.of(5, 10, sim::getCurrentStep));
Variable bounded = new Variable("Bounded Ramp", THING, Ramp.of(5, 10, 20, sim::getCurrentStep));
```

**Delay functions:**

```java
// Smooth (first-order information delay): smooths input over 5 timesteps
Smooth perceived = Smooth.of(() -> actualDemand.getValue(), 5, sim::getCurrentStep);

// Delay3 (third-order material delay): delays input by 6 timesteps
Delay3 delayed = Delay3.of(() -> orders.getValue(), 6, sim::getCurrentStep);
```

| Function | Type | Behavior |
|---|---|---|
| `Step.of(height, stepTime, currentStep)` | Input | Returns 0 before step time, constant height after |
| `Ramp.of(slope, startStep, [endStep], currentStep)` | Input | Linearly increasing from start, optionally bounded |
| `Smooth.of(input, smoothingTime, [initialValue], currentStep)` | Delay | First-order exponential smoothing (information delay) |
| `Delay3.of(input, delayTime, [initialValue], currentStep)` | Delay | Third-order material delay with 3 internal stages |

Smooth and Delay3 default their initial value to the first input (standard SD convention). An explicit initial value can be provided as an optional parameter.

### Parameter Sweep (`sweep/` package)

The `ParameterSweep` runner iterates an array of parameter values, builds a fresh model per value via a `DoubleFunction<Model>` factory, runs each simulation, and collects results. Each run gets its own object graph — no shared mutable state, no reset needed.

```java
SweepResult result = ParameterSweep.builder()
    .parameterName("Contact Rate")
    .parameterValues(ParameterSweep.linspace(2.0, 14.0, 2.0))
    .modelFactory(this::buildSirModel)
    .timeStep(DAY)
    .duration(Times.weeks(8))
    .build()
    .execute();

result.writeTimeSeriesCsv("sweep-timeseries.csv");
result.writeSummaryCsv("sweep-summary.csv");
```

| Class | Purpose |
|---|---|
| `ParameterSweep` | Builder API + execute loop; includes `linspace()` utility for evenly spaced values |
| `RunResult` | `EventHandler` that snapshots stock/variable values at each timestep for one run |
| `SweepResult` | Aggregates run results; delegates to `SweepCsvWriter` for CSV export |
| `SweepCsvWriter` | Static methods for time-series CSV (all steps, all runs) and summary CSV (final/peak per run) |

### Monte Carlo Simulation (`sweep/` package)

The `MonteCarlo` runner extends the sweep infrastructure to uncertainty analysis. Multiple parameters are sampled from probability distributions (using Apache Commons Math) across hundreds of runs. Results are aggregated into percentile envelopes for statistical analysis and fan chart visualization.

**Sampling methods:**

| Method | Description |
|---|---|
| `RANDOM` | Pure Monte Carlo — independent random draws from each distribution |
| `LATIN_HYPERCUBE` (default) | Stratified sampling — better coverage of the parameter space with fewer iterations |

```java
MonteCarloResult result = MonteCarlo.builder()
    .parameter("Contact Rate", new NormalDistribution(8, 2))
    .parameter("Infectivity", new UniformRealDistribution(0.05, 0.15))
    .modelFactory(params -> buildModel(params.get("Contact Rate"), params.get("Infectivity")))
    .iterations(200)
    .sampling(SamplingMethod.LATIN_HYPERCUBE)
    .seed(42L)
    .timeStep(DAY)
    .duration(Times.weeks(8))
    .build()
    .execute();

// Extract percentile series for analysis
double[] median = result.getPercentileSeries("Infectious", 50);
double[] mean = result.getMeanSeries("Infectious");

// Export percentile envelope to CSV
result.writePercentileCsv("output.csv", "Infectious", 2.5, 25, 50, 75, 97.5);

// Display interactive fan chart
FanChart.show(result, "Infectious");
```

| Class | Purpose |
|---|---|
| `MonteCarlo` | Builder API + execute loop with random/LHS sampling; accepts `RealDistribution` per parameter |
| `MonteCarloResult` | Aggregates run results; computes percentile and mean series via `DescriptiveStatistics`; CSV export |
| `SamplingMethod` | Enum: `RANDOM` or `LATIN_HYPERCUBE` |
| `FanChart` | JavaFX Canvas-based fan chart — renders nested percentile bands (95%, 75%, 50%) with median line |

### Output & Visualization

- **CsvSubscriber** - Writes simulation results to CSV files (columns: step, datetime, stock levels, variable values)
- **StockLevelChartViewer** - JavaFX real-time line chart of stock and variable values over time
- **FlowChartViewer** - JavaFX real-time line chart of flow rates over time
- **ModelReport** - Generates structural reports describing a model's composition

The chart viewers support series toggling via checkboxes and image export.

## Example Models

The demo package (`src/main/java/.../demo/`) contains a rich set of example models demonstrating the framework's capabilities. Each demo has a `main()` entry point and can be run directly.

### Fundamental

**ExponentialGrowthDemo** — Models unconstrained population growth. A single Population stock has an exponential birth inflow (4%/day) and death outflow (3%/day). The positive net rate produces the classic exponential growth curve driven by a reinforcing feedback loop.

**ExponentialDecayDemo** — Models a population declining with no births. A Population stock drains through a death outflow proportional to its current level (1/80 per day), decaying asymptotically toward zero — the basic exponential decay pattern.

**CoffeeCoolingDemo** — Simulates Newton's law of cooling. A Coffee Temperature stock (100 °C) cools toward room temperature (18 °C) via a negative-feedback outflow proportional to the temperature gap. The cooling rate decelerates as the gap narrows, producing a goal-seeking decay curve.

**TubDemo** — The classic bathtub model, the simplest stock-and-flow demonstration. A Water-in-Tub stock (50 gallons) drains at 5 gal/min while inflow is delayed 5 minutes. The tub level drops then stabilizes, showing how a stock accumulates the difference between inflow and outflow.

**SShapedPopulationGrowthDemo** — Models logistic population growth constrained by a carrying capacity (1,000). Growth starts exponentially, inflects at the midpoint, and levels off — the S-curve produced by a balancing loop limiting a reinforcing loop.

**FlowTimeDemo** — Models turnaround time (TAT) for a work queue with demand and capacity constraints. Two stocks (WIP and TAT) interact through flows defined at different time units (per-day demand, per-hour adjustment), demonstrating the engine's automatic rate conversion.

**LookupTableDemo** — Models population growth modulated by a crowding-effect lookup table. As the population-to-capacity ratio increases, a `LookupTable` maps it to a declining growth multiplier, producing S-shaped growth with a user-defined nonlinear curve rather than an algebraic approximation.

### Delays

**FirstOrderMaterialDelayDemo** — Demonstrates a first-order material delay (exponential smoothing). A Potential Customers stock drains at a rate equal to stock level divided by a 120-day average delay. Assumes full mixing (no FIFO), producing exponential decay — the simplest material delay.

**ThirdOrderMaterialDelayDemo** — Chains three first-order material delays (7 h, 6.3 h, 3.2 h activity times) to form a third-order delay. A Total WIP variable tracks combined inventory. The cascaded stages smooth output more than a single delay, producing a bell-shaped throughput response to a step input.

**SimplePipelineDelayDemo** — Demonstrates a FIFO pipeline delay. A WIP stock receives 5 items/day; departures replay the arrival history shifted by a 3-day constant using `Flows.pipelineDelay()`. WIP rises for 3 days then stabilizes — in contrast to the material delays which assume mixing.

### Feedback & Interaction

**NegativeFeedbackDemo** — Demonstrates goal-seeking behavior via negative feedback. An Inventory stock (1,000 units) adjusts toward a target of 860 through a production inflow proportional to the gap divided by an 8-day adjustment time, approaching the goal asymptotically.

**SirInfectiousDiseaseDemo** — Implements the classic SIR epidemiological model with three stocks: Susceptible (1,000), Infectious (10), and Recovered (0). Infection depends on contact rate, infectious fraction, and infectivity. Produces the characteristic epidemic curve — Infectious peaks then falls as the susceptible pool is depleted.

**SirSweepDemo** — Demonstrates parameter sweeps using the SIR model. Sweeps contact rate from 2 to 14 (step 2) across 7 runs, writing time-series and summary CSVs to the system temp directory. Higher contact rates produce dramatically higher peak infections (10 → 480) and deplete the susceptible pool more completely.

**SirMonteCarloDemo** — Demonstrates Monte Carlo simulation on the SIR model with two uncertain parameters: contact rate (Normal distribution, mean=8, sd=2) and infectivity (Uniform distribution, 0.05–0.15). Runs 200 iterations with Latin Hypercube Sampling, writes percentile CSV output, and displays a fan chart showing the uncertainty envelope around the Infectious stock trajectory.

**PredatorPreyDemo** — Implements the Lotka-Volterra predator-prey model. Prey (Rabbits) and predator (Foxes) populations are coupled through birth and death flows that depend on both species' levels. The model produces sustained oscillations where predator peaks lag prey peaks.

**InventoryModelDemo** — Models a car dealership's inventory with perception (5-day), response (3-day), and delivery (5-day) delays, inspired by *Thinking in Systems*. A step increase in demand causes the dealer to overshoot and oscillate before settling, demonstrating how delays in feedback loops amplify fluctuations.

**SalesMixDemo** — Models the evolving revenue mix between one-time hardware sales and recurring service revenue. A Customers stock grows linearly; hardware revenue scales with new acquisitions while service revenue scales with total customers. Over time the hardware proportion falls, showing how stock-dependent flows shift composition.

### Software Development

**AgileSoftwareDevelopmentDemo** — Models an agile project with product/release/sprint backlogs, completed work, latent defects, and known defects. Work flows from sprint backlog to completion at a bounded productivity rate, generating defects at a fraction of the completion rate. Defects are discovered and fixed through separate feedback loops, illustrating rework dynamics.

**WaterfallSoftwareDevelopmentDemo** — Models a waterfall project using four composable Modules: Workforce, Development, Staff Allocation, and Test & Rework. Hiring delays, training overhead, defect injection, and rework cycles interact to show how phased development with late testing can lead to schedule overruns and staffing oscillations.

## Usage Pattern

A typical workflow for building and running a model:

```java
// 1. Create a model
Model model = new Model("My Model");

// 2. Define stocks with initial values
Stock population = new Stock("Population", 1000, ItemUnits.PEOPLE);
model.addStock(population);

// 3. Define flows — use Flows factory for common patterns, or Flow.create() for custom formulas
Constant birthRate = new Constant("Birth Rate", DimensionlessUnits.DIMENSIONLESS, 0.03);
Flow births = Flows.exponentialGrowth("Births", YEAR, population, birthRate.getValue());
population.addInflow(births);

// 4. Create and configure the simulation
Simulation sim = new Simulation(model, TimeUnits.DAY, Units.YEAR, 1);

// 5. Attach output handlers
sim.addEventHandler(new CsvSubscriber("output.csv"));
sim.addEventHandler(new StockLevelChartViewer());

// 6. Run
sim.execute();
```

## Learning System Dynamics

New to system dynamics? These resources provide a solid introduction to the methodology behind this framework:

### Books

- [Thinking in Systems: A Primer](https://www.chelseagreen.com/product/thinking-in-systems/) by Donella Meadows — the classic accessible introduction to stocks, flows, feedback loops, and system behavior. Several of this project's demo models are drawn from examples in the book.

### Online Courses

- [MIT OCW: Introduction to System Dynamics](https://ocw.mit.edu/courses/15-871-introduction-to-system-dynamics-fall-2013/) — full MIT course with lectures, readings, and assignments covering conceptual and modeling skills
- [MIT OCW: System Dynamics Self Study](https://ocw.mit.edu/courses/15-988-system-dynamics-self-study-fall-1998-spring-1999/) — comprehensive self-paced "Road Maps" series with papers and simulation exercises
- [MIT OCW: Systems Thinking and Modeling for a Complex World](https://ocw.mit.edu/courses/res-15-004-system-dynamics-systems-thinking-and-modeling-for-a-complex-world-january-iap-2020/) — shorter IAP workshop covering core concepts

### Tutorials & References

- [System Dynamics Society: Introduction to System Dynamics](https://systemdynamics.org/introduction-to-system-dynamics-modeling/) — overview and webinars from the professional society
- [System Dynamics Learning Guide](https://pressbooks.lib.jmu.edu/sdlearningguide/chapter/chapter-4-introduction-to-system-dynamics-modeling/) — open textbook chapter on modeling fundamentals
- [Donella Meadows Project: Systems Thinking Resources](https://donellameadows.org/systems-thinking-resources/) — curated collection from the Donella Meadows Institute
- [Wikipedia: System Dynamics](https://en.wikipedia.org/wiki/System_dynamics) — broad overview of the field's history, concepts, and applications

## Project Status

The project is at version 1.0-SNAPSHOT and is under active development. Recent work has focused on:

- Adding `MonteCarlo` runner for uncertainty analysis — samples multiple parameters from probability distributions (Normal, Uniform, Triangular, etc.) via random or Latin Hypercube Sampling, aggregates results into percentile envelopes, and visualizes as fan charts
- Adding `ParameterSweep` runner for multi-run analysis — sweeps a parameter across an array of values, builds a fresh model per value, and collects results into time-series and summary CSV output
- Adding standard SD functions: `Step`, `Ramp`, `Smooth` (first-order information delay), and `Delay3` (third-order material delay)
- Adding `LookupTable` for piecewise interpolation curves (linear and cubic spline) — the standard SD mechanism for nonlinear effects
- Adding `NegativeValuePolicy` guardrails to `Stock` — prevents physical quantities from going negative by default
- Adding `Flows` factory class and `Flow.create()` to eliminate flow boilerplate — common patterns are now one-liners
- Consolidating archetype logic into `Flows` and removing the `archetypes` package
- Removing `FlowPer*` convenience subclasses in favor of lambda-based `Flow.create()`
- Making `Quantity` fully immutable for safer value semantics
- Introducing `ItemUnit` for user-defined units with `Item` dimension
- Adding input validation and null safety across core model elements
- Improving test coverage with dedicated unit tests for the simulation engine, stocks, flows, models, modules, and rate conversion
