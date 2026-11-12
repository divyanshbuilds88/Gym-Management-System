package com.smartgym.gui.common;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.time.LocalDate;

/** Table cell renderer with alternate row colours. Also formats dates and shows "-" for empty cells. */
public class ZebraRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
        setHorizontalAlignment(LEFT);
        setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        if (value == null) {
            setText("-");
        } else if (value instanceof LocalDate) {
            setText(((LocalDate) value).format(UiFactory.DATE_FORMAT));
        } else if (value instanceof Double) {
            setText(String.format("%,.2f", (Double) value));          // money amounts
        }
        if (!isSelected) {
            setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF9FAFB));
            setForeground(Theme.TEXT);
        }
        return this;
    }
}
