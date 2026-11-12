package com.smartgym.gui.common;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** One menu entry in the sidebar. Highlights on mouse hover and when selected. */
public class NavButton extends JButton {

    private static final long serialVersionUID = 1L;

    private final String iconName;
    private boolean selected = false;
    private boolean hover = false;

    public NavButton(String text, String iconName) {
        super(text);
        this.iconName = iconName;
        setHorizontalAlignment(SwingConstants.LEFT);
        setFont(Theme.font(java.awt.Font.PLAIN, 14));
        setBorder(new EmptyBorder(11, 20, 11, 12));
        setIconTextGap(14);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        setAlignmentX(LEFT_ALIGNMENT);
        applyColors();
        addMouseListener(new MouseAdapter() {                     // anonymous inner class
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        });
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        applyColors();
        repaint();
    }

    private void applyColors() {
        Color c = selected ? Color.WHITE : Theme.SIDEBAR_TEXT;
        setForeground(c);
        Icon icon = IconFactory.get(iconName, 18, selected ? Theme.ACCENT : Theme.SIDEBAR_TEXT);
        setIcon(icon);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        if (selected) {
            g2.setColor(Theme.SIDEBAR_ACTIVE);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Theme.ACCENT);
            g2.fillRect(0, 0, 4, getHeight());
        } else if (hover) {
            g2.setColor(Theme.SIDEBAR_HOVER);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
