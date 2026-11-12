package com.smartgym.gui.common;

import javax.swing.BorderFactory;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A simple bar chart drawn with Java2D (no chart library needed).
 * Moving the mouse over a bar shows its exact value (MouseMotion event).
 */
public class BarChartPanel extends RoundedPanel {

    private static final long serialVersionUID = 1L;

    private static final int LEFT = 54;
    private static final int RIGHT = 18;
    private static final int TOP = 52;
    private static final int BOTTOM = 34;

    private final String title;
    private final Color barColor;
    private final boolean currency;
    private transient Map<String, Double> data = new LinkedHashMap<>();
    private int hoverIndex = -1;

    public BarChartPanel(String title, Color barColor, boolean currency) {
        super(null);
        this.title = title;
        this.barColor = barColor;
        this.currency = currency;
        setBorder(BorderFactory.createEmptyBorder());
        setPreferredSize(new java.awt.Dimension(320, 260));

        addMouseMotionListener(new MouseAdapter() {               // MouseAdapter also implements MouseMotionListener
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = barAt(e.getX());
                if (index != hoverIndex) {
                    hoverIndex = index;
                    repaint();
                }
                setToolTipText(index < 0 ? null : tooltipFor(index));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverIndex = -1;
                repaint();
            }
        });
    }

    public void setData(Map<String, Double> newData) {
        this.data = new LinkedHashMap<>(newData);
        this.hoverIndex = -1;
        repaint();
    }

    private double maxValue() {
        double max = 0;
        for (double v : data.values()) {
            max = Math.max(max, v);
        }
        if (currency) {
            return niceCeiling(max);
        }
        return Math.max(4, Math.ceil(max / 4.0) * 4);      // counts: whole-number ticks (0, 1, 2, 3, 4 ...)
    }

    /** Rounds a number up to 1, 2, 5 or 10 times a power of ten (for a tidy y-axis). */
    private static double niceCeiling(double max) {
        if (max <= 0) {
            return 1;
        }
        double magnitude = Math.pow(10, Math.floor(Math.log10(max)));
        double n = max / magnitude;
        double nice = (n <= 1) ? 1 : (n <= 2) ? 2 : (n <= 5) ? 5 : 10;
        return nice * magnitude;
    }

    private String format(double value) {
        if (currency) {
            return value >= 1000 ? String.format("%.1fk", value / 1000) : String.format("%.0f", value);
        }
        return String.format("%.0f", value);
    }

    private String tooltipFor(int index) {
        int i = 0;
        for (Map.Entry<String, Double> e : data.entrySet()) {
            if (i++ == index) {
                return currency ? e.getKey() + ": Rs. " + String.format("%,.2f", e.getValue())
                                : e.getKey() + ": " + String.format("%.0f", e.getValue());
            }
        }
        return null;
    }

    private int barAt(int mouseX) {
        if (data.isEmpty()) {
            return -1;
        }
        double slot = (double) (getWidth() - LEFT - RIGHT) / data.size();
        int index = (int) ((mouseX - LEFT) / slot);
        return (mouseX >= LEFT && index >= 0 && index < data.size()) ? index : -1;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setFont(Theme.bold(15));
        g2.setColor(Theme.TEXT);
        g2.drawString(title, 20, 30);

        int chartW = getWidth() - LEFT - RIGHT;
        int chartH = getHeight() - TOP - BOTTOM;
        if (chartW < 40 || chartH < 40) {
            g2.dispose();
            return;
        }

        if (data.isEmpty()) {
            g2.setFont(Theme.regular(13));
            g2.setColor(Theme.MUTED);
            String msg = "No data yet";
            g2.drawString(msg, LEFT + (chartW - g2.getFontMetrics().stringWidth(msg)) / 2, TOP + chartH / 2);
            g2.dispose();
            return;
        }

        double max = maxValue();

        // horizontal grid lines + y labels
        g2.setFont(Theme.regular(11));
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i <= 4; i++) {
            int y = TOP + chartH - (int) (chartH * i / 4.0);
            g2.setColor(new Color(0xEEF0F3));
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(LEFT, y, LEFT + chartW, y);
            String label = format(max * i / 4.0);
            g2.setColor(Theme.MUTED);
            g2.drawString(label, LEFT - 8 - fm.stringWidth(label), y + 4);
        }

        // bars
        double slot = (double) chartW / data.size();
        int barW = (int) Math.min(46, slot * 0.55);
        int index = 0;
        for (Map.Entry<String, Double> e : data.entrySet()) {
            double v = e.getValue();
            int h = (int) Math.round(v / max * chartH);
            int x = LEFT + (int) (slot * index + (slot - barW) / 2);
            int y = TOP + chartH - h;

            g2.setColor(index == hoverIndex ? barColor.darker() : barColor);
            if (h > 0) {
                g2.fill(new RoundRectangle2D.Float(x, y, barW, h, 10, 10));
                if (h > 10) {
                    g2.fillRect(x, y + h / 2, barW, h - h / 2);       // square off the bottom corners
                }
            }

            g2.setFont(Theme.bold(11));
            g2.setColor(Theme.TEXT);
            String val = format(v);
            g2.drawString(val, x + (barW - g2.getFontMetrics().stringWidth(val)) / 2, y - 6);

            g2.setFont(Theme.regular(11));
            g2.setColor(Theme.MUTED);
            String lbl = e.getKey();
            g2.drawString(lbl, LEFT + (int) (slot * index + (slot - g2.getFontMetrics().stringWidth(lbl)) / 2),
                    TOP + chartH + 20);
            index++;
        }
        g2.dispose();
    }
}
