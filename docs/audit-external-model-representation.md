# Audit: External Model Representation Implementation

**Date:** 2026-03-01
**Scope:** 36 source files, 17 test files across 6 phases (commit d314ffe)
**Result:** 728 tests pass. Design gaps and style issues identified below.

---

## Medium-Priority Issues

### 1. Silent fall-through in ExprStringifier and ExprDependencies

**Files:** `ExprStringifier.java` (lines 21-34), `ExprDependencies.java`

Both use `if-else instanceof` chains over the sealed `Expr` interface. If a new variant is added, the stringifier silently produces empty output and the dependency extractor silently returns an incomplete set. Should throw `IllegalArgumentException` in the else branch (or use exhaustive switch when migrating to Java 21+).

### 2. NaN/Infinity break round-trip contract

**File:** `ExprStringifier.java`

`Literal(Double.NaN)` stringifies to `"NaN"` and `Literal(Double.POSITIVE_INFINITY)` to `"Infinity"`, which the parser cannot parse back. This breaks the documented round-trip contract.

### 3. No null validation in Expr record constructors

**File:** `Expr.java`

`Ref`, `BinaryOp`, `UnaryOp`, `Conditional`, and `FunctionCall.name` accept null without validation. Null values propagate to the parser/stringifier where they cause confusing NPEs.

### 4. `ExprDependencies.extract()` returns mutable set

**File:** `ExprDependencies.java`

Returns the internal `LinkedHashSet` directly. Callers can mutate the returned collection.

### 5. `DependencyGraph.dependentsOf()` returns mutable internal set

**File:** `DependencyGraph.java` (line ~108)

Returns either a mutable `LinkedHashSet` from the adjacency map or an empty immutable set. When returning the internal set, callers can corrupt the graph.

### 6. Inconsistent null handling for lists in records

**Files:** `ModuleInterface.java`, `SubscriptDef.java`

These call `List.copyOf()` directly without null-checking, throwing NPE on null input. All other records in the package use the `== null ? List.of() : List.copyOf(...)` pattern.

### 7. `double[]` in records breaks equals/hashCode

**Files:** `LookupTableDef.java`, `ConnectorRoute.java`, `FlowRoute.java`

Java records auto-generate `equals()` and `hashCode()` using array identity, not structural equality. Two records with identical data report as unequal. Additionally, `ConnectorRoute` and `FlowRoute` do not clone inner `double[]` elements, so external holders can mutate "immutable" record data.

### 8. Massive code duplication between compileInto and compileModule

**File:** `ModelCompiler.java`

`compileInto()` (~100 lines) and `compileModule()` (~120 lines) duplicate nearly identical logic for creating constants, stocks, lookup tables, aux holders, flow holders, and compiling formulas. Changes to compilation logic must be applied in both places.

### 9. ModelDefinitionSerializer NPE on missing required JSON fields

**File:** `ModelDefinitionSerializer.java`

Calls like `root.get("name").asText()` do not guard against null return from `get()`. Missing required fields produce unhelpful NullPointerExceptions instead of descriptive error messages.

### 10. ViewValidator does not validate FlowRoute endpoints

**File:** `ViewValidator.java`

Validates `ElementPlacement` and `ConnectorRoute` references but does not check `FlowRoute.flowName()` against existing model elements.

### 11. AutoLayout does not place module instances

**File:** `AutoLayout.java`

The layout loops over stocks, flows, auxiliaries, constants, and lookup tables, but has no loop for `ModuleInstanceDef`. Module instances are silently omitted from the generated view.

---

## Low-Priority Issues

### 12. ExprParser dead code
`parseCall()` method serves no purpose; function calls are handled inside `parsePrimary()`. The method contradicts the documented grammar.

### 13. ExprParser identical methods
`matchMinus()` and `matchUnaryMinus()` have identical implementations. Only differentiated by call site.

### 14. ExprParser no depth limit
Recursive descent with no depth limit. Deeply nested expressions cause StackOverflowError.

### 15. ExprParser position reporting
Input is trimmed before parsing, so error positions in ParseException are relative to trimmed input, not the original string.

### 16. ExprParser hardcoded TIME/DT
`TIME` and `DT` are always parsed as zero-arg function calls, shadowing any model elements named TIME or DT.

### 17. ExprCompiler STEP/RAMP truncation
`double` step/ramp times are silently cast to `int`, truncating fractional values.

### 18. ExprCompiler MEAN with zero args
Returns `0.0 / 0 = NaN` silently. No argument count validation.

### 19. QualifiedName.parse allows blank parts
`"a..b"` produces a name with an empty middle part. `""` produces a single blank part. No validation on individual part content.

### 20. UnitRegistry unbounded auto-creation
Any unknown unit name causes a new ItemUnit to be created and permanently registered. Could be a memory concern with untrusted input.

### 21. ModelReport unbounded recursion
`appendModule` recursively traverses sub-modules with no cycle detection. If modules form a cycle, this stack-overflows.

### 22. Module.addSubModule no cycle detection
A module can be added as its own sub-module, creating an infinite loop for any recursive traversal.

### 23. No `@Override` on Resettable.reset()
`Smooth.reset()` and `Delay3.reset()` implement the interface method but lack `@Override`.

### 24. Resettable could be @FunctionalInterface
Single abstract method interface without the annotation.

### 25. AutoLayout fully-qualified type in method body
`List<com.deathrayresearch.forrester.model.def.ConnectorRoute>` used inline instead of importing `ConnectorRoute`.

### 26. String-typed enumerations throughout model/def
`StockDef.negativeValuePolicy`, `LookupTableDef.interpolation`, and `ElementPlacement.type` use free-form strings where enums would be safer.

### 27. No NaN/Infinity guards on double fields
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
| Medium-priority | 11 |
| Low-priority | 16 |
| Style violations | 4 |
| Test coverage gaps | 7 |
| **Total findings** | **38** |
