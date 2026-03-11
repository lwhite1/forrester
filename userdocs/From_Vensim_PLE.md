# From Vensim PLE to Shrewd

You've been building System Dynamics models in Vensim PLE. You know stocks, flows, auxiliaries, and feedback loops. This guide shows you how to bring your models into Shrewd and what you gain by switching.

The short version: **File > Open** your `.mdl` file. Done. Your model loads with its diagram layout, equations, and simulation settings intact. But there's more to the story.

---

## Why switch?

Vensim PLE is a learning tool. It's designed to get you started, and it does that well. But PLE locks you out of the features you need once your models grow serious:

| Capability | Vensim PLE | Vensim Pro ($1,200/yr) | Shrewd |
|---|:---:|:---:|:---:|
| Stocks, flows, auxiliaries | Yes | Yes | Yes |
| Lookup tables | Yes | Yes | Yes |
| Subscripts / arrays | **No** | Yes | Yes |
| Parameter sweeps | **No** | Yes | Yes |
| Monte Carlo simulation | **No** | Yes | Yes |
| Optimization / calibration | **No** | Yes | Yes |
| Causal loop diagrams | Separate tool | Separate tool | Integrated |
| Hierarchical modules | **No** | Yes | Yes |
| Programmable API | **No** | **No** | Yes |
| XMILE import/export | **No** | Limited | Yes |
| SVG diagram export | **No** | **No** | Yes |
| Cost | Free | $1,200/year | Free |

If you've ever wanted to sweep a parameter across a range, run a thousand Monte Carlo trials, or fit your model to observed data — and then hit PLE's paywall — Shrewd gives you all of that at no cost.

---

## Importing your model

### In the visual editor

1. Launch Shrewd
2. **File > Open** (or Ctrl+O)
3. Select your `.mdl` file
4. Your model appears on the canvas with its original layout

That's it. The importer reads your stocks, flows, auxiliaries, constants, lookup tables, subscripts, sketch layout, and simulation settings.

If anything couldn't be imported cleanly, you'll see warnings in the activity log at the bottom of the window. Common warnings:

- **Approximated functions** — `SMOOTH3` becomes `SMOOTH`, `DELAY1` becomes `DELAY3`. The behavior is similar but not identical.
- **Skipped data variables** — Variables defined with `:=` (external data) are skipped.
- **Expression-based initial values** — Stock initial values that aren't simple numbers default to 0. You'll need to set them manually.

### From code

```java
VensimImporter importer = new VensimImporter();
ImportResult result = importer.importModel(Path.of("my_model.mdl"));

// Check for warnings
result.warnings().forEach(System.out::println);

// Compile and run
ModelDefinition def = result.definition();
CompiledModel compiled = new ModelCompiler().compile(def);
Simulation sim = compiled.createSimulation();
RunResult run = sim.execute();
```

---

## What translates directly

Most of what you've built in Vensim PLE works without changes.

### Equations

Your equations carry over with automatic name translation. Vensim's multi-word names become underscored identifiers:

| Vensim | Shrewd |
|---|---|
| `Contact Rate` | `Contact_Rate` |
| `Total Population` | `Total_Population` |

You can also use backtick-quoted names for readability: `` `Contact Rate` ``.

### Operators and functions

| Vensim | Shrewd | Notes |
|---|---|---|
| `IF THEN ELSE(c, t, e)` | `IF(c, t, e)` | Same logic, shorter syntax |
| `XIDZ(a, b, x)` | `IF(b == 0, x, a / b)` | Safe division, expanded |
| `ZIDZ(a, b)` | `IF(b == 0, 0, a / b)` | Safe division to zero |
| `WITH LOOKUP(input, data)` | `LOOKUP(table, input)` | Lookup table extracted separately |
| `:AND:` / `:OR:` / `:NOT:` | `&&` / `\|\|` / `!()` | Standard operators |
| `^` (exponentiation) | `**` | Python-style |
| `Time` | `TIME` | Uppercase |

### Functions that work identically

These pass through with no changes: `MIN`, `MAX`, `ABS`, `EXP`, `LN`, `LOG`, `SQRT`, `SIN`, `COS`, `TAN`, `INT`, `ROUND`, `MODULO`, `STEP`, `RAMP`, `PULSE`, `SMOOTH`, `DELAY3`.

### Simulation settings

`INITIAL TIME`, `FINAL TIME`, and `TIME STEP` are extracted from your `.mdl` file and applied automatically.

---

## What needs attention

### SMOOTH3 and DELAY1 approximations

Vensim's `SMOOTH3` (third-order exponential smoothing) is approximated as `SMOOTH` (first-order). Similarly, `DELAY1` becomes `DELAY3`. The qualitative behavior is the same — smoothing and delaying — but the transient response differs. If your model is sensitive to the order of these functions, you may need to adjust.

The `SMOOTHI` and `DELAY1I` variants (with explicit initial values) also lose the initial value argument.

### PULSE semantics

**This is the most important difference.** Vensim's `PULSE(start, width)` fires a pulse of magnitude `1/TIME STEP` starting at `start` for `width` time units. Shrewd's `PULSE(magnitude, start)` fires a single-step pulse of the given magnitude at the start time.

If your model uses `PULSE`, check the equations after import and adjust manually.

### Data variables

Variables defined with `:=` (Vensim's data variable syntax) are external data lookups. Shrewd doesn't support external data binding, so these are skipped with a warning. You'll need to replace them — often a lookup table or constant works.

### Complex initial values

Stock initial values that are expressions rather than numbers (e.g., `INTEG(rate, other_stock * 0.5)`) default to 0. Check your stocks after import.

### Macros

Vensim macros (`:MACRO:` blocks) are skipped entirely. If your model uses macros, you'll need to inline the logic or use Shrewd's module system instead.

---

## Side-by-side workflows

### Running a simulation

| Step | Vensim PLE | Shrewd |
|---|---|---|
| Set time horizon | Model > Settings | Simulate > Simulation Settings |
| Run | Simulation menu or toolbar | Ctrl+R |
| View results | Click variable, see graph | Dashboard: chart + table |
| Export data | Copy from table | Right-click chart > Export CSV |

### Editing equations

| Step | Vensim PLE | Shrewd |
|---|---|---|
| Edit | Click variable, type in panel | Double-click variable on canvas |
| Autocomplete | No | Yes — press Tab to accept suggestions |
| Validate | Check Model | Ctrl+B (build/validate) |

### Building a model from scratch

| Step | Vensim PLE | Shrewd |
|---|---|---|
| Add stock | Click Stock tool, click canvas | Press 2 (or toolbar), click canvas |
| Add flow | Click Rate tool, drag | Press 3, click source, click sink |
| Add variable | Click Variable tool | Press 4, click canvas |
| Add constant | (use variable with number) | Press 5, click canvas |
| Connect | Draw arrow | Equations auto-connect when you reference a variable |

Shrewd distinguishes constants from variables. In Vensim PLE, a constant is just a variable with a number (called an "auxiliary" in Vensim terminology). In Shrewd, constants are a first-class element type — they appear differently on the diagram and can be targeted by parameter sweeps.

---

## What you can do now that you couldn't before

### Parameter sweeps

Sweep any constant across a range and see how the system responds:

1. **Simulate > Parameter Sweep**
2. Pick a constant (e.g., `Contact Rate`)
3. Set start, end, and step values
4. Click OK — a family of curves appears, one per value

In Vensim PLE, you'd change the constant, run, change it again, run again, and try to compare by memory. Here you see all runs simultaneously on one chart with a legend.

### Multi-parameter sweeps

Vary two or more parameters at once:

1. **Simulate > Multi-Parameter Sweep**
2. Add parameters with their ranges
3. Every combination runs as an independent simulation
4. The chart shows all results with parameter values in the legend

### Monte Carlo simulation

Assign probability distributions to your parameters and run thousands of trials:

1. **Simulate > Monte Carlo**
2. Select parameters and set distributions (uniform, normal, triangular, etc.)
3. Set the number of trials
4. Run — see the full envelope of possible behaviors

This is how you answer questions like "What's the probability the project finishes on time?" or "What's the 90th-percentile outbreak size?"

### Optimization and calibration

Fit your model to observed data or find optimal parameter values:

1. **Simulate > Optimization**
2. Choose an objective: minimize, maximize, hit a target, or fit to a time series
3. Select which parameters to vary and set bounds
4. Choose an algorithm (Nelder-Mead, BOBYQA, or CMA-ES)
5. Run — Shrewd finds the best parameter values

This is how you calibrate a model against real data. Vensim requires the Pro license ($1,200/year) for this.

### Causal loop diagrams

Sketch out your system's feedback structure before formalizing:

- Press **8** to place CLD variables
- Press **9** to draw causal links with polarity (+, -, ?)
- Shrewd automatically detects feedback loops and classifies them as reinforcing (R) or balancing (B)
- When you're ready, right-click a CLD variable and classify it as a stock, flow, variable, or constant

In Vensim, CLDs are a separate tool (Vensim CLD). In Shrewd, CLDs and stock-and-flow diagrams share the same canvas.

### Modules

Break large models into reusable sub-models:

- Press **7** to add a module
- Double-click to drill into its inner definition
- Define input and output ports to connect it to the parent model
- Reuse the same module definition in multiple places

### Subscripts

Add dimensions to your elements without duplicating them:

Define a subscript range (e.g., `Region: North, South, East`) and apply it to stocks, flows, and auxiliaries. Shrewd expands them into parallel instances automatically.

In Vensim PLE, subscripts are locked behind the Pro paywall.

---

## Tips for a smooth transition

1. **Import first, validate second.** Open your `.mdl` file, then press Ctrl+B to check for errors. Fix any issues the validator flags.

2. **Check your PULSE equations.** This is the one function with genuinely different semantics. Everything else is either identical or a close approximation.

3. **Review warnings in the activity log.** The importer tells you exactly what it changed or couldn't handle.

4. **Save in Shrewd's native format** (.json) after import. This preserves everything including your layout, and loads faster than re-parsing `.mdl` each time.

5. **Try a parameter sweep immediately.** Pick any constant in your model and sweep it. Seeing a family of curves instead of a single run is the fastest way to appreciate what you've gained.

6. **Use keyboard shortcuts.** Shrewd is keyboard-driven: numbers 1-9 for tools, Ctrl+R to run, Ctrl+B to validate, Ctrl+Z to undo. Press Ctrl+Shift+P to open the command palette.

---

## Function reference

For the complete list of supported functions and operators, see the [Expression Language Reference](Expression_Language.md).

For technical details on the import process, see the [Vensim Import](../docs/Vensim%20Import.md) documentation.
