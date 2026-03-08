# Code Audit & Quality Assessment — 2026-03-08

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

**Build status:** 549 tests pass (2 skipped). Clean compile. JaCoCo coverage reporting active.

**Static analysis:** SpotBugs reports **2 bugs** in forrester-engine (see findings below).

---

## Summary of Findings

| Severity | New Issues | Pre-Existing Issues | Total Open |
|----------|-----------|-------------------|------------|
| Critical | 1 | 0 | 1 |
| High | 4 | 1 | 5 |
| Medium | 8 | 5 | 13 |
| Low | 4 | 8 | 12 |

---

## Critical Issues

### C1. ModelCompiler silently swallows exceptions during stock initial-value resolution
**File:** `forrester-engine/.../model/compile/ModelCompiler.java:268`
**SpotBugs:** DE_MIGHT_IGNORE

The `resolveInitialExpressions()` method catches `Exception` with an empty body and a comment
"Fall back to numeric initialValue." This means:
- If a stock's initial expression references an undefined variable, no warning is produced
- The stock silently uses its numeric default (often 0), producing wrong simulation results
- Users have no way to know their initial expression was ignored

This can cause silent data corruption in simulation output — models appear to run fine but produce
incorrect results with no error indication.

**Fix:** Log a warning with the stock name, expression, and exception message. Consider adding it
to the `CompilationContext.warnings()` list so the UI can surface it.

---

## High Issues

### H1. AnalysisRunner passes null to error handler when exception has no message
**File:** `forrester-app/.../canvas/AnalysisRunner.java:55`

```java
errorHandler.accept(errorTitle,
    e.getMessage() != null ? e.getMessage() : null);
```

The ternary always evaluates to `e.getMessage()` (which is already null when null). More importantly,
NullPointerException, ArrayIndexOutOfBoundsException, and other unchecked exceptions often have null
messages, so the error dialog will display an empty body. Users will see "Simulation failed" with no
indication of what went wrong.

**Fix:** Use `e.toString()` as fallback, which always includes the exception class name.

### H2. VensimExporter builds a useless `flowRouteNames` HashSet
**File:** `forrester-engine/.../io/vensim/VensimExporter.java:318`
**SpotBugs:** UC_USELESS_OBJECT

A `HashSet<String>` is populated in `buildSketchView()` but never read. This is either dead code
from an incomplete implementation, or a bug where the set was meant to be used for type-11
classification of flow valve elements but was forgotten.

**Impact:** If this set was meant to distinguish flow valves in the sketch output, the Vensim
export may produce incorrect element type codes.

**Fix:** Investigate whether flows need type-11 classification; either use the set or remove it.

### H3. XmileExporter does not set XXE protections on DocumentBuilderFactory
**File:** `forrester-engine/.../io/xmile/XmileExporter.java:83`

The importer (`XmileImporter`) correctly disables external entities, but the exporter creates a
`DocumentBuilderFactory` without setting security features. While the exporter builds documents
from trusted internal data (not parsing external input), this is inconsistent and could become a
vector if the code is ever refactored to also parse.

**Fix:** Add the same 3 lines of XXE protection as in `XmileImporter.parseXml()`.

### H4. XmileExporter TransformerFactory is not hardened
**File:** `forrester-engine/.../io/xmile/XmileExporter.java:533`

`TransformerFactory.newInstance()` is used without setting `ACCESS_EXTERNAL_DTD` and
`ACCESS_EXTERNAL_STYLESHEET` attributes. OWASP recommends:
```java
tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
```

**Fix:** Add the two setAttribute calls.

### H5. FileController catches broad Exception in openExample and saveToFile
**File:** `forrester-app/.../app/FileController.java:171,320`

Catching `Exception` masks unexpected failures (ClassCastException, IllegalStateException) that
indicate programming errors. These should propagate or at minimum be logged at ERROR level.
Currently they're displayed as user-facing messages, which may not be actionable.

**Fix:** Catch `IOException | JsonProcessingException` specifically, and let runtime exceptions
propagate.

---

## Medium Issues

### M1. ModelEditor has 78 public methods and 1,365 lines
**File:** `forrester-app/.../canvas/ModelEditor.java`

This is the largest file in the app module. It combines element CRUD, equation management,
module operations, connector generation, validation, and model definition export. The public
API surface is unwieldy.

**Fix:** Extract element-type-specific operations into strategy classes (e.g., `StockOperations`,
`FlowOperations`). The connector generation logic (~200 lines) could also be its own class.

### M2. No synchronization on ModelEditor despite being accessed from background threads
**File:** `forrester-app/.../canvas/ModelEditor.java` (see existing issue #71)

`AnalysisRunner` passes the editor to background tasks for simulation compilation while the FX
thread continues to allow edits. There's no defensive copy or synchronization.

**Impact:** Concurrent modification during simulation could corrupt model state.

### M3. Broad `catch (Exception)` in 5 locations across production code
**Files:**
- `ModelCompiler.java:268` (Critical — see C1)
- `AnalysisRunner.java:51,69`
- `FileController.java:171,320`

Per project guidelines, catch specific exception types. Broad catch masks programming errors.

### M4. Equation rename uses string token replacement instead of AST
**(Existing issue #131)**

`ModelEditor.renameInEquation()` does string replacement, which can corrupt backtick-quoted
identifiers or identifiers that are substrings of other identifiers (e.g., renaming "Rate" also
matches "Birth_Rate").

### M5. forrester-ui module has zero tests
**(Existing issue #145)**

5 source files, 545 lines, no test directory at all.

### M6. forrester-tools and forrester-demos have zero tests

The tools module includes HTTP download logic (BatchImportCli) and Vensim model translation.
The demos module has 26 model definitions. Neither has any tests.

### M7. `return null` used extensively in exporters instead of Optional
**Files:**
- `VensimExporter.java` — 8 occurrences
- `XmileExporter.java` — 7 occurrences
- `XmileImporter.java` — 3 occurrences
- `VensimExprTranslator.java` — 1 occurrence

Per project guidelines, prefer `Optional` for methods that may return absent values.

### M8. ModelDefinition has 15 fields and 3 telescoping constructors
**(Existing issue #72)**

Adding new fields requires updating all 3 constructors plus every call site that uses the
backward-compatible constructors. A builder or `toBuilder()` pattern would reduce churn.

---

## Low Issues

### L1. `System.out::println` in Javadoc examples
**Files:** `VensimImporter.java:43`, `XmileImporter.java:52`

Javadoc usage examples show `result.warnings().forEach(System.out::println)`. Minor, but
could mislead users of the library.

### L2. Unused TODO comment
**File:** `forrester-engine/.../measure/Quantity.java:185`

```java
// TODO(lwhite): Extend this to handle inherited compatibility
```

Should be converted to a GitHub issue or removed.

### L3. Default branches in exhaustive enum switches
**(Existing issue #78)**

Several switch statements on enums have `default -> {}` branches that will silently
ignore new enum values added in the future.

### L4. Color constants hardcoded and duplicated
**(Existing issue #77)**

Colors are defined inline across CanvasRenderer, ElementRenderer, ConnectionRenderer,
FeedbackLoopRenderer, and SelectionRenderer.

---

## Architecture Assessment

### Strengths

1. **Clean engine/app separation** — Engine module is pure Java with no UI dependencies. Records
   used for all domain types (ModelDefinition, StockDef, etc.) ensuring immutability.

2. **Well-structured interaction controllers** — Canvas interaction is decomposed into 10+
   focused controllers (DragController, MarqueeController, ResizeController, etc.) plus the
   new InputDispatcher and SelectionController from today's refactoring.

3. **Security** — XXE protection on XML import, expression parser depth limiting (MAX_DEPTH=200),
   no raw SQL, no command injection vectors.

4. **Logging** — Consistent SLF4J usage throughout. No `System.out/err` or `printStackTrace()`
   in production code.

5. **Defensive records** — `ModelDefinition` compact constructor null-checks all list fields
   and wraps them with `List.copyOf()`.

6. **Background threading** — All heavy computation (simulation, sweep, Monte Carlo, optimization)
   goes through `AnalysisRunner` which properly marshals results back to the FX thread via
   `Platform.runLater()`.

7. **No wildcard imports, no `@SuppressWarnings` abuse** (only 1 occurrence in the codebase).

### Weaknesses

1. **Test coverage gaps** — 3 of 5 modules have zero tests. Test:source ratio is 0.56 overall,
   dragged down by untested modules.

2. **ModelEditor is still large** (1,365 lines, 78 public methods) — biggest remaining god-class.

3. **No Checkstyle or ErrorProne** — SpotBugs catches some issues but misses style violations
   and common Java pitfalls.

4. **Exception handling inconsistency** — Some code catches specific types, some catches
   `Exception`. The ModelCompiler silently swallows exceptions.

5. **`return null` pattern** in importers/exporters instead of Optional, contrary to guidelines.

### Largest Files (potential refactoring targets)

| File | Lines | Notes |
|------|-------|-------|
| ModelEditor.java | 1,365 | 78 public methods, god-class |
| ModelWindow.java | 915 | Main window wiring, acceptable |
| SvgExporter.java | 835 | Rendering logic, partly duplicates CanvasRenderer |
| ModelDefinitionSerializer.java | 762 | Hand-rolled JSON, could use Jackson annotations |
| ModelCanvas.java | 744 | Recently refactored from 1,555 |
| CanvasRenderer.java | 718 | Drawing code, acceptable for its role |

---

## Comparison with Previous Audit (2026-03-07)

| Metric | Mar 7 | Mar 8 | Change |
|--------|-------|-------|--------|
| Total source files | ~165 | 274 | +109 (recounted accurately) |
| Total source LoC | ~26,400 | 43,269 | (recounted, includes all 5 modules) |
| Tests passing | 540 | 549 | +9 |
| SpotBugs findings | 0 | 2 | New bugs introduced |
| ModelCanvas lines | 1,592 | 744 | −53% (refactored) |
| JaCoCo coverage | N/A | Active | Added this session |
| Open critical issues | 0 | 1 | New: C1 |
| Open high issues | 0 | 5 | New: H1-H5 |
| Open medium issues | 2 | 13 | New: M1-M8 |

---

## Recommendations

### Immediate (before next release)

1. **Fix C1** — Log the swallowed exception in ModelCompiler (1-line fix that prevents silent
   simulation errors)
2. **Fix H1** — Use `e.toString()` fallback in AnalysisRunner error handler
3. **Fix H2** — Investigate and fix/remove the dead `flowRouteNames` set
4. **Fix H3/H4** — Add XXE protections to XmileExporter

### Short-term (R1 milestone)

5. Add basic tests for forrester-tools and forrester-demos modules
6. Replace broad `catch (Exception)` with specific types
7. Refactor ModelEditor to reduce public API surface

### Ongoing

8. Add Checkstyle or ErrorProne to the build pipeline
9. Track and improve JaCoCo coverage metrics
10. Convert `return null` patterns to Optional in io/ package
