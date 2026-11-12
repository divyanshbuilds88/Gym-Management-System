package com.smartgym.gui.common;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/** A panel with rounded corners, used for all "cards". */
public class RoundedPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final int arc;
    private Color fill;
    private Color borderColor;

    public RoundedPanel(LayoutManager layout, Color fill, int arc) {
        super(layout);
        this.fill = fill;
        this.arc = arc;
        this.borderColor = Theme.BORDER;
        setOpaque(false);                  // we paint the background ourselves
    }

    public RoundedPanel(LayoutManager layout) {
        this(layout, Theme.CARD, 16);
    }

    public void setBorderColor(Color c) {
        this.borderColor = c;
        repaint();
    }

    public void setFill(Color c) {
        this.fill = c;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
