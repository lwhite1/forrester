package systems.courant.sd.model.compile;

import systems.courant.sd.model.expr.Expr;

import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * Strategy interface for compiling a named built-in function call into an
 * executable {@link DoubleSupplier}. Implementations are registered in
 * {@link FunctionCompilerRegistry} and looked up by function name during
 * expression compilation.
 */
@FunctionalInterface
public interface FunctionCompiler {

    /**
     * Compiles a function call into a supplier that evaluates it at runtime.
     *
     * @param name     the upper-case function name (e.g. "SMOOTH", "ABS")
     * @param args     the argument expressions from the AST
     * @param compiler the parent expression compiler, used to compile sub-expressions
     *                 and access compilation utilities
     * @return a supplier that evaluates the function at each simulation step
     * @throws CompilationException if the arguments are invalid
     */
    DoubleSupplier compile(String name, List<Expr> args, ExprCompiler compiler);
}
