# UI Audit Report — Phase 8 (Simulation Running + Undo/Redo)

Audit of all `forrester-app` canvas code after the simulation running feature and undo/redo support were added.

## Scope

New features: (1) Simulate menu with Run Simulation (Ctrl+R) and Simulation Settings dialog, backed by a non-UI `SimulationRunner` class that compiles and executes models. (2) Undo/redo infrastructure (`UndoManager`, `ModelCanvas` integration, Edit menu wiring).

Changes across `SimulationRunner.java`, `SimulationSettingsDialog.java`, `SimulationResultsDialog.java`, `SimulationRunnerTest.java`, `UndoManager.java`, `UndoManagerTest.java`, `ModelEditor.java`, `ModelCanvas.java`, `ForresterApp.java`.

---

## Regression Check — Previously-Fixed Findings

| Previous Finding | Status |
|---|---|
| BUG-7: Inline editor width not scaled by zoom | **Still fixed** |
| BUG-8: Duplicate CLOUD_OFFSET | **Still fixed** |
| BUG-9: Cloud position logic duplication | **Still fixed** |
| BUG-10: Reattachment self-loop | **Still fixed** |
| BUG-11: Constant value editor Y offset ignores zoom | **Still fixed** |
| BUG-12: reconnectFlow doesn't validate stockName | **Still fixed** |
| BUG-14: Flow equation/name text outside hit area | **Still present** (carried) |
| BUG-16: Default "0" equation shown on new flows/auxes | **Still present** (carried) |
| EDGE-1: Self-loop flow creation | **Still fixed** |
| EDGE-3: Name validation | **Still fixed** |
| EDGE-5: Equation references on delete | **Still fixed** |
| EDGE-6: Cloud-to-cloud | **Still fixed** |
| QUALITY-10: drawFlow coord convention | **Still fixed** |
| UX-5: No cloud preview during reattachment | **Still fixed** |
| UX-6: No undo support | **Fixed** — UndoManager + full canvas integration |
| UX-9: No status bar | **Fixed** |

**No regressions detected.** All previously-fixed findings remain fixed. UX-6 (undo support) is now resolved.

---

## Major / Bug

### BUG-17 [NEW]: SimulationResult `double[]` rows are mutable despite record contract
**File:** `SimulationRunner.java:30-35`

`SimulationResult` is declared as an immutable record, and the compact constructor uses `List.copyOf(rows)`. However, `List.copyOf` performs a shallow copy — the individual `double[]` arrays inside each row remain mutable. Code receiving a `SimulationResult` can modify row values via `result.rows().get(i)[0] = 999`. In practice, the only consumer is the read-only `TableView`, so this is not exploitable today, but violates the stated immutability contract.

**Severity:** Low-risk bug (defensive contract violation, not exploitable in current usage).

### BUG-18 [NEW]: Simulation runs on JavaFX Application Thread — UI freezes for large models
**File:** `ForresterApp.java:268-271`

`runSimulation()` calls `runner.run(def, settings)` synchronously on the FX Application Thread. For models with many elements or large durations (e.g., 10,000 steps), the UI will freeze until simulation completes. No progress indicator or cancellation mechanism exists.

**Severity:** UX bug for non-trivial models. Acceptable for small models / MVP.

### BUG-14 [CARRIED]: Flow equation/name text rendered outside hit area
### BUG-16 [CARRIED]: Default "0" equation shown on all new flows and auxes

---

## Minor

### MINOR-8 [NEW]: Fully-qualified `javafx.beans.binding.Bindings` reference instead of import
**File:** `SimulationSettingsDialog.java:66`

Uses `javafx.beans.binding.Bindings.createBooleanBinding(...)` inline instead of adding a top-level import. Functional but inconsistent with project style.

### MINOR-9 [NEW]: Settings dialog pre-fills `100.0` when reopening saved settings
**File:** `SimulationSettingsDialog.java:39`

When loading existing settings, `String.valueOf(existing.duration())` converts `100.0` to `"100.0"`, not `"100"`. This is cosmetically inconsistent with the default `"100"` string used for new settings. The trailing `.0` is harmless but visually noisy.

### MINOR-10 [NEW]: Flow rate values not captured in simulation results
**File:** `SimulationRunner.java:73-82, 86-102`

The `DataCaptureHandler` captures stocks and variables (auxiliaries) but not flow rates. In system dynamics tools, users commonly want to see flow rates alongside stock levels. This is a design choice but limits the utility of the results table.

### MINOR-11 [NEW]: Multiple results windows can accumulate
**File:** `ForresterApp.java:273-274`

Each "Run Simulation" opens a new `Stage`. Previous results windows are not closed or tracked. After several runs, many windows accumulate.

### MINOR-1 [CARRIED]: Equation editor overlaps flow name label
### MINOR-4 [CARRIED]: Flow equation editor field is too narrow (50px at 100% zoom)
### MINOR-5 [CARRIED]: Inline editor position assumes canvas at (0,0)
### MINOR-6 [CARRIED]: No feedback clicking non-stock during flow creation
### MINOR-7 [CARRIED]: No text clipping for equations inside aux rectangles

---

## Edge Cases

### EDGE-7 [NEW]: Non-standard time unit in loaded SimulationSettings
**File:** `SimulationSettingsDialog.java:37-38`

If a model file contains a `SimulationSettings` with a custom time unit string not in the hardcoded `TIME_UNIT_OPTIONS` (e.g., `"Fortnight"`), `timeStepCombo.setValue("Fortnight")` will display the value but it won't appear in the dropdown. If the user opens the combo and selects something else, they cannot re-select the custom unit. The value is preserved if the user clicks Cancel. Low probability since the UI only creates standard units.

### EDGE-8 [NEW]: Undo after simulation does not restore pre-simulation state
**File:** `ForresterApp.java:256-278`

Running a simulation does not modify the model, so no undo state is needed. However, the settings dialog saves settings to the editor (`editor.setSimulationSettings(settings)`) without saving undo state. If the user opens settings, changes them, then tries to undo, the settings change is not undoable. This is minor since settings don't affect the visual model.

---

## Code Quality

### CQ-6 [NEW]: `SimulationRunner` creates a redundant `UnitRegistry`
**File:** `SimulationRunner.java:50`

`SimulationRunner.run()` creates a new `UnitRegistry()` to resolve time unit strings. The `ModelCompiler` also creates its own `UnitRegistry` internally during `compile()`. Both registries contain the same built-in units. For standard time units this is fine, but if custom units were ever added, only the compiler's registry would know about them. Consider exposing the compiled model's registry or using `CompiledModel.createSimulation()` which uses the internal registry.

### CQ-7 [NEW]: `DataCaptureHandler` uses package-private fields instead of encapsulation
**File:** `SimulationRunner.java:69-70`

`columnNames` and `rows` are package-private (`final List<...>` without access modifier), accessed directly from the outer class. This works but exposes internal state to the test class in the same package. Consider using private fields with accessor methods.

### CQ-8 [NEW]: `showError` duplicates title in header and title bar
**File:** `ForresterApp.java:280-286`

`showError` sets both `setTitle(title)` and `setHeaderText(title)`, making the error title appear twice. Consider using `setHeaderText(null)` or a distinct header message.

### CQ-1 [CARRIED]: Duplication across name-then-chain edit methods
### CQ-2 [CARRIED]: `findFlow`/`findAux`/`findConstant` are linear scans
### CQ-3 [CARRIED]: No equation syntax validation in `setFlowEquation`/`setAuxEquation`
### CQ-4 [CARRIED]: Magic numbers for text positioning in ElementRenderer
### CQ-5 [CARRIED]: `startInlineEdit` growing if/else chain
### QUALITY-4 [CARRIED]: `double[]` for positions instead of record

---

## Test Coverage

### New Tests Added (9 tests)

| Test | Description |
|------|-------------|
| `shouldIncludeStepAndStockNames` | Column names contain "Step" + stock names |
| `shouldIncludeVariableNames` | Column names contain auxiliary variable names |
| `shouldCaptureCorrectRowCount` | N+1 rows for N-step simulation |
| `shouldCaptureInitialStockValueInFirstRow` | Step 0 has initial stock values |
| `shouldCaptureVariableValues` | Aux variable computed correctly |
| `shouldHandleFlowDrainOverTime` | Stock drains correctly over time |
| `shouldThrowOnInvalidTimeStepUnit` | Invalid unit throws RuntimeException |
| `shouldThrowOnBadEquationReference` | Undefined variable in equation throws |
| `shouldProduceResultsForModelWithNoElements` | Empty model produces valid results |

### Test Coverage Gaps

| Gap | Description |
|-----|-------------|
| TEST-GAP-15 [NEW] | No test for `SimulationSettings` round-trip through `ModelEditor.loadFrom()` / `toModelDefinition()` |
| TEST-GAP-16 [NEW] | No test for `SimulationSettingsDialog` (requires JavaFX) |
| TEST-GAP-17 [NEW] | No test for `SimulationResultsDialog` (requires JavaFX) |
| TEST-GAP-18 [NEW] | No test for undo/redo integration in ModelCanvas (requires JavaFX) |
| TEST-GAP-19 [NEW] | No test for simulation with mismatched time step and duration unit (e.g., time step = Month, duration = 365 Days) |
| TEST-GAP-13 [CARRIED] | No test for equation interaction with rename |
| TEST-GAP-14 [CARRIED] | No test for equation cleanup in aux equations on delete |
| TEST-GAP-6 [CARRIED] | No test for `addFlow` with nonexistent stock names |
| TEST-GAP-1 [CARRIED] | No tests for ModelCanvas event handling (requires JavaFX) |
| TEST-GAP-2 [CARRIED] | No tests for InlineEditor (requires JavaFX) |
| TEST-GAP-7 [CARRIED] | No tests for ConnectionRenderer/SelectionRenderer (requires JavaFX) |
| TEST-GAP-8 [CARRIED] | No tests for CanvasToolBar (requires JavaFX) |

---

## UX

| Finding | Description |
|---------|-------------|
| UX-13 [NEW] | No progress indicator during simulation — UI freezes for large models |
| UX-14 [NEW] | No way to export/copy simulation results (only viewable in table) |
| UX-15 [NEW] | Results window has no chart/graph view — tabular data only |
| UX-16 [NEW] | Multiple results windows accumulate — no management/tracking |
| UX-2 [CARRIED] | Escape in SELECT mode does nothing visible |
| UX-3 [CARRIED] | No cursor shape changes |
| UX-7 [CARRIED] | Right-click pans instead of context menu |
| UX-8 [CARRIED] | No keyboard shortcuts for tool modes |

---

## Summary

| Severity | New | Carried | Total |
|----------|-----|---------|-------|
| Critical | 0 | 0 | 0 |
| Major/Bug | 2 | 2 | 4 |
| Minor | 4 | 5 | 9 |
| Edge Cases | 2 | 0 | 2 |
| Code Quality | 3 | 6 | 9 |
| Test Gaps | 5 | 7 | 12 |
| UX | 4 | 4 | 8 |
| **Total** | **20** | **24** | **44** |

### Priority Recommendations

1. **TEST-GAP-15**: Add test for `SimulationSettings` round-trip — easy, no JavaFX, validates save/load integrity
2. **BUG-18**: Move simulation execution to a background thread with progress indicator (future enhancement)
3. **MINOR-10**: Capture flow rate values in results for more useful output
4. **CQ-6**: Use the compiled model's internal `UnitRegistry` instead of creating a second one
5. **BUG-14 / BUG-16**: Fix carried bugs from Phase 7

### Test count: 188 passing (up from 156 — includes UndoManager tests)
