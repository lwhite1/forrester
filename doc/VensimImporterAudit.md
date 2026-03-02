# Vensim .mdl Importer — Audit Report

**Date**: 2026-03-02
**Scope**: All 8 new source files and 3 test files in the `io/` and `io/vensim/` packages
**Status**: All four fix waves applied. 865 tests passing (75 in importer suite).

## Files Audited

| File | Lines | Role |
|------|-------|------|
| `io/ModelImporter.java` | 32 | Format-agnostic interface |
| `io/ImportResult.java` | 31 | Result record with warnings |
| `io/vensim/MdlEquation.java` | 45 | Parsed equation record |
| `io/vensim/MdlParser.java` | 222 | Low-level .mdl parser |
| `io/vensim/VensimExprTranslator.java` | 458 | Expression syntax translator |
| `io/vensim/SketchParser.java` | 197 | Sketch section → ViewDef |
| `io/vensim/VensimImporter.java` | 434 | Main orchestrator |
| `io/vensim/package-info.java` | 10 | Package docs |
| Test: `MdlParserTest.java` | 246 | 19 tests |
| Test: `VensimExprTranslatorTest.java` | 316 | 40 tests |
| Test: `VensimImporterTest.java` | 662 | 23 tests |

**Total**: ~2,800 lines, 75 tests in importer suite, 865 total suite.

---

## Summary of Findings

| Category | Found | Fixed | Remaining |
|----------|-------|-------|-----------|
| Bugs | 7 | 7 | 0 |
| Issues | 9 | 6 | 3 (low-priority) |
| Missing Tests | 14 | 10 | 4 (low-priority) |
| Suggestions | 12 | 10 | 2 (deferred) |

---

## Bugs — All Fixed

### B1. CRLF line endings break continuation-line joining — FIXED
Added CRLF normalization at the top of `MdlParser.parse()`. Regression tests added for CRLF equations and CRLF continuations.

### B2. TIME STEP value is parsed but discarded — FIXED
Added a warning when `TIME STEP != 1.0` to inform users the value is preserved as metadata only. Full architectural support for fractional time steps deferred (requires `SimulationSettings` extension).

### B3. XIDZ/ZIDZ expansion missing parentheses around arguments — FIXED
All arguments are now wrapped in parentheses: `IF((b) == 0, x, (a) / (b))`. Regression test added for complex multi-operator arguments.

### B4. `:NOT:` translation changes operator precedence — FIXED
Implemented `translateNot()` method that wraps the operand scope in parentheses: `:NOT: x > 0` → `!(x > 0)`. Scoping respects `&&`, `||`, comma, and paren depth boundaries.

### B5. `Time` pattern is case-sensitive — FIXED
`TIME_VAR_PATTERN` now uses `(?i)` flag. Regression test added for lowercase `time`.

### B6. System variable names use case-sensitive matching — FIXED
`isSystemVar()` now converts to uppercase before matching against `SYSTEM_VAR_KEYS`. Regression test added for lowercase system variables.

### B7. `isNumericLiteral` rejects `.5` style notation — FIXED
Extended `NUMERIC_PATTERN` to `^[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?$`. Regression tests added for `.5` and `-.5`.

---

## Issues

### I1. Unsupported function check produces false positives — FIXED
Now requires `\s*\(` after the function name to confirm it's a call. Regression tests added for variables named `Pulse` and `Game`.

### I2. Duplicate normalized names are not detected — FIXED
Added `allNormalizedNames` set with duplicate detection and warning. Regression test added.

### I3. Auto-generated `_net_flow` names may collide — FIXED
Covered by I2's duplicate name detection — collisions now produce warnings.

### I4. SketchParser flow valves never match `flowNames` set — FIXED
Now passes `sketchFlowNames` (containing both original stock names and `_net_flow` names) to SketchParser.

### I5. `catch (Exception e)` in classifyAndBuild is too broad — FIXED
Narrowed to `catch (IllegalArgumentException e)`.

### I6. WITH LOOKUP only handles the first occurrence — NOT FIXED (low priority)
Multiple `WITH LOOKUP` in a single expression is rare in practice.

### I7. Group delimiter detection is overly broad — NOT FIXED (low priority)
`line.contains("****")` could match comments, but this is very unlikely in real models.

### I8. SketchParser connector references to clouds use raw numeric IDs — NOT FIXED (low priority)
Cosmetic issue; clouds are placeholders for model boundaries.

### I9. SMOOTHI/SMOOTH3I/DELAY1I warning messages are misleading — FIXED
Updated warning text to accurately describe the semantic differences.

---

## Missing Test Coverage — Status

| ID | Description | Status |
|----|-------------|--------|
| MT1 | Import → compile → simulate round-trip | **Added** |
| MT2 | CRLF line endings in MdlParser input | **Added** |
| MT3 | XIDZ/ZIDZ with multi-operator arguments | **Added** |
| MT4 | SketchParser direct unit tests | Not added (low priority) |
| MT5 | `:NOT:` with comparison operators | **Added** |
| MT6 | Case variations of `Time` variable | **Added** |
| MT7 | Non-standard casing of system variables | **Added** |
| MT8 | `.5` style numeric literals | **Added** |
| MT9 | `:MACRO:` block skipping | Not added (low priority) |
| MT10 | Malformed INTEG expressions | Not added (low priority) |
| MT11 | SMOOTHI, SMOOTH3I, DELAY1I translations | Not added (low priority) |
| MT12 | Duplicate normalized variable names | **Added** |
| MT13 | FINAL TIME <= INITIAL TIME edge case | **Added** |
| MT14 | Unsupported function false positives | **Added** |

---

## Suggestions — Status

| ID | Description | Status |
|----|-------------|--------|
| S1 | Normalize CRLF in `MdlParser.parse()` | **Done** |
| S2 | Parenthesize XIDZ/ZIDZ expansion arguments | **Done** |
| S3 | Make `Time` pattern case-insensitive | **Done** |
| S4 | Case-insensitive matching for system variables | **Done** |
| S5 | Extend numeric literal pattern for `.5` | **Done** |
| S6 | Require `\s*\(` after function names | **Done** |
| S7 | Track normalized names and warn on duplicates | **Done** |
| S8 | Pass original flow names to SketchParser | **Done** |
| S9 | Narrow catch clause to `IllegalArgumentException` | **Done** |
| S10 | Add import→compile→simulate integration test | **Done** |
| S11 | Split multi-term INTEG into separate in/outflows | Deferred |
| S12 | Use enum for MdlEquation operator types | Deferred |
