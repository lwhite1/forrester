# Quality Audit Report — 2026-03-10T18:30

## Scope

Full codebase audit of all 5 modules (~220 source files, ~126 test files), including:
- Deep line-by-line review of every source file via parallel agents
- SpotBugs static analysis (effort=Max, threshold=Medium)
- JaCoCo code coverage analysis
- Test quality and coverage gap analysis

## Static Analysis: SpotBugs

**Result: 0 bugs** across all 5 modules (SpotBugs 4.9.8, Java 25).

## Code Coverage: JaCoCo

| Module | Line Coverage | Branch Coverage | Notes |
|--------|-------------|-----------------|-------|
| forrester-engine | 88.3% | 76.2% | Strong coverage |
| forrester-app | 41.5% | 35.4% | 46 zero-coverage classes |
| forrester-ui | 87.0% | 83.3% | Good coverage |
| forrester-demos | 37.4% | 16.7% | Demo code, lower priority |
| forrester-tools | 58.5% | 53.6% | CLI tools |

**Weaknesses:**
- **App module** is the primary concern at 41.5% line coverage. Major gaps in: all controller classes (Drag, Marquee, Selection, Reattach, Resize, InlineEdit, ModuleNavigation, ContextMenu), all renderer classes (Connection, Selection, FeedbackLoop, ErrorIndicator, Sparkline), all form classes (ElementForm, StockForm, AuxForm, FlowForm, LookupForm), FileController, SimulationController, PropertiesPanel, all result panes, and all dialog classes.
- **Engine module** has solid coverage (88.3%) with minor gaps in event classes and ModelReport.
- **Demos module** at 37.4% is expected — demo code has smoke tests but not unit-level coverage.

## Findings Summary

### By Severity

| Severity | New Issues | Pre-existing Issues | Total |
|----------|-----------|-------------------|-------|
| CRITICAL | 1 (#363) | 4 (#264, #280, #307, #270) | 5 |
| HIGH | 10 (#364-368, #378-380, plus pre-existing) | 12 (#266, #277, #281, #282, #296, #300, #301, #303, #304, #324, #329, #333) | 22 |
| MEDIUM | 13 (#370-377, #381-384, #387) | 15+ (various) | 28+ |
| LOW | 5 (#385-386, #388-391) | 10+ (various) | 15+ |

### CRITICAL Findings

| # | Issue | Module | Status |
|---|-------|--------|--------|
| #363 | **EquationReferenceManager drops materialUnit/subscripts from FlowDef** | app | **NEW — Milestone R1** |
| #264 | SubscriptExpander produces flat union instead of Cartesian product | engine | Pre-existing |
| #280 | Application.launch() in chart viewers crashes on second simulation | ui | Pre-existing |
| #307 | FlowTimeDemo TAT stock can go negative | demos | Pre-existing |
| #270 | ThirdOrderMaterialDelayDemo Math.min no-op | demos | Pre-existing |

### New Issues Created (This Audit)

**Engine module (13 issues):**
- #364 — CompiledModel/RunResult long-to-int step truncation
- #365 — ExprCompiler hardcoded epsilon 1e-10
- #366 — VensimExprTranslator TIME shadows user variables
- #367 — DependencyGraph.dependenciesOf O(V*E) reverse lookup
- #368 — XmileImporter defaults stock init to 0 instead of initialExpression
- #369 — Simulation nanosecond step can round to zero
- #370 — Duplicated Tarjan SCC in DependencyGraph and FeedbackAnalysis
- #371 — CompositeUnit.normalize() mutates argument map
- #372 — LoopDominanceAnalysis mutable double[][] in record
- #373 — AutoLayout overlap resolution limited to 3 passes
- #374 — SensitivitySummary.formatValue unsafe double-to-long cast
- #375 — VensimImporter merges INTEG rate terms into single net flow
- #376 — ModelReport visited set copy semantics
- #377 — XmileExprTranslator = to == misfire on assignment expressions

**App module (8 issues):**
- #363 — EquationReferenceManager drops materialUnit/subscripts (CRITICAL, R1)
- #378 — DashboardPanel dominanceTab stale reference
- #379 — ValidationDialog singleton leaks callbacks across windows
- #380 — ModelCanvas rebuilds toModelDefinition 2-3x per mutation
- #381 — SvgExporter does not escape XML special characters
- #382 — InputDispatcher no mouse-exit handler
- #383 — navigateToDepth creates redundant undo entries
- #384 — Copy/paste places at fixed origin when panned

**Test quality (5 issues):**
- #385 — SimulationResultPane truncates fractional time steps
- #386 — Duplicate ZOOM_FACTOR constant
- #387 — ImportPipelineTest uses silent return instead of assumeThat
- #388 — ChartViewerApplicationTest thread test doesn't read
- #389 — Viewer tests only assert no exception
- #390 — Demo tests use JUnit assertTrue instead of AssertJ
- #391 — CanvasRendererTest misnamed (tests FlowGeometry)

## Noteworthy Pre-existing Open Issues

Key issues from previous audits that remain open and should be prioritized:

- **#264** (CRITICAL) — SubscriptExpander Cartesian product broken
- **#300** — Delay3/Smooth/Trend/Forecast catch-up loop uses stale input
- **#304** — ModelEditor.toModelDefinition drops subscript definitions
- **#296** — ExprCompiler uppercase breaks mixed-case lookup tables
- **#266** — addConnectionReference substring match (confirmed again in this audit)
- **#277** — Model.addArrayedStock O(n) contains on ArrayList
- **#324** — VensimExporter ignores initialExpression

## Recommendations

1. **Fix #363 immediately** — single-line fix prevents silent data corruption of material units and subscripts
2. **Address #264** — multi-dimensional subscripts are fundamentally broken; infrastructure exists
3. **Improve app module coverage** — prioritize FileController and SimulationController (data loss risk)
4. **Consolidate duplicated patterns** — Tarjan SCC (#370), formatDouble/findTopLevelComma (#287), CSV export (#291)
5. **Set up CI quality gates** (#210) — enforce coverage thresholds and checkstyle project-wide

## Test Quality Assessment

Overall test quality is **HIGH**. The engine has excellent coverage with thorough edge case testing. The app module has good logic coverage but significant gaps in UI controllers, renderers, and forms. No flaky test patterns were found — TestFX tests use proper async waiting throughout.
