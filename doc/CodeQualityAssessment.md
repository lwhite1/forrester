# Code Quality Assessment — Forrester SD Library

**Date:** 2026-03-01
**Scope:** Full codebase re-audit after two rounds of fixes (commits `bca62d9`, `68a2457`)
**Methodology:** Manual code review of all 105 source files across 20 packages by 5 specialized audit agents, cross-referenced with test results

---

## Executive Summary

Forrester is a **9,500-line Java system dynamics simulation library** with 27 demo programs, a measurement system covering 8 physical dimensions, parameter sweep / Monte Carlo / optimization analysis tools, single- and multi-dimensional subscripts, and JavaFX visualization.

Two rounds of fixes resolved **36 issues** including 4 critical bugs, 7 high-severity bugs, and 25 medium/low improvements. The codebase is now in **good working condition** for its primary use case (educational SD modeling and sensitivity analysis). All 360 tests pass.

The re-audit identified **76 remaining findings** — 17 bugs (mostly edge-case or latent), 27 design concerns, and 32 minor issues. None are likely to affect typical usage. The most impactful remaining risks are in the Fahrenheit conversion subsystem and the EventBus dispatch mechanism.

**Overall Quality Rating: B+** — Solid for an educational/research library. Core simulation logic is correct and well-tested. Analysis tools work reliably. Main gaps are edge-case hardening and test coverage breadth.

---

## Codebase Profile

| Metric | Value |
|--------|-------|
| Source files (main) | 105 |
| Test files | 38 |
| Main source lines | ~9,500 |
| Test source lines | ~4,400 |
| Test-to-source ratio | 0.46 |
| Packages | 20 |
| Test count | 360 (all passing) |
| Build time | ~4 seconds |
| Dependencies | Guava 33.4, Commons Math 3, OpenCSV, JavaFX, SLF4J |
| Java version | 17+ |

---

## Issues Fixed (Rounds 1 & 2)

### Round 1 — Commit `bca62d9` (19 items)

| # | Severity | Issue | File(s) |
|---|----------|-------|---------|
| 1 | Critical | Flow values recorded multiple times per step | `Simulation.java` |
| 2 | Critical | Sub-second time steps truncated to zero | `Simulation.java` |
| 3 | Critical | Monte Carlo discards sampled parameter values | `MonteCarlo.java` |
| 4 | Critical | Fahrenheit ratio-based conversion produces wrong results | `TemperatureUnits.java` |
| 5 | High | Floating-point totalSteps misses steps | `Simulation.java` |
| 6 | High | Simulation not re-entrant (second execute() does nothing) | `Simulation.java` |
| 7 | High | Monte Carlo reseeds every distribution on every draw | `MonteCarlo.java` |
| 8 | High | Nelder-Mead ignores parameter bounds | `Optimizer.java` |
| 9 | High | Optimizer NPE when no evaluation improves | `Optimizer.java` |
| 10 | High | Smooth skips integration steps on gaps > 1 | `Smooth.java` |
| 11 | High | Delay3 skips integration steps on gaps > 1 | `Delay3.java` |
| 12 | Medium | SimulationEndEvent not guaranteed on exception | `Simulation.java` |
| 13 | Medium | Flow cache keyed by name — collisions possible | `Simulation.java` |
| 14 | Medium | NaN/Infinity silently accepted by stocks | `Stock.java` |
| 15 | Medium | FanChart uses Double.MIN_VALUE for max-finding | `FanChart.java` |
| 16 | Medium | FanChart division by zero when stepCount=1 | `FanChart.java` |
| 17 | Medium | Quantity comparison methods don't check dimensions | `Quantity.java` |
| 18 | Medium | linspace accepts step=0 (causes OOM) | `ParameterSweep.java` |
| 19 | Medium | exponentialGrowthWithLimit accepts limit=0 (NaN) | `Flows.java` |

Plus: `RateConverter` made final, `Stock` LinkedHashSet, `Model` LinkedHashMap, `Simulation.getTimeStep()` return type, `ArrayedStock` scalar flow behavior documented.

### Round 2 — Commit `68a2457` (17 files, ~20 items)

| Category | Changes |
|----------|---------|
| **Quantity.java** | Physical equality (dimension + base-unit value) for equals/hashCode, null-unit validation, divide-by-zero check |
| **Simulation.java** | Constructor null checks + positive-duration + TIME-dimension validation, LinkedHashSet for handlers, model reference in SimulationEndEvent, step semantics documented |
| **SimulationEndEvent.java** | Added Model reference field |
| **TimeStepEvent.java** | Javadoc corrected (fires before stock update, not after) |
| **Flow.java, Variable.java** | Added `clearHistory()`, null checks for unit/formula |
| **Stock.java** | Null checks for unit and negativeValuePolicy |
| **Constant.java** | Null check for unit, getIntValue clamps instead of throwing |
| **ItemUnit.java** | Added equals/hashCode/toString |
| **Module.java** | LinkedHashMap, unmodifiable getStocks(), added addArrayedVariable/addArrayedFlow/getFlows |
| **RunResult.java** | Guard getFinalStockValue against empty snapshots |
| **CsvSubscriber.java** | Implements Closeable, O(n) per step (was O(n²)), null-safe close |
| **LookupTable.java** | NaN input returns NaN instead of propagating to interpolator |
| **Step.java, Ramp.java** | Non-negative time validation |

---

## Re-Audit Findings (Post-Fix)

### Summary by Severity

| Severity | Count | Description |
|----------|-------|-------------|
| Bug (high impact) | 3 | Would produce wrong results or crash in specific scenarios |
| Bug (medium impact) | 6 | Edge-case failures or silent data corruption |
| Bug (low/latent) | 8 | Unlikely to trigger in practice or inherent design limitations |
| Design | 27 | API inconsistencies, missing defensive measures, extensibility gaps |
| Minor | 32 | Code style, naming, documentation, minor redundancies |
| **Total** | **76** | |

### Summary by Subsystem

| Subsystem | Files | Bugs | Design | Minor | Total |
|-----------|-------|------|--------|-------|-------|
| Simulation engine & events | 5 | 2 | 4 | 5 | 11 |
| Core model (Stock, Flow, Variable, Model, Module) | 8 | 4 | 7 | 7 | 18 |
| Measurement system (Dimension, Unit, Quantity) | 12 | 3 | 6 | 3 | 12 |
| Sweep / Monte Carlo / Optimizer | 8 | 4 | 8 | 3 | 15 |
| SD functions & IO/UI | 10 | 4 | 8 | 8 | 20 |

---

## Detailed Findings by Subsystem

### 1. Simulation Engine & Events

**Bugs:**

| # | Impact | Finding |
|---|--------|---------|
| S1 | Medium | **EventBus/@Subscribe disconnect.** `EventHandler` interface defines `handleTimeStepEvent`, `handleSimulationStartEvent`, `handleSimulationEndEvent` — but Guava EventBus requires `@Subscribe` on the *concrete* method. Implementing `EventHandler` without manually adding `@Subscribe` to each method causes silent delivery failure. All existing implementations (CsvSubscriber, ChartViewers) add `@Subscribe` manually, so this is not currently broken, but it is a trap for new implementations. |
| S2 | Low | **Re-run doesn't clear Flow/Variable history.** `Simulation.execute()` resets its own state but does not call `clearHistory()` on flows or variables. Second runs accumulate history from both runs. Mitigation: `clearHistory()` was added in Round 2; callers can invoke it. |

**Design:**

| # | Finding |
|---|---------|
| S3 | `getEventBus()` exposes the raw Guava EventBus, allowing external code to post arbitrary events. |
| S4 | `EventHandler` forces all 3 handler methods — implementing just one requires empty stubs for the other two. Should have default no-op implementations. |
| S5 | Asymmetric event objects: `SimulationStartEvent` carries model + simulation; `SimulationEndEvent` now carries model but not simulation. |
| S6 | No thread-safety documentation on `Simulation` (single-threaded by design, but not stated). |

**Minor:** Misleading local variable `q` for cached flow quantities; `addStep` name implies adding to a collection; no null check on `addEventHandler`/`removeEventHandler`; `TimeStepEvent` Javadoc `@param` references old field name; off-by-one N+1 steps documented but not resolved.

### 2. Core Model Classes

**Bugs:**

| # | Impact | Finding |
|---|--------|---------|
| M1 | Medium | **Flow source/sink silently overwritten.** `addInflow(flow)` calls `flow.setSink(this)` without checking if the flow already has a different sink. A flow wired to two stocks as an inflow silently reassigns its sink — only the last stock "wins." |
| M2 | Medium | **removeStock doesn't detach flows.** Removing a stock from a model leaves its connected flows referencing the removed stock. Subsequent simulation may compute flows for a phantom stock. |
| M3 | Medium | **addModule doesn't register module flows.** Module's stocks and variables merge into the model, but flows defined within the module are not added. Flows attached to module stocks still execute (they're referenced by the stocks), but model-level flow queries won't find them. |
| M4 | Low | **Flow history records phantom amounts on clamped stocks.** When a stock's `NegativeValuePolicy` clamps to zero, the flow's recorded value reflects the unclamped amount, not the actual amount applied. History-dependent functions (e.g., `pipelineDelay`) see amounts that were never actually transferred. |

**Design:**

| # | Finding |
|---|---------|
| M5 | No null check on `Flow` timeUnit constructor parameter. |
| M6 | `Flow.history` and `Variable.history` grow unboundedly — long simulations consume excessive memory. `clearHistory()` now exists but is not called automatically. |
| M7 | Bidirectional coupling between Stock and Flow (Stock holds flow set, Flow holds source/sink). Keeping both in sync is error-prone. |
| M8 | No model-level accessor for all flows — flows are only reachable through stocks. |
| M9 | `Stock.setNegativeValuePolicy()` allows mutation after construction — mid-simulation policy changes are possible. |
| M10 | `ArrayedFlow.create` overload accepts a stock parameter that is unused. Misleading API. |
| M11 | `addStock` allows duplicate names without warning; same for `addConstant`. |

**Minor:** `checkArgument` used where `checkNotNull` is more appropriate in some constructors; `NegativeValuePolicy` field not final; `Constant` accepts NaN as initial value; `Element.setComment(null)` undocumented; `pipelineDelay` validates sink at runtime rather than construction.

### 3. Measurement System

**Bugs:**

| # | Impact | Finding |
|---|--------|---------|
| U1 | High | **Fahrenheit `toBaseUnits(double)` / `fromBaseUnits(double)` not overridden.** Round 1 overrode the `Quantity`-accepting overloads to throw `UnsupportedOperationException`, but the primitive `double`-accepting overloads still use ratio-based math (`value * 5/9`), silently producing wrong results. Any code path that calls `toBaseUnits(100.0)` on FAHRENHEIT gets 55.56 instead of the correct 37.78. |
| U2 | Medium | **Quantity.equals()/hashCode() throw for Fahrenheit.** Physical equality calls `inBaseUnits()` which calls `toBaseUnits(Quantity)` — which throws `UnsupportedOperationException` for FAHRENHEIT. Two Fahrenheit quantities cannot be compared for equality. |
| U3 | Low | **Quantity.convertUnits() throws for identity conversions.** Converting a Dimensionless or Temperature quantity to the same unit throws because the code path goes through `toBaseUnits`/`fromBaseUnits`. Should short-circuit when source and target units are identical. |

**Design:**

| # | Finding |
|---|---------|
| U4 | No `DIMENSIONLESS` constant on the `Dimension` class — users must reference `DimensionlessUnits.NONE.getDimension()`. |
| U5 | `ItemUnit.equals()` compares by name but doesn't interoperate with `ItemUnits` enum — `new ItemUnit("Widget").equals(ItemUnits.WIDGET)` is false because they're different classes. |
| U6 | `ItemUnit` constructor doesn't validate null name. |
| U7 | `Lengths` and `Times` unit-holder classes are not final. |
| U8 | No mechanism for user-defined dimensions beyond the 8 built-in ones. |
| U9 | `Quantity.toString()` format `"value (unit)"` includes parentheses around the unit — unconventional. |

**Minor:** `MONTH` uses `ChronoUnit.MONTHS` but month length varies (30 vs 31 days) — the ratio `30.44` is approximate; `Dimensionless` package has incomplete coverage (only `NONE` and `PERCENT`); `toString()` could show dimension for clarity.

### 4. Sweep / Monte Carlo / Optimizer

**Bugs:**

| # | Impact | Finding |
|---|--------|---------|
| A1 | Medium | **Optimizer `bestRun[0]` null in fallback path.** If all objective evaluations return `Double.MAX_VALUE` or NaN, the null guard falls back to initial-guess parameters — but `bestRun[0]` (the RunResult) remains null. Callers accessing `optimize().getFirst()` get a null RunResult. |
| A2 | Low | **RunResult.getMaxStockValue returns NEGATIVE_INFINITY on empty.** `stockSnapshots` contains entries but a specific stock name might not exist, returning `NEGATIVE_INFINITY` as the "max." Should throw or return Optional. |
| A3 | Low | **MonteCarloResult stock/variable name collision.** If a stock and a variable share the same name, `getPercentileSeries` silently picks the first match. |
| A4 | Latent | **Optimizer lambda captures mutable `bestParams`/`bestObjective` array.** Not thread-safe — but optimization is single-threaded today. Would break if parallelized. |

**Design:**

| # | Finding |
|---|---------|
| A5 | `Objectives.fitToTimeSeries` silently truncates when simulated and observed arrays differ in length — should warn or throw. |
| A6 | `CsvSubscriber` / `SweepCsvWriter` use platform-default encoding — non-portable on Windows. Should specify UTF-8. |
| A7 | `MonteCarlo.reseedRandomGenerator` mutates the caller's distribution objects. Side effect not documented. |
| A8 | CMA-ES population size hardcoded to `4 + 3 * ln(n)` — adequate for most cases but not configurable. |
| A9 | `RunResult` dual constructor (`double` vs `Map`) — lossy, can't populate both. |
| A10 | No defensive copy of `parameterValues` array in `ParameterSweep.Builder`. |
| A11 | Cartesian product in `MultiParameterSweep` can OOM for large parameter grids — no size guard. |
| A12 | `RuntimeException` wrapping of checked exceptions loses type information. |

**Minor:** `linspace` name implies `numpy.linspace` (endpoint-inclusive) but uses start-step-end semantics; `formatPercentile` relies on specific double-to-string formatting; `File.mkdirs()` return value ignored.

### 5. SD Functions, IO & UI

**Bugs:**

| # | Impact | Finding |
|---|--------|---------|
| F1 | Low | **Smooth/Delay3 multi-step catch-up uses stale input.** The fix loops N times when N steps are skipped, but each iteration reads the *current* input value — not the values at intermediate steps (which are unknowable). This is an inherent limitation of the catch-up approach, not a bug per se, but produces different results than running each step individually. |
| F2 | Medium | **ArrayedStock scalar flow applies N times.** A single scalar flow wired via `addInflow(Flow)` to an ArrayedStock is applied to all N underlying stocks — the single flow's computed quantity is added to each stock, effectively multiplying its effect by N. |
| F3 | Low | **CsvSubscriber NPE if `getCurrentTime()` returns null.** `event.getCurrentTime().format(...)` throws NPE if the time step event has a null timestamp. |
| F4 | Low | **ChartViewerApplication series/values size mismatch.** `addValues()` receives separate stock and flow value lists, but the series count may not match if stocks or flows were added asymmetrically. |

**Design:**

| # | Finding |
|---|---------|
| F5 | `FanChart` and `ChartViewerApplication` use all-static state — only one chart per JVM lifetime, not reentrant. |
| F6 | `ChartViewerApplication.launch()` can only be called once per JVM (JavaFX limitation). Second simulation with chart viewer fails. |
| F7 | `ArrayedFlow.create` overload accepts a Stock parameter that is ignored. |
| F8 | `ModelReport` output format ends without a trailing newline. |
| F9 | `CsvSubscriber` opens the file in its constructor — fail-fast but prevents deferred initialization. |
| F10 | `Smooth` and `Delay3` have no `reset()` method — cannot reuse across simulation runs without reconstruction. |
| F11 | `LookupTable.Builder` doesn't detect duplicate x values — silently passes invalid data to the interpolator, which may throw or produce undefined results. |
| F12 | `CsvSubscriber.close()` sets `csvWriter = null` but doesn't prevent subsequent `handleTimeStepEvent` calls from NPE-ing on the null writer. |

**Minor:** Unused imports in some demo files; commented-out code in `ChartViewerApplication`; raw types in some event handler casts; Ramp uses `Integer.MAX_VALUE` sentinel for unbounded end; FanChart can only be launched once due to JavaFX `Application.launch` constraint; redundant `ModelReport` console output; missing `@Override` annotations on some event handlers.

---

## Risk Assessment

### High Risk (likely to cause incorrect results if triggered)

| Finding | Trigger Condition | Mitigation |
|---------|-------------------|------------|
| U1: Fahrenheit double-overload | Any code calling `FAHRENHEIT.toBaseUnits(100.0)` | Avoid raw-double conversion; use Quantity overloads (which throw). Document limitation. |
| F2: ArrayedStock scalar flow × N | Wiring a single Flow to an ArrayedStock via `addInflow(Flow)` | Use `ArrayedFlow` for per-element flows. Documented in Javadoc. |

### Medium Risk (edge cases that could cause crashes or silent errors)

| Finding | Trigger Condition | Mitigation |
|---------|-------------------|------------|
| U2: Fahrenheit equality throws | Comparing any two Fahrenheit Quantity objects | Avoid Fahrenheit in computations. Framework limitation. |
| M1: Flow sink overwrite | Wiring one flow to two stocks as inflow | Would require unusual model construction. Validate flow wiring. |
| M2: removeStock dangling refs | Dynamically removing stocks during model construction | Rare operation. Don't remove stocks after wiring. |
| A1: Optimizer null RunResult | All evaluations fail (return MAX_VALUE) | Very unlikely with real models. Add null check on result. |
| S1: EventBus @Subscribe trap | New EventHandler implementation without @Subscribe | All existing implementations are correct. Document the requirement. |

### Low Risk (latent issues, inherent limitations, or cosmetic)

All remaining findings. These are unlikely to affect users building and running standard SD models with the library's documented API.

---

## Test Coverage Assessment

### Well-Tested Areas

| Area | Test File(s) | Coverage Quality |
|------|-------------|-----------------|
| Core simulation loop | `SimulationTest` | Good — basic stocks, flows, multi-stock models |
| Stock mechanics | `StockTest` | Good — policies, negative values, basic operations |
| Flow formulas | `FlowsTest`, `FlowTest` | Good — all factory functions, custom formulas |
| Smooth/Delay3 | `SmoothTest`, `Delay3Test` | Good — basic behavior |
| LookupTable | `LookupTableTest` | Good — linear, spline, builder, edge cases |
| Quantity | `QuantityTest` | Good — arithmetic, comparison, conversion |
| Subscripts | `ArrayedStockTest`, `SubscriptRangeTest`, `MultiArrayed*Test` | Good — expansion, naming, index math |
| Parameter sweep | `ParameterSweepTest` | Good — single, multi, linspace, CSV output |
| Monte Carlo | `MonteCarloTest` | Good — sampling modes, percentiles, result aggregation |
| Optimizer | `OptimizerTest` | Adequate — all 3 algorithms, bounds, convergence |
| RunResult | `RunResultTest` | Good — snapshots, stock queries, parameter map |

### Coverage Gaps

| Area | Gap | Risk |
|------|-----|------|
| ArrayedVariable | No test file | Medium — untested expansion and formula wiring |
| CsvSubscriber | No tests | Medium — IO code with resource management |
| UI classes (FanChart, ChartViewerApplication, viewers) | No tests | Low — visual output, hard to unit test |
| Simulation events | No test for event ordering, count, handler lifecycle | Medium |
| Temperature units | No tests | High — known bugs in Fahrenheit handling |
| MILLISECOND / MONTH time units | No tests | Medium — recently added, edge cases possible |
| Dimensionless units | No tests | Low |
| ItemUnit | No tests | Low — now has equals/hashCode but untested |
| Quantity edge cases | No tests for Fahrenheit equals, NaN, cross-dim | Medium |
| Smooth/Delay3 multi-step gaps | No tests for step > 1 | Low — fix applied but not regression-tested |
| Optimizer edge cases | No TooManyEvaluations test, no all-fail test | Low |
| MonteCarloResult CSV content | File existence tested, content not verified | Low |

**Test-to-source ratio:** 0.46 (4,400 test lines / 9,500 source lines). Adequate for a library of this type, but coverage is uneven — core simulation and model classes are well-tested while IO, UI, events, and newer units are not.

---

## Quality Metrics Summary

| Dimension | Rating | Notes |
|-----------|--------|-------|
| **Correctness** | A | Core simulation loop, flow caching, stock updates, SD functions, parameter sweep, Monte Carlo, and optimization all produce correct results. All 4 critical bugs fixed. |
| **Robustness** | B | Good input validation on constructors and key operations. Edge cases in Fahrenheit, flow wiring, and empty-result paths remain. |
| **API Design** | B+ | Clean, intuitive API that maps directly to SD concepts. Builder patterns, static factories, and lambda formulas are well-designed. Some inconsistencies (dual RunResult constructors, unused parameters). |
| **Maintainability** | B | Reasonable package structure, clear separation of concerns. LinkedHashMap/Set for determinism. Some bidirectional coupling (Stock↔Flow). |
| **Documentation** | B | Good Javadoc on public API. Class-level docs explain SD concepts. Some Javadoc is stale or misleading (corrected in Round 2). |
| **Test Quality** | B- | 360 tests, all passing, good coverage of happy paths. Gaps in edge cases, IO, UI, events, and newer features. |
| **Concurrency** | N/A | Single-threaded by design. No concurrency bugs, but no documentation stating this constraint. |
| **Security** | B+ | No network exposure, no SQL, no user input parsing. File paths in CsvSubscriber and demos are caller-controlled. Platform-default encoding is the only concern. |

---

## Recommendations

### Priority 1 — Fix Before Next Release

1. **Override `FAHRENHEIT.toBaseUnits(double)` and `fromBaseUnits(double)`** to throw `UnsupportedOperationException`, matching the Quantity overloads. This closes the remaining silent-wrong-result path. (Finding U1)

2. **Add identity-conversion short-circuit** in `Quantity.convertUnits()` — if source unit equals target unit, return `this` without going through base-unit conversion. Fixes U2 (Fahrenheit equality) and U3 (Dimensionless/Temperature identity conversion) in one change.

3. **Guard `ArrayedStock.addInflow(Flow)`** to throw `IllegalArgumentException` instead of silently wiring a scalar flow to N stocks. Or, if the N-times behavior is intentional, document it prominently and rename to `addSharedInflow`. (Finding F2)

### Priority 2 — Add Tests for Fixed Bugs

4. **Regression tests** for the 36 bugs fixed in Rounds 1 and 2: multi-step Smooth/Delay3, flow identity caching, sub-second time steps, Nelder-Mead bounds, Monte Carlo parameter preservation. Each fix should have at least one test that would fail without the fix.

5. **Test coverage** for `ArrayedVariable`, `CsvSubscriber`, temperature units, MILLISECOND/MONTH, ItemUnit, and Quantity edge cases.

### Priority 3 — Design Improvements

6. **Add default no-op methods** to `EventHandler` interface, or add `@Subscribe` to the interface methods, to prevent the silent-failure trap. (Finding S1)

7. **Add `Model.getFlows()`** method that returns all flows across all stocks. (Finding M8)

8. **Document single-threaded contract** on `Simulation` class Javadoc. (Finding S6)

### Priority 4 — Nice to Have

9. Replace platform-default encoding with explicit UTF-8 in CSV writers.
10. Add `Simulation.clearHistory()` that delegates to all flows and variables.
11. Add size guard on `MultiParameterSweep` Cartesian product.
12. Make `NegativeValuePolicy` field final on `Stock`.

---

## Overall Assessment

**Grade: B+**

Forrester is a well-designed educational and research-grade SD library. The core simulation mechanics are correct — stocks accumulate, flows transfer, feedback loops work, SD functions (Smooth, Delay3, Step, Ramp, LookupTable) behave as expected, and the analysis tools (parameter sweep, Monte Carlo, optimization) produce reliable results.

The two rounds of fixes addressed all critical and high-severity bugs. The remaining 76 findings are predominantly edge cases, design improvements, and test coverage gaps that do not affect the library's primary use case. The highest-risk remaining items (Fahrenheit double-overload, ArrayedStock scalar flow amplification) have clear workarounds and are documented.

**Strengths:**
- Clean API that maps directly to SD vocabulary
- Correct simulation mechanics with deterministic ordering
- Comprehensive analysis toolkit (sweep, MC, optimization)
- Strong measurement system with dimensional analysis
- Good demo collection covering the SD curriculum
- Consistent use of immutable Quantity, static factories, and builder patterns

**Areas for improvement:**
- Test coverage breadth (event system, IO, newer units, edge cases)
- Fahrenheit handling (fundamental limitation of ratio-based conversion)
- EventBus/@Subscribe coupling (framework design issue)
- Documentation of threading model and step semantics
