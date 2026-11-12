package com.smartgym.gui.common;

import javax.swing.UIManager;
import java.awt.Insets;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/** One place for colours and fonts, so every screen looks the same. */
public final class Theme {

    public static final Color SIDEBAR        = new Color(0x111827);
    public static final Color SIDEBAR_HOVER  = new Color(0x1F2937);
    public static final Color SIDEBAR_ACTIVE = new Color(0x273449);
    public static final Color SIDEBAR_TEXT   = new Color(0x9CA3AF);

    public static final Color ACCENT      = new Color(0xF97316);   // gym orange
    public static final Color ACCENT_DARK = new Color(0xEA580C);

    public static final Color BACKGROUND = new Color(0xF3F4F6);
    public static final Color CARD       = Color.WHITE;
    public static final Color BORDER     = new Color(0xE5E7EB);
    public static final Color TEXT       = new Color(0x111827);
    public static final Color MUTED      = new Color(0x6B7280);

    public static final Color SUCCESS = new Color(0x16A34A);
    public static final Color DANGER  = new Color(0xDC2626);
    public static final Color INFO    = new Color(0x2563EB);
    public static final Color PURPLE  = new Color(0x7C3AED);
    public static final Color TEAL    = new Color(0x0D9488);
    public static final Color AMBER   = new Color(0xD97706);

    private static final String FONT_FAMILY = "Segoe UI";   // Java falls back to another font if missing

    private Theme() { }

    public static Font font(int style, int size) {
        return new Font(FONT_FAMILY, style, size);
    }

    public static Font regular(int size) { return font(Font.PLAIN, size); }

    public static Font bold(int size) { return font(Font.BOLD, size); }

    /** Sets the default fonts of dialogs and tooltips. */
    public static void applyGlobalDefaults() {
        UIManager.put("OptionPane.messageFont", regular(14));
        UIManager.put("OptionPane.buttonFont", regular(13));
        UIManager.put("ToolTip.font", regular(12));
        UIManager.put("Label.font", regular(13));
        UIManager.put("TabbedPane.font", bold(13));
        UIManager.put("CheckBox.font", regular(13));
        UIManager.put("RadioButton.font", regular(13));
        UIManager.put("TabbedPane.tabInsets", new Insets(9, 22, 9, 22));
    }

    /** The small icon shown in the title bar and task bar (drawn with Java2D, no image file needed). */
    public static BufferedImage appIcon() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(ACCENT);
        g.fill(new RoundRectangle2D.Float(2, 2, 60, 60, 16, 16));
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(20, 32, 44, 32);
        g.fillRoundRect(12, 20, 8, 24, 4, 4);
        g.fillRoundRect(44, 20, 8, 24, 4, 4);
        g.dispose();
        return img;
    }
}
