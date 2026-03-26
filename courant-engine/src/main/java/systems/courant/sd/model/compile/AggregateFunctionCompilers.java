package systems.courant.sd.model.compile;

import systems.courant.sd.model.expr.Expr;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * Aggregate functions that operate on variable-length argument lists:
 * SUM, MEAN, VMIN, VMAX, PROD.
 */
final class AggregateFunctionCompilers {

    private AggregateFunctionCompilers() {
    }

    static void registerAll(FunctionCompilerRegistry r) {
        r.register("SUM", AggregateFunctionCompilers::compileSum);
        r.register("MEAN", AggregateFunctionCompilers::compileMean);
        r.register("VMIN", AggregateFunctionCompilers::compileVmin);
        r.register("VMAX", AggregateFunctionCompilers::compileVmax);
        r.register("PROD", AggregateFunctionCompilers::compileProd);
    }

    private static List<DoubleSupplier> compileArgs(String name, List<Expr> args,
                                                      ExprCompiler c) {
        if (args.isEmpty()) {
            throw new CompilationException(
                    name + " requires at least 1 argument", name);
        }
        List<DoubleSupplier> compiled = new ArrayList<>();
        for (Expr arg : args) {
            compiled.add(c.compileExpr(arg));
        }
        return compiled;
    }

    private static DoubleSupplier compileSum(String name, List<Expr> args, ExprCompiler c) {
        List<DoubleSupplier> compiled = compileArgs(name, args, c);
        return () -> {
            double sum = 0;
            for (DoubleSupplier s : compiled) {
                sum += s.getAsDouble();
            }
            return sum;
        };
    }

    private static DoubleSupplier compileMean(String name, List<Expr> args, ExprCompiler c) {
        List<DoubleSupplier> compiled = compileArgs(name, args, c);
        int count = compiled.size();
        return () -> {
            double sum = 0;
            for (DoubleSupplier s : compiled) {
                sum += s.getAsDouble();
            }
            return sum / count;
        };
    }

    private static DoubleSupplier compileVmin(String name, List<Expr> args, ExprCompiler c) {
        List<DoubleSupplier> compiled = compileArgs(name, args, c);
        return () -> {
            double result = compiled.get(0).getAsDouble();
            for (int i = 1; i < compiled.size(); i++) {
                result = Math.min(result, compiled.get(i).getAsDouble());
            }
            return result;
        };
    }

    private static DoubleSupplier compileVmax(String name, List<Expr> args, ExprCompiler c) {
        List<DoubleSupplier> compiled = compileArgs(name, args, c);
        return () -> {
            double result = compiled.get(0).getAsDouble();
            for (int i = 1; i < compiled.size(); i++) {
                result = Math.max(result, compiled.get(i).getAsDouble());
            }
            return result;
        };
    }

    private static DoubleSupplier compileProd(String name, List<Expr> args, ExprCompiler c) {
        List<DoubleSupplier> compiled = compileArgs(name, args, c);
        return () -> {
            double result = 1;
            for (DoubleSupplier s : compiled) {
                result *= s.getAsDouble();
            }
            return result;
        };
    }
}
