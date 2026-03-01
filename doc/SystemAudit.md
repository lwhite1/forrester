# System-Wide Code Audit — Remaining Findings

Audit of the Forrester simulation library after four rounds of fixes (commits `bca62d9`, `68a2457`, `169f0f3`, and the current round). All critical and high-severity bugs have been resolved. This document lists only the **remaining open items**.

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

### DESIGN — Model.removeStock does not disconnect flows (Medium)
Removed stock's connected flows retain stale references.

### DESIGN — Model.addModule does not propagate flows (Medium)
Module's stocks and variables merge, but flows are not registered at the model level.

### DESIGN — No equals/hashCode on Element/Stock/Flow/Variable (Medium)
All rely on identity equality.

### DESIGN — No model-level accessor for all flows (Low)
Flows are only reachable through stocks.

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

### BUG — Optimizer `bestRun[0]` null in fallback path (Medium)
If all evaluations fail, `bestRun[0]` remains null. Callers get a null RunResult.

### BUG — Objectives.fitToTimeSeries silently truncates length mismatch (Medium)
`Math.min()` silently discards tail data without warning.

### BUG — RunResult.getMaxStockValue returns NEGATIVE_INFINITY on empty (Low)
Missing stock name returns `NEGATIVE_INFINITY` instead of throwing.

### BUG — MonteCarloResult stock/variable name collision (Low)
Same-named stock and variable: `getPercentileSeries` silently picks the first match.

### DESIGN — RunResult has two constructors with lossy semantics (Medium)
Can't populate both `parameterValue` and `parameterMap`.

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

## 6. Remaining Test Gaps

| Area | Gap | Risk |
|------|-----|------|
| UI classes (FanChart, ChartViewerApplication, viewers) | No tests | Low — visual output |
| Simulation events | No test for event ordering or handler lifecycle | Low |
| Optimizer edge cases | No TooManyEvaluations test | Low |
| MonteCarloResult CSV content | File existence tested, content not verified | Low |
