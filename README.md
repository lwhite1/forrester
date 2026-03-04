# Forrester - System Dynamics Modeling Tool

## Overview

Forrester is a Java implementation of a System Dynamics simulation engine and visual modeling tool developed by Death Ray Research. System Dynamics is a methodology for modeling complex systems characterized by feedback loops, significant delays, and non-linear interactions. The framework provides the building blocks to construct, run, and visualize such models — both programmatically through a code-first API and interactively through a JavaFX canvas-based visual editor.

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
| Apache Commons Math | 3.6.1 | Mathematical functions, derivative-free optimization |
| OpenCSV | 5.9 | CSV output |
| JavaMoney Moneta | 1.4.4 | Currency handling |
| Logback | 1.5.16 | Logging |
| OpenJFX | 21 | Visual editor canvas and chart visualization |

## Core Concepts

System Dynamics models are built from four fundamental elements:

- **Stocks** - Accumulations that represent the state of a system (e.g., population, inventory, money). Stocks change only through inflows and outflows. By default, stocks clamp negative values to zero (configurable via `NegativeValuePolicy`).
- **Flows** - Rates of change that add to or drain from stocks (e.g., birth rate, sales rate, spending rate). Flows are defined per unit of time.
- **Variables** - Calculated quantities derived from formulas that may reference stocks, constants, or other variables.
- **Constants** - Fixed exogenous values that parameterize the model.
- **Lookup Tables** - Piecewise interpolation curves for modeling nonlinear effects (e.g., "effect of crowding on birth rate"). Supports linear and cubic spline interpolation.
- **Subscripts / Arrays** - Dimensions that expand a single stock, flow, or variable definition into N parallel instances (e.g., `Population[North]`, `Population[South]`, `Population[East]`). Supports single-dimension arrays and multi-dimensional subscripts (e.g., `Population[North,Young]`, `Population[South,Elder]`) for cross-tabulated modeling. Enables modeling of multiple regions, products, cohorts, or any combination without duplicating stocks manually.

These elements are connected into feedback loops that drive system behavior over time.

## Architecture

### Package Structure

```
com.deathrayresearch.forrester
├── Simulation.java              # Core simulation engine
├── model/                       # Model elements (Stock, Flow, Flows, Variable, Constant, Module,
│   │                            #   Subscript, ArrayedStock, ArrayedFlow, ArrayedVariable,
│   │                            #   SubscriptRange, MultiArrayedStock, MultiArrayedFlow, MultiArrayedVariable,
│   │                            #   IndexedValue)
│   ├── flows/                   # Rate conversion utilities
│   ├── expr/                    # Sealed Expr AST, recursive-descent parser, stringifier, dependency extractor
│   ├── def/                     # Immutable definition records (ModelDefinition, StockDef, FlowDef, etc.)
│   ├── compile/                 # Two-pass ModelCompiler: definition → runnable Model
│   └── graph/                   # Dependency graph, connector generation, auto-layout, view validation
├── measure/                     # Dimensional analysis, unit system, and UnitRegistry
├── event/                       # Event-driven communication
├── sweep/                       # Parameter sweep, Monte Carlo, optimization, and CSV output
├── io/                          # CSV export, reporting, JSON serialization, Vensim .mdl import, and XMILE import/export
├── ui/                          # JavaFX chart visualization
└── app/                         # JavaFX canvas-based visual editor (forrester-app module)
    └── canvas/                  # Canvas rendering, interaction, editing, and simulation integration
```

### Simulation Engine (`Simulation.java`)

The simulation engine manages execution of a model over a specified duration with configurable time steps. It follows this lifecycle:

1. Post `SimulationStartEvent`
2. For each time step:
   - Post `TimeStepEvent` (carrying current time, step number, model state)
   - Update all stock levels based on their inflows and outflows
   - Record variable and flow values
3. Post `SimulationEndEvent`

Supported time steps: Millisecond, Second, Minute, Hour, Day, Week, Month, Year.

### Event System

The engine uses Google Guava's `EventBus` for decoupled communication. Any class implementing `EventHandler` can subscribe to simulation events. This allows multiple observers (CSV export, UI charts) to operate without coupling to the simulation core.

Event types:
- `SimulationStartEvent` - fired once at the beginning
- `TimeStepEvent` - fired at each simulation step
- `SimulationEndEvent` - fired once at completion

### Measurement System (`measure/` package)

A dimension-aware quantity system ensures unit correctness:

- **Dimensions:** Time, Mass, Length, Volume, Money, Item, Temperature, Dimensionless
- **40 predefined units** across 8 dimensions including Millisecond, Second, Minute, Hour, Day, Week, Month, Year, Millimeter, Centimeter, Meter, Kilometer, Inch, Foot, Yard, Mile, Nautical Mile, Milligram, Gram, Kilogram, Metric Ton, Ounce, Pound, Short Ton, Milliliter, Liter, Cubic Meter, Cup, Pint, Quart, Gallon, Imperial Gallon, Barrel, Celsius, Fahrenheit, USD, People, Thing
- **Unit conversion** is handled automatically; incompatible dimensions (e.g., adding miles to pounds) are rejected
- `Quantity` objects are fully immutable - all operations return new instances

### Flow Creation

Flows are created via the `Flow.create()` static factory or the higher-level `Flows` utility class. Both accept a name, time unit, and a lambda that computes the quantity per time step. The `RateConverter` automatically translates rates to match the simulation's time step.

**`Flow.create()`** — general-purpose factory for any custom flow formula:

```java
Flow cooling = Flow.create("Cooling", MINUTE, () ->
        new Quantity(discrepancy.getValue() * coolingRate, CELSIUS));
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

### Subscripts / Arrays

Subscripts let you define a dimension (e.g., Region) and create arrayed stocks, flows, and variables that expand to one instance per element. Each expanded element is named `"BaseName[label]"` (e.g., `"Population[North]"`), and is transparently expanded into the model's flat stock/variable list — the simulation loop and all output infrastructure work without changes.

**Define a dimension and create arrayed elements:**

```java
// Define a subscript dimension
Subscript region = new Subscript("Region", "North", "South", "East");

// Create an arrayed stock — one Stock per element
ArrayedStock population = new ArrayedStock("Population", region, 1000, PEOPLE);

// Per-element initial values
ArrayedStock infectious = new ArrayedStock("Infectious", region,
    new double[]{10, 0, 0}, PEOPLE);

// Create an arrayed flow with index-aware formula
ArrayedFlow births = ArrayedFlow.create("Births", DAY, region,
    i -> new Quantity(population.getValue(i) * 0.04, PEOPLE));
population.addInflow(births);

// Create an arrayed variable
ArrayedVariable totalPop = ArrayedVariable.create("TotalPop", PEOPLE, region,
    i -> susceptible.getValue(i) + infectious.getValue(i) + recovered.getValue(i));

// Add to model — expands into 3 stocks + 3 flows automatically
model.addArrayedStock(population);
model.addArrayedVariable(totalPop);
```

**Access individual elements:**

```java
double north = population.getValue(0);            // by index
double south = population.getValue("South");       // by label
double total = population.sum();                   // aggregate
Stock northStock = population.getStock(0);         // raw Stock access
```

**Cross-element flows** (e.g., migration between regions) use scalar `Flow.create()` referencing specific array elements:

```java
Flow migration = Flow.create("Migration[North->South]", DAY,
    () -> new Quantity(infectious.getValue("North") * migrationRate, PEOPLE));
infectious.getStock("North").addOutflow(migration);
infectious.getStock("South").addInflow(migration);
```

| Class | Purpose |
|---|---|
| `Subscript` | Immutable dimension definition with name and labels |
| `ArrayedStock` | Wraps N `Stock` instances — uniform or per-element initial values |
| `ArrayedFlow` | Wraps N `Flow` instances with index-aware formula |
| `ArrayedVariable` | Wraps N `Variable` instances with index-aware formula |

### Multi-Dimensional Subscripts

Multi-dimensional subscripts compose two or more `Subscript` dimensions into a `SubscriptRange`, expanding model elements to one instance per combination. Each expanded element is named with comma-separated labels (e.g., `"Population[North,Young]"`), following the Vensim convention. Like single-dimension arrays, the expansion is transparent — the simulation loop and all output infrastructure work without changes.

**Define dimensions and create multi-arrayed elements:**

```java
// Define two subscript dimensions
Subscript region = new Subscript("Region", "North", "South", "East");
Subscript ageGroup = new Subscript("AgeGroup", "Young", "Adult", "Elder");

// Compose into a multi-dimensional range
SubscriptRange range = new SubscriptRange(List.of(region, ageGroup));  // 3×3 = 9 elements

// Create a multi-arrayed stock — one Stock per combination
MultiArrayedStock pop = new MultiArrayedStock("Population", range, 1000, PEOPLE);

// Per-element initial values (row-major order: [North,Young], [North,Adult], ...)
MultiArrayedStock pop2 = new MultiArrayedStock("Population", range,
    new double[]{500,400,100, 500,400,100, 500,400,100}, PEOPLE);

// Create a multi-arrayed flow with coordinate-aware formula
MultiArrayedFlow births = MultiArrayedFlow.create("Births", DAY, range,
    coords -> new Quantity(pop.getValueAt(coords) * 0.04, PEOPLE));
pop.addInflow(births);

// Create a multi-arrayed variable
MultiArrayedVariable density = MultiArrayedVariable.create("Density", PEOPLE, range,
    coords -> pop.getValueAt(coords) / area[coords[0]]);

// Add to model — expands into 9 stocks automatically
model.addMultiArrayedStock(pop);
model.addMultiArrayedVariable(density);
```

**Access individual elements:**

```java
double val = pop.getValueAt(1, 2);               // by coordinate indices (South, Elder)
double val2 = pop.getValueAt("North", "Young");   // by labels
Stock stock = pop.getStockAt("South", "Adult");    // raw Stock access
double total = pop.sum();                          // aggregate all elements
```

**Aggregation and slicing:**

```java
// Sum over one dimension
double[] perRegion = pop.sumOver(1);   // collapse AgeGroup → double[3], one per Region
double[] perAge = pop.sumOver(0);      // collapse Region → double[3], one per AgeGroup

// Fix one dimension to get a slice
Stock[] northStocks = pop.slice(0, "North");  // 3 stocks: [North,Young], [North,Adult], [North,Elder]
```

| Class | Purpose |
|---|---|
| `SubscriptRange` | Multi-dimensional index manager — cartesian product, flat↔coordinate index math, name composition |
| `MultiArrayedStock` | Wraps N×M×... `Stock` instances with coordinate access, `sum()`, `sumOver()`, `slice()` |
| `MultiArrayedFlow` | Wraps N×M×... `Flow` instances with coordinate-aware or flat-index formulas |
| `MultiArrayedVariable` | Wraps N×M×... `Variable` instances with coordinate-aware or flat-index formulas |

### Intelligent Arrays (IndexedValue)

`IndexedValue` provides immutable multi-dimensional values with automatic broadcasting arithmetic — matching the "intelligent array" semantics of tools like Analytica. When two values with different dimensions are combined, shared dimensions align by name and non-shared dimensions expand via outer product. This eliminates manual looping over subscript combinations.

**Broadcasting rules:**

| Left | Right | Result |
|---|---|---|
| `[Region]` | `[Region]` | Elementwise `[Region]` |
| `scalar` | `[Region]` | Broadcast scalar to every element → `[Region]` |
| `[Region]` | `[AgeGroup]` | Outer product → `[Region × AgeGroup]` |
| `[Region × AgeGroup]` | `[Region]` | Broadcast Region-only value across AgeGroup → `[Region × AgeGroup]` |
| `[Region × AgeGroup]` | `[AgeGroup × Scenario]` | Shared AgeGroup aligned, others expanded → `[Region × AgeGroup × Scenario]` |

**Create indexed values:**

```java
// Scalar
IndexedValue rate = IndexedValue.scalar(0.03);

// One-dimensional
IndexedValue population = IndexedValue.of(region, 1000, 2000, 500);

// Multi-dimensional
IndexedValue grid = IndexedValue.of(range, new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9});

// Fill all elements with a constant
IndexedValue uniform = IndexedValue.fill(region, 100.0);
```

**Arithmetic with broadcasting:**

```java
// Scalar broadcast: multiply every region by a rate
IndexedValue growth = population.multiply(rate);

// Cross-dimension: population[Region] * rate[AgeGroup] → [Region × AgeGroup]
IndexedValue byRegion = IndexedValue.of(region, 1000, 2000, 500);
IndexedValue byAge = IndexedValue.of(ageGroup, 0.1, 0.2, 0.3);
IndexedValue product = byRegion.multiply(byAge);  // 9-element result

// Chained operations
IndexedValue netGrowth = population.multiply(birthRate).subtract(population.multiply(deathRate));
```

**Aggregation:**

```java
double total = population.sum();
double avg = population.mean();
double highest = population.max();

// Collapse a dimension by summing over it
IndexedValue byAge = product.sumOver(region);  // [Region × AgeGroup] → [AgeGroup]
```

**Access from arrayed model elements:**

```java
// Convenience methods on ArrayedStock, ArrayedVariable, MultiArrayedStock, MultiArrayedVariable
IndexedValue popValues = arrayedStock.getIndexedValue();
IndexedValue densityValues = multiArrayedVariable.getIndexedValue();
```

| Class | Purpose |
|---|---|
| `IndexedValue` | Immutable multi-dimensional value with broadcasting arithmetic, aggregation (`sum`, `mean`, `max`, `min`, `sumOver`), and named-dimension access |

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

### Model Definitions (`model/def/` package)

Model definitions provide a pure-data representation of a system dynamics model — no lambdas, no closures, fully serializable. Every element is an immutable Java record with constructor validation. This is the foundation for JSON persistence, external tooling, and the compilation pipeline.

**Build a definition with the fluent builder:**

```java
ModelDefinition def = new ModelDefinitionBuilder()
    .name("SIR Model")
    .stock("Susceptible", 990, "Person")
    .stock("Infected", 10, "Person")
    .stock("Recovered", 0, "Person")
    .flow("Infection", "Susceptible * Infected * contact_rate", "Day", "Susceptible", "Infected")
    .flow("Recovery", "Infected * recovery_rate", "Day", "Infected", "Recovered")
    .constant("contact_rate", 0.005, "1/Day")
    .constant("recovery_rate", 0.1, "1/Day")
    .lookupTable("effect_of_crowding", new double[]{0, 0.5, 1.0}, new double[]{1.0, 0.5, 0.0}, "LINEAR")
    .defaultSimulation("Day", 100, "Day")
    .build();
```

**Validate before compiling:**

```java
List<String> errors = DefinitionValidator.validate(def);
// Checks: no duplicate names, flow source/sink references exist, equations parse, port bindings match
```

**Nested modules** are supported via `ModuleInstanceDef`, which embeds a child `ModelDefinition` with input/output port bindings:

```java
ModelDefinition workforce = new ModelDefinitionBuilder()
    .name("Workforce")
    .moduleInterface(inputs, outputs)   // declare ports
    .stock("Staff", 10, "Person")
    .flow("Hiring", "vacancy * hire_rate", "Month", null, "Staff")
    .build();

ModelDefinition parent = new ModelDefinitionBuilder()
    .name("Project")
    .moduleInstance("workforce", workforce, inputBindings, outputBindings)
    .build();
```

| Record | Purpose |
|---|---|
| `ModelDefinition` | Top-level container: stocks, flows, auxiliaries, constants, lookup tables, modules, subscripts, views, simulation settings |
| `StockDef` | Accumulator with initial value, unit, and optional negative-value policy |
| `FlowDef` | Rate with equation string, time unit, and optional source/sink stock names |
| `AuxDef` | Auxiliary variable with equation string and unit |
| `ConstantDef` | Fixed exogenous value with unit |
| `LookupTableDef` | Table function with x/y arrays and interpolation mode (`LINEAR` or `SPLINE`) |
| `SubscriptDef` | Dimension label set |
| `ModuleInstanceDef` | Nested module with input/output port bindings |
| `ModuleInterface` / `PortDef` | Public interface and port declarations for reusable modules |
| `SimulationSettings` | Default time step, duration, and duration unit |
| `ViewDef` / `ElementPlacement` / `ConnectorRoute` / `FlowRoute` | Graphical view layout data |
| `ModelDefinitionBuilder` | Fluent builder for constructing `ModelDefinition` instances |
| `DefinitionValidator` | Structural validation returning a list of error messages |

### Expression AST (`model/expr/` package)

The expression system provides a sealed `Expr` AST for representing formulas as data rather than lambdas. Expressions can be parsed from strings, traversed for dependency extraction, stringified back to text, and compiled to executable lambdas.

**Expr types** (sealed hierarchy):

| Type | Example | Description |
|---|---|---|
| `Literal` | `3.14` | Numeric constant |
| `Ref` | `Population`, `` `Birth Rate` `` | Reference to a model element (backtick-quoted names supported) |
| `BinaryOp` | `A + B`, `X * Y` | Binary operation with operator precedence |
| `UnaryOp` | `-X`, `!flag` | Negation or logical NOT |
| `FunctionCall` | `SMOOTH(input, 5)` | Built-in function call |
| `Conditional` | `IF(X > 0, X, 0)` | Conditional expression |

**Parse, extract dependencies, and stringify:**

```java
// Parse an expression string into an AST
Expr expr = ExprParser.parse("Population * birth_rate + Immigration");

// Extract all referenced element names
Set<String> deps = ExprDependencies.extract(expr);  // {Population, birth_rate, Immigration}

// Convert back to a string
String text = ExprStringifier.stringify(expr);       // "Population * birth_rate + Immigration"
```

The parser supports standard arithmetic (`+`, `-`, `*`, `/`, `^`, `%`), comparisons (`==`, `!=`, `<`, `<=`, `>`, `>=`), logical operators (`&&`, `||`, `!`), and the reserved names `TIME` and `DT`.

| Class | Purpose |
|---|---|
| `Expr` | Sealed interface with six record variants |
| `ExprParser` | Recursive-descent parser with operator-precedence climbing |
| `ExprStringifier` | AST to human-readable infix string with minimal parentheses |
| `ExprDependencies` | Extracts the set of referenced element names from an AST |
| `BinaryOperator` / `UnaryOperator` | Operator enums with symbol and precedence |

### Model Compiler (`model/compile/` package)

The compiler bridges the gap between pure-data definitions and executable simulations. It translates a `ModelDefinition` (expression strings, no behavior) into a runnable `Model` with compiled lambda formulas.

**Compile and run:**

```java
ModelCompiler compiler = new ModelCompiler();
CompiledModel compiled = compiler.compile(def);

// Create simulation using the definition's default settings
Simulation sim = compiled.createSimulation();
sim.execute();

// Or specify settings explicitly
Simulation sim2 = compiled.createSimulation(TimeUnits.DAY, 200, TimeUnits.DAY);
```

**Two-pass compilation strategy:**
1. **Pass 1:** Create all model elements (stocks, flows, variables, constants, lookup tables) with placeholder `DoubleSupplier` holders
2. **Pass 2:** Compile all formula expressions and fill in the holders

This indirection allows forward references — a flow's formula can reference a variable defined later in the model.

**Nested module compilation:** The compiler handles `ModuleInstanceDef` entries recursively, creating child `CompilationContext` scopes with parent lookups for port bindings. Qualified names (e.g., `Workforce.Staff`) resolve through the module hierarchy.

**Built-in functions** available in expressions: `SMOOTH`, `DELAY3`, `STEP`, `RAMP`, `LOOKUP`, `MIN`, `MAX`, `ABS`, `SQRT`, `EXP`, `LN`, `LOG10`, `SIN`, `COS`, `IF`, `TIME`, `DT`.

| Class | Purpose |
|---|---|
| `ModelCompiler` | Entry point: compiles `ModelDefinition` → `CompiledModel` |
| `ExprCompiler` | Compiles `Expr` AST nodes into executable `DoubleSupplier` lambdas |
| `CompilationContext` | Name resolution and scoping with parent-chain lookup for nested modules |
| `CompiledModel` | Result container: runnable `Model`, resettable state, convenience `createSimulation()` |
| `QualifiedName` | Dot-separated name parsing for hierarchical module references (e.g., `Workforce.Staff`) |
| `Resettable` | Interface for stateful formula objects (Smooth, Delay3) that need resetting between runs |

### JSON Serialization (`io/json/` package)

`ModelDefinitionSerializer` provides round-trip JSON persistence for `ModelDefinition` using Jackson. Models can be saved to files, loaded back, and compiled — enabling external tools, model sharing, and version control of model structure.

**Serialize and deserialize:**

```java
ModelDefinitionSerializer serializer = new ModelDefinitionSerializer();

// To/from JSON string
String json = serializer.toJson(def);
ModelDefinition loaded = serializer.fromJson(json);

// To/from file
serializer.toFile(def, Paths.get("sir_model.json"));
ModelDefinition fromFile = serializer.fromFile(Paths.get("sir_model.json"));
```

**JSON structure:**

```json
{
  "name": "SIR Model",
  "stocks": [
    { "name": "Susceptible", "initialValue": 990.0, "unit": "Person" }
  ],
  "flows": [
    { "name": "Infection", "equation": "Susceptible * Infected * contact_rate",
      "timeUnit": "Day", "source": "Susceptible", "sink": "Infected" }
  ],
  "constants": [
    { "name": "contact_rate", "value": 0.005, "unit": "1/Day" }
  ],
  "defaultSimulation": { "timeStep": "Day", "duration": 100.0, "durationUnit": "Day" }
}
```

Nested modules are serialized recursively (depth-limited to 50). All definition information is preserved losslessly.

**Full workflow — define, serialize, reload, compile, run:**

```java
// Build and save
ModelDefinition def = new ModelDefinitionBuilder()
    .name("SIR").stock("S", 990, "Person").stock("I", 10, "Person").stock("R", 0, "Person")
    .flow("Infection", "S * I * 0.005", "Day", "S", "I")
    .flow("Recovery", "I * 0.1", "Day", "I", "R")
    .defaultSimulation("Day", 100, "Day")
    .build();
new ModelDefinitionSerializer().toFile(def, Paths.get("sir.json"));

// Load and run
ModelDefinition loaded = new ModelDefinitionSerializer().fromFile(Paths.get("sir.json"));
CompiledModel compiled = new ModelCompiler().compile(loaded);
compiled.createSimulation().execute();
```

### Vensim .mdl Import (`io/vensim/` package)

`VensimImporter` reads Vensim `.mdl` model files and produces a `ModelDefinition` that can be compiled and simulated. This enables importing models from the most widely used System Dynamics tool.

**Import a .mdl file:**

```java
VensimImporter importer = new VensimImporter();
ImportResult result = importer.importModel(Path.of("model.mdl"));
if (!result.isClean()) {
    result.warnings().forEach(System.out::println);
}
ModelDefinition def = result.definition();
```

**Full workflow — import, compile, simulate:**

```java
ImportResult result = new VensimImporter().importModel(Path.of("sir.mdl"));
CompiledModel compiled = new ModelCompiler().compile(result.definition());
Simulation sim = compiled.createSimulation();
sim.execute();
```

**Supported constructs:**

| Vensim Construct | Forrester Element |
|---|---|
| `INTEG(rate, init)` | `StockDef` + `FlowDef` |
| Numeric literal | `ConstantDef` |
| Unchangeable (`==`) | `ConstantDef` |
| Expression (`=`) | `AuxDef` |
| Standalone lookup table | `LookupTableDef` |
| `WITH LOOKUP(input, data)` | `AuxDef` + extracted `LookupTableDef` |
| Subscript range (`:`) | `SubscriptDef` |
| `IF THEN ELSE` | `IF` |
| `XIDZ` / `ZIDZ` | `IF` with division guards |
| `SMOOTH3` / `DELAY1` | `SMOOTH` / `DELAY3` (with warning) |
| `:AND:` / `:OR:` / `:NOT:` | `&&` / `\|\|` / `!()` |
| Sketch section | `ViewDef` with element placements and connectors |
| Simulation settings | `SimulationSettings` (INITIAL TIME, FINAL TIME, TIME STEP) |

Unsupported constructs (macros, data variables, PULSE, DELAY FIXED, etc.) emit warnings rather than failing.

| Class | Purpose |
|---|---|
| `VensimImporter` | Main entry point implementing `ModelImporter` |
| `MdlParser` | Low-level .mdl file parser (equations + sketch extraction) |
| `VensimExprTranslator` | Expression syntax translation (Vensim → Forrester) |
| `SketchParser` | Sketch section → `ViewDef` records |
| `MdlEquation` | Parsed equation data record |

### XMILE Import & Export (`io/xmile/` package)

`XmileImporter` reads XMILE XML files (the OASIS standard format used by Stella/iThink) and produces a `ModelDefinition`. `XmileExporter` writes any `ModelDefinition` to valid XMILE 1.0 XML. Together they enable bidirectional model exchange with the second-most-popular SD tool ecosystem.

**Import an XMILE file:**

```java
XmileImporter importer = new XmileImporter();
ImportResult result = importer.importModel(Path.of("model.xmile"));
if (!result.isClean()) {
    result.warnings().forEach(System.out::println);
}
ModelDefinition def = result.definition();
```

**Export to XMILE:**

```java
String xml = XmileExporter.toXmile(modelDefinition);
XmileExporter.toFile(modelDefinition, Path.of("model.xmile"));
```

**Full workflow — import, compile, simulate:**

```java
ImportResult result = new XmileImporter().importModel(Path.of("sir.xmile"));
CompiledModel compiled = new ModelCompiler().compile(result.definition());
Simulation sim = compiled.createSimulation();
sim.execute();
```

**Supported constructs:**

| XMILE Construct | Forrester Element |
|---|---|
| `<stock>` with `<eqn>` | `StockDef` (eqn = initial value) |
| `<flow>` with `<eqn>` | `FlowDef` (source/sink from stock `<inflow>`/`<outflow>`) |
| `<aux>` with numeric `<eqn>` | `ConstantDef` |
| `<aux>` with expression `<eqn>` | `AuxDef` |
| `<aux>` or `<flow>` with `<gf>` | `LookupTableDef` + `AuxDef` |
| `<sim_specs>` | `SimulationSettings` (start, stop, dt, time_units) |
| `<views>` / `<view>` | `ViewDef` with element placements and connectors |
| `IF_THEN_ELSE` | `IF` |
| `AND` / `OR` / `NOT` | `&&` / `\|\|` / `!` |
| `=` / `<>` (comparison) | `==` / `!=` |
| `SMTH3` / `SMTH1` | `SMOOTH` (with approximation warning) |
| `Time` | `TIME` |
| `<non_negative>` | `NegativeValuePolicy.CLAMP_TO_ZERO` |

**Import limitations:** Non-literal stock initial values default to 0 (with warning). Start time offset and dt value are not preserved in `SimulationSettings`. Unsupported elements (`<group>`, `<module>`, `<macro>`, arrays, vendor-specific extensions) emit warnings rather than failing.

**Export limitations:** Constants are exported as `<aux>` with numeric equations (re-imported correctly). Start time is always 0. Subscripts and module instances are not exported.

| Class | Purpose |
|---|---|
| `XmileImporter` | Main entry point implementing `ModelImporter` |
| `XmileExporter` | Static methods: `toXmile(def)`, `toFile(def, path)` |
| `XmileExprTranslator` | Bidirectional expression syntax translation |
| `XmileViewParser` | View XML → `ViewDef` records |
| `XmileViewWriter` | `ViewDef` records → view XML |
| `XmileConstants` | Namespace URIs and element/attribute name constants |

### Dependency Graph & Auto-Layout (`model/graph/` package)

The graph package extracts structural information from model definitions for visualization and analysis.

**Dependency graph** — builds a directed graph from all equations and flow connections:

```java
DependencyGraph graph = DependencyGraph.fromDefinition(def);

Set<String> influencers = graph.dependenciesOf("Infection");  // elements that feed into Infection
Set<String> affected = graph.influencesOf("contact_rate");    // elements influenced by contact_rate
String[][] edges = graph.allEdges();                          // all [from, to] pairs
```

**Connector generation** — auto-generates influence arrows from the dependency graph:

```java
List<ConnectorRoute> connectors = ConnectorGenerator.generate(def);
```

**Auto-layout** — places all elements in a simple layered arrangement (stocks center, auxiliaries above, constants below):

```java
ViewDef view = AutoLayout.layout(def);
```

**View validation** — checks that element placements, connectors, and flow routes reference existing model elements:

```java
List<String> errors = ViewValidator.validate(view, def);
```

| Class | Purpose |
|---|---|
| `DependencyGraph` | Directed graph extracted from equations: `dependenciesOf()`, `influencesOf()`, `allEdges()` |
| `ConnectorGenerator` | Auto-generates `ConnectorRoute` influence arrows from the dependency graph |
| `AutoLayout` | Generates a `ViewDef` with layered element placement |
| `ViewValidator` | Validates view integrity against the model definition |

### Visual Editor (`app/` package — `forrester-app` module)

The `forrester-app` module provides a JavaFX canvas-based visual editor for creating and editing stock-and-flow diagrams interactively. Users can build, edit, save, and simulate models entirely through the GUI.

**Core interactions:**

- **Element creation** — toolbar or keyboard shortcuts (1–6) to place stocks, flows, auxiliaries, constants, and modules on the canvas
- **Flow connections** — two-click protocol: click source (stock or cloud), rubber-band follows cursor with stock hover highlight, click sink to create flow at midpoint
- **Inline editing** — double-click any element to rename; constants chain name→value editing; flows and auxiliaries chain name→equation editing. Rename propagates to flow references and equation tokens
- **Flow reattachment** — drag cloud endpoints onto stocks to reconnect, or drag connected endpoints off stocks to disconnect to cloud
- **Resize** — drag corner handles on selected elements to resize stocks, auxiliaries, constants, and modules
- **Hover highlighting** — subtle outline on mouse-over gives immediate visual feedback about which element will be acted on
- **Selection** — click to select, Shift+click to toggle, rubber-band marquee to select multiple elements, Ctrl+A to select all
- **Pan & zoom** — Space+drag or middle/right-drag to pan; scroll wheel to zoom at cursor; Ctrl+Plus/Minus/0 for keyboard zoom
- **Undo/redo** — Ctrl+Z / Ctrl+Shift+Z with a 100-level snapshot stack capturing full model + view state
- **File persistence** — New, Open, Save, Save As (JSON format with full view layout preservation)
- **Properties panel** — right-side panel showing editable fields for the selected element (name, value, equation, unit, negative-value policy) with a context toolbar for rename, delete, drill-into, and bindings actions
- **Simulation** — Ctrl+R compiles the model definition, runs on a background thread, and displays results in a sortable table window

**Visual language:** The editor renders the Layered Flow Diagram notation with distinct shapes for each element type (rounded-rectangle stocks, diamond flow indicators, rounded-rectangle auxiliaries, dashed-border constants, thick-bordered module containers with "mod" badge), material flow arrows routed through diamond indicators, dashed info link connectors, and cloud symbols for disconnected flow endpoints.

| Class | Purpose |
|---|---|
| `ForresterApp` | JavaFX entry point, menus, undo/simulation wiring |
| `ModelCanvas` | Event handling and editing orchestration |
| `ModelEditor` | Mutable model editing layer with name index |
| `CanvasRenderer` | Rendering coordinator (connections, elements, overlays) |
| `CanvasState` | Mutable positions, types, draw order, selection |
| `FlowCreationController` | Two-click flow creation state machine |
| `FlowEndpointCalculator` | Cloud positions and endpoint hit testing |
| `PropertiesPanel` | Right-side panel for viewing/editing element properties |
| `SimulationRunner` | Compile + run + capture simulation results |
| `UndoManager` | Snapshot-based undo/redo stack |

### Modules

`Module` supports hierarchical model composition — sub-modules, constants, and all arrayed element types. Modules are used both in the lambda-based API (runtime composition) and as compilation targets for `ModuleInstanceDef`.

```java
Module workforce = new Module("Workforce");
workforce.addStock(staff);
workforce.addFlow(hiring);
workforce.addConstant(hireRate);

Module project = new Module("Project");
project.addSubModule(workforce);

// Access nested elements
Module wf = project.getSubModule("Workforce");
Map<String, Constant> constants = wf.getConstants();
```

The `ModelReport` class generates hierarchical structural reports that recurse into sub-modules, listing all stocks, flows, variables, and constants at each level.

### Unit Registry (`measure/` package)

`UnitRegistry` maps unit name strings (as used in `ModelDefinition` records) to `Unit` objects for the compiler. It pre-loads all 40+ built-in units and auto-creates custom `ItemUnit` instances for unknown names.

```java
UnitRegistry registry = new UnitRegistry();

Unit day = registry.resolve("Day");             // built-in TimeUnit
Unit people = registry.resolve("Person");       // built-in ItemUnit
Unit custom = registry.resolve("Widget");       // auto-creates ItemUnit("Widget")

TimeUnit month = registry.resolveTimeUnit("Month");  // returns TimeUnit or throws
```

Resolution is case-sensitive with a case-insensitive fallback. Auto-created custom units are capped at 10,000 to prevent unbounded growth.

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

### Multi-Parameter Sweep (`sweep/` package)

The `MultiParameterSweep` runner extends single-parameter sweeps to combinatorial grids. It computes the Cartesian product of N parameter arrays and runs every combination, enabling interaction analysis across multiple parameters simultaneously.

The factory signature matches `MonteCarlo`'s (`Function<Map<String, Double>, Model>`), so model factories can be shared between sweep and Monte Carlo analysis.

```java
MultiSweepResult result = MultiParameterSweep.builder()
    .parameter("Contact Rate", ParameterSweep.linspace(2.0, 14.0, 4.0))
    .parameter("Infectivity", new double[]{0.05, 0.10, 0.15})
    .modelFactory(params -> buildModel(params.get("Contact Rate"), params.get("Infectivity")))
    .timeStep(DAY)
    .duration(Times.weeks(8))
    .build()
    .execute();

result.writeTimeSeriesCsv("multisweep-timeseries.csv");
result.writeSummaryCsv("multisweep-summary.csv");
```

| Class | Purpose |
|---|---|
| `MultiParameterSweep` | Builder API + execute loop; computes Cartesian product of parameter arrays |
| `MultiSweepResult` | Aggregates run results with ordered parameter names; delegates to `SweepCsvWriter` for CSV export |

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

### Optimization / Calibration (`sweep/` package)

The `Optimizer` finds parameter values that minimize an objective function evaluated on simulation output. It wraps Apache Commons Math derivative-free optimizers behind a builder API consistent with the sweep and Monte Carlo runners. The optimizer runs the simulation repeatedly, tracking the best result across all evaluations.

**Algorithms:**

| Algorithm | Description |
|---|---|
| `NELDER_MEAD` (default) | Simplex-based; good general-purpose choice for low-dimensional problems |
| `BOBYQA` | Quadratic interpolation within bounds; requires at least 2 parameters |
| `CMAES` | Population-based evolution strategy; well-suited for higher-dimensional problems |

**Built-in objective functions (`Objectives` class):**

| Method | Behavior |
|---|---|
| `fitToTimeSeries(stockName, observed)` | Sum of squared errors against observed data |
| `minimize(stockName)` | Minimize final stock value |
| `maximize(stockName)` | Maximize final stock value (negated internally) |
| `target(stockName, targetValue)` | Minimize squared deviation from target |
| `minimizePeak(stockName)` | Minimize peak stock value across all steps |

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
    .duration(Times.weeks(8))
    .build()
    .execute();

Map<String, Double> best = result.getBestParameters();
double sse = result.getBestObjectiveValue();
RunResult bestRun = result.getBestRunResult();
```

| Class | Purpose |
|---|---|
| `Optimizer` | Builder API + execute loop; dispatches to Commons Math optimizers with best-tracking adapter |
| `OptimizationResult` | Immutable result: best parameters, objective value, run result, evaluation count |
| `OptimizationParameter` | Record: name, bounds, optional initial guess |
| `OptimizationAlgorithm` | Enum: `NELDER_MEAD`, `BOBYQA`, `CMAES` |
| `ObjectiveFunction` | Functional interface: `double evaluate(RunResult)` |
| `Objectives` | Static factory methods for common objective functions |

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

**SirMultiSweepDemo** — Demonstrates multi-parameter sweep on the SIR model. Sweeps contact rate (2, 6, 10, 14) across infectivity (0.05, 0.10, 0.15) for 12 combinations, writing time-series and summary CSVs to the system temp directory. The summary CSV shows how higher contact rate combined with higher infectivity produces dramatically larger epidemic peaks.

**SirMonteCarloDemo** — Demonstrates Monte Carlo simulation on the SIR model with two uncertain parameters: contact rate (Normal distribution, mean=8, sd=2) and infectivity (Uniform distribution, 0.05–0.15). Runs 200 iterations with Latin Hypercube Sampling, writes percentile CSV output, and displays a fan chart showing the uncertainty envelope around the Infectious stock trajectory.

**SirCalibrationDemo** — Demonstrates model calibration via a twin experiment. Generates synthetic observed data by running the SIR model with known parameters (contactRate=8.0, infectivity=0.10), then uses the `Optimizer` with Nelder-Mead and a sum-of-squared-errors objective to recover those parameters from the synthetic data. Reports recovered vs true values and fit error.

**MultiRegionSirDemo** — Extends the SIR model to three regions (North, South, East) using subscripts. Each region has its own S/I/R arrayed stocks with independent infection and recovery dynamics. Small daily migration flows (1% of infectious) move infected individuals between regions, seeding outbreaks that started in the North. Demonstrates `Subscript`, `ArrayedStock`, `ArrayedFlow`, and cross-element scalar flows.

**PopulationRegionAgeDemo** — A population model with Region (North, South, East) × AgeGroup (Young, Adult, Elder) = 9 stocks using multi-dimensional subscripts. Demonstrates `SubscriptRange` and `MultiArrayedStock` with per-element initial values, aging flows (Young→Adult→Elder) within each region, birth flows proportional to adult population, death flows for elders, and circular migration between regions per age group.

**PredatorPreyDemo** — Implements the Lotka-Volterra predator-prey model. Prey (Rabbits) and predator (Foxes) populations are coupled through birth and death flows that depend on both species' levels. The model produces sustained oscillations where predator peaks lag prey peaks.

**InventoryModelDemo** — Models a car dealership's inventory with perception (5-day), response (3-day), and delivery (5-day) delays, inspired by *Thinking in Systems*. A step increase in demand causes the dealer to overshoot and oscillate before settling, demonstrating how delays in feedback loops amplify fluctuations.

**SalesMixDemo** — Models the evolving revenue mix between one-time hardware sales and recurring service revenue. A Customers stock grows linearly; hardware revenue scales with new acquisitions while service revenue scales with total customers. Over time the hardware proportion falls, showing how stock-dependent flows shift composition.

### Software Development

**AgileSoftwareDevelopmentDemo** — Models an agile project with product/release/sprint backlogs, completed work, latent defects, and known defects. Work flows from sprint backlog to completion at a bounded productivity rate, generating defects at a fraction of the completion rate. Defects are discovered and fixed through separate feedback loops, illustrating rework dynamics.

**WaterfallSoftwareDevelopmentDemo** — Models a waterfall project using four composable Modules: Workforce, Development, Staff Allocation, and Test & Rework. Hiring delays, training overhead, defect injection, and rework cycles interact to show how phased development with late testing can lead to schedule overruns and staffing oscillations.

## Usage Patterns

### Lambda-based API (programmatic)

Build models directly in Java using stocks, flows, and lambda formulas:

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

### Definition-based API (data-driven)

Define models as pure data, serialize to JSON, and compile to runnable simulations:

```java
// 1. Build a model definition (pure data — no lambdas)
ModelDefinition def = new ModelDefinitionBuilder()
    .name("Population Model")
    .stock("Population", 1000, "Person")
    .flow("Births", "Population * birth_rate", "Year", null, "Population")
    .flow("Deaths", "Population * death_rate", "Year", "Population", null)
    .constant("birth_rate", 0.03, "1/Year")
    .constant("death_rate", 0.01, "1/Year")
    .defaultSimulation("Day", 365, "Day")
    .build();

// 2. Validate
List<String> errors = DefinitionValidator.validate(def);

// 3. Save to JSON (and load later)
ModelDefinitionSerializer serializer = new ModelDefinitionSerializer();
serializer.toFile(def, Paths.get("population.json"));

// 4. Compile to a runnable model
CompiledModel compiled = new ModelCompiler().compile(def);

// 5. Run
Simulation sim = compiled.createSimulation();
sim.execute();
```

Both APIs produce the same simulation output. The lambda-based API is best for models built and run within a single Java program. The definition-based API is best when models need to be saved, shared, loaded from external sources, or composed from reusable module definitions.

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

- Adding a JavaFX canvas-based visual editor (`forrester-app` module) — interactive stock-and-flow diagram editor with element creation via toolbar or keyboard shortcuts (including module/submodel placement), two-click flow connection protocol with rubber-band preview, inline name/value/equation editing with rename propagation, flow endpoint reattachment, element resize via corner handles, hover highlighting for discoverability, a properties panel with context toolbar for editing element attributes, rubber-band marquee selection, pan/zoom navigation, 100-level snapshot-based undo/redo, JSON file persistence with view layout, integrated simulation with background execution and sortable results table, context-sensitive cursor feedback, and a status bar showing tool/selection/element counts/zoom. Models can be built, edited, saved, and simulated entirely through the GUI
- Adding XMILE import and export (`io/xmile/` package) — bidirectional model exchange with Stella/iThink and other XMILE-compatible tools via the OASIS standard XML format. `XmileImporter` reads XMILE files to `ModelDefinition`; `XmileExporter` writes any `ModelDefinition` to valid XMILE 1.0 XML. Supports stocks, flows, auxiliaries, constants, lookup tables (standalone and embedded `<gf>`), simulation settings, view data, and bidirectional expression translation. Audited and hardened with 65 tests including round-trip compile+simulate and export→re-import verification
- Adding Vensim `.mdl` import (`io/vensim/` package) — reads Vensim model files and produces `ModelDefinition` records that can be compiled and simulated. Supports stocks, constants, auxiliaries, lookup tables, subscript ranges, simulation settings, sketch data, and expression translation for common Vensim functions. Audited and hardened with 75 tests covering CRLF handling, case-insensitive matching, operator precedence, and duplicate name detection
- Adding an external model representation with expression AST, definition records, model compiler, JSON serialization, nested modules, and dependency graph — six new packages (`model/expr`, `model/def`, `model/compile`, `io/json`, `model/graph`, `measure/UnitRegistry`) that enable defining models as pure data, persisting them to JSON, compiling them to runnable simulations, and extracting dependency graphs for visualization
- Adding intelligent arrays (`IndexedValue`) — immutable multi-dimensional values with automatic broadcasting arithmetic. Shared dimensions align by name; non-shared dimensions expand via outer product. Supports elementwise and cross-dimension operations, scalar broadcasting, aggregation (`sum`, `mean`, `max`, `min`, `sumOver`), and convenience accessors on arrayed model elements
- Adding multi-dimensional subscripts — `SubscriptRange`, `MultiArrayedStock`, `MultiArrayedFlow`, and `MultiArrayedVariable` enable cross-tabulated dimensions (e.g., Region × AgeGroup) with coordinate access, aggregation (`sumOver`, `slice`), and transparent expansion
- Adding arrays/subscripts support — `Subscript`, `ArrayedStock`, `ArrayedFlow`, and `ArrayedVariable` enable dimensioned model elements (regions, cohorts, products) that transparently expand into the flat simulation loop
- Adding `Optimizer` for model calibration — wraps Apache Commons Math derivative-free optimizers (Nelder-Mead, BOBYQA, CMA-ES) behind a builder API with built-in objective functions (SSE fit-to-data, minimize, maximize, target, minimize-peak) for automated parameter fitting against observed data
- Adding `MultiParameterSweep` runner for combinatorial grid analysis — computes the Cartesian product of N parameter arrays, runs every combination, and exports multi-column CSV output for interaction analysis
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
