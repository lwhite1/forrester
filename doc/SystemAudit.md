# System-Wide Code Audit — Remaining Findings

Audit of the Forrester simulation library after six rounds of fixes (commits `bca62d9`, `68a2457`, `169f0f3`, `988f495`, the initial additions, and the current round targeting 8 medium-severity items) plus the external model representation (commit `d314ffe`). All critical, high, and medium-severity bugs have been resolved. This document lists only the **remaining open items**.

For the full fix history and quality assessment, see `doc/CodeQualityAssessment.md`.

---

## 1. Simulation Engine

### DESIGN — Off-by-one: simulation runs N+1 steps (Documented)
**File:** `Simulation.java`

`while (currentStep <= totalSteps)` runs steps 0 through N inclusive = N+1 iterations. Documented in class Javadoc. Not changed because all tests and demos depend on the current behavior.

### DESIGN — `getEventBus()` exposes raw Guava EventBus (Low)
Allows external code to post arbitrary events.

### DESIGN — Asymmetric event objects (Low)
`SimulationStartEvent` carries model + simulation; `SimulationEndEvent` carries model but not simulation.

### MINOR
- Misleading local variable `q` for cached flow quantities
- `addStep` name implies adding to a collection
- No null check on `addEventHandler`/`removeEventHandler`
- `TimeStepEvent` Javadoc `@param` references old field name

---

## 2. Core Model (Stock, Flow, Variable, Constant, Model, Module)

### DESIGN — No equals/hashCode on Element/Stock/Flow/Variable (Low)
All rely on identity equality.

### DESIGN — `addStock`/`addConstant` allow duplicate names without warning (Low)

### BUG — Flow history records phantom amounts on clamped stocks (Low)
When `NegativeValuePolicy` clamps to zero, the flow's recorded value reflects the unclamped amount.

### MINOR
- No null check on `Flow` timeUnit constructor parameter
- `Constant` accepts NaN as initial value
- `Element.setComment(null)` undocumented
- `ArrayedFlow.create` overload accepts unused stock parameter

---

## 3. SD Functions (Smooth, Delay3, Step, Ramp, LookupTable)

### DESIGN — Smooth and Delay3 internal stages are opaque (Low)
No accessors for debugging or conservation-of-material verification.

### DESIGN — LookupTable.Builder doesn't detect duplicate x values (Low)
Silently passes invalid data to the interpolator.

### BUG — Smooth/Delay3 multi-step catch-up uses stale input (Inherent limitation)
The fix loops N times when N steps are skipped, but each iteration reads the *current* input value (intermediate values are unknowable).

---

## 4. Sweep / Monte Carlo / Optimizer

### BUG — RunResult.getMaxStockValue returns NEGATIVE_INFINITY on empty (Low)
Missing stock name returns `NEGATIVE_INFINITY` instead of throwing.

### BUG — MonteCarloResult stock/variable name collision (Low)
Same-named stock and variable: `getPercentileSeries` silently picks the first match.

### DESIGN — `MonteCarlo.reseedRandomGenerator` mutates caller's objects (Low)
Side effect not documented.

### DESIGN — No defensive copy of arrays/lists (Low)
`ParameterSweep.Builder`, `SweepResult`, `MonteCarloResult` don't defensively copy.

### DESIGN — RuntimeException wrapping loses type information (Low)

### MINOR
- `linspace` name implies `numpy.linspace` (endpoint-inclusive) but uses start-step-end semantics
- `formatPercentile` relies on specific double-to-string formatting
- `File.mkdirs()` return value ignored

---

## 5. IO & UI

### DESIGN — `FanChart` and `ChartViewerApplication` use all-static state (Medium)
Only one chart per JVM lifetime.

### DESIGN — `ChartViewerApplication.launch()` can only be called once (Medium)
JavaFX limitation. Second simulation with chart viewer fails.

### DESIGN — `CsvSubscriber` opens file in constructor (Low)
Prevents deferred initialization.

### DESIGN — `CsvSubscriber.close()` doesn't guard against post-close events (Low)
Sets writer to null but doesn't prevent subsequent event handler calls.

### DESIGN — `ModelReport` output format ends without trailing newline (Low)

### BUG — CsvSubscriber NPE if `getCurrentTime()` null (Low)
Unlikely — Simulation always provides non-null timestamp.

### BUG — ChartViewerApplication series/values size mismatch (Low)
Only triggers with asymmetric stock/flow chart setup.

### MINOR
- Unused imports in some demo files
- Commented-out code in `ChartViewerApplication`
- Ramp uses `Integer.MAX_VALUE` sentinel for unbounded end
- Redundant `ModelReport` console output
- Missing `@Override` on some event handlers

---

## 6. Expression AST, Definition Records, Compiler, Serialization, and Graph

### BUG — Circular module detection keyed on name, not identity (Low)
**File:** `DefinitionValidator.java`

Two structurally different module definitions with the same name are flagged as circular when they are not. In practice, circular reference through records is not constructible, so this is unlikely to cause real problems.

### DESIGN — Chained comparisons silently produce wrong semantics (Low)
**File:** `ExprParser.java`

`a < b < c` parses as `(a < b) < c`, where `(a < b)` yields 0.0 or 1.0, then is compared with `c`. No warning is produced. Standard C-like behavior but almost certainly a user error in an SD modeling context.

### DESIGN — `topologicalSort()` silently drops cycle-involved nodes (Low)
**File:** `DependencyGraph.java`

Documented in Javadoc. The `hasCycle()` method works by comparing sort result size to node count. But callers using `topologicalSort()` directly for evaluation order will silently skip elements in cycles.

### DESIGN — UnitRegistry not thread-safe (Low)
**File:** `UnitRegistry.java`

The internal maps are plain `LinkedHashMap` with no synchronization. Concurrent `resolve()` calls that trigger auto-creation can corrupt internal state. Only relevant if a `UnitRegistry` is shared across threads.

### DESIGN — UnitRegistry auto-creation silently masks typos (Low)
**File:** `UnitRegistry.java`

Any unknown unit name silently creates a new `ItemUnit`. A typo like `"Dallar"` instead of `"Dollar"` produces no error. The `MAX_CUSTOM_UNITS` cap (10,000) is a safety net but does not help with correctness.

### DESIGN — AND/OR compile both branches unconditionally (Low)
**File:** `ExprCompiler.java`

Stateful functions (SMOOTH, DELAY3) in unreachable branches are still compiled and registered as `Resettable`, accumulating unnecessary state.

### MINOR
- `evaluateConstant` rejects arithmetic on constants (e.g. `2 + 3` as a SMOOTH argument) — only literal, constant ref, and negation are supported
- LOOKUP table name not included in `ExprDependencies.extract()` output — dependency arrows from lookup tables to consuming formulas missing from auto-generated views
- Serialization has no depth limit for nested modules (only deserialization has `MAX_MODULE_DEPTH = 50`)
- `requiredText` in `ModelDefinitionSerializer` silently coerces non-string JSON types to strings via Jackson's `asText()`
- `ElementPlacement` type validation is case-insensitive but storage preserves original case — downstream `equals("stock")` checks may fail for `"Stock"`
- Round-trip imperfection for negative literals in `ExprStringifier` (documented)

---

## 7. Remaining Test Gaps

| Area | Gap | Risk |
|------|-----|------|
| UI classes (FanChart, ChartViewerApplication, viewers) | No tests | Low — visual output |
| Simulation events | No test for event ordering or handler lifecycle | Low |
| Optimizer edge cases | No TooManyEvaluations test | Low |
| MonteCarloResult CSV content | File existence tested, content not verified | Low |
