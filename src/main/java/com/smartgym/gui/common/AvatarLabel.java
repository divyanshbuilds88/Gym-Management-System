package com.smartgym.gui.common;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A round badge with the person's initials. */
public class AvatarLabel extends JComponent {

    private static final long serialVersionUID = 1L;

    private final int size;
    private final Color color;
    private String initials = "?";

    public AvatarLabel(String fullName, int size, Color color) {
        this.size = size;
        this.color = color;
        setPreferredSize(new Dimension(size, size));
        setMinimumSize(new Dimension(size, size));
        setMaximumSize(new Dimension(size, size));
        setName(fullName);
    }

    public void setPerson(String fullName) {
        StringBuilder sb = new StringBuilder();
        if (fullName != null) {
            for (String part : fullName.trim().split("\\s+")) {
                if (!part.isEmpty() && sb.length() < 2) {
                    sb.append(Character.toUpperCase(part.charAt(0)));
                }
            }
        }
        initials = sb.length() == 0 ? "?" : sb.toString();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(color);
        g2.fillOval(0, 0, size - 1, size - 1);
        g2.setColor(Color.WHITE);
        g2.setFont(Theme.bold(Math.max(11, size * 38 / 100)));
        int w = g2.getFontMetrics().stringWidth(initials);
        int y = (size + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2;
        g2.drawString(initials, (size - w) / 2, y);
        g2.dispose();
    }
}
