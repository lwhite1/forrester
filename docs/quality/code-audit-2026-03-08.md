# Code Quality Audit — 2026-03-08

Comprehensive audit covering architecture, security, UI/UX, testing, and build configuration.

## Executive Summary

| Dimension | Rating | Critical | High | Medium | Low |
|-----------|--------|----------|------|--------|-----|
| Architecture & Design | Mixed | 1 | 3 | 4 | 1 |
| Security & Correctness | Good | 0 | 2 | 4 | 8 |
| UI/UX Code Quality | Good | 0 | 6 | 24 | 6 |
| Testing & Coverage | Weak | 0 | 3 | 6 | 3 |
| Build & Dependencies | Good | 0 | 2 | 4 | 2 |
| **Totals** | | **1** | **16** | **42** | **20** |

The engine is well-designed with good use of Java 21 features (records, sealed types, pattern matching). The main risks are: **god classes** in the app layer, **missing simulation safety guards**, and **~45% test coverage** with critical gaps in validation and integration paths.

> **Note on severity**: The audit agents originally flagged several items as "critical" that are more accurately High or Medium for a desktop application at this stage. Severities below have been recalibrated.

---

## Issues Created

| # | Finding | Issue | Severity |
|---|---------|-------|----------|
| **Architecture** | | | |
| A1 | ModelEditor god class (1,367 lines, 78 methods) | [#160](https://github.com/Courant-Systems/shrewd/issues/160) (reopened) | Critical |
| **Security & Correctness** | | | |
| S1 | Simulation has no timeout — hangs on runaway formulas | [#198](https://github.com/Courant-Systems/shrewd/issues/198) | High |
| S2 | Stock values can become NaN/Infinity with no detection | [#199](https://github.com/Courant-Systems/shrewd/issues/199) | Medium |
| S3 | ObjectMapper lacks security hardening | [#200](https://github.com/Courant-Systems/shrewd/issues/200) | Low |
| **UI/UX** | | | |
| U1 | EquationAutoComplete.detach() not called from most forms | [#201](https://github.com/Courant-Systems/shrewd/issues/201) | Medium |
| U2 | Hardcoded dialog sizes don't adapt to small screens | [#202](https://github.com/Courant-Systems/shrewd/issues/202) | Low |
| U3 | Canvas redraws twice on resize | [#203](https://github.com/Courant-Systems/shrewd/issues/203) | Low |
| U4 | Connector regeneration expensive for large models | [#204](https://github.com/Courant-Systems/shrewd/issues/204) | Low |
| U5 | Help windows can hide behind main window | [#205](https://github.com/Courant-Systems/shrewd/issues/205) | Low |
| U6 | ESC doesn't dismiss autocomplete popup | [#206](https://github.com/Courant-Systems/shrewd/issues/206) | Medium |
| U7 | Silent parsing failures in form fields | [#207](https://github.com/Courant-Systems/shrewd/issues/207) | Medium |
| U8 | Sweep/Monte Carlo dialogs disable OK with no explanation | [#208](https://github.com/Courant-Systems/shrewd/issues/208) | Medium |
| U9 | ValidationDialog extends Stage (inconsistent) | [#213](https://github.com/Courant-Systems/shrewd/issues/213) | Low |
| **Testing** | | | |
| T1 | End-to-end integration tests missing | [#214](https://github.com/Courant-Systems/shrewd/issues/214) | High |
| T2 | Validation logic in constructors has no unit tests | [#215](https://github.com/Courant-Systems/shrewd/issues/215) | Medium |
| **Build** | | | |
| B1 | Unused Moneta dependency | [#209](https://github.com/Courant-Systems/shrewd/issues/209) | Low |
| B2 | CI missing quality gates (Checkstyle, coverage, multi-OS) | [#210](https://github.com/Courant-Systems/shrewd/issues/210) | Medium |
| B3 | Checkstyle not applied project-wide | [#211](https://github.com/Courant-Systems/shrewd/issues/211) | Low |
| B4 | No .editorconfig file | [#212](https://github.com/Courant-Systems/shrewd/issues/212) | Low |

### Pre-existing open issues that overlap with audit findings

| Finding | Existing Issue |
|---------|---------------|
| Test coverage gaps | #177, #178, #145, #54, #29 |
| Null vs Optional returns | #163 |
| Color/CSS centralization | #77 |
| Dialog boilerplate duplication | #68 |
| Chart/export utility duplication | #69 |
| logger.xml misnamed | #187 |
| Keyboard-driven connection creation | #4 |

---

## Detailed Findings

### 1. Architecture & Design

#### A1. ModelEditor God Class — Critical

**Location**: `forrester-app/.../canvas/ModelEditor.java` (1,367 lines, 78 public methods)

Combines element CRUD, equation management, routing, lookup tables, module bindings, and subscript handling in a single class. Violates Single Responsibility Principle.

**Recommendation**: Split into `StockManager`, `FlowManager`, `EquationValidator`, `RoutingManager`.

#### A2. ModelCanvas Complexity — High

**Location**: `forrester-app/.../canvas/ModelCanvas.java` (783 lines)

Manages rendering, event dispatching, tool state, undo/redo, and model/view synchronization. Already delegates to many controllers, but still coordinates too much.

#### A3. Large Supporting Classes — Medium

| Class | Lines | Notes |
|-------|-------|-------|
| ModelDefinitionSerializer | 774 | Serialization + deserialization in one class |
| CanvasRenderer | 718 | All element types rendered in one class |
| EquationAutoComplete | 677 | Tokenization + completion + popup + rendering |
| InputDispatcher | 649 | Large event handler |
| VensimImporter | 580 | Parsing + translation + validation + building |

#### A4. Inconsistent Null Handling — Medium

Some methods return `null` (e.g., `EquationAutoComplete` has 6+ null returns), while others use `Optional` or empty collections. The engine side is generally better about this than the app side.

#### A5. Good Patterns Worth Noting

- Records used throughout for immutable data (33+ record definitions)
- Sealed interfaces for AST (`Expr`, `BinaryOperator`)
- Two-pass compilation with `DoubleSupplier[]` indirection solves forward references elegantly
- `checkFxThread()` guards in ModelEditor
- `CopyOnWriteArrayList` for thread-safe listener management
- Engine has no UI dependencies — clean module boundary

---

### 2. Security & Correctness

#### S1. Simulation Timeout — High

**Location**: `Simulation.java:99-114`

`while (currentStep <= totalSteps)` with no timeout or cancellation. A formula that hangs blocks the calling thread indefinitely. `totalSteps` can overflow `long` for extreme duration/timeStep ratios.

#### S2. NaN Propagation in Stocks — Medium

**Location**: `Simulation.java:148-150`

Stock values updated without checking for NaN/Infinity. Once a stock becomes NaN, all downstream calculations produce NaN for the rest of the simulation with no warning.

#### S3. Expression Parser Nesting — Good

`ExprParser.java:30` — `MAX_DEPTH = 200` prevents stack overflow. Well-handled.

#### S4. Division by Zero — Good

`ExprCompiler.java:98-111` — Returns NaN with a warn-once guard. Correct behavior for SD modeling.

#### S5. Floating-Point Equality — Good

`ExprCompiler.java:140` — Uses `1e-10` epsilon comparison instead of `==`. Correct.

#### S6. File Size Limits — Good

Both JSON (10 MB) and Vensim importers have file size caps. Added in earlier audit rounds.

#### S7. Other Medium/Low Findings

- ObjectMapper created without security configurations (Low for desktop app)
- Vensim file size check happens after `Files.readString()` loads file (Low — TOCTOU race is theoretical)
- No symlink validation on file operations (Low — requires attacker file access)
- DELAY3 negative delay time silently defaults to 1.0 instead of erroring (Low)

---

### 3. UI/UX Code Quality

#### Memory & Cleanup

- **Canvas listeners**: Width/height change listeners and 6+ mouse handlers are registered in constructor but never explicitly removed. In practice, window close tears down the scene graph so this is a hygiene issue, not an actual leak for the current usage pattern.
- **Property bindings**: `canvas.widthProperty().bind(canvasPane.widthProperty())` — same as above.
- **Autocomplete detach**: Only `FlowForm` calls `EquationAutoComplete.detach()`. Other forms don't.
- **Stage focusedProperty listener**: Registered but never removed.

#### Keyboard Accessibility

- Good: All menu items have accelerators; command palette via Ctrl+K; tooltips on zoom buttons
- Gap: ESC doesn't dismiss autocomplete popup (closes the editor instead)
- Gap: Properties panel only reachable via mouse click on canvas
- Gap: Context menu not keyboard-accessible (no Shift+F10 / Menu key handling)
- ZoomOverlay buttons are `setFocusTraversable(false)` — intentional (keyboard shortcuts exist)

#### Error Presentation

- Good: ValidationDialog, error alerts, activity log warnings
- Gap: Silent parsing failures in form fields (invalid input silently reverts)
- Gap: Sweep/Monte Carlo dialogs disable OK button with no explanation
- Gap: No inline equation validation feedback (errors only at validation time)

#### Styling

- Inline CSS throughout (consistent approach, but many magic numbers)
- `Styles.java` centralizes some constants but color hex values are scattered
- See #77 for CSS centralization effort

#### Canvas Rendering

- Good: Proper `gc.save()/restore()`, layered rendering, efficient redraw
- Minor: Double redraw on resize (width + height listeners fire separately)
- Minor: Full connector regeneration on every model change

#### Dialog Patterns

- Most dialogs use `Dialog<T>` (correct)
- `ValidationDialog` extends `Stage` (inconsistent)
- Help windows are non-modal and can hide behind main window

---

### 4. Testing & Coverage

#### Overall: ~45% estimated code coverage

**Test files**: 122 across 4 modules

| Module | Test Files | Estimated Coverage | Notes |
|--------|-----------|-------------------|-------|
| forrester-engine | 82 | ~50% | 80+ classes untested |
| forrester-app | 35 | ~40% | 70+ UI/controller classes untested |
| forrester-tools | 4 | ~50% | Import pipeline mostly tested |
| forrester-demos | 6 | ~25% | Demos are examples |

#### Strengths

- Excellent use of `@Nested` + `@DisplayName` for hierarchical organization
- Consistent JUnit 5 usage, no JUnit 3/4 patterns
- Good naming: `shouldX_whenY()` pattern
- Real objects used instead of over-mocking (records are naturally testable)
- TestFX used appropriately for UI interaction tests

#### Critical Gaps

- **No integration tests** for end-to-end workflows (file round-trip, import→compile→simulate)
- **Validation logic untested**: `StockDef`, `SimulationSettings`, `Forecast` constructors have guards that are never exercised
- **Formula implementations untested**: `DelayFixed`, `Pulse`, `Trend`, `Npv`, `Forecast`
- **File operations untested**: `FileController` save/discard confirmation logic
- **Only 1 `@ParameterizedTest`** in the entire codebase (`Batch2SimulationTest`)

#### Pre-existing Broken Tests

14 test failures in `forrester-app` are pre-existing JavaFX runtime issues in headless environments:
- `CanvasRendererTest` (8 failures) — `NoClassDefFoundError` for `FlowGeometry`
- `CanvasStateTest` — `NoClassDefFoundError` for `CanvasState`
- `BreadcrumbBarFxTest` — `NoClassDefFoundError` for `BreadcrumbBar`
- `BindingConfigDialogFxTest` (4 failures) — requires running JavaFX application thread

These need JavaFX on the test classpath or should be excluded from headless CI runs.

---

### 5. Build & Dependencies

#### POM Structure — Good

- All dependency versions pinned in parent POM `<dependencyManagement>`
- No SNAPSHOT dependencies (except project itself)
- Plugin versions pinned in `<pluginManagement>`
- Test dependencies correctly scoped
- Java 21 consistently configured via `maven.compiler.release`

#### Dependencies

| Status | Finding |
|--------|---------|
| Unused | Moneta (JavaMoney) — declared in engine, zero imports in codebase |
| Minimal | Guava — only 5 files import `com.google.common` |
| Good | Everything else properly used |

#### CI/CD (`.github/workflows/ci.yml`)

What's included:
- Checkout v4, JDK 21 Temurin, Maven cache
- `mvn verify -B -q`, SpotBugs, JaCoCo report upload

What's missing:
- No Checkstyle execution in CI
- No JaCoCo coverage thresholds enforced
- Single OS only (ubuntu-latest) — no Windows/macOS for JavaFX portability
- Quiet mode (`-q`) hides warnings

#### Configuration Files

| File | Status |
|------|--------|
| `.editorconfig` | Missing |
| `spotbugs-exclude.xml` | Present, used |
| `checkstyle.xml` | Only in `forrester-demos` |
| `.github/workflows/ci.yml` | Present, needs expansion |

---

## What's Working Well

1. **Engine architecture**: Clean records, sealed types, two-pass compilation
2. **Module boundaries**: Engine has no UI dependencies; clear one-way dependency
3. **Thread safety**: `checkFxThread()` guards, `CopyOnWriteArrayList` for listeners
4. **Expression evaluation**: Nesting limits, epsilon comparison, division-by-zero handling
5. **File safety**: Size limits on all import paths
6. **Test organization**: Nested classes, display names, real objects over mocks
7. **Java 21 adoption**: Records, pattern matching, switch expressions used throughout

---

## Recommended Priority Order

### Do Now
1. **Simulation safety** (S1) — add timeout and totalSteps bounds check
2. **NaN detection** (S2) — warn when stocks become non-finite

### Do Soon
3. **Integration tests** (T1) — file round-trip and import→simulate paths
4. **Validation tests** (T2) — exercise constructor guards
5. **ESC dismisses autocomplete** (U6) — common keyboard workflow
6. **Silent failure feedback** (U7, U8) — show what went wrong

### Do Later
7. **ModelEditor decomposition** (A1) — largest refactoring effort
8. **Remove Moneta** (B1) — quick cleanup
9. **CI quality gates** (B2) — Checkstyle, coverage thresholds
10. **Remaining UI polish** (U2-U5, U9) — dialog sizes, help windows, etc.
