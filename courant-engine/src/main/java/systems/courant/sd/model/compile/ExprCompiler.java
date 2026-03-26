package systems.courant.sd.model.compile;

import systems.courant.sd.model.Formula;
import systems.courant.sd.model.expr.BinaryOperator;
import systems.courant.sd.model.expr.Expr;
import systems.courant.sd.model.expr.ExprParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.DoubleSupplier;

/**
 * Compiles an {@link Expr} AST into executable {@link Formula} lambdas using a
 * {@link CompilationContext} for name resolution.
 *
 * <p>Function calls are delegated to a {@link FunctionCompilerRegistry}, so new
 * built-in functions can be added without modifying this class.</p>
 */
public class ExprCompiler {

    private static final Logger logger = LoggerFactory.getLogger(ExprCompiler.class);

    private final CompilationContext context;
    private final List<Resettable> resettables;
    private final FunctionCompilerRegistry functionRegistry;

    /**
     * Creates an expression compiler that resolves names against the given context
     * and registers stateful formulas in the resettables list.
     *
     * @param context     the compilation context for name resolution
     * @param resettables the list where stateful formulas (e.g., SMOOTH, DELAY3) are registered
     */
    public ExprCompiler(CompilationContext context, List<Resettable> resettables) {
        this(context, resettables, FunctionCompilerRegistry.createDefault());
    }

    /**
     * Creates an expression compiler with a custom function registry.
     *
     * @param context          the compilation context for name resolution
     * @param resettables      the list where stateful formulas are registered
     * @param functionRegistry the registry of function compilers to use
     */
    public ExprCompiler(CompilationContext context, List<Resettable> resettables,
                        FunctionCompilerRegistry functionRegistry) {
        this.context = context;
        this.resettables = resettables;
        this.functionRegistry = functionRegistry;
    }

    /**
     * Creates a warned flag array and registers a resettable that clears it between runs.
     */
    boolean[] newWarnedFlag() {
        boolean[] warned = {false};
        resettables.add(() -> warned[0] = false);
        return warned;
    }

    /**
     * Registers a resettable that will be cleared between simulation runs.
     */
    void addResettable(Resettable resettable) {
        resettables.add(resettable);
    }

    /**
     * Returns the compilation context for name resolution and model state access.
     */
    CompilationContext getContext() {
        return context;
    }

    /**
     * Returns the resettables list for direct access by function compilers
     * (e.g., FIND_ZERO which creates a child compiler).
     */
    List<Resettable> getResettables() {
        return resettables;
    }

    /**
     * Compiles an expression string into a Formula.
     *
     * @param equation the expression string to parse and compile
     * @return a Formula that evaluates the expression at runtime
     * @throws CompilationException if the expression references unknown names or functions
     * @throws RuntimeException if the expression string is syntactically invalid
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
                boolean[] warned = newWarnedFlag();
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
                boolean[] warned = newWarnedFlag();
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
                boolean[] warned = newWarnedFlag();
                yield () -> {
                    double result = Math.pow(left.getAsDouble(), right.getAsDouble());
                    if (Double.isNaN(result)) {
                        if (!warned[0]) {
                            logger.warn("Power produced NaN result");
                            warned[0] = true;
                        }
                    } else if (Double.isInfinite(result) && !warned[0]) {
                        logger.warn("Power produced infinite result");
                        warned[0] = true;
                    }
                    return result;
                };
            }
            case EQ -> () -> {
                double a = left.getAsDouble();
                double b = right.getAsDouble();
                return Math.abs(a - b) <= 1e-10 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)))
                        ? 1.0 : 0.0;
            };
            case NE -> () -> {
                double a = left.getAsDouble();
                double b = right.getAsDouble();
                return Math.abs(a - b) > 1e-10 * Math.max(1.0, Math.max(Math.abs(a), Math.abs(b)))
                        ? 1.0 : 0.0;
            };
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
        String name = call.name().toUpperCase(java.util.Locale.ROOT);
        List<Expr> args = call.arguments();

        // Special variables (TIME, DT) are not true functions
        if ("TIME".equals(name) || "DT".equals(name)) {
            return compileSpecialVariable(name, args);
        }

        // Delegate to registered function compiler
        Optional<FunctionCompiler> compiler = functionRegistry.find(name);
        if (compiler.isPresent()) {
            return compiler.get().compile(name, args, this);
        }

        // Check if the function name is a lookup table (Vensim allows table(input) syntax)
        String originalName = call.name();
        if (args.size() == 1 && context.resolveLookupTable(originalName).isPresent()) {
            List<Expr> lookupArgs = List.of(new Expr.Ref(originalName), args.get(0));
            return functionRegistry.find("LOOKUP")
                    .orElseThrow(() -> new CompilationException(
                            "LOOKUP function not registered", "LOOKUP"))
                    .compile("LOOKUP", lookupArgs, this);
        }

        throw new CompilationException("Unknown function: " + name, name);
    }

    private DoubleSupplier compileSpecialVariable(String name, List<Expr> args) {
        return switch (name) {
            case "TIME" -> {
                requireArgs(name, args, 0);
                double[] dtH = context.getDtHolder();
                yield () -> context.getCurrentStep().getAsLong() * dtH[0];
            }
            case "DT" -> {
                requireArgs(name, args, 0);
                yield () -> context.getDt();
            }
            default -> throw new CompilationException(
                    "Unknown special variable: " + name, name);
        };
    }

    private DoubleSupplier compileConditional(Expr.Conditional cond) {
        DoubleSupplier condition = compileExpr(cond.condition());
        DoubleSupplier thenExpr = compileExpr(cond.thenExpr());
        DoubleSupplier elseExpr = compileExpr(cond.elseExpr());
        if (cond.shortCircuit()) {
            // IF_SHORT: only evaluate the taken branch (true short-circuit).
            // Stateful functions (SMOOTH, DELAY, TREND) in the untaken branch
            // will NOT be updated, causing stale values when the condition flips.
            return () -> condition.getAsDouble() != 0
                    ? thenExpr.getAsDouble() : elseExpr.getAsDouble();
        }
        // IF: evaluate condition first, then both branches every step so stateful
        // SD functions (SMOOTH, DELAY3, TREND, etc.) in the untaken branch keep
        // their internal state current without influencing the condition result.
        return () -> {
            double condVal = condition.getAsDouble();
            double thenVal = thenExpr.getAsDouble();
            double elseVal = elseExpr.getAsDouble();
            return condVal != 0 ? thenVal : elseVal;
        };
    }

    /**
     * Compiles an initial-value expression. If the expression is a compile-time constant,
     * returns a supplier wrapping that constant. Otherwise, returns the compiled expression
     * as a supplier — deferring evaluation to simulation start when all model elements
     * are initialized through holder indirection.
     */
    DoubleSupplier compileInitialValue(Expr expr, String paramDescription) {
        try {
            double constant = evaluateConstant(expr, paramDescription);
            if (Double.isNaN(constant)) {
                String msg = paramDescription + " evaluated to NaN; using 0.0";
                logger.warn(msg);
                context.addWarning(msg);
                return () -> 0.0;
            }
            return () -> constant;
        } catch (CompilationException e) {
            // Defer evaluation to simulation start when holders are filled
            return compileExpr(expr);
        }
    }

    /**
     * Evaluates an expression at compile time to obtain a numeric value.
     * Tries strict constant evaluation first (literals, named constants,
     * arithmetic on those). If that fails, falls back to compiling and
     * evaluating the full expression against current model state, which
     * may contain uninitialized variables. A warning is logged when the
     * fallback is used so that incorrect initial values can be diagnosed.
     */
    double evaluateAtCompileTime(Expr expr, String paramDescription) {
        try {
            return evaluateConstant(expr, paramDescription);
        } catch (CompilationException e) {
            String msg = paramDescription
                    + ": expression is not a compile-time constant ("
                    + e.getMessage()
                    + ") — evaluating against current model state, which may"
                    + " contain uninitialized variables";
            logger.warn(msg);
            context.addWarning(msg);
            DoubleSupplier compiled = compileExpr(expr);
            return compiled.getAsDouble();
        }
    }

    /**
     * Evaluates a constant expression at compile time. Supports literals and
     * references to constants.
     */
    double evaluateConstant(Expr expr, String paramDescription) {
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
                && un.operator() == systems.courant.sd.model.expr.UnaryOperator.NEGATE) {
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
                case EQ -> Math.abs(left - right)
                        <= 1e-10 * Math.max(1.0, Math.max(Math.abs(left), Math.abs(right)))
                        ? 1.0 : 0.0;
                case NE -> Math.abs(left - right)
                        > 1e-10 * Math.max(1.0, Math.max(Math.abs(left), Math.abs(right)))
                        ? 1.0 : 0.0;
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

    void requireArgs(String funcName, List<Expr> args, int expected) {
        if (args.size() != expected) {
            throw new CompilationException(
                    funcName + " requires " + expected + " arguments, got " + args.size(),
                    funcName);
        }
    }
}
