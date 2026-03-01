# System-Wide Code Audit

Full audit of the Forrester simulation library, organized by subsystem. Each issue includes severity, file/line references, and a suggested fix. Issues are deduplicated across the overlapping reports from 5 parallel audit agents.

---

## 1. Simulation Engine

### BUG — Flow values recorded multiple times per step (Critical)
**File:** `Simulation.java:108-128`

When a flow connects two stocks (outflow of A, inflow of B), `flow.recordValue(q)` is called once per stock encounter — i.e., twice for a transfer flow. This corrupts `Flow.history`, breaking `pipelineDelay` and any history-dependent computation.

**Fix:** Move `flow.recordValue(q)` inside the `else` branch (cache-miss path), so it is only called once when the flow is first evaluated.

### BUG — Sub-second time steps truncated to zero (Critical)
**File:** `Simulation.java:130-134`

`addStep()` casts `timeStep.ratioToBaseUnit()` to `long`. For MILLISECOND (ratio 0.001), this truncates to 0 — `currentDateTime` and `elapsedTime` never advance. Completely breaks the recently-added MILLISECOND time unit.

**Fix:** Use `Duration.ofNanos(Math.round(timeStep.ratioToBaseUnit() * 1_000_000_000L))` or expose the `ChronoUnit` from `TimeUnits`.

### BUG — Off-by-one: simulation runs N+1 steps (High)
**File:** `Simulation.java:82`

`while (currentStep <= totalSteps)` runs steps 0 through N inclusive = N+1 iterations. A "5-step simulation" actually runs 6 steps. Tests are written to match this behavior, but it contradicts the standard SD convention.

**Fix:** Change `<=` to `<`, or document the inclusive-endpoint semantics.

### BUG — Floating-point totalSteps can miss a step (High)
**File:** `Simulation.java:78-80`

`totalSteps` is a `double`. Rounding can produce `9.999...` instead of `10.0`, causing one fewer iteration.

**Fix:** `long totalSteps = Math.round(...)`.

### BUG — Simulation is not re-entrant (High)
**File:** `Simulation.java:34,38,74`

`currentStep`, `currentDateTime`, `elapsedTime` are never reset. Calling `execute()` a second time silently does nothing (loop already past `totalSteps`). Stock/flow/variable state from the first run persists.

**Fix:** Either reset all state at the top of `execute()`, or throw `IllegalStateException` on re-entry.

### BUG — SimulationEndEvent not guaranteed on exception (Medium)
**File:** `Simulation.java:74-93`

If a flow formula or event handler throws, `SimulationEndEvent` is never posted. `CsvSubscriber`'s file handle leaks.

**Fix:** Wrap the simulation loop in try/finally: `finally { eventBus.post(new SimulationEndEvent()); }`.

### BUG — Flow cache keyed by name — collisions possible (Medium)
**File:** `Simulation.java:108-128`

`flowMap` is `HashMap<String, Quantity>` keyed by flow name. Two distinct flows with the same name silently share cached values. No uniqueness constraint exists.

**Fix:** Use `IdentityHashMap<Flow, Quantity>`, or enforce unique flow names in Model.

### DESIGN — TimeStepEvent fires before stock update (Medium)
**File:** `Simulation.java:85-89`

Event handlers see pre-update stock values. The Javadoc says "fired after each time step has been computed", which is false.

**Fix:** Move `eventBus.post()` to after `updateStocks()`, or correct the Javadoc.

### DESIGN — EventHandler interface does not carry @Subscribe (Medium)
**File:** `EventHandler.java:14-28`

Guava EventBus requires `@Subscribe` on methods, but the interface doesn't annotate them. Implementing `EventHandler` without manually adding `@Subscribe` causes silent delivery failure.

**Fix:** Add `@Subscribe` to interface methods, or replace EventBus with direct dispatch.

### DESIGN — SimulationEndEvent carries no context (Low)
**File:** `SimulationEndEvent.java`

Unlike `SimulationStartEvent`, the end event has no fields — handlers can't identify which simulation ended.

**Fix:** Add a `Simulation` or `Model` field.

### DESIGN — No validation that duration is a TIME quantity (Low)
**File:** `Simulation.java:44-58`

Passing `new Quantity(5, GALLON_US)` as duration produces nonsensical results.

**Fix:** Check `duration.getUnit().getDimension() == Dimension.TIME`.

### DESIGN — `getTimeStep()` returns `Unit` instead of `TimeUnit` (Low)
**File:** `Simulation.java:144`

The field is `TimeUnit` but the getter widens to `Unit`.

### MINOR — No null checks on constructor parameters (Medium)
**File:** `Simulation.java:44-58`

### MINOR — No validation for zero/negative duration (Medium)
**File:** `Simulation.java:52-58`

---

## 2. Core Model (Stock, Flow, Variable, Constant, Model, Module)

### BUG — ArrayedStock scalar addInflow/addOutflow overwrites sink/source N times (High)
**File:** `ArrayedStock.java:191-207`

Wiring a single scalar Flow to all N underlying stocks calls `flow.setSink(this)` N times. Only the last stock "wins" — the flow's sink is inconsistent with the first N-1 stocks' inflow sets. The flow's value is also applied N times during simulation.

**Fix:** Either remove these methods (require separate flows), or skip `setSink`/`setSource` calls (cloud-flow semantics only).

### BUG — Stock allows conflicting source/sink assignments without error (Medium)
**File:** `Stock.java:54-67`

`addInflow` calls `flow.setSink(this)` without checking if the flow already has a different sink. This silently breaks the previous stock's relationship.

**Fix:** Check for existing source/sink and throw on conflict.

### DESIGN — Model stores stocks in List, variables in Map (Medium)
**File:** `Model.java:15-16`

`stocks` is `ArrayList<Stock>` (allows duplicates, O(n) lookup); `variables` is `HashMap<String, Variable>` (deduplicates, O(1) lookup). `addStock()` does no dedup while `addArrayedStock()` does — inconsistent.

**Fix:** Use `LinkedHashMap<String, Stock>` for stocks. Add `getStock(String name)`.

### DESIGN — Model.removeStock does not disconnect flows (Medium)
**File:** `Model.java:39-41`

Removed stock's connected flows retain stale references.

### DESIGN — Model.addModule does not propagate flows or constants (Medium)
**File:** `Model.java:101-111`

Module's stocks and variables are merged, but flows are not.

### DESIGN — No equals/hashCode on Element/Stock/Flow/Variable (Medium)
**File:** `Element.java`

All rely on identity equality. Two objects with the same name are not considered equal.

### DESIGN — Flow.history and Variable.history grow unboundedly (Medium)
**File:** `Flow.java:18`, `Variable.java:15`

No way to clear or cap history. Long simulations consume excessive memory.

**Fix:** Add `clearHistory()` methods.

### DESIGN — Flows.exponentialGrowthWithLimit — no guard for limit=0 (Medium)
**File:** `Flows.java:84`

`value * rate * (1 - value / limit)` produces NaN/Infinity when limit is 0.

**Fix:** `Preconditions.checkArgument(limit > 0)`.

### DESIGN — ArrayedFlow.create overload ignores its stock parameter (Medium)
**File:** `ArrayedFlow.java:65-68`

The stock parameter is accepted but unused. Misleading API.

### DESIGN — NaN/Infinity silently accepted or masked by stocks (Medium)
**File:** `Stock.java:110`

NaN falls through `applyPolicy` to `CLAMP_TO_ZERO`, masking formula bugs. Infinity passes unchecked.

**Fix:** Add explicit NaN/Infinity validation in `setValue`.

### MINOR — No null validation on constructors (Stock, Variable, Constant, Flow) (Medium)
**File:** Multiple

### MINOR — Module.getStocks returns mutable copy; getVariables returns unmodifiable (Low)
**File:** `Module.java:130-132`

### MINOR — Element.setComment accepts null without documenting it (Low)

### MINOR — RateConverter is not final, has public constructor (Low)
**File:** `RateConverter.java`

### MINOR — Constant.getIntValue throws ArithmeticException for large values (Low)
**File:** `Constant.java:36-38`

### API — Module missing addArrayedVariable, addArrayedFlow (Low)

---

## 3. SD Functions (Smooth, Delay3, Step, Ramp, LookupTable)

### BUG — Smooth skips integration steps when simulation advances by >1 (High)
**File:** `Smooth.java:87-89`

`getCurrentValue()` only integrates once when `step > lastStep`, regardless of gap size. Jumping from step 1 to step 5 applies only one integration step instead of four.

**Fix:** Loop `(step - lastStep)` times.

### BUG — Delay3 has the same multi-step skip bug (High)
**File:** `Delay3.java:104-118`

Same issue as Smooth — single Euler pass regardless of skipped steps.

### DESIGN — Smooth and Delay3 are not resettable (Medium)
**File:** `Smooth.java`, `Delay3.java`

Mutable internal state with no `reset()` method. Cannot re-run simulations (Monte Carlo, sweeps) without recreating objects.

**Fix:** Add `reset()` method.

### DESIGN — Smooth and Delay3 internal stages are opaque (Low)
No accessors for Delay3's `stage1/2/3` or Smooth's `smoothed` value, preventing debugging and conservation-of-material verification.

### DESIGN — Step/Ramp do not validate stepTime >= 0 (Low)
**File:** `Step.java:27`, `Ramp.java:31`

Negative step times silently accepted.

### MINOR — LookupTable NaN input falls through to interpolation (Low)
**File:** `LookupTable.java:116-124`

NaN input bypasses clamping logic; `interpolation.value(NaN)` propagates NaN silently.

**Fix:** Add `Double.isNaN(input)` check.

---

## 4. Sweep / Monte Carlo / Optimizer

### BUG — MonteCarlo discards sampled parameter values in RunResult (Critical)
**File:** `MonteCarlo.java:79`

`new RunResult(i)` stores the iteration index, not the sampled parameters. `getParameterMap()` returns empty for all Monte Carlo runs.

**Fix:** Change to `new RunResult(paramMap)`.

### BUG — MonteCarlo RANDOM sampling reseeds every distribution on every draw (High)
**File:** `MonteCarlo.java:112-117`

Calling `dist.reseedRandomGenerator(rng.nextLong())` per sample is wasteful and destroys the distribution's internal RNG state.

**Fix:** Reseed each distribution once before the loop.

### BUG — Nelder-Mead does not enforce parameter bounds (High)
**File:** `Optimizer.java:123-137`

User-specified bounds are silently ignored. Nelder-Mead explores unbounded space.

**Fix:** Clamp parameters to bounds inside the `MultivariateFunction` adapter.

### BUG — Optimizer NPE when no evaluation improves (High)
**File:** `Optimizer.java:78-120`

If all evaluations return >= `Double.MAX_VALUE` or NaN, `bestParams[0]` stays null, causing NPE in `OptimizationResult` constructor.

**Fix:** Record first evaluation unconditionally. Guard against null before constructing result.

### BUG — RunResult crashes on zero-step runs (Medium)
**File:** `RunResult.java:120-165`

`getFinalStockValue` accesses `stockSnapshots.get(-1)` when empty.

**Fix:** Add empty-check guard.

### BUG — Objectives.fitToTimeSeries silently truncates length mismatch (Medium)
**File:** `Objectives.java:23`

`Math.min(simulated.length, observedData.length)` silently discards tail data.

**Fix:** Throw or warn when lengths differ significantly.

### DESIGN — RunResult has two constructors with lossy semantics (Medium)
**File:** `RunResult.java:38-51`

`RunResult(double)` sets parameterMap to empty; `RunResult(Map)` hardcodes parameterValue to 0. No way to populate both.

### DESIGN — linspace does not validate step > 0 (Medium)
**File:** `ParameterSweep.java:83-94`

Step=0 causes OOM; negative step returns empty array silently.

**Fix:** Validate `step > 0`.

### DESIGN — SweepResult/MultiSweepResult/MonteCarloResult don't defensively copy input lists (Low)

### DESIGN — ParameterSweep.Builder doesn't clone parameterValues array (Low)

### MINOR — MonteCarloResult.getPercentileSeries recomputes sorting for each percentile (Low)

### MINOR — MonteCarloResult stock/variable name ambiguity silently resolved (Low)

---

## 5. Measurement System & IO/UI

### BUG — Temperature conversion via ratio produces wrong results (Critical)
**File:** `TemperatureUnits.java:17-18`

Fahrenheit uses ratio-based conversion (multiply by 5/9), which is fundamentally wrong for temperature (requires offset). `100°F.inBaseUnits()` = 55.56°C (should be 37.78°C). The `Temperature.getConverter()` throws, but `inBaseUnits()`, `add()`, and comparison methods bypass the converter.

**Fix:** Override `toBaseUnits()`/`fromBaseUnits()` on Fahrenheit to throw, or implement affine conversion properly.

### BUG — Quantity.equals() has contradictory semantics (High)
**File:** `Quantity.java:155-170`

Compares base-unit values (cross-unit equality) but then also requires same unit. Neither "physical equality" nor "exact equality" is implemented consistently.

**Fix:** Choose one semantic and align `hashCode`.

### BUG — FanChart uses Double.MIN_VALUE for max-finding (Medium)
**File:** `FanChart.java:94`

`Double.MIN_VALUE` (~4.9E-324) is the smallest positive double, not the most negative. Breaks when all values are negative.

**Fix:** Use `Double.NEGATIVE_INFINITY`.

### BUG — FanChart division by zero when stepCount=1 (Medium)
**File:** `FanChart.java:126,131,144`

`plotWidth / (stepCount - 1)` = division by zero.

### DESIGN — CsvSubscriber does not close writer on exception (Medium)
**File:** `CsvSubscriber.java:37-48`

File opened in constructor, closed only in `handleSimulationEndEvent`. If simulation throws, file handle leaks. Does not implement `Closeable`.

**Fix:** Implement `Closeable`. Open file in `handleSimulationStartEvent` or use try-with-resources.

### DESIGN — ChartViewerApplication uses all-static state (Medium)
**File:** `ChartViewerApplication.java:41-48`

Only one chart per JVM. `Application.launch()` can only be called once. Both `StockLevelChartViewer` and `FlowChartViewer` call it, causing crashes on second use.

### DESIGN — Quantity comparison methods don't check dimension compatibility (Medium)
**File:** `Quantity.java:106-148`

`isLessThan()`, `isGreaterThan()`, etc. allow comparing meters to dollars.

**Fix:** Add `isCompatibleWith()` check (consistent with `add`/`subtract`).

### DESIGN — ItemUnit lacks equals/hashCode (Medium)
**File:** `ItemUnit.java`

Non-enum unit class; two `ItemUnit("Widget")` instances are not equal.

### DESIGN — Quantity constructor does not validate null unit or NaN (Low)
**File:** `Quantity.java:21-24`

### DESIGN — Quantity.divide(0) silently produces Infinity (Low)
**File:** `Quantity.java:56`

### MINOR — CsvSubscriber calls getStockValues()/getVariableNames() in every loop iteration — O(n²) (Low)
**File:** `CsvSubscriber.java:62-67`

### MINOR — FlowChartViewer Javadoc says "stock levels" — copy-paste error (Low)
**File:** `FlowChartViewer.java:16`

### MINOR — ModelReport output order nondeterministic (HashMap iteration) (Low)

---

## 6. Test Coverage Gaps (Cross-Cutting)

| Area | Gap |
|------|-----|
| ArrayedVariable | No test file exists at all |
| CsvSubscriber | Zero test coverage |
| UI classes | Zero test coverage (StockLevelChartViewer, FlowChartViewer, FanChart, ChartViewerApplication) |
| Simulation events | No test for event ordering, count, or handler registration/removal |
| Mixed time units | No test for simulation with different time-step and duration units |
| MILLISECOND/MONTH | No tests for recently-added time units |
| Temperature units | No tests |
| Dimensionless | No tests |
| ItemUnit | No tests |
| ModelReportTest | Has no assertions (smoke test only) |
| Flow.recordValue double-counting | No test exposes the duplicate-recording bug |
| Smooth/Delay3 multi-step jump | No tests for step gaps > 1 |
| Optimizer edge cases | No test for TooManyEvaluationsException, initial guess out of bounds |
| linspace edge cases | No test for step=0, step<0, start>end |
| pipelineDelay null sink | No test for the precondition failure path |
| Quantity edge cases | No tests for divide(0), NaN, null unit, cross-dimension comparison |
| MonteCarloResult CSV content | File existence tested, but content never verified |

---

## Priority Summary

| Severity | Count |
|----------|-------|
| Critical | 5 |
| High | 12 |
| Medium | 25 |
| Low | 20+ |
| Test Gaps | 17 areas |

### Top 10 fixes (highest impact):

1. **Simulation: double-recording of flow values** — corrupts history, breaks pipelineDelay
2. **Simulation: sub-second time truncation** — makes MILLISECOND time unit completely broken
3. **MonteCarlo: discarded parameter map** — makes MC results fundamentally incomplete
4. **MonteCarlo: per-sample reseeding** — wastes performance, destroys RNG state
5. **Nelder-Mead: ignores parameter bounds** — users specify bounds that are silently ignored
6. **Smooth/Delay3: skipped integration steps** — wrong results when steps are skipped
7. **Temperature: ratio-based conversion** — silently produces wrong Fahrenheit conversions
8. **ArrayedStock: scalar flow wiring** — overwrites sink N-1 times, applies flow N times
9. **Simulation: off-by-one (N+1 steps)** — every simulation runs one extra step
10. **Optimizer: NPE when no evaluation improves** — crashes instead of returning best-so-far
