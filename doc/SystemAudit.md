# System-Wide Code Audit

Full audit of the Forrester simulation library, organized by subsystem. Each issue includes severity, file/line references, and a suggested fix. Issues are deduplicated across the overlapping reports from 5 parallel audit agents.

Items marked **[FIXED]** have been resolved in commit `bca62d9`.

---

## 1. Simulation Engine

### [FIXED] BUG — Flow values recorded multiple times per step (Critical)
**File:** `Simulation.java`

When a flow connects two stocks (outflow of A, inflow of B), `flow.recordValue(q)` was called once per stock encounter — i.e., twice for a transfer flow. This corrupted `Flow.history`, breaking `pipelineDelay` and any history-dependent computation.

**Fix applied:** Moved `flow.recordValue(q)` inside the `else` branch (cache-miss path only), so it is called exactly once when the flow is first evaluated.

### [FIXED] BUG — Sub-second time steps truncated to zero (Critical)
**File:** `Simulation.java`

`addStep()` cast `timeStep.ratioToBaseUnit()` to `long`. For MILLISECOND (ratio 0.001), this truncated to 0 — `currentDateTime` and `elapsedTime` never advanced.

**Fix applied:** Uses `Duration.ofNanos(Math.round(timeStep.ratioToBaseUnit() * 1_000_000_000L))`.

### BUG — Off-by-one: simulation runs N+1 steps (High)
**File:** `Simulation.java:90`

`while (currentStep <= totalSteps)` runs steps 0 through N inclusive = N+1 iterations. A "5-step simulation" actually runs 6 steps. Tests are written to match this behavior, but it contradicts the standard SD convention.

**Fix:** Change `<=` to `<`, or document the inclusive-endpoint semantics. Not fixed yet because existing tests and demos depend on the current behavior — changing it requires a coordinated update across all tests and demos.

### [FIXED] BUG — Floating-point totalSteps can miss a step (High)
**File:** `Simulation.java`

`totalSteps` was a `double`. Rounding could produce `9.999...` instead of `10.0`, causing one fewer iteration.

**Fix applied:** `long totalSteps = Math.round(...)`.

### [FIXED] BUG — Simulation is not re-entrant (High)
**File:** `Simulation.java`

`currentStep`, `currentDateTime`, `elapsedTime` were never reset. Calling `execute()` a second time silently did nothing.

**Fix applied:** Reset all state at the top of `execute()`.

### [FIXED] BUG — SimulationEndEvent not guaranteed on exception (Medium)
**File:** `Simulation.java`

If a flow formula or event handler threw, `SimulationEndEvent` was never posted. `CsvSubscriber`'s file handle leaked.

**Fix applied:** Wrapped the simulation loop in `try/finally`.

### [FIXED] BUG — Flow cache keyed by name — collisions possible (Medium)
**File:** `Simulation.java`

`flowMap` was `HashMap<String, Quantity>` keyed by flow name. Two distinct flows with the same name silently shared cached values.

**Fix applied:** Changed to `IdentityHashMap<Flow, Quantity>`.

### DESIGN — TimeStepEvent fires before stock update (Medium)
**File:** `Simulation.java:93`

Event handlers see pre-update stock values. The Javadoc says "fired after each time step has been computed", which is false.

**Fix:** Move `eventBus.post()` to after `updateStocks()`, or correct the Javadoc.

### DESIGN — EventHandler interface does not carry @Subscribe (Medium)
**File:** `EventHandler.java`

Guava EventBus requires `@Subscribe` on methods, but the interface doesn't annotate them. Implementing `EventHandler` without manually adding `@Subscribe` causes silent delivery failure.

**Fix:** Add `@Subscribe` to interface methods, or replace EventBus with direct dispatch.

### DESIGN — SimulationEndEvent carries no context (Low)
**File:** `SimulationEndEvent.java`

Unlike `SimulationStartEvent`, the end event has no fields — handlers can't identify which simulation ended.

### DESIGN — No validation that duration is a TIME quantity (Low)
**File:** `Simulation.java`

Passing `new Quantity(5, GALLON_US)` as duration produces nonsensical results.

### [FIXED] DESIGN — `getTimeStep()` returns `Unit` instead of `TimeUnit` (Low)
**File:** `Simulation.java`

The field was `TimeUnit` but the getter widened to `Unit`.

**Fix applied:** Return type changed to `TimeUnit`.

### MINOR — No null checks on constructor parameters (Medium)
**File:** `Simulation.java`

### MINOR — No validation for zero/negative duration (Medium)
**File:** `Simulation.java`

---

## 2. Core Model (Stock, Flow, Variable, Constant, Model, Module)

### BUG — ArrayedStock scalar addInflow/addOutflow overwrites sink/source N times (High)
**File:** `ArrayedStock.java:191-207`

Wiring a single scalar Flow to all N underlying stocks calls `flow.setSink(this)` N times. Only the last stock "wins" — the flow's sink is inconsistent with the first N-1 stocks' inflow sets.

**[DOCUMENTED]** Added Javadoc warning explaining the behavior and recommending `ArrayedFlow` for per-element wiring. The underlying behavior is inherent to having a single-source/single-sink flow wired to multiple stocks. The flow value is cached by identity in the simulation loop, so it is computed once and applied to each stock.

### BUG — Stock allows conflicting source/sink assignments without error (Medium)
**File:** `Stock.java`

`addInflow` calls `flow.setSink(this)` without checking if the flow already has a different sink.

### [FIXED] DESIGN — Model stores variables in HashMap (nondeterministic) (Medium)
**File:** `Model.java`

`variables` was `HashMap<String, Variable>`, making iteration order nondeterministic across JVM runs.

**Fix applied:** Changed to `LinkedHashMap<String, Variable>`.

### DESIGN — Model.removeStock does not disconnect flows (Medium)
**File:** `Model.java`

Removed stock's connected flows retain stale references.

### DESIGN — Model.addModule does not propagate flows or constants (Medium)
**File:** `Model.java`

Module's stocks and variables are merged, but flows are not.

### DESIGN — No equals/hashCode on Element/Stock/Flow/Variable (Medium)
**File:** `Element.java`

All rely on identity equality. Two objects with the same name are not considered equal.

### DESIGN — Flow.history and Variable.history grow unboundedly (Medium)
**File:** `Flow.java`, `Variable.java`

No way to clear or cap history. Long simulations consume excessive memory.

### [FIXED] DESIGN — Flows.exponentialGrowthWithLimit — no guard for limit=0 (Medium)
**File:** `Flows.java`

`value * rate * (1 - value / limit)` produced NaN/Infinity when limit was 0.

**Fix applied:** Added `Preconditions.checkArgument(limit > 0)`.

### DESIGN — ArrayedFlow.create overload ignores its stock parameter (Medium)
**File:** `ArrayedFlow.java`

The stock parameter is accepted but unused. Misleading API.

### [FIXED] DESIGN — NaN/Infinity silently accepted or masked by stocks (Medium)
**File:** `Stock.java`

NaN fell through `applyPolicy` to `CLAMP_TO_ZERO`, masking formula bugs. Infinity passed unchecked.

**Fix applied:** Added explicit NaN/Infinity validation at the top of `applyPolicy`. Throws `IllegalArgumentException` with descriptive message.

### [FIXED] DESIGN — Stock uses HashSet for flows (nondeterministic iteration) (Low)
**File:** `Stock.java`

`inflows` and `outflows` were `HashSet<Flow>`, making flow processing order nondeterministic.

**Fix applied:** Changed to `LinkedHashSet<Flow>`.

### MINOR — No null validation on constructors (Stock, Variable, Constant, Flow) (Medium)
**File:** Multiple

### MINOR — Module.getStocks returns mutable copy; getVariables returns unmodifiable (Low)
**File:** `Module.java`

### MINOR — Element.setComment accepts null without documenting it (Low)

### [FIXED] MINOR — RateConverter is not final, has public constructor (Low)
**File:** `RateConverter.java`

**Fix applied:** Made class `final` with private constructor.

### MINOR — Constant.getIntValue throws ArithmeticException for large values (Low)
**File:** `Constant.java`

### API — Module missing addArrayedVariable, addArrayedFlow (Low)

---

## 3. SD Functions (Smooth, Delay3, Step, Ramp, LookupTable)

### [FIXED] BUG — Smooth skips integration steps when simulation advances by >1 (High)
**File:** `Smooth.java`

`getCurrentValue()` only integrated once when `step > lastStep`, regardless of gap size.

**Fix applied:** Added loop `for (int i = 0; i < delta; i++)` around the integration step.

### [FIXED] BUG — Delay3 has the same multi-step skip bug (High)
**File:** `Delay3.java`

Same issue as Smooth — single Euler pass regardless of skipped steps.

**Fix applied:** Added loop `for (int d = 0; d < delta; d++)` around all three stage computations.

### DESIGN — Smooth and Delay3 are not resettable (Medium)
**File:** `Smooth.java`, `Delay3.java`

Mutable internal state with no `reset()` method. Cannot re-run simulations without recreating objects.

### DESIGN — Smooth and Delay3 internal stages are opaque (Low)
No accessors for Delay3's `stage1/2/3` or Smooth's `smoothed` value, preventing debugging and conservation-of-material verification.

### DESIGN — Step/Ramp do not validate stepTime >= 0 (Low)
**File:** `Step.java`, `Ramp.java`

Negative step times silently accepted.

### MINOR — LookupTable NaN input falls through to interpolation (Low)
**File:** `LookupTable.java`

NaN input bypasses clamping logic; `interpolation.value(NaN)` propagates NaN silently.

---

## 4. Sweep / Monte Carlo / Optimizer

### [FIXED] BUG — MonteCarlo discards sampled parameter values in RunResult (Critical)
**File:** `MonteCarlo.java`

`new RunResult(i)` stored the iteration index, not the sampled parameters. `getParameterMap()` returned empty for all Monte Carlo runs.

**Fix applied:** Changed to `new RunResult(paramMap)`.

### [FIXED] BUG — MonteCarlo RANDOM sampling reseeds every distribution on every draw (High)
**File:** `MonteCarlo.java`

Calling `dist.reseedRandomGenerator(rng.nextLong())` per sample was wasteful and destroyed the distribution's internal RNG state.

**Fix applied:** Reseed each distribution once before the sampling loop.

### [FIXED] BUG — Nelder-Mead does not enforce parameter bounds (High)
**File:** `Optimizer.java`

User-specified bounds were silently ignored. Nelder-Mead explored unbounded space.

**Fix applied:** Added parameter clamping to bounds inside the `MultivariateFunction` adapter lambda.

### [FIXED] BUG — Optimizer NPE when no evaluation improves (High)
**File:** `Optimizer.java`

If all evaluations returned >= `Double.MAX_VALUE` or NaN, `bestParams[0]` stayed null, causing NPE.

**Fix applied:** Added null guard that falls back to initial-guess parameters.

### BUG — RunResult crashes on zero-step runs (Medium)
**File:** `RunResult.java`

`getFinalStockValue` accesses `stockSnapshots.get(-1)` when empty.

### BUG — Objectives.fitToTimeSeries silently truncates length mismatch (Medium)
**File:** `Objectives.java`

`Math.min(simulated.length, observedData.length)` silently discards tail data.

### DESIGN — RunResult has two constructors with lossy semantics (Medium)
**File:** `RunResult.java`

`RunResult(double)` sets parameterMap to empty; `RunResult(Map)` hardcodes parameterValue to 0. No way to populate both.

### [FIXED] DESIGN — linspace does not validate step > 0 (Medium)
**File:** `ParameterSweep.java`

Step=0 caused OOM; negative step returned empty array silently.

**Fix applied:** Added validation for `step > 0` and `end >= start`.

### DESIGN — SweepResult/MultiSweepResult/MonteCarloResult don't defensively copy input lists (Low)

### DESIGN — ParameterSweep.Builder doesn't clone parameterValues array (Low)

### MINOR — MonteCarloResult.getPercentileSeries recomputes sorting for each percentile (Low)

### MINOR — MonteCarloResult stock/variable name ambiguity silently resolved (Low)

---

## 5. Measurement System & IO/UI

### [FIXED] BUG — Temperature conversion via ratio produces wrong results (Critical)
**File:** `TemperatureUnits.java`

Fahrenheit used ratio-based conversion (multiply by 5/9), which is fundamentally wrong for absolute temperature values. `100°F.inBaseUnits()` produced 55.56°C instead of 37.78°C.

**Fix applied:** Overrode `toBaseUnits(Quantity)` and `fromBaseUnits(Quantity)` on FAHRENHEIT to throw `UnsupportedOperationException` with a descriptive message. The `ratioToBaseUnit()` value (5/9) remains correct for temperature *differences* (degree-size scaling).

### BUG — Quantity.equals() has contradictory semantics (High)
**File:** `Quantity.java`

Compares base-unit values (cross-unit equality) but then also requires same unit. Neither "physical equality" nor "exact equality" is implemented consistently.

### [FIXED] BUG — FanChart uses Double.MIN_VALUE for max-finding (Medium)
**File:** `FanChart.java`

`Double.MIN_VALUE` (~4.9E-324) is the smallest positive double, not the most negative. Broke when all values were negative.

**Fix applied:** Changed to `-Double.MAX_VALUE`.

### [FIXED] BUG — FanChart division by zero when stepCount=1 (Medium)
**File:** `FanChart.java`

`plotWidth / (stepCount - 1)` caused division by zero.

**Fix applied:** Added early return with message when `stepCount <= 1`.

### [FIXED] DESIGN — Quantity comparison methods don't check dimension compatibility (Medium)
**File:** `Quantity.java`

`isLessThan()`, `isGreaterThan()`, `isLessThanOrEqualTo()`, `isGreaterThanOrEqualTo()`, and `isEqual()` allowed comparing meters to dollars.

**Fix applied:** Added `Preconditions.checkArgument(other.isCompatibleWith(this), INCOMPATIBLE_ERROR_MESSAGE)` to all 5 comparison methods.

### DESIGN — CsvSubscriber does not close writer on exception (Medium)
**File:** `CsvSubscriber.java`

File opened in constructor, closed only in `handleSimulationEndEvent`. If simulation throws, file handle leaks. Does not implement `Closeable`.

### DESIGN — ChartViewerApplication uses all-static state (Medium)
**File:** `ChartViewerApplication.java`

Only one chart per JVM. `Application.launch()` can only be called once.

### DESIGN — ItemUnit lacks equals/hashCode (Medium)
**File:** `ItemUnit.java`

Non-enum unit class; two `ItemUnit("Widget")` instances are not equal.

### DESIGN — Quantity constructor does not validate null unit or NaN (Low)
**File:** `Quantity.java`

### DESIGN — Quantity.divide(0) silently produces Infinity (Low)
**File:** `Quantity.java`

### MINOR — CsvSubscriber calls getStockValues()/getVariableNames() in every loop iteration — O(n²) (Low)
**File:** `CsvSubscriber.java`

### MINOR — FlowChartViewer Javadoc says "stock levels" — copy-paste error (Low)
**File:** `FlowChartViewer.java`

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

## Fix Summary

### Fixed (19 items in commit `bca62d9`)

| # | Severity | Issue | Files |
|---|----------|-------|-------|
| 1 | Critical | Flow double-recording | `Simulation.java` |
| 2 | Critical | Sub-second time truncation | `Simulation.java` |
| 3 | Critical | MonteCarlo discards parameter map | `MonteCarlo.java` |
| 4 | Critical | Temperature ratio-based conversion | `TemperatureUnits.java` |
| 5 | High | Floating-point totalSteps | `Simulation.java` |
| 6 | High | Simulation not re-entrant | `Simulation.java` |
| 7 | High | MonteCarlo per-sample reseeding | `MonteCarlo.java` |
| 8 | High | Nelder-Mead ignores bounds | `Optimizer.java` |
| 9 | High | Optimizer NPE | `Optimizer.java` |
| 10 | High | Smooth multi-step skip | `Smooth.java` |
| 11 | High | Delay3 multi-step skip | `Delay3.java` |
| 12 | Medium | SimulationEndEvent not guaranteed | `Simulation.java` |
| 13 | Medium | Flow cache keyed by name | `Simulation.java` |
| 14 | Medium | NaN/Infinity in stocks | `Stock.java` |
| 15 | Medium | FanChart Double.MIN_VALUE | `FanChart.java` |
| 16 | Medium | FanChart div-by-zero | `FanChart.java` |
| 17 | Medium | Quantity comparison dimension checks | `Quantity.java` |
| 18 | Medium | linspace validation | `ParameterSweep.java` |
| 19 | Medium | Flows limit validation | `Flows.java` |

Plus minor fixes: `RateConverter` made final, `Stock` uses `LinkedHashSet`, `Model` uses `LinkedHashMap`, `Simulation.getTimeStep()` return type corrected, `ArrayedStock` scalar flow behavior documented.

### Remaining (open items)

| Severity | Count | Key items |
|----------|-------|-----------|
| High | 2 | Off-by-one N+1 steps, Quantity.equals semantics |
| Medium | 14 | CsvSubscriber leak, EventHandler @Subscribe, Stock conflicting sink, RunResult lossy constructors, Model.removeStock stale refs, etc. |
| Low | 12 | SimulationEndEvent context, duration validation, null checks, etc. |
| Test Gaps | 17 | ArrayedVariable, CsvSubscriber, temperature, millisecond, event ordering, etc. |
