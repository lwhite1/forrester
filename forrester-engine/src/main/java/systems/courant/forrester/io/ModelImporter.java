package systems.courant.forrester.io;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Format-agnostic interface for importing system dynamics models from external tools.
 *
 * <p>Implementations translate tool-specific file formats (e.g., Vensim .mdl, XMILE)
 * into a {@link systems.courant.forrester.model.def.ModelDefinition} wrapped in
 * an {@link ImportResult}.
 */
public interface ModelImporter {

    /**
     * Imports a model from a file on disk.
     *
     * @param path the path to the model file
     * @return the import result containing the model definition and any warnings
     * @throws IOException if the file cannot be read
     */
    ImportResult importModel(Path path) throws IOException;

    /**
     * Imports a model from a string containing the file content.
     *
     * @param content the raw file content
     * @param modelName the name to assign to the imported model
     * @return the import result containing the model definition and any warnings
     */
    ImportResult importModel(String content, String modelName);
}
