package com.smartgym.gui.common;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Small helpers that create styled Swing components, so the screens stay short and consistent. */
public final class UiFactory {

    /** The date format used everywhere on screen, for example 8 Nov 2026. */
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private UiFactory() { }

    // ---------------------------------------------------------------- labels

    public static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(bold ? Theme.bold(size) : Theme.regular(size));
        l.setForeground(color);
        return l;
    }

    public static JLabel title(String text) {
        return label(text, 24, true, Theme.TEXT);
    }

    public static JLabel muted(String text) {
        return label(text, 13, false, Theme.MUTED);
    }

    public static JLabel fieldLabel(String text) {
        return label(text, 12, true, Theme.MUTED);
    }

    // ---------------------------------------------------------------- text fields

    private static Border fieldBorder(Color line) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(line, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10));
    }

    /** Border turns orange while the field has the keyboard focus (FocusAdapter). */
    private static void highlightOnFocus(final JTextField field) {
        field.setBorder(fieldBorder(Theme.BORDER));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(fieldBorder(Theme.ACCENT));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(fieldBorder(Theme.BORDER));
            }
        });
    }

    public static JTextField textField(int columns) {
        JTextField f = new JTextField(columns);
        f.setFont(Theme.regular(14));
        f.setForeground(Theme.TEXT);
        f.setCaretColor(Theme.ACCENT);
        highlightOnFocus(f);
        return f;
    }

    public static JPasswordField passwordField(int columns) {
        JPasswordField f = new JPasswordField(columns);
        f.setFont(Theme.regular(14));
        f.setForeground(Theme.TEXT);
        f.setCaretColor(Theme.ACCENT);
        f.setEchoChar('\u2022');
        highlightOnFocus(f);
        return f;
    }

    /** A text field that shows grey hint text while it is empty (like a browser search box). */
    public static JTextField searchField(final String hint, int columns) {
        JTextField f = new JTextField(columns) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(new Color(0x9CA3AF));
                    g2.setFont(Theme.regular(14));
                    g2.drawString(hint, getInsets().left + 2, (getHeight() + g2.getFontMetrics().getAscent()
                            - g2.getFontMetrics().getDescent()) / 2);
                    g2.dispose();
                }
            }
        };
        f.setFont(Theme.regular(14));
        f.setForeground(Theme.TEXT);
        f.setCaretColor(Theme.ACCENT);
        highlightOnFocus(f);
        return f;
    }

    public static void styleCombo(JComboBox<?> combo) {
        combo.setFont(Theme.regular(14));
        combo.setBackground(Color.WHITE);
        combo.setPreferredSize(new Dimension(150, 38));
    }

    public static void styleSpinner(JSpinner spinner) {
        spinner.setFont(Theme.regular(14));
        spinner.setPreferredSize(new Dimension(100, 38));
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor) {
            JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
            editor.getTextField().setHorizontalAlignment(JTextField.LEFT);
            editor.getTextField().setFont(Theme.regular(14));
        }
        spinner.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
    }

    /**
     * Creates a JTable that stretches to fill a wide window but, when the window is too narrow for
     * the column widths, scrolls sideways instead of squeezing and cutting off the text.
     */
    public static JTable createTable(javax.swing.table.TableModel model) {
        return new JTable(model) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean getScrollableTracksViewportWidth() {
                return getParent() == null || getPreferredSize().width < getParent().getWidth();
            }
        };
    }

    /** Sets the starting width of each table column. */
    public static void setColumnWidths(JTable table, int... widths) {
        for (int i = 0; i < widths.length && i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    // ---------------------------------------------------------------- tables

    /** A table model whose cells cannot be edited by typing in the table. */
    public static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /** Gives a JTable the modern look: no grid, zebra rows, bold header. */
    public static void styleTable(JTable table) {
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFont(Theme.regular(13));
        table.setForeground(Theme.TEXT);
        table.setSelectionBackground(new Color(0xFFEDD5));
        table.setSelectionForeground(Theme.TEXT);
        table.setFillsViewportHeight(true);
        ZebraRenderer zebra = new ZebraRenderer();
        table.setDefaultRenderer(Object.class, zebra);
        table.setDefaultRenderer(Number.class, zebra);
        table.setDefaultRenderer(Double.class, zebra);          // JTable has its own Double renderer, so replace it too
        table.setDefaultRenderer(java.time.LocalDate.class, zebra);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(100, 34));
        header.setDefaultRenderer(new HeaderRenderer());
    }

    public static JScrollPane scroll(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        sp.getViewport().setBackground(Color.WHITE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    private static class HeaderRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setFont(Theme.font(Font.BOLD, 12));
            setForeground(Theme.MUTED);
            setBackground(new Color(0xF3F4F6));
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                    BorderFactory.createEmptyBorder(0, 10, 0, 10)));
            return this;
        }
    }
}
