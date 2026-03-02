# Vensim .mdl Importer — Audit Report

**Date**: 2026-03-02
**Scope**: All 8 new source files and 3 test files in the `io/` and `io/vensim/` packages

## Files Audited

| File | Lines | Role |
|------|-------|------|
| `io/ModelImporter.java` | 32 | Format-agnostic interface |
| `io/ImportResult.java` | 31 | Result record with warnings |
| `io/vensim/MdlEquation.java` | 45 | Parsed equation record |
| `io/vensim/MdlParser.java` | 218 | Low-level .mdl parser |
| `io/vensim/VensimExprTranslator.java` | 413 | Expression syntax translator |
| `io/vensim/SketchParser.java` | 197 | Sketch section → ViewDef |
| `io/vensim/VensimImporter.java` | 420 | Main orchestrator |
| `io/vensim/package-info.java` | 10 | Package docs |
| Test: `MdlParserTest.java` | 224 | 17 tests |
| Test: `VensimExprTranslatorTest.java` | 290 | 33 tests |
| Test: `VensimImporterTest.java` | 477 | 12 tests |

**Total**: 2,514 lines added, 62 tests, all passing (852 total suite).

---

## Summary of Findings

| Category | Count | Critical | High | Medium | Low |
|----------|-------|----------|------|--------|-----|
| Bugs | 7 | 0 | 3 | 3 | 1 |
| Issues | 9 | 0 | 0 | 5 | 4 |
| Missing Tests | 14 | — | — | — | — |
| Suggestions | 12 | — | — | — | — |

---

## Bugs

### B1. CRLF line endings break continuation-line joining — HIGH

**File**: `MdlParser.java:27`

The `CONTINUATION_PATTERN` regex is `\\\n\s*`, which only matches backslash + LF. Windows-saved Vensim .mdl files use CRLF (`\r\n`), and `Files.readString()` does not normalize line endings. Any real Vensim .mdl file with backslash-continuation lines will not have those continuations joined, producing broken equation strings.

**Fix**: Normalize line endings at the top of `parse()`:
```java
content = content.replace("\r\n", "\n").replace("\r", "\n");
```
This also fixes the related `\n`-only splits in `extractGroupName` and `splitLines`.

### B2. TIME STEP value is parsed but discarded — HIGH

**File**: `VensimImporter.java:91`

```java
double timeStepValue = getDoubleFromControl(controlVars, "TIME STEP", 1.0, warnings);
```

This variable is computed but never used. `SimulationSettings` stores only the time unit name, not the step magnitude. A Vensim model with `TIME STEP = 0.25 Day` will import with the time unit "Day" but the 0.25 value is lost. The simulation will use whatever default step size the runtime selects, which can produce incorrect results for stiff models.

**Fix**: The `SimulationSettings` record does not currently have a field for step magnitude. This needs architectural consideration — either extend the record or store the value as a comment/warning.

### B3. XIDZ/ZIDZ expansion missing parentheses around arguments — HIGH

**File**: `VensimExprTranslator.java:245,272`

`XIDZ(a + b, c * d, 0)` translates to `IF(c * d == 0, 0, a + b / c * d)`. Due to operator precedence, `a + b / c * d` parses as `a + ((b / c) * d)`, not `(a + b) / (c * d)`.

**Fix**: Wrap arguments in parentheses:
```java
String replacement = "IF((" + b + ") == 0, " + x + ", (" + a + ") / (" + b + "))";
```

### B4. `:NOT:` translation changes operator precedence — MEDIUM

**File**: `VensimExprTranslator.java:111`

`:NOT:` is replaced with `!` (no parentheses). In Vensim, `:NOT:` has lower precedence than comparisons, so `:NOT: x > 0` means `NOT(x > 0)`. After translation, `! x > 0` parses as `(!x) > 0` because `!` (unary) binds tighter than `>` in ExprParser.

**Fix**: Difficult with pure text replacement. Options: (a) document as known limitation, (b) require users to parenthesize, or (c) implement a more sophisticated `:NOT:` handler that finds the operand scope.

### B5. `Time` pattern is case-sensitive — MEDIUM

**File**: `VensimExprTranslator.java:42`

`TIME_VAR_PATTERN` is `\bTime\b` (case-sensitive). Vensim allows `Time`, `time`, or `TIME`. Lowercase `time` in an expression will not be translated to `TIME`, causing compile-time resolution failures.

**Fix**: Make the pattern case-insensitive: `Pattern.compile("(?i)\\bTime\\b")`.

### B6. System variable names use case-sensitive matching — MEDIUM

**File**: `VensimImporter.java:51-53`

`SYSTEM_VARS` only contains two casing variants per name. A Vensim model using `"initial time"` (all lowercase) would not be recognized, causing the system variable to leak into the model as a regular element.

**Fix**: Use case-insensitive matching:
```java
private static boolean isSystemVar(String name) {
    String upper = name.strip().toUpperCase(Locale.ROOT);
    return Set.of("INITIAL TIME", "FINAL TIME", "TIME STEP", "SAVEPER").contains(upper);
}
```

### B7. `isNumericLiteral` rejects `.5` style notation — LOW

**File**: `VensimImporter.java:49`

The `NUMERIC_PATTERN` rejects `".5"`, `"-.5"`, and `"+3.0"`, which are valid in Vensim. A constant written as `.5` would be misclassified as an auxiliary with an unparseable expression.

**Fix**: Extend the pattern:
```java
"^[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?$"
```

---

## Issues

### I1. Unsupported function check produces false positives for identically-named variables — MEDIUM

**File**: `VensimExprTranslator.java:404-407`

A variable named `Pulse` would match `\bPULSE\b` and trigger a spurious warning. The check should require `\s*\(` after the function name to confirm it's a call.

### I2. Duplicate normalized names are not detected — MEDIUM

**File**: `VensimImporter.java`, Pass 2

Two Vensim variables that normalize to the same name (e.g., `"GDP ($B)"` and `"GDP B"` both → `"GDP_B"`) are silently added, producing a definition with duplicate element names. No warning is emitted.

### I3. Auto-generated `_net_flow` names may collide — MEDIUM

**File**: `VensimImporter.java:289`

A Vensim variable `"Population net flow"` normalizes to `Population_net_flow`, colliding with the auto-generated flow for the `Population` stock. No collision detection exists.

### I4. SketchParser flow valves never match `flowNames` set — MEDIUM

**File**: `SketchParser.java:145-161`

The `flowNames` set contains auto-generated `_net_flow` names, but sketch flow valves use the original Vensim names. All flow valves are therefore classified as `"aux"` type instead of `"flow"`.

### I5. `catch (Exception e)` in classifyAndBuild is too broad — MEDIUM

**File**: `VensimImporter.java:153`

Catches all exceptions including `NullPointerException` and `ArrayIndexOutOfBoundsException`, masking potential programmer errors. Should catch `IllegalArgumentException` specifically.

### I6. WITH LOOKUP only handles the first occurrence — LOW

**File**: `VensimExprTranslator.java:177`

Multiple `WITH LOOKUP` constructs in one expression (rare) would leave all but the first untranslated.

### I7. Group delimiter detection is overly broad — LOW

**File**: `MdlParser.java:185`

`line.contains("****")` matches any block containing four consecutive asterisks, including comments.

### I8. SketchParser connector references to clouds use raw numeric IDs — LOW

**File**: `SketchParser.java:173`

Clouds (type 12) are skipped, but connectors referencing their IDs fall back to using the numeric ID string as a name.

### I9. SMOOTHI/SMOOTH3I/DELAY1I warning messages are misleading — LOW

**File**: `VensimExprTranslator.java:122-133`

Warnings say "initial value argument dropped" but the argument is actually preserved and passed through to SMOOTH/DELAY3.

---

## Missing Test Coverage

| ID | Description | Priority |
|----|-------------|----------|
| MT1 | **Import → compile → simulate** round-trip (verify simulation produces correct results) | High |
| MT2 | CRLF line endings in MdlParser input | High |
| MT3 | XIDZ/ZIDZ with multi-operator arguments (e.g., `XIDZ(a+b, c*d, 0)`) | High |
| MT4 | SketchParser direct unit tests | Medium |
| MT5 | `:NOT:` with comparison operators | Medium |
| MT6 | Case variations of `Time` variable (lowercase `time`) | Medium |
| MT7 | Non-standard casing of system variables | Medium |
| MT8 | `.5` style numeric literals | Medium |
| MT9 | `:MACRO:` / `:END OF MACRO:` block skipping | Medium |
| MT10 | Malformed INTEG expressions (missing init value, non-numeric init) | Medium |
| MT11 | SMOOTHI, SMOOTH3I, DELAY1I individual translations | Low |
| MT12 | Duplicate normalized variable names | Low |
| MT13 | FINAL TIME <= INITIAL TIME edge case | Low |
| MT14 | Unsupported function false positives for variables named `Pulse`, `Game` | Low |

---

## Suggestions

| ID | Description |
|----|-------------|
| S1 | Normalize CRLF at the entry point of `MdlParser.parse()` (fixes B1 and all related CRLF issues in one line) |
| S2 | Parenthesize XIDZ/ZIDZ expansion arguments (fixes B3) |
| S3 | Make `Time` pattern case-insensitive (fixes B5) |
| S4 | Use case-insensitive matching for system variables (fixes B6) |
| S5 | Extend numeric literal pattern to accept `.5` notation (fixes B7) |
| S6 | Require `\s*\(` after function names in unsupported-function check (fixes I1) |
| S7 | Track normalized names and warn on duplicates (fixes I2) |
| S8 | Pass original Vensim flow names (not `_net_flow`) to SketchParser (fixes I4) |
| S9 | Narrow catch clause to `IllegalArgumentException` (fixes I5) |
| S10 | Add end-to-end import-compile-simulate test for teacup model (verify temperature decay toward room temperature) |
| S11 | Consider splitting multi-term INTEG expressions into separate inflow/outflow definitions for semantic fidelity |
| S12 | Use an enum for MdlEquation operator types instead of raw strings |

---

## Recommended Fix Priority

**Wave 1 — Silent correctness bugs (fix immediately):**
1. B1 — CRLF normalization (1-line fix in MdlParser)
2. B3 — XIDZ/ZIDZ parentheses (2-line fix in VensimExprTranslator)
3. B6 — Case-insensitive system var matching (small refactor)

**Wave 2 — Accuracy and robustness:**
4. B5 — Case-insensitive Time pattern
5. B7 — Extend numeric literal pattern
6. I5 — Narrow catch clause
7. I1 — Unsupported function false-positive guard

**Wave 3 — Semantic fidelity and test coverage:**
8. B2 — TIME STEP value preservation (needs `SimulationSettings` design discussion)
9. I2, I3 — Duplicate name detection
10. I4 — SketchParser flow name matching
11. MT1 — Add import→compile→simulate integration test

**Wave 4 — Known limitations to document:**
12. B4 — `:NOT:` precedence (document as limitation)
13. I9 — Fix misleading warning text
