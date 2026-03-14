# Vensim `.mdl` Import

Courant reads Vensim `.mdl` model files and produces a `ModelDefinition` that can be compiled and simulated.

## Usage

```java
VensimImporter importer = new VensimImporter();
ImportResult result = importer.importModel(Path.of("model.mdl"));
if (!result.isClean()) {
    result.warnings().forEach(System.out::println);
}
ModelDefinition def = result.definition();

// Compile and run
CompiledModel compiled = new ModelCompiler().compile(def);
Simulation sim = compiled.createSimulation();
sim.execute();
```

In the visual editor, use **File > Open** to load `.mdl` files directly.

---

## Supported Constructs

### Core Variable Types

| Vensim Construct | Courant Element | Notes |
|---|---|---|
| `INTEG(rate, init)` | `StockDef` + `FlowDef` | Net flow auto-created as `{stock}_net_flow`; expression-based initial values supported |
| Numeric literal (`=` or `==`) | `AuxDef` (literal) | Scientific notation supported (e.g. `1.5e-3`) |
| Unchangeable (`==`) | `AuxDef` (literal) | |
| Expression (`=`) | `AuxDef` | Any non-INTEG, non-literal equation |
| Standalone lookup table | `LookupTableDef` | Range annotation `[(xmin,ymin)-(xmax,ymax)]` stripped |
| `WITH LOOKUP(input, data)` | `AuxDef` + extracted `LookupTableDef` | |
| Subscript range (`:`) | `SubscriptDef` | Labels normalized (spaces to underscores) |
| Data variables (`:=`) | Skipped with warning | |

### Simulation Settings

All settings from the `.Control` group are extracted (case-insensitive):

- `INITIAL TIME` -- simulation start time
- `FINAL TIME` -- simulation end time (converted to duration)
- `TIME STEP` -- extracted and preserved as fractional dt
- `SAVEPER` -- recognized but not enforced

### Expression Translation

**Name normalization:**
- Multi-word names converted: `Contact Rate` becomes `Contact_Rate`
- Longest-name-first matching prevents partial substitution
- Case-insensitive, word-boundary-aware replacement
- Special characters stripped; names starting with digits get `_` prefix
- Surrounding quotes removed
- Duplicate normalized names detected and warned

**Operator translation:**

| Vensim | Courant |
|---|---|
| `:AND:` | `and` |
| `:OR:` | `or` |
| `:NOT:` | `not(...)` |
| `^` | `**` |
| `Time` | `TIME` |

**Function translation:**

| Vensim | Courant | Notes |
|---|---|---|
| `IF THEN ELSE(c, t, e)` | `IF(c, t, e)` | |
| `XIDZ(a, b, x)` | `IF((b)==0, x, (a)/(b))` | Safe division with fallback |
| `ZIDZ(a, b)` | `IF((b)==0, 0, (a)/(b))` | Safe division, zero fallback |
| `WITH LOOKUP(input, data)` | `LOOKUP(name, input)` | Lookup extracted to separate table |
| `GAME(expr)` | `expr` | Wrapper stripped |
| `DELAY FIXED(input, time, init)` | `DELAY_FIXED(input, time, init)` | Space to underscore |
| `PULSE TRAIN(start, dur, repeat, end)` | `PULSE_TRAIN(start, dur, repeat, end)` | Space to underscore |
| `RANDOM NORMAL(min, max, mean, std, seed)` | `RANDOM_NORMAL(min, max, mean, std, seed)` | Space to underscore |
| `RANDOM UNIFORM(min, max, seed)` | `RANDOM_UNIFORM(min, max, seed)` | Space to underscore |
| `SAMPLE IF TRUE(cond, input, init)` | `SAMPLE_IF_TRUE(cond, input, init)` | Space to underscore |
| `FIND ZERO(expr, var, lo, hi)` | `FIND_ZERO(expr, var, lo, hi)` | Space to underscore |
| `LOOKUP AREA(table, x1, x2)` | `LOOKUP_AREA(table, x1, x2)` | Space to underscore |
| `ACTIVE INITIAL(expr, init)` | `expr` | Pass-through first arg (no game mode) |
| `MESSAGE(args)` | `0` | No-op (UI-only function) |
| `SIMULTANEOUS(args)` | `0` | No-op (solver hint for Euler integration) |

**Natively supported functions (pass-through):**

`SMOOTH`, `SMOOTH3`, `SMOOTH3I`, `SMOOTHI`, `DELAY1`, `DELAY1I`, `DELAY3`, `DELAY3I`, `DELAY_FIXED`, `PULSE`, `PULSE_TRAIN`, `RANDOM_NORMAL`, `RANDOM_UNIFORM`, `MIN`, `MAX`, `ABS`, `EXP`, `LN`, `LOG`, `SQRT`, `SIN`, `COS`, `TAN`, `ARCSIN`, `ARCCOS`, `ARCTAN`, `INT`, `ROUND`, `MODULO`, `POWER`, `QUANTUM`, `SIGN`, `PI`, `VMIN`, `VMAX`, `PROD`, `RAMP`, `STEP`, `TREND`, `FORECAST`, `NPV`, `INITIAL`, `LOOKUP`, `LOOKUP_AREA`, `SAMPLE_IF_TRUE`, `FIND_ZERO`, `NOT`, `OR`, `AND`, `TRUE`, `FALSE`

### Sketch/View Parsing

- View declarations (`*View Name`)
- Element placements (type 10 lines) with x/y coordinates
- Flow valve placement (type 11 lines)
- Dependency connectors (type 1 lines)
- Clouds (type 12 lines) recognized but skipped (source/sink boundaries)

### CLD Mode Detection

If a model has no stocks and no flow valves in its sketch, Courant treats it as a Causal Loop Diagram. Sketch connectors become `CausalLinkDef` entries with unknown polarity.

### File Format Support

- CRLF and LF line endings
- UTF-8 BOM stripping; `{UTF-8}` header stripping
- Backslash continuation lines (e.g. `expr +\` / `  more`)
- Pipe `|` block delimiters
- Tilde-separated sections: `name ~ units ~ comment`
- Comment preservation on each variable
- Group sections (lines of `****` with `.Group_Name` headers)
- Quoted variable names (e.g. `"name with (parens)"`)

---

## Unsupported Features

### Functions That Trigger Warnings

The following functions are recognized but not supported. They remain in the equation text unchanged and a warning is issued:

| Function | Notes |
|---|---|
| `DELAY N` | Nth-order material delay. DELAY1 and DELAY3 are supported. |
| `GET XLS DATA` | Reads time-series from Excel. External data dependency. |
| `GET DIRECT DATA` | Reads data from external file. |
| `GET DIRECT CONSTANTS` | Reads constants from external file. |
| `TABBED ARRAY` | Inline array definition. Would need full array support. |
| `VECTOR SELECT` | Vector operations. Would need array support. |
| `VECTOR ELM MAP` | Vector element mapping. |
| `VECTOR SORT ORDER` | Vector sorting. |
| `ALLOCATE AVAILABLE` | Resource allocation across subscripts. |

### Structural Features

| Feature | Status |
|---|---|
| Macros (`:MACRO:` to `:END OF MACRO:`) | Inline expansion (single-output only) |
| Module/component structures | Not supported |
| Reality checks | Not recognized |
| `:SUPPLEMENTARY:` variables | Keyword not recognized |
| Subscripted variable access (e.g. `Stock[Region]`) | Subscript ranges imported but subscripted variable access untested |

---

## Known Limitations

### PULSE Semantics

Vensim's `PULSE(start, width)` returns `1/TIME_STEP` for a duration. Courant's `PULSE(magnitude, start)` returns a magnitude for one timestep. Models using Vensim PULSE may need manual adjustment after import.

### Name Normalization

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
- Lookup table range annotations are reconstructed on export but not preserved from import
- Function names protected from denormalization on export (e.g. `INTEG` not converted to `I N T E G`)

---

## Error Handling

All import errors are non-fatal. The importer collects warnings in `ImportResult.warnings()` and continues processing. The model may be partial if elements are skipped.

**Warning categories:**
- Data variables skipped
- Malformed INTEG or WITH LOOKUP expressions
- Non-numeric simulation settings
- Duplicate normalized names
- Unsupported functions detected in expressions
- Lookup parsing failures
- Individual element processing errors

**Fatal errors:** None. The parser is permissive -- empty content returns an empty model, unparseable blocks are skipped.

---

## Import Compatibility

Tested against 59 Vensim sample models (`D:\Vensim\Models\Sample`):
- **Import (parse .mdl)**: 59/59 (100%)
- **Compile**: 45/59 (76%)
- **Simulate**: 45/59 (76%)

The 14 compile failures are caused by subscript/array models that require array expansion the importer doesn't yet support, not by missing functions.

Also tested against 25 models from the TU Delft repository (Pruyt, 2013):
- **Compilation**: 25/25 (100%)
- **Simulation**: 24/25 (96%) — ProjectManagement fails due to algebraic loops

---

## Key Classes

| Class | Purpose |
|---|---|
| `VensimImporter` | Main entry point implementing `ModelImporter` |
| `MdlParser` | Low-level `.mdl` file parser (equations, macros + sketch extraction) |
| `MacroDef` | Parsed macro definition record (name, params, body) |
| `MacroExpander` | Inline expansion of macro calls into ordinary equations |
| `VensimExprTranslator` | Expression syntax translation (Vensim to Courant) |
| `SketchParser` | Sketch section to `ViewDef` records |

---

## Summary

| Category | Support Level |
|---|---|
| Stocks (INTEG) | Full (including expression-based initial values) |
| Constants | Full |
| Auxiliaries | Full |
| Lookup tables | Full |
| Subscript range definitions | Full |
| Simulation settings | Full |
| Multi-word name normalization | Full |
| Logical operators | Full |
| IF THEN ELSE / XIDZ / ZIDZ | Full |
| SMOOTH / SMOOTH3 / SMOOTH3I / SMOOTHI | Full (native) |
| DELAY1 / DELAY1I / DELAY3 / DELAY3I / DELAY_FIXED | Full (native) |
| PULSE / PULSE_TRAIN | Full (native) |
| RANDOM_NORMAL / RANDOM_UNIFORM | Full (native) |
| SAMPLE_IF_TRUE / FIND_ZERO | Full (native) |
| LOOKUP_AREA | Full (trapezoidal integration) |
| ACTIVE INITIAL / GAME / MESSAGE / SIMULTANEOUS | Full (translated or no-op) |
| NOT / OR / AND / TRUE / FALSE (function forms) | Full |
| TREND / FORECAST / NPV | Full |
| Sketch/views | Full (4 line types) |
| Macros (single-output) | Full (inline expansion) |
| Data variables | Skipped (with warning) |
| DELAY N, GET XLS DATA, etc. | Warned, left in equation |
| Subscripted variable access | Untested |
| Module/component hierarchy | Not supported |
