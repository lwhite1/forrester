package com.deathrayresearch.forrester.model.compile;

import com.deathrayresearch.forrester.model.Delay3;
import com.deathrayresearch.forrester.model.DelayFixed;
import com.deathrayresearch.forrester.model.Forecast;
import com.deathrayresearch.forrester.model.Formula;
import com.deathrayresearch.forrester.model.LookupTable;
import com.deathrayresearch.forrester.model.Npv;
import com.deathrayresearch.forrester.model.Pulse;
import com.deathrayresearch.forrester.model.Ramp;
import com.deathrayresearch.forrester.model.Smooth;
import com.deathrayresearch.forrester.model.Step;
import com.deathrayresearch.forrester.model.Trend;
import com.deathrayresearch.forrester.model.expr.BinaryOperator;
import com.deathrayresearch.forrester.model.expr.Expr;
import com.deathrayresearch.forrester.model.expr.ExprParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.DoubleSupplier;

/**
 * Compiles an {@link Expr} AST into executable {@link Formula} lambdas using a
 * {@link CompilationContext} for name resolution.
 */
public class ExprCompiler {

    private static final Logger logger = LoggerFactory.getLogger(ExprCompiler.class);

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
            case DIV -> () -> {
                double divisor = right.getAsDouble();
                if (divisor == 0) {
                    logger.warn("Division by zero");
                    return Double.NaN;
                }
                return left.getAsDouble() / divisor;
            };
            case MOD -> () -> {
                double divisor = right.getAsDouble();
                if (divisor == 0) {
                    logger.warn("Modulo by zero");
                    return Double.NaN;
                }
                return left.getAsDouble() % divisor;
            };
            case POW -> () -> {
                double result = Math.pow(left.getAsDouble(), right.getAsDouble());
                if (Double.isNaN(result) || Double.isInfinite(result)) {
                    logger.warn("Power produced non-finite result");
                    return Double.NaN;
                }
                return result;
            };
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
        String name = call.name();
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
                yield () -> {
                    double v = a.getAsDouble();
                    if (v < 0) {
                        logger.warn("SQRT of negative value: {}", v);
                        return Double.NaN;
                    }
                    return Math.sqrt(v);
                };
            }
            case "LN" -> {
                requireArgs(name, args, 1);
                DoubleSupplier a = compileExpr(args.get(0));
                yield () -> {
                    double v = a.getAsDouble();
                    if (v <= 0) {
                        logger.warn("LN of non-positive value: {}", v);
                        return Double.NaN;
                    }
                    return Math.log(v);
                };
            }
            case "EXP" -> {
                requireArgs(name, args, 1);
                DoubleSupplier a = compileExpr(args.get(0));
                yield () -> {
                    double result = Math.exp(a.getAsDouble());
                    if (Double.isInfinite(result)) {
                        logger.warn("EXP overflow");
                        return Double.NaN;
                    }
                    return result;
                };
            }
            case "LOG" -> {
                requireArgs(name, args, 1);
                DoubleSupplier a = compileExpr(args.get(0));
                yield () -> {
                    double v = a.getAsDouble();
                    if (v <= 0) {
                        logger.warn("LOG of non-positive value: {}", v);
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
            case "INT" -> {
                requireArgs(name, args, 1);
                DoubleSupplier a = compileExpr(args.get(0));
                yield () -> (double) (long) a.getAsDouble();
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
                yield () -> {
                    double divisor = b.getAsDouble();
                    if (divisor == 0) {
                        logger.warn("MODULO by zero");
                        return Double.NaN;
                    }
                    return a.getAsDouble() % divisor;
                };
            }
            case "POWER" -> {
                requireArgs(name, args, 2);
                DoubleSupplier a = compileExpr(args.get(0));
                DoubleSupplier b = compileExpr(args.get(1));
                yield () -> {
                    double result = Math.pow(a.getAsDouble(), b.getAsDouble());
                    if (Double.isNaN(result) || Double.isInfinite(result)) {
                        logger.warn("POWER produced non-finite result");
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
            case "SMOOTH" -> compileSmooth(args);
            case "DELAY3" -> compileDelay3(args);
            case "STEP" -> compileStep(args);
            case "RAMP" -> compileRamp(args);
            case "PULSE" -> compilePulse(args);
            case "DELAY_FIXED" -> compileDelayFixed(args);
            case "TREND" -> compileTrend(args);
            case "FORECAST" -> compileForecast(args);
            case "NPV" -> compileNpv(args);
            case "RANDOM_NORMAL" -> compileRandomNormal(args);
            case "RANDOM_UNIFORM" -> compileRandomUniform(args);
            case "LOOKUP" -> compileLookup(args);
            default -> {
                // Check if the function name is a lookup table (Vensim allows table(input) syntax)
                if (args.size() == 1 && context.resolveLookupTable(name).isPresent()) {
                    List<Expr> lookupArgs = List.of(new Expr.Ref(name), args.get(0));
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

    private DoubleSupplier compileDelay3(List<Expr> args) {
        if (args.size() < 2 || args.size() > 3) {
            throw new CompilationException(
                    "DELAY3 requires 2-3 arguments, got " + args.size(), "DELAY3");
        }
        DoubleSupplier input = compileExpr(args.get(0));
        double delayTime = evaluateConstant(args.get(1), "DELAY3 delayTime");
        Delay3 delay3;
        if (args.size() == 3) {
            double initial = evaluateConstant(args.get(2), "DELAY3 initialValue");
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

    private DoubleSupplier compileDelayFixed(List<Expr> args) {
        requireArgs("DELAY_FIXED", args, 3);
        DoubleSupplier input = compileExpr(args.get(0));
        double delayTime = evaluateConstant(args.get(1), "DELAY_FIXED delayTime");
        DoubleSupplier initial = compileExpr(args.get(2));
        DelayFixed delayFixed = DelayFixed.of(input, (int) Math.round(delayTime),
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
        requireArgs("RANDOM_NORMAL", args, 4);
        DoubleSupplier minVal = compileExpr(args.get(0));
        DoubleSupplier maxVal = compileExpr(args.get(1));
        DoubleSupplier mean = compileExpr(args.get(2));
        DoubleSupplier stddev = compileExpr(args.get(3));
        long seed = System.nanoTime();
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
        // Third arg is seed — Vensim uses it for reproducibility, we use nanoTime
        long seed = System.nanoTime();
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
        // Fallback: use shared holder (for tables registered without a def)
        double[] inputHolder = context.resolveLookupInputHolder(resolvedName)
                .orElseThrow(() -> new CompilationException(
                        "No input holder found for lookup table: " + tableName, tableName));
        LookupTable finalTable = existing.get();
        return () -> {
            inputHolder[0] = input.getAsDouble();
            return finalTable.getCurrentValue();
        };
    }

    private DoubleSupplier compileConditional(Expr.Conditional cond) {
        DoubleSupplier condition = compileExpr(cond.condition());
        DoubleSupplier thenExpr = compileExpr(cond.thenExpr());
        DoubleSupplier elseExpr = compileExpr(cond.elseExpr());
        return () -> condition.getAsDouble() != 0 ? thenExpr.getAsDouble() : elseExpr.getAsDouble();
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
        if (expr instanceof Expr.UnaryOp un && un.operator() == com.deathrayresearch.forrester.model.expr.UnaryOperator.NEGATE) {
            return -evaluateConstant(un.operand(), paramDescription);
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
