# Code Audit & Quality Assessment — 2026-03-08

## Scope

Full audit of the Forrester System Dynamics modeling platform covering all five modules.

| Module | Source Files | Source LoC | Test Files | Test LoC | Test:Source Ratio |
|--------|-------------|-----------|------------|----------|-------------------|
| forrester-engine | 154 | 21,293 | 78 | 16,134 | 0.76 |
| forrester-app | 89 | 19,722 | 37 | 8,918 | 0.45 |
| forrester-demos | 26 | 2,683 | 6 | 962 | 0.36 |
| forrester-tools | 8 | 1,339 | 5 | 593 | 0.44 |
| forrester-ui | 5 | 549 | 0 | 0 | 0.00 |
| **Total** | **282** | **45,586** | **126** | **26,607** | **0.58** |

**Build status:** 1,830 tests pass (0 failures, 2 skipped) across all modules. Clean compile.

**Static analysis:** SpotBugs reports **0 bugs** (effort=Max, threshold=Medium).

**Code hygiene:** 0 empty catch blocks, 0 printStackTrace calls, 0 wildcard imports, 1 @SuppressWarnings (justified), 1 TODO, 61 `return null` occurrences in production code.

---

## JaCoCo Coverage Report

| Module | Instruction Coverage | Branch Coverage | Line Coverage |
|--------|---------------------|----------------|---------------|
| forrester-engine | 34,007 / 38,852 (87.5%) | 2,984 / 3,964 (75.3%) | 6,957 / 8,001 (87.0%) |
| forrester-app | 19,583 / 49,848 (39.3%) | 1,144 / 3,347 (34.2%) | 3,904 / 9,820 (39.8%) |
| forrester-tools | 1,654 / 3,066 (53.9%) | 153 / 303 (50.5%) | 349 / 684 (51.0%) |
| forrester-demos | 2,003 / 5,188 (38.6%) | 6 / 42 (14.3%) | 416 / 1,114 (37.3%) |
| forrester-ui | — | — | — |

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

### App Module — Low Coverage Classes (>300 instructions, <50% covered)

| Class | Coverage | Notes |
|-------|----------|-------|
| QuickstartDialog | 0 / 1,224 (0%) | Help dialog |
| ExpressionLanguageDialog | 0 / 1,191 (0%) | Help dialog |
| SdConceptsDialog | 0 / 926 (0%) | Help dialog |
| LookupForm | 0 / 979 (0%) | Form |
| MultiSweepResultPane | 0 / 743 (0%) | Result pane |
| CausalLinkGeometry | 0 / 705 (0%) | Geometry — testable |
| FanChartPane | 0 / 692 (0%) | Chart pane |
| KeyboardShortcutsDialog | 0 / 506 (0%) | Dialog |
| SweepResultPane | 0 / 481 (0%) | Result pane |
| SelectionRenderer | 0 / 479 (0%) | Renderer |
| OptimizationResultPane | 0 / 473 (0%) | Result pane |
| StockForm | 0 / 471 (0%) | Form |
| FlowForm | 0 / 455 (0%) | Form |
| FeedbackLoopRenderer | 0 / 378 (0%) | Renderer |
| AuxForm | 0 / 374 (0%) | Form |
| ConstantForm | 0 / 352 (0%) | Form |
| DiagramExporter | 0 / 330 (0%) | PNG/SVG bridge |
| SvgExporter | 56 / 3,271 (2%) | SVG rendering |
| SimulationController | 24 / 948 (3%) | Simulation orchestration |
| InputDispatcher | 222 / 1,445 (15%) | Event routing |
| ModelCanvas | 365 / 1,909 (19%) | Main canvas |
| FileController | 183 / 825 (22%) | File I/O |
| CanvasRenderer | 488 / 1,797 (27%) | Drawing |
| FormContext | 192 / 692 (28%) | Form state |
| PropertiesPanel | 604 / 1,652 (37%) | Properties sidebar |
| ConnectionRenderer | 403 / 878 (46%) | Connection drawing |

---

## Summary of Findings

| Severity | Total (All Time) | Currently Open | Fixed/Closed |
|----------|-----------------|----------------|--------------|
| Critical | 7 | 0 | 7 |
| High | 17 | 3 | 14 |
| Medium | 23 | 15 | 8 |
| Low | 15 | 14 | 1 |

**All critical issues resolved.** 3 high-severity issues remain (found by deep code review): a missing recursion depth guard, a data corruption bug, and a path traversal vulnerability.

---

## Critical Issues — ALL FIXED

C1 (#155), C2 (#164), C3 (#166), C4 (#179), C5 (#180), C6 (#181), C7 (#182) — all closed.

Key fixes this audit cycle:
- **C4**: Added MAX_DEPTH=200 recursion guard to Tarjan SCC and dfsCycles
- **C5**: Two-phase Euler integration — stocks see pre-step values
- **C6**: dirtyListener removed on ModelWindow.close()
- **C7**: AnalysisRunner.shutdown() awaits termination

---

## High Issues — 3 Open, 14 Fixed

H1–H10 (#155–#172), H11 (#183), H12 (#184), H13 (#185), H14 (#186) — all closed.

Key fixes this audit cycle:
- **H11**: ExprCompiler warn-once flag for math warnings
- **H12**: XmileImporter getFirstChild/getChildTexts search direct children only
- **H13**: ImportPipelineCli requireValue() bounds check
- **H14**: ModelDefinitionSerializer 10 MB file size limit

### H15. DependencyGraph Tarjan SCC has no recursion depth guard
**Issue:** [#218](https://github.com/Courant-Systems/shrewd/issues/218) (open, R1)

`DependencyGraph.tarjanStrongconnect()` recurses without a depth limit. A pathological graph can cause `StackOverflowError`. `FeedbackAnalysis` already has `MAX_DEPTH=200` — `DependencyGraph` needs the same.

### H16. LookupForm mutates array before validation — data corruption
**Issue:** [#219](https://github.com/Courant-Systems/shrewd/issues/219) (open, R1)

`commitDataPoint()` writes new x/y values into the backing arrays, then validates monotonicity. If validation fails, the arrays are already corrupted. Fix: validate before mutation, or save/restore on failure.

### H17. BatchImportCli path traversal via unsanitized URL filename
**Issue:** [#222](https://github.com/Courant-Systems/shrewd/issues/222) (open, R1)

`downloadToTemp()` extracts a filename from a URL path and passes it unsanitized to `tempDir.resolve()`. A malicious manifest URL containing `../` can write files outside the temp directory.

---

## Medium Issues — 12 Open, 8 Closed

*Previously fixed: M1 (#161), M2 (#71), M3 (#162), M6 (#163), M9 (#169), M10 (#171), M11 (#173), M12 — all closed.*

### M4. Equation rename uses string token replacement instead of AST
**Issue:** [#131](https://github.com/Courant-Systems/shrewd/issues/131) (open, R1)

### M5. forrester-ui module has zero tests
**Issue:** [#145](https://github.com/Courant-Systems/shrewd/issues/145) (open, R1)

### M7. `return null` used extensively in exporters/importers instead of Optional
**Issue:** [#163](https://github.com/Courant-Systems/shrewd/issues/163) (open, R1)

61 occurrences of `return null` across production code.

### M8. ModelDefinition has 15 fields and 2 telescoping constructors
**Issue:** [#72](https://github.com/Courant-Systems/shrewd/issues/72) (open, R2)

### M13. CsvSubscriber uses string concatenation in SLF4J logging
**Issue:** [#175](https://github.com/Courant-Systems/shrewd/issues/175) (open, R2)

### M14. ChartViewerApplication.saveToFile throws RuntimeException — FIXED
**Issue:** [#176](https://github.com/Courant-Systems/shrewd/issues/176) (closed)

### M15. App module test coverage at 39.3% — many core classes at 0%
**Issue:** [#177](https://github.com/Courant-Systems/shrewd/issues/177) (open, R2)

26 classes with >300 instructions have <50% coverage.

### M16. ModelReport (engine) has 0% test coverage
**Issue:** [#178](https://github.com/Courant-Systems/shrewd/issues/178) (open, R1)

### M17. logger.xml misnamed — logging config never loaded
**Issue:** [#187](https://github.com/Courant-Systems/shrewd/issues/187) (open, R2)

### M18. String.format without Locale in chart rendering
**Issue:** [#188](https://github.com/Courant-Systems/shrewd/issues/188) (open, R2)

### M19. BatchImportCli.downloadToTemp accepts file:// URIs
**Issue:** [#189](https://github.com/Courant-Systems/shrewd/issues/189) (open, R1)

### M20. UndoManager serializes full model on FX thread
**Issue:** [#190](https://github.com/Courant-Systems/shrewd/issues/190) (open, R1)

### M21. ExportBounds.compute() returns invalid bounds for empty models
**Issue:** [#220](https://github.com/Courant-Systems/shrewd/issues/220) (open, R1)

When a model has no elements, min/max accumulators stay at their sentinel values, producing `Bounds` with `-Infinity` dimensions. Exporters will fail or produce invalid output.

### M22. FanChart uses unsynchronized static fields for cross-thread state passing
**Issue:** [#221](https://github.com/Courant-Systems/shrewd/issues/221) (open, R1)

`pendingResult` and `pendingVariableName` are written in `show()` and read in `start()` on different threads without `volatile` or synchronization. JMM does not guarantee visibility.

### M23. UndoManager compressor ExecutorService never shut down — thread leak
**Issue:** [#223](https://github.com/Courant-Systems/shrewd/issues/223) (open, R1)

Each `UndoManager` creates a single-threaded executor that is never shut down. Module navigation via `drillInto()` replaces the `UndoManager` without cleanup, leaking a thread per navigation.

---

## Low Issues — 14 Open, 1 Closed

### L1. `System.out::println` in Javadoc examples
### L2. Unused TODO comment in Quantity.java:185
### L3. Default branches in exhaustive enum switches (issue #78 — closed)
### L4. Color constants hardcoded and duplicated (#77)
### L5. SirCalibrationDemo uses System.out extensively
### L6. CsvSubscriber wraps IOException in generic RuntimeException
### L7. ModelReport creates new HashSet per recursive call

Plus 7 pre-existing low-severity issues tracked in GitHub.

---

## Test Results

```
Module              Tests   Failures  Errors  Skipped
forrester-engine    1,171   0         0       0
forrester-app       578     0         0       2
forrester-demos     44      0         0       0
forrester-tools     37      0         0       0
forrester-ui        0       —         —       —
TOTAL               1,830   0         0       2
```

SpotBugs: **0 bugs** (effort=Max, threshold=Medium).

---

## Security Assessment

| Category | Status |
|----------|--------|
| XXE protection | XML import and export both hardened (DocumentBuilderFactory + TransformerFactory) |
| Expression parser depth | Limited to MAX_DEPTH=200, prevents stack overflow |
| Graph traversal depth | FeedbackAnalysis: MAX_DEPTH=200. **DependencyGraph: NO depth guard** (#218) |
| Unsafe deserialization | None — no ObjectInputStream, no Serializable |
| Reflection abuse | None — no setAccessible, getDeclaredField |
| SQL injection | N/A — no database usage |
| Command injection | None — no Runtime.exec, ProcessBuilder |
| File path traversal | UI: FileChooser dialogs. **CLI: BatchImportCli lacks filename sanitization** (#222) |
| File size limits | All importers enforce 10 MB cap (Vensim, XMILE, JSON) |
| Simulation safety | Timeout (60s), MAX_STEPS (10M), NaN detection, cancellation support |
| Empty catch blocks | None in production code |
| printStackTrace calls | None in production code |
| Wildcard imports | None |

**Resource management:** All I/O uses try-with-resources or Files API. CsvSubscriber implements Closeable. Exporters use Files.writeString(). SweepCsvWriter uses try-with-resources throughout.

**Thread safety:** FX thread confinement enforced via checkFxThread() on all ModelEditor mutations. CopyOnWriteArrayList for listeners. AnalysisRunner uses Platform.runLater() for marshaling. **FanChart has unsynchronized static fields** (#221). **UndoManager leaks executor threads** (#223).

---

## Architecture Assessment

### Strengths

1. **Clean engine/app separation** — Engine has zero UI dependencies. Records used for all domain types.
2. **Well-structured interaction controllers** — Canvas interaction decomposed into 10+ focused controllers.
3. **Security hardened** — XXE protection, expression depth limits, file size limits, simulation safety guards.
4. **Consistent logging** — SLF4J throughout. No System.out/err or printStackTrace in production code.
5. **Defensive records** — Compact constructors with null-checks, List.copyOf(), and validation guards.
6. **Background threading** — Computation via AnalysisRunner with proper FX thread marshaling and assertions.
7. **Code hygiene** — No wildcard imports, 1 @SuppressWarnings, 1 TODO, no empty catch blocks.
8. **Two-phase Euler integration** — Stocks updated simultaneously from pre-step values (correct).
9. **Simulation safety** — Timeout, step limit, NaN detection, cancellation via Thread.interrupt().

### Weaknesses

1. **Test coverage gaps** — App module at 39.3%, 26 classes with <50% coverage. UI module has no tests.
2. **ModelEditor god class** — 1,367+ lines, 78+ public members. FX thread confinement works but class is too large.
3. **No Checkstyle or ErrorProne** — Only SpotBugs for static analysis.
4. **`return null` pattern** — 61 occurrences in production code, concentrated in importers/exporters.

### Largest Files

| File | Lines | Notes |
|------|-------|-------|
| ModelEditor.java | ~1,367 | 78 public members, god-class |
| ModelWindow.java | ~930 | Main window wiring |
| SvgExporter.java | ~835 | Rendering, partly duplicates CanvasRenderer |
| ModelDefinitionSerializer.java | ~774 | Hand-rolled JSON |
| ModelCanvas.java | ~783 | Recently refactored |
| CanvasRenderer.java | ~718 | Drawing code |
| XmileImporter.java | ~687 | XML parsing, well-tested (87.6%) |
| EquationAutoComplete.java | ~677 | Tokenization + completion + popup |
| InputDispatcher.java | ~649 | Event routing, 15% coverage |
| ExprCompiler.java | ~628 | Expression compilation, well-tested (88.9%) |

---

## Open Issues Summary

### By Milestone

| Milestone | Count |
|-----------|-------|
| R1 | 30 open issues |
| R2 | 27 open issues |
| Unassigned | 0 |

All open issues have been triaged to a milestone.

---

## Comparison: Previous Audit vs. Current

| Metric | Previous (start of session) | Current | Change |
|--------|---------------------------|---------|--------|
| Source files | 277 | 282 | +5 |
| Source LoC | 44,029 | 45,586 | +1,557 |
| Test LoC | 25,828 | 26,607 | +779 |
| Test:Source ratio | 0.59 | 0.58 | -0.01 |
| Tests passing | 1,732 | 1,830 | +98 |
| SpotBugs findings | 0 | 0 | — |
| Open critical issues | 4 | 0 | -4 (all fixed) |
| Open high issues | 4 | 3 | -1 |
| Open medium issues | 12 | 15 | +3 |
| Open low issues | 14 | 14 | — |
| Engine instruction coverage | 87.3% | 87.5% | +0.2% |
| App instruction coverage | 37.5% | 39.3% | +1.8% |

---

## Recommendations

### Short-term (R1)

1. **Add depth guard to DependencyGraph Tarjan SCC (#218)** — high severity, stack overflow risk
2. **Fix LookupForm mutation-before-validation (#219)** — high severity, data corruption
3. **Sanitize URL filenames in BatchImportCli (#222)** — high severity, path traversal
4. Fix ExportBounds crash on empty model (#220)
5. Add `volatile` to FanChart static fields (#221)
6. Shut down UndoManager compressor executor (#223)
7. Fix equation rename via AST (#131)
8. Add forrester-ui tests (#145)
9. Convert `return null` to Optional in io/ package (#163)
10. Add unit tests for ModelReport (#178)
11. Fix BatchImportCli URI validation (#189)
12. Move UndoManager serialization off FX thread (#190)
13. Fix ESC to dismiss autocomplete (#206)
14. Fix silent parsing failures in forms (#207)
15. Add end-to-end integration tests (#214)
16. Fix EquationAutoComplete.detach() in all forms (#201)

### Medium-term (R2)

11. Fix misnamed logger.xml (#187)
12. Fix String.format locale issues (#188)
13. Improve app module test coverage (#177)
14. Add Checkstyle/ErrorProne to build pipeline (#210, #211)
15. Add .editorconfig (#212)
16. ModelDefinition builder pattern (#72)
17. Reduce SvgExporter/CanvasRenderer duplication (#67)

### Ongoing

18. Monitor SpotBugs on every commit (enforced in CI)
19. Maintain zero-tolerance for empty catch blocks and printStackTrace
20. Track JaCoCo coverage per release
