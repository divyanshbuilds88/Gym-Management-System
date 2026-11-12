package com.smartgym.gui.common;

import com.smartgym.exception.SmartGymException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

/**
 * ABSTRACT base class for every "Add / Edit" pop-up window.
 * It supplies the title, the error line, the Save and Cancel buttons and the background saving.
 * A subclass only has to say what the form looks like (buildForm) and what Save does (onSave).
 *
 * Inheritance + abstract methods + polymorphism: the base class calls buildForm() and onSave(),
 * and Java runs the version written in the subclass.
 */
public abstract class FormDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final String subtitle;
    private final String saveText;
    private final JLabel errorLabel = UiFactory.label(" ", 13, true, Theme.DANGER);
    private final RoundedButton saveButton;
    private final RoundedButton cancelButton = RoundedButton.secondary("Cancel");

    protected FormDialog(Window owner, String title, String subtitle, String saveText) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.subtitle = subtitle;
        this.saveText = saveText;
        this.saveButton = new RoundedButton(saveText, Theme.ACCENT);
    }

    /** The form fields (created by the subclass). */
    protected abstract JComponent buildForm();

    /** Called when the user presses Save. Usually validates and calls runSave(...). */
    protected abstract void onSave();

    /**
     * A subclass calls build() at the END of its own constructor, because only then are the
     * subclass's fields created (a base-class constructor runs before the subclass fields exist).
     */
    protected final void build() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        JPanel header = new JPanel(new BorderLayout(0, 2));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(20, 26, 4, 26));
        header.add(UiFactory.label(getTitle(), 20, true, Theme.TEXT), BorderLayout.NORTH);
        header.add(UiFactory.muted(subtitle), BorderLayout.CENTER);
        root.add(header, BorderLayout.NORTH);

        JComponent form = buildForm();
        form.setBorder(BorderFactory.createEmptyBorder(0, 26, 8, 26));
        clearErrorWhenEdited(form);
        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(0, 6));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(8, 26, 14, 26)));
        errorLabel.setPreferredSize(new Dimension(10, 40));          // room for two lines of text
        errorLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        footer.add(errorLabel, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancelButton);
        buttons.add(saveButton);
        footer.add(buttons, BorderLayout.SOUTH);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);

        saveButton.setName("save");
        cancelButton.setName("cancel");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                errorLabel.setText(" ");
                onSave();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        getRootPane().setDefaultButton(saveButton);                    // Enter = Save

        // Esc closes the window (key binding)
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        getRootPane().getActionMap().put("close", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.max(getWidth(), 520);
        int height = Math.min(getHeight(), (int) (screen.height * 0.9));
        setSize(width, height);
        setLocationRelativeTo(getOwner());
    }

    /** Walks through all components; every text field / text area clears the error message when it changes. */
    private void clearErrorWhenEdited(Component c) {
        if (c instanceof JTextComponent) {
            ((JTextComponent) c).getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    clearError();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    clearError();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    clearError();
                }
            });
        }
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                clearErrorWhenEdited(child);
            }
        }
    }

    protected void clearError() {
        errorLabel.setText(" ");
    }

    /** Shows a red message at the bottom of the dialog (the dialog stays open). */
    protected void showError(String message) {
        errorLabel.setText("<html><body style='width:430px'>" + message + "</body></html>");
    }

    /**
     * Runs 'work' on a background thread. On success the Save button is re-enabled and
     * onSuccess is called; on failure the error text is shown inside the dialog.
     */
    protected <T> void runSave(BackgroundTask.Work<T> work, Consumer<T> onSuccess) {
        setBusy(true);
        BackgroundTask.run(this, work, result -> {
            setBusy(false);
            onSuccess.accept(result);
        }, ex -> {
            setBusy(false);
            showError(ex instanceof SmartGymException ? ex.getMessage() : "Unexpected error: " + ex.getMessage());
        });
    }

    protected void setBusy(boolean busy) {
        saveButton.setEnabled(!busy);
        saveButton.setText(busy ? "Saving..." : saveText);
    }
}
