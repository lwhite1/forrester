# IndexedValue Audit

Audit of `IndexedValue.java`, `IndexedValueTest.java`, and the `getIndexedValue()` convenience methods added to `ArrayedStock`, `ArrayedVariable`, `MultiArrayedStock`, and `MultiArrayedVariable`.

## Priority 1 — ~~Should fix~~ Fixed

### 1.1 ~~No null-parameter validation in factory methods~~ Fixed

**Files:** `IndexedValue.java:55-57`, `IndexedValue.java:73-75`, `IndexedValue.java:87`, `IndexedValue.java:97`

`Preconditions.checkNotNull()` added to all four factory methods: `of(Subscript, double...)`, `of(SubscriptRange, double[])`, `fill(SubscriptRange, double)`, and `fill(Subscript, double)`. Null arguments now throw `NullPointerException` with descriptive messages at the API boundary.

**Tests:** 6 tests in `NullValidation` nested class verify null rejection for every factory method parameter.

### 1.2 ~~`get(int flatIndex)` has no bounds checking~~ Fixed

**File:** `IndexedValue.java:129-133`

`get(int)` now validates bounds explicitly and throws `IndexOutOfBoundsException` with a message including the flat index and array size: `"Flat index X out of range (size=Y)"`.

**Tests:** 3 tests in `BoundsChecking` nested class cover negative index, index equal to size, and index far beyond size.

## Priority 2 — Design issues

### 2.1 ~~Per-element array allocation in broadcasting loop~~ Fixed

**Files:** `IndexedValue.java:300-321` (`sumOver`), `IndexedValue.java:388-422` (`broadcastOp`)

Both `broadcastOp()` and `sumOver()` were rewritten to use stride arithmetic instead of `toCoordinates()`. A new `SubscriptRange.getStrides()` method exposes the precomputed stride array (returns a defensive copy). The inner loops decompose flat indices into coordinates and recompose into left/right flat indices using precomputed stride maps — zero per-iteration array allocations.

This also resolved P3.2 (fragile coordinate array reuse) since `leftCoords` and `rightCoords` arrays no longer exist.

### 2.2 Result dimension order depends on operand order

**File:** `IndexedValue.java:379-386`

Broadcasting puts left dimensions first, then right-only dimensions. This means:
- `[Region] + [AgeGroup]` → `[Region × AgeGroup]`
- `[AgeGroup] + [Region]` → `[AgeGroup × Region]`

Both contain the same values, but the dimension ordering differs. Downstream code using `getAt(int...)` with positional coordinates would get different results depending on operand order.

**Impact:** Could surprise users. Analytica avoids this by maintaining a canonical dimension order per variable. NumPy avoids it by not having named dimensions.

**Not necessarily a bug** — the current behavior is consistent and tested (see `ReversedDimensionOrder` test class). The class Javadoc documents the broadcasting behavior.

### 2.3 No NaN/Infinity validation

**File:** `IndexedValue.java` (entire class)

`Stock.setValue()` rejects NaN and Infinity with descriptive errors. `IndexedValue` silently accepts and propagates them through arithmetic. A NaN in one element will silently corrupt every downstream operation.

**Impact:** Low for correct models. High for debugging when a formula produces NaN — the error surfaces far from the source.

**Fix options:**
- Validate in factory methods (reject NaN/Infinity on construction)
- Validate after each arithmetic operation (catches NaN from 0/0 or Infinity from overflow)
- Document the pass-through behavior and leave validation to the caller

### 2.4 Private constructor doesn't clone its array argument

**File:** `IndexedValue.java:34-37`

The private constructor stores the array reference directly. This is safe today because all call sites pass freshly-created arrays. But if a future code change passes an externally-owned array to the constructor, immutability breaks silently.

**Fix:** Either clone in the constructor (defensive, small perf cost) or add a comment documenting the contract.

## Priority 3 — ~~Test coverage gaps~~ Largely resolved

### 3.1 ~~Missing edge case tests~~ Mostly fixed

| Test | Status |
|---|---|
| Single-element subscript (1 label) | **Fixed** — `SingleElementSubscript` nested class (3 tests) |
| Null arguments to factory methods | **Fixed** — `NullValidation` nested class (6 tests) |
| `getAt(int...)` with wrong coordinate count | **Fixed** — `BoundsChecking.shouldRejectWrongCoordinateCount` |
| `subtract(double)` and `divide(double)` scalar overloads | **Fixed** — `ScalarConvenienceOverloads` nested class (3 tests) |
| `toString()` output | Not tested; minor |
| Same dimensions, different order: `[Region × AgeGroup]` op `[AgeGroup × Region]` | **Fixed** — `ReversedDimensionOrder.shouldAlignSharedDimensionsRegardlessOfOrder` |

### 3.2 ~~Fragile coordinate array reuse in broadcastOp~~ Resolved

No longer applicable. The P2.1 fix replaced the mutable `leftCoords`/`rightCoords` arrays with stride-based arithmetic that computes flat indices directly — no coordinate arrays are allocated or reused.

## Summary

| Priority | Count | Status |
|---|---|---|
| P1 (should fix) | 2 | Both fixed with tests |
| P2 (design issues) | 4 | 1 fixed (2.1), 3 remaining (2.2, 2.3, 2.4) |
| P3 (test gaps) | 2 | Both resolved (only `toString()` test still missing — minor) |

Overall the implementation is correct and hardened — all 509 tests pass, null arguments are rejected at API boundaries, flat index access is bounds-checked, broadcasting and aggregation loops use allocation-free stride arithmetic, and edge cases (single-element subscripts, reversed dimension order, wrong coordinate count) are tested. The remaining items (2.2, 2.3, 2.4) are documentation and defensive-coding considerations, not correctness issues.
