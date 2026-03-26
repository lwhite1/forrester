package systems.courant.sd.model.compile;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Registry mapping upper-case function names to their {@link FunctionCompiler}
 * implementations. A default registry is created via {@link #createDefault()},
 * which registers all built-in SD functions grouped by category.
 *
 * <p>To add a new function, create or extend a category class and call
 * {@link #register(String, FunctionCompiler)} — no changes to
 * {@link ExprCompiler} are needed.</p>
 */
public class FunctionCompilerRegistry {

    private final Map<String, FunctionCompiler> compilers = new LinkedHashMap<>();

    /**
     * Registers a function compiler under the given name (case-insensitive).
     */
    public void register(String name, FunctionCompiler compiler) {
        compilers.put(name.toUpperCase(Locale.ROOT), compiler);
    }

    /**
     * Looks up the compiler for the given function name (case-insensitive).
     */
    public Optional<FunctionCompiler> find(String name) {
        return Optional.ofNullable(compilers.get(name.toUpperCase(Locale.ROOT)));
    }

    /**
     * Creates a registry pre-populated with all built-in SD functions.
     */
    public static FunctionCompilerRegistry createDefault() {
        FunctionCompilerRegistry registry = new FunctionCompilerRegistry();
        MathFunctionCompilers.registerAll(registry);
        AggregateFunctionCompilers.registerAll(registry);
        StatefulFunctionCompilers.registerAll(registry);
        InputFunctionCompilers.registerAll(registry);
        LookupFunctionCompilers.registerAll(registry);
        return registry;
    }
}
