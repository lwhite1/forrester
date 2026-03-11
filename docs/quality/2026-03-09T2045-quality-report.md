# Quality Audit Report — Rev 6

**Date:** 2026-03-09
**Auditor:** Automated deep code review (all source files read line-by-line)
**Scope:** All 5 modules — shrewd-engine, shrewd-app, shrewd-tools, shrewd-demos, shrewd-ui
**Context:** Post constant/auxiliary unification (commit f37dd4b)

---

## Executive Summary

Full codebase audit covering 135+ Java source files across all modules, performed after the
constant/auxiliary unification migration. Eight parallel deep-review agents (4 foreground + 4
background) read every source file line-by-line. SpotBugs, JaCoCo, and full test suite were
run independently.

Five critical issues identified, primarily UX/cognitive barriers from the SD education
literature (unexplained cloud metaphor, loop filtering, loop narratives, loop dominance
analysis) plus the long-standing ModelDefinition builder gap (#72). Nine high-severity bugs
in the expression compiler, simulation engine, and metadata handling. Forty-five GitHub
issues created (#72 bumped, #294–#339).

**SpotBugs:** 0 bugs (clean across all modules)
**Build + Tests:** All 2,020 tests pass (0 failures, 2 skipped)

---

## JaCoCo Coverage Summary

| Module | Instruction Coverage | Branch Coverage | Assessment |
|--------|---------------------|----------------|------------|
| shrewd-engine | 89% | 77% | Strong |
| shrewd-ui | 90% | 83% | Strong |
| shrewd-demos | 38% | 17% | Weak |
| shrewd-app | 41% | 35% | Weak (expected for JavaFX UI) |
| shrewd-tools | 62% | 54% | Moderate |

Coverage is essentially unchanged from Rev 5. The constant/auxiliary unification did not
significantly alter coverage percentages since tests were migrated 1:1.

---

## R1 — Critical Findings

| # | Issue | Module | GitHub |
|---|-------|--------|--------|
| 1 | ModelDefinition record needs toBuilder() — manual 13-parameter constructor calls are error-prone and prevalent | engine | [#72](https://github.com/Courant-Systems/shrewd/issues/72) |
| 2 | Cloud source/sink symbol unexplained — no tooltip or legend for new users | app | [#334](https://github.com/Courant-Systems/shrewd/issues/334) |
| 3 | Add loop type filtering to LoopNavigatorBar (show only R or B loops) | app | [#335](https://github.com/Courant-Systems/shrewd/issues/335) |
| 4 | Generate behavioral loop narratives instead of mechanical variable chains | app | [#336](https://github.com/Courant-Systems/shrewd/issues/336) |
| 5 | No way to identify which feedback loop is dominant at a given time step | app | [#337](https://github.com/Courant-Systems/shrewd/issues/337) |

---

## R2 — High and Medium Findings

### High

| # | Issue | Module | GitHub |
|---|-------|--------|--------|
| 1 | DELAY_FIXED compilation crashes when delay time rounds to zero | engine | [#294](https://github.com/Courant-Systems/shrewd/issues/294) |
| 2 | ExprCompiler.evaluateConstant rejects binary expressions — SMOOTH(x, delay/2) fails | engine | [#295](https://github.com/Courant-Systems/shrewd/issues/295) |
| 3 | ExprCompiler uppercase function name breaks lookup table resolution for mixed-case tables | engine | [#296](https://github.com/Courant-Systems/shrewd/issues/296) |
| 4 | ExprDependencies misses lookup table deps in table(input) syntax | engine | [#297](https://github.com/Courant-Systems/shrewd/issues/297) |
| 5 | SimulationRunner.run drops ModelMetadata from definition | app | [#298](https://github.com/Courant-Systems/shrewd/issues/298) |
| 6 | ChartViewerApplication static series list accumulates data across simulations | ui | [#299](https://github.com/Courant-Systems/shrewd/issues/299) |
| 7 | Simulation step counter is int but totalSteps is long — infinite loop for small DT | engine | [#320](https://github.com/Courant-Systems/shrewd/issues/320) |
| 8 | ExprCompiler conditional does not evaluate untaken branch — stateful SD functions go stale | engine | [#321](https://github.com/Courant-Systems/shrewd/issues/321) |
| 9 | Model.addModule silently swallows module name collisions | engine | [#322](https://github.com/Courant-Systems/shrewd/issues/322) |

### Medium

| # | Issue | Module | GitHub |
|---|-------|--------|--------|
| 10 | Delay3/Smooth/Trend/Forecast catch-up loop uses stale input value | engine | [#300](https://github.com/Courant-Systems/shrewd/issues/300) |
| 11 | Model.addStock accepts duplicate stock names silently | engine | [#301](https://github.com/Courant-Systems/shrewd/issues/301) |
| 12 | MonteCarloResult constructor does not defensively copy results list | engine | [#302](https://github.com/Courant-Systems/shrewd/issues/302) |
| 13 | XmileExprTranslator Time pattern matches user variable names | engine | [#303](https://github.com/Courant-Systems/shrewd/issues/303) |
| 14 | ModelEditor.toModelDefinition drops subscript definitions on save | app | [#304](https://github.com/Courant-Systems/shrewd/issues/304) |
| 15 | CopyPasteController EQUATION_KEYWORDS missing PI and E | app | [#305](https://github.com/Courant-Systems/shrewd/issues/305) |
| 16 | SoftwareProduction split flows can violate conservation under Euler | demos | [#306](https://github.com/Courant-Systems/shrewd/issues/306) |
| 17 | FlowTimeDemo TAT stock can go negative, causing invalid delay | demos | [#307](https://github.com/Courant-Systems/shrewd/issues/307) |
| 18 | DefinitionValidator.validateStructure fragile string prefix filter | engine | [#308](https://github.com/Courant-Systems/shrewd/issues/308) |
| 19 | DemoClassGenerator produces invalid Java identifiers for digit-starting names | tools | [#309](https://github.com/Courant-Systems/shrewd/issues/309) |
| 20 | FanChart calls getPercentileSeries 7 times without caching | ui | [#310](https://github.com/Courant-Systems/shrewd/issues/310) |
| 21 | ElementRenderer.truncate creates new Text node per call | app | [#311](https://github.com/Courant-Systems/shrewd/issues/311) |
| 22 | CanvasRenderer.fireStatusChanged on every redraw | app | [#312](https://github.com/Courant-Systems/shrewd/issues/312) |
| 23 | SensitivitySummary.formatPercentage no bounds validation | engine | [#313](https://github.com/Courant-Systems/shrewd/issues/313) |
| 24 | ImportPipelineCli.prompt leaks Scanner on each call | tools | [#314](https://github.com/Courant-Systems/shrewd/issues/314) |
| 25 | CompiledModel.reset() does not clear flow/variable history | engine | [#323](https://github.com/Courant-Systems/shrewd/issues/323) |
| 26 | VensimExporter ignores initialExpression on stock export | engine | [#324](https://github.com/Courant-Systems/shrewd/issues/324) |
| 27 | DefinitionValidator duplicate name check is case-sensitive | engine | [#325](https://github.com/Courant-Systems/shrewd/issues/325) |
| 28 | VensimImporter subscript labels can collide with variable names | engine | [#326](https://github.com/Courant-Systems/shrewd/issues/326) |
| 29 | Optimizer returns uninitialized result on early exception | engine | [#327](https://github.com/Courant-Systems/shrewd/issues/327) |
| 30 | SensitivitySummary array access has no bounds check | engine | [#328](https://github.com/Courant-Systems/shrewd/issues/328) |
| 31 | AuxDef.isLiteral() re-parses equation on every call | engine | [#329](https://github.com/Courant-Systems/shrewd/issues/329) |
| 32 | DemoClassGenerator does not escape metadata in Javadoc | tools | [#330](https://github.com/Courant-Systems/shrewd/issues/330) |
| 33 | FormContext.commitRename does not push undo state | app | [#331](https://github.com/Courant-Systems/shrewd/issues/331) |
| 34 | SalesMixDemo totalSales variable not registered in model | demos | [#332](https://github.com/Courant-Systems/shrewd/issues/332) |
| 35 | BatchImportCli uses System.exit() in parseArgs | tools | [#333](https://github.com/Courant-Systems/shrewd/issues/333) |

---

## Low Findings

| # | Issue | Module | GitHub |
|---|-------|--------|--------|
| 36 | package-info.java references deleted Constant class | engine | [#315](https://github.com/Courant-Systems/shrewd/issues/315) |
| 37 | QuickstartDialog shows wrong keyboard shortcut (9 vs 8) | app | [#316](https://github.com/Courant-Systems/shrewd/issues/316) |
| 38 | SvgExporter renders loop edges as straight lines instead of curves | app | [#317](https://github.com/Courant-Systems/shrewd/issues/317) |
| 39 | PropertiesPanel name/comment changes don't set dirty flag or support undo | app | [#318](https://github.com/Courant-Systems/shrewd/issues/318) |
| 40 | Workforce communicationOverhead negative for fractional team sizes | demos | [#319](https://github.com/Courant-Systems/shrewd/issues/319) |

---

## Additional Observations (not filed as issues)

- **ExprCompiler.compileLookup** has redundant underscore-to-space fallback logic (context already handles it internally). Confusing but not incorrect.
- **Optimizer** uses raw generic `Map[]` instead of `@SuppressWarnings("unchecked") Map<String, Double>[]`. Produces compiler warning but is functionally harmless.
- **EquationAutoComplete** suggestion list is never invalidated after model edits during an editing session. Low impact since editors are short-lived.
- **Clipboard** is documented as cross-window safe but uses unsynchronized ArrayLists. Safe in practice because JavaFX is single-threaded.
- **ModelCanvas.toModelDefinition()** inside modules creates intermediate ModelEditors with O(n^2) serialization cost for deeply nested modules.
- **UndoManager.decompress()** has a theoretical TOCTOU race on `rawSnapshot` volatile field. The risk window is tiny under normal conditions.
- **Flow/Variable history** grows unbounded for long simulations. `clearHistory()` exists but requires explicit caller management.
- **XmileImporter** silently defaults non-numeric stock initials to 0 without logging a warning.
- **XmileExporter/ModelDefinitionSerializer** lack serialization depth guards (deserialization is guarded but serialization is not).
- **InlineEditor.commit()** has fragile reentrant call ordering that could fire multiple commits on focus loss during close.
- **MonteCarloResultPane** catches only `UncheckedIOException` — other exceptions from CSV export would propagate uncaught.
- **LookupForm.commitDataPoint** silently reverts non-monotonic X values without user feedback.

---

## New Issues Created

| Issue | Title | Severity |
|-------|-------|----------|
| [#294](https://github.com/Courant-Systems/shrewd/issues/294) | DELAY_FIXED compilation crashes on zero delay | High |
| [#295](https://github.com/Courant-Systems/shrewd/issues/295) | evaluateConstant rejects binary expressions | High |
| [#296](https://github.com/Courant-Systems/shrewd/issues/296) | Uppercase function name breaks lookup tables | High |
| [#297](https://github.com/Courant-Systems/shrewd/issues/297) | ExprDependencies misses lookup table deps | High |
| [#298](https://github.com/Courant-Systems/shrewd/issues/298) | SimulationRunner drops metadata | High |
| [#299](https://github.com/Courant-Systems/shrewd/issues/299) | ChartViewer static data accumulation | High |
| [#320](https://github.com/Courant-Systems/shrewd/issues/320) | Simulation step counter int overflow | High |
| [#321](https://github.com/Courant-Systems/shrewd/issues/321) | Conditional branch skips stateful SD functions | High |
| [#322](https://github.com/Courant-Systems/shrewd/issues/322) | Module name collisions silently swallowed | High |
| [#300](https://github.com/Courant-Systems/shrewd/issues/300) | Catch-up loop uses stale input | Medium |
| [#301](https://github.com/Courant-Systems/shrewd/issues/301) | Duplicate stock names accepted | Medium |
| [#302](https://github.com/Courant-Systems/shrewd/issues/302) | MonteCarloResult no defensive copy | Medium |
| [#303](https://github.com/Courant-Systems/shrewd/issues/303) | XmileExprTranslator Time pattern | Medium |
| [#304](https://github.com/Courant-Systems/shrewd/issues/304) | ModelEditor drops subscripts | Medium |
| [#305](https://github.com/Courant-Systems/shrewd/issues/305) | CopyPaste missing PI/E keywords | Medium |
| [#306](https://github.com/Courant-Systems/shrewd/issues/306) | SoftwareProduction flow conservation | Medium |
| [#307](https://github.com/Courant-Systems/shrewd/issues/307) | FlowTimeDemo negative TAT | Medium |
| [#308](https://github.com/Courant-Systems/shrewd/issues/308) | DefinitionValidator fragile filter | Medium |
| [#309](https://github.com/Courant-Systems/shrewd/issues/309) | DemoClassGenerator digit identifier | Medium |
| [#310](https://github.com/Courant-Systems/shrewd/issues/310) | FanChart redundant percentile calls | Medium |
| [#311](https://github.com/Courant-Systems/shrewd/issues/311) | ElementRenderer Text node churn | Medium |
| [#312](https://github.com/Courant-Systems/shrewd/issues/312) | fireStatusChanged on every redraw | Medium |
| [#313](https://github.com/Courant-Systems/shrewd/issues/313) | formatPercentage no bounds check | Medium |
| [#314](https://github.com/Courant-Systems/shrewd/issues/314) | ImportPipelineCli Scanner leak | Medium |
| [#323](https://github.com/Courant-Systems/shrewd/issues/323) | CompiledModel.reset stale history | Medium |
| [#324](https://github.com/Courant-Systems/shrewd/issues/324) | VensimExporter drops initialExpression | Medium |
| [#325](https://github.com/Courant-Systems/shrewd/issues/325) | Case-sensitive duplicate name check | Medium |
| [#326](https://github.com/Courant-Systems/shrewd/issues/326) | Subscript/variable namespace collision | Medium |
| [#327](https://github.com/Courant-Systems/shrewd/issues/327) | Optimizer uninitialized result | Medium |
| [#328](https://github.com/Courant-Systems/shrewd/issues/328) | SensitivitySummary array bounds | Medium |
| [#329](https://github.com/Courant-Systems/shrewd/issues/329) | AuxDef.isLiteral re-parses every call | Medium |
| [#330](https://github.com/Courant-Systems/shrewd/issues/330) | DemoClassGenerator unescaped Javadoc | Medium |
| [#331](https://github.com/Courant-Systems/shrewd/issues/331) | FormContext.commitRename no undo | Medium |
| [#332](https://github.com/Courant-Systems/shrewd/issues/332) | SalesMixDemo totalSales not registered | Medium |
| [#333](https://github.com/Courant-Systems/shrewd/issues/333) | BatchImportCli System.exit in parseArgs | Medium |
| [#315](https://github.com/Courant-Systems/shrewd/issues/315) | Stale Constant class reference in Javadoc | Low |
| [#316](https://github.com/Courant-Systems/shrewd/issues/316) | Wrong keyboard shortcut in quickstart | Low |
| [#317](https://github.com/Courant-Systems/shrewd/issues/317) | SVG loop edges straight vs curved | Low |
| [#318](https://github.com/Courant-Systems/shrewd/issues/318) | Model name/comment no dirty flag | Low |
| [#319](https://github.com/Courant-Systems/shrewd/issues/319) | Workforce negative communication overhead | Low |
| [#334](https://github.com/Courant-Systems/shrewd/issues/334) | Cloud source/sink symbol unexplained | Critical |
| [#335](https://github.com/Courant-Systems/shrewd/issues/335) | Add loop type filtering to LoopNavigatorBar | Critical |
| [#336](https://github.com/Courant-Systems/shrewd/issues/336) | Generate behavioral loop narratives | Critical |
| [#337](https://github.com/Courant-Systems/shrewd/issues/337) | No way to identify dominant feedback loop | Critical |
| [#338](https://github.com/Courant-Systems/shrewd/issues/338) | CLD variables always show yellow warning border | High |
| [#339](https://github.com/Courant-Systems/shrewd/issues/339) | Show validation issue details in info pane, status bar, View menu | High |

---

## Overall Assessment

| Metric | Result |
|--------|--------|
| **SpotBugs** | 0 bugs |
| **Test suite** | 2,020 pass, 0 fail, 2 skipped |
| **Total new findings** | 45 |
| **Critical** | 5 |
| **High** | 9 |
| **Medium** | 26 |
| **Low** | 5 |
| **New issues created** | 45 |

### Top 5 Priority Items

1. **#295 — evaluateConstant rejects binary expressions**: Blocks common SD modeling patterns like `SMOOTH(x, delay/2)`. Fix: extend evaluateConstant to support `BinaryOp` with constant operands.
2. **#321 — Conditional branch skips stateful SD functions**: SMOOTH/DELAY3 in IF branches go stale when the branch is not taken. Fix: always evaluate both branches (SD convention).
3. **#296 — Uppercase function name breaks lookup tables**: Silently breaks Vensim-style `table(input)` syntax for mixed-case table names. Fix: preserve original case for lookup table resolution.
4. **#320 — Simulation step counter overflow**: int counter wraps around for long simulations with small DT. Fix: change `currentStep` to `long`.
5. **#294 — DELAY_FIXED zero delay crash**: Runtime crash for edge-case delay values. Fix: add the same guard that `compileDelay3` already has.
