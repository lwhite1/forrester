# IndexedValue Audit

Audit of `IndexedValue.java`, `IndexedValueTest.java`, and the `getIndexedValue()` convenience methods added to `ArrayedStock`, `ArrayedVariable`, `MultiArrayedStock`, and `MultiArrayedVariable`.

## Priority 1 — Should fix

### 1.1 No null-parameter validation in factory methods

**Files:** `IndexedValue.java:53`, `IndexedValue.java:69`

`of(Subscript, double...)` and `of(SubscriptRange, double[])` don't validate null arguments. Passing `null` produces a confusing `NullPointerException` deep inside `SubscriptRange` or at `values.length` rather than a clear error at the API boundary.

**Fix:** Add `Preconditions.checkNotNull()` or explicit null checks at the top of each factory method. Consistent with how `Stock`, `Variable`, and `Quantity` validate their constructor arguments.

### 1.2 `get(int flatIndex)` has no bounds checking

**File:** `IndexedValue.java:119-121`

`get(int)` directly indexes into the raw array. An out-of-range index produces `ArrayIndexOutOfBoundsException` with no context about the IndexedValue's size. By contrast, `getAt(int...)` delegates to `SubscriptRange.toFlatIndex()` which throws `IllegalArgumentException` with a descriptive message.

**Fix:** Add a bounds check: `if (flatIndex < 0 || flatIndex >= values.length) throw new IndexOutOfBoundsException(...)`. Or at minimum, document that it throws `ArrayIndexOutOfBoundsException`.

## Priority 2 — Design issues

### 2.1 Per-element array allocation in broadcasting loop

**File:** `IndexedValue.java:384`, `IndexedValue.java:288`

Both `broadcastOp()` and `sumOver()` call `resultRange.toCoordinates(i)` inside a loop, which allocates a new `int[]` on every iteration. For a `[100 × 100 × 10]` value (100,000 elements), this creates 100K short-lived arrays per operation.

**Impact:** Not a problem for typical SD models (subscripts rarely exceed ~50 elements per dimension). Would matter if IndexedValue were used in inner-loop Monte Carlo or optimization where the same operation runs thousands of times.

**Fix (if needed):** Inline the coordinate decomposition using stride arithmetic instead of delegating to `toCoordinates()`. The strides are already computed inside `SubscriptRange` but not publicly exposed.

### 2.2 Result dimension order depends on operand order

**File:** `IndexedValue.java:357-363`

Broadcasting puts left dimensions first, then right-only dimensions. This means:
- `[Region] + [AgeGroup]` → `[Region × AgeGroup]`
- `[AgeGroup] + [Region]` → `[AgeGroup × Region]`

Both contain the same values, but the dimension ordering differs. Downstream code using `getAt(int...)` with positional coordinates would get different results depending on operand order.

**Impact:** Could surprise users. Analytica avoids this by maintaining a canonical dimension order per variable. NumPy avoids it by not having named dimensions.

**Not necessarily a bug** — the current behavior is consistent and tested. But should be documented clearly in the class javadoc.

### 2.3 No NaN/Infinity validation

**File:** `IndexedValue.java` (entire class)

`Stock.setValue()` rejects NaN and Infinity with descriptive errors. `IndexedValue` silently accepts and propagates them through arithmetic. A NaN in one element will silently corrupt every downstream operation.

**Impact:** Low for correct models. High for debugging when a formula produces NaN — the error surfaces far from the source.

**Fix options:**
- Validate in factory methods (reject NaN/Infinity on construction)
- Validate after each arithmetic operation (catches NaN from 0/0 or Infinity from overflow)
- Document the pass-through behavior and leave validation to the caller

### 2.4 Private constructor doesn't clone its array argument

**File:** `IndexedValue.java:32-35`

The private constructor stores the array reference directly. This is safe today because all call sites pass freshly-created arrays. But if a future code change passes an externally-owned array to the constructor, immutability breaks silently.

**Fix:** Either clone in the constructor (defensive, small perf cost) or add a comment documenting the contract.

## Priority 3 — Test coverage gaps

### 3.1 Missing edge case tests

| Missing test | Risk |
|---|---|
| Single-element subscript (1 label) | Broadcasting with a 1-element dimension is a degenerate case that should work but isn't tested |
| Null arguments to factory methods | Should verify clean error messages after P1.1 fix |
| `getAt(int...)` with wrong coordinate count | Delegates to SubscriptRange, which throws — but not tested through IndexedValue |
| `subtract(double)` and `divide(double)` scalar convenience overloads | Only `add(double)` and `multiply(double)` are tested via the ScalarBroadcast group |
| `toString()` output | Not tested; minor |
| Same dimensions, different order: `[Region × AgeGroup]` op `[AgeGroup × Region]` | Shared dimensions should align by name regardless of position. The `shouldBroadcast_ageGroup_plus_regionAge` test covers a partial case but doesn't test two multi-dimensional operands with reversed dimension order |

### 3.2 Fragile coordinate array reuse in broadcastOp

**File:** `IndexedValue.java:380-381`

`leftCoords` and `rightCoords` are allocated once and mutated each iteration. Every entry is overwritten because the result range contains all dimensions from both operands. But if the dimension-mapping logic were changed to skip a dimension, stale values from the previous iteration would silently produce wrong results.

**Recommendation:** Add a comment explaining the invariant, or zero the arrays at the top of each iteration (small perf cost, safer).

## Summary

| Priority | Count | Effort |
|---|---|---|
| P1 (should fix) | 2 | ~30 min |
| P2 (design issues) | 4 | ~1-2 hours if addressed |
| P3 (test gaps) | 2 | ~1 hour |

Overall the implementation is correct — all 53 tests pass, the broadcasting logic handles the tested scenarios, and immutability is maintained through defensive copying in factory methods. The issues above are hardening items, not correctness bugs.
