package com.smartgym.gui.common;

import com.smartgym.exception.UnauthorizedException;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.awt.Component;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Runs slow work (database calls) on a BACKGROUND thread with SwingWorker, then hands the
 * result back to the Event Dispatch Thread. This way the window never freezes.
 *
 *   BackgroundTask.run(this, () -> service.load(), data -> showData(data));
 *
 * doInBackground()  -> runs on a worker thread   (never touch Swing components here)
 * done()            -> runs on the EDT           (safe to update Swing components)
 */
public final class BackgroundTask {

    /** The work to do in the background; may throw any exception. */
    public interface Work<T> {
        T call() throws Exception;
    }

    private BackgroundTask() { }

    public static <T> void run(Component parent, Work<T> work, Consumer<T> onSuccess) {
        run(parent, work, onSuccess, null);
    }

    /** If onError is null, a message dialog is shown automatically. */
    public static <T> void run(final Component parent, final Work<T> work,
                               final Consumer<T> onSuccess, final Consumer<Exception> onError) {
        SwingWorker<T, Void> worker = new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();                                // worker thread
            }

            @Override
            protected void done() {                                // Event Dispatch Thread
                try {
                    onSuccess.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    Exception ex = (cause instanceof Exception) ? (Exception) cause : new Exception(cause);
                    if (onError != null) {
                        onError.accept(ex);
                    } else {
                        showError(parent, ex);
                    }
                }
            }
        };
        worker.execute();
    }

    /** Friendly dialog for any exception. Business errors show their own message. */
    public static void showError(Component parent, Exception ex) {
        boolean warning = ex instanceof UnauthorizedException;
        String message = (ex instanceof com.smartgym.exception.SmartGymException)
                ? ex.getMessage()
                : "Unexpected error: " + ex.getMessage();
        JOptionPane.showMessageDialog(parent, message, "Smart Gym",
                warning ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Smart Gym", JOptionPane.INFORMATION_MESSAGE);
    }
}
