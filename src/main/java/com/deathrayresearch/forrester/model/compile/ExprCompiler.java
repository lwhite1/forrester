package com.deathrayresearch.forrester.model.compile;

import com.deathrayresearch.forrester.model.Delay3;
import com.deathrayresearch.forrester.model.Formula;
import com.deathrayresearch.forrester.model.LookupTable;
import com.deathrayresearch.forrester.model.Ramp;
import com.deathrayresearch.forrester.model.Smooth;
import com.deathrayresearch.forrester.model.Step;
import com.deathrayresearch.forrester.model.expr.BinaryOperator;
import com.deathrayresearch.forrester.model.expr.Expr;
import com.deathrayresearch.forrester.model.expr.ExprParser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * Compiles an {@link Expr} AST into executable {@link Formula} lambdas using a
 * {@link CompilationContext} for name resolution.
 */
public class ExprCompiler {

    private final CompilationContext context;
    private final List<Resettable> resettables;

    public ExprCompiler(CompilationContext context, List<Resettable> resettables) {
        this.context = context;
        this.resettables = resettables;
    }

    /**
     * Compiles an expression string into a Formula.
     */
    public Formula compile(String equation) {
        Expr expr = ExprParser.parse(equation);
        DoubleSupplier supplier = compileExpr(expr);
        return supplier::getAsDouble;
    }

    /**
     * Compiles an Expr AST node into a DoubleSupplier.
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
            case DIV -> () -> left.getAsDouble() / right.getAsDouble();
            case MOD -> () -> left.getAsDouble() % right.getAsDouble();
            case POW -> () -> Math.pow(left.getAsDouble(), right.getAsDouble());
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
            case "TIME" -> () -> context.getCurrentStep().getAsInt();
            case "DT" -> () -> context.getDt();
            case "ABS" -> {
                requireArgs(name, args, 1);
                DoubleSupplier a = compileExpr(args.get(0));
                yield () -> Math.abs(a.getAsDouble());
            }
            case "SQRT" -> {
                requireArgs(name, args, 1);
                DoubleSupplier a = compileExpr(args.get(0));
                yield () -> Math.sqrt(a.getAsDouble());
            }
            case "LN" -> {
                requireArgs(name, args, 1);
                DoubleSupplier a = compileExpr(args.get(0));
                yield () -> Math.log(a.getAsDouble());
            }
            case "EXP" -> {
                requireArgs(name, args, 1);
                DoubleSupplier a = compileExpr(args.get(0));
                yield () -> Math.exp(a.getAsDouble());
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
            case "LOOKUP" -> compileLookup(args);
            default -> throw new CompilationException(
                    "Unknown function: " + name, name);
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
        Step step = Step.of(height, (int) time, context.getCurrentStep());
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
            ramp = Ramp.of(slope, (int) start, (int) end, context.getCurrentStep());
        } else {
            ramp = Ramp.of(slope, (int) start, context.getCurrentStep());
        }
        return ramp::getCurrentValue;
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
        LookupTable table = context.resolveLookupTable(tableName);
        if (table == null && tableName.contains("_")) {
            resolvedName = tableName.replace('_', ' ');
            table = context.resolveLookupTable(resolvedName);
        }
        if (table == null) {
            throw new CompilationException(
                    "Lookup table not found: " + tableName, tableName);
        }
        // Wire the compiled input expression to the table's input holder
        DoubleSupplier input = compileExpr(args.get(1));
        double[] inputHolder = context.resolveLookupInputHolder(resolvedName);
        LookupTable finalTable = table;
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
            Double val = context.resolveConstant(ref.name());
            if (val != null) {
                return val;
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
