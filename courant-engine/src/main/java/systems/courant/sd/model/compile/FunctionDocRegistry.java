package systems.courant.sd.model.compile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of documentation for all built-in functions recognized by the expression compiler.
 * This is the single source of truth — autocomplete, tooltips, and help dialogs all read from here.
 */
public final class FunctionDocRegistry {

    private static final Map<String, FunctionDoc> DOCS;

    static {
        Map<String, FunctionDoc> m = new LinkedHashMap<>();

        // ── Special variables ───────────────────────────────────────────
        add(m, "TIME", "TIME", "Current simulation time",
                "Special",
                List.of(),
                "Returns the current simulation time step. At the start of a simulation "
                        + "this equals the initial time; it advances by DT each step.",
                "TIME → 0, 1, 2, 3, ... (with DT=1)",
                List.of("DT"));

        add(m, "DT", "DT", "Simulation time step size",
                "Special",
                List.of(),
                "Returns the size of each simulation time step. This is set in the "
                        + "simulation settings and remains constant throughout a run.",
                "DT → 0.25 (if time step is 0.25)",
                List.of("TIME"));

        // ── Math functions ──────────────────────────────────────────────
        add(m, "ABS", "ABS(x)", "Absolute value",
                "Math",
                List.of(p("x", "numeric value")),
                "Returns the absolute (non-negative) value of x.",
                "ABS(-5) → 5",
                List.of("MIN", "MAX"));

        add(m, "SQRT", "SQRT(x)", "Square root",
                "Math",
                List.of(p("x", "non-negative numeric value")),
                "Returns the square root of x. Returns NaN if x is negative.",
                "SQRT(16) → 4",
                List.of("POWER", "EXP"));

        add(m, "LN", "LN(x)", "Natural logarithm",
                "Math",
                List.of(p("x", "positive numeric value")),
                "Returns the natural logarithm (base e) of x. Returns NaN if x ≤ 0.",
                "LN(2.718) → ~1.0",
                List.of("EXP", "LOG"));

        add(m, "EXP", "EXP(x)", "Exponential (e^x)",
                "Math",
                List.of(p("x", "numeric value")),
                "Returns e raised to the power x. Returns NaN on overflow.",
                "EXP(1) → 2.718...",
                List.of("LN", "POWER"));

        add(m, "LOG", "LOG(x [, base])", "Logarithm",
                "Math",
                List.of(p("x", "positive numeric value"),
                        p("base", "(optional) logarithm base; defaults to 10")),
                "Returns the logarithm of x. With one argument, computes base-10 logarithm. "
                        + "With two arguments, computes log base 'base' of x.",
                "LOG(100) → 2\nLOG(8, 2) → 3",
                List.of("LN", "EXP"));

        add(m, "SIN", "SIN(x)", "Sine",
                "Math",
                List.of(p("x", "angle in radians")),
                "Returns the sine of x (radians).",
                "SIN(3.14159) → ~0",
                List.of("COS", "TAN"));

        add(m, "COS", "COS(x)", "Cosine",
                "Math",
                List.of(p("x", "angle in radians")),
                "Returns the cosine of x (radians).",
                "COS(0) → 1",
                List.of("SIN", "TAN"));

        add(m, "TAN", "TAN(x)", "Tangent",
                "Math",
                List.of(p("x", "angle in radians")),
                "Returns the tangent of x (radians).",
                "TAN(0) → 0",
                List.of("SIN", "COS"));

        add(m, "ARCSIN", "ARCSIN(x)", "Inverse sine (arcsine)",
                "Math",
                List.of(p("x", "value in range [-1, 1]")),
                "Returns the arc sine of x in radians, in the range [-π/2, π/2]. "
                        + "Returns NaN if x is outside [-1, 1].",
                "ARCSIN(1) → 1.5708 (π/2)",
                List.of("ARCCOS", "ARCTAN", "SIN"));

        add(m, "ARCCOS", "ARCCOS(x)", "Inverse cosine (arccosine)",
                "Math",
                List.of(p("x", "value in range [-1, 1]")),
                "Returns the arc cosine of x in radians, in the range [0, π]. "
                        + "Returns NaN if x is outside [-1, 1].",
                "ARCCOS(0) → 1.5708 (π/2)",
                List.of("ARCSIN", "ARCTAN", "COS"));

        add(m, "ARCTAN", "ARCTAN(x)", "Inverse tangent (arctangent)",
                "Math",
                List.of(p("x", "numeric value")),
                "Returns the arc tangent of x in radians, in the range [-π/2, π/2].",
                "ARCTAN(1) → 0.7854 (π/4)",
                List.of("ARCSIN", "ARCCOS", "TAN"));

        add(m, "SIGN", "SIGN(x)", "Sign of a value",
                "Math",
                List.of(p("x", "numeric value")),
                "Returns -1 if x is negative, 0 if x is zero, and 1 if x is positive.",
                "SIGN(-5) → -1\nSIGN(0) → 0\nSIGN(3) → 1",
                List.of("ABS"));

        add(m, "PI", "PI", "The constant π",
                "Math",
                List.of(),
                "Returns the mathematical constant π (3.14159265...). "
                        + "Useful for trigonometric calculations.",
                "PI → 3.14159...\nSIN(PI / 2) → 1",
                List.of("SIN", "COS", "TAN"));

        add(m, "INT", "INT(x)", "Truncate toward zero",
                "Math",
                List.of(p("x", "numeric value")),
                "Truncates x toward zero: removes the fractional part. "
                        + "INT(3.7) = 3, INT(-3.7) = -3.",
                "INT(3.7) → 3\nINT(-3.7) → -3",
                List.of("ROUND"));

        add(m, "ROUND", "ROUND(x)", "Round to nearest integer",
                "Math",
                List.of(p("x", "numeric value")),
                "Rounds x to the nearest integer using standard rounding (half-up).",
                "ROUND(3.5) → 4\nROUND(3.4) → 3",
                List.of("INT"));

        add(m, "MIN", "MIN(a, b)", "Minimum of two values",
                "Math",
                List.of(p("a", "first value"), p("b", "second value")),
                "Returns the smaller of a and b.",
                "MIN(3, 7) → 3",
                List.of("MAX", "ABS"));

        add(m, "MAX", "MAX(a, b)", "Maximum of two values",
                "Math",
                List.of(p("a", "first value"), p("b", "second value")),
                "Returns the larger of a and b.",
                "MAX(3, 7) → 7",
                List.of("MIN", "ABS"));

        add(m, "QUANTUM", "QUANTUM(x, quantum)", "Round down to nearest multiple",
                "Math",
                List.of(p("x", "numeric value"), p("quantum", "the quantum size")),
                "Rounds x down to the nearest multiple of quantum. "
                        + "If quantum is zero, returns x unchanged.",
                "QUANTUM(7.5, 2) → 6\nQUANTUM(10, 3) → 9",
                List.of("INT", "ROUND", "MODULO"));

        add(m, "MODULO", "MODULO(a, b)", "Remainder after division",
                "Math",
                List.of(p("a", "dividend"), p("b", "divisor")),
                "Returns the remainder of a divided by b. Returns NaN if b is zero.",
                "MODULO(7, 3) → 1",
                List.of("INT", "POWER"));

        add(m, "POWER", "POWER(base, exponent)", "Raise to a power",
                "Math",
                List.of(p("base", "the base value"), p("exponent", "the power to raise to")),
                "Returns base raised to the power exponent. Equivalent to base ** exponent.",
                "POWER(2, 10) → 1024",
                List.of("SQRT", "EXP"));

        add(m, "SUM", "SUM(a, b, ...)", "Sum of values",
                "Math",
                List.of(p("a, b, ...", "one or more numeric values")),
                "Returns the sum of all arguments.",
                "SUM(1, 2, 3) → 6",
                List.of("MEAN", "PROD"));

        add(m, "MEAN", "MEAN(a, b, ...)", "Average of values",
                "Math",
                List.of(p("a, b, ...", "one or more numeric values")),
                "Returns the arithmetic mean (average) of all arguments.",
                "MEAN(2, 4, 6) → 4",
                List.of("SUM", "VMIN", "VMAX"));

        add(m, "VMIN", "VMIN(a, b, ...)", "Minimum of multiple values",
                "Math",
                List.of(p("a, b, ...", "one or more numeric values")),
                "Returns the smallest of all arguments. Unlike MIN which takes exactly "
                        + "two arguments, VMIN accepts any number of arguments.",
                "VMIN(5, 2, 8, 1) → 1",
                List.of("VMAX", "MIN"));

        add(m, "VMAX", "VMAX(a, b, ...)", "Maximum of multiple values",
                "Math",
                List.of(p("a, b, ...", "one or more numeric values")),
                "Returns the largest of all arguments. Unlike MAX which takes exactly "
                        + "two arguments, VMAX accepts any number of arguments.",
                "VMAX(5, 2, 8, 1) → 8",
                List.of("VMIN", "MAX"));

        add(m, "PROD", "PROD(a, b, ...)", "Product of values",
                "Math",
                List.of(p("a, b, ...", "one or more numeric values")),
                "Returns the product of all arguments.",
                "PROD(2, 3, 4) → 24",
                List.of("SUM", "POWER"));

        // ── Safe division ──────────────────────────────────────────────
        add(m, "XIDZ", "XIDZ(a, b, x)", "Safe divide with fallback",
                "Math",
                List.of(p("a", "numerator"), p("b", "denominator"),
                        p("x", "value to return if b is zero")),
                "Returns a / b if b is non-zero, otherwise returns x. "
                        + "Stands for 'X If Divide by Zero'. Useful for avoiding "
                        + "division-by-zero errors in equations.",
                "XIDZ(10, 2, 0) → 5\nXIDZ(10, 0, -1) → -1",
                List.of("ZIDZ"));

        add(m, "ZIDZ", "ZIDZ(a, b)", "Safe divide returning zero",
                "Math",
                List.of(p("a", "numerator"), p("b", "denominator")),
                "Returns a / b if b is non-zero, otherwise returns 0. "
                        + "Stands for 'Zero If Divide by Zero'. Equivalent to XIDZ(a, b, 0).",
                "ZIDZ(10, 2) → 5\nZIDZ(10, 0) → 0",
                List.of("XIDZ"));

        add(m, "INITIAL", "INITIAL(expr)", "Value at initial time",
                "SD",
                List.of(p("expr", "expression to evaluate at time zero")),
                "Evaluates the expression once at the initial time step and returns "
                        + "that value for all subsequent time steps. Useful for capturing "
                        + "starting conditions.",
                "INITIAL(Population) → value of Population at t=0",
                List.of("TIME"));

        // ── SD functions ────────────────────────────────────────────────
        add(m, "STEP", "STEP(height, step_time)", "Step function at a point in time",
                "SD",
                List.of(p("height", "value returned after the step activates"),
                        p("step_time", "simulation time at which the step occurs")),
                "Returns 0 for all time steps before step_time, then returns height "
                        + "from step_time onward. Useful for introducing sudden changes "
                        + "or activating inputs at a specific time.",
                "STEP(10, 3) → t0:0, t1:0, t2:0, t3:10, t4:10, ...",
                List.of("RAMP", "PULSE"));

        add(m, "RAMP", "RAMP(slope, start_time [, end_time])", "Linear ramp over time",
                "SD",
                List.of(p("slope", "rate of increase per time unit"),
                        p("start_time", "simulation time at which the ramp begins"),
                        p("end_time", "(optional) time at which the ramp stops increasing")),
                "Returns 0 before start_time, then increases linearly at the given slope. "
                        + "If end_time is specified, the output levels off at that time.",
                "RAMP(2, 3) → t0:0, t3:0, t4:2, t5:4, t6:6, ...\n"
                        + "RAMP(2, 3, 5) → t3:0, t4:2, t5:4, t6:4, ...",
                List.of("STEP", "PULSE"));

        add(m, "PULSE", "PULSE(magnitude, start_time [, interval])", "Pulse at one or more times",
                "SD",
                List.of(p("magnitude", "value of the pulse"),
                        p("start_time", "simulation time of the first pulse"),
                        p("interval", "(optional) repeat interval; 0 or omitted = single pulse")),
                "Returns magnitude for a single time step at start_time, then 0. "
                        + "If interval is given, repeats every interval steps.",
                "PULSE(5, 3) → t0:0, t3:5, t4:0, ...\n"
                        + "PULSE(5, 2, 4) → t2:5, t3:0, ..., t6:5, t7:0, ...",
                List.of("STEP", "RAMP", "PULSE_TRAIN"));

        add(m, "PULSE_TRAIN", "PULSE_TRAIN(start, duration, repeat, end)",
                "Repeating rectangular pulse",
                "SD",
                List.of(p("start", "time of first pulse"),
                        p("duration", "how long each pulse lasts"),
                        p("repeat", "interval between pulse starts"),
                        p("end", "time after which no more pulses occur")),
                "Returns 1 during each pulse window and 0 otherwise. Pulses start at 'start', "
                        + "last for 'duration' time units, repeat every 'repeat' time units, "
                        + "and stop after 'end'.",
                "PULSE_TRAIN(2, 1, 4, 20) → 1 at t2, 0 at t3-t5, 1 at t6, ...",
                List.of("PULSE", "STEP"));

        add(m, "SMOOTH", "SMOOTH(input, time [, initial])", "Exponential smoothing (1st order)",
                "SD",
                List.of(p("input", "the value to smooth"),
                        p("time", "averaging time (smoothing period)"),
                        p("initial", "(optional) initial output value; defaults to first input")),
                "First-order exponential smoothing. The output adjusts gradually toward "
                        + "the input with a time constant equal to 'time'. "
                        + "Equivalent to a stock: Smooth = Smooth + (input - Smooth) / time * DT.",
                "SMOOTH(Revenue, 4) → smooths Revenue over 4 time units",
                List.of("DELAY3", "DELAY_FIXED"));

        add(m, "SMOOTHI", "SMOOTHI(input, time, initial)", "Exponential smoothing with initial (1st order)",
                "SD",
                List.of(p("input", "the value to smooth"),
                        p("time", "averaging time (smoothing period)"),
                        p("initial", "initial output value at time zero")),
                "First-order exponential smoothing with a caller-specified initial value. "
                        + "Behaves like SMOOTH but starts from 'initial' instead of the first input value.",
                "SMOOTHI(Revenue, 4, 1000) → smooths Revenue over 4 time units, starting at 1000",
                List.of("SMOOTH", "SMOOTH3", "SMOOTH3I"));

        add(m, "SMOOTH3", "SMOOTH3(input, time [, initial])", "Exponential smoothing (3rd order)",
                "SD",
                List.of(p("input", "the value to smooth"),
                        p("time", "averaging time (smoothing period)"),
                        p("initial", "(optional) initial output value; defaults to first input")),
                "Third-order exponential smoothing. Chains three first-order smooths, each "
                        + "with time constant time/3. Produces a more delayed, S-shaped response "
                        + "compared to first-order SMOOTH.",
                "SMOOTH3(Revenue, 6) → third-order smooth of Revenue over 6 time units",
                List.of("SMOOTH", "SMOOTHI", "SMOOTH3I"));

        add(m, "SMOOTH3I", "SMOOTH3I(input, time, initial)", "Exponential smoothing with initial (3rd order)",
                "SD",
                List.of(p("input", "the value to smooth"),
                        p("time", "averaging time (smoothing period)"),
                        p("initial", "initial output value at time zero")),
                "Third-order exponential smoothing with a caller-specified initial value. "
                        + "Combines the S-shaped response of SMOOTH3 with explicit initialization.",
                "SMOOTH3I(Revenue, 6, 1000) → third-order smooth starting at 1000",
                List.of("SMOOTH", "SMOOTHI", "SMOOTH3"));

        add(m, "DELAY1", "DELAY1(input, delay_time [, initial])", "First-order material delay",
                "SD",
                List.of(p("input", "the value to delay"),
                        p("delay_time", "average delay duration"),
                        p("initial", "(optional) initial output value; defaults to first input")),
                "Delays the input using a first-order exponential delay (single stage). "
                        + "Produces an immediate partial response that decays exponentially. "
                        + "The average delay equals delay_time.",
                "DELAY1(Orders, 5) → orders delayed by ~5 time units (exponential)",
                List.of("DELAY1I", "DELAY3", "SMOOTH", "DELAY_FIXED"));

        add(m, "DELAY1I", "DELAY1I(input, delay_time, initial)",
                "First-order material delay with initial value",
                "SD",
                List.of(p("input", "the value to delay"),
                        p("delay_time", "average delay duration"),
                        p("initial", "initial output value at time zero")),
                "First-order exponential delay with a caller-specified initial value. "
                        + "Behaves like DELAY1 but starts from 'initial' instead of the first input value.",
                "DELAY1I(Orders, 5, 100) → first-order delay starting at 100",
                List.of("DELAY1", "DELAY3I", "SMOOTHI"));

        add(m, "DELAY3", "DELAY3(input, delay_time [, initial])", "Third-order material delay",
                "SD",
                List.of(p("input", "the value to delay"),
                        p("delay_time", "average delay duration"),
                        p("initial", "(optional) initial output value; defaults to first input")),
                "Delays the input using a third-order exponential delay (pipeline of "
                        + "3 first-order delays). The output represents a smoothed, lagged "
                        + "version of the input. The average delay equals delay_time.",
                "DELAY3(Orders, 5) → orders delayed by ~5 time units (smoothed)",
                List.of("DELAY3I", "SMOOTH", "DELAY_FIXED"));

        add(m, "DELAY3I", "DELAY3I(input, delay_time, initial)",
                "Third-order material delay with initial value",
                "SD",
                List.of(p("input", "the value to delay"),
                        p("delay_time", "average delay duration"),
                        p("initial", "initial output value at time zero")),
                "Third-order exponential delay with a caller-specified initial value. "
                        + "Behaves like DELAY3 but starts from 'initial' instead of the first input value.",
                "DELAY3I(Orders, 5, 100) → third-order delay starting at 100",
                List.of("DELAY3", "DELAY1I", "SMOOTH3I"));

        add(m, "DELAY_FIXED", "DELAY_FIXED(input, delay_time, initial)",
                "Fixed pipeline delay (exact)",
                "SD",
                List.of(p("input", "the value to delay"),
                        p("delay_time", "exact number of time steps to delay"),
                        p("initial", "output value during the initial delay period")),
                "Delays the input by exactly delay_time steps. Unlike DELAY3 which smooths, "
                        + "DELAY_FIXED preserves the exact shape of the input, shifted in time. "
                        + "During the first delay_time steps, returns 'initial'.",
                "DELAY_FIXED(Shipments, 3, 0) → t0:0, t1:0, t2:0, t3:Shipments(t0), ...",
                List.of("DELAY3", "SMOOTH"));

        add(m, "TREND", "TREND(input, averaging_time, initial_trend)",
                "Fractional rate of change",
                "SD",
                List.of(p("input", "the value to measure trend of"),
                        p("averaging_time", "time period for trend calculation"),
                        p("initial_trend", "initial fractional growth rate")),
                "Estimates the fractional rate of change of the input over the averaging period. "
                        + "Returns a value like 0.05 meaning 5% growth per time unit. "
                        + "Uses exponential smoothing internally.",
                "TREND(GDP, 4, 0) → fractional growth rate of GDP over 4 periods",
                List.of("FORECAST", "SMOOTH"));

        add(m, "FORECAST", "FORECAST(input, avg_time, horizon, initial_trend)",
                "Linear extrapolation forecast",
                "SD",
                List.of(p("input", "the value to forecast"),
                        p("avg_time", "averaging time for trend estimation"),
                        p("horizon", "how far ahead to forecast"),
                        p("initial_trend", "initial fractional growth rate")),
                "Forecasts the input value 'horizon' time units into the future by "
                        + "estimating the current trend and extrapolating linearly.",
                "FORECAST(Sales, 4, 8, 0) → predicted Sales 8 periods ahead",
                List.of("TREND", "SMOOTH"));

        add(m, "NPV", "NPV(stream, discount_rate [, factor] | [, initial, factor])",
                "Net present value",
                "SD",
                List.of(p("stream", "cash flow per time step"),
                        p("discount_rate", "discount rate per time unit"),
                        p("initial", "(optional, 4-arg form) initial accumulated value; defaults to 0"),
                        p("factor", "(optional) initial accumulation factor; defaults to 1")),
                "Accumulates the discounted present value of a stream of payments. "
                        + "Each step, the stream value is discounted back to the initial time. "
                        + "With 3 arguments, the third is the accumulation factor. "
                        + "With 4 arguments, the third is an initial value and the fourth is the factor.",
                "NPV(Revenue, 0.1) → cumulative present value of Revenue at 10% rate\n"
                        + "NPV(Revenue, 0.1, 2) → same with factor=2\n"
                        + "NPV(Revenue, 0.1, 500, 1) → starting from initial=500",
                List.of("TREND", "FORECAST"));

        add(m, "LOOKUP", "LOOKUP(table_name, input)", "Table lookup with interpolation",
                "SD",
                List.of(p("table_name", "name of a lookup table defined in the model"),
                        p("input", "the x-value to look up")),
                "Interpolates the value from a lookup table at the given input. "
                        + "If the input is between defined points, linear interpolation is used. "
                        + "Values outside the table range return the nearest endpoint value.",
                "LOOKUP(Effect_of_Density, Population / Area)",
                List.of());

        add(m, "RANDOM_NORMAL", "RANDOM_NORMAL(min, max, mean, std_dev [, seed])",
                "Bounded normal random number",
                "SD",
                List.of(p("min", "minimum allowed value"),
                        p("max", "maximum allowed value"),
                        p("mean", "mean of the normal distribution"),
                        p("std_dev", "standard deviation"),
                        p("seed", "(optional) random seed (currently ignored; uses system time)")),
                "Generates a random number from a normal distribution with the given mean "
                        + "and standard deviation, clamped to [min, max].",
                "RANDOM_NORMAL(0, 100, 50, 10) → ~50 with std dev 10, clamped to 0-100",
                List.of("RANDOM_UNIFORM"));

        add(m, "RANDOM_UNIFORM", "RANDOM_UNIFORM(min, max, seed)", "Uniform random number",
                "SD",
                List.of(p("min", "minimum value"),
                        p("max", "maximum value"),
                        p("seed", "random seed (currently ignored; uses system time)")),
                "Generates a uniformly distributed random number between min and max.",
                "RANDOM_UNIFORM(0, 1, 0) → uniform random in [0, 1]",
                List.of("RANDOM_NORMAL"));

        // ── Advanced SD functions ──────────────────────────────────────
        add(m, "SAMPLE_IF_TRUE", "SAMPLE_IF_TRUE(condition, input, initial)",
                "Sample input when condition is true",
                "SD",
                List.of(p("condition", "expression that evaluates to true (non-zero) or false (zero)"),
                        p("input", "value to sample when condition is true"),
                        p("initial", "initial output value before the first true condition")),
                "Returns the most recent value of 'input' at a time when 'condition' was true. "
                        + "Before the condition first becomes true, returns 'initial'. Once the "
                        + "condition becomes true, the output latches to the current input and holds "
                        + "that value until the condition is true again.",
                "SAMPLE_IF_TRUE(Month = 1, Revenue, 0) → Revenue sampled each January",
                List.of("IF", "SMOOTH"));

        add(m, "FIND_ZERO", "FIND_ZERO(expression, variable, low, high)",
                "Find zero of an expression by bisection",
                "SD",
                List.of(p("expression", "expression that depends on the variable"),
                        p("variable", "name of the loop variable to solve for"),
                        p("low", "lower bound of the search interval"),
                        p("high", "upper bound of the search interval")),
                "Finds the value of 'variable' in the interval [low, high] that makes "
                        + "'expression' equal to zero, using bisection. The variable is temporarily "
                        + "bound to trial values during the search; it does not need to be a model element.",
                "FIND_ZERO(Supply - Demand, Price, 0, 100) → equilibrium price",
                List.of("XIDZ", "ZIDZ"));

        add(m, "LOOKUP_AREA", "LOOKUP_AREA(table_name, low, high)",
                "Area under a lookup table curve",
                "SD",
                List.of(p("table_name", "name of a lookup table defined in the model"),
                        p("low", "lower x-bound of integration"),
                        p("high", "upper x-bound of integration")),
                "Computes the area under the lookup table's curve between 'low' and 'high' "
                        + "using trapezoidal integration. Useful for computing cumulative effects "
                        + "over a range of input values.",
                "LOOKUP_AREA(Effect_Curve, 0, 10) → area under Effect_Curve from x=0 to x=10",
                List.of("LOOKUP"));

        // ── Conditional ─────────────────────────────────────────────────
        add(m, "IF", "IF(condition, a, b)", "Conditional expression",
                "Special",
                List.of(p("condition", "expression that evaluates to true (non-zero) or false (zero)"),
                        p("a", "value returned when condition is true"),
                        p("b", "value returned when condition is false")),
                "Evaluates the condition and returns 'a' if true (non-zero), 'b' if false (zero). "
                        + "Comparisons return 1 for true and 0 for false.",
                "IF(Population > 1000, Growth_Rate, 0)",
                List.of("IF_SHORT"));

        add(m, "IF_SHORT", "IF_SHORT(condition, a, b)", "Short-circuit conditional expression",
                "Special",
                List.of(p("condition", "expression that evaluates to true (non-zero) or false (zero)"),
                        p("a", "value returned when condition is true"),
                        p("b", "value returned when condition is false")),
                "Like IF, but only evaluates the taken branch (short-circuit). "
                        + "Use IF_SHORT when branches may cause errors (e.g. division by zero) "
                        + "that should be avoided. Caution: stateful functions (SMOOTH, DELAY) "
                        + "in the untaken branch will not be updated, causing stale values when "
                        + "the condition flips.",
                "IF_SHORT(x > 0, y / x, 0)",
                List.of("IF"));

        DOCS = Collections.unmodifiableMap(m);
    }

    private FunctionDocRegistry() { }

    /**
     * Returns the documentation for the named function, if it exists.
     */
    public static Optional<FunctionDoc> get(String functionName) {
        return Optional.ofNullable(DOCS.get(functionName.toUpperCase()));
    }

    /**
     * Returns all documented functions in registration order.
     */
    public static List<FunctionDoc> all() {
        return List.copyOf(DOCS.values());
    }

    /**
     * Returns all function names in registration order.
     */
    public static List<String> allNames() {
        return List.copyOf(DOCS.keySet());
    }

    /**
     * Returns all functions in the given category (e.g. "SD", "Math", "Special").
     */
    public static List<FunctionDoc> byCategory(String category) {
        return DOCS.values().stream()
                .filter(d -> d.category().equals(category))
                .toList();
    }

    private static FunctionDoc.ParamDoc p(String name, String description) {
        return new FunctionDoc.ParamDoc(name, description);
    }

    private static void add(Map<String, FunctionDoc> map,
                            String name, String signature, String oneLiner,
                            String category,
                            List<FunctionDoc.ParamDoc> params,
                            String behavior, String example,
                            List<String> related) {
        map.put(name, new FunctionDoc(name, signature, oneLiner, category,
                params, behavior, example, related));
    }
}
