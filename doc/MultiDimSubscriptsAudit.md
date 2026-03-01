# Audit: Multi-Dimensional Subscripts Implementation

Scope: `SubscriptRange`, `MultiArrayedStock`, `MultiArrayedFlow`, `MultiArrayedVariable`, modifications to `Model`/`Module`, tests, demo, and documentation updates.

## Bugs (fix now)

| # | File | Issue |
|---|------|-------|
| 3.2 | `MultiArrayedStock.java` | **`addInflow(MultiArrayedFlow)` / `addOutflow(MultiArrayedFlow)` don't validate range compatibility.** Wiring a 2x4 flow to a 3x3 stock silently corrupts or throws a raw `ArrayIndexOutOfBoundsException`. Needs a precondition check. |
| 1.1 | `MultiArrayedStock.java` | **No `NegativeValuePolicy` constructor support.** Always hardcodes `CLAMP_TO_ZERO`. Stocks that legitimately go negative (financial balances, temperature deltas) can't be modeled. Matches the single-dim gap but amplifies it. |
| 1.3 | `SubscriptRange.java:52-57` | **Integer overflow in size computation.** `stride *= subscripts.get(d).size()` uses plain `int` multiplication — pathological dimensions (e.g., 5 dims of 100 = 10B) silently overflow. Should use `Math.multiplyExact`. |

## Design Gaps

| # | File | Issue |
|---|------|-------|
| 2.3 | `MultiArrayedVariable.java` | **Missing `getUnit()` accessor.** `MultiArrayedStock` has it, `MultiArrayedVariable` doesn't — inconsistent within the multi-dim family. |
| 2.4 | `MultiArrayedVariable.java` | **Missing `slice()`.** Stock has `slice()` and `sumOver()`; Variable has only `sumOver()`. |
| 2.5 | `MultiArrayedFlow.java` | **Missing `sum()`, `sumOver()`, `slice()`.** Stock and Variable have aggregation; Flow has none. |
| 8.1 | `Module.java` | **Missing `addMultiArrayedVariable()`.** Module has `addMultiArrayedStock` but not the Variable equivalent. Matches the single-dim gap but still incomplete. |
| 8.4 | `Subscript.java` | **Missing `equals()`/`hashCode()`.** Two Subscripts with same name/labels aren't `.equals()`. Tests pass only because they use identity. Would bite users comparing or using Subscripts as map keys. |
| 2.1/2.2 | `MultiArrayed*.java` | **Naming divergence from single-dim.** Single-dim uses `getValue(String)`, multi-dim uses `getValueAt(String...)`. Intentional (avoids varargs ambiguity) but means porting code requires renames. |

## Minor / Hardening

| # | File | Issue |
|---|------|-------|
| 1.2 | `MultiArrayed*.java` | `getValue(int flatIndex)` / `getStock(int flatIndex)` — raw array access with no bounds check. Inconsistent with `getValueAt(int...)` which gives descriptive errors. |
| 1.4 | `SubscriptRange.java:176,191` | `composeName` / `getLabelsAt` don't validate flatIndex — raw `IndexOutOfBoundsException` while `toCoordinates` has a descriptive check. |
| 7.3 | `MultiArrayedFlow.java:77-85` | Javadoc claims the convenience factory "captures a stock reference" but the `stock` parameter is completely ignored. |

## Performance (non-blocking, latent scalability)

| # | File | Issue |
|---|------|-------|
| 4.1 | `SubscriptRange.java` | Eagerly materializes entire cartesian product. Fine for small ranges, but 5 dims x 20 labels = 3.2M lists allocated in the constructor. Could compute on-the-fly. |
| 3.4 | `Model.java` | `stocks.contains()` is O(n) on ArrayList — O(n*m) when adding m multi-arrayed stocks. Pre-existing but amplified by multi-dim. |
| 4.2/4.3 | `MultiArrayedStock.java` | `sumOver()` and `slice()` allocate `int[]` per element and iterate all elements. Could use stride arithmetic directly. |

## Test Coverage Gaps

| # | Description |
|---|-------------|
| 6.1 | No `sumOver` test with 3+ dimensions |
| 6.2 | No `slice` test with 3+ dimensions |
| 6.3 | No out-of-range dimension index test for `sumOver`/`slice` |
| 6.4 | No mismatched-size flow wiring test |
| 6.5 | No invalid-label test for `toFlatIndex(String...)` |
| 6.6 | No 3D middle-dimension `removeDimension` test |
| 6.7 | No simulation integration test — demo exists but no JUnit test that runs a Simulation with MultiArrayedStock |

## Documentation

| # | Description |
|---|-------------|
| 7.2 | README code example has a backslash typo (`\` instead of `//`) |

## Recommended Fix Priority

1. **Range-compatibility validation on flow wiring (3.2/3.3)** — highest impact; prevents silent data corruption
2. **Test coverage gaps (6.7, 6.4, 6.1-6.3)** — especially the simulation integration test and mismatch wiring test
3. **Missing `getUnit()` on MultiArrayedVariable (2.3)** — simple one-liner, inconsistent without it
4. **NegativeValuePolicy support (1.1)** — blocks legitimate negative-stock models
5. **Integer overflow guard (1.3)** — simple `Math.multiplyExact` swap
6. **Design gaps (2.4, 2.5, 8.1, 8.4)** — lower urgency, improve API completeness
7. **Performance (4.1, 3.4, 4.2/4.3)** — non-blocking for typical 2-3 dimension models
