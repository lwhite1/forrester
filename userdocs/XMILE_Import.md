# XMILE (.xmile / .stmx / .itmx) Import Support

This document describes what Forrester supports when importing XMILE files (IEEE 1855-2016 / OASIS XMILE 1.0), what is unsupported, and what known limitations exist. The `.stmx` (Stella) and `.itmx` (iThink) extensions use the same XMILE format.

## Supported Features

### Core Model Elements

| Element | XML Tag | Notes |
|---------|---------|-------|
| Stocks | `<stock>` | Initial values, inflow/outflow declarations, units, documentation |
| Flows | `<flow>` | Equations, units, source/sink linkage via stock declarations |
| Auxiliaries | `<aux>` | Numeric and expression-based equations, units |
| Constants | `<aux>` with numeric equation | Imported as Forrester constants |
| Lookup Tables | `<gf>` (graphical function) | Standalone or embedded in flow/aux definitions |

**Stock behavior:**
- `<non_negative>` tag recognized and mapped to `CLAMP_TO_ZERO` policy

**Lookup table data formats:**
- Explicit `<xpts>` / `<ypts>` data
- Implicit x-values generated from `xscale` min/max attributes
- LINEAR interpolation (hardcoded)

### Simulation Settings (`<sim_specs>`)

| Setting | Attribute/Element | Notes |
|---------|------------------|-------|
| Time units | `time_units` attribute | Capitalized (e.g. "day" becomes "Day") |
| Start time | `<start>` | Always reset to 0 internally |
| Stop time | `<stop>` | Converted to duration (`stop - start`) |
| dt | `<dt>` | Extracted but **not used** -- Forrester uses fixed step dt=1 |

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

| XMILE | Forrester |
|-------|-----------|
| `AND` | `&&` |
| `OR` | `\|\|` |
| `NOT` | `!` |
| `<>` | `!=` |
| `=` (comparison) | `==` |
| `Time` | `TIME` |

**Function conversions:**

| XMILE | Forrester | Notes |
|-------|-----------|-------|
| `IF_THEN_ELSE(c, t, e)` | `IF(c, t, e)` | Also handles space-separated form |
| `IF THEN ELSE(c, t, e)` | `IF(c, t, e)` | |
| `SMTH3(input, delay)` | `SMOOTH(input, delay)` | Approximation with warning |
| `SMTH1(input, delay)` | `SMOOTH(input, delay)` | Approximation with warning |

**Pass-through functions** (no translation needed):
`EXP`, `LN`, `ABS`, `MIN`, `MAX`, `SQRT`, `SIN`, `COS`, `TAN`, `LOG`, `ROUND`, `INT`, `MOD`, `LOOKUP`, `DELAY3`, `SMOOTH`

Translation is bidirectional -- `XmileExprTranslator` also converts Forrester expressions back to XMILE for export.

---

## Unsupported Features

### Elements Skipped with Warnings

| Element | XML Tag | Warning Message |
|---------|---------|----------------|
| Modules/submodels | `<module>` | "Unsupported XMILE element `<module>` 'name' skipped" |
| Groups | `<group>` | "Unsupported XMILE element `<group>` 'name' skipped" |
| Macros | `<macro>` | "Unsupported XMILE element `<macro>` 'name' skipped" |
| Event posters | `<event_poster>` | "Unsupported XMILE element `<event_poster>` 'name' skipped" |

### Stock Types Not Recognized

| Type | Status |
|------|--------|
| Conveyor stocks (`conveyor` attribute) | Ignored |
| Queue stocks (`queue` attribute) | Ignored |
| Oven stocks (`oven` attribute) | Ignored |
| Only `non_negative` is recognized | |

### Flow Types Not Recognized

| Type | Status |
|------|--------|
| Uniflow vs. biflow distinction | All treated as unidirectional |
| Leak flows | Not recognized as special |
| Material flow direction | Not preserved |

### Lookup Table Interpolation Modes

- Only **LINEAR** interpolation is used (hardcoded)
- CUBIC, STEP, and other XMILE interpolation modes are **silently ignored**
- No warning issued if the source file specifies a non-LINEAR mode

### Arrayed Variables / Subscripts

- No support for `<subscript>` or `<subscripts>` tags
- Arrayed stocks, flows, and auxiliaries cannot be imported
- The engine has `SubscriptDef` infrastructure but the XMILE importer does not use it

### Variable Attributes

| Attribute | Status |
|-----------|--------|
| `<range>` specifications | Ignored |
| Scale/unit conversions | Not applied |
| Display attributes (color, font, size, line style) | Discarded |
| `<doc>` documentation elements | Parsed but not comprehensively preserved |

### Model Hierarchy

- Only the top-level `<model>` is parsed
- Nested models (models within models) are ignored
- No support for module instances or model composition

### Built-in Functions Not Tested

The following XMILE functions may or may not work -- they have no explicit handling or tests:

`SAMPLE`, `STDDEV`, `VARIANCE`, `RANDOM`, `LOOKUP2D`, `UNRESTRICTED`, `ALLOCATE`

The following pass through and are supported by the Forrester expression compiler:
`PULSE`, `RAMP`, `STEP`, `MEAN`, `RANDOM_NORMAL`, `SIN`, `COS`, `TAN`, `LOG`, `INT`, `ROUND`, `MODULO`, `POWER`, `DELAY_FIXED`, `TREND`, `FORECAST`, `NPV`

### Other Missing Features

- Connector line styles, colors, and curved vs. straight line rendering
- Flow initial values (flows always start at their computed equation value)
- User-defined functions
- 2D lookup tables
- iSee Systems vendor-specific extensions

---

## Known Limitations

### Time Step (dt)

The `<dt>` value from the XMILE file is extracted and stored as metadata but **not used in simulation**. Forrester uses fixed Euler integration with dt=1 regardless. A warning is issued if dt is not 1.0:

> "dt = X (Forrester uses fixed step; value preserved as metadata only)"

Models designed for smaller dt values may produce numerically different results.

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
- Start time always reset to 0 internally in Forrester

---

## Error Handling

All import errors are non-fatal. The importer returns `ImportResult` containing the parsed model and a list of warnings.

**Warning categories:**

| Category | Example |
|----------|---------|
| Unsupported elements | "Unsupported XMILE element `<module>` 'name' skipped" |
| Non-literal initial values | "Non-literal initial value for stock 'X': 'expr', defaulting to 0.0" |
| Missing equations | "Flow 'X' has no equation, defaulting to 0" |
| Invalid graphical functions | "Could not parse graphical function data for 'X'" |
| dt not 1.0 | "dt = 0.25 (Forrester uses fixed step; value preserved as metadata only)" |
| Invalid time range | "stop (0) <= start (10), defaulting duration to 100" |
| Function approximations | "SMTH3 approximated as SMOOTH" |
| Element processing errors | "Error processing stock 'X': message" |

**Fatal errors:**
- XML parsing failure throws `IllegalArgumentException`: "Failed to parse XMILE XML: error"
- All other errors are caught per-element and added as warnings

---

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

### Lost or Modified

- dt value (always written as 1.0 on export)
- Conveyor/queue/oven stock attributes
- Subscripted variable dimensions
- Display attributes (color, font, size)
- SMTH3/SMTH1 functions (permanently converted to SMOOTH)
- Non-LINEAR interpolation modes on lookup tables
- Module, group, macro, and event poster definitions
- Nested model hierarchy

---

## Test Coverage

### Example Models
- **teacup.xmile** -- Newton's cooling law: 1 stock, 1 flow, 2 constants, basic view
- **sir.xmile** -- SIR epidemiological model: 3 stocks, 2 flows, 3 constants, 1 auxiliary, views with connectors

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
| Simulation specs | Full (dt metadata only) |
| Expression translation | Full (bidirectional) |
| SMTH3 / SMTH1 | Approximated as SMOOTH (with warnings) |
| Modules / submodels | Skipped with warning |
| Groups | Skipped with warning |
| Macros | Skipped with warning |
| Event posters | Skipped with warning |
| Subscripts / arrays | Not supported |
| Conveyor / queue / oven stocks | Not supported |
| Biflow / leak flows | Not supported |
| Non-LINEAR interpolation | Not supported (silently ignored) |
| Display attributes | Not supported |
| Model hierarchy | Not supported |
