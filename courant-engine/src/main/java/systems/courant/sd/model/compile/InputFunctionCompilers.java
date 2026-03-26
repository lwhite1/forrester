package systems.courant.sd.model.compile;

import systems.courant.sd.model.expr.Expr;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

/**
 * Time-dependent input-generator functions: STEP, RAMP, PULSE, PULSE_TRAIN,
 * and random number generators (RANDOM_NORMAL, RANDOM_UNIFORM).
 */
final class InputFunctionCompilers {

    /** Counter mixed with nanoTime to ensure unique RANDOM seeds across compilations. */
    private static final AtomicLong SEED_COUNTER = new AtomicLong();

    private InputFunctionCompilers() {
    }

    static void registerAll(FunctionCompilerRegistry r) {
        r.register("STEP", InputFunctionCompilers::compileStep);
        r.register("RAMP", InputFunctionCompilers::compileRamp);
        r.register("PULSE", InputFunctionCompilers::compilePulse);
        r.register("PULSE_TRAIN", InputFunctionCompilers::compilePulseTrain);
        r.register("RANDOM_NORMAL", InputFunctionCompilers::compileRandomNormal);
        r.register("RANDOM_UNIFORM", InputFunctionCompilers::compileRandomUniform);
    }

    private static DoubleSupplier compileStep(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs("STEP", args, 2);
        double height = c.evaluateConstant(args.get(0), "STEP height");
        double time = c.evaluateConstant(args.get(1), "STEP time");
        double[] dtH = c.getContext().getDtHolder();
        LongSupplier stepSupplier = c.getContext().getCurrentStep();
        return () -> stepSupplier.getAsLong() * dtH[0] >= time ? height : 0;
    }

    private static DoubleSupplier compileRamp(String name, List<Expr> args, ExprCompiler c) {
        if (args.size() < 2 || args.size() > 3) {
            throw new CompilationException(
                    "RAMP requires 2-3 arguments, got " + args.size(), "RAMP");
        }
        double slope = c.evaluateConstant(args.get(0), "RAMP slope");
        double startTime = c.evaluateConstant(args.get(1), "RAMP startTime");
        double[] dtH = c.getContext().getDtHolder();
        LongSupplier stepSupplier = c.getContext().getCurrentStep();
        if (args.size() == 3) {
            double endTime = c.evaluateConstant(args.get(2), "RAMP endTime");
            return () -> {
                double t = stepSupplier.getAsLong() * dtH[0];
                if (t < startTime) {
                    return 0.0;
                }
                double elapsed = Math.min(t, endTime) - startTime;
                return slope * elapsed;
            };
        } else {
            return () -> {
                double t = stepSupplier.getAsLong() * dtH[0];
                if (t < startTime) {
                    return 0.0;
                }
                return slope * (t - startTime);
            };
        }
    }

    private static DoubleSupplier compilePulse(String name, List<Expr> args, ExprCompiler c) {
        if (args.size() < 2 || args.size() > 3) {
            throw new CompilationException(
                    "PULSE requires 2-3 arguments, got " + args.size(), "PULSE");
        }
        double magnitude = c.evaluateConstant(args.get(0), "PULSE magnitude");
        double startTime = c.evaluateConstant(args.get(1), "PULSE startTime");
        double[] dtH = c.getContext().getDtHolder();
        LongSupplier stepSupplier = c.getContext().getCurrentStep();
        if (args.size() == 3) {
            double interval = c.evaluateConstant(args.get(2), "PULSE interval");
            return () -> {
                long step = stepSupplier.getAsLong();
                long startStep = Math.round(startTime / dtH[0]);
                if (step < startStep) {
                    return 0.0;
                }
                if (step == startStep) {
                    return magnitude;
                }
                long intervalSteps = Math.round(interval / dtH[0]);
                if (intervalSteps > 0 && (step - startStep) % intervalSteps == 0) {
                    return magnitude;
                }
                return 0.0;
            };
        } else {
            return () -> {
                long step = stepSupplier.getAsLong();
                long startStep = Math.round(startTime / dtH[0]);
                return step == startStep ? magnitude : 0.0;
            };
        }
    }

    private static DoubleSupplier compilePulseTrain(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs("PULSE_TRAIN", args, 4);
        DoubleSupplier startTime = c.compileExpr(args.get(0));
        DoubleSupplier duration = c.compileExpr(args.get(1));
        DoubleSupplier repeatInterval = c.compileExpr(args.get(2));
        DoubleSupplier endTime = c.compileExpr(args.get(3));
        double[] dtH = c.getContext().getDtHolder();
        LongSupplier stepSupplier = c.getContext().getCurrentStep();
        return () -> {
            double t = stepSupplier.getAsLong() * dtH[0];
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
                double tol = dtH[0] * 1e-6;
                if (repeat - phase < tol) {
                    phase = 0.0;
                }
                return phase < dur ? 1.0 : 0.0;
            }
            return elapsed < dur ? 1.0 : 0.0;
        };
    }

    private static DoubleSupplier compileRandomNormal(String name, List<Expr> args,
                                                        ExprCompiler c) {
        if (args.size() < 4 || args.size() > 5) {
            throw new CompilationException(
                    "RANDOM_NORMAL requires 4-5 arguments, got " + args.size(),
                    "RANDOM_NORMAL");
        }
        DoubleSupplier minVal = c.compileExpr(args.get(0));
        DoubleSupplier maxVal = c.compileExpr(args.get(1));
        DoubleSupplier mean = c.compileExpr(args.get(2));
        DoubleSupplier stddev = c.compileExpr(args.get(3));
        long userSeed = args.size() == 5
                ? Math.round(c.evaluateConstant(args.get(4), "RANDOM_NORMAL seed")) : 0L;
        long seed = userSeed != 0 ? userSeed
                : System.nanoTime() ^ SEED_COUNTER.incrementAndGet();
        java.util.Random rng = new java.util.Random(seed);
        c.addResettable(() -> rng.setSeed(seed));
        return () -> {
            double raw = mean.getAsDouble() + stddev.getAsDouble() * rng.nextGaussian();
            return Math.max(minVal.getAsDouble(), Math.min(maxVal.getAsDouble(), raw));
        };
    }

    private static DoubleSupplier compileRandomUniform(String name, List<Expr> args,
                                                         ExprCompiler c) {
        c.requireArgs("RANDOM_UNIFORM", args, 3);
        DoubleSupplier minVal = c.compileExpr(args.get(0));
        DoubleSupplier maxVal = c.compileExpr(args.get(1));
        long userSeed = Math.round(c.evaluateConstant(args.get(2), "RANDOM_UNIFORM seed"));
        long seed = userSeed != 0 ? userSeed
                : System.nanoTime() ^ SEED_COUNTER.incrementAndGet();
        java.util.Random rng = new java.util.Random(seed);
        c.addResettable(() -> rng.setSeed(seed));
        return () -> {
            double lo = minVal.getAsDouble();
            double hi = maxVal.getAsDouble();
            return lo + (hi - lo) * rng.nextDouble();
        };
    }
}
