package systems.courant.shrewd.tools.importer;

import systems.courant.shrewd.model.ModelMetadata;

/**
 * A single entry in a batch import manifest.
 *
 * @param url       the remote URL of the model file (.mdl, .xmile, .stmx)
 * @param className the Java class name or JSON file base name for the output
 * @param category  optional sub-package / subdirectory (e.g. "ecology"), or null
 * @param comment   optional description of the model
 * @param metadata  attribution and licensing metadata
 */
public record ManifestEntry(
        String url,
        String className,
        String category,
        String comment,
        ModelMetadata metadata
) {

    public ManifestEntry {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Manifest entry url must not be blank");
        }
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("Manifest entry className must not be blank");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("Manifest entry metadata must not be null");
        }
    }
}
