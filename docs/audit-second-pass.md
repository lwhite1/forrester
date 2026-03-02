# Second Audit — External Model Representation

**Date:** 2026-03-01
**Scope:** All 55 source + test files from the external model representation feature
**Prior audit:** First audit found 43 issues, all fixed and committed

## Summary

The second audit found ~83 raw findings across three categories:
- **~8 genuine source bugs** (fixed)
- **~10 meaningful test improvements** (fixed)
- **~65 design observations, style suggestions, and minor test coverage wishes** (deferred)

## Issues Fixed

### Source Bugs (8)

| # | Severity | File | Fix |
|---|----------|------|-----|
| 1 | CRITICAL | `LookupTableDef.java` | Override `xValues()`/`yValues()` accessors to return defensive clones — prevents callers from mutating internal arrays |
| 2 | CRITICAL | `LookupTableDef.java` | Validate xValues strictly increasing (monotonicity required for interpolation) |
| 3 | CRITICAL | `LookupTableDef.java` | Validate xValues/yValues for NaN/Infinity |
| 4 | CRITICAL | `ExprCompiler.java` | Guard DIV and MOD against division by zero — return 0.0 instead of propagating Infinity/NaN |
| 5 | CRITICAL | `ModelCompiler.java` | Output binding now throws `CompilationException` for unknown port names instead of silently skipping |
| 6 | HIGH | `QualifiedName.java` | Use `split("\\.", -1)` to preserve trailing empty strings — trailing dots now fail validation |
| 7 | HIGH | `SimulationSettings.java` | Validate `durationUnit` for null/blank |
| 8 | HIGH | `ModelDefinitionSerializer.java` | Add `MAX_MODULE_DEPTH = 50` recursion limit on nested module deserialization |

### Additional Source Improvements (4)

| # | File | Fix |
|---|------|-----|
| 1 | `ModelCompiler.java` | Remove unused parameters from `createAuxHolder()` |
| 2 | `ModelReport.java` | Replace inline `java.util.HashSet`/`java.util.Set` with proper imports |
| 3 | `ElementPlacement.java` | Replace inline `java.util.Set`/`java.util.Locale` with proper imports; make type validation case-insensitive |
| 4 | `ModelDefinitionSerializer.java` | Fix subscript deserialization NPE — use `requiredNode()` for labels |

### Test Improvements (10)

| # | File | Fix |
|---|------|-----|
| 1 | `NestedModuleTest.java` | Replace `assertThat(x < 100).isTrue()` with `assertThat(x).isLessThan(100)` for meaningful failure messages |
| 2 | `NestedModuleTest.java` | Replace `assertThat(list.isEmpty()).isFalse()` with `assertThat(list).isNotEmpty()` |
| 3 | `ModelCompilerTest.java` | Change `Exception.class` to specific `CompilationException`/`ParseException` in `shouldThrowForBadFormula` |
| 4 | `ExprDependenciesTest.java` | Replace `assertThat(deps.isEmpty()).isTrue()` with `assertThat(deps).isEmpty()` |
| 5 | `ExprDependenciesTest.java` | Replace `assertThat(deps.size()).isEqualTo(1)` + `contains()` boolean with fluent `hasSize(1).contains()` |
| 6 | `DependencyGraphTest.java` | Convert all ~20 boolean-wrapper assertions to fluent AssertJ (`contains()`, `hasSize()`, `isEmpty()`, `isNotEmpty()`, `isLessThan()`) |
| 7 | `ModelDefinitionTest.java` | Replace `isEmpty().isTrue()` and `size().isEqualTo()` with fluent equivalents |
| 8 | `JsonRoundTripTest.java` | Compare stocks by name instead of by index to prevent false positives from ordering changes |
| 9 | `ModelReportTest.java` | Add real assertions (report contains model name, "Stocks:" header); remove `System.out.println`; use package-private visibility |
| 10 | `ModelReportTest.java` | Add `@DisplayName` annotation |

## Deferred (Not Fixed)

These are legitimate observations but are either design trade-offs, minor style preferences, or low-risk edge cases:

### Design Observations
- `UnitRegistry` is not thread-safe (acceptable for current single-threaded usage)
- `UnitRegistry.resolve()` auto-creates units for unknown names (intentional design for convenience)
- `DependencyGraph.topologicalSort()` uses `ArrayList.remove(0)` which is O(n) per dequeue (acceptable for current model sizes)
- `TIME` returns step count, not simulation clock time (consistent with current Euler integration where dt=1)
- `dt` hardcoded to 1.0 in `CompilationContext` (matches current simulation semantics)
- `Smooth`/`Delay3` re-read current input when steps are skipped (simplified Euler assumption)
- `evaluateConstant` only supports literals, refs, and negation — not binary constant expressions
- `QualifiedName` is unused dead code (retained for planned module hierarchy feature)

### Style Suggestions
- Several utility classes (`ConnectorGenerator`, `AutoLayout`, `ModelReport`) could be `final` with private constructors
- Duplicated `pointListEquals` method in `ConnectorRoute`/`FlowRoute` could be extracted
- `ModelDefinitionSerializer` wraps `JsonProcessingException` in generic `RuntimeException`
- `requiredDouble()` returns 0.0 for non-numeric JSON fields via `asDouble()`
- `DefinitionValidator` circular reference detection is name-based, not identity-based

### Test Coverage Wishes
- No tests for `CompilationContext`, `CompiledModel`, `QualifiedName` in isolation
- No test for SMOOTH function behavior over multiple steps
- No test for 3-arg RAMP (with end step)
- No test for LOOKUP underscore-to-space fallback
- No test for `ExprParser` MAX_DEPTH overflow
- No test for `ExprStringifier` NaN/Infinity rejection
- Various missing edge case tests across parser, stringifier, and validator
- Several test files lack `@Nested` grouping (style preference)

## Test Results

All 750 tests pass after fixes.
