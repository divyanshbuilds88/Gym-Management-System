package com.smartgym.gui.common;

import javax.swing.JTable;
import java.awt.Color;
import java.awt.Component;

/** Shows a status word in colour: ACTIVE green, EXPIRED red, PENDING amber and so on. */
public class StatusCellRenderer extends ZebraRenderer {

    private static final long serialVersionUID = 1L;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String status = value == null ? "" : value.toString();
        setText("\u25CF  " + (status.equals("NONE") ? "NO PLAN" : status));
        setFont(Theme.bold(12));
        setForeground(colorFor(status));               // keep the colour even on a selected row
        return this;
    }

    public static Color colorFor(String status) {
        switch (status) {
            case "ACTIVE":
            case "PAID":
            case "COMPLETED":
                return Theme.SUCCESS;
            case "EXPIRED":
            case "FAILED":
                return Theme.DANGER;
            case "PENDING":
                return Theme.AMBER;
            case "SCHEDULED":
            case "INSIDE":
                return Theme.INFO;
            default:
                return Theme.MUTED;                          // CANCELLED, NONE ...
        }
    }
}
