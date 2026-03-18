package systems.courant.sd.model;

import java.util.Set;
import java.util.function.Function;

/**
 * Resolves element names across the underscore/space boundary.
 *
 * <p>Element names in a model use spaces (e.g. "birth rate") but equation
 * references use underscores (e.g. "birth_rate"). This utility centralises
 * the fallback-lookup pattern that was previously duplicated across
 * {@code CompilationContext}, {@code FeedbackAnalysis}, and
 * {@code ModelValidator}.
 */
public final class NameResolver {

    private NameResolver() {
    }

    /**
     * Tries {@code lookup.apply(name)}; if the result is {@code null} and
     * the name contains an underscore, retries with underscores replaced by
     * spaces.
     *
     * @param name   the reference name (may contain underscores)
     * @param lookup a function that returns a value for a known name, or {@code null}
     * @param <V>    the value type
     * @return the first non-null result, or {@code null} if neither form matches
     */
    public static <V> V resolve(String name, Function<String, V> lookup) {
        V value = lookup.apply(name);
        if (value != null) {
            return value;
        }
        if (name.contains("_")) {
            return lookup.apply(name.replace('_', ' '));
        }
        return null;
    }

    /**
     * Returns the matching form of {@code name} present in {@code names},
     * trying the exact name first, then the underscore-to-space variant.
     *
     * @param name  the reference name
     * @param names the set of known element names
     * @return the form found in the set, or {@code null}
     */
    public static String resolveInSet(String name, Set<String> names) {
        if (names.contains(name)) {
            return name;
        }
        String spaced = name.replace('_', ' ');
        if (names.contains(spaced)) {
            return spaced;
        }
        return null;
    }

    /**
     * Checks whether {@code name} is present in {@code names} under any
     * variant: exact, underscore→space, or space→underscore.
     *
     * @param name  the element name to check
     * @param names the set of reference names (typically extracted from equations)
     * @return {@code true} if any variant of the name is in the set
     */
    public static boolean containsName(String name, Set<String> names) {
        if (names.contains(name)) {
            return true;
        }
        if (names.contains(name.replace(' ', '_'))) {
            return true;
        }
        return names.contains(name.replace('_', ' '));
    }
}
