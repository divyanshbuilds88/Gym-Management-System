package com.smartgym.gui.common;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A slim rounded progress bar (0.0 to 1.0) drawn with Java2D. */
public class ThinProgressBar extends JComponent {

    private static final long serialVersionUID = 1L;

    private double fraction = 0;
    private Color color = Theme.ACCENT;

    public ThinProgressBar() {
        setPreferredSize(new Dimension(200, 12));
        setMinimumSize(new Dimension(50, 12));            // never let a layout squash it to a hairline
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));
    }

    public void setProgress(double fraction, Color color) {
        this.fraction = Math.max(0, Math.min(1, fraction));
        this.color = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int h = getHeight();
        g2.setColor(new Color(0xE5E7EB));
        g2.fillRoundRect(0, 0, getWidth(), h, h, h);
        g2.setColor(color);
        g2.fillRoundRect(0, 0, (int) (getWidth() * fraction), h, h, h);
        g2.dispose();
    }
}
