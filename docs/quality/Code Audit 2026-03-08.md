# Code Audit & Quality Assessment — 2026-03-08 (Revised)

## Scope

Full code audit of the Forrester System Dynamics modeling platform covering all five modules:

| Module | Source Files | Source LoC | Test LoC | Test:Source Ratio |
|--------|-------------|-----------|----------|-------------------|
| forrester-engine | 150 | 20,732 | 15,577 | 0.75 |
| forrester-app | 85 | 18,160 | 8,622 | 0.47 |
| forrester-demos | 26 | 2,553 | — | 0.00 |
| forrester-tools | 8 | 1,279 | — | 0.00 |
| forrester-ui | 5 | 545 | — | 0.00 |
| **Total** | **274** | **43,269** | **24,199** | **0.56** |

**Build status:** 1,732 tests pass (2 skipped) across all modules. Clean compile. JaCoCo coverage reporting active.

**Static analysis:** SpotBugs reports **0 bugs** (down from 2).

---

## Summary of Findings

| Severity | Found | Fixed This Session | Remaining Open |
|----------|-------|-------------------|----------------|
| Critical | 3 | 3 | 0 |
| High | 10 | 10 | 0 |
| Medium | 12 | 1 | 16 |
| Low | 4 | 0 | 12 |

---

## Critical Issues — ALL FIXED

### C1. ModelCompiler silently swallows exceptions during stock initial-value resolution — FIXED
**File:** `forrester-engine/.../model/compile/ModelCompiler.java`
**Issue:** [#155](https://github.com/Courant-Systems/shrewd/issues/155) (closed)

`resolveInitialExpressions()` caught `Exception` with an empty body. Now logs a warning
with stock name, expression, and reason via SLF4J.

### C2. Immutable `List.of()` passed as resettables list in initial-expression compilation — FIXED
**File:** `forrester-engine/.../model/compile/ModelCompiler.java`
**Issue:** [#164](https://github.com/Courant-Systems/shrewd/issues/164) (closed)

`resolveInitialExpressions()` passed `List.of()` (immutable) to `ExprCompiler`. Now passes
`new ArrayList<>()`. The crash was previously masked by C1's empty catch block.

### C3. SubscriptExpander drops `initialExpression` field from StockDef — FIXED
**File:** `forrester-engine/.../model/compile/SubscriptExpander.java`
**Issue:** [#166](https://github.com/Courant-Systems/shrewd/issues/166) (closed)

Expanded stocks used the backward-compatible constructor that defaulted `initialExpression`
to null. Now uses the canonical 7-parameter constructor preserving all fields.

---

## High Issues — ALL FIXED

### H1. AnalysisRunner passes null to error handler when exception has no message — FIXED
**Issue:** [#156](https://github.com/Courant-Systems/shrewd/issues/156) (closed)

Changed fallback from `null` to `e.toString()`, which always includes the exception class name.

### H2. VensimExporter builds a useless `flowRouteNames` HashSet — FIXED
**Issue:** [#157](https://github.com/Courant-Systems/shrewd/issues/157) (closed)

Removed the dead `HashSet` and unused `FlowRoute` import. Flow valves are already classified
via `ElementPlacement.type()`.

### H3. XmileExporter does not set XXE protections on DocumentBuilderFactory — FIXED
**Issue:** [#158](https://github.com/Courant-Systems/shrewd/issues/158) (closed)

Added `disallow-doctype-decl`, `external-general-entities`, and `external-parameter-entities`
features matching `XmileImporter`.

### H4. XmileExporter TransformerFactory is not hardened — FIXED
**Issue:** [#159](https://github.com/Courant-Systems/shrewd/issues/159) (closed)

Added `ACCESS_EXTERNAL_DTD` and `ACCESS_EXTERNAL_STYLESHEET` attributes.

### H5. FileController catches broad Exception in openExample — FIXED
**Issue:** [#160](https://github.com/Courant-Systems/shrewd/issues/160) (closed)

Changed `catch (Exception)` to `catch (IOException)` in `openExample`. `saveToFile` already
caught `IOException` specifically.

### H6. Window close bypasses unsaved-changes check — FIXED
**Issue:** [#165](https://github.com/Courant-Systems/shrewd/issues/165) (closed)

Added `stage.setOnCloseRequest()` handler that calls `confirmDiscardChanges()` and consumes the
event if the user cancels.

### H7. INT function truncates large doubles silently — FIXED
**Issue:** [#167](https://github.com/Courant-Systems/shrewd/issues/167) (closed)

Changed from `(double)(long)` cast to truncation-toward-zero via `Math.floor`/`Math.ceil`,
which handles values outside `long` range correctly and matches Vensim semantics.

### H8. Lookup rewrite only replaces first occurrence in equation — FIXED
**Issue:** [#168](https://github.com/Courant-Systems/shrewd/issues/168) (closed)

Changed `if (m.find())` to `while (m.find())` with matcher re-creation after each replacement
in `VensimExprTranslator.rewriteLookupCalls()`.

### H9. SimulationRunner drops CLD variables during model export — FIXED
**Issue:** [#170](https://github.com/Courant-Systems/shrewd/issues/170) (closed)

Changed from the 12-parameter backward-compatible `ModelDefinition` constructor (which defaulted
CLD fields to empty) to the full constructor preserving `cldVariables` and `causalLinks`.

### H10. CLI arguments crash with ArrayIndexOutOfBoundsException — FIXED
**Issue:** [#172](https://github.com/Courant-Systems/shrewd/issues/172) (closed)

Added bounds checking before `args[++i]` for `--manifest` and `--output-dir` flags, with
user-friendly error messages and usage output.

---

## Medium Issues — 16 Open

### M1. ModelEditor has 78 public methods and 1,365 lines
**File:** `forrester-app/.../canvas/ModelEditor.java`
**Issue:** [#161](https://github.com/Courant-Systems/shrewd/issues/161)

Largest file in app module. Combines element CRUD, equation management, module operations,
connector generation, validation, and model definition export.

### M2. No synchronization on ModelEditor despite being accessed from background threads
**File:** `forrester-app/.../canvas/ModelEditor.java` (existing issue #71)

`AnalysisRunner` passes the editor to background tasks while the FX thread continues to allow edits.

### M3. Broad `catch (Exception)` in remaining locations — PARTIALLY FIXED
**Issue:** [#162](https://github.com/Courant-Systems/shrewd/issues/162)

Fixed in `ModelCompiler` (C1) and `FileController.openExample` (H5). Remaining:
- `AnalysisRunner.java:51,69` — catches `Exception` from `Callable.call()` (acceptable since
  `Callable` throws checked `Exception`)
- `FileController.java:171` — example catalog loading (low risk)

### M4. Equation rename uses string token replacement instead of AST
**(Existing issue #131)**

### M5. forrester-ui module has zero tests
**(Existing issue #145)**

### M6. forrester-tools and forrester-demos have zero tests
**Issue:** [#163](https://github.com/Courant-Systems/shrewd/issues/163)

### M7. `return null` used extensively in exporters instead of Optional
Multiple files in `io/` package.

### M8. ModelDefinition has 15 fields and 3 telescoping constructors
**(Existing issue #72)**

### M9. Command palette can orphan help windows
**Issue:** [#169](https://github.com/Courant-Systems/shrewd/issues/169)

### M10. ModelEditor lacks FX thread assertion
**Issue:** [#171](https://github.com/Courant-Systems/shrewd/issues/171)

### M11. ImportPipeline crashes on extensionless filenames
**Issue:** [#173](https://github.com/Courant-Systems/shrewd/issues/173)

### M12. HttpClient resource leak in BatchImportCli
**File:** `forrester-tools/.../cli/BatchImportCli.java`

---

## Low Issues — 12 Open

### L1. `System.out::println` in Javadoc examples
### L2. Unused TODO comment in Quantity.java
### L3. Default branches in exhaustive enum switches (existing issue #78)
### L4. Color constants hardcoded and duplicated (existing issue #77)

Plus 8 pre-existing low-severity issues tracked in GitHub.

---

## Test Results After Fixes

```
Module              Tests   Failures  Errors  Skipped
forrester-engine    1,102   0         0       0
forrester-app       549     0         0       2
forrester-demos     44      0         0       0
forrester-tools     37      0         0       0
forrester-ui        0       —         —       —
TOTAL               1,732   0         0       2
```

SpotBugs: **0 bugs** (both previous findings resolved by H2 and C1/C2).

---

## Architecture Assessment

### Strengths

1. **Clean engine/app separation** — Engine module is pure Java with no UI dependencies. Records
   used for all domain types (ModelDefinition, StockDef, etc.) ensuring immutability.

2. **Well-structured interaction controllers** — Canvas interaction is decomposed into 10+
   focused controllers (DragController, MarqueeController, ResizeController, etc.) plus
   InputDispatcher and SelectionController.

3. **Security** — XXE protection on XML import and export, expression parser depth limiting
   (MAX_DEPTH=200), no raw SQL, no command injection vectors. TransformerFactory hardened.

4. **Logging** — Consistent SLF4J usage throughout. No `System.out/err` or `printStackTrace()`
   in production code. ModelCompiler now logs initial-expression failures.

5. **Defensive records** — `ModelDefinition` compact constructor null-checks all list fields
   and wraps them with `List.copyOf()`.

6. **Background threading** — All heavy computation (simulation, sweep, Monte Carlo, optimization)
   goes through `AnalysisRunner` which properly marshals results back to the FX thread via
   `Platform.runLater()`.

7. **No wildcard imports, no `@SuppressWarnings` abuse** (only 1 occurrence in the codebase).

8. **Data safety** — Window close now checks for unsaved changes via OS close button.

### Weaknesses

1. **Test coverage gaps** — 3 of 5 modules have zero tests. Test:source ratio is 0.56 overall.

2. **ModelEditor is still large** (1,365 lines, 78 public methods) — biggest remaining god-class.

3. **No Checkstyle or ErrorProne** — SpotBugs catches some issues but misses style violations.

4. **`return null` pattern** in importers/exporters instead of Optional, contrary to guidelines.

### Largest Files (potential refactoring targets)

| File | Lines | Notes |
|------|-------|-------|
| ModelEditor.java | 1,365 | 78 public methods, god-class |
| ModelWindow.java | 922 | Main window wiring, acceptable |
| SvgExporter.java | 835 | Rendering logic, partly duplicates CanvasRenderer |
| ModelDefinitionSerializer.java | 762 | Hand-rolled JSON, could use Jackson annotations |
| ModelCanvas.java | 744 | Recently refactored from 1,555 |
| CanvasRenderer.java | 718 | Drawing code, acceptable for its role |

---

## Comparison: Start of Session vs. After Fixes

| Metric | Before Fixes | After Fixes | Change |
|--------|-------------|-------------|--------|
| Tests passing | 1,732 | 1,732 | No regressions |
| SpotBugs findings | 2 | 0 | -2 (all resolved) |
| Open critical issues | 3 | 0 | -3 (all fixed) |
| Open high issues | 10 | 0 | -10 (all fixed) |
| Open medium issues | 17 | 16 | -1 (M3 partially fixed) |
| Open low issues | 12 | 12 | No change |

---

## Recommendations

### Short-term (R1 milestone)

1. Add basic tests for forrester-tools and forrester-demos modules (#163)
2. Refactor ModelEditor to reduce public API surface (#161)
3. Add FX thread assertions to ModelEditor mutation methods (#171)
4. Fix ImportPipeline extensionless filename crash (#173)
5. Fix command palette orphan windows (#169)

### Ongoing

6. Add Checkstyle or ErrorProne to the build pipeline
7. Track and improve JaCoCo coverage metrics
8. Convert `return null` patterns to Optional in io/ package
9. Address ModelEditor thread safety (existing #71)
