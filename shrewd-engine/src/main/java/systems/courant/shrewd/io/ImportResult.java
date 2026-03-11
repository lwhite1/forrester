package systems.courant.shrewd.io;

import systems.courant.shrewd.model.def.ModelDefinition;

import java.util.List;

/**
 * Result of importing a model from an external format.
 *
 * @param definition the imported model definition
 * @param warnings any non-fatal warnings generated during import
 */
public record ImportResult(
        ModelDefinition definition,
        List<String> warnings
) {

    public ImportResult {
        if (definition == null) {
            throw new IllegalArgumentException("Definition must not be null");
        }
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    /**
     * Returns {@code true} if the import produced no warnings.
     */
    public boolean isClean() {
        return warnings.isEmpty();
    }
}
