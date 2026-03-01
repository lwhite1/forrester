# IndexedValue Audit

Audit of `IndexedValue.java`, `IndexedValueTest.java`, and the `getIndexedValue()` convenience methods added to `ArrayedStock`, `ArrayedVariable`, `MultiArrayedStock`, and `MultiArrayedVariable`.

## Priority 2 — Design issues

### 2.2 ~~Result dimension order depends on operand order~~ Documented

Broadcasting puts left dimensions first, then right-only dimensions. This means:
- `[Region] + [AgeGroup]` → `[Region × AgeGroup]`
- `[AgeGroup] + [Region]` → `[AgeGroup × Region]`

Both contain the same values, but the dimension ordering differs. Downstream code using `getAt(int...)` with positional coordinates would get different results depending on operand order. Label-based access via `getAt(String...)` is unaffected.

**Resolution:** Documented in class Javadoc ("Result dimension ordering" section) and on each arithmetic method. The current left-first rule is simple, predictable, and consistent. Adopting Analytica-style canonical ordering would require a global ordering rule that `IndexedValue` (a standalone value object) has no context to define. The `ReversedDimensionOrder` test class verifies the behavior.

### 2.3 ~~No NaN/Infinity validation~~ Fixed

**Files:** `IndexedValue.java:66`, `IndexedValue.java:86`, `IndexedValue.java:107`, `IndexedValue.java:117`

All factory methods (`scalar`, `of`, `fill`) now reject NaN and Infinity via `validateFinite()`. Error messages include the index of the offending value for array inputs. Consistent with `Stock.applyPolicy()` which rejects non-finite values.

**Tests:** 10 tests in `NaNInfinityValidation` nested class cover NaN, positive Infinity, and negative Infinity across `scalar()`, `of(Subscript,...)`, `of(SubscriptRange,...)`, `fill(SubscriptRange,...)`, and `fill(Subscript,...)`, plus error message content verification.

### 2.4 Private constructor doesn't clone its array argument

**File:** `IndexedValue.java:34-37`

The private constructor stores the array reference directly. This is safe today because all call sites pass freshly-created arrays. But if a future code change passes an externally-owned array to the constructor, immutability breaks silently.

**Fix:** Either clone in the constructor (defensive, small perf cost) or add a comment documenting the contract.

## Priority 3 — ~~Test coverage gaps~~ Largely resolved



Overall the implementation is correct and hardened — all 519 tests pass, null arguments are rejected at API boundaries, NaN and Infinity are rejected on construction, flat index access is bounds-checked, broadcasting and aggregation loops use allocation-free stride arithmetic, dimension ordering is documented, and edge cases (single-element subscripts, reversed dimension order, wrong coordinate count) are tested. The only remaining item (2.4) is a defensive-coding consideration, not a correctness issue.
