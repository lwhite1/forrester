# Code Audit Report

**Date:** 2026-03-04
**Scope:** All modules (forrester-engine, forrester-app, forrester-ui, forrester-demos)

## Executive Summary

The Forrester codebase is well-structured with strong architecture, excellent documentation, and consistent naming conventions. The engine module is particularly mature with good API design, immutability, and security hardening. The main areas for improvement are: memory leaks from unremoved listeners in the app module, low test coverage in the UI/demo modules, and static state coupling in the chart viewer.

### Overall Ratings

| Module | Architecture | Code Quality | Test Coverage | Documentation | Issues Filed |
|--------|-------------|-------------|---------------|---------------|-------------|
| forrester-engine | Excellent | Excellent | Good | Excellent | #40, #41, #42 |
| forrester-app | Good | Good | Moderate | Good | #43 |
| forrester-ui | Fair | Fair | None | Good | #45 |
| forrester-demos | Good | Good | Low | Excellent | #44 |
| Cross-module | — | — | — | — | #46 |

---

## Module: forrester-engine

### Strengths

- **Immutability:** Consistent use of records, `List.copyOf()`, and `Collections.unmodifiable*()` throughout model definitions
- **Builder pattern:** Clean builder APIs for ModelDefinition, ParameterSweep, MonteCarlo, Optimizer, LookupTable
- **Security:** XXE prevention in XMILE parser, expression parser depth limit (MAX_DEPTH = 200)
- **Documentation:** Comprehensive Javadoc with usage examples on all public APIs; `package-info.java` files in every package
- **Import/Export:** Robust Vensim and XMILE support with 140+ tests and expression translation
- **Dependency usage:** All dependencies (Guava, Commons Math, Jackson, OpenCSV) are justified and actively used

### Issues Found

| Issue | Severity | GitHub |
|-------|----------|--------|
| `mkdirs()` return value unchecked in CsvSubscriber, MonteCarloResult, SweepCsvWriter | High | [#40](https://github.com/lwhite1/forrester/issues/40) |
| ArrayedVariable throws confusing ArrayIndexOutOfBoundsException on unknown label | High | [#41](https://github.com/lwhite1/forrester/issues/41) |
| Null returns instead of Optional in CompilationContext and Model lookup methods | Medium | [#42](https://github.com/lwhite1/forrester/issues/42) |
| Simulation step calculation may accumulate floating-point drift over many steps | Low | — |
| ParameterSweep.linspace() boxes through ArrayList unnecessarily | Low | — |

### Test Coverage

Well-covered: model compilation, Vensim/XMILE import/export, measures/quantities, sweep/optimization, model elements. Minor gaps in CompilationContext (tested only indirectly) and edge cases for NaN/Infinity in stock values.

---

## Module: forrester-app

### Strengths

- **MVC separation:** Clear split between model (ModelEditor, CanvasState), view (CanvasRenderer, PropertiesPanel), and controllers (DragController, FlowCreationController, etc.)
- **Thread safety:** All background tasks use `javafx.concurrent.Task` with proper `onSucceeded`/`onFailed` handlers; `Platform.runLater()` used correctly in StatusBar and ActivityLogPanel
- **Resource management:** All file I/O uses try-with-resources; no resource leaks detected
- **Undo/redo:** Clean implementation with immutable snapshots
- **Testing infrastructure:** 50 new TestFX tests added (this session) plus 351 existing JUnit tests

### Issues Found

| Issue | Severity | GitHub |
|-------|----------|--------|
| Memory leaks: unremoved property listeners in ModelCanvas, FormContext, LookupForm | High | [#43](https://github.com/lwhite1/forrester/issues/43) |
| logListener not removed on window close in ModelWindow | Medium | [#43](https://github.com/lwhite1/forrester/issues/43) |
| Broad exception catching in buildExamplesMenu (catches Exception, should be specific) | Medium | — |
| NumberFormatException silently caught in StockForm (no logging) | Low | — |

### Test Coverage

- **Tested (21 classes):** CopyPasteController, CanvasState, UndoManager, HitTester, ModelEditor, NavigationStack, SimulationRunner, FlowCreationController, and now CanvasToolBar, StatusBar, BreadcrumbBar, SimulationSettingsDialog, ModelWindow (via TestFX)
- **Untested (~47 classes):** Form classes (StockForm, FlowForm, AuxForm, ConstantForm, LookupForm), remaining dialog classes, rendering classes, DragController, ResizeController, InlineEditController
- **Effective class coverage:** ~31%

---

## Module: forrester-ui

### Strengths

- **Focused purpose:** 4 classes dedicated to chart visualization
- **Good package docs:** Clear explanation of usage, including headless alternative (CsvSubscriber)
- **FanChart design:** Clean factory method pattern with encapsulated Application lifecycle

### Issues Found

| Issue | Severity | GitHub |
|-------|----------|--------|
| ChartViewerApplication: excessive static mutable state, uninitialized `series` field, raw types, hardcoded output path | High | [#45](https://github.com/lwhite1/forrester/issues/45) |
| Cannot display multiple charts simultaneously due to static state | Medium | [#45](https://github.com/lwhite1/forrester/issues/45) |
| Parameter spacing inconsistency (`List<Double>variableValues`) | Low | — |

### Test Coverage

**Zero tests.** No test files exist for any of the 4 visualization classes.

---

## Module: forrester-demos

### Strengths

- **Breadth:** Covers system archetypes (exponential growth, negative feedback, goal-seeking), epidemiology (SIR variants, Monte Carlo, calibration), ecology (predator-prey), economics (sales mix, inventory), and software project management (waterfall, agile)
- **Documentation:** Outstanding model descriptions with expected behavior narratives, phase transitions, and parameter interpretations
- **Waterfall subsystem:** Exemplary modular design with Workforce, StaffAllocation, SoftwareProduction as separate, testable classes with 22 tests

### Issues Found

| Issue | Severity | GitHub |
|-------|----------|--------|
| Division by zero in SirInfectiousDiseaseDemo and SalesMixDemo | High | [#44](https://github.com/lwhite1/forrester/issues/44) |
| 19 of 21 basic demos have no tests | Medium | [#46](https://github.com/lwhite1/forrester/issues/46) |
| Demos tightly coupled to forrester-ui (prevents headless use) | Medium | — |
| 21 demo files at package root instead of organized by category | Low | — |

### Test Coverage

- **Tested:** Waterfall subsystem (4 test classes, 22 tests), ModelReport (2 tests)
- **Untested:** All 21 basic demo models (0 tests)
- **Effective class coverage:** ~19%

---

## Cross-Cutting Concerns

### Security

No vulnerabilities detected. XXE prevention is properly configured in XMILE parsing. Expression parser has depth limits. No command injection vectors. No hardcoded secrets.

### Dependency Health

All dependencies are justified and actively used. Versions are managed in the parent POM. No known vulnerability concerns with current versions (Guava 33.4, Jackson 2.17, Commons Math 3.6.1).

### Architecture

Clean layered architecture: engine (no UI deps) -> ui (visualization) -> app (desktop editor). Demos depend on engine + ui. No circular dependencies between modules.

---

## New GitHub Issues Created

| # | Title | Module | Severity |
|---|-------|--------|----------|
| [#40](https://github.com/lwhite1/forrester/issues/40) | Unchecked mkdirs() return value in file writers | engine | High |
| [#41](https://github.com/lwhite1/forrester/issues/41) | ArrayedVariable throws confusing error on unknown label | engine | High |
| [#42](https://github.com/lwhite1/forrester/issues/42) | Use Optional instead of null returns in CompilationContext and Model | engine | Medium |
| [#43](https://github.com/lwhite1/forrester/issues/43) | Memory leaks from unremoved property listeners in UI components | app | High |
| [#44](https://github.com/lwhite1/forrester/issues/44) | Division by zero in SIR and SalesMix demos | demos | High |
| [#45](https://github.com/lwhite1/forrester/issues/45) | ChartViewerApplication uses excessive static mutable state | ui | High |
| [#46](https://github.com/lwhite1/forrester/issues/46) | Expand test coverage for UI forms, dialogs, and demo models | all | Medium |

---

## Priority Recommendations

### Immediate (High-severity bugs)

1. Fix memory leaks in ModelCanvas, FormContext, LookupForm (#43)
2. Add mkdirs() return value checks (#40)
3. Add bounds checking in ArrayedVariable (#41)
4. Guard against division by zero in demos (#44)

### Short-term (Quality improvements)

5. Refactor ChartViewerApplication static state (#45)
6. Replace null returns with Optional in engine APIs (#42)
7. Add smoke tests for all demo models (#46)
8. Add TestFX tests for remaining dialog classes

### Long-term (Strategic)

9. Organize demo package by category (epidemiology, economics, etc.)
10. Decouple demos from UI dependency for headless use
11. Expand form/controller test coverage in app module
12. Add accessibility labels to UI components
