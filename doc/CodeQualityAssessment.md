# Code Quality Assessment — Forrester SD Library

**Date:** 2026-03-01
**Scope:** Full codebase audit after five rounds of fixes and comprehensive regression testing
**Methodology:** Manual code review of all 105 source files across 20 packages by 5 specialized audit agents, cross-referenced with 439 passing tests

---

## Executive Summary

Forrester is a **9,500-line Java system dynamics simulation library** with 27 demo programs, a measurement system covering 8 physical dimensions, parameter sweep / Monte Carlo / optimization analysis tools, single- and multi-dimensional subscripts, and JavaFX visualization.

Five rounds of fixes resolved **50 bugs and improvements** including 4 critical bugs, 7 high-severity bugs, and 39 medium/low items. Eighty new tests were added covering regression scenarios, temperature units, time units, Quantity edge cases, ArrayedVariable, CsvSubscriber, and ItemUnit.

**Overall Quality Rating: A** — Core simulation logic is correct and well-tested. Analysis tools work reliably. The measurement system handles unit conversions correctly (with Fahrenheit explicitly unsupported). CSV output uses explicit UTF-8 encoding. All 439 tests pass.

---

## Codebase Profile

| Metric | Value |
|--------|-------|
| Source files (main) | 105 |
| Test files | 44 |
| Main source lines | ~9,500 |
| Test source lines | ~5,200 |
| Test-to-source ratio | 0.55 |
| Packages | 20 |
| Test count | 439 (all passing) |
| Build time | ~4 seconds |
| Dependencies | Guava 33.4, Commons Math 3, OpenCSV, JavaFX, SLF4J |
| Java version | 17+ |

---

## Remaining Findings

After five rounds of fixes, **52 findings remain** — none are high-severity bugs. The remaining items are design concerns, latent/inherent edge cases, and minor issues that do not affect the library's primary use case.

### Summary by Severity

| Severity | Count | Description |
|----------|-------|-------------|
| Bug (medium) | 3 | Edge-case failures unlikely in normal usage |
| Bug (low/latent) | 6 | Inherent limitations or extremely unlikely triggers |
| Design | 18 | API inconsistencies, missing defensive measures, extensibility gaps |
| Minor | 25 | Code style, naming, documentation, minor redundancies |
| **Total** | **52** | |

### Summary by Subsystem

| Subsystem | Bugs | Design | Minor | Total |
|-----------|------|--------|-------|-------|
| Simulation engine & events | 0 | 2 | 4 | 6 |
| Core model (Stock, Flow, Variable, Model, Module) | 2 | 4 | 4 | 10 |
| Measurement system (Dimension, Unit, Quantity) | 0 | 6 | 3 | 9 |
| Sweep / Monte Carlo / Optimizer | 3 | 6 | 3 | 12 |
| SD functions & IO/UI | 3 | 6 | 8 | 17 |
| Remaining test gaps | 0 | 0 | 2 | 2 |

---

## Detailed Findings by Subsystem

### 1. Simulation Engine & Events

No remaining bugs. `EventHandler` now provides default no-op methods with `@Subscribe`, eliminating the silent-delivery trap. `Simulation` Javadoc documents the single-threaded contract. `Simulation.clearHistory()` delegates to all flows and variables and is auto-invoked at the start of each `execute()` call.

**Design:**

| # | Finding |
|---|---------|
| S2 | `getEventBus()` exposes the raw Guava EventBus, allowing external code to post arbitrary events. |
| S4 | Asymmetric event objects: `SimulationStartEvent` carries model + simulation; `SimulationEndEvent` carries model but not simulation. |

**Minor:** Misleading local variable `q` for cached flow quantities; `addStep` name implies adding to a collection; no null check on `addEventHandler`/`removeEventHandler`; off-by-one N+1 steps documented but not resolved.

### 2. Core Model Classes

**Bugs:**

| # | Impact | Finding |
|---|--------|---------|
| M3 | Medium | **addModule doesn't register module flows.** Module's stocks and variables merge into the model, but flows are not. Flows still execute (referenced by stocks), but model-level flow queries won't find them. |
| M4 | Low | **Flow history records phantom amounts on clamped stocks.** When `NegativeValuePolicy` clamps to zero, the flow's recorded value reflects the unclamped amount. |

**Design:**

| # | Finding |
|---|---------|
| M5 | No null check on `Flow` timeUnit constructor parameter. |
| M7 | Bidirectional coupling between Stock and Flow. Keeping both in sync is error-prone. |
| M8 | No model-level accessor for all flows — flows are only reachable through stocks. |
| M10 | `addStock` allows duplicate names without warning; same for `addConstant`. |

**Minor:** `checkArgument` used where `checkNotNull` is more appropriate in some places; `Constant` accepts NaN as initial value; `Element.setComment(null)` undocumented; `ArrayedFlow.create` overload accepts unused Stock parameter.

### 3. Measurement System

All Priority 1 bugs have been fixed. No remaining bugs.

**Design:**

| # | Finding |
|---|---------|
| U1 | No `DIMENSIONLESS` constant on the `Dimension` class — users must reference `DimensionlessUnits.NONE.getDimension()`. |
| U2 | `ItemUnit.equals()` compares by name but doesn't interoperate with `ItemUnits` enum — different classes. |
| U3 | `ItemUnit` constructor doesn't validate null name. |
| U4 | `Lengths` and `Times` unit-holder classes are not final. |
| U5 | No mechanism for user-defined dimensions beyond the 8 built-in ones. |
| U6 | `Quantity.toString()` format includes parentheses around the unit — unconventional. |

**Minor:** `MONTH` uses 30-day approximation (standard SD convention but differs from calendar); `Dimensionless` package only has `NONE` and `PERCENT`; `toString()` could show dimension for clarity.

### 4. Sweep / Monte Carlo / Optimizer

**Bugs:**

| # | Impact | Finding |
|---|--------|---------|
| A1 | Medium | **Optimizer `bestRun[0]` null in fallback path.** If all evaluations fail, `bestRun[0]` (the RunResult) remains null. |
| A2 | Low | **RunResult.getMaxStockValue returns NEGATIVE_INFINITY on empty.** Missing stock name returns `NEGATIVE_INFINITY` instead of throwing. |
| A3 | Low | **MonteCarloResult stock/variable name collision.** Same-named stock and variable: `getPercentileSeries` silently picks the first match. |

**Design:**

| # | Finding |
|---|---------|
| A4 | `Objectives.fitToTimeSeries` silently truncates when simulated/observed arrays differ in length. |
| A6 | `MonteCarlo.reseedRandomGenerator` mutates the caller's distribution objects. Side effect not documented. |
| A7 | CMA-ES population size hardcoded — adequate but not configurable. |
| A8 | `RunResult` dual constructor (`double` vs `Map`) — lossy, can't populate both. |
| A9 | No defensive copy of `parameterValues` array in `ParameterSweep.Builder`. |
| A11 | `RuntimeException` wrapping of checked exceptions loses type information. |

**Minor:** `linspace` name implies `numpy.linspace` semantics; `formatPercentile` relies on specific double-to-string formatting; `File.mkdirs()` return value ignored.

### 5. SD Functions, IO & UI

**Bugs:**

| # | Impact | Finding |
|---|--------|---------|
| F1 | Low | **Smooth/Delay3 multi-step catch-up uses stale input.** Loops N times but reads the current input at each iteration (intermediate values are unknowable). Inherent limitation. |
| F2 | Low | **CsvSubscriber NPE if `getCurrentTime()` returns null.** Unlikely — Simulation always provides a non-null timestamp. |
| F3 | Low | **ChartViewerApplication series/values size mismatch.** Only triggers with asymmetric stock/flow chart setup. |

**Design:**

| # | Finding |
|---|---------|
| F4 | `FanChart` and `ChartViewerApplication` use all-static state — only one chart per JVM. |
| F5 | `ChartViewerApplication.launch()` can only be called once per JVM (JavaFX limitation). |
| F6 | `ModelReport` output format ends without a trailing newline. |
| F7 | `CsvSubscriber` opens the file in its constructor — prevents deferred initialization. |
| F9 | `LookupTable.Builder` doesn't detect duplicate x values. |
| F10 | `CsvSubscriber.close()` doesn't prevent subsequent event handler calls on a closed writer. |

**Minor:** Unused imports in some demo files; commented-out code in `ChartViewerApplication`; raw types in some event handler casts; Ramp uses `Integer.MAX_VALUE` sentinel for unbounded end; FanChart can only be launched once; redundant `ModelReport` console output; missing `@Override` on some handlers; latent optimizer thread-safety assumption.

---

## Test Coverage

### Well-Tested Areas

| Area | Test File(s) | Coverage |
|------|-------------|----------|
| Core simulation loop | `SimulationTest`, `RegressionTest` | Good — stocks, flows, transfers, re-entrancy, sub-second steps, exception handling |
| Stock mechanics | `StockTest`, `RegressionTest` | Good — policies, NaN/Infinity rejection, null validation |
| Flow formulas | `FlowsTest`, `FlowTest`, `RegressionTest` | Good — factory functions, identity caching, history |
| Smooth/Delay3 | `SmoothTest`, `Delay3Test`, `RegressionTest` | Good — basic behavior + multi-step gap regression |
| LookupTable | `LookupTableTest` | Good — linear, spline, builder, NaN, edge cases |
| Quantity | `QuantityTest`, `QuantityEdgeCaseTest` | Good — arithmetic, comparison, cross-unit equality, null, divide-by-zero |
| Temperature | `TemperatureUnitsTest` | Good — Celsius/Fahrenheit behavior, equality, comparisons, conversion blocking |
| Time units | `TimeUnitsTest` | Good — all 8 units, ratios, MILLISECOND/MONTH conversion |
| ItemUnit | `ItemUnitTest` | Good — equality, hashCode, toString |
| Subscripts | `ArrayedStockTest`, `ArrayedVariableTest`, `SubscriptRangeTest`, `MultiArrayed*Test` | Good — expansion, naming, index math, scalar flow rejection |
| Parameter sweep | `ParameterSweepTest` | Good — single, multi, linspace, CSV output |
| Monte Carlo | `MonteCarloTest` | Good — sampling modes, percentiles, result aggregation |
| Optimizer | `OptimizerTest` | Good — all 3 algorithms, bounds, convergence |
| CsvSubscriber | `CsvSubscriberTest` | Good — header, data rows, variables, closeable, parent dirs |
| Variable | `VariableTest`, `RegressionTest` | Good — formulas, null checks, history |

### Remaining Gaps

| Area | Gap | Risk |
|------|-----|------|
| UI classes (FanChart, ChartViewerApplication, viewers) | No tests | Low — visual output, hard to unit test |
| Simulation events | No test for event ordering or handler lifecycle | Low |
| Optimizer edge cases | No TooManyEvaluations test | Low |
| MonteCarloResult CSV content | File existence tested, content not verified | Low |

**Test-to-source ratio:** 0.55 (5,200 test lines / 9,500 source lines). Good coverage with regression tests for all major fixed bugs.

---

## Quality Metrics Summary

| Dimension | Rating | Notes |
|-----------|--------|-------|
| **Correctness** | A | All critical and high bugs fixed with regression tests. Core simulation, SD functions, and analysis tools produce correct results. |
| **Robustness** | A | Strong input validation. Fahrenheit explicitly unsupported (throws). NaN/Infinity rejected. Scalar-flow-to-array blocked. Flow source/sink reassignment throws. |
| **API Design** | A- | Clean SD-vocabulary API. Builder patterns, static factories, lambdas. EventHandler has default no-ops. Smooth/Delay3 resettable. Some inconsistencies remain (dual RunResult constructors, unused parameters). |
| **Maintainability** | A- | Good package structure. Deterministic ordering (LinkedHashMap/Set). removeStock properly detaches flows. clearHistory auto-invoked on execute(). |
| **Documentation** | A- | Good Javadoc on public API. Class-level docs explain SD concepts. Threading contract documented. |
| **Test Quality** | B+ | 439 tests, all passing. Regression coverage for all major bugs. Remaining gaps are UI and edge cases. |
| **Security** | A- | No network exposure, no SQL, no user input parsing. CSV writers use explicit UTF-8 encoding. |

---

## Recommendations (Remaining)

### Priority 1 — Design Improvements

1. **Add `Model.getFlows()`** method that returns all flows across all stocks. (Finding M8)

### Priority 2 — Nice to Have

2. Register module flows at the model level in `addModule`. (Finding M3)

---

## Overall Assessment

**Grade: A**

Forrester is a well-designed educational and research-grade SD library. The core simulation mechanics are correct — stocks accumulate, flows transfer, feedback loops work, SD functions (Smooth, Delay3, Step, Ramp, LookupTable) behave as expected, and the analysis tools (parameter sweep, Monte Carlo, optimization) produce reliable results.

Five rounds of fixes addressed all critical and high-severity bugs with comprehensive regression tests. The remaining 52 findings are design concerns, inherent limitations, and minor issues that do not affect the library's primary use case.

**Strengths:**
- Clean API that maps directly to SD vocabulary
- Correct simulation mechanics with deterministic ordering
- Comprehensive analysis toolkit (sweep, MC, optimization)
- Strong measurement system with dimensional analysis and explicit Fahrenheit blocking
- Good demo collection covering the SD curriculum
- Consistent use of immutable Quantity, static factories, and builder patterns
- EventHandler with default no-ops and proper `@Subscribe` annotations
- Immutable NegativeValuePolicy on Stock (final field)
- Flow source/sink reassignment validation
- removeStock properly detaches connected flows
- Simulation auto-clears history on each execute() call
- Smooth and Delay3 resettable for simulation re-runs
- CSV output with explicit UTF-8 encoding
- MultiParameterSweep size guard against OOM
- 439 tests with regression coverage for all major bug fixes

**Areas for improvement:**
- No model-level flow accessor (flows only reachable through stocks)
- Bidirectional Stock↔Flow coupling
- Some API inconsistencies (dual RunResult constructors, unused flow parameters)
- UI layer is all-static (single chart per JVM)
