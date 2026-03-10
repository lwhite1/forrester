# Forrester Expression Language Reference

This document describes the expression language used in flow equations, auxiliary equations, and lookup table inputs.

## Basics

Expressions are written as standard mathematical formulas. Variable names reference other elements in the model (stocks, constants, auxiliaries, flows, lookup tables).

```
Birth_Rate * Population
```

Variable names use underscores in equations. Spaces in element names are automatically converted to underscores. You can also use backtick-quoted names for multi-word identifiers:

```
`Birth Rate` * Population
```

## Numbers

Numeric literals support integers, decimals, and scientific notation:

```
42
3.14
0.5
.5
1.5e-3
```

## Operators

### Arithmetic

| Operator | Description | Example |
|----------|-------------|---------|
| `+` | Addition | `Population + 100` |
| `-` | Subtraction | `Births - Deaths` |
| `*` | Multiplication | `Rate * Population` |
| `/` | Division | `Total / Count` |
| `%` | Modulo (remainder) | `Step % 7` |
| `**` | Exponentiation | `2 ** 10` |
| `-` (unary) | Negation | `-Rate` |

Division by zero returns 0 (safe division).

### Comparison

Comparisons return `1` (true) or `0` (false).

| Operator | Description | Example |
|----------|-------------|---------|
| `==` | Equal | `Status == 1` |
| `!=` | Not equal | `Mode != 0` |
| `<` | Less than | `Stock < 0` |
| `<=` | Less than or equal | `Level <= Threshold` |
| `>` | Greater than | `Demand > Supply` |
| `>=` | Greater than or equal | `Age >= 18` |

Equality uses epsilon comparison (tolerance of 1e-10) to handle floating-point precision.

### Logical

Logical operators treat `0` as false and any non-zero value as true. Keywords are case-insensitive.

| Operator | Description | Example |
|----------|-------------|---------|
| `and` | And | `A > 0 and B > 0` |
| `or` | Or | `Shortage or Emergency` |
| `not` | Not | `not Active` |

### Operator Precedence

From highest to lowest binding:

1. `**` (exponentiation, right-associative)
2. `*`, `/`, `%` (multiplicative)
3. `+`, `-` (additive)
4. `==`, `!=`, `<`, `<=`, `>`, `>=` (comparison)
5. `and` (logical and)
6. `or` (logical or)

Use parentheses to override precedence: `(A + B) * C`

## Special Variables

| Variable | Description |
|----------|-------------|
| `TIME` | Current simulation timestep (starts at 0) |
| `DT` | Simulation time step size (default 1.0) |

## Conditional

```
IF(condition, then_value, else_value)
```

Returns `then_value` if `condition` is non-zero, otherwise `else_value`.

```
IF(Inventory > 0, Order_Rate, 0)
IF(Population > Capacity, Capacity, Population)
```

## Math Functions

| Function | Arguments | Description |
|----------|-----------|-------------|
| `ABS(x)` | 1 | Absolute value |
| `SQRT(x)` | 1 | Square root |
| `EXP(x)` | 1 | e raised to the power x |
| `LN(x)` | 1 | Natural logarithm (base e) |
| `LOG(x)` | 1 | Common logarithm (base 10) |
| `MIN(a, b)` | 2 | Smaller of two values |
| `MAX(a, b)` | 2 | Larger of two values |
| `SIN(x)` | 1 | Sine (x in radians) |
| `COS(x)` | 1 | Cosine (x in radians) |
| `TAN(x)` | 1 | Tangent (x in radians) |
| `INT(x)` | 1 | Truncate to integer (toward zero) |
| `ROUND(x)` | 1 | Round to nearest integer |
| `MODULO(a, b)` | 2 | Remainder of a / b (returns 0 if b is 0) |
| `POWER(a, b)` | 2 | a raised to the power b (equivalent to `a ^ b`) |
| `SUM(a, b, ...)` | 1+ | Sum of all arguments |
| `MEAN(a, b, ...)` | 1+ | Arithmetic mean of all arguments |

## System Dynamics Functions

### SMOOTH — Exponential Smoothing

```
SMOOTH(input, smoothing_time)
SMOOTH(input, smoothing_time, initial_value)
```

First-order exponential smoothing. Smooths the input signal over the specified time. If no initial value is given, the first input value is used.

### DELAY3 — Third-Order Material Delay

```
DELAY3(input, delay_time)
DELAY3(input, delay_time, initial_value)
```

Third-order material delay. Delays the input signal by approximately `delay_time` steps with a smooth S-shaped response. If no initial value is given, the first input value is used.

### STEP — Step Input

```
STEP(height, step_time)
```

Returns 0 before `step_time`, then returns `height` at and after `step_time`.

```
STEP(100, 10)    -- 0 for steps 0-9, then 100 from step 10 onward
```

### RAMP — Linear Ramp

```
RAMP(slope, start_time)
RAMP(slope, start_time, end_time)
```

Returns 0 before `start_time`, then increases linearly at `slope` per timestep. If `end_time` is specified, the value holds constant after that point.

```
RAMP(5, 10)       -- 0 until step 10, then +5 per step
RAMP(5, 10, 20)   -- 0 until step 10, +5 per step until step 20, then holds at 50
```

### PULSE — Impulse Input

```
PULSE(magnitude, start_time)
PULSE(magnitude, start_time, interval)
```

Returns `magnitude` for one timestep at `start_time`, then 0. If `interval` is specified, the pulse repeats every `interval` timesteps.

```
PULSE(100, 5)       -- 100 at step 5, 0 everywhere else
PULSE(100, 5, 10)   -- 100 at steps 5, 15, 25, ...
```

### DELAY_FIXED — Fixed Pipeline Delay

```
DELAY_FIXED(input, delay_time, initial_value)
```

Returns the input value from exactly `delay_time` timesteps ago. Unlike DELAY3 (which smooths the output through three stages), DELAY_FIXED produces a pure time-shifted copy — a step change in input appears as a step change in output after the delay. Returns `initial_value` until the delay has elapsed.

```
DELAY_FIXED(Orders, 5, 0)   -- output equals Orders from 5 steps ago; 0 before step 5
```

### TREND — Fractional Rate of Change

```
TREND(input, averaging_time, initial_trend)
```

Estimates the fractional rate of change of the input using exponential smoothing. Returns the growth rate per timestep (e.g., 0.05 means 5% growth per step). Useful for detecting whether a quantity is growing or declining.

```
TREND(Revenue, 12, 0)   -- estimate revenue growth rate over 12-step window
```

### FORECAST — Linear Extrapolation

```
FORECAST(input, averaging_time, horizon, initial_trend)
```

Estimates where the input will be after `horizon` timesteps, based on its current trend. Uses exponential smoothing to detect the trend, then extrapolates linearly.

```
FORECAST(Demand, 10, 5, 0)   -- predict demand 5 steps ahead using 10-step trend
```

### NPV — Net Present Value

```
NPV(stream, discount_rate)
NPV(stream, discount_rate, factor)
```

Accumulates the discounted present value of a stream of payments. The discount rate is the fractional rate per timestep. The optional factor is a multiplier applied to each payment before discounting (default 1).

```
NPV(Cash_Flow, 0.05)       -- accumulate PV at 5% discount per step
NPV(Cash_Flow, 0.05, 0.5)  -- same, but each payment weighted by 0.5
```

### LOOKUP — Table Lookup

```
LOOKUP(table_name, input_value)
```

Looks up `input_value` in the named lookup table and returns the interpolated output. The lookup table must be defined as a separate element in the model.

```
LOOKUP(Effect_of_Density, Population / Area)
```

### RANDOM_NORMAL — Random Normal Distribution

```
RANDOM_NORMAL(min, max, mean, std_dev)
```

Returns a random value from a normal distribution with the specified mean and standard deviation, clamped to the range [min, max]. Each evaluation returns a new random value.

```
RANDOM_NORMAL(0, 200, 100, 20)   -- normal around 100, std dev 20, clamped to [0, 200]
```

## Common Equation Patterns

### Exponential Growth

```
Growth_Rate * Population
```
Used as a flow equation feeding into a stock.

### Exponential Decay

```
Decay_Rate * Remaining
```
Used as an outflow from a stock.

### Logistic Growth

```
Growth_Rate * Population * (1 - Population / Carrying_Capacity)
```

### Goal-Seeking (Gap-Closing)

```
(Goal - Current) / Adjustment_Time
```

### Conditional Logic

```
IF(Inventory > Reorder_Point, 0, Order_Quantity)
```

### Seasonal Input

```
Base_Value * (1 + Amplitude * SIN(2 * 3.14159 * TIME / Period))
```

### Clamping to Non-Negative

```
MAX(0, Calculated_Rate)
```

## Grammar

For reference, the formal grammar of the expression language:

```
expr       = or_expr
or_expr    = and_expr ( "or" and_expr )*
and_expr   = comparison ( "and" comparison )*
comparison = addition ( ("==" | "!=" | "<" | "<=" | ">" | ">=") addition )?
addition   = mult ( ("+" | "-") mult )*
mult       = power ( ("*" | "/" | "%") power )*
power      = unary ( "**" power )?
unary      = ("-" | "not") unary | call
call       = primary ( "(" arglist? ")" )?
primary    = NUMBER | IDENTIFIER | QUOTED_ID | "(" expr ")"
           | "IF" "(" expr "," expr "," expr ")"
arglist    = expr ( "," expr )*
```

- `NUMBER`: integer, decimal, or scientific notation (`42`, `3.14`, `1e-3`)
- `IDENTIFIER`: letters, digits, and underscores, starting with a letter or underscore
- `QUOTED_ID`: backtick-delimited name (`` `My Variable` ``)
