package systems.courant.shrewd.model.compile;

import systems.courant.shrewd.model.Delay1;
import systems.courant.shrewd.model.Delay3;
import systems.courant.shrewd.model.DelayFixed;
import systems.courant.shrewd.model.Forecast;
import systems.courant.shrewd.model.Formula;
import systems.courant.shrewd.model.LookupTable;
import systems.courant.shrewd.model.Npv;
import systems.courant.shrewd.model.Pulse;
import systems.courant.shrewd.model.Ramp;
import systems.courant.shrewd.model.Smooth;
import systems.courant.shrewd.model.Smooth3;
import systems.courant.shrewd.model.Step;
import systems.courant.shrewd.model.Trend;
import systems.courant.shrewd.model.expr.BinaryOperator;
import systems.courant.shrewd.model.expr.Expr;
import systems.courant.shrewd.model.expr.ExprParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleSupplier;

/**
 * Compiles an {@link Expr} AST into executable {@link Formula} lambdas using a
 * {@link CompilationContext} for name resolution.
 */
public class ExprCompiler {

    private static final Logger logger = LoggerFactory.getLogger(ExprCompiler.class);

    /** Counter mixed with nanoTime to ensure unique RANDOM seeds across compilations. */
    private static final AtomicLong SEED_COUNTER = new AtomicLong();

    private final CompilationContext context;
    private final List<Resettable> resettables;

    /**
     * Creates an expression compiler that resolves names against the given context
     * and registers stateful formulas in the resettables list.
     *
     * @param context     the compilation context for name resolution
     * @param resettables the list where stateful formulas (e.g., SMOOTH, DELAY3) are registered
     */
    public ExprCompiler(CompilationContext context, List<Resettable> resettables) {
        this.context = context;
        this.resettables = resettables;
    }

    /**
     * Compiles an expression string into a Formula.
     *
     * @param equation the expression string to parse and compile
     * @return a Formula that evaluates the expression at runtime
     * @throws CompilationException if the expression references unknown names or functions
     * @throws ParseException if the expression string is syntactically invalid
     */
    public Formula compile(String equation) {
        Expr expr = ExprParser.parse(equation);
        DoubleSupplier supplier = compileExpr(expr);
        return supplier::getAsDouble;
    }

    /**
     * Compiles an Expr AST node into a DoubleSupplier that evaluates it at runtime.
     *
     * @param expr the expression AST node to compile
     * @return a supplier that evaluates the expression each time it is called
     * @throws CompilationException if the expression references unknown names or functions
     */
    public DoubleSupplier compileExpr(Expr expr) {
        if (expr instanceof Expr.Literal lit) {
            double v = lit.value();
            return () -> v;
        } else if (expr instanceof Expr.Ref ref) {
            String name = ref.name();
            return () -> context.resolveValue(name);
        } else if (expr instanceof Expr.BinaryOp bin) {
            return compileBinaryOp(bin);
        } else if (expr instanceof Expr.UnaryOp un) {
            return compileUnaryOp(un);
        } else if (expr instanceof Expr.FunctionCall call) {
            return compileFunctionCall(call);
        } else if (expr instanceof Expr.Conditional cond) {
            return compileConditional(cond);
        }
        throw new CompilationException("Unknown expression type: " + expr.getClass(), "");
    }

    private DoubleSupplier compileBinaryOp(Expr.BinaryOp bin) {
        DoubleSupplier left = compileExpr(bin.left());
        DoubleSupplier right = compileExpr(bin.right());
        BinaryOperator op = bin.operator();
        return switch (op) {
            case ADD -> () -> left.getAsDouble() + right.getAsDouble();
            case SUB -> () -> left.getAsDouble() - right.getAsDouble();
            case MUL -> () -> left.getAsDouble() * right.getAsDouble();
            case DIV -> {
                boolean[] warned = {false};
                yield () -> {
                    double divisor = right.getAsDouble();
                    if (divisor == 0) {
                        if (!warned[0]) {
                            logger.warn("Division by zero");
                            warned[0] = true;
                        }
                        return Double.NaN;
                    }
                    return left.getAsDouble() / divisor;
                };
            }
            case MOD -> {
                boolean[] warned = {false};
                yield () -> {
                    double divisor = right.getAsDouble();
                    if (divisor == 0) {
                        if (!warned[0]) {
                            logger.warn("Modulo by zero");
                            warned[0] = true;
                        }
                        return Double.NaN;
                    }
                    return left.getAsDouble() % divisor;
                };
            }
            case POW -> {
                boolean[] warned = {false};
                yield () -> {
                    double result = Math.pow(left.getAsDouble(), right.getAsDouble());
                    if (Double.isNaN(result) || Double.isInfinite(result)) {
                        if (!warned[0]) {
                            logger.warn("Power produced non-finite result");
                            warned[0] = true;
                        }
                        return Double.NaN;
                    }
                    return result;
                };
            }
            case EQ -> () -> Math.abs(left.getAsDouble() - right.getAsDouble()) < 1e-10 ? 1.0 : 0.0;
            case NE -> () -> Math.abs(left.getAsDouble() - right.getAsDouble()) >= 1e-10 ? 1.0 : 0.0;
            case LT -> () -> left.getAsDouble() < right.getAsDouble() ? 1.0 : 0.0;
            case LE -> () -> left.getAsDouble() <= right.getAsDouble() ? 1.0 : 0.0;
            case GT -> () -> left.getAsDouble() > right.getAsDouble() ? 1.0 : 0.0;
            case GE -> () -> left.getAsDouble() >= right.getAsDouble() ? 1.0 : 0.0;
            case AND -> () -> (left.getAsDouble() != 0 && right.getAsDouble() != 0) ? 1.0 : 0.0;
            case OR -> () -> (left.getAsDouble() != 0 || right.getAsDouble() != 0) ? 1.0 : 0.0;
        };
    }

    private DoubleSupplier compileUnaryOp(Expr.UnaryOp un) {
        DoubleSupplier operand = compileExpr(un.operand());
        return switch (un.operator()) {
            case NEGATE -> () -> -operand.getAsDouble();
            case NOT -> () -> operand.getAsDouble() == 0 ? 1.0 : 0.0;
        };
    }

    private DoubleSupplier compileFunctionCall(Expr.FunctionCall call) {
        String name = call.name().toUpperCase();
        List<Expr> args = call.arguments();

        return switch (name) {
            case "TIME" -> {
                requireArgs(name, args, 0);
                yield () -> context.getCurrentStep().getAsInt();
            }
            case "DT" -> {
                requireArgs(name, args, 0);
                yield () -> context.getDt();
            }
            case "ABS" -> {
                requireArgs(name, args, 1);
                DoubleSupplier a = compileExpr(args.get(0));
                yield () -> Math.abs(a.getAsDouble());
            }
            case "SQRT" -> {
                requireArgs(name, args, 1);
                DoubleSupplier a = compileExpr(args.get(0));
                boolean[] warned = {false};
                yield () -> {
                    double v = a.getAsDouble();
                    if (v < 0) {
                        if (!warned[0]) {
                            logger.warn("SQRT of negative value: {}", v);
                            warned[0] = true;
                        }
                        return Double.NaN;
                    }
                    return Math.sqrt(v);
                };
            }
            case "LN" -> {
                requireArgs(name, args, 1);
                DoubleSupplier a = compileExpr(args.get(0));
                boolean[] warned = {false};
                yield () -> {
                    double v = a.getAsDouble();
                    if (v <= 0) {
                        if (!warned[0]) {
                            logger.warn("LN of non-positive value: {}", v);
                            warned[0] = true;
                        }
                        return Double.NaN;
                    }
                    return Math.log(v);
                };
            }
            case "EXP" -> {
                requireArgs(name, args, 1);
                DoubleSupplier a = compileExpr(args.get(0));
                boolean[] warned = {false};
                yield () -> {
                    double result = Math.exp(a.getAsDouble());
                    if (Double.isInfinite(result)) {
                        if (!warned[0]) {
                            logger.warn("EXP overflow");
                            warned[0] = true;
                        }
                        return Double.NaN;
                    }
                    return result;
                };
            }
            case "LOG" -> {
                if (args.size() == 2) {
                    DoubleSupplier a = compileExpr(args.get(0));
                    DoubleSupplier base = compileExpr(args.get(1));
                    boolean[] warned = {false};
                    yield () -> {
                        double v = a.getAsDouble();
                        double b = base.getAsDouble();
                        if (v <= 0 || b <= 0 || b == 1) {
                            if (!warned[0]) {
                                logger.warn("LOG with invalid arguments: value={}, base={}", v, b);
                                warned[0] = true;
                            }
                            return Double.NaN;
                        }
                        return Math.log(v) / Math.log(b);
                    };
                }
                requireArgs(name, args, 1);
                DoubleSupplier a = compileExpr(args.get(0));
                boolean[] warned = {false};
                yield () -> {
                    double v = a.getAsDouble();
                    if (v <= 0) {
                        if (!warned[0]) {
                            logger.warn("LOG of non-positive value: {}", v);
                            warned[0] = true;
                        }
                        return Double.NaN;
                    }
                    return Math.log10(v);
                };
            }
            case "SIN" -> {
                requireArgs(name, args, 1);
                DoubleSupplier a = compileExpr(args.get(0));
                yield () -> Math.sin(a.getAsDouble());
            }
            case "COS" -> {
                requireArgs(name, args, 1);
                DoubleSupplier a = compileExpr(args.get(0));
                yield () -> Math.cos(a.getAsDouble());
            }
            case "TAN" -> {
                requireArgs(name, args, 1);
                DoubleSupplier a = compileExpr(args.get(0));
                yield () -> Math.tan(a.getAsDouble());
            }
            case "ARCSIN" -> {
                requireArgs(name, args, 1);
                DoubleSupplier a = compileExpr(args.get(0));
                boolean[] warned = {false};
                yield () -> {
                    double v = a.getAsDouble();
                    if (v < -1 || v > 1) {
                        if (!warned[0]) {
                            logger.warn("ARCSIN of value outside [-1, 1]: {}", v);
                            warned[0] = true;
                        }
                        return Double.NaN;
                    }
                    return Math.asin(v);
                };
            }
            case "ARCCOS" -> {
                requireArgs(name, args, 1);
                DoubleSupplier a = compileExpr(args.get(0));
                boolean[] warned = {false};
                yield () -> {
                    double v = a.getAsDouble();
                    if (v < -1 || v > 1) {
                        if (!warned[0]) {
                            logger.warn("ARCCOS of value outside [-1, 1]: {}", v);
                            warned[0] = true;
                        }
                        return Double.NaN;
                    }
                    return Math.acos(v);
                };
            }
            case "ARCTAN" -> {
                requireArgs(name, args, 1);
                DoubleSupplier a = compileExpr(args.get(0));
                yield () -> Math.atan(a.getAsDouble());
            }
            case "SIGN" -> {
                requireArgs(name, args, 1);
                DoubleSupplier a = compileExpr(args.get(0));
                yield () -> Math.signum(a.getAsDouble());
            }
            case "PI" -> {
                requireArgs(name, args, 0);
                yield () -> Math.PI;
            }
            case "INT" -> {
                requireArgs(name, args, 1);
                DoubleSupplier a = compileExpr(args.get(0));
                yield () -> {
                    double v = a.getAsDouble();
                    return v >= 0 ? Math.floor(v) : Math.ceil(v);
                };
            }
            case "ROUND" -> {
                requireArgs(name, args, 1);
                DoubleSupplier a = compileExpr(args.get(0));
                yield () -> Math.round(a.getAsDouble());
            }
            case "MODULO" -> {
                requireArgs(name, args, 2);
                DoubleSupplier a = compileExpr(args.get(0));
                DoubleSupplier b = compileExpr(args.get(1));
                boolean[] warned = {false};
                yield () -> {
                    double divisor = b.getAsDouble();
                    if (divisor == 0) {
                        if (!warned[0]) {
                            logger.warn("MODULO by zero");
                            warned[0] = true;
                        }
                        return Double.NaN;
                    }
                    return a.getAsDouble() % divisor;
                };
            }
            case "QUANTUM" -> {
                requireArgs(name, args, 2);
                DoubleSupplier a = compileExpr(args.get(0));
                DoubleSupplier b = compileExpr(args.get(1));
                boolean[] warned = {false};
                yield () -> {
                    double quantum = b.getAsDouble();
                    if (quantum == 0) {
                        if (!warned[0]) {
                            logger.warn("QUANTUM with zero quantum size");
                            warned[0] = true;
                        }
                        return a.getAsDouble();
                    }
                    return Math.floor(a.getAsDouble() / quantum) * quantum;
                };
            }
            case "POWER" -> {
                requireArgs(name, args, 2);
                DoubleSupplier a = compileExpr(args.get(0));
                DoubleSupplier b = compileExpr(args.get(1));
                boolean[] warned = {false};
                yield () -> {
                    double result = Math.pow(a.getAsDouble(), b.getAsDouble());
                    if (Double.isNaN(result) || Double.isInfinite(result)) {
                        if (!warned[0]) {
                            logger.warn("POWER produced non-finite result");
                            warned[0] = true;
                        }
                        return Double.NaN;
                    }
                    return result;
                };
            }
            case "MIN" -> {
                requireArgs(name, args, 2);
                DoubleSupplier a = compileExpr(args.get(0));
                DoubleSupplier b = compileExpr(args.get(1));
                yield () -> Math.min(a.getAsDouble(), b.getAsDouble());
            }
            case "MAX" -> {
                requireArgs(name, args, 2);
                DoubleSupplier a = compileExpr(args.get(0));
                DoubleSupplier b = compileExpr(args.get(1));
                yield () -> Math.max(a.getAsDouble(), b.getAsDouble());
            }
            case "SUM" -> {
                List<DoubleSupplier> compiled = new ArrayList<>();
                for (Expr arg : args) {
                    compiled.add(compileExpr(arg));
                }
                yield () -> {
                    double sum = 0;
                    for (DoubleSupplier s : compiled) {
                        sum += s.getAsDouble();
                    }
                    return sum;
                };
            }
            case "MEAN" -> {
                if (args.isEmpty()) {
                    throw new CompilationException(
                            "MEAN requires at least 1 argument", "MEAN");
                }
                List<DoubleSupplier> compiled = new ArrayList<>();
                for (Expr arg : args) {
                    compiled.add(compileExpr(arg));
                }
                int count = compiled.size();
                yield () -> {
                    double sum = 0;
                    for (DoubleSupplier s : compiled) {
                        sum += s.getAsDouble();
                    }
                    return sum / count;
                };
            }
            case "VMIN" -> {
                if (args.isEmpty()) {
                    throw new CompilationException(
                            "VMIN requires at least 1 argument", "VMIN");
                }
                List<DoubleSupplier> compiled = new ArrayList<>();
                for (Expr arg : args) {
                    compiled.add(compileExpr(arg));
                }
                yield () -> {
                    double result = compiled.get(0).getAsDouble();
                    for (int i = 1; i < compiled.size(); i++) {
                        result = Math.min(result, compiled.get(i).getAsDouble());
                    }
                    return result;
                };
            }
            case "VMAX" -> {
                if (args.isEmpty()) {
                    throw new CompilationException(
                            "VMAX requires at least 1 argument", "VMAX");
                }
                List<DoubleSupplier> compiled = new ArrayList<>();
                for (Expr arg : args) {
                    compiled.add(compileExpr(arg));
                }
                yield () -> {
                    double result = compiled.get(0).getAsDouble();
                    for (int i = 1; i < compiled.size(); i++) {
                        result = Math.max(result, compiled.get(i).getAsDouble());
                    }
                    return result;
                };
            }
            case "PROD" -> {
                if (args.isEmpty()) {
                    throw new CompilationException(
                            "PROD requires at least 1 argument", "PROD");
                }
                List<DoubleSupplier> compiled = new ArrayList<>();
                for (Expr arg : args) {
                    compiled.add(compileExpr(arg));
                }
                yield () -> {
                    double result = 1;
                    for (DoubleSupplier s : compiled) {
                        result *= s.getAsDouble();
                    }
                    return result;
                };
            }
            case "XIDZ" -> {
                requireArgs(name, args, 3);
                DoubleSupplier a = compileExpr(args.get(0));
                DoubleSupplier b = compileExpr(args.get(1));
                DoubleSupplier x = compileExpr(args.get(2));
                yield () -> {
                    double divisor = b.getAsDouble();
                    return divisor == 0 ? x.getAsDouble() : a.getAsDouble() / divisor;
                };
            }
            case "ZIDZ" -> {
                requireArgs(name, args, 2);
                DoubleSupplier a = compileExpr(args.get(0));
                DoubleSupplier b = compileExpr(args.get(1));
                yield () -> {
                    double divisor = b.getAsDouble();
                    return divisor == 0 ? 0.0 : a.getAsDouble() / divisor;
                };
            }
            case "INITIAL" -> {
                requireArgs(name, args, 1);
                DoubleSupplier a = compileExpr(args.get(0));
                double[] cached = {Double.NaN};
                boolean[] initialized = {false};
                Resettable reset = () -> {
                    cached[0] = Double.NaN;
                    initialized[0] = false;
                };
                resettables.add(reset);
                yield () -> {
                    if (!initialized[0]) {
                        cached[0] = a.getAsDouble();
                        initialized[0] = true;
                    }
                    return cached[0];
                };
            }
            case "SMOOTH" -> compileSmooth(args);
            case "SMOOTHI" -> compileSmoothI(args);
            case "SMOOTH3" -> compileSmooth3(args);
            case "SMOOTH3I" -> compileSmooth3I(args);
            case "DELAY1", "DELAY1I" -> compileDelay1(args);
            case "DELAY3", "DELAY3I" -> compileDelay3(args);
            case "STEP" -> compileStep(args);
            case "RAMP" -> compileRamp(args);
            case "PULSE" -> compilePulse(args);
            case "PULSE_TRAIN" -> compilePulseTrain(args);
            case "DELAY_FIXED" -> compileDelayFixed(args);
            case "TREND" -> compileTrend(args);
            case "FORECAST" -> compileForecast(args);
            case "NPV" -> compileNpv(args);
            case "RANDOM_NORMAL" -> compileRandomNormal(args);
            case "RANDOM_UNIFORM" -> compileRandomUniform(args);
            case "LOOKUP" -> compileLookup(args);
            default -> {
                // Check if the function name is a lookup table (Vensim allows table(input) syntax)
                String originalName = call.name();
                if (args.size() == 1 && context.resolveLookupTable(originalName).isPresent()) {
                    List<Expr> lookupArgs = List.of(new Expr.Ref(originalName), args.get(0));
                    yield compileLookup(lookupArgs);
                }
                throw new CompilationException(
                        "Unknown function: " + name, name);
            }
        };
    }

    private DoubleSupplier compileSmooth(List<Expr> args) {
        if (args.size() < 2 || args.size() > 3) {
            throw new CompilationException(
                    "SMOOTH requires 2-3 arguments, got " + args.size(), "SMOOTH");
        }
        DoubleSupplier input = compileExpr(args.get(0));
        double smoothingTime = evaluateConstant(args.get(1), "SMOOTH smoothingTime");
        Smooth smooth;
        if (args.size() == 3) {
            double initial = evaluateConstant(args.get(2), "SMOOTH initialValue");
            smooth = Smooth.of(input, smoothingTime, initial, context.getCurrentStep());
        } else {
            smooth = Smooth.of(input, smoothingTime, context.getCurrentStep());
        }
        resettables.add(smooth);
        return smooth::getCurrentValue;
    }

    private DoubleSupplier compileSmoothI(List<Expr> args) {
        requireArgs("SMOOTHI", args, 3);
        DoubleSupplier input = compileExpr(args.get(0));
        double smoothingTime = evaluateConstant(args.get(1), "SMOOTHI smoothingTime");
        double initial = evaluateConstant(args.get(2), "SMOOTHI initialValue");
        Smooth smooth = Smooth.of(input, smoothingTime, initial, context.getCurrentStep());
        resettables.add(smooth);
        return smooth::getCurrentValue;
    }

    private DoubleSupplier compileSmooth3(List<Expr> args) {
        if (args.size() < 2 || args.size() > 3) {
            throw new CompilationException(
                    "SMOOTH3 requires 2-3 arguments, got " + args.size(), "SMOOTH3");
        }
        DoubleSupplier input = compileExpr(args.get(0));
        double smoothingTime = evaluateConstant(args.get(1), "SMOOTH3 smoothingTime");
        Smooth3 smooth3;
        if (args.size() == 3) {
            double initial = evaluateConstant(args.get(2), "SMOOTH3 initialValue");
            smooth3 = Smooth3.of(input, smoothingTime, initial, context.getCurrentStep());
        } else {
            smooth3 = Smooth3.of(input, smoothingTime, context.getCurrentStep());
        }
        resettables.add(smooth3);
        return smooth3::getCurrentValue;
    }

    private DoubleSupplier compileSmooth3I(List<Expr> args) {
        requireArgs("SMOOTH3I", args, 3);
        DoubleSupplier input = compileExpr(args.get(0));
        double smoothingTime = evaluateConstant(args.get(1), "SMOOTH3I smoothingTime");
        double initial = evaluateConstant(args.get(2), "SMOOTH3I initialValue");
        Smooth3 smooth3 = Smooth3.of(input, smoothingTime, initial, context.getCurrentStep());
        resettables.add(smooth3);
        return smooth3::getCurrentValue;
    }

    private DoubleSupplier compileDelay1(List<Expr> args) {
        if (args.size() < 2 || args.size() > 3) {
            throw new CompilationException(
                    "DELAY1 requires 2-3 arguments, got " + args.size(), "DELAY1");
        }
        DoubleSupplier input = compileExpr(args.get(0));
        double delayTime = evaluateAtCompileTime(args.get(1), "DELAY1 delayTime");
        if (delayTime <= 0 || Double.isNaN(delayTime)) {
            logger.warn("DELAY1 delayTime evaluated to {} at compile time, defaulting to 1.0",
                    delayTime);
            delayTime = 1.0;
        }
        Delay1 delay1;
        if (args.size() == 3) {
            double initial = evaluateAtCompileTime(args.get(2), "DELAY1 initialValue");
            if (Double.isNaN(initial)) {
                initial = 0.0;
            }
            delay1 = Delay1.of(input, delayTime, initial, context.getCurrentStep());
        } else {
            delay1 = Delay1.of(input, delayTime, context.getCurrentStep());
        }
        resettables.add(delay1);
        return delay1::getCurrentValue;
    }

    private DoubleSupplier compileDelay3(List<Expr> args) {
        if (args.size() < 2 || args.size() > 3) {
            throw new CompilationException(
                    "DELAY3 requires 2-3 arguments, got " + args.size(), "DELAY3");
        }
        DoubleSupplier input = compileExpr(args.get(0));
        double delayTime = evaluateAtCompileTime(args.get(1), "DELAY3 delayTime");
        if (delayTime <= 0 || Double.isNaN(delayTime)) {
            // Delay time couldn't be resolved at compile time (variable not yet initialized).
            // Use a default of 1 timestep to avoid crashing; the model may still produce
            // approximate results.
            logger.warn("DELAY3 delayTime evaluated to {} at compile time, defaulting to 1.0",
                    delayTime);
            delayTime = 1.0;
        }
        Delay3 delay3;
        if (args.size() == 3) {
            double initial = evaluateAtCompileTime(args.get(2), "DELAY3 initialValue");
            if (Double.isNaN(initial)) {
                initial = 0.0;
            }
            delay3 = Delay3.of(input, delayTime, initial, context.getCurrentStep());
        } else {
            delay3 = Delay3.of(input, delayTime, context.getCurrentStep());
        }
        resettables.add(delay3);
        return delay3::getCurrentValue;
    }

    private DoubleSupplier compileStep(List<Expr> args) {
        requireArgs("STEP", args, 2);
        double height = evaluateConstant(args.get(0), "STEP height");
        double time = evaluateConstant(args.get(1), "STEP time");
        Step step = Step.of(height, (int) Math.round(time), context.getCurrentStep());
        return step::getCurrentValue;
    }

    private DoubleSupplier compileRamp(List<Expr> args) {
        if (args.size() < 2 || args.size() > 3) {
            throw new CompilationException(
                    "RAMP requires 2-3 arguments, got " + args.size(), "RAMP");
        }
        double slope = evaluateConstant(args.get(0), "RAMP slope");
        double start = evaluateConstant(args.get(1), "RAMP startStep");
        Ramp ramp;
        if (args.size() == 3) {
            double end = evaluateConstant(args.get(2), "RAMP endStep");
            ramp = Ramp.of(slope, (int) Math.round(start), (int) Math.round(end),
                    context.getCurrentStep());
        } else {
            ramp = Ramp.of(slope, (int) Math.round(start), context.getCurrentStep());
        }
        return ramp::getCurrentValue;
    }

    private DoubleSupplier compilePulse(List<Expr> args) {
        if (args.size() < 2 || args.size() > 3) {
            throw new CompilationException(
                    "PULSE requires 2-3 arguments, got " + args.size(), "PULSE");
        }
        double magnitude = evaluateConstant(args.get(0), "PULSE magnitude");
        double start = evaluateConstant(args.get(1), "PULSE startTime");
        Pulse pulse;
        if (args.size() == 3) {
            double interval = evaluateConstant(args.get(2), "PULSE interval");
            pulse = Pulse.of(magnitude, (int) Math.round(start),
                    (int) Math.round(interval), context.getCurrentStep());
        } else {
            pulse = Pulse.of(magnitude, (int) Math.round(start), context.getCurrentStep());
        }
        return pulse::getCurrentValue;
    }

    private DoubleSupplier compilePulseTrain(List<Expr> args) {
        requireArgs("PULSE_TRAIN", args, 4);
        DoubleSupplier startTime = compileExpr(args.get(0));
        DoubleSupplier duration = compileExpr(args.get(1));
        DoubleSupplier repeatInterval = compileExpr(args.get(2));
        DoubleSupplier endTime = compileExpr(args.get(3));
        return () -> {
            double t = context.getCurrentStep().getAsInt();
            double start = startTime.getAsDouble();
            double end = endTime.getAsDouble();
            double dur = duration.getAsDouble();
            double repeat = repeatInterval.getAsDouble();
            if (t < start || t > end) {
                return 0.0;
            }
            double elapsed = t - start;
            if (repeat > 0) {
                double phase = elapsed % repeat;
                return phase < dur ? 1.0 : 0.0;
            }
            return elapsed < dur ? 1.0 : 0.0;
        };
    }

    private DoubleSupplier compileDelayFixed(List<Expr> args) {
        requireArgs("DELAY_FIXED", args, 3);
        DoubleSupplier input = compileExpr(args.get(0));
        double delayTime = evaluateConstant(args.get(1), "DELAY_FIXED delayTime");
        int delaySteps = (int) Math.round(delayTime);
        if (delaySteps <= 0 || Double.isNaN(delayTime)) {
            logger.warn("DELAY_FIXED delayTime evaluated to {} (rounded to {}) at compile time, "
                    + "defaulting to 1 step", delayTime, delaySteps);
            delaySteps = 1;
        }
        DoubleSupplier initial = compileExpr(args.get(2));
        DelayFixed delayFixed = DelayFixed.of(input, delaySteps,
                initial, context.getCurrentStep());
        resettables.add(delayFixed);
        return delayFixed::getCurrentValue;
    }

    private DoubleSupplier compileTrend(List<Expr> args) {
        requireArgs("TREND", args, 3);
        DoubleSupplier input = compileExpr(args.get(0));
        double averagingTime = evaluateConstant(args.get(1), "TREND averagingTime");
        double initialTrend = evaluateConstant(args.get(2), "TREND initialTrend");
        Trend trend = Trend.of(input, averagingTime, initialTrend, context.getCurrentStep());
        resettables.add(trend);
        return trend::getCurrentValue;
    }

    private DoubleSupplier compileForecast(List<Expr> args) {
        requireArgs("FORECAST", args, 4);
        DoubleSupplier input = compileExpr(args.get(0));
        double averagingTime = evaluateConstant(args.get(1), "FORECAST averagingTime");
        double horizon = evaluateConstant(args.get(2), "FORECAST horizon");
        double initialTrend = evaluateConstant(args.get(3), "FORECAST initialTrend");
        Forecast forecast = Forecast.of(input, averagingTime, horizon, initialTrend,
                context.getCurrentStep());
        resettables.add(forecast);
        return forecast::getCurrentValue;
    }

    private DoubleSupplier compileNpv(List<Expr> args) {
        if (args.size() < 2 || args.size() > 3) {
            throw new CompilationException(
                    "NPV requires 2-3 arguments, got " + args.size(), "NPV");
        }
        DoubleSupplier stream = compileExpr(args.get(0));
        double discountRate = evaluateConstant(args.get(1), "NPV discountRate");
        Npv npv;
        if (args.size() == 3) {
            double factor = evaluateConstant(args.get(2), "NPV factor");
            npv = Npv.of(stream, discountRate, factor, context.getCurrentStep());
        } else {
            npv = Npv.of(stream, discountRate, context.getCurrentStep());
        }
        resettables.add(npv);
        return npv::getCurrentValue;
    }

    private DoubleSupplier compileRandomNormal(List<Expr> args) {
        if (args.size() < 4 || args.size() > 5) {
            throw new CompilationException(
                    "RANDOM_NORMAL requires 4-5 arguments, got " + args.size(),
                    "RANDOM_NORMAL");
        }
        DoubleSupplier minVal = compileExpr(args.get(0));
        DoubleSupplier maxVal = compileExpr(args.get(1));
        DoubleSupplier mean = compileExpr(args.get(2));
        DoubleSupplier stddev = compileExpr(args.get(3));
        long seed = System.nanoTime() ^ SEED_COUNTER.incrementAndGet();
        java.util.Random rng = new java.util.Random(seed);
        resettables.add(() -> rng.setSeed(seed));
        return () -> {
            double raw = mean.getAsDouble() + stddev.getAsDouble() * rng.nextGaussian();
            return Math.max(minVal.getAsDouble(), Math.min(maxVal.getAsDouble(), raw));
        };
    }

    private DoubleSupplier compileRandomUniform(List<Expr> args) {
        requireArgs("RANDOM_UNIFORM", args, 3);
        DoubleSupplier minVal = compileExpr(args.get(0));
        DoubleSupplier maxVal = compileExpr(args.get(1));
        // Third arg is seed — Vensim uses it for reproducibility, we mix nanoTime with counter
        long seed = System.nanoTime() ^ SEED_COUNTER.incrementAndGet();
        java.util.Random rng = new java.util.Random(seed);
        resettables.add(() -> rng.setSeed(seed));
        return () -> {
            double lo = minVal.getAsDouble();
            double hi = maxVal.getAsDouble();
            return lo + (hi - lo) * rng.nextDouble();
        };
    }

    private DoubleSupplier compileLookup(List<Expr> args) {
        requireArgs("LOOKUP", args, 2);
        // First arg must be a Ref to a lookup table name
        if (!(args.get(0) instanceof Expr.Ref ref)) {
            throw new CompilationException(
                    "LOOKUP first argument must be a table name reference", "LOOKUP");
        }
        String tableName = ref.name();
        String resolvedName = tableName;
        // Verify table exists
        Optional<LookupTable> existing = context.resolveLookupTable(tableName);
        if (existing.isEmpty() && tableName.contains("_")) {
            resolvedName = tableName.replace('_', ' ');
            existing = context.resolveLookupTable(resolvedName);
        }
        if (existing.isEmpty()) {
            throw new CompilationException(
                    "Lookup table not found: " + tableName, tableName);
        }
        // Create a fresh LookupTable for this reference with its own isolated input,
        // preventing cross-formula interference when multiple formulas use the same table
        DoubleSupplier input = compileExpr(args.get(1));
        Optional<LookupTable> freshTable = context.createFreshLookupTable(resolvedName, input);
        if (freshTable.isPresent()) {
            return freshTable.get()::getCurrentValue;
        }
        // Fallback: create a per-reference copy to prevent cross-formula interference
        // when multiple formulas reference the same lookup table (tables registered without a def)
        LookupTable isolatedTable = existing.get().withInput(input);
        return isolatedTable::getCurrentValue;
    }

    private DoubleSupplier compileConditional(Expr.Conditional cond) {
        DoubleSupplier condition = compileExpr(cond.condition());
        DoubleSupplier thenExpr = compileExpr(cond.thenExpr());
        DoubleSupplier elseExpr = compileExpr(cond.elseExpr());
        // Evaluate both branches every step so stateful SD functions (SMOOTH, DELAY3,
        // TREND, etc.) in the untaken branch keep their internal state current.
        return () -> {
            double thenVal = thenExpr.getAsDouble();
            double elseVal = elseExpr.getAsDouble();
            return condition.getAsDouble() != 0 ? thenVal : elseVal;
        };
    }

    /**
     * Compiles an expression and evaluates it immediately. Used for parameters
     * like delay times that must be known at compile time but may reference
     * variables or auxiliaries (evaluated at their initial/current values).
     */
    private double evaluateAtCompileTime(Expr expr, String paramDescription) {
        try {
            return evaluateConstant(expr, paramDescription);
        } catch (CompilationException e) {
            // Fall back to compiling and evaluating the full expression
            DoubleSupplier compiled = compileExpr(expr);
            return compiled.getAsDouble();
        }
    }

    /**
     * Evaluates a constant expression at compile time. Supports literals and
     * references to constants.
     */
    private double evaluateConstant(Expr expr, String paramDescription) {
        if (expr instanceof Expr.Literal lit) {
            return lit.value();
        }
        if (expr instanceof Expr.Ref ref) {
            OptionalDouble val = context.resolveConstant(ref.name());
            if (val.isPresent()) {
                return val.getAsDouble();
            }
            throw new CompilationException(
                    paramDescription + ": reference '" + ref.name()
                            + "' must be a constant", ref.name());
        }
        if (expr instanceof Expr.UnaryOp un
                && un.operator() == systems.courant.shrewd.model.expr.UnaryOperator.NEGATE) {
            return -evaluateConstant(un.operand(), paramDescription);
        }
        if (expr instanceof Expr.BinaryOp bin) {
            double left = evaluateConstant(bin.left(), paramDescription);
            double right = evaluateConstant(bin.right(), paramDescription);
            return switch (bin.operator()) {
                case ADD -> left + right;
                case SUB -> left - right;
                case MUL -> left * right;
                case DIV -> right == 0 ? Double.NaN : left / right;
                case POW -> Math.pow(left, right);
                case MOD -> right == 0 ? Double.NaN : left % right;
                case EQ -> left == right ? 1.0 : 0.0;
                case NE -> left != right ? 1.0 : 0.0;
                case LT -> left < right ? 1.0 : 0.0;
                case GT -> left > right ? 1.0 : 0.0;
                case LE -> left <= right ? 1.0 : 0.0;
                case GE -> left >= right ? 1.0 : 0.0;
                case AND -> (left != 0 && right != 0) ? 1.0 : 0.0;
                case OR -> (left != 0 || right != 0) ? 1.0 : 0.0;
            };
        }
        throw new CompilationException(
                paramDescription + ": must be a constant expression", "");
    }

    private void requireArgs(String funcName, List<Expr> args, int expected) {
        if (args.size() != expected) {
            throw new CompilationException(
                    funcName + " requires " + expected + " arguments, got " + args.size(),
                    funcName);
        }
    }
}
