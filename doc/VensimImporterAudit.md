# Vensim .mdl Importer — Audit Report

**Date**: 2026-03-02
**Scope**: All 8 new source files and 3 test files in the `io/` and `io/vensim/` packages
**Status**: All bugs fixed. 865 tests passing (75 in importer suite). Remaining items are low-priority.

---

## Remaining Issues

### I6. WITH LOOKUP only handles the first occurrence — LOW

**File**: `VensimExprTranslator.java:177`

Multiple `WITH LOOKUP` constructs in one expression (rare) would leave all but the first untranslated.

### I7. Group delimiter detection is overly broad — LOW

**File**: `MdlParser.java:185`

`line.contains("****")` matches any block containing four consecutive asterisks, including comments.

### I8. SketchParser connector references to clouds use raw numeric IDs — LOW

**File**: `SketchParser.java:173`

Clouds (type 12) are skipped, but connectors referencing their IDs fall back to using the numeric ID string as a name.

---

## Missing Test Coverage

| ID | Description | Priority |
|----|-------------|----------|
| MT4 | SketchParser direct unit tests | Low |
| MT9 | `:MACRO:` / `:END OF MACRO:` block skipping | Low |
| MT10 | Malformed INTEG expressions (missing init value, non-numeric init) | Low |
| MT11 | SMOOTHI, SMOOTH3I, DELAY1I individual translations | Low |

---

## Deferred Suggestions

| ID | Description |
|----|-------------|
| S11 | Split multi-term INTEG into separate inflow/outflow definitions for semantic fidelity |
| S12 | Use an enum for MdlEquation operator types instead of raw strings |
