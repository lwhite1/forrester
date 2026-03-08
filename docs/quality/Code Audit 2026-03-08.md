# Code Audit & Quality Assessment — 2026-03-08 (Second Revision)

## Scope

Full code audit of the Forrester System Dynamics modeling platform covering all five modules:

| Module | Source Files | Source LoC | Test Files | Test LoC | Test:Source Ratio |
|--------|-------------|-----------|------------|----------|-------------------|
| forrester-engine | 152 | 21,139 | 76 | 15,651 | 0.74 |
| forrester-app | 86 | 18,337 | 36 | 8,622 | 0.47 |
| forrester-demos | 26 | 2,683 | 6 | 962 | 0.36 |
| forrester-tools | 8 | 1,325 | 5 | 593 | 0.45 |
| forrester-ui | 5 | 545 | 0 | 0 | 0.00 |
| **Total** | **277** | **44,029** | **123** | **25,828** | **0.59** |

**Build status:** 1,732 tests pass (2 skipped) across all modules. Clean compile.

**Static analysis:** SpotBugs reports **0 bugs** (effort=Max, threshold=Medium).

---

## JaCoCo Coverage Report

| Module | Instruction Coverage | Branch Coverage | Line Coverage |
|--------|---------------------|----------------|---------------|
| forrester-engine | 33,568 / 38,453 (87.3%) | 2,937 / 3,914 (75.0%) | 6,887 / 7,934 (86.8%) |
| forrester-app | 17,467 / 46,549 (37.5%) | 1,024 / 3,105 (33.0%) | 3,455 / 9,062 (38.1%) |
| forrester-tools | 1,612 / 2,979 (54.1%) | 150 / 291 (51.5%) | 338 / 663 (51.0%) |
| forrester-demos | 2,003 / 5,188 (38.6%) | 6 / 42 (14.3%) | 416 / 1,114 (37.3%) |
| forrester-ui | — | — | — |

### Engine Coverage by Package

| Package | Instruction Coverage | Branch Coverage |
|---------|---------------------|----------------|
| model | 5,154 / 5,711 (90.2%) | 376 / 445 (84.5%) |
| model.expr | 1,927 / 2,060 (93.5%) | 248 / 288 (86.1%) |
| model.graph | 3,874 / 4,181 (92.7%) | 420 / 494 (85.0%) |
| model.compile | 3,893 / 4,416 (88.2%) | 320 / 412 (77.7%) |
| sweep | 3,728 / 4,277 (87.2%) | 252 / 338 (74.6%) |
| io.xmile | 3,276 / 3,744 (87.5%) | 316 / 473 (66.8%) |
| io.vensim | 4,165 / 4,879 (85.4%) | 408 / 591 (69.0%) |
| io.json | 1,827 / 2,330 (78.4%) | 166 / 234 (70.9%) |
| io (top-level) | 202 / 609 (33.2%) | 12 / 46 (26.1%) |
| measure (all) | 1,396 / 1,539 (90.7%) | 47 / 72 (65.3%) |

### App Module — Low Coverage Classes (>300 instructions, <50% covered)

| Class | Coverage | Notes |
|-------|----------|-------|
| QuickstartDialog | 0 / 1,224 (0%) | Dialog — TestFX candidate |
| ExpressionLanguageDialog | 0 / 1,167 (0%) | Dialog |
| SdConceptsDialog | 0 / 926 (0%) | Dialog |
| LookupForm | 0 / 979 (0%) | Form |
| MultiSweepResultPane | 0 / 743 (0%) | Result pane |
| CausalLinkGeometry | 0 / 705 (0%) | Geometry calculations |
| FanChartPane | 0 / 692 (0%) | Chart pane |
| KeyboardShortcutsDialog | 0 / 506 (0%) | Dialog |
| SelectionRenderer | 0 / 479 (0%) | Renderer |
| OptimizationResultPane | 0 / 473 (0%) | Result pane |
| SweepResultPane | 0 / 481 (0%) | Result pane |
| StockForm | 0 / 471 (0%) | Form |
| FlowForm | 0 / 444 (0%) | Form |
| FeedbackLoopRenderer | 0 / 378 (0%) | Renderer |
| AuxForm | 0 / 363 (0%) | Form |
| ConstantForm | 0 / 352 (0%) | Form |
| DiagramExporter | 0 / 330 (0%) | PNG/SVG export bridge |
| SvgExporter | 56 / 3,271 (2%) | SVG rendering |
| SimulationController | 24 / 948 (3%) | Simulation orchestration |
| InputDispatcher | 222 / 1,415 (16%) | Event routing |
| PropertiesPanel | 225 / 1,166 (19%) | Properties sidebar |
| FileController | 183 / 825 (22%) | File I/O |
| ModelCanvas | 365 / 1,404 (26%) | Main canvas |
| CanvasRenderer | 487 / 1,797 (27%) | Drawing |

---

## Summary of Findings

| Severity | Total Found (All Time) | Open | Closed Since Last Audit |
|----------|----------------------|------|------------------------|
| Critical | 3 prior + 4 new = 7 | 0 | 4 (C4, C5, C6, C7) |
| High | 10 prior + 4 new = 14 | 4 | — |
| Medium | 16 prior + 4 new = 20 | 12 | 8 (M1, M2, M3, M6, M9, M10, M11, M12) |
| Low | 12 + 3 new = 15 | 14 | 1 (L3) |

---

## Critical Issues — ALL FIXED

*Previously fixed: C1 (#155), C2 (#164), C3 (#166), C4 (#179), C5 (#180), C6 (#181), C7 (#182) — all closed.*

---

## High Issues — 4 New, 10 Previously Fixed

### H1–H10: Previously fixed
[#156](https://github.com/Courant-Systems/shrewd/issues/156),
[#157](https://github.com/Courant-Systems/shrewd/issues/157),
[#158](https://github.com/Courant-Systems/shrewd/issues/158),
[#159](https://github.com/Courant-Systems/shrewd/issues/159),
[#160](https://github.com/Courant-Systems/shrewd/issues/160),
[#165](https://github.com/Courant-Systems/shrewd/issues/165),
[#167](https://github.com/Courant-Systems/shrewd/issues/167),
[#168](https://github.com/Courant-Systems/shrewd/issues/168),
[#170](https://github.com/Courant-Systems/shrewd/issues/170),
[#172](https://github.com/Courant-Systems/shrewd/issues/172) — all closed.

### H11. ExprCompiler division-by-zero warning floods logs with no throttling *(new)*
**File:** `forrester-engine/.../model/compile/ExprCompiler.java:99-103`
**Issue:** [#183](https://github.com/Courant-Systems/shrewd/issues/183)

DIV/MOD/SQRT/LN/POWER lambdas log warnings on every timestep. A 10,000-step simulation
with persistent zero divisor produces 10,000 identical log lines, degrading performance.

### H12. XmileImporter.getFirstChild searches entire subtree instead of direct children *(new)*
**File:** `forrester-engine/.../io/xmile/XmileImporter.java:555-568`
**Issue:** [#184](https://github.com/Courant-Systems/shrewd/issues/184)

`getElementsByTagNameNS` searches the full DOM subtree. For XMILE files with nested models
containing same-named elements, this can return wrong elements from deeper nesting levels.

### H13. ImportPipelineCli crashes on missing flag values (AIOOBE) *(new)*
**File:** `forrester-tools/.../ImportPipelineCli.java:142-153`
**Issue:** [#185](https://github.com/Courant-Systems/shrewd/issues/185)

`args[++i]` without bounds check for 8 value-taking flags. `BatchImportCli` handles this
properly but `ImportPipelineCli` does not.

### H14. ModelDefinitionSerializer.fromFile has no file size limit — potential OOM *(new)*
**File:** `forrester-engine/.../io/json/ModelDefinitionSerializer.java:114`
**Issue:** [#186](https://github.com/Courant-Systems/shrewd/issues/186)

`Files.readString(path)` with no size check. Both Vensim and XMILE importers have
`MAX_FILE_SIZE` (10 MB) but JSON deserializer does not.

---

## Medium Issues — 12 Open, 8 Closed

*Previously fixed: M1 (#161), M2 (#71), M3 (#162), M6 (#163), M9 (#169), M10 (#171), M11 (#173), M12 — all closed.*

### M4. Equation rename uses string token replacement instead of AST
**Issue:** [#131](https://github.com/Courant-Systems/shrewd/issues/131) (open)

### M5. forrester-ui module has zero tests
**Issue:** [#145](https://github.com/Courant-Systems/shrewd/issues/145) (open)

### M7. `return null` used extensively in exporters/importers instead of Optional
**Issue:** [#163](https://github.com/Courant-Systems/shrewd/issues/163) (open)

55 occurrences of `return null` across production code. Concentrated in:
- `XmileExporter.java` — 8 occurrences
- `VensimExporter.java` — 7 occurrences
- `XmileImporter.java` — 5 occurrences
- `FlowEndpointCalculator.java` — 5 occurrences
- `HitTester.java` — 3 occurrences
- Various dialog classes — acceptable for JavaFX dialog patterns

### M8. ModelDefinition has 15 fields and 2 telescoping constructors
**Issue:** [#72](https://github.com/Courant-Systems/shrewd/issues/72) (open, milestone R2)

Down from 3 constructors to 2. Builder proposed.

### M13. CsvSubscriber uses string concatenation in SLF4J logging *(new)*
**File:** `forrester-engine/.../io/CsvSubscriber.java:86`
**Issue:** [#175](https://github.com/Courant-Systems/shrewd/issues/175)

`logger.info("Starting simulation: " + event.getModel().getName())` should use parameterized
logging: `logger.info("Starting simulation: {}", event.getModel().getName())`.

### M14. ChartViewerApplication.saveToFile throws RuntimeException on I/O failure *(new)*
**File:** `forrester-ui/.../ChartViewerApplication.java:241`
**Issue:** [#176](https://github.com/Courant-Systems/shrewd/issues/176)

`saveToFile()` wraps `IOException` in `RuntimeException`, which can crash the application.
Should show an error dialog instead, consistent with other UI error handling.

### M15. App module test coverage at 37.5% — many core classes at 0% *(new)*
**Issue:** [#177](https://github.com/Courant-Systems/shrewd/issues/177)

24 classes with >300 instructions have 0% coverage. Key testable classes include:
`CausalLinkGeometry`, `SimulationController`, `FormContext`, forms (Stock/Flow/Aux/Constant).
Dialogs are harder to test but geometry and controller logic can be unit-tested.

### M16. ModelReport (engine) has 0% test coverage *(new)*
**File:** `forrester-engine/.../io/ModelReport.java`
**Issue:** [#178](https://github.com/Courant-Systems/shrewd/issues/178)

376 instructions, 0% covered. Pure logic class (builds text from model structure) that is
straightforward to unit-test.

### M17. logger.xml misnamed and contains boilerplate — logging config never loaded *(new)*
**File:** `forrester-engine/src/main/resources/logger.xml`
**Issue:** [#187](https://github.com/Courant-Systems/shrewd/issues/187)

Logback expects `logback.xml` but file is named `logger.xml`. Contains leftover
`com.lordofthejars.foo` logger and root=DEBUG. App module has no logback config at all.

### M18. String.format without Locale in 3 UI rendering call sites *(new)*
**Files:** `FanChart.java:177`, `FanChartPane.java:168`, `ChartUtils.java:42`
**Issue:** [#188](https://github.com/Courant-Systems/shrewd/issues/188)

`String.format("%.0f", val)` without `Locale` produces locale-dependent output. On systems
with comma decimal separator, chart labels display incorrectly.

### M19. BatchImportCli.downloadToTemp accepts file:// URIs — local file read *(new)*
**File:** `forrester-tools/.../BatchImportCli.java:197`
**Issue:** [#189](https://github.com/Courant-Systems/shrewd/issues/189)

No URI scheme validation. A malicious manifest could cause reading of arbitrary local files.

### M20. UndoManager serializes full model on FX thread — jank during drag *(new)*
**File:** `forrester-app/.../canvas/UndoManager.java`
**Issue:** [#190](https://github.com/Courant-Systems/shrewd/issues/190)

Full JSON serialization + GZIP on every `saveUndoState()`, including first pixel of drag.
Blocks FX thread for large models.

---

## Low Issues — 14 Open, 1 Closed

### L1. `System.out::println` in Javadoc examples
### L2. Unused TODO comment in Quantity.java:185
### L3. Default branches in exhaustive enum switches (issue #78 — closed)
### L4. Color constants hardcoded and duplicated (existing issue #77)
### L5. SirCalibrationDemo uses System.out extensively *(new)*
**File:** `forrester-demos/.../SirCalibrationDemo.java`

10 `System.out.println` calls for demo output. Acceptable for CLI demos but inconsistent with
SLF4J logging convention.

### L6. CsvSubscriber wraps IOException in generic RuntimeException *(new)*
**File:** `forrester-engine/.../io/CsvSubscriber.java:53`

Constructor wraps `IOException` in bare `RuntimeException`. Could use a custom
`CsvWriterException` or `UncheckedIOException` (from JDK) for better error typing.

### L7. ModelReport creates new HashSet per recursive call *(new)*
**File:** `forrester-engine/.../io/ModelReport.java:121`

`new HashSet<>(visited)` on every recursive call. Harmless for small models but could be
improved to use a single mutable `Set` passed through.

Plus 8 pre-existing low-severity issues tracked in GitHub.

---

## Test Results

```
Module              Tests   Failures  Errors  Skipped
forrester-engine    1,102   0         0       0
forrester-app       549     0         0       2
forrester-demos     44      0         0       0
forrester-tools     37      0         0       0
forrester-ui        0       —         —       —
TOTAL               1,732   0         0       2
```

SpotBugs: **0 bugs** (effort=Max, threshold=Medium).

---

## Security Assessment

| Category | Status |
|----------|--------|
| XXE protection | XML import and export both hardened (DocumentBuilderFactory + TransformerFactory) |
| Expression parser depth | Limited to MAX_DEPTH=200, prevents stack overflow |
| Unsafe deserialization | None — no ObjectInputStream, no Serializable |
| Reflection abuse | None — no setAccessible, getDeclaredField, etc. |
| SQL injection | N/A — no database usage |
| Command injection | None — no Runtime.exec, ProcessBuilder, or shell invocation |
| File path traversal | File operations use user-selected FileChooser dialogs |
| Empty catch blocks | None in production code |
| printStackTrace calls | None in production code |
| Wildcard imports | None |

---

## Architecture Assessment

### Strengths

1. **Clean engine/app separation** — Engine module is pure Java with no UI dependencies. Records
   used for all domain types ensuring immutability.

2. **Well-structured interaction controllers** — Canvas interaction decomposed into 10+ focused
   controllers plus InputDispatcher and SelectionController.

3. **Security hardened** — XXE protection, expression parser depth limits, no injection vectors,
   TransformerFactory hardened.

4. **Consistent logging** — SLF4J throughout production code. No `System.out/err` or
   `printStackTrace()` in production code (only in CLI tools and demos, where appropriate).

5. **Defensive records** — `ModelDefinition` compact constructor null-checks and wraps with
   `List.copyOf()`.

6. **Background threading** — All heavy computation goes through `AnalysisRunner` with proper
   `Platform.runLater()` marshaling and FX thread assertions on mutations.

7. **Code hygiene** — No wildcard imports, only 1 `@SuppressWarnings` (justified unchecked cast
   in EquationAutoComplete), 1 TODO, no empty catch blocks.

8. **Data safety** — Window close checks for unsaved changes. FX thread enforcement on mutations.

### Weaknesses

1. **Test coverage gaps** — App module at 37.5%, 24 classes with 0% coverage. UI module has no
   tests at all.

2. **ModelEditor god class** — 1,367 lines, 79 public members. Thread safety relies on FX thread
   confinement but `toModelDefinition()` reads state from background threads.

3. **No Checkstyle or ErrorProne** — Only SpotBugs for static analysis.

4. **`return null` pattern** — 55 occurrences in production code, concentrated in importers/exporters.

### Largest Files (potential refactoring targets)

| File | Lines | Notes |
|------|-------|-------|
| ModelEditor.java | 1,367 | 79 public members, god-class |
| ModelWindow.java | 922 | Main window wiring, acceptable |
| SvgExporter.java | 835 | Rendering logic, partly duplicates CanvasRenderer |
| ModelDefinitionSerializer.java | 767 | Hand-rolled JSON |
| ModelCanvas.java | 744 | Recently refactored from 1,555 |
| CanvasRenderer.java | 718 | Drawing code, acceptable |
| XmileImporter.java | 687 | XML parsing, complex but well-tested (87.5%) |
| InputDispatcher.java | 649 | Event routing, 16% coverage |
| VensimExporter.java | 636 | Format export, well-tested (85.4%) |
| ExprCompiler.java | 628 | Expression compilation, well-tested (88.2%) |

---

## Comparison: Previous Audit vs. Current

| Metric | Previous | Current | Change |
|--------|----------|---------|--------|
| Source files | 274 | 277 | +3 |
| Source LoC | 43,269 | 44,029 | +760 |
| Test LoC | 24,199 | 25,828 | +1,629 |
| Test:Source ratio | 0.56 | 0.59 | +0.03 |
| Tests passing | 1,732 | 1,732 | — |
| SpotBugs findings | 0 | 0 | — |
| Open critical issues | 0 | 0 | +4 found, +4 fixed (C4–C7) |
| Open high issues | 0 | 4 | +4 new (H11–H14) |
| Open medium issues | 16 | 12 | +4 new, -8 closed |
| Open low issues | 12 | 15 | +3 new |
| Engine instruction coverage | — | 87.3% | baseline |
| App instruction coverage | — | 37.5% | baseline |

---

## Recommendations

### Short-term (high + medium — R1 milestone)

1. **Throttle ExprCompiler diagnostic logging** — per-timestep warning floods (#183)
2. **Fix XmileImporter.getFirstChild** subtree search — wrong elements returned (#184)
3. **Bounds-check ImportPipelineCli flag parsing** — AIOOBE on missing values (#185)
4. **Add file size limit to ModelDefinitionSerializer** — OOM on large files (#186)
5. Fix misnamed `logger.xml` → `logback.xml` and clean up boilerplate (#187)
6. Fix `String.format` locale issues in chart rendering (#188)
7. Validate URI scheme in `BatchImportCli.downloadToTemp` (#189)
8. Move `UndoManager` serialization off FX thread (#190)
9. Improve app module test coverage for non-UI logic classes (#177)
10. Add unit tests for ModelReport (#178)

### Medium-term (R2)

15. Convert `return null` patterns to Optional in io/ package (#163)
16. Add equation rename via AST (#131)
17. Add forrester-ui tests (#145)
18. Add Checkstyle or ErrorProne to the build pipeline
19. Add toBuilder() to ModelDefinition (#72)
20. Reduce SvgExporter / CanvasRenderer duplication (#67)
21. Track and improve JaCoCo coverage metrics per release

### Ongoing

22. Monitor SpotBugs on every commit (currently enforced in CI)
23. Maintain zero-tolerance for empty catch blocks and printStackTrace
