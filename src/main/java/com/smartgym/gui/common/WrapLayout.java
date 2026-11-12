package com.smartgym.gui.common;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/**
 * A FlowLayout that WRAPS onto a new row when the window is too narrow (a normal FlowLayout would
 * just cut the buttons off). Used for toolbars so the screens stay usable on small windows.
 * It extends FlowLayout and overrides only how the preferred size is calculated.
 */
public class WrapLayout extends FlowLayout {

    private static final long serialVersionUID = 1L;

    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension min = layoutSize(target, false);
        min.width -= getHgap() + 1;
        return min;
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            // use the width of the nearest ancestor that already has a size
            Container container = target;
            while (container.getSize().width == 0 && container.getParent() != null) {
                container = container.getParent();
            }
            int targetWidth = container.getSize().width;
            if (targetWidth == 0) {
                targetWidth = Integer.MAX_VALUE;
            }

            int hgap = getHgap();
            int vgap = getVgap();
            Insets insets = target.getInsets();
            int insetsAndGaps = insets.left + insets.right + hgap * 2;
            int maxWidth = targetWidth - insetsAndGaps;

            Dimension size = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;
            for (int i = 0; i < target.getComponentCount(); i++) {
                Component c = target.getComponent(i);
                if (!c.isVisible()) {
                    continue;
                }
                Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
                if (rowWidth + d.width > maxWidth && rowWidth > 0) {      // start a new row
                    addRow(size, rowWidth, rowHeight);
                    rowWidth = 0;
                    rowHeight = 0;
                }
                if (rowWidth != 0) {
                    rowWidth += hgap;
                }
                rowWidth += d.width;
                rowHeight = Math.max(rowHeight, d.height);
            }
            addRow(size, rowWidth, rowHeight);

            size.width += insetsAndGaps;
            size.height += insets.top + insets.bottom + vgap * 2;
            if (SwingUtilities.getAncestorOfClass(JScrollPane.class, target) != null && target.isValid()) {
                size.width -= hgap + 1;
            }
            return size;
        }
    }

    private void addRow(Dimension size, int rowWidth, int rowHeight) {
        size.width = Math.max(size.width, rowWidth);
        if (size.height > 0) {
            size.height += getVgap();
        }
        size.height += rowHeight;
    }
}
