package systems.courant.sd.model.compile;

import systems.courant.sd.model.Delay1;
import systems.courant.sd.model.Delay3;
import systems.courant.sd.model.DelayFixed;
import systems.courant.sd.model.FindZero;
import systems.courant.sd.model.Forecast;
import systems.courant.sd.model.Npv;
import systems.courant.sd.model.SampleIfTrue;
import systems.courant.sd.model.Smooth;
import systems.courant.sd.model.Smooth3;
import systems.courant.sd.model.Trend;
import systems.courant.sd.model.expr.Expr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * Stateful functions that carry internal state between simulation steps:
 * SMOOTH, DELAY, TREND, FORECAST, NPV, SAMPLE_IF_TRUE, FIND_ZERO, and INITIAL.
 * Each registers a {@link Resettable} so its state is cleared between runs.
 */
final class StatefulFunctionCompilers {

    private static final Logger logger = LoggerFactory.getLogger(StatefulFunctionCompilers.class);

    private StatefulFunctionCompilers() {
    }

    static void registerAll(FunctionCompilerRegistry r) {
        r.register("INITIAL", StatefulFunctionCompilers::compileInitial);
        r.register("SMOOTH", StatefulFunctionCompilers::compileSmooth);
        r.register("SMOOTHI", StatefulFunctionCompilers::compileSmoothI);
        r.register("SMOOTH3", StatefulFunctionCompilers::compileSmooth3);
        r.register("SMOOTH3I", StatefulFunctionCompilers::compileSmooth3I);
        r.register("DELAY1", StatefulFunctionCompilers::compileDelay1);
        r.register("DELAY1I", StatefulFunctionCompilers::compileDelay1I);
        r.register("DELAY3", StatefulFunctionCompilers::compileDelay3);
        r.register("DELAY3I", StatefulFunctionCompilers::compileDelay3I);
        r.register("DELAY_FIXED", StatefulFunctionCompilers::compileDelayFixed);
        r.register("TREND", StatefulFunctionCompilers::compileTrend);
        r.register("FORECAST", StatefulFunctionCompilers::compileForecast);
        r.register("NPV", StatefulFunctionCompilers::compileNpv);
        r.register("SAMPLE_IF_TRUE", StatefulFunctionCompilers::compileSampleIfTrue);
        r.register("FIND_ZERO", StatefulFunctionCompilers::compileFindZero);
    }

    private static DoubleSupplier compileInitial(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs(name, args, 1);
        DoubleSupplier a = c.compileExpr(args.get(0));
        var holder = new Object() { volatile double value = Double.NaN; };
        c.addResettable(() -> holder.value = Double.NaN);
        return () -> {
            double v = holder.value;
            if (Double.isNaN(v)) {
                v = a.getAsDouble();
                holder.value = v;
            }
            return v;
        };
    }

    // ---- SMOOTH family ----

    private static DoubleSupplier compileSmooth(String name, List<Expr> args, ExprCompiler c) {
        if (args.size() < 2 || args.size() > 3) {
            throw new CompilationException(
                    "SMOOTH requires 2-3 arguments, got " + args.size(), "SMOOTH");
        }
        DoubleSupplier input = c.compileExpr(args.get(0));
        DoubleSupplier smoothingTime = c.compileExpr(args.get(1));
        double[] dtH = c.getContext().getDtHolder();
        Smooth smooth;
        if (args.size() == 3) {
            DoubleSupplier initial = c.compileInitialValue(args.get(2), "SMOOTH initialValue");
            smooth = Smooth.of(input, smoothingTime, initial, dtH, c.getContext().getCurrentStep());
        } else {
            smooth = Smooth.of(input, smoothingTime, dtH, c.getContext().getCurrentStep());
        }
        c.addResettable(smooth);
        return smooth::getCurrentValue;
    }

    private static DoubleSupplier compileSmoothI(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs("SMOOTHI", args, 3);
        DoubleSupplier input = c.compileExpr(args.get(0));
        DoubleSupplier smoothingTime = c.compileExpr(args.get(1));
        DoubleSupplier initial = c.compileInitialValue(args.get(2), "SMOOTHI initialValue");
        double[] dtH = c.getContext().getDtHolder();
        Smooth smooth = Smooth.of(input, smoothingTime, initial, dtH, c.getContext().getCurrentStep());
        c.addResettable(smooth);
        return smooth::getCurrentValue;
    }

    private static DoubleSupplier compileSmooth3(String name, List<Expr> args, ExprCompiler c) {
        if (args.size() < 2 || args.size() > 3) {
            throw new CompilationException(
                    "SMOOTH3 requires 2-3 arguments, got " + args.size(), "SMOOTH3");
        }
        DoubleSupplier input = c.compileExpr(args.get(0));
        DoubleSupplier smoothingTime = c.compileExpr(args.get(1));
        double[] dtH = c.getContext().getDtHolder();
        Smooth3 smooth3;
        if (args.size() == 3) {
            DoubleSupplier initial = c.compileInitialValue(args.get(2), "SMOOTH3 initialValue");
            smooth3 = Smooth3.of(input, smoothingTime, initial, dtH, c.getContext().getCurrentStep());
        } else {
            smooth3 = Smooth3.of(input, smoothingTime, dtH, c.getContext().getCurrentStep());
        }
        c.addResettable(smooth3);
        return smooth3::getCurrentValue;
    }

    private static DoubleSupplier compileSmooth3I(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs("SMOOTH3I", args, 3);
        DoubleSupplier input = c.compileExpr(args.get(0));
        DoubleSupplier smoothingTime = c.compileExpr(args.get(1));
        DoubleSupplier initial = c.compileInitialValue(args.get(2), "SMOOTH3I initialValue");
        double[] dtH = c.getContext().getDtHolder();
        Smooth3 smooth3 = Smooth3.of(input, smoothingTime, initial, dtH, c.getContext().getCurrentStep());
        c.addResettable(smooth3);
        return smooth3::getCurrentValue;
    }

    // ---- DELAY family ----

    private static DoubleSupplier compileDelay1(String name, List<Expr> args, ExprCompiler c) {
        if (args.size() < 2 || args.size() > 3) {
            throw new CompilationException(
                    "DELAY1 requires 2-3 arguments, got " + args.size(), "DELAY1");
        }
        DoubleSupplier input = c.compileExpr(args.get(0));
        double delayTime = c.evaluateAtCompileTime(args.get(1), "DELAY1 delayTime");
        if (delayTime <= 0 || Double.isNaN(delayTime)) {
            String msg = "DELAY1 delayTime evaluated to " + delayTime
                    + " at compile time; using default of 1.0 — simulation results may be inaccurate";
            logger.warn(msg);
            c.getContext().addWarning(msg);
            delayTime = 1.0;
        }
        double[] dtH = c.getContext().getDtHolder();
        Delay1 delay1;
        if (args.size() == 3) {
            DoubleSupplier initial = c.compileInitialValue(args.get(2), "DELAY1 initialValue");
            delay1 = Delay1.of(input, delayTime, initial, dtH, c.getContext().getCurrentStep());
        } else {
            delay1 = Delay1.of(input, delayTime, dtH, c.getContext().getCurrentStep());
        }
        c.addResettable(delay1);
        return delay1::getCurrentValue;
    }

    private static DoubleSupplier compileDelay1I(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs("DELAY1I", args, 3);
        return compileDelay1("DELAY1", args, c);
    }

    private static DoubleSupplier compileDelay3(String name, List<Expr> args, ExprCompiler c) {
        if (args.size() < 2 || args.size() > 3) {
            throw new CompilationException(
                    "DELAY3 requires 2-3 arguments, got " + args.size(), "DELAY3");
        }
        DoubleSupplier input = c.compileExpr(args.get(0));
        double delayTime = c.evaluateAtCompileTime(args.get(1), "DELAY3 delayTime");
        if (delayTime <= 0 || Double.isNaN(delayTime)) {
            String msg = "DELAY3 delayTime evaluated to " + delayTime
                    + " at compile time; using default of 1.0 — simulation results may be inaccurate";
            logger.warn(msg);
            c.getContext().addWarning(msg);
            delayTime = 1.0;
        }
        double[] dtH = c.getContext().getDtHolder();
        Delay3 delay3;
        if (args.size() == 3) {
            DoubleSupplier initial = c.compileInitialValue(args.get(2), "DELAY3 initialValue");
            delay3 = Delay3.of(input, delayTime, initial, dtH, c.getContext().getCurrentStep());
        } else {
            delay3 = Delay3.of(input, delayTime, dtH, c.getContext().getCurrentStep());
        }
        c.addResettable(delay3);
        return delay3::getCurrentValue;
    }

    private static DoubleSupplier compileDelay3I(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs("DELAY3I", args, 3);
        return compileDelay3("DELAY3", args, c);
    }

    private static DoubleSupplier compileDelayFixed(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs("DELAY_FIXED", args, 3);
        DoubleSupplier input = c.compileExpr(args.get(0));
        double delayTime = c.evaluateConstant(args.get(1), "DELAY_FIXED delayTime");
        CompilationContext ctx = c.getContext();
        if (Double.isNaN(delayTime) || delayTime <= 0) {
            String msg = "DELAY_FIXED delayTime evaluated to " + delayTime
                    + " at compile time; using default of 1.0"
                    + " — simulation results may be inaccurate";
            logger.warn(msg);
            ctx.addWarning(msg);
            delayTime = 1.0;
        } else {
            int estimatedSteps = (int) Math.round(delayTime / ctx.getDt());
            if (estimatedSteps <= 0) {
                String msg = "DELAY_FIXED delayTime " + delayTime
                        + " rounds to 0 steps at current DT; will default to 1 step"
                        + " — simulation results may be inaccurate";
                logger.warn(msg);
                ctx.addWarning(msg);
            }
        }
        DoubleSupplier initial = c.compileExpr(args.get(2));
        double[] dtH = ctx.getDtHolder();
        DelayFixed delayFixed = DelayFixed.of(input, delayTime, dtH,
                initial, ctx.getCurrentStep());
        c.addResettable(delayFixed);
        return delayFixed::getCurrentValue;
    }

    // ---- Other stateful ----

    private static DoubleSupplier compileTrend(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs("TREND", args, 3);
        DoubleSupplier input = c.compileExpr(args.get(0));
        double averagingTime = c.evaluateConstant(args.get(1), "TREND averagingTime");
        double initialTrend = c.evaluateConstant(args.get(2), "TREND initialTrend");
        double[] dtH = c.getContext().getDtHolder();
        Trend trend = Trend.of(input, averagingTime, initialTrend, dtH,
                c.getContext().getCurrentStep());
        c.addResettable(trend);
        return trend::getCurrentValue;
    }

    private static DoubleSupplier compileForecast(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs("FORECAST", args, 4);
        DoubleSupplier input = c.compileExpr(args.get(0));
        double averagingTime = c.evaluateConstant(args.get(1), "FORECAST averagingTime");
        double horizon = c.evaluateConstant(args.get(2), "FORECAST horizon");
        double initialTrend = c.evaluateConstant(args.get(3), "FORECAST initialTrend");
        double[] dtH = c.getContext().getDtHolder();
        Forecast forecast = Forecast.of(input, averagingTime, horizon, initialTrend,
                dtH, c.getContext().getCurrentStep());
        c.addResettable(forecast);
        return forecast::getCurrentValue;
    }

    private static DoubleSupplier compileNpv(String name, List<Expr> args, ExprCompiler c) {
        if (args.size() < 2 || args.size() > 4) {
            throw new CompilationException(
                    "NPV requires 2-4 arguments, got " + args.size(), "NPV");
        }
        DoubleSupplier stream = c.compileExpr(args.get(0));
        double discountRate = c.evaluateConstant(args.get(1), "NPV discountRate");
        double initialValue = 0;
        double factor = 1.0;
        if (args.size() == 3) {
            factor = c.evaluateConstant(args.get(2), "NPV factor");
        } else if (args.size() == 4) {
            initialValue = c.evaluateConstant(args.get(2), "NPV initialValue");
            factor = c.evaluateConstant(args.get(3), "NPV factor");
        }
        Npv npv = Npv.of(stream, discountRate, factor, initialValue,
                c.getContext().getCurrentStep());
        c.addResettable(npv);
        return npv::getCurrentValue;
    }

    private static DoubleSupplier compileSampleIfTrue(String name, List<Expr> args,
                                                        ExprCompiler c) {
        c.requireArgs("SAMPLE_IF_TRUE", args, 3);
        DoubleSupplier condition = c.compileExpr(args.get(0));
        DoubleSupplier input = c.compileExpr(args.get(1));
        DoubleSupplier initial = c.compileInitialValue(args.get(2), "SAMPLE_IF_TRUE initialValue");
        SampleIfTrue sampler = SampleIfTrue.of(condition, input, initial,
                c.getContext().getCurrentStep());
        c.addResettable(sampler);
        return sampler::getCurrentValue;
    }

    private static DoubleSupplier compileFindZero(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs("FIND_ZERO", args, 4);
        if (!(args.get(1) instanceof Expr.Ref ref)) {
            throw new CompilationException(
                    "FIND_ZERO second argument must be a variable reference", "FIND_ZERO");
        }
        String varName = ref.name();
        double[] holder = {0.0};
        CompilationContext ctx = c.getContext();
        CompilationContext childContext = new CompilationContext(
                ctx.getUnitRegistry(), ctx.getCurrentStep(), ctx);
        childContext.addMutableHolder(varName, holder);
        ExprCompiler childCompiler = new ExprCompiler(childContext, c.getResettables());
        DoubleSupplier expression = childCompiler.compileExpr(args.get(0));
        DoubleSupplier lo = c.compileExpr(args.get(2));
        DoubleSupplier hi = c.compileExpr(args.get(3));
        FindZero findZero = FindZero.of(expression, holder, lo, hi);
        return findZero::getCurrentValue;
    }
}
