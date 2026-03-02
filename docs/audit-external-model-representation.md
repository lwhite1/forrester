# Audit: External Model Representation Implementation

**Date:** 2026-03-01
**Scope:** 36 source files, 17 test files across 6 phases (commit d314ffe)
**Result:** 728 tests pass. Design gaps and style issues identified below.

---

## Low-Priority Issues

### 1. ExprParser dead code
`parseCall()` method serves no purpose; function calls are handled inside `parsePrimary()`. The method contradicts the documented grammar.

### 2. ExprParser identical methods
`matchMinus()` and `matchUnaryMinus()` have identical implementations. Only differentiated by call site.

### 3. ExprParser no depth limit
Recursive descent with no depth limit. Deeply nested expressions cause StackOverflowError.

### 4. ExprParser position reporting
Input is trimmed before parsing, so error positions in ParseException are relative to trimmed input, not the original string.

### 5. ExprParser hardcoded TIME/DT
`TIME` and `DT` are always parsed as zero-arg function calls, shadowing any model elements named TIME or DT.

### 6. ExprCompiler STEP/RAMP truncation
`double` step/ramp times are silently cast to `int`, truncating fractional values.

### 7. ExprCompiler MEAN with zero args
Returns `0.0 / 0 = NaN` silently. No argument count validation.

### 8. QualifiedName.parse allows blank parts
`"a..b"` produces a name with an empty middle part. `""` produces a single blank part. No validation on individual part content.

### 9. UnitRegistry unbounded auto-creation
Any unknown unit name causes a new ItemUnit to be created and permanently registered. Could be a memory concern with untrusted input.

### 10. ModelReport unbounded recursion
`appendModule` recursively traverses sub-modules with no cycle detection. If modules form a cycle, this stack-overflows.

### 11. Module.addSubModule no cycle detection
A module can be added as its own sub-module, creating an infinite loop for any recursive traversal.

### 12. No `@Override` on Resettable.reset()
`Smooth.reset()` and `Delay3.reset()` implement the interface method but lack `@Override`.

### 13. Resettable could be @FunctionalInterface
Single abstract method interface without the annotation.

### 14. String-typed enumerations throughout model/def
`StockDef.negativeValuePolicy`, `LookupTableDef.interpolation`, and `ElementPlacement.type` use free-form strings where enums would be safer.

### 15. No NaN/Infinity guards on double fields
`StockDef.initialValue`, `ConstantDef.value`, `SimulationSettings.duration`, and lookup table arrays accept NaN and Infinity silently.

---

## Style Violations

### S1. Wildcard static imports in all 17 test files
Every test file uses `import static org.junit.jupiter.api.Assertions.*`, violating the project's no-wildcard-imports rule.

### S2. AssertJ not used
Project guidelines prefer AssertJ for fluent assertions. All tests use JUnit 5 assertions exclusively.

### S3. Inconsistent use of `var` in DependencyGraph
Lines 52 and 55 use `var` for loop variables while the rest of the method uses explicit types.

### S4. Magic numbers in ExprStringifier
`-10` and `10` used as precedence bounds without named constants.

---

## Test Coverage Gaps

### T1. SubscriptDef never tested anywhere
`SubscriptDef` is defined and accepted by `ModelDefinition` and the builder, but no test exercises it — not in construction, validation, serialization, or compilation.

### T2. ExprCompiler function coverage gaps
The following compiled functions have no unit tests: `%` (modulo), `&&`/`||` (logical), `!` (NOT), `SUM`, `MEAN`, `DELAY3`. Some are covered indirectly via integration tests in ModelCompilerTest, but error paths (wrong arg count, non-constant args) are untested.

### T3. Weak assertions in integration tests
- `shouldCompileSTEP`: asserts `> 50` instead of approximate expected value
- `shouldCompileRAMP`: asserts `> 0` (extremely weak)
- `shouldHandleAuxReferencingAnotherAux`: no value assertion
- `shouldSupportTwoModuleInstances`: checks stock count but not values
- `shouldCompileModelWithOutputBindings`: checks `assertNotNull` but not value

### T4. ModelCompilerTest stock ordering assumption
`shouldMatchHandBuiltSIR` uses `getStocks().get(0)`, `.get(1)`, `.get(2)` by index. If ordering changes, the test breaks silently (comparing wrong stocks).

### T5. No compilation failure tests
No tests verify that the compiler produces meaningful errors for invalid definitions (missing variables, bad formulas, type mismatches).

### T6. No auxiliary equation validation test
`DefinitionValidator` checks auxiliary equations parse correctly, but no test exercises this path.

### T7. JSON serializer string-level assertions are fragile
`shouldOmitNullFields` and parts of `shouldSerializeAndDeserializeSIRModel` check raw JSON strings, making them dependent on serialization format.

---

## Summary

| Category | Count |
|----------|-------|
| Low-priority | 15 |
| Style violations | 4 |
| Test coverage gaps | 7 |
| **Total findings** | **26** |
