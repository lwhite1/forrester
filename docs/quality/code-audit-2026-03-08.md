# Code Audit & Quality Assessment — 2026-03-08 (Rev 2)

## Scope

Full audit of the Forrester System Dynamics modeling platform covering all five modules.
Supersedes the previous revision from earlier today.

| Module | Source Files | Source LoC | Test Files | Test LoC | Test:Source Ratio |
|--------|-------------|-----------|------------|----------|-------------------|
| forrester-engine | 154 | 21,383 | 84 | 17,307 | 0.81 |
| forrester-app | 95 | 20,807 | 39 | 9,263 | 0.45 |
| forrester-ui | 5 | 549 | 5 | 444 | 0.81 |
| forrester-tools | 8 | 1,342 | 5 | 593 | 0.44 |
| forrester-demos | 26 | 2,683 | 6 | 962 | 0.36 |
| **Total** | **288** | **46,764** | **139** | **28,569** | **0.61** |

**Build status:** 1,928 tests pass (0 failures, 2 skipped) across all modules. Clean compile on JDK 25.

**Static analysis:** SpotBugs 4.9.8 reports **1 bug** (effort=Max, threshold=Medium) — null dereference in `BatchImportCli.downloadToTemp` (#227).

**Code hygiene:** 12 comment-only catch blocks (#230), 0 printStackTrace calls, 0 wildcard imports, 9 @SuppressWarnings (all justified unchecked casts in JavaFX code), 1 TODO, ~40 `return null` occurrences in production code, 15 unused imports (#229).

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
| Manual review | — | Architecture, patterns, security |

---

## JaCoCo Coverage Report

### Module Summary

| Module | Instruction Coverage | Branch Coverage | Line Coverage |
|--------|---------------------|----------------|---------------|
| forrester-engine | 34,945 / 39,220 (89.1%) | 3,045 / 3,966 (76.8%) | 7,129 / 8,045 (88.6%) |
| forrester-ui | 1,146 / 1,265 (90.6%) | 40 / 48 (83.3%) | 214 / 248 (86.3%) |
| forrester-tools | 1,654 / 3,078 (53.7%) | 153 / 305 (50.2%) | 349 / 686 (50.9%) |
| forrester-app | 21,257 / 52,407 (40.6%) | 1,230 / 3,581 (34.3%) | 4,255 / 10,393 (40.9%) |
| forrester-demos | 2,003 / 5,188 (38.6%) | 6 / 42 (14.3%) | 416 / 1,114 (37.3%) |
| **Grand Total** | **61,005 / 101,158 (60.3%)** | **4,474 / 7,942 (56.3%)** | **12,363 / 20,486 (60.3%)** |

### Engine Coverage by Package

| Package | Instruction Coverage | Branch Coverage |
|---------|---------------------|----------------|
| model | 5,262 / 5,711 (92.1%) | 386 / 445 (86.7%) |
| model.expr | 1,927 / 2,060 (93.5%) | 248 / 288 (86.1%) |
| model.graph | 3,888 / 4,196 (92.7%) | 422 / 498 (84.7%) |
| model.compile | 4,656 / 5,235 (88.9%) | 323 / 432 (74.8%) |
| model.def | 3,048 / 3,560 (85.6%) | 357 / 491 (72.7%) |
| sweep | 3,713 / 4,277 (86.8%) | 250 / 338 (74.0%) |
| io.xmile | 3,280 / 3,746 (87.6%) | 320 / 477 (67.1%) |
| io.vensim | 4,165 / 4,879 (85.4%) | 408 / 591 (69.0%) |
| io.json | 1,827 / 2,344 (77.9%) | 166 / 236 (70.3%) |
| io (top-level) | 202 / 609 (33.2%) | 12 / 46 (26.1%) |
| measure (all) | 1,431 / 1,639 (87.3%) | 47 / 72 (65.3%) |

### App Module — Zero-Coverage Classes (>300 instructions)

| Class | Instructions | Notes |
|-------|-------------|-------|
| QuickstartDialog | 1,224 | Help dialog |
| ExpressionLanguageDialog | 1,191 | Help dialog |
| LookupForm | 993 | Form |
| SdConceptsDialog | 926 | Help dialog |
| MultiSweepResultPane | 743 | Result pane |
| CausalLinkGeometry | 705 | Geometry — testable |
| FanChartPane | 692 | Chart pane |
| KeyboardShortcutsDialog | 506 | Dialog |
| SweepResultPane | 481 | Result pane |
| SelectionRenderer | 479 | Renderer |
| StockForm | 475 | Form |
| OptimizationResultPane | 473 | Result pane |
| FlowForm | 455 | Form |
| FeedbackLoopRenderer | 378 | Renderer |
| AuxForm | 374 | Form |
| ConstantForm | 356 | Form |
| DiagramExporter | 332 | PNG/SVG bridge |
| SensitivityPane | 287 | Sensitivity pane |
| SparklineRenderer | 268 | Renderer |
| MonteCarloResultPane | 220 | Result pane |
| CldVariableForm | 169 | Form |
| UndoHistoryPopup | 136 | Popup |

22 zero-coverage classes totaling ~10,666 uncovered instructions. See #177 for tracking.

---

## SpotBugs Results

**SpotBugs 4.9.8** (upgraded from 4.8.6, which could not scan JDK 25 class files).

| Module | Bugs Found |
|--------|-----------|
| forrester-engine | 0 |
| forrester-app | 0 |
| forrester-ui | 0 |
| forrester-demos | 0 |
| forrester-tools | **1** |

**Bug:** `NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE` in `BatchImportCli.downloadToTemp()` line 205 — `Path.of(rawName).getFileName()` can return null. → **#227**

---

## Summary of All Findings

### By Severity

| Severity | Total (All Time) | Currently Open | Fixed/Closed |
|----------|-----------------|----------------|--------------|
| Critical | 7 | 0 | 7 |
| High | 17 | 0 | 17 |
| Medium | 31 | 17 | 14 |
| Low | 18 | 17 | 1 |

**All critical and high-severity issues resolved.** 17 medium and 17 low issues remain open.

### New Issues Created This Audit

| # | Title | Severity | Milestone |
|---|-------|----------|-----------|
| #227 | BatchImportCli.downloadToTemp null dereference on Path.getFileName() | Medium | R1 |
| #228 | Decompose ModelCanvas — 1,082 lines, 63 public methods | Medium | R1 |
| #229 | Remove 15 unused imports across codebase | Low | R2 |
| #230 | Replace 12 comment-only catch blocks with debug logging | Medium | R1 |
| #231 | Replace remaining broad catch(Exception) in ModelCompiler and ImportPipeline | Medium | R1 |
| #232 | Split app.canvas mega-package into sub-packages | Low | R2 |
| #233 | Add unit tests for CompilationContext and CompiledModel (engine) | Medium | R1 |
| #234 | Extract long methods exceeding 150 lines | Low | R2 |

### Issues Fixed Since Previous Audit

| # | Title | Was |
|---|-------|-----|
| #145 | Add TestFX tests for forrester-ui module | M5 |
| #160 | ModelEditor decomposition | — |
| #163 | Replace return null with Optional in exporter/importer methods | M7 |
| #178 | Add test coverage for ModelReport | M16 |
| #189 | BatchImportCli.downloadToTemp accepts file:// URIs | M19 |
| #190 | UndoManager serializes full model on FX thread | M20 |
| #218 | DependencyGraph Tarjan SCC has no recursion depth guard | H15 |
| #219 | LookupForm mutates array before validation | H16 |
| #220 | ExportBounds.compute() returns invalid bounds for empty models | M21 |
| #221 | FanChart uses unsynchronized static fields | M22 |
| #222 | BatchImportCli path traversal via unsanitized URL filename | H17 |
| #223 | UndoManager compressor ExecutorService never shut down | M23 |

---

## Critical Issues — ALL FIXED

C1–C7 (#155, #164, #166, #179–#182) — all closed. No new critical findings.

---

## High Issues — ALL FIXED

H1–H17 — all closed. Former open issues H15 (#218), H16 (#219), H17 (#222) were all resolved since the last audit. No new high-severity findings.

---

## Medium Issues — 17 Open, 14 Fixed

### Previously Fixed
M1 (#161), M2 (#71), M3 (#162), M5 (#145), M6 (#163), M7 (#163), M9 (#169), M10 (#171), M11 (#173), M12, M14 (#176), M16 (#178), M19 (#189), M20 (#190), M21 (#220), M22 (#221), M23 (#223)

### Open

| ID | Issue | # | Milestone |
|----|-------|---|-----------|
| M4 | Equation rename uses string token replacement instead of AST | #131 | R1 |
| M8 | ModelDefinition has 15 fields and 2 telescoping constructors | #72 | R2 |
| M13 | CsvSubscriber uses string concatenation in SLF4J logging | #175 | R2 |
| M15 | App module test coverage at 40.6% — 22 core classes at 0% | #177 | R2 |
| M17 | logger.xml misnamed — logging config never loaded | #187 | R2 |
| M18 | String.format without Locale in chart rendering | #188 | R2 |
| M24 | BatchImportCli.downloadToTemp null dereference | #227 | R1 |
| M25 | ModelCanvas god class (1,082 lines, 63 public methods) | #228 | R1 |
| M26 | 12 comment-only catch blocks swallowing exceptions | #230 | R1 |
| M27 | Broad catch(Exception) in ModelCompiler and ImportPipeline | #231 | R1 |
| M28 | CompilationContext and CompiledModel lack unit tests | #233 | R1 |
| M29 | ObjectMapper lacks security hardening | #200 | R2 |
| M30 | Connector regeneration on every model change | #204 | R2 |
| M31 | Canvas redraws twice on resize | #203 | R2 |
| M32 | Hardcoded dialog sizes | #202 | R2 |
| M33 | Unsaved changes dialog comes up twice | #226 | R1 |
| M34 | Review element naming policy | #154 | R1 |

---

## Low Issues — 17 Open

| ID | Issue | # | Milestone |
|----|-------|---|-----------|
| L1 | Unused TODO comment in Quantity.java | — | — |
| L2 | Color constants hardcoded and duplicated | #77 | R2 |
| L3 | 15 unused imports across codebase | #229 | R2 |
| L4 | Split app.canvas mega-package (87 files) | #232 | R2 |
| L5 | Extract long methods exceeding 150 lines | #234 | R2 |
| L6 | Causal link polarity labels clipped in export | #114 | R2 |
| L7 | AutoLayout back-edge marking | #116 | R2 |
| L8 | ValidationDialog extends Stage inconsistently | #213 | R2 |
| L9 | Help windows hide behind main window | #205 | R2 |
| L10 | Add Help menu links for bug reports | #216 | R2 |
| L11 | Add .editorconfig | #212 | R2 |
| L12 | Checkstyle not applied project-wide | #211 | R2 |
| L13 | CI pipeline missing quality gates | #210 | R2 |
| L14 | Fractional DT not supported | #133 | R2 |
| L15 | Define formal grammar for model files | #191 | R2 |
| L16 | Keyboard-driven connection creation | #4 | R2 |
| L17 | Example model tests lack behavioral assertions | #29 | R2 |

---

## Security Assessment

| Category | Status |
|----------|--------|
| XXE protection | XML import/export both hardened (DocumentBuilderFactory + TransformerFactory) |
| Expression parser depth | Limited to MAX_DEPTH=200 |
| Graph traversal depth | FeedbackAnalysis and DependencyGraph both guarded (MAX_DEPTH=200) |
| Unsafe deserialization | None — no ObjectInputStream, no Serializable |
| Reflection abuse | None in production code (TestFX tests use reflection for FanChart) |
| SQL injection | N/A — no database |
| Command injection | None — no Runtime.exec, ProcessBuilder |
| File path traversal | Fixed (#222). Residual null-safety issue (#227) |
| File size limits | All importers enforce 10 MB cap |
| Simulation safety | Timeout (60s), MAX_STEPS (10M), NaN detection, cancellation support |
| ObjectMapper hardening | Missing (#200) — should disable default typing |

**Resource management:** All I/O uses try-with-resources. All executor services now properly shut down (#223 fixed).

**Thread safety:** FX thread confinement enforced via `checkFxThread()` on all ModelEditor mutations. `CopyOnWriteArrayList` for listeners. `FanChart` volatile fields fixed (#221). `UndoManager` executor leak fixed (#223).

---

## Architecture Assessment

### Module Dependency Graph

```
forrester-engine  (no internal deps — foundation)
    ↑         ↑
    |         |
forrester-ui  |  (depends on: engine)
    ↑         |
    |         |
    |    forrester-app   (depends on: engine)
    |    forrester-tools (depends on: engine)
    |
forrester-demos (depends on: engine, ui)
```

**No circular dependencies. No layering violations.** Clean acyclic dependency graph.

### Strengths

1. **Clean engine/app separation** — Engine has zero UI dependencies. Records for all domain types.
2. **Well-structured interaction controllers** — Canvas interaction decomposed into 10+ focused controllers.
3. **Security hardened** — XXE protection, expression depth limits, file size limits, simulation safety guards.
4. **Consistent logging** — SLF4J throughout. No System.out/err in production code (only in CLI tools and demos).
5. **Defensive records** — Compact constructors with null-checks, List.copyOf(), and validation guards.
6. **Good engine test coverage** — 89.1% instruction coverage, 76.8% branch coverage.
7. **No deep inheritance** — Composition over inheritance consistently applied.
8. **No wildcard imports, no printStackTrace** — Clean code hygiene.

### Weaknesses

1. **App module coverage gap** — 40.6% instruction coverage, 22 classes at 0%. (#177)
2. **Two god classes remain** — ModelEditor (1,200 lines, decomposed but still large) and ModelCanvas (1,082 lines, #228).
3. **Mega-package** — app.canvas has 87 files. (#232)
4. **No Checkstyle or ErrorProne** — Only SpotBugs for static analysis. (#210, #211)
5. **~40 return null occurrences** — Partially addressed (#163 closed), more remain.

### Largest Files

| File | Lines | Public Methods | Coverage | Notes |
|------|-------|---------------|----------|-------|
| ModelEditor.java | 1,200 | 79 | 87% | Decomposed (#160), still large |
| ModelWindow.java | 1,092 | 8 | ~30% | Main window, long methods |
| ModelCanvas.java | 1,082 | 63 | 19% | God class (#228) |
| SvgExporter.java | 835 | 2 | 2% | Duplicates CanvasRenderer (#67) |
| ModelDefinitionSerializer.java | 774 | — | ~78% | Hand-rolled JSON |
| CanvasRenderer.java | 748 | — | 27% | Drawing code |
| FeedbackAnalysis.java | 696 | — | ~93% | Well-tested |
| XmileImporter.java | 680 | — | 88% | Well-tested |
| InputDispatcher.java | 680 | — | 15% | Event routing |
| EquationAutoComplete.java | 679 | — | ~10% | Tokenization + completion |

---

## Test Results

```
Module              Tests   Failures  Errors  Skipped
forrester-engine    1,207   0         0       0
forrester-app       618     0         0       2
forrester-demos     44      0         0       0
forrester-tools     37      0         0       0
forrester-ui        22      0         0       0
TOTAL               1,928   0         0       2
```

SpotBugs 4.9.8: **1 bug** (Medium — #227).

---

## Open Issues by Milestone

### R1 (Release 1) — 18 open

| # | Title | Type |
|---|-------|------|
| 49 | Add larger demo models from MetaSD and TU Delft | Feature |
| 81 | Add automated layout regression tests | Feature |
| 89 | Add maturity visual indicators on canvas | Feature |
| 96 | Create landing page / README | Docs |
| 97 | Write user manual | Docs |
| 99 | Publish forrester-engine JAR to Maven Central | Release |
| 100 | Create platform installers with jpackage | Release |
| 101 | Write GitHub README | Docs |
| 131 | Equation rename uses string token replacement | Medium bug |
| 154 | Review element naming policy | Medium |
| 226 | Unsaved changes dialog comes up twice | Medium bug |
| 227 | BatchImportCli null dereference | Medium bug |
| 228 | Decompose ModelCanvas god class | Medium |
| 230 | Comment-only catch blocks | Medium |
| 231 | Broad catch(Exception) in ModelCompiler/ImportPipeline | Medium bug |
| 233 | Add tests for CompilationContext/CompiledModel | Medium |

### R2 — 24 open

4, 29, 67, 68, 69, 72, 77, 85, 88, 114, 116, 133, 175, 177, 187, 188, 191, 200, 202, 203, 204, 205, 210, 211, 212, 213, 216, 229, 232, 234

---

## Comparison: Previous Audit (Rev 1) vs. Current (Rev 2)

| Metric | Rev 1 | Rev 2 | Change |
|--------|-------|-------|--------|
| Source files | 282 | 288 | +6 |
| Source LoC | 45,586 | 46,764 | +1,178 |
| Test files | 126 | 139 | +13 |
| Test LoC | 26,607 | 28,569 | +1,962 |
| Test:Source ratio | 0.58 | 0.61 | +0.03 |
| Tests passing | 1,830 | 1,928 | +98 |
| SpotBugs version | 4.8.6 (broken on JDK 25) | 4.9.8 | Upgraded |
| SpotBugs findings | 0 (incomplete scan) | 1 | Now scanning all classes |
| Open critical issues | 0 | 0 | — |
| Open high issues | 3 | 0 | **-3 (all fixed)** |
| Open medium issues | 15 | 17 | +2 (5 fixed, 7 new) |
| Open low issues | 14 | 17 | +3 (new findings) |
| Engine instruction coverage | 87.5% | 89.1% | +1.6% |
| App instruction coverage | 39.3% | 40.6% | +1.3% |
| UI instruction coverage | 0% | 90.6% | **+90.6%** |

---

## Recommendations

### Immediate (R1 blockers)

1. Fix null dereference in BatchImportCli (#227) — SpotBugs finding
2. Replace broad catch(Exception) blocks (#231) — 2 remaining
3. Add debug logging to comment-only catch blocks (#230) — 12 occurrences
4. Add engine tests for CompilationContext/CompiledModel (#233)
5. Begin ModelCanvas decomposition (#228)

### Short-term (R1)

6. Fix equation rename to use AST (#131)
7. Fix unsaved-changes double-dialog (#226)
8. Review element naming policy (#154)

### Medium-term (R2)

9. Improve app module test coverage (#177) — 22 zero-coverage classes
10. Apply Checkstyle project-wide (#211) with CI enforcement (#210)
11. Add .editorconfig (#212)
12. Split app.canvas mega-package (#232)
13. Extract long methods (#234)
14. Remove unused imports (#229)
