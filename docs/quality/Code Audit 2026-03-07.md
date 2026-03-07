# Code Audit & Quality Assessment — 2026-03-07

## Scope

Full code audit of the Forrester System Dynamics modeling platform covering all four modules:

| Module | Source Files | Lines of Code | Test Files |
|--------|-------------|---------------|------------|
| forrester-engine | ~75 | ~9,500 | ~45 |
| forrester-app | ~70 | ~14,000 | ~15 |
| forrester-demos | ~15 | ~2,500 | ~5 |
| forrester-ui | 5 | ~400 | 0 |

**Build status:** All 540 tests pass (2 skipped). Clean compile. SpotBugs passes clean.

---

## Summary of Findings

| Severity | New Issues | Existing (Open) | Total |
|----------|-----------|-----------------|-------|
| Critical | 1 | 0 | 1 |
| High | 8 | 1 | 9 |
| Medium | 10 | 14 | 24 |
| Low | — | 6 | 6 |

All critical, high, and medium issues have been filed as GitHub issues and assigned to milestone **R1**.

---

## Critical Issues

| # | Title | Module | Impact |
|---|-------|--------|--------|
| 123 | Concurrent analysis runs can corrupt shared model state | app | Race conditions produce silently wrong simulation results |

The `AnalysisRunner` uses an unbounded thread pool with no task cancellation. Concurrent simulation/sweep/Monte Carlo runs share the same `CompiledModel` mutable state (stepHolder, stock values, Resettable formulas), leading to data corruption.

---

## High Issues

| # | Title | Module | Impact |
|---|-------|--------|--------|
| 124 | RANDOM_NORMAL not resettable | engine | Non-reproducible simulation results |
| 125 | Guava EventBus silently swallows event handler exceptions | engine | Silent data corruption if StepSyncHandler fails |
| 128 | No unsaved changes warning before New/Open/Close | app | Data loss risk for users |
| 134 | Division by zero silently returns 0 | engine | Masks modeling errors |
| 135 | Math.pow, LN, SQRT produce NaN/Infinity with no guard | engine | Crashes or silent corruption |
| 136 | PropertiesPanel duplicate polarity change handler | app | Duplicate undo entries, double mutations |
| 140 | No unsaved changes warning | app | User data loss |
| 146 | Flow value resolved in wrong time unit in expressions | engine | Wrong results when flow/sim time units differ |

---

## Medium Issues

| # | Title | Module | Impact |
|---|-------|--------|--------|
| 126 | Unbounded history accumulation (boxed Double lists) | engine | OOM on large/long simulations |
| 127 | clearHistory() misses cloud-to-cloud flows | engine | Stale data across re-runs |
| 129 | ModelDefinitionBuilder allows duplicate element names | engine | Silent overwrites in compilation |
| 130 | No file size limit on XMILE/Vensim import | engine | Memory exhaustion on large files |
| 131 | Equation rename uses string replacement, not AST | app | Stale refs with backtick-quoted names |
| 132 | ModelWindow god class (1400+ lines) | app | Maintainability/testability |
| 137 | NPV overflow on large step counts | engine | Meaningless financial results |
| 138 | Equation rename fragile (string replacement) | app | Stale references |
| 139 | No duplicate name validation in builder | engine | Silent compilation errors |
| 141 | No import file size limit | engine | DoS via large files |
| 142 | Unbounded history in Variable/Flow | engine | Memory pressure |
| 143 | clearHistory misses unconnected flows | engine | Stale history |
| 144 | ModelWindow decomposition needed | app | Maintainability |
| 145 | forrester-ui module has zero tests | ui | Untested code, CI blocking |

---

## Pre-Existing Open Issues (context)

These were already tracked before this audit:

| # | Title | Severity |
|---|-------|----------|
| 5 | Autocomplete: Enter key does not select highlighted item | High |
| 70 | ModelEditor methods return null instead of Optional | Medium |
| 67 | Extract renderer-agnostic geometry (SvgExporter duplication) | Medium |
| 68 | Extract shared ParameterRowComponent from dialogs | Medium |
| 69 | Extract shared chart/export utilities from result panes | Medium |
| 72 | Add toBuilder()/with*() methods to ModelDefinition record | Medium |
| 77 | Centralize color constants, move inline CSS to stylesheets | Medium |
| 78 | Default branches in enum switches mask future additions | Medium |
| 54 | Improve import/export test coverage | Medium |
| 29 | Example model tests lack behavioral assertions | Medium |

---

## Architecture Assessment

**Strengths:**
- Clean separation between engine (immutable definitions, compilation, simulation) and app (mutable editor, UI)
- Records used extensively for immutable domain types (ModelDefinition, StockDef, etc.)
- XXE protection in XML parsers
- No System.out/err, no swallowed exceptions, proper SLF4J logging throughout
- Good use of Preconditions for constructor validation
- Expression parser is well-structured with depth limiting (MAX_DEPTH = 200)
- Builder pattern for MonteCarlo, LookupTable
- Thread confinement documented and asserted in ModelEditor

**Weaknesses:**
- SpotBugs configured but no Checkstyle or ErrorProne yet
- Guava EventBus is a silent failure point — should be replaced with explicit listener list
- Several god classes remain: ModelWindow (1412 lines), ModelCanvas (1592 lines)
- History storage is inefficient (boxed Doubles in ArrayLists)
- No dirty tracking for unsaved changes
- Engine API allows construction of invalid models (no validation in builder/compiler)

---

## Prioritized Fix Recommendations

### P0 — Fix Before Release (blocks R1 quality bar)

1. **#123 — Concurrent analysis corruption.** Replace `newCachedThreadPool` with single-thread executor or add task cancellation. Estimated: 1-2 hours.

2. **#128/#140 — Unsaved changes warning.** Add dirty flag to ModelEditor, confirmation dialog on New/Open/Close. Estimated: 2-3 hours.

3. **#125 — EventBus exception swallowing.** Replace Guava EventBus with a simple listener loop that propagates exceptions. Estimated: 1-2 hours.

### P1 — Fix Soon (correctness issues)

4. **#134 — Division by zero.** Add configurable behavior (log warning + return 0, or return NaN). Estimated: 1 hour.

5. **#135 — NaN/Infinity guards.** Add domain checks to SQRT, LN, EXP, POWER. Estimated: 2 hours.

6. **#124 — RANDOM_NORMAL resettable.** Wrap Random in a Resettable, register it. Estimated: 30 min.

7. **#146 — Flow time unit in expressions.** Resolve flow values in simulation time unit. Estimated: 1 hour.

8. **#136 — Duplicate polarity handler.** Merge the two setOnAction calls. Estimated: 15 min.

### P2 — Fix This Release (robustness)

9. **#127/#143 — clearHistory misses flows.** One-line fix: iterate model.getFlows() directly. Estimated: 15 min.

10. **#129/#139 — Duplicate name validation.** Add duplicate check in ModelCompiler.compile(). Estimated: 1 hour.

11. **#130/#141 — Import file size limit.** Add size check before Files.readString(). Estimated: 30 min.

12. **#137 — NPV overflow.** Use incremental discounting. Estimated: 1 hour.

### P3 — Improve Over Time (quality/maintainability)

13. **#126/#142 — History memory efficiency.** Switch to primitive double[] pre-allocated to totalSteps. Estimated: 2-3 hours.

14. **#131/#138 — AST-based rename.** Parse → walk → replace → stringify. Estimated: 3-4 hours.

15. **#132/#144 — ModelWindow decomposition.** Extract MenuBarBuilder, FileController. Estimated: 4-6 hours.

16. **#145 — forrester-ui tests.** Add test directory, basic tests, headless mode. Estimated: 3-4 hours.

---

## Recommendations for Ongoing Quality

1. **Adopt mutation testing:** PIT or similar would identify the weak assertions flagged in the test audit (assertions that pass regardless of behavior).

2. **Track code coverage:** JaCoCo report would quantify the forrester-ui gap (0%) and identify other blind spots.

3. **Consider replacing Guava EventBus:** It's deprecated-in-spirit (Guava team recommends alternatives). A simple typed listener interface is safer and more debuggable.
