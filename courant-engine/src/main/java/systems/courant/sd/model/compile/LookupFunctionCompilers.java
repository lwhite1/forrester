package systems.courant.sd.model.compile;

import systems.courant.sd.model.LookupTable;
import systems.courant.sd.model.expr.Expr;

import java.util.List;
import java.util.Optional;
import java.util.function.DoubleSupplier;

/**
 * Lookup table functions: LOOKUP and LOOKUP_AREA.
 */
final class LookupFunctionCompilers {

    private LookupFunctionCompilers() {
    }

    static void registerAll(FunctionCompilerRegistry r) {
        r.register("LOOKUP", LookupFunctionCompilers::compileLookup);
        r.register("LOOKUP_AREA", LookupFunctionCompilers::compileLookupArea);
    }

    static DoubleSupplier compileLookup(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs("LOOKUP", args, 2);
        if (!(args.get(0) instanceof Expr.Ref ref)) {
            throw new CompilationException(
                    "LOOKUP first argument must be a table name reference", "LOOKUP");
        }
        String tableName = ref.name();
        String resolvedName = tableName;
        CompilationContext ctx = c.getContext();
        Optional<LookupTable> existing = ctx.resolveLookupTable(tableName);
        if (existing.isEmpty() && tableName.contains("_")) {
            resolvedName = tableName.replace('_', ' ');
            existing = ctx.resolveLookupTable(resolvedName);
        }
        if (existing.isEmpty()) {
            throw new CompilationException(
                    "Lookup table not found: " + tableName, tableName);
        }
        DoubleSupplier input = c.compileExpr(args.get(1));
        Optional<LookupTable> freshTable = ctx.createFreshLookupTable(resolvedName, input);
        if (freshTable.isPresent()) {
            return freshTable.get()::getCurrentValue;
        }
        LookupTable isolatedTable = existing.get().withInput(input);
        return isolatedTable::getCurrentValue;
    }

    private static DoubleSupplier compileLookupArea(String name, List<Expr> args, ExprCompiler c) {
        c.requireArgs("LOOKUP_AREA", args, 3);
        if (!(args.get(0) instanceof Expr.Ref ref)) {
            throw new CompilationException(
                    "LOOKUP_AREA first argument must be a table name reference", "LOOKUP_AREA");
        }
        String tableName = ref.name();
        String resolvedName = tableName;
        CompilationContext ctx = c.getContext();
        Optional<systems.courant.sd.model.def.LookupTableDef> defOpt =
                ctx.resolveLookupTableDef(tableName);
        if (defOpt.isEmpty() && tableName.contains("_")) {
            resolvedName = tableName.replace('_', ' ');
            defOpt = ctx.resolveLookupTableDef(resolvedName);
        }
        if (defOpt.isEmpty()) {
            throw new CompilationException(
                    "Lookup table not found: " + tableName, tableName);
        }
        double[] xValues = defOpt.get().xValues();
        DoubleSupplier fromX = c.compileExpr(args.get(1));
        DoubleSupplier toX = c.compileExpr(args.get(2));

        Optional<LookupTable> tableOpt = ctx.createFreshLookupTable(resolvedName, () -> 0);
        if (tableOpt.isEmpty()) {
            tableOpt = ctx.resolveLookupTable(resolvedName);
        }
        if (tableOpt.isEmpty()) {
            throw new CompilationException(
                    "Lookup table not found: " + tableName, tableName);
        }
        LookupTable table = tableOpt.get();
        return () -> {
            double x1 = fromX.getAsDouble();
            double x2 = toX.getAsDouble();
            if (x1 == x2) {
                return 0.0;
            }
            boolean negate = x1 > x2;
            double lo = negate ? x2 : x1;
            double hi = negate ? x1 : x2;
            double area = 0.0;
            double prevX = lo;
            double prevY = table.evaluate(lo);
            for (double xVal : xValues) {
                if (xVal <= lo) {
                    continue;
                }
                if (xVal >= hi) {
                    break;
                }
                double curY = table.evaluate(xVal);
                area += (xVal - prevX) * (prevY + curY) / 2.0;
                prevX = xVal;
                prevY = curY;
            }
            double hiY = table.evaluate(hi);
            area += (hi - prevX) * (prevY + hiY) / 2.0;
            return negate ? -area : area;
        };
    }
}
