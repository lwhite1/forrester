# Third Audit Pass — External Model Representation

**Date:** 2026-03-02
**Scope:** All 56 source + test files from the external model representation feature
**Prior audits:** First audit (43 issues fixed), second audit (12 source + 10 test fixes)

## Summary

This pass addressed all remaining issues deferred in the second audit, plus
additional test coverage gaps. All items from the previous "Deferred" lists that
were actionable bugs or meaningful improvements have been resolved.

## Source Fixes (14)

| # | Severity | File | Fix |
|---|----------|------|-----|
| 1 | HIGH | `Expr.java` | `Ref` compact constructor validates name not null/blank |
| 2 | HIGH | `ModelDefinitionSerializer.java` | Replace `RuntimeException` with `IllegalArgumentException` for serialization/deserialization errors |
| 3 | HIGH | `ModelDefinitionSerializer.java` | `requiredDouble()` validates field is numeric, rejects strings |
| 4 | MEDIUM | `UnitRegistry.java` | `find()` null-checks the name parameter |
| 5 | MEDIUM | `ConnectorGenerator.java` | Made `final` with private constructor |
| 6 | MEDIUM | `AutoLayout.java` | Made `final` with private constructor |
| 7 | MEDIUM | `DependencyGraph.java` | Replaced `ArrayList` with `ArrayDeque` in `topologicalSort()` — O(1) poll vs O(n) remove(0) |
| 8 | MEDIUM | `SubscriptDef.java` | Reject null/empty labels (require at least one label) |
| 9 | LOW | `ExprStringifier.java` | Fixed javadoc to accurately describe round-trip limitations |
| 10 | MEDIUM | `DefinitionValidator.java` | Added validation for unbound required input ports |
| 11 | MEDIUM | `PointListUtil.java` | NEW: extracted shared `pointListEquals()` utility from ConnectorRoute/FlowRoute |
| 12 | MEDIUM | `ConnectorRoute.java` | Use `PointListUtil` instead of duplicated `equals` logic |
| 13 | MEDIUM | `FlowRoute.java` | Use `PointListUtil` instead of duplicated `equals` logic |
| 14 | HIGH | `ModelDefinitionSerializer.java` | Depth limit exception changed from RuntimeException to IllegalArgumentException |

## Test Coverage Added (20 new tests)

| File | Tests Added |
|------|-------------|
| `ExprStringifierTest.java` | NaN literal rejection, Infinity literal rejection, negative literal, unary with binary operand, reserved word quoting |
| `ExprCompilerTest.java` | Division by zero, modulo by zero, DELAY3 too many args, RAMP too many args, LOOKUP non-Ref first arg, SMOOTH over steps, 3-arg RAMP, LOOKUP underscore-to-space |
| `ModelCompilerTest.java` | Bad output binding, createSimulation without defaults, createSimulation with explicit args, unknown NegativeValuePolicy |
| `ModelDefinitionSerializerTest.java` | Non-numeric double field, malformed JSON exception type |
| `DefinitionValidatorTest.java` | Unbound required input port |
| `ViewValidatorTest.java` | Non-existent flow route |

## Assertion Quality Improvements

Converted remaining non-fluent AssertJ assertions across:
- `DefinitionValidatorTest.java` — all `stream().anyMatch()` + `.isTrue()` → `.anyMatch()`
- `ViewValidatorTest.java` — all `.isEmpty().isTrue()` → `.isEmpty()`, `.size().isEqualTo()` → `.hasSize()`
- `ModelDefinitionSerializerTest.java` — all `.size().isEqualTo()` → `.hasSize()` / `.hasSameSizeAs()`
- `ModelDefinitionTest.java` — SubscriptDef null/empty labels test updated

## Items Still Deferred (Design Trade-offs)

These are acknowledged design decisions, not bugs:
- `UnitRegistry` is not thread-safe (single-threaded usage)
- `UnitRegistry.resolve()` auto-creates units for unknown names (convenience feature)
- `TIME` returns step count, not clock time (matches Euler dt=1)
- `evaluateConstant` only supports literals, refs, negation (not binary constant expressions)
- `QualifiedName` is unused (retained for planned module hierarchy feature)
- Circular reference detection is name-based (not identity-based)
- No isolated tests for `CompilationContext`, `QualifiedName` (covered via integration tests)

## Test Results

All 770 tests pass (20 new tests added).
