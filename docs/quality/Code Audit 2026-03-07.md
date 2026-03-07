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

| Severity | Open | Fixed | Total |
|----------|------|-------|-------|
| Critical | 0 | 1 | 1 |
| High | 5 | 4 | 9 |
| Medium | 10 | 0 | 10 |
| Low | — | — | 6 |

All open issues have been filed as GitHub issues and assigned to milestone **R1**.

---

## High Issues

| # | Title | Module | Impact |
|---|-------|--------|--------|
| 124 | RANDOM_NORMAL not resettable | engine | Non-reproducible simulation results |
| 134 | Division by zero silently returns 0 | engine | Masks modeling errors |
| 135 | Math.pow, LN, SQRT produce NaN/Infinity with no guard | engine | Crashes or silent corruption |
| 136 | PropertiesPanel duplicate polarity change handler | app | Duplicate undo entries, double mutations |
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

### P1 — Fix Soon (correctness issues)

1. **#134 — Division by zero.** Add configurable behavior (log warning + return 0, or return NaN).

2. **#135 — NaN/Infinity guards.** Add domain checks to SQRT, LN, EXP, POWER.

3. **#124 — RANDOM_NORMAL resettable.** Wrap Random in a Resettable, register it.

4. **#146 — Flow time unit in expressions.** Resolve flow values in simulation time unit.

5. **#136 — Duplicate polarity handler.** Merge the two setOnAction calls.

### P2 — Fix This Release (robustness)

6. **#127/#143 — clearHistory misses flows.** One-line fix: iterate model.getFlows() directly.

7. **#129/#139 — Duplicate name validation.** Add duplicate check in ModelCompiler.compile().

8. **#130/#141 — Import file size limit.** Add size check before Files.readString().

9. **#137 — NPV overflow.** Use incremental discounting.

### P3 — Improve Over Time (quality/maintainability)

10. **#126/#142 — History memory efficiency.** Switch to primitive double[] pre-allocated to totalSteps.

11. **#131/#138 — AST-based rename.** Parse → walk → replace → stringify.

12. **#132/#144 — ModelWindow decomposition.** Extract MenuBarBuilder, FileController.

13. **#145 — forrester-ui tests.** Add test directory, basic tests, headless mode.

---

## Recommendations for Ongoing Quality

1. **Adopt mutation testing:** PIT or similar would identify the weak assertions flagged in the test audit (assertions that pass regardless of behavior).

2. **Track code coverage:** JaCoCo report would quantify the forrester-ui gap (0%) and identify other blind spots.

3. **Consider replacing Guava EventBus:** It's deprecated-in-spirit (Guava team recommends alternatives). A simple typed listener interface is safer and more debuggable.
