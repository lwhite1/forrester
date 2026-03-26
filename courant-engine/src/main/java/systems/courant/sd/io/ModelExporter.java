package systems.courant.sd.io;

import systems.courant.sd.model.def.ModelDefinition;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Format-agnostic interface for exporting system dynamics models to external
 * tool formats (e.g., Vensim .mdl, XMILE).
 *
 * <p>Implementations translate a {@link ModelDefinition} into the target
 * file format. Shared export utilities (lookup handling, name map building)
 * are available in {@link ExportUtils}.</p>
 */
public interface ModelExporter {

    /**
     * Exports a model definition to a string in the target format.
     *
     * @param definition the model definition to export
     * @return the formatted model content
     */
    String export(ModelDefinition definition);

    /**
     * Exports a model definition to a file on disk.
     *
     * @param definition the model definition to export
     * @param path       the file path to write to
     * @throws IOException if the file cannot be written
     */
    void exportToFile(ModelDefinition definition, Path path) throws IOException;
}
