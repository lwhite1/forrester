package systems.courant.forrester.app;

import javafx.stage.FileChooser;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * Remembers the last directory used by file dialogs, persisted across sessions
 * via Java Preferences. Separate keys are stored for open and save operations.
 */
public final class LastDirectoryStore {

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(LastDirectoryStore.class);

    private static final String KEY_OPEN = "lastOpenDir";
    private static final String KEY_SAVE = "lastSaveDir";
    private static final String KEY_EXPORT = "lastExportDir";

    private LastDirectoryStore() {
    }

    /**
     * Sets the initial directory on a FileChooser from the stored preference.
     */
    public static void applyOpenDirectory(FileChooser chooser) {
        apply(chooser, KEY_OPEN);
    }

    /**
     * Sets the initial directory on a FileChooser from the stored preference.
     */
    public static void applySaveDirectory(FileChooser chooser) {
        apply(chooser, KEY_SAVE);
    }

    /**
     * Sets the initial directory on a FileChooser from the stored preference.
     */
    public static void applyExportDirectory(FileChooser chooser) {
        apply(chooser, KEY_EXPORT);
    }

    /**
     * Records the directory of the chosen file for future open dialogs.
     */
    public static void recordOpenDirectory(File file) {
        record(file, KEY_OPEN);
    }

    /**
     * Records the directory of the chosen file for future save dialogs.
     */
    public static void recordSaveDirectory(File file) {
        record(file, KEY_SAVE);
    }

    /**
     * Records the directory of the chosen file for future export dialogs.
     */
    public static void recordExportDirectory(File file) {
        record(file, KEY_EXPORT);
    }

    private static void apply(FileChooser chooser, String key) {
        String path = PREFS.get(key, null);
        if (path != null) {
            File dir = new File(path);
            if (dir.isDirectory()) {
                chooser.setInitialDirectory(dir);
            }
        }
    }

    private static void record(File file, String key) {
        if (file != null && file.getParentFile() != null) {
            PREFS.put(key, file.getParentFile().getAbsolutePath());
        }
    }
}
