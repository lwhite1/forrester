package systems.courant.sd.app;

/**
 * Non-Application launcher that avoids the "JavaFX runtime components are missing" error.
 * When the main class extends {@link javafx.application.Application}, the JVM checks for
 * JavaFX modules at startup. This indirection bypasses that check.
 */
public class Launcher {

    public static void main(String[] args) {
        CourantApp.main(args);
    }
}
