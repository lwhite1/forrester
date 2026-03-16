package systems.courant.sd.app.canvas;

import javafx.application.Platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Runs analysis tasks (simulation, sweep, Monte Carlo, optimization, validation)
 * on a shared background thread pool and delivers results on the JavaFX thread.
 */
public class AnalysisRunner {

    private static final Logger log = LoggerFactory.getLogger(AnalysisRunner.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "analysis-runner");
        t.setDaemon(true);
        return t;
    });

    private final StatusBar statusBar;
    private final BiConsumer<String, String> errorHandler;
    private volatile Future<?> currentTask;
    private volatile boolean shutdownRequested;

    public AnalysisRunner(StatusBar statusBar,
                          BiConsumer<String, String> errorHandler) {
        this.statusBar = statusBar;
        this.errorHandler = errorHandler;
    }

    /**
     * Runs a background task with progress indication.
     *
     * @param progressMessage message to show in the status bar while running
     * @param task            the computation to run off the FX thread
     * @param onSuccess       callback on FX thread with the result
     * @param errorTitle      title for error dialogs if the task fails
     * @param <T>             result type
     */
    public <T> void run(String progressMessage, Callable<T> task,
                        Consumer<T> onSuccess, String errorTitle) {
        cancelCurrentTask();
        statusBar.showProgress(progressMessage);
        currentTask = executor.submit(() -> {
            try {
                T result = task.call();
                if (!shutdownRequested) {
                    Platform.runLater(() -> {
                        statusBar.clearProgress();
                        onSuccess.accept(result);
                    });
                }
            } catch (Exception e) { // Callable.call() declares checked Exception
                log.error("{}: {}", errorTitle, e.toString(), e);
                if (!shutdownRequested) {
                    Platform.runLater(() -> {
                        statusBar.clearProgress();
                        errorHandler.accept(errorTitle,
                                e.getMessage() != null ? e.getMessage() : e.toString());
                    });
                }
            }
        });
    }

    /**
     * Runs a background task without progress indication (e.g. validation).
     */
    public <T> void run(Callable<T> task, Consumer<T> onSuccess, String errorTitle) {
        cancelCurrentTask();
        currentTask = executor.submit(() -> {
            try {
                T result = task.call();
                if (!shutdownRequested) {
                    Platform.runLater(() -> onSuccess.accept(result));
                }
            } catch (Exception e) { // Callable.call() declares checked Exception
                log.error("{}: {}", errorTitle, e.toString(), e);
                if (!shutdownRequested) {
                    Platform.runLater(() -> errorHandler.accept(errorTitle,
                            e.getMessage() != null ? e.getMessage() : e.toString()));
                }
            }
        });
    }

    private void cancelCurrentTask() {
        Future<?> prev = currentTask;
        if (prev != null && !prev.isDone()) {
            prev.cancel(true);
        }
    }

    /**
     * Shuts down the thread pool. Call when the window closes.
     */
    public void shutdown() {
        shutdownRequested = true;
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                log.warn("Analysis executor did not terminate within 2 seconds");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
