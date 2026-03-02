# Code Quality Assessment — Forrester SD Library

**Date:** 2026-03-02
**Scope:** Full codebase audit after six rounds of fixes (including 8 medium-severity items), comprehensive regression testing, and the addition of the external model representation (expression AST, definition records, compiler, JSON serialization, nested modules, and dependency graph)
**Methodology:** Manual code review of all 148 source files across 25 packages, cross-referenced with 790 passing tests

---

## Executive Summary

Forrester is a **~14,900-line Java system dynamics simulation library** with 27 demo programs, a measurement system covering 8 physical dimensions, parameter sweep / Monte Carlo / optimization analysis tools, single- and multi-dimensional subscripts, an expression AST with recursive-descent parser, immutable model definition records with structural validation, a two-pass model compiler, round-trip JSON serialization, dependency graph extraction with auto-layout, nested module support, and JavaFX visualization.

Six rounds of fixes resolved **58 bugs and improvements** including 4 critical bugs, 7 high-severity bugs, 8 medium-severity items in the latest round, and 39 earlier medium/low items. The external model representation added ~3,850 lines of source and ~3,250 lines of tests across 6 new packages (38 source files, 16 test files). Two hundred eighty-one new tests were added in total.

**Overall Quality Rating: A** — Core simulation logic is correct and well-tested. Analysis tools work reliably. The definition/compilation pipeline is well-tested with 281 tests covering expression parsing, definition validation, model compilation, JSON round-trip, dependency graphs, nested modules, and regression tests for all 8 medium-severity fixes. The measurement system handles unit conversions correctly (with Fahrenheit explicitly unsupported). CSV output uses explicit UTF-8 encoding. All 790 tests pass.

---

## Codebase Profile

| Metric | Value |
|--------|-------|
| Source files (main) | 148 |
| Test files | 63 |
| Main source lines | ~14,900 |
| Test source lines | ~10,000 |
| Test-to-source ratio | 0.67 |
| Packages | 25 |
| Test count | 790 (all passing) |
| Build time | ~4 seconds |
| Dependencies | Guava 33.4, Commons Math 3, Jackson 2.x, OpenCSV, JavaFX, SLF4J |
| Java version | 17+ |

---

## Remaining Findings

After six rounds of fixes plus the addition of the external model representation, **65 findings remain** — none are medium-severity or higher. The remaining items are low-severity design concerns, latent/inherent edge cases, and minor issues that do not affect the library's primary use case.

### Summary by Severity

| Severity | Count | Description |
|----------|-------|-------------|
| Bug (low/latent) | 7 | Inherent limitations or extremely unlikely triggers |
| Design (low) | 26 | API inconsistencies, missing defensive measures, extensibility gaps |
| Minor | 32 | Code style, naming, documentation, minor redundancies |
| **Total** | **65** | |

### Summary by Subsystem

| Subsystem | Bugs | Design | Minor | Total |
|-----------|------|--------|-------|-------|
| Simulation engine & events | 0 | 2 | 4 | 6 |
| Core model (Stock, Flow, Variable, Model, Module) | 1 | 2 | 4 | 7 |
| Measurement system (Dimension, Unit, Quantity) | 0 | 6 | 3 | 9 |
| Sweep / Monte Carlo / Optimizer | 2 | 4 | 3 | 9 |
| SD functions & IO/UI | 3 | 6 | 8 | 17 |
| Expr AST, definitions, compiler, serialization, graph | 1 | 6 | 6 | 13 |
| Remaining test gaps (all low risk) | — | — | 4 | 4 |

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
| M4 | Low | **Flow history records phantom amounts on clamped stocks.** When `NegativeValuePolicy` clamps to zero, the flow's recorded value reflects the unclamped amount. |

**Design:**

| # | Finding |
|---|---------|
| M5 | No null check on `Flow` timeUnit constructor parameter. |
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
| A2 | Low | **RunResult.getMaxStockValue returns NEGATIVE_INFINITY on empty.** Missing stock name returns `NEGATIVE_INFINITY` instead of throwing. |
| A3 | Low | **MonteCarloResult stock/variable name collision.** Same-named stock and variable: `getPercentileSeries` silently picks the first match. |

**Design:**

| # | Finding |
|---|---------|
| A6 | `MonteCarlo.reseedRandomGenerator` mutates the caller's distribution objects. Side effect not documented. |
| A7 | CMA-ES population size hardcoded — adequate but not configurable. |
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

### 6. Expression AST, Definitions, Compiler, Serialization, Graph

**Bugs:**

| # | Impact | Finding |
|---|--------|---------|
| D3 | Low | **Circular module detection keyed on name, not identity.** Two structurally different modules with the same name are falsely flagged as circular. In practice, circular references through records are not constructible. |

**Design:**

| # | Finding |
|---|---------|
| D6 | Chained comparisons `a < b < c` silently produce C-like semantics (almost certainly a user error in SD context). |
| D7 | `topologicalSort()` silently drops cycle-involved nodes (documented in Javadoc). |
| D8 | `UnitRegistry` not thread-safe — concurrent `resolve()` calls can corrupt internal maps. |
| D9 | `UnitRegistry` auto-creation silently masks typos — any unknown name creates a new `ItemUnit`. |
| D10 | AND/OR compile both branches unconditionally — stateful functions (SMOOTH, DELAY3) in unreachable branches are still created. |
| D11 | `ExprDependencies` does not extract LOOKUP table names from `FunctionCall` — dependency arrows from lookup tables to formulas missing from auto-generated views. |

**Minor:** `evaluateConstant` rejects arithmetic on constants in SMOOTH/DELAY3 arguments; serialization has no depth limit for nested modules (only deserialization does); `requiredText` silently coerces non-string JSON types to strings; `ElementPlacement` type validation is case-insensitive but stores original case; round-trip imperfection for negative literals in `ExprStringifier` (documented).

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
| IndexedValue | `IndexedValueTest` | Good — factory methods, accessors, scalar/elementwise/cross-dimension broadcasting, aggregation (`sum`, `mean`, `max`, `min`, `sumOver`), chained operations, null validation, bounds checking, single-element subscripts, reversed dimension order, convenience accessors on arrayed model elements (70 tests) |
| Parameter sweep | `ParameterSweepTest` | Good — single, multi, linspace, CSV output |
| Monte Carlo | `MonteCarloTest` | Good — sampling modes, percentiles, result aggregation |
| Optimizer | `OptimizerTest` | Good — all 3 algorithms, bounds, convergence |
| CsvSubscriber | `CsvSubscriberTest` | Good — header, data rows, variables, closeable, parent dirs |
| Variable | `VariableTest`, `RegressionTest` | Good — formulas, null checks, history |
| Expression parser | `ExprParserTest` | Good — literals, references, binary/unary ops, precedence, function calls, conditionals, complex expressions, round-trip, error cases, depth limits (59 tests) |
| Expression stringifier | `ExprStringifierTest` | Good — all expression types, precedence-aware parenthesization, round-trip (20 tests) |
| Expression dependencies | `ExprDependenciesTest` | Good — all expression types, nested extraction (9 tests) |
| Model definitions | `ModelDefinitionTest`, `ModelDefinitionBuilderTest` | Good — record validation, builder fluency, immutability, null defaults (20 tests) |
| Definition validator | `DefinitionValidatorTest` | Good — duplicate names, flow references, equation parsing, module bindings (11 tests) |
| Expression compiler | `ExprCompilerTest` | Good — arithmetic, references, functions (SMOOTH, DELAY3, STEP, RAMP, LOOKUP), division edge cases, compilation failures (44 tests) |
| Model compiler | `ModelCompilerTest` | Good — SIR model, exponential growth, drain, forward references, step/ramp, lookups, negative-value policy, output bindings, simulation creation, reset/re-simulate, compilation failures, module errors (19 tests) |
| Nested modules | `NestedModuleTest` | Good — port bindings, sub-module compilation, hierarchical name resolution (6 tests) |
| JSON serialization | `JsonRoundTripTest`, `ModelDefinitionSerializerTest` | Good — round-trip for all element types, nested modules, file I/O, edge cases (13 tests) |
| Dependency graph | `DependencyGraphTest` | Good — edge extraction, cycle detection, topological sort, influences/dependencies (9 tests) |
| Auto-layout | `AutoLayoutTest` | Good — element placement, layered positioning, all element types (7 tests) |
| Connector generator | `ConnectorGeneratorTest` | Good — auto-generated influence arrows from dependency graph (5 tests) |
| View validator | `ViewValidatorTest` | Good — element references, connector endpoints, flow routes (8 tests) |
| Unit registry | `UnitRegistryTest` | Good — built-in resolution, case fallback, auto-creation, time unit resolution, cap limit (11 tests) |
| Module constants | `ModuleConstantTest` | Good — constant access, sub-module hierarchy (6 tests) |
| Audit fix regressions | `AuditFixRegressionTest` | Good — optimizer null guard, fitToTimeSeries length mismatch, LOOKUP isolation, configurable DT, model flow propagation, validator reference checks, module port units, RunResult constructors (17 tests) |

### Remaining Gaps

| Area | Gap | Risk |
|------|-----|------|
| UI classes (FanChart, ChartViewerApplication, viewers) | No tests | Low — visual output, hard to unit test |
| Simulation events | No test for event ordering or handler lifecycle | Low |
| Optimizer edge cases | No TooManyEvaluations test | Low |
| MonteCarloResult CSV content | File existence tested, content not verified | Low |

**Test-to-source ratio:** 0.67 (10,000 test lines / 14,900 source lines). Good coverage with regression tests for all major fixed bugs, comprehensive tests for the definition/compilation pipeline, and dedicated regression tests for all 8 medium-severity audit fixes.

---

## Quality Metrics Summary

| Dimension | Rating | Notes |
|-----------|--------|-------|
| **Correctness** | A | All critical and high bugs fixed with regression tests. Core simulation, SD functions, analysis tools, and the definition/compilation pipeline produce correct results. |
| **Robustness** | A | Strong input validation. Fahrenheit explicitly unsupported (throws). NaN/Infinity rejected. Scalar-flow-to-array blocked. Flow source/sink reassignment throws. Definition records validate in constructors. JSON deserialization depth-limited. |
| **API Design** | A | Clean SD-vocabulary API. Builder patterns, static factories, lambdas, sealed expression AST, immutable definition records. EventHandler has default no-ops. Smooth/Delay3 resettable. Configurable DT via `CompiledModel.setDt()`. Module port units resolved from `PortDef`. |
| **Maintainability** | A | Good package structure (25 packages). Deterministic ordering (LinkedHashMap/Set). removeStock properly detaches flows. clearHistory auto-invoked on execute(). Clean separation between definition records, compiler, and runtime model. |
| **Documentation** | A- | Good Javadoc on public API. Class-level docs explain SD concepts. Threading contract documented. |
| **Test Quality** | A | 790 tests, all passing. Regression coverage for all major bugs including 17 dedicated tests for the 8 medium-severity fixes. Comprehensive tests for the definition/compilation pipeline (281 tests across 17 test files). Remaining gaps are UI and minor edge cases only. |
| **Security** | A- | No network exposure, no SQL, no user input parsing. CSV writers use explicit UTF-8 encoding. JSON deserialization depth-limited to 50 for nested modules. Expression parser depth-limited. |

---

## Recommendations (Remaining)

All Priority 1 items have been resolved in the latest round of fixes. The remaining items are nice-to-have improvements.

### Nice to Have

1. Add thread safety to `UnitRegistry` if concurrent usage is anticipated. (Finding D8)
2. Add duplicate x-value detection to `LookupTable.Builder`. (Finding F9)
3. Make CMA-ES population size configurable. (Finding A7)
4. Consider short-circuit evaluation for AND/OR to avoid compiling unreachable stateful functions. (Finding D10)

---

## Overall Assessment

**Grade: A**

Forrester is a well-designed educational and research-grade SD library. The core simulation mechanics are correct — stocks accumulate, flows transfer, feedback loops work, SD functions (Smooth, Delay3, Step, Ramp, LookupTable) behave as expected, and the analysis tools (parameter sweep, Monte Carlo, optimization) produce reliable results. The definition/compilation pipeline — expression AST, immutable definition records, two-pass compiler, JSON serialization, dependency graph, and nested modules — is architecturally clean and well-tested.

Six rounds of fixes addressed all critical, high, and medium-severity bugs with comprehensive regression tests. The external model representation added ~3,850 source lines and 281 tests across 6 new packages. The remaining 65 findings are low-severity design concerns, inherent limitations, and minor issues — none require immediate attention.

**Strengths:**
- Clean API that maps directly to SD vocabulary
- Correct simulation mechanics with deterministic ordering
- Comprehensive analysis toolkit (sweep, MC, optimization)
- Strong measurement system with dimensional analysis and explicit Fahrenheit blocking
- Good demo collection covering the SD curriculum
- Intelligent arrays (`IndexedValue`) with automatic broadcasting, null validation, bounds checking, and allocation-free inner loops
- Consistent use of immutable Quantity, static factories, and builder patterns
- Sealed `Expr` AST with type-safe expression representation — all six variants are immutable records
- Immutable definition records (`ModelDefinition`, `StockDef`, `FlowDef`, etc.) with constructor validation — pure data, no closures
- Fluent `ModelDefinitionBuilder` and structural `DefinitionValidator`
- Two-pass `ModelCompiler` with forward-reference support via indirection holders
- Round-trip JSON serialization via `ModelDefinitionSerializer` with depth-limited deserialization
- Dependency graph extraction, auto-layout, connector generation, and view validation
- Nested module support with hierarchical `CompilationContext` scoping and `QualifiedName` resolution
- `UnitRegistry` with case-insensitive fallback and auto-creation cap
- EventHandler with default no-ops and proper `@Subscribe` annotations
- Immutable NegativeValuePolicy on Stock (final field)
- Flow source/sink reassignment validation
- removeStock properly detaches connected flows
- Simulation auto-clears history on each execute() call
- Smooth and Delay3 resettable for simulation re-runs
- CSV output with explicit UTF-8 encoding
- MultiParameterSweep size guard against OOM
- 790 tests with regression coverage for all major bug fixes, including 17 dedicated tests for the 8 medium-severity audit fixes

**Areas for improvement:**
- Bidirectional Stock↔Flow coupling
- UnitRegistry auto-creation silently masks unit name typos
- UI layer is all-static (single chart per JVM)
