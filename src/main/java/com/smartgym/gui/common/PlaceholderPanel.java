package com.smartgym.gui.common;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;

/**
 * TEMPORARY screen for modules that are built in a later phase.
 * Each later phase replaces one placeholder with the real panel (one line in MainFrame).
 */
public class PlaceholderPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    public PlaceholderPanel(String moduleName, String phase, String description) {
        super(new GridBagLayout());
        setBackground(Theme.BACKGROUND);

        RoundedPanel card = new RoundedPanel(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(36, 48, 36, 48));

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel icon = new JLabel(IconFactory.get("workout", 44, Theme.ACCENT));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = UiFactory.label(moduleName, 22, true, Theme.TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel badge = UiFactory.label("Coming in " + phase, 13, true, Theme.ACCENT_DARK);
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel desc = UiFactory.muted("<html><div style='text-align:center;width:340px'>" + description + "</div></html>");
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);

        box.add(icon);
        box.add(Box.createVerticalStrut(14));
        box.add(title);
        box.add(Box.createVerticalStrut(6));
        box.add(badge);
        box.add(Box.createVerticalStrut(14));
        box.add(desc);
        card.add(box, BorderLayout.CENTER);
        add(card);
    }
}
