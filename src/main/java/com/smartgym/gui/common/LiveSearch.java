package com.smartgym.gui.common;

import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * "Search as you type" for a text field. It waits a moment after the LAST key press before running
 * the search (so a query is not sent for every single letter), and it numbers the searches so a
 * slow old answer can never overwrite a newer one.
 *
 *   LiveSearch search = new LiveSearch(field, 300, this::load);
 *   ...
 *   int token = search.nextToken();      // at the start of load()
 *   ... later, in the result handler:
 *   if (!search.isCurrent(token)) return;
 */
public final class LiveSearch {

    private final Timer timer;
    private int token = 0;

    public LiveSearch(JTextField field, int delayMillis, final Runnable action) {
        timer = new Timer(delayMillis, e -> action.run());       // javax.swing.Timer runs on the Swing thread
        timer.setRepeats(false);
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                timer.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                timer.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                timer.restart();
            }
        });
    }

    /** Call at the start of every search; returns the number of this search. */
    public int nextToken() {
        return ++token;
    }

    /** True if no newer search has been started since this token was issued. */
    public boolean isCurrent(int t) {
        return t == token;
    }
}
