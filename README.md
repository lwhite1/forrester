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
| JUnit 4 | 4.12 | Testing |
| Google Guava | 33.4.0-jre | Event bus, collections |
| Apache Commons Math | 3.6.1 | Mathematical functions |
| OpenCSV | 5.9 | CSV output |
| JavaMoney Moneta | 1.4.4 | Currency handling |
| Logback | 1.5.16 | Logging |
| OpenJFX | 21 | Chart visualization |

## Core Concepts

System Dynamics models are built from four fundamental elements:

- **Stocks** - Accumulations that represent the state of a system (e.g., population, inventory, money). Stocks change only through inflows and outflows.
- **Flows** - Rates of change that add to or drain from stocks (e.g., birth rate, sales rate, spending rate). Flows are defined per unit of time.
- **Variables** - Calculated quantities derived from formulas that may reference stocks, constants, or other variables.
- **Constants** - Fixed exogenous values that parameterize the model.

These elements are connected into feedback loops that drive system behavior over time.

## Architecture

### Package Structure

```
com.deathrayresearch.forrester
├── Simulation.java              # Core simulation engine
├── model/                       # Model elements (Stock, Flow, Variable, Constant, Module)
│   └── flows/                   # Time-unit-specific flow implementations
├── archetypes/                  # Reusable behavioral patterns
├── measure/                     # Dimensional analysis and unit system
├── event/                       # Event-driven communication
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

### Flow Rate Conversion

Flows are defined with a natural time unit (e.g., `FlowPerDay`, `FlowPerWeek`, `FlowPerYear`) and the `RateConverter` automatically translates rates to match the simulation's time step. Each `FlowPer*` class is a convenience constructor that sets the time unit; callers override `quantityPerTimeUnit()` directly from `Flow`.

### Archetypes (Reusable Patterns)

The `archetypes` package provides common system dynamics building blocks:

| Archetype | Behavior |
|---|---|
| `SimpleLinearChange` | Constant amount added/removed per time step |
| `SimpleExponentialChange` | Growth or decay by a fixed multiplier |
| `ExponentialChangeWithLimit` | S-shaped growth that levels off at a carrying capacity |
| `PipelineDelay` | FIFO material delay based on historical inflow values |

### Output & Visualization

- **CsvSubscriber** - Writes simulation results to CSV files (columns: step, datetime, stock levels, variable values)
- **StockLevelChartViewer** - JavaFX real-time line chart of stock and variable values over time
- **FlowChartViewer** - JavaFX real-time line chart of flow rates over time
- **ModelReport** - Generates structural reports describing a model's composition

The chart viewers support series toggling via checkboxes and image export.

## Example Models

The test suite contains a rich set of example models demonstrating the framework's capabilities:

### Fundamental

| Model | Demonstrates |
|---|---|
| `ExponentialGrowthModel` | Unconstrained population growth with birth/death rates |
| `ExponentialDecayModel` | Exponential decay pattern |
| `CoffeeCoolingModel` | Newton's law of cooling via negative feedback |
| `TubTest` | Simple bathtub inflow/outflow |
| `sShapedPopulationGrowth` | Logistic growth with a carrying capacity |

### Delays

| Model | Demonstrates |
|---|---|
| `FirstOrderMaterialDelay` | Exponential smoothing delay |
| `SecondOrderMaterialDelay` | Two-stage material delay |
| `ThirdOrderMaterialDelay` | Three-stage material delay |
| `SimplePipelineDelay` | FIFO pipeline delay |

### Feedback & Interaction

| Model | Demonstrates |
|---|---|
| `NegativeFeedbackWithGoalTest` | Inventory adjustment toward a target level |
| `SirInfectiousDiseaseModel` | SIR epidemiological model (Susceptible/Infected/Recovered) |
| `PredatorPreyModel` | Lotka-Volterra predator-prey dynamics |
| `InventoryModel` | Inventory management with perception, response, and delivery delays |
| `SalesModel` | Sales pipeline dynamics |

### Software Development

| Model | Demonstrates |
|---|---|
| `AgileSoftwareDevelopment` | Agile team with backlog, defects, and productivity |
| `WaterfallSoftwareDevelopment` | Waterfall methodology with rework cycles |

The `largemodels/` directory contains more elaborate versions of the software development models with separate modules for development, workforce, testing, rework, and staff allocation.

## Usage Pattern

A typical workflow for building and running a model:

```java
// 1. Create a model
Model model = new Model("My Model");

// 2. Define stocks with initial values
Stock population = new Stock("Population", 1000, ItemUnits.PEOPLE);
model.addStock(population);

// 3. Define flows attached to stocks
Flow births = new FlowPerYear("Births") {
    @Override
    protected Quantity quantityPerTimeUnit() {
        return new Quantity(population.getValue() * birthRate, ItemUnits.PEOPLE);
    }
};
population.addInflow(births);

// 4. Add variables and constants as needed
Constant birthRate = new Constant("Birth Rate", DimensionlessUnits.DIMENSIONLESS, 0.03);

// 5. Create and configure the simulation
Simulation sim = new Simulation(model, TimeUnits.DAY, Units.YEAR, 1);

// 6. Attach output handlers
sim.addEventHandler(new CsvSubscriber("output.csv"));
sim.addEventHandler(new StockLevelChartViewer());

// 7. Run
sim.execute();
```

## Project Status

The project is at version 1.0-SNAPSHOT and is under active development. Recent work has focused on:

- Making `Quantity` fully immutable for safer value semantics
- Simplifying the `FlowPer*` class hierarchy to thin convenience constructors
- Introducing `ItemUnit` for user-defined units with `Item` dimension
- Adding input validation and null safety across core model elements
- Improving test coverage with dedicated unit tests for the simulation engine, stocks, models, modules, and rate conversion
