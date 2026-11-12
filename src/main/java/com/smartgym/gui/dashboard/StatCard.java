package com.smartgym.gui.dashboard;

import com.smartgym.gui.common.IconFactory;
import com.smartgym.gui.common.RoundedPanel;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** One number on the dashboard, e.g. "Total Members  42". */
public class StatCard extends RoundedPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel valueLabel = UiFactory.label("...", 30, true, Theme.TEXT);

    public StatCard(String title, Color accent, String iconName) {
        super(new BorderLayout(10, 0));
        setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JLabel titleLabel = UiFactory.label(title, 13, false, Theme.MUTED);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(titleLabel);
        text.add(javax.swing.Box.createVerticalStrut(6));
        text.add(valueLabel);
        add(text, BorderLayout.CENTER);

        add(new JLabel(badge(iconName, accent)), BorderLayout.EAST);
    }

    public void setValue(String text) {
        valueLabel.setText(text);
    }

    /** A tinted circle with a white-ish icon in the middle. */
    private static Icon badge(final String iconName, final Color accent) {
        final int size = 46;
        final Icon inner = IconFactory.get(iconName, 22, accent);
        return new Icon() {
            @Override
            public int getIconWidth() { return size; }

            @Override
            public int getIconHeight() { return size; }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 34));
                g2.fillOval(x, y, size, size);
                g2.dispose();
                inner.paintIcon(c, g, x + (size - 22) / 2, y + (size - 22) / 2);
            }
        };
    }
}
