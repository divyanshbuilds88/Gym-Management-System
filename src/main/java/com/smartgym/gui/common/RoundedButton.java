package com.smartgym.gui.common;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A flat rounded button that paints itself.
 * Its hover / pressed effect is handled by the inner class HoverEffect
 * (a MouseAdapter: an ADAPTER class, so only the needed mouse methods are written).
 */
public class RoundedButton extends JButton {

    private static final long serialVersionUID = 1L;

    private final Color baseColor;
    private final boolean outline;
    private boolean hover = false;
    private boolean pressed = false;

    public RoundedButton(String text, Color baseColor) {
        this(text, baseColor, false);
    }

    private RoundedButton(String text, Color baseColor, boolean outline) {
        super(text);
        this.baseColor = baseColor;
        this.outline = outline;
        setFont(Theme.bold(13));
        setForeground(outline ? Theme.TEXT : Color.WHITE);
        setBorder(new EmptyBorder(9, 18, 9, 18));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new HoverEffect());
    }

    /** White button with a grey outline for less important actions. */
    public static RoundedButton secondary(String text) {
        return new RoundedButton(text, Color.WHITE, true);
    }

    public static RoundedButton withIcon(String text, Color color, String iconName) {
        RoundedButton b = new RoundedButton(text, color);
        Icon icon = IconFactory.get(iconName, 16, Color.WHITE);
        b.setIcon(icon);
        b.setIconTextGap(8);
        return b;
    }

    public static RoundedButton secondaryWithIcon(String text, String iconName) {
        RoundedButton b = secondary(text);
        b.setIcon(IconFactory.get(iconName, 16, Theme.TEXT));
        b.setIconTextGap(8);
        return b;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color fill = baseColor;
        if (!isEnabled()) {
            fill = outline ? new Color(0xF3F4F6) : new Color(0xD1D5DB);
        } else if (pressed) {
            fill = outline ? new Color(0xE5E7EB) : baseColor.darker().darker();
        } else if (hover) {
            fill = outline ? new Color(0xF3F4F6) : baseColor.darker();
        }
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
        if (outline) {
            g2.setColor(Theme.BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
        }
        g2.dispose();
        super.paintComponent(g);               // draws the text and icon
    }

    /** Inner class: it can use the private fields hover and pressed of its outer class. */
    private class HoverEffect extends MouseAdapter {
        @Override
        public void mouseEntered(MouseEvent e) {
            hover = true;
            repaint();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            hover = false;
            pressed = false;
            repaint();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            pressed = true;
            repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            pressed = false;
            repaint();
        }
    }
}
