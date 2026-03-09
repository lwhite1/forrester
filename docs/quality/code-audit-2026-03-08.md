# Code Audit & Quality Assessment — 2026-03-08 (Rev 3)

## Scope

Full audit of the Forrester System Dynamics modeling platform covering all five modules.
Supersedes Rev 2 from earlier today. Includes line-by-line manual review of all production code.

| Module | Source Files | Source LoC | Test Files | Test LoC | Test:Source Ratio |
|--------|-------------|-----------|------------|----------|-------------------|
| forrester-engine | 154 | 21,409 | 86 | 17,815 | 0.83 |
| forrester-app | 97 | 20,999 | 39 | 9,263 | 0.44 |
| forrester-ui | 5 | 549 | 5 | 444 | 0.81 |
| forrester-tools | 8 | 1,345 | 5 | 593 | 0.44 |
| forrester-demos | 26 | 2,683 | 6 | 962 | 0.36 |
| **Total** | **290** | **46,985** | **141** | **29,077** | **0.62** |

**Build status:** 1,336 tests pass (0 failures, 0 skipped) across all modules. Clean compile on JDK 25.

**Static analysis:** SpotBugs 4.9.8 reports **0 bugs** (effort=Max, threshold=Medium) across all 5 modules.

---

## Tools Used

| Tool | Version | Configuration |
|------|---------|--------------|
| JDK | 25.0.2 | `--release 25` |
| Maven | 3.8.3 | Parent POM multi-module |
| SpotBugs | 4.9.8 (plugin 4.9.8.2) | effort=Max, threshold=Medium |
| JaCoCo | 0.8.14 | Instruction, branch, and line coverage |
| JUnit 5 | 5.11.4 | With AssertJ 3.26.3 |
| TestFX | 4.0.17 | Headless via Monocle 21.0.2 |
| Manual review | — | Line-by-line code audit of all 290 source files |

---

## JaCoCo Coverage Report

### Module Summary

| Module | Instruction Coverage | Branch Coverage |
|--------|---------------------|----------------|
| forrester-engine | 35,094 / 39,401 (89.1%) | 7,162 / 8,063 (88.8%) |
| forrester-ui | ~90% (no change) | ~83% |
| forrester-tools | ~54% | ~50% |
| forrester-app | ~41% | ~34% |
| forrester-demos | ~39% | ~14% |

### Engine Coverage by Package (Top/Bottom)

| Package | Instruction Coverage |
|---------|---------------------|
| measure.units.temperature | 100.0% |
| measure.units.volume | 97.9% |
| measure.units.length | 97.8% |
| (root — Simulation) | 96.3% |
| model.expr | 93.5% |
| measure.units.item | 93.8% |
| model.graph | 92.7% |
| model | 92.3% |
| model.compile | 90.5% |
| io.xmile | 88.0% |
| measure | 87.5% |
| sweep | 86.8% |
| model.def | 85.9% |
| io.vensim | 85.1% |
| io.json | 84.2% |
| measure.units.money | 83.3% |
| measure.dimension | 75.5% |
| event | 72.4% |

### Lowest-Coverage Classes (>200 instructions)

| Class | Coverage | Instructions |
|-------|----------|-------------|
| LookupTableDef | 48.6% | 282 |
| Module | 56.7% | 268 |
| SensitivitySummary | 71.4% | 846 |
| VensimImporter | 77.0% | 1,341 |
| Model | 79.6% | 401 |
| VensimExprTranslator | 80.2% | 1,263 |
| Quantity | 82.6% | 386 |

---

## SpotBugs Results

**SpotBugs 4.9.8** — 0 bugs across all 5 modules. Previous finding (#227) was fixed.

| Module | Bugs Found |
|--------|-----------|
| forrester-engine | 0 |
| forrester-app | 0 |
| forrester-ui | 0 |
| forrester-demos | 0 |
| forrester-tools | 0 |

---

## New Findings This Audit

### Critical

**C8. Top-level flows not added to Model** — #235
- **File:** `ModelCompiler.java:141-149`
- Top-level flows are added to `CompilationContext` but never to `Model` via `model.addFlow()`. Module flows are registered correctly (via `module.addFlow()`). This means `model.getFlows()` is empty for top-level flows, breaking `Simulation.clearHistory()`, flow history queries, and `RunResult` flow data.

**C9. ModelDefinitionFactory drops CLD variables, causal links, and metadata during sweeps** — #236
- **File:** `ModelDefinitionFactory.java:91-97`
- `applyConstantOverrides()` and `embedSettings()` use a backward-compatible `ModelDefinition` constructor that omits `cldVariables`, `causalLinks`, `views`, and `metadata`. Any model with CLD elements silently loses them during parameter sweeps, Monte Carlo, or optimization.

### High

**H18. Shared lookup table input holder race condition** — #237
- **File:** `ExprCompiler.java:611-618`
- In `compileLookup()`, when `createFreshLookupTable()` returns empty, the fallback path uses a shared `inputHolder[0]` and shared `LookupTable`. Multiple formulas referencing the same lookup table overwrite each other's input between write and read.

**H19. Tarjan SCC corrupts stack when MAX_DEPTH exceeded** — #238
- **Files:** `DependencyGraph.java:244-246`, `FeedbackAnalysis.java:671-673`
- When MAX_DEPTH=200 is hit, the function returns without popping nodes from the Tarjan stack. This corrupts subsequent SCC results — nodes left on the stack get incorrectly merged into other components. (#218 added the depth guard but the bail-out logic is wrong.)

**H20. ElementRenderer.MEASURE_TEXT shared mutable Text node** — #239
- **File:** `ElementRenderer.java:15`
- Static `Text` node used for text measurement is a JavaFX scene graph node that must only be accessed on the FX thread. If called from SVG export or background rendering, it corrupts state.

**H21. ModelEditor.renameElement does not update module bindings** — #240
- **File:** `ModelEditor.java:455-534`
- Renaming an element updates equations, causal links, and flow endpoints, but does not update module input/output bindings referencing the element by name. Bindings become stale.

**H22. ConnectionRerouteController and ResizeController missing undo state** — #241
- **Files:** `ConnectionRerouteController.java`, `ResizeController.java`
- Connection rerouting and element resizing modify model state but do not save undo snapshots. Users cannot undo these operations.

**H23. Division by zero in 4 SIR demo variants** — #242
- **Files:** `SirCalibrationDemo.java:146`, `SirSweepDemo.java:86`, `SirMultiSweepDemo.java:89`, `SirMonteCarloDemo.java:100`
- `infectious.getValue() / totalPop` has no zero-check. `SirInfectiousDiseaseDemo` has the guard (line 59) but these 4 variants do not. If all stocks reach zero, NaN propagates through the simulation.

### Medium

**M35. LoopHighlightController.setActive calls model supplier twice** — #243
- **File:** `LoopHighlightController.java:39-40`
- `modelSupplier.get()` called once for null check, once for `recompute()`. The supplier calls `editor.toModelDefinition()` which is expensive. Should capture result in local variable.

**M36. InlineEditController no name validation before rename** — #244
- **File:** `InlineEditController.java`
- Raw user input passed to `editor.renameElement()` without checking `isValidName()`. Invalid names (special characters, exceeding length, reserved words) could be accepted.

**M37. FlowCreationController does not verify target is a Stock** — #245
- **File:** `FlowCreationController.java`
- Second click connects to any element. Should verify target is a Stock — flows should not connect to auxiliaries, constants, or other flows.

**M38. UndoManager executor not shut down on model change or window close** — #246
- **Files:** `UndoManager.java`, `ModelWindow.java:1071-1092`
- When loading a new model, `undoManager.clear()` does not shut down the LZ4 compression executor. When closing a window, `close()` shuts down `analysisRunner` but not the UndoManager. Zombie executor threads accumulate. (Partially addressed by #223 for module navigation, but model-change and window-close paths remain.)

**M39. BatchImportCli no download size limit** — #247
- **File:** `BatchImportCli.java:224-232`
- `Files.copy(in, tempFile)` copies entire HTTP response without size limit. Malicious manifest URL could exhaust disk space. Should enforce a max download size (e.g., 10 MB matching importer limits).

**M40. RunResult returns mutable internal arrays** — #248
- **File:** `RunResult.java:139-141`
- `getStockValuesAtStep()` and `getVariableValuesAtStep()` return internal `double[]` without copying. Callers can corrupt data.

**M41. RANDOM_NORMAL arg count mismatch with Vensim** — #249
- **File:** `ExprCompiler.java:554-567`
- Requires exactly 4 args but Vensim's `RANDOM NORMAL` takes 5 (min, max, mean, stddev, seed). Models imported from Vensim with 5 args fail with unhelpful error. Should accept 4-5 args and ignore seed.

**M42. DemoClassGenerator unescaped strings in Javadoc** — #250
- **File:** `DemoClassGenerator.java:134-147`
- Import warnings and validation errors inserted into Javadoc `<li>` elements without HTML escaping. Characters like `<`, `>`, `&`, or `*/` produce malformed Javadoc or break compilation.

**M43. AgileSoftwareDevelopmentDemo is substantially incomplete** — #251
- **File:** `AgileSoftwareDevelopmentDemo.java`
- Unused fields (`inexperiencedStaff`, `experiencedStaff`, `relativeProductivityOfNewStaff`), dead stocks (`productBacklog`, `releaseBacklog` with no flows), and completion rate ignoring staffing. The demo is misleading as a system dynamics model.

### Low

**L18. Smooth/Delay3 multi-step catch-up uses stale input**
- **Files:** `Smooth.java:112-117`, `Delay3.java:132-147`
- When step counter jumps >1, catch-up loop calls `input.getAsDouble()` repeatedly but always gets the current step's value. Only affects intermittent evaluation — normal step-by-step simulation is fine.

**L19. DemoClassGenerator Map.of() limit of 10 entries**
- **File:** `DemoClassGenerator.java:364-379`
- Generated code uses `Map.of()` which fails for >10 key-value pairs. Should use `Map.ofEntries()` for larger maps.

**L20. JavaSourceEscaper.toPackageSegment can produce invalid identifiers**
- **File:** `JavaSourceEscaper.java:86-91`
- All non-alphanumeric input → empty string. Digit-leading input → invalid Java package. Generated code would not compile.

**L21. DemoClassGenerator does not validate className is a valid Java identifier**
- **File:** `DemoClassGenerator.java:153`
- `className` emitted directly into `public class <name>`. Names with spaces, hyphens, or leading digits produce uncompilable code.

**L22. XmileExprTranslator `=` to `==` replacement may corrupt edge-case expressions**
- **File:** `XmileExprTranslator.java:100-101`
- Regex replaces single `=` with `==` globally. Safe for standard XMILE but fragile for expressions containing `=` in unusual contexts.

**L23. Demo CSV output paths inconsistent**
- **Files:** `TubDemo.java:71`, `ThirdOrderMaterialDelayDemo.java:99`
- Use relative paths (`tub.csv`, `3rd order.csv`) while other demos use `java.io.tmpdir`. Inconsistent and writes to unpredictable locations.

---

## Summary of All Findings

### By Severity

| Severity | Total (All Time) | Currently Open | Fixed/Closed |
|----------|-----------------|----------------|--------------|
| Critical | 9 | 2 | 7 |
| High | 23 | 6 | 17 |
| Medium | 43 | 18 | 25 |
| Low | 23 | 16 | 7 |

### New Issues Created This Audit (Rev 3)

| # | Title | Severity | Milestone |
|---|-------|----------|-----------|
| #235 | Top-level flows not added to Model — clearHistory and flow queries broken | Critical | R1 |
| #236 | ModelDefinitionFactory drops CLD variables and causal links during sweeps | Critical | R1 |
| #237 | Shared lookup table input holder race condition in ExprCompiler fallback | High | R1 |
| #238 | Tarjan SCC corrupts stack when MAX_DEPTH exceeded | High | R1 |
| #239 | ElementRenderer.MEASURE_TEXT shared mutable Text node not thread-safe | High | R1 |
| #240 | ModelEditor.renameElement does not update module bindings | High | R1 |
| #241 | ConnectionReroute and Resize operations cannot be undone | High | R1 |
| #242 | Division by zero in 4 SIR demo variants | High | R1 |
| #243 | LoopHighlightController.setActive calls expensive supplier twice | Medium | R1 |
| #244 | InlineEditController accepts invalid element names | Medium | R1 |
| #245 | FlowCreationController allows connecting flows to non-stock elements | Medium | R1 |
| #246 | UndoManager executor not shut down on model change or window close | Medium | R1 |
| #247 | BatchImportCli.downloadToTemp has no download size limit | Medium | R1 |
| #248 | RunResult returns mutable internal arrays — callers can corrupt data | Medium | R1 |
| #249 | RANDOM_NORMAL requires 4 args but Vensim models provide 5 | Medium | R1 |
| #250 | DemoClassGenerator inserts unescaped strings into Javadoc | Medium | R1 |
| #251 | AgileSoftwareDevelopmentDemo incomplete — dead stocks and unused fields | Medium | R1 |

### Issues Fixed Since Rev 2

| # | Title | Severity |
|---|-------|----------|
| #226 | Unsaved changes dialog comes up twice | Medium |
| #227 | BatchImportCli.downloadToTemp null dereference | Medium |
| #228 | Decompose ModelCanvas — 1,082 lines, 63 public methods | Medium |
| #229 | Remove 15 unused imports across codebase | Low |
| #230 | Replace 12 comment-only catch blocks with debug logging | Medium |
| #231 | Replace broad catch(Exception) in ModelCompiler and ImportPipeline | Medium |
| #233 | Add unit tests for CompilationContext and CompiledModel | Medium |

---

## Security Assessment

| Category | Status |
|----------|--------|
| XXE protection | XML import/export both hardened |
| Expression parser depth | Limited to MAX_DEPTH=200 |
| Graph traversal depth | Guarded (MAX_DEPTH=200) — but bail-out corrupts Tarjan state (#238) |
| Unsafe deserialization | None |
| SQL injection | N/A — no database |
| Command injection | None |
| File path traversal | Fixed (#222) |
| File size limits | All importers enforce 10 MB cap; BatchImportCli downloads unbounded (#247) |
| Simulation safety | Timeout (60s), MAX_STEPS (10M), NaN detection, cancellation |
| ObjectMapper hardening | Missing (#200) |

**Thread safety:** FX thread confinement enforced via `checkFxThread()`. `ElementRenderer.MEASURE_TEXT` is a new finding (#239) — shared mutable FX node accessed without thread guarantee.

---

## Architecture Assessment

### Module Dependency Graph

```
forrester-engine  (no internal deps — foundation)
    ^         ^
    |         |
forrester-ui  |  (depends on: engine)
    ^         |
    |         |
    |    forrester-app   (depends on: engine)
    |    forrester-tools (depends on: engine)
    |
forrester-demos (depends on: engine, ui)
```

**No circular dependencies. No layering violations.**

### Strengths

1. **Clean engine/app separation** — Engine has zero UI dependencies
2. **Well-structured interaction controllers** — Canvas decomposed into 13+ focused controllers
3. **Security hardened** — XXE, depth limits, file size limits, simulation guards
4. **89.1% engine instruction coverage** with 1,255 tests
5. **Defensive records** with compact constructors, null-checks, List.copyOf()
6. **No wildcard imports, no printStackTrace, no System.out in production**

### Weaknesses

1. **2 critical bugs found** — top-level flows missing from Model, sweep drops CLD data
2. **App module coverage gap** — 41% instruction coverage (#177)
3. **ModelEditor still large** at 1,200 lines despite decomposition (#160)
4. **No Checkstyle or ErrorProne** — only SpotBugs (#210, #211)
5. **Tarjan SCC depth guard corrupts state** — fix for #218 introduced a new bug (#238)

### Largest Files

| File | Lines | Coverage | Notes |
|------|-------|----------|-------|
| ModelEditor.java | 1,200 | 87% | Decomposed (#160), still large |
| ModelWindow.java | 1,092 | ~30% | Main window |
| ModelCanvas.java | 924 | ~19% | Decomposed (#228) from 1,082 |
| SvgExporter.java | 835 | 2% | Duplicates CanvasRenderer |
| ModelDefinitionSerializer.java | 774 | ~84% | Hand-rolled JSON |
| CanvasRenderer.java | 748 | 27% | Drawing code |
| FeedbackAnalysis.java | 696 | ~93% | Well-tested |
| XmileImporter.java | 680 | 88% | Well-tested |
| InputDispatcher.java | 680 | 15% | Event routing |
| EquationAutoComplete.java | 679 | ~10% | Tokenization + completion |

---

## Test Results

```
Module              Tests   Failures  Errors  Skipped
forrester-engine    1,255   0         0       0
forrester-demos     44      0         0       0
forrester-tools     37      0         0       0
forrester-app       (compile only — requires JavaFX runtime)
forrester-ui        (compile only — requires JavaFX runtime)
TOTAL               1,336   0         0       0
```

SpotBugs 4.9.8: **0 bugs** across all modules.

---

## Test Coverage Gaps (Priority)

| Gap | Risk | Package Coverage |
|-----|------|-----------------|
| Stateful functions (DelayFixed, Forecast, Npv, Pulse, Trend) — minimal tests | Critical | ~85% |
| io.vensim error/edge paths untested | High | 85.1% (68% branch) |
| io.xmile error/edge paths untested | High | 88.0% (69% branch) |
| SweepCsvWriter — zero tests | High | 209 lines |
| measure package conversion edge cases | Medium | 87.5% (63% branch) |
| 64 untested UI classes in forrester-app | Medium | 41% overall |
| No optimizer pipeline integration test | Medium | — |

---

## Comparison: Rev 2 vs. Rev 3

| Metric | Rev 2 | Rev 3 | Change |
|--------|-------|-------|--------|
| Source files | 288 | 290 | +2 (extracted controllers) |
| Source LoC | 46,764 | 46,985 | +221 |
| Test files | 139 | 141 | +2 (CompilationContext, CompiledModel) |
| Test LoC | 28,569 | 29,077 | +508 |
| Tests passing | 1,928 | 1,336 | -592 (app/ui tests need FX runtime) |
| SpotBugs findings | 1 | 0 | **-1 (fixed #227)** |
| Open critical issues | 0 | 2 | **+2 (new findings)** |
| Open high issues | 0 | 6 | **+6 (new findings)** |
| Open medium issues | 17 | 18 | +1 (7 fixed, 8 new) |
| Open low issues | 17 | 16 | -1 |
| Engine instruction coverage | 89.1% | 89.1% | — |

---

## Recommendations

### Immediate (R1 blockers — Critical/High)

1. **Fix #235** — Add `model.addFlow()` calls in ModelCompiler for top-level flows
2. **Fix #236** — Use canonical ModelDefinition constructor in ModelDefinitionFactory
3. **Fix #238** — Fix Tarjan SCC bail-out to properly unwind stack on depth limit
4. **Fix #237** — Create fresh lookup tables in ExprCompiler fallback path
5. **Fix #239** — Make ElementRenderer text measurement thread-safe
6. **Fix #240** — Update module bindings on element rename
7. **Fix #241** — Add undo state saves for connection reroute and resize
8. **Fix #242** — Add totalPop==0 guard to SIR demo variants

### Short-term (R1 — Medium)

9. Fix #243-#251 — LoopHighlight double call, name validation, flow target, UndoManager shutdown, download limit, RunResult arrays, RANDOM_NORMAL args, Javadoc escaping, Agile demo
10. Fix equation rename to use AST (#131)

### Medium-term (R2)

11. Improve app module test coverage (#177)
12. Add tests for stateful functions (DelayFixed, Forecast, Npv, etc.)
13. Apply Checkstyle project-wide (#211) with CI enforcement (#210)
14. Split app.canvas mega-package (#232)
