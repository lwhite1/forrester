package systems.courant.sd.io;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Base class for {@link ModelImporter} implementations. Provides common
 * file-reading logic (size validation, encoding fallback) and shared
 * utilities so that format-specific importers focus only on parsing.
 *
 * <p>Subclasses implement {@link #importModel(String, String)} for
 * format-specific parsing; file I/O is handled here.</p>
 */
public abstract class AbstractModelImporter implements ModelImporter {

    /** Maximum file size accepted for import (10 MB). */
    protected static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    /** Regex for detecting numeric literals (integer, decimal, scientific notation). */
    private static final Pattern NUMERIC_PATTERN = Pattern.compile(
            "^[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?$");

    /**
     * Reads a model file with encoding fallback (UTF-8 → windows-1252),
     * validates its size, extracts the model name from the filename, and
     * delegates to {@link #importModel(String, String)}.
     */
    @Override
    public ImportResult importModel(Path path) throws IOException {
        long size = Files.size(path);
        if (size > MAX_FILE_SIZE) {
            throw new IOException("File exceeds maximum allowed size of "
                    + (MAX_FILE_SIZE / (1024 * 1024)) + " MB: " + path);
        }
        String content = readFileContent(path);
        String modelName = extractModelName(path);
        return importModel(content, modelName);
    }

    /**
     * Reads file content with UTF-8, falling back to windows-1252 on encoding errors.
     */
    protected String readFileContent(Path path) throws IOException {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (CharacterCodingException e) {
            return Files.readString(path, Charset.forName("windows-1252"));
        }
    }

    /**
     * Extracts the model name from a file path by stripping the extension.
     */
    protected String extractModelName(Path path) {
        Path fileName = path.getFileName();
        String name = fileName != null ? fileName.toString() : path.toString();
        int dotPos = name.lastIndexOf('.');
        if (dotPos > 0) {
            name = name.substring(0, dotPos);
        }
        return name;
    }

    /**
     * Returns true if the expression is a numeric literal (integer, decimal,
     * or scientific notation).
     */
    public static boolean isNumericLiteral(String expr) {
        return expr != null && NUMERIC_PATTERN.matcher(expr.strip()).matches();
    }
}
