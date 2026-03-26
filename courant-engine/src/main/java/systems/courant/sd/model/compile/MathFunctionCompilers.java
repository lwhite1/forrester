package systems.courant.sd.model.compile;

import systems.courant.sd.model.expr.Expr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;

/**
 * Pure math functions, safe-division functions, and boolean logic functions.
 * All are stateless — they produce a result solely from their arguments
 * without carrying state between simulation steps.
 */
final class MathFunctionCompilers {

    private static final Logger logger = LoggerFactory.getLogger(MathFunctionCompilers.class);

    private MathFunctionCompilers() {
    }

    static void registerAll(FunctionCompilerRegistry r) {
        // Core math
        r.register("ABS", MathFunctionCompilers::compileAbs);
        r.register("SQRT", MathFunctionCompilers::compileSqrt);
        r.register("LN", MathFunctionCompilers::compileLn);
        r.register("EXP", MathFunctionCompilers::compileExp);
        r.register("LOG", MathFunctionCompilers::compileLog);
        r.register("SIN", (n, a, c) -> compileTrig(n, a, c, Math::sin));
        r.register("COS", (n, a, c) -> compileTrig(n, a, c, Math::cos));
        r.register("TAN", (n, a, c) -> compileTrig(n, a, c, Math::tan));
        r.register("ARCSIN", (n, a, c) -> compileArcSinCos(n, a, c, Math::asin));
        r.register("ARCCOS", (n, a, c) -> compileArcSinCos(n, a, c, Math::acos));
        r.register("ARCTAN", (n, a, c) -> compileTrig(n, a, c, Math::atan));
        r.register("SIGN", (n, a, c) -> compileTrig(n, a, c, Math::signum));
        r.register("PI", MathFunctionCompilers::compilePi);
        r.register("INT", MathFunctionCompilers::compileInt);
        r.register("ROUND", MathFunctionCompilers::compileRound);
        r.register("MODULO", MathFunctionCompilers::compileModulo);
        r.register("QUANTUM", MathFunctionCompilers::compileQuantum);
        r.register("POWER", MathFunctionCompilers::compilePower);
        r.register("MIN", (n, a, c) -> compileMinMax(n, a, c, Math::min));
        r.register("MAX", (n, a, c) -> compileMinMax(n, a, c, Math::max));

        // Safe division
        r.register("XIDZ", MathFunctionCompilers::compileXidz);
        r.register("ZIDZ", MathFunctionCompilers::compileZidz);

        // Logic
        r.register("NOT", MathFunctionCompilers::compileNot);
        r.register("OR", MathFunctionCompilers::compileOr);
        r.register("AND", MathFunctionCompilers::compileAnd);
        r.register("TRUE", MathFunctionCompilers::compileTrue);
        r.register("FALSE", MathFunctionCompilers::compileFalse);
    }

    // ---- Core math ----

    private static DoubleSupplier compileAbs(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs(name, args, 1);
        DoubleSupplier a = c.compileExpr(args.get(0));
        return () -> Math.abs(a.getAsDouble());
    }

    private static DoubleSupplier compileSqrt(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs(name, args, 1);
        DoubleSupplier a = c.compileExpr(args.get(0));
        boolean[] warned = c.newWarnedFlag();
        return () -> {
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

    private static DoubleSupplier compileLn(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs(name, args, 1);
        DoubleSupplier a = c.compileExpr(args.get(0));
        boolean[] warned = c.newWarnedFlag();
        return () -> {
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

    private static DoubleSupplier compileExp(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs(name, args, 1);
        DoubleSupplier a = c.compileExpr(args.get(0));
        boolean[] warned = c.newWarnedFlag();
        return () -> {
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

    private static DoubleSupplier compileLog(String name, List<Expr> args, ExprCompiler c) {
        if (args.size() == 2) {
            DoubleSupplier a = c.compileExpr(args.get(0));
            DoubleSupplier base = c.compileExpr(args.get(1));
            boolean[] warned = c.newWarnedFlag();
            return () -> {
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
        c.requireArgs(name, args, 1);
        DoubleSupplier a = c.compileExpr(args.get(0));
        boolean[] warned = c.newWarnedFlag();
        return () -> {
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

    private static DoubleSupplier compileTrig(String name, List<Expr> args, ExprCompiler c,
                                               DoubleUnaryOperator op) {
        c.requireArgs(name, args, 1);
        DoubleSupplier a = c.compileExpr(args.get(0));
        return () -> op.applyAsDouble(a.getAsDouble());
    }

    private static DoubleSupplier compileArcSinCos(String name, List<Expr> args, ExprCompiler c,
                                                     DoubleUnaryOperator op) {
        c.requireArgs(name, args, 1);
        DoubleSupplier a = c.compileExpr(args.get(0));
        boolean[] warned = c.newWarnedFlag();
        return () -> {
            double v = a.getAsDouble();
            if (v < -1 || v > 1) {
                if (!warned[0]) {
                    logger.warn("{} of value outside [-1, 1]: {}", name, v);
                    warned[0] = true;
                }
                return Double.NaN;
            }
            return op.applyAsDouble(v);
        };
    }

    private static DoubleSupplier compilePi(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs(name, args, 0);
        return () -> Math.PI;
    }

    private static DoubleSupplier compileInt(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs(name, args, 1);
        DoubleSupplier a = c.compileExpr(args.get(0));
        return () -> {
            double v = a.getAsDouble();
            return v >= 0 ? Math.floor(v) : Math.ceil(v);
        };
    }

    private static DoubleSupplier compileRound(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs(name, args, 1);
        DoubleSupplier a = c.compileExpr(args.get(0));
        return () -> {
            double v = a.getAsDouble();
            if (Math.abs(v) >= 1e15) {
                return v;
            }
            return Math.round(v);
        };
    }

    private static DoubleSupplier compileModulo(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs(name, args, 2);
        DoubleSupplier a = c.compileExpr(args.get(0));
        DoubleSupplier b = c.compileExpr(args.get(1));
        boolean[] warned = c.newWarnedFlag();
        return () -> {
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

    private static DoubleSupplier compileQuantum(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs(name, args, 2);
        DoubleSupplier a = c.compileExpr(args.get(0));
        DoubleSupplier b = c.compileExpr(args.get(1));
        boolean[] warned = c.newWarnedFlag();
        return () -> {
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

    private static DoubleSupplier compilePower(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs(name, args, 2);
        DoubleSupplier a = c.compileExpr(args.get(0));
        DoubleSupplier b = c.compileExpr(args.get(1));
        boolean[] warned = c.newWarnedFlag();
        return () -> {
            double result = Math.pow(a.getAsDouble(), b.getAsDouble());
            if (Double.isNaN(result)) {
                if (!warned[0]) {
                    logger.warn("POWER produced NaN result");
                    warned[0] = true;
                }
            } else if (Double.isInfinite(result) && !warned[0]) {
                logger.warn("POWER produced infinite result");
                warned[0] = true;
            }
            return result;
        };
    }

    private static DoubleSupplier compileMinMax(String name, List<Expr> args, ExprCompiler c,
                                                  DoubleBinaryOperator op) {
        c.requireArgs(name, args, 2);
        DoubleSupplier a = c.compileExpr(args.get(0));
        DoubleSupplier b = c.compileExpr(args.get(1));
        return () -> op.applyAsDouble(a.getAsDouble(), b.getAsDouble());
    }

    // ---- Safe division ----

    private static DoubleSupplier compileXidz(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs(name, args, 3);
        DoubleSupplier a = c.compileExpr(args.get(0));
        DoubleSupplier b = c.compileExpr(args.get(1));
        DoubleSupplier x = c.compileExpr(args.get(2));
        return () -> {
            double divisor = b.getAsDouble();
            return divisor == 0 ? x.getAsDouble() : a.getAsDouble() / divisor;
        };
    }

    private static DoubleSupplier compileZidz(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs(name, args, 2);
        DoubleSupplier a = c.compileExpr(args.get(0));
        DoubleSupplier b = c.compileExpr(args.get(1));
        return () -> {
            double divisor = b.getAsDouble();
            return divisor == 0 ? 0.0 : a.getAsDouble() / divisor;
        };
    }

    // ---- Logic ----

    private static DoubleSupplier compileNot(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs(name, args, 1);
        DoubleSupplier a = c.compileExpr(args.get(0));
        return () -> a.getAsDouble() == 0 ? 1.0 : 0.0;
    }

    private static DoubleSupplier compileOr(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs(name, args, 2);
        DoubleSupplier a = c.compileExpr(args.get(0));
        DoubleSupplier b = c.compileExpr(args.get(1));
        return () -> (a.getAsDouble() != 0 || b.getAsDouble() != 0) ? 1.0 : 0.0;
    }

    private static DoubleSupplier compileAnd(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs(name, args, 2);
        DoubleSupplier a = c.compileExpr(args.get(0));
        DoubleSupplier b = c.compileExpr(args.get(1));
        return () -> (a.getAsDouble() != 0 && b.getAsDouble() != 0) ? 1.0 : 0.0;
    }

    private static DoubleSupplier compileTrue(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs(name, args, 0);
        return () -> 1.0;
    }

    private static DoubleSupplier compileFalse(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs(name, args, 0);
        return () -> 0.0;
    }
}
