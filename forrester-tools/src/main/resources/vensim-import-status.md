# Vensim Import — Function & Syntax Support Status

Based on importing 15 models from TU Delft (Pruyt, 2013).

## Supported (fixed during import exercise)

| Feature | Occurrences | Models |
|---|---|---|
| Unary `+` operator | 3 | EcologicalOvershoot, KaibabDeer, FamilyPlanning |
| `GAME()` pass-through | 1 (4 uses) | PneumonicPlague |
| `RANDOM UNIFORM(min, max, seed)` | 1 | FeralPigs |
| Standalone lookup call `table(x)` → `LOOKUP(table, x)` | 1 | BluefinTuna |
| WITH LOOKUP range annotation in outer parens | 1 | BluefinTuna |
| `DELAY_FIXED` with expression initial value | 1 | BluefinTuna |
| INTEG initial value: constant name reference | 1 | MuskratPlague |
| INTEG initial value: arbitrary expression | 3 | PneumonicPlague, SupplyChain, RealEstateBoom |
| Duplicate x-values in lookup tables | 1 | DebtCrisis |

## Not yet supported

Functions listed as unsupported in `VensimExprTranslator.UNSUPPORTED_FUNCTIONS`.
None of these have caused a trial-compile failure yet — they generate import warnings.

| Function | Notes |
|---|---|
| `PULSE` | Generates a one-time pulse. Straightforward to implement. |
| `PULSE TRAIN` | Repeating pulse. Needs PULSE first. |
| `DELAY N` | Nth-order material delay. We have DELAY3 (3rd-order) and DELAY_FIXED already. |
| `GET XLS DATA` | Reads time-series from Excel. External data dependency — unlikely to support. |
| `GET DIRECT DATA` | Reads data from external file. Same issue as GET XLS DATA. |
| `GET DIRECT CONSTANTS` | Reads constants from external file. Same issue. |
| `TABBED ARRAY` | Inline array definition. Would need subscript/array support. |
| `SAMPLE IF TRUE` | Conditional sampling. Niche. |
| `VECTOR SELECT` | Vector operations. Would need array support. |
| `VECTOR ELM MAP` | Vector element mapping. Would need array support. |
| `VECTOR SORT ORDER` | Vector sorting. Would need array support. |
| `ALLOCATE AVAILABLE` | Resource allocation across subscripts. Complex. |
| `FIND ZERO` | Numerical root-finding. Niche. |

## Recurring patterns

- **Non-literal INTEG initial values** — most common gap (4/15 models). Fixed by
  storing expressions in `StockDef.initialExpression` and evaluating at compile
  time after all stocks are registered.
- **Unary `+`** — second most common (3/15). Vensim allows `+expr` as a no-op.
- **BluefinTuna** was the hardest single model, hitting 3 gaps on its own.
- Hit rate: ~1 new parser/compiler gap per 2 models. Should improve as coverage grows.
