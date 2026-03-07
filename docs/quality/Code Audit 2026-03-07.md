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
| High | 0 | 9 | 9 |
| Medium | 8 | 2 | 10 |
| Low | — | — | 6 |

All open issues have been filed as GitHub issues and assigned to milestone **R1**.

---

## Medium Issues

| # | Title | Module | Impact |
|---|-------|--------|--------|
| 127 | clearHistory() misses cloud-to-cloud flows | engine | Stale data across re-runs |
| 129 | ModelDefinitionBuilder allows duplicate element names | engine | Silent overwrites in compilation |
| 130 | No file size limit on XMILE/Vensim import | engine | Memory exhaustion on large files |
| 131 | Equation rename uses string replacement, not AST | app | Stale refs with backtick-quoted names |
| 132 | ModelWindow god class (1400+ lines) | app | Maintainability/testability |
| 137 | NPV overflow on large step counts | engine | Meaningless financial results |
| 138 | Equation rename fragile (string replacement) | app | Stale references |
| 139 | No duplicate name validation in builder | engine | Silent compilation errors |
| 141 | No import file size limit | engine | DoS via large files |
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
- Several god classes remain: ModelWindow (1412 lines), ModelCanvas (1592 lines)
- Engine API allows construction of invalid models (no validation in builder/compiler)

---

## Prioritized Fix Recommendations

### P2 — Fix This Release (robustness)

6. **#127/#143 — clearHistory misses flows.** One-line fix: iterate model.getFlows() directly.

7. **#129/#139 — Duplicate name validation.** Add duplicate check in ModelCompiler.compile().

8. **#130/#141 — Import file size limit.** Add size check before Files.readString().

9. **#137 — NPV overflow.** Use incremental discounting.

### P3 — Improve Over Time (quality/maintainability)

10. **#131/#138 — AST-based rename.** Parse → walk → replace → stringify.

11. **#132/#144 — ModelWindow decomposition.** Extract MenuBarBuilder, FileController.

12. **#145 — forrester-ui tests.** Add test directory, basic tests, headless mode.

---

## Recommendations for Ongoing Quality

1. **Adopt mutation testing:** PIT or similar would identify the weak assertions flagged in the test audit (assertions that pass regardless of behavior).

2. **Track code coverage:** JaCoCo report would quantify the forrester-ui gap (0%) and identify other blind spots.
