package systems.courant.sd.tools.importer;

import systems.courant.sd.model.ModelMetadata;

/**
 * A single entry in a batch import manifest.
 *
 * @param url         the URL or local file path of the model file (.mdl, .xmile, .stmx)
 * @param className   the Java class name for generated demo code
 * @param id          optional kebab-case slug used as the JSON filename and catalog ID
 * @param category    optional sub-package / subdirectory (e.g. "ecology"), or null
 * @param displayName optional human-friendly model name for the catalog (e.g. "Corporate Growth"),
 *                    or null to derive from className
 * @param comment     optional description of the model
 * @param metadata    attribution and licensing metadata
 */
public record ManifestEntry(
        String url,
        String className,
        String id,
        String category,
        String displayName,
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
