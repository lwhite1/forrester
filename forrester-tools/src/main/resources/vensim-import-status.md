# Vensim Import — Function & Syntax Support Status

Based on importing 25 models from TU Delft (Pruyt, 2013).

## Supported (fixed during import exercise)

### Batch 1 (15 models)

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

### Batch 2 (10 models)

| Feature | Occurrences | Models |
|---|---|---|
| `DELAY3I` (DELAY3 with initial value) | 3 | Cholera, HousingMarket, ProjectManagement |
| Case-insensitive function names (`min`, `max`) | 1 | Deradicalization |
| `PULSE TRAIN(start, duration, repeat, end)` | 1 (2 uses) | HigherEducation |
| `LOG(x, base)` — two-argument logarithm | 1 | EnergyTransition |
| Non-constant DELAY3 delay time parameter | 1 | HousingMarket |
| Quoted variable names `"name with (parens)"` | 1 | EVsLithium |
| Vensim built-in `TIME_STEP` / `INITIAL_TIME` / `FINAL_TIME` | 1 | HigherEducation |
| `SMOOTHI` → `SMOOTH` approximation | 1 | ProjectManagement |
| `SMOOTH3` → `SMOOTH` approximation | 3 | Deradicalization, HigherEducation, HousingMarket |
| `DELAY1I` → `DELAY3` approximation | 1 (2 uses) | HigherEducation |

## Not yet supported

Functions listed as unsupported in `VensimExprTranslator.UNSUPPORTED_FUNCTIONS`.

| Function | Notes |
|---|---|
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

## Known limitations

- **Algebraic loops** — Models with circular variable references (e.g.,
  ProjectManagement) cause StackOverflow at simulation time. Would need a
  simultaneous equation solver or topological sort with loop-breaking.
- **SMOOTH3 / SMOOTHI / SMOOTH3I** — approximated as first-order SMOOTH.
  Acceptable for import but produces different dynamic behavior.
- **DELAY1 / DELAY1I** — approximated as DELAY3. First-order vs third-order
  delay produces different response shapes.

## Recurring patterns

- **Non-literal INTEG initial values** — most common gap in batch 1 (4/15 models).
  Fixed by storing expressions in `StockDef.initialExpression` and evaluating at
  compile time after all stocks are registered.
- **Unary `+`** — second most common in batch 1 (3/15). Vensim allows `+expr` as a no-op.
- **DELAY variants** — most common gap in batch 2 (4/10 models). DELAY3I, DELAY1I,
  non-constant delay times.
- **Batch 2 hit rate**: ~1.0 new parser/compiler gap per model (10 gaps in 10 models).
  Harder models exercise more features.
- **Trial compile pass rate**: 25/25 models pass trial compilation (100%).
- **Simulation pass rate**: 24/25 models simulate successfully (96%).
  ProjectManagement fails due to algebraic loops.
