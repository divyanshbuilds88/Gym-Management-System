package com.smartgym.gui.common;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;

/**
 * A panel for use inside a JScrollPane that always matches the window WIDTH
 * (only the height scrolls), so the layout adapts when the window is resized.
 */
public class ScrollPanel extends JPanel implements Scrollable {

    private static final long serialVersionUID = 1L;

    public ScrollPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 16; }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return 80; }

    @Override
    public boolean getScrollableTracksViewportWidth() { return true; }

    @Override
    public boolean getScrollableTracksViewportHeight() { return false; }
}
