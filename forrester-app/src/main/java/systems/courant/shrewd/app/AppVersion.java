package systems.courant.forrester.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the application version from {@code forrester-version.properties},
 * which is populated by Maven resource filtering at build time.
 */
final class AppVersion {

    private static final Logger log = LoggerFactory.getLogger(AppVersion.class);

    private static final String VERSION;

    static {
        String v = "unknown";
        try (InputStream in = AppVersion.class.getResourceAsStream("/forrester-version.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                v = props.getProperty("version", "unknown");
            }
        } catch (IOException ex) {
            log.debug("Fall through with unknown version: {}", ex.getMessage(), ex);
        }
        VERSION = v;
    }

    private AppVersion() {}

    static String get() {
        return VERSION;
    }
}
