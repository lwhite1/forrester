# Quality Audit Report — Rev 5

**Date:** 2026-03-09
**Auditor:** Automated deep code review (all source files read line-by-line)
**Scope:** All 5 modules — shrewd-engine, shrewd-app, shrewd-tools, shrewd-demos, shrewd-ui

---

## Executive Summary

Full codebase audit covering 135+ Java source files across all modules. The codebase is well-architected with clean separation of concerns, good use of Java 25 features (records, sealed classes, pattern matching), and solid test coverage in the engine module. Key areas for improvement are: hot-path performance in the simulation loop, a crashing bug in the UI chart viewers, time unit mismatch in the Agile demo, and code duplication in the SIR demos.

**SpotBugs:** 0 bugs (clean across all modules)
**Build + Tests:** All tests pass (clean compile, full test suite)

---

## JaCoCo Coverage Summary

| Module | Line Coverage | Branch Coverage | Assessment |
|--------|-------------|----------------|------------|
| shrewd-engine | 89% | 77% | Strong |
| shrewd-ui | 86% | 83% | Strong |
| shrewd-demos | 37% | 17% | Weak |
| shrewd-app | 41% | 34% | Weak (expected for JavaFX UI) |
| shrewd-tools | 58% | 54% | Moderate |

**Coverage weaknesses:**
- **shrewd-demos (37%)**: Most demo classes have no dedicated tests beyond smoke tests. The SIR, SoftwareProject, and Workforce demos have complex logic that is untested in isolation.
- **shrewd-app (41%)**: Many UI controllers, dialog classes, and renderers are at 0% coverage. This is expected for JavaFX code but some logic-heavy classes (e.g., `ModelEditor`, `EquationValidator`, `UndoManager`) should have higher coverage. `ModelEditor` is partially covered; many canvas controllers are not.
- **shrewd-tools (58%)**: Integration tests skip when fixture files are absent (using early return instead of `assumeTrue`), so CI may report green while tests are silently skipped.

---

## Findings by Module

### shrewd-engine (28 findings: 0 critical, 6 high, 12 medium, 10 low)

**High:**
| # | Issue | GitHub |
|---|-------|--------|
| 1 | Simulation hot-path allocates new objects every step (IdentityHashMap, Quantity) | [#276](https://github.com/Courant-Systems/shrewd/issues/276) |
| 2 | Model.addArrayedStock uses O(n) contains() on ArrayList | [#277](https://github.com/Courant-Systems/shrewd/issues/277) |
| 3 | Non-finite value handling inconsistent: Simulation graceful vs Stock.setValue throws | [#278](https://github.com/Courant-Systems/shrewd/issues/278) |

**Medium:**
| # | Issue | GitHub |
|---|-------|--------|
| 4 | CsvSubscriber.handleTimeStepEvent does not catch IOException from writeNext | [#279](https://github.com/Courant-Systems/shrewd/issues/279) |
| 5 | CsvSubscriber uses string concatenation in SLF4J logging | [#175](https://github.com/Courant-Systems/shrewd/issues/175) (existing) |
| 6 | formatDouble() duplicated in 3 files, findTopLevelComma() in 4 files | [#287](https://github.com/Courant-Systems/shrewd/issues/287) |
| 7 | IndexedValue.divide throws vs ExprCompiler returns NaN for division by zero | [#292](https://github.com/Courant-Systems/shrewd/issues/292) |
| 8 | Model.getStockValues/getVariableValues boxing on hot path | Included in [#276](https://github.com/Courant-Systems/shrewd/issues/276) |
| 9 | ExprCompiler warn-once pattern duplicated ~8 times | Code quality — low priority |
| 10 | LookupTable.Builder.sortedPoints() error message unclear for duplicate x-values | Minor UX |
| 11 | Simulation currentStep is int but totalSteps is long — fragile if MAX_STEPS raised | Defensive |
| 12 | EventHandler default no-op methods — silent failure if method name misspelled | API design |

### shrewd-app (22 findings: 0 critical, 3 high, 11 medium, 8 low)

**High:**
| # | Issue | GitHub |
|---|-------|--------|
| 1 | UndoManager race condition: rawSnapshot cleared by compression thread | Related to [#267](https://github.com/Courant-Systems/shrewd/issues/267) |
| 2 | FormContext mutable shared state with package-private fields | Design risk |
| 3 | ModelEditor.getSimulationSettings/getMetadata return mutable references | Minor if types are immutable records |

**Medium:**
| # | Issue | GitHub |
|---|-------|--------|
| 4 | ModelEditor.removeElement doesn't fire equation change events | [#285](https://github.com/Courant-Systems/shrewd/issues/285) |
| 5 | ReattachController saves undo before verifying reconnect succeeds | [#286](https://github.com/Courant-Systems/shrewd/issues/286) |
| 6 | SvgExporter default branch suppresses exhaustiveness checking | [#289](https://github.com/Courant-Systems/shrewd/issues/289) |
| 7 | O(n) element lookup by name in ModelEditor — 7 sequential list scans | Enhancement |
| 8 | FeedbackLoopRenderer text width approximation (length * 9) | Visual glitch risk |
| 9 | Bezier segment count (30) hardcoded in 3 places | Magic number |
| 10 | SvgExporter silently returns on empty diagram | UX issue |
| 11 | PropertiesPanel hardcodes "dt = 1" instead of reading actual time step | [#133](https://github.com/Courant-Systems/shrewd/issues/133) related |

**Low:**
| # | Issue | GitHub |
|---|-------|--------|
| 12 | getCldVariableByName returns null instead of Optional | [#290](https://github.com/Courant-Systems/shrewd/issues/290) |
| 13 | Duplicated CSV export pattern across 5+ result panes | [#291](https://github.com/Courant-Systems/shrewd/issues/291) |

### shrewd-tools (19 findings: 0 critical, 3 high, 8 medium, 8 low)

**High:**
| # | Issue | GitHub |
|---|-------|--------|
| 1 | System.exit() in CLI parseArgs makes error paths untestable | [#283](https://github.com/Courant-Systems/shrewd/issues/283) |
| 2 | PipelineResult record doesn't defensively copy List fields | [#284](https://github.com/Courant-Systems/shrewd/issues/284) |
| 3 | ImportPipelineCli.prompt() leaks Scanner wrapping System.in | Resource leak |

**Medium:**
| # | Issue | GitHub |
|---|-------|--------|
| 4 | toPascalCase Javadoc claims camelCase splitting but doesn't implement it | Documentation |
| 5 | toPackageSegment can produce invalid package starting with digit | Related to [#274](https://github.com/Courant-Systems/shrewd/issues/274) |
| 6 | DemoClassGenerator does not handle nested modules recursively | [#273](https://github.com/Courant-Systems/shrewd/issues/273) (existing) |
| 7 | downloadToTemp does not validate URI path before substring | NPE risk |
| 8 | PipelineResult fields lack null checks in constructor | Defensive |

### shrewd-demos (21 findings: 0 critical, 3 high, 10 medium, 8 low)

**High:**
| # | Issue | GitHub |
|---|-------|--------|
| 1 | SIR model logic duplicated across 6 demo files | [#281](https://github.com/Courant-Systems/shrewd/issues/281) |
| 2 | ThirdOrderMaterialDelayDemo Math.min is no-op for current constants | [#270](https://github.com/Courant-Systems/shrewd/issues/270) (existing) |
| 3 | FlowTimeDemo Math.toIntExact can throw ArithmeticException | Edge case |

**Medium:**
| # | Issue | GitHub |
|---|-------|--------|
| 4 | AgileSoftwareDevelopmentDemo defect creation time unit mismatch | [#282](https://github.com/Courant-Systems/shrewd/issues/282) |
| 5 | SoftwareProduction flow conservation not enforced (FCC can change mid-step) | Related to [#268](https://github.com/Courant-Systems/shrewd/issues/268) |
| 6 | Negative history index risk in FlowTimeDemo and InventoryModelDemo | Runtime error risk |
| 7 | ThirdOrderMaterialDelayDemo and TubDemo write CSV to relative path | Inconsistency |
| 8 | SIR demos use exact float equality for totalPop == 0 guard | Fragile |
| 9 | CoffeeCoolingDemo negative flow through outflow not guarded | Edge case |
| 10 | Hardcoded CSV filename collision between SirInfectiousDiseaseDemo and SalesMixDemo | Overwrites output |

### shrewd-ui (15 findings: 0 critical, 3 high, 6 medium, 6 low)

**High:**
| # | Issue | GitHub |
|---|-------|--------|
| 1 | Application.launch() in chart viewers crashes on second simulation | [#280](https://github.com/Courant-Systems/shrewd/issues/280) |
| 2 | Global mutable static state in ChartViewerApplication | Same root cause as #280 |
| 3 | FlowChartViewer doesn't defensively copy varargs array | [#288](https://github.com/Courant-Systems/shrewd/issues/288) |

**Medium:**
| # | Issue | GitHub |
|---|-------|--------|
| 4 | FanChart.show() volatile fields lack atomicity for the pair | Race condition risk |
| 5 | FanChart.drawFanChart does not null-check getPercentileSeries | NPE risk |
| 6 | ChartViewerApplication addSeries discards data without warning | Silent data loss |

---

## New Issues Created

| Issue | Title | Severity | Module |
|-------|-------|----------|--------|
| [#276](https://github.com/Courant-Systems/shrewd/issues/276) | Simulation hot-path allocations | High | engine |
| [#277](https://github.com/Courant-Systems/shrewd/issues/277) | Model.addArrayedStock O(n) contains | Low | engine |
| [#278](https://github.com/Courant-Systems/shrewd/issues/278) | Non-finite handling inconsistency | Medium | engine |
| [#279](https://github.com/Courant-Systems/shrewd/issues/279) | CsvSubscriber IOException handling | Medium | engine |
| [#280](https://github.com/Courant-Systems/shrewd/issues/280) | Application.launch crashes on second sim | High | ui |
| [#281](https://github.com/Courant-Systems/shrewd/issues/281) | SIR model code duplication (6 files) | Medium | demos |
| [#282](https://github.com/Courant-Systems/shrewd/issues/282) | Agile demo time unit mismatch | High | demos |
| [#283](https://github.com/Courant-Systems/shrewd/issues/283) | System.exit in CLI parseArgs | Medium | tools |
| [#284](https://github.com/Courant-Systems/shrewd/issues/284) | PipelineResult defensive copies | Medium | tools |
| [#285](https://github.com/Courant-Systems/shrewd/issues/285) | removeElement missing events | Medium | app |
| [#286](https://github.com/Courant-Systems/shrewd/issues/286) | ReattachController spurious undo | Medium | app |
| [#287](https://github.com/Courant-Systems/shrewd/issues/287) | Duplicated utility methods | Low | engine |
| [#288](https://github.com/Courant-Systems/shrewd/issues/288) | FlowChartViewer defensive copy | Low | ui |
| [#289](https://github.com/Courant-Systems/shrewd/issues/289) | SvgExporter default branch | Low | app |
| [#290](https://github.com/Courant-Systems/shrewd/issues/290) | getCldVariableByName null vs Optional | Low | app |
| [#291](https://github.com/Courant-Systems/shrewd/issues/291) | Duplicated CSV export pattern | Low | app |
| [#292](https://github.com/Courant-Systems/shrewd/issues/292) | Division-by-zero inconsistency | Low | engine |

---

## Overall Assessment

| Metric | Result |
|--------|--------|
| **SpotBugs** | 0 bugs |
| **Test suite** | All pass |
| **Total findings** | 105 (across all modules) |
| **Critical** | 0 |
| **High** | 18 |
| **Medium** | 47 |
| **Low** | 40 |
| **New issues created** | 17 |
| **Existing issues matched** | 7 (#133, #175, #267, #268, #270, #273, #274) |

### Top 5 Priority Items

1. **#280 — Application.launch() crash**: Prevents reuse of chart viewers across simulations. Fix is straightforward.
2. **#282 — Agile demo time unit mismatch**: Produces incorrect simulation results. Needs formula review.
3. **#276 — Hot-path allocations**: Performance bottleneck for large models. Pre-allocate maps and reduce Quantity construction.
4. **#281 — SIR duplication**: Maintenance hazard affecting 6 files. Extract shared builder.
5. **#285 — Missing equation change events**: Silent data inconsistency when elements are deleted.

---

## Post-Audit Addendum: Constant/Auxiliary Unification (2026-03-09)

A major refactoring was completed after this audit: **Constants and Auxiliaries were unified into a single element type.** This touched 144 files across all modules. Key changes:

- **Deleted**: `ConstantDef`, `Constant`, `ConstantForm`, `ConstantTest`, `ElementType.CONSTANT`
- **Unified type**: `AuxDef` with `isLiteral()` / `literalValue()` for literal-valued auxiliaries (parameters)
- **ModelDefinition**: Reduced from 14 to 13 fields (removed `constants` parameter)
- **Runtime**: `Constant` replaced by `Variable` with constant lambdas
- **UI**: Literal-valued auxiliaries show dashed borders + numeric value badge; formula auxiliaries show solid borders + "fx" badge
- **JSON**: 43 model files migrated from `"constants"` array to `"auxiliaries"`. Backward-compatible deserialization via `ModelDefinition.withMigratedConstants()`
- **All 2,020 tests pass** after the migration

**Impact on audit findings**: All findings in this report remain valid — none were specific to the old constant/auxiliary type separation. Issue **#72** (add `toBuilder()` to ModelDefinition) was bumped to `priority:critical` since manual 13-parameter constructor calls are now more prevalent and error-prone.
