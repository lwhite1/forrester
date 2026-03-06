# Vensim (.mdl) Import Support

This document describes what Forrester supports when importing Vensim `.mdl` files, what is unsupported, and what known limitations exist.

## Supported Features

### Core Variable Types

| Type | How Detected | Notes |
|------|-------------|-------|
| Stocks | `INTEG(rate_expr, initial_value)` | Net flow auto-created as `{stock}_net_flow` |
| Constants | Numeric literal with `=` or `==` operator | Scientific notation supported (e.g. `1.5e-3`) |
| Auxiliaries | Expression-based equation with `=` operator | Any non-INTEG, non-literal equation |
| Lookup Tables | `()` operator or `WITH LOOKUP` | Range annotation `[(xmin,ymin)-(xmax,ymax)]` stripped |
| Subscript Ranges | `:` operator (e.g. `Region : North, South`) | Labels normalized (spaces to underscores) |

### Simulation Settings

All settings from the `.Control` group are extracted (case-insensitive):

- `INITIAL TIME` -- simulation start time
- `FINAL TIME` -- simulation end time (converted to duration)
- `TIME STEP` -- extracted but preserved as metadata only (Forrester uses fixed step dt=1)
- `SAVEPER` -- recognized but not enforced

### Expression Translation

**Name normalization:**
- Multi-word names converted: `Contact Rate` becomes `Contact_Rate`
- Longest-name-first matching prevents partial substitution
- Case-insensitive, word-boundary-aware replacement

**Operator translation:**

| Vensim | Forrester |
|--------|-----------|
| `:AND:` | `&&` |
| `:OR:` | `\|\|` |
| `:NOT:` | `!` (parenthesized) |

**Function translation:**

| Vensim | Forrester | Notes |
|--------|-----------|-------|
| `IF THEN ELSE(c, t, e)` | `IF(c, t, e)` | |
| `XIDZ(a, b, x)` | `IF((b)==0, x, (a)/(b))` | Safe division with fallback |
| `ZIDZ(a, b)` | `IF((b)==0, 0, (a)/(b))` | Safe division, zero fallback |
| `WITH LOOKUP(input, data)` | `LOOKUP(name, input)` | Lookup extracted to separate table |
| `Time` | `TIME` | |

**Pass-through functions** (no translation needed):
`INTEG`, `SMOOTH`, `DELAY3`, `MIN`, `MAX`, `ABS`, `EXP`, `LN`, `LOG`, `SQRT`, `SIN`, `COS`, `TAN`, `INT`, `ROUND`, `MODULO`, `POWER`, `QUANTUM`, `RAMP`, `STEP`, `PULSE`

### Approximated Functions

These functions are translated with a warning because the semantics differ:

| Vensim | Translated To | Difference |
|--------|--------------|------------|
| `SMOOTH3(input, time)` | `SMOOTH(input, time)` | Third-order smoothing reduced to first-order |
| `SMOOTHI(input, time, init)` | `SMOOTH(input, time)` | Initial value argument dropped |
| `SMOOTH3I(input, time, init)` | `SMOOTH(input, time)` | Third-order + initial value both lost |
| `DELAY1(input, time)` | `DELAY3(input, time)` | First-order delay changed to third-order |
| `DELAY1I(input, time, init)` | `DELAY3(input, time)` | First-order + initial value both lost |

### File Format Support

- CRLF and LF line endings
- UTF-8 BOM stripping
- `{UTF-8}` header stripping
- Backslash continuation lines (e.g. `expr +\` / `  more`)
- Pipe `|` block delimiters
- Tilde-separated sections: `name ~ units ~ comment`
- Comment preservation on each variable
- Group sections (lines of `****` with `.Group_Name` headers)

### Sketch/View Parsing

- View declarations (`*View Name`)
- Element placements (type 10 lines) with x/y coordinates
- Flow valve placement (type 11 lines)
- Dependency connectors (type 1 lines)
- Clouds (type 12 lines) recognized but skipped (source/sink boundaries)

---

## Unsupported Features

### Variable Types Not Imported

| Type | Behavior |
|------|----------|
| Data variables (`:=` operator) | Skipped with warning |
| Game variables | Silently skipped |
| Reality checks | Not recognized |
| `:SUPPLEMENTARY:` variables | Keyword not recognized |

### Functions That Trigger Warnings

The following functions are recognized in equations but not supported. They remain in the equation text unchanged and a warning is issued:

`PULSE TRAIN`, `GAME`, `DELAY FIXED`, `DELAY N`, `FORECAST`, `TREND`, `NPV`, `GET XLS DATA`, `GET DIRECT DATA`, `GET DIRECT CONSTANTS`, `TABBED ARRAY`, `SAMPLE IF TRUE`, `VECTOR SELECT`, `VECTOR ELM MAP`, `VECTOR SORT ORDER`, `ALLOCATE AVAILABLE`, `FIND ZERO`

Note: Vensim's `PULSE(start, width)` has different semantics from Forrester's `PULSE(magnitude, start)`. Vensim PULSE returns `1/TIME_STEP` for a duration; Forrester's returns a magnitude for one timestep. Models using Vensim PULSE will need manual adjustment after import.

### Structural Features

| Feature | Status |
|---------|--------|
| Macros (`:MACRO:` to `:END OF MACRO:`) | Content skipped entirely |
| Module/component structures | Not supported |
| A-to-B notation | No special handling beyond sketch connectors |
| Subscripted variables (e.g. `Stock[Region]`) | Subscript ranges imported but subscripted variable access untested |

---

## Known Limitations

### Initial Values

- Only numeric literals accepted for `INTEG` initial values
- Expression-based initial values (e.g. `INTEG(rate, stock * 2)`) default to `0.0` with a warning
- Missing initial values default to `0.0` with a warning

### Time Step

- `TIME STEP` is extracted but **not used in simulation**
- Forrester simulates with fixed Euler integration at dt=1 regardless of the `.mdl` setting
- Models designed for different dt values may produce different results

### Name Normalization

- Special characters stripped (not preserved)
- Names starting with digits get `_` prefix
- Surrounding quotes removed
- No validation against reserved words
- Duplicate normalized names detected and warned

### Sketch Parsing

- Only 4 line types parsed (1, 10, 11, 12); annotations, text boxes, and other sketch objects are silently ignored
- If a connector references an unknown element ID, the connector is skipped
- Flow valve names must match a known flow name

### Simulation Settings Edge Cases

- Non-numeric values for `INITIAL TIME`, `FINAL TIME`, `TIME STEP` use defaults with warning
- `FINAL TIME <= INITIAL TIME` defaults duration to 100 with warning

### Round-Trip Fidelity

- Synthetic net flows (`{stock}_net_flow`) are inlined back into `INTEG` on export
- Lookup table range annotations `[(xmin,ymin)-(xmax,ymax)]` are reconstructed on export but not preserved from import
- Function names protected from denormalization on export (e.g. `INTEG` not converted to `I N T E G`)

---

## Error Handling

All import errors are non-fatal. The importer collects warnings in `ImportResult.warnings()` and continues processing. The model may be partial if elements are skipped.

**Warning categories:**
- Data variables skipped
- Non-literal initial values defaulting to 0
- Malformed INTEG or WITH LOOKUP expressions
- Non-numeric simulation settings
- Duplicate normalized names
- Unsupported functions detected in expressions
- Lookup parsing failures
- Individual element processing errors

**Fatal errors:** None. The parser is permissive -- empty content returns an empty model, unparseable blocks are skipped.

---

## Test Coverage

### Example Models
- **teacup.mdl** -- Single stock, constant room temperature, auxiliary heat loss rate
- **sir.mdl** -- Three stocks (S/I/R), two auxiliaries, three constants, full sketch view

### Tested Scenarios
- Element classification (INTEG, literal, expression, lookup, subscript, data variable)
- Multi-word name normalization in expressions
- Lookup table import and export
- Simulation setting extraction
- Case-insensitive system variables
- Numeric literal detection (integers, decimals, negative, scientific notation)
- Duplicate name detection
- Continuation line handling
- UTF-8 and BOM header stripping
- Group section extraction
- Sketch line parsing and view creation
- Round-trip: import, export, reimport for SIR model
- Full pipeline: import, compile, simulate for Teacup model

---

## Summary

| Category | Support Level |
|----------|--------------|
| Stocks (INTEG) | Full |
| Constants | Full |
| Auxiliaries | Full |
| Lookup tables | Full |
| Subscript range definitions | Full |
| Simulation settings | Full (dt metadata only) |
| Multi-word name normalization | Full |
| Logical operators | Full |
| IF THEN ELSE / XIDZ / ZIDZ | Full |
| SMOOTH, DELAY3 | Full |
| SMOOTH3, SMOOTHI, DELAY1 | Approximated (with warnings) |
| Sketch/views | Full (4 line types) |
| Macros | Skipped |
| Data variables | Skipped (with warning) |
| Game variables | Skipped |
| PULSE TRAIN, FORECAST, TREND, etc. | Warned, left in equation |
| Subscripted variable access | Untested |
| Module/component hierarchy | Not supported |
