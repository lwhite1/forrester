# XMILE (.xmile / .stmx / .itmx) Import & Export

`XmileImporter` reads XMILE XML files (IEEE 1855-2016 / OASIS XMILE 1.0) and produces a `ModelDefinition` that can be compiled and simulated. `XmileExporter` writes any `ModelDefinition` to valid XMILE 1.0 XML. Together they enable bidirectional model exchange with the Stella/iThink ecosystem. The `.stmx` (Stella) and `.itmx` (iThink) extensions use the same XMILE format.

## Usage

### Import

```java
XmileImporter importer = new XmileImporter();
ImportResult result = importer.importModel(Path.of("model.xmile"));
if (!result.isClean()) {
    result.warnings().forEach(System.out::println);
}
ModelDefinition def = result.definition();

// Compile and run
CompiledModel compiled = new ModelCompiler().compile(def);
Simulation sim = compiled.createSimulation();
sim.execute();
```

### Export

```java
String xml = XmileExporter.toXmile(modelDefinition);
XmileExporter.toFile(modelDefinition, Path.of("model.xmile"));
```

In the visual editor, use File > Open to load `.xmile` files and File > Export XMILE to export.

## Supported Features (Import)

### Core Model Elements

| Element | XML Tag | Notes |
|---------|---------|-------|
| Stocks | `<stock>` | Initial values, inflow/outflow declarations, units, documentation |
| Flows | `<flow>` | Equations, units, source/sink linkage via stock declarations |
| Auxiliaries | `<aux>` | Numeric and expression-based equations, units |
| Constants | `<aux>` with numeric equation | Imported as Courant constants |
| Lookup Tables | `<gf>` (graphical function) | Standalone or embedded in flow/aux definitions |
| Modules | `<module>` | Instance name, `<connect>` input/output bindings |

**Stock behavior:**
- `<non_negative>` tag recognized and mapped to `CLAMP_TO_ZERO` policy

**Lookup table data formats:**
- Explicit `<xpts>` / `<ypts>` data
- Implicit x-values generated from `xscale` min/max attributes
- LINEAR interpolation (hardcoded)

### Modules and Multi-Model Files

XMILE files can contain multiple `<model>` elements. Named models define reusable module types; the unnamed (or first) model is the main model.

**How modules are imported:**
- Each named `<model name="X">` is parsed into a standalone `ModelDefinition`
- `<module name="X">` elements in the main model reference the named model
- `<connect to="inner_var" from="outer_var"/>` creates an input binding
- `<connect to=".outer_alias" from="inner_var"/>` (dot prefix on `to`) creates an output binding
- Module instances compile into isolated execution contexts with parent-scope fallback for input bindings

**Limitations:**
- The module instance name must match a named `<model>` element exactly
- Modules without a matching named model are skipped with a warning
- Nested modules (modules within modules) are supported recursively
- Module views are not imported (only the main model's views are used)

### Simulation Settings (`<sim_specs>`)

| Setting | Attribute/Element | Notes |
|---------|------------------|-------|
| Time units | `time_units` attribute | Capitalized (e.g. "day" becomes "Day") |
| Start time | `<start>` | Always reset to 0 internally |
| Stop time | `<stop>` | Converted to duration (`stop - start`) |
| dt | `<dt>` | Applied as simulation time step (defaults to 1 if absent) |

### Model Metadata

- Model name from `<header><name>`
- Namespace support: both OASIS XMILE 1.0 namespace (`http://docs.oasis-open.org/xmile/ns/XMILE/v1.0`) and namespace-free variants

### Views and Graphical Layout

- **Element placements** -- `<stock>`, `<flow>`, `<aux>` with x/y coordinates
- **Connectors** -- `<connector>` dependency arrows with optional control points via `<pts><pt>`
- **Flow routes** -- `<flow>` with `<pts>` control points for polyline visualization
- **Multiple views** -- `<view name="...">` all imported; unnamed views get default names ("View 1", "View 2", etc.)
- **Element type resolution** -- auxiliaries classified as constant, aux, or lookup based on name lookups

### Expression Translation

**Operator conversions:**

| XMILE | Courant |
|-------|-----------|
| `AND` | `and` |
| `OR` | `or` |
| `NOT` | `not` |
| `<>` | `!=` |
| `=` (comparison) | `==` |
| `^` | `**` |
| `Time` | `TIME` |

**Function conversions:**

| XMILE | Courant | Notes |
|-------|-----------|-------|
| `IF_THEN_ELSE(c, t, e)` | `IF(c, t, e)` | Also handles space-separated form |
| `IF THEN ELSE(c, t, e)` | `IF(c, t, e)` | |
| `SMTH3(input, delay)` | `SMOOTH(input, delay)` | Approximation with warning |
| `SMTH1(input, delay)` | `SMOOTH(input, delay)` | Approximation with warning |

**Pass-through functions** (no translation needed):
`EXP`, `LN`, `ABS`, `MIN`, `MAX`, `SQRT`, `SIN`, `COS`, `TAN`, `LOG`, `ROUND`, `INT`, `MOD`, `LOOKUP`, `DELAY3`, `SMOOTH`

Translation is bidirectional -- `XmileExprTranslator` also converts Courant expressions back to XMILE for export.

---

## Unsupported Features

### Elements Skipped with Warnings

| Element | XML Tag | Warning Message |
|---------|---------|----------------|
| Groups | `<group>` | "Unsupported XMILE element `<group>` 'name' skipped" |
| Macros | `<macro>` | "Unsupported XMILE element `<macro>` 'name' skipped" |
| Event posters | `<event_poster>` | "Unsupported XMILE element `<event_poster>` 'name' skipped" |

### Stock Types (Warned)

Special stock types are imported as standard stocks with a warning:

| Type | Warning Message |
|------|----------------|
| Conveyor stocks (`conveyor="true"`) | "Stock 'X' is a conveyor stock (not supported, treated as standard stock)" |
| Queue stocks (`queue="true"`) | "Stock 'X' is a queue stock (not supported, treated as standard stock)" |
| Oven stocks (`oven="true"`) | "Stock 'X' is an oven stock (not supported, treated as standard stock)" |

### Flow Types (Warned)

| Type | Warning Message |
|------|----------------|
| Biflows (no `<non_negative>`) | "Flow 'X' is a biflow (may allow negative values; Courant treats all flows as unidirectional)" |
| Leak flows | Not recognized as special |
| Material flow direction | Not preserved |

### Lookup Table Interpolation Modes (Warned)

- Only **LINEAR** (continuous) interpolation is used
- Non-continuous interpolation modes (`extrapolate`, `discrete`) trigger a warning:
  > "Graphical function 'X' uses interpolation type 'extrapolate' (only LINEAR/continuous is supported)"

### Variable Attributes (Warned)

| Attribute | Warning Message |
|-----------|----------------|
| `<range>` specifications | "Range specification on stock/flow/auxiliary 'X' ignored" |
| Scale/unit conversions | Not applied (no warning) |
| Display attributes (color, font, size, line style) | Discarded (no warning) |
| `<doc>` documentation elements | Parsed but not comprehensively preserved |

### Functions (Warned)

The following XMILE functions are left in the equation text unchanged with a warning:

| Function | Warning Message |
|----------|----------------|
| `SAFEDIV(a, b, x)` | "SAFEDIV function not supported (left in equation as-is)" |
| `INIT(variable)` | "INIT function not supported (left in equation as-is)" |
| `PREVIOUS(stock, time)` | "PREVIOUS function not supported (left in equation as-is)" |
| `HISTORY(variable, time)` | "HISTORY function not supported (left in equation as-is)" |

### Arrayed Variables / Subscripts

- No support for `<subscript>`, `<subscripts>`, or `<dimensions>` tags
- Arrayed stocks, flows, and auxiliaries cannot be imported
- Subscript indexing in equations (e.g. `Population[North]`) is not parsed
- The engine has runtime array infrastructure (`ArrayedStock`, `MultiArrayedStock`, `IndexedValue`) but the definition→compilation bridge for subscripts does not exist yet

### Built-in Functions Not Tested

The following XMILE functions may or may not work -- they have no explicit handling or tests:

`SAMPLE`, `STDDEV`, `VARIANCE`, `RANDOM`, `LOOKUP2D`, `UNRESTRICTED`, `ALLOCATE`

The following pass through and are supported by the Courant expression compiler:
`PULSE`, `RAMP`, `STEP`, `MEAN`, `RANDOM_NORMAL`, `SIN`, `COS`, `TAN`, `LOG`, `INT`, `ROUND`, `MODULO`, `POWER`, `DELAY_FIXED`, `TREND`, `FORECAST`, `NPV`

### Other Missing Features

- Connector line styles, colors, and curved vs. straight line rendering
- Flow initial values (flows always start at their computed equation value)
- User-defined functions
- 2D lookup tables
- iSee Systems vendor-specific extensions

---

## Known Limitations

### Stock Initial Values

- Only numeric literals accepted
- Expression-based initial values (e.g. `SUM(a, b)`) default to `0.0` with warning
- This can cause loss of initialization logic for complex models

### Missing Equations

- Flows with no `<eqn>` element default to equation `"0"` with warning
- Auxiliaries with no `<eqn>` are treated as constants with value 0 with warning

### Embedded Graphical Functions

When an auxiliary has a `<gf>` element but no `<eqn>`:
- `TIME` is used as the default input variable
- This may not match the model's intended behavior if the lookup was meant to receive a different input

When an auxiliary has both `<gf>` and `<eqn>`:
- The equation becomes the lookup input: `LOOKUP(lookup_name, eqn_text)`

### Stock-Flow Linkage

- Flows resolved to source/sink by **first match** in the inflow/outflow declarations
- If multiple stocks declare the same flow, only the first match is used
- No error for ambiguous declarations

### View Coordinate Parsing

- Empty or blank x/y coordinates silently skipped
- Non-numeric coordinates silently dropped
- Elements with missing coordinates omitted from view

### Duration Calculation

- `stop <= start` defaults duration to 100 with warning
- Start time always reset to 0 internally in Courant

---

## Error Handling

All import errors are non-fatal. The importer returns `ImportResult` containing the parsed model and a list of warnings.

**Warning categories:**

| Category | Example |
|----------|---------|
| Unsupported elements | "Unsupported XMILE element `<group>` 'name' skipped" |
| Unknown module references | "Module 'X' references unknown model definition, skipped" |
| Unsupported stock types | "Stock 'X' is a conveyor stock (not supported, treated as standard stock)" |
| Biflow detection | "Flow 'X' is a biflow (may allow negative values; ...)" |
| Non-literal initial values | "Non-literal initial value for stock 'X': 'expr', defaulting to 0.0" |
| Missing equations | "Flow 'X' has no equation, defaulting to 0" |
| Invalid graphical functions | "Could not parse graphical function data for 'X'" |
| Non-linear interpolation | "Graphical function 'X' uses interpolation type 'Y' (only LINEAR/continuous is supported)" |
| Range specifications | "Range specification on stock 'X' ignored" |
| Unsupported functions | "SAFEDIV function not supported (left in equation as-is)" |
| Invalid time range | "stop (0) <= start (10), defaulting duration to 100" |
| Function approximations | "SMTH3 approximated as SMOOTH" |
| Element processing errors | "Error processing stock 'X': message" |

**Fatal errors:**
- XML parsing failure throws `IllegalArgumentException`: "Failed to parse XMILE XML: error"
- All other errors are caught per-element and added as warnings

---

## Export Limitations

- Constants are exported as `<aux>` with numeric equations (re-imported correctly)
- Start time is always 0
- dt is always written as the model's configured time step
- Subscripts and module instances are not exported
- Display attributes (color, font, size) are not exported

## Round-Trip Fidelity

### Preserved Through Import/Export Cycle

- Stock names, initial values, units, non_negative behavior
- Flow names, equations, units, source/sink linkage
- Constant names, values, units
- Lookup table names and x/y point data
- Model name, simulation duration, time unit
- View element placements (x/y coordinates)
- Connector definitions
- Flow graphical routes
- Module instances with input/output bindings
- Named model definitions (for module types)

### Lost or Modified

- dt value (written as model's time step, which may differ from original if changed)
- Conveyor/queue/oven stock attributes
- Subscripted variable dimensions
- Display attributes (color, font, size)
- SMTH3/SMTH1 functions (permanently converted to SMOOTH)
- Non-LINEAR interpolation modes on lookup tables
- Group, macro, and event poster definitions

---

## Test Coverage

### Example Models
- **teacup.xmile** -- Newton's cooling law: 1 stock, 1 flow, 2 constants, basic view
- **sir.xmile** -- SIR epidemiological model: 3 stocks, 2 flows, 3 constants, 1 auxiliary, views with connectors
- **modular_sir.xmile** -- Modular SIR: main model with Susceptible stock, Disease module with Infected/Recovered stocks, input/output bindings

### Tested Scenarios
- Stock import with inflows/outflows and non_negative
- Flow equation parsing and source/sink resolution
- Auxiliary and constant classification
- Lookup table parsing (explicit xpts/ypts and xscale-generated)
- Simulation settings extraction
- Expression translation (both directions)
- View parsing with element placement, connectors, and flow routes
- Round-trip: import, export, reimport
- Full pipeline: import, compile, simulate
- Conveyor/queue/oven stock type warnings
- Biflow vs. uniflow detection warnings
- Range specification warnings on stocks, flows, and auxiliaries
- Non-linear interpolation mode warnings on graphical functions
- Unsupported function warnings (SAFEDIV, INIT, PREVIOUS, HISTORY)
- Module import with input bindings
- Module import with output bindings
- Unknown module reference warning
- Modular SIR model (multi-model file with module instance)
- Module compile-and-simulate (import → compile → run)
- Module export → import round-trip

---

## Summary

| Category | Support Level |
|----------|--------------|
| Stocks | Full (non_negative supported) |
| Flows | Full (unidirectional only) |
| Auxiliaries | Full |
| Constants | Full (as numeric aux) |
| Lookup tables | Full (LINEAR interpolation only) |
| Graphical functions | Full (standalone and embedded) |
| Views and connectors | Full |
| Simulation specs | Full |
| Expression translation | Full (bidirectional) |
| SMTH3 / SMTH1 | Approximated as SMOOTH (with warnings) |
| Modules / submodels | Full (input/output bindings, nested modules) |
| Multiple models | Full (named models as module types) |
| Groups | Skipped with warning |
| Macros | Skipped with warning |
| Event posters | Skipped with warning |
| Subscripts / arrays | Not supported |
| Conveyor / queue / oven stocks | Warned, treated as standard stock |
| Biflow / leak flows | Warned, treated as unidirectional |
| Non-LINEAR interpolation | Warned, uses LINEAR |
| Range specifications | Warned, ignored |
| SAFEDIV / INIT / PREVIOUS / HISTORY | Warned, left in equation |
| Display attributes | Not supported |
