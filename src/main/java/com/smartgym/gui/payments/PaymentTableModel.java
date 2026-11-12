package com.smartgym.gui.payments;

import com.smartgym.model.Payment;

import javax.swing.table.AbstractTableModel;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Table data for payments. The member columns can be left out (a member's own view). */
public class PaymentTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final String[] ALL_NAMES = {"Receipt", "Date", "Code", "Member", "Amount (Rs.)", "Method", "Status", "Notes"};

    private final int[] columns;
    private transient List<Payment> rows = new ArrayList<>();

    public PaymentTableModel(boolean showMember) {
        columns = showMember ? new int[] {0, 1, 2, 3, 4, 5, 6, 7} : new int[] {0, 1, 4, 5, 6, 7};
    }

    public void setPayments(List<Payment> list) {
        this.rows = new ArrayList<>(list);
        fireTableDataChanged();
    }

    public Payment getPayment(int modelRow) {
        return rows.get(modelRow);
    }

    /** Position of a "full" column number (0..7) among the visible columns, or -1. */
    public int columnOf(int fullIndex) {
        for (int i = 0; i < columns.length; i++) {
            if (columns[i] == fullIndex) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getRowCount() { return rows.size(); }

    @Override
    public int getColumnCount() { return columns.length; }

    @Override
    public String getColumnName(int column) { return ALL_NAMES[columns[column]]; }

    @Override
    public Class<?> getColumnClass(int column) {
        switch (columns[column]) {
            case 1:
                return LocalDate.class;
            case 4:
                return Double.class;
            default:
                return String.class;
        }
    }

    @Override
    public Object getValueAt(int row, int column) {
        Payment p = rows.get(row);
        switch (columns[column]) {
            case 0:
                return p.getReceiptNo();
            case 1:
                return p.getPaymentDate();
            case 2:
                return p.getMemberCode();
            case 3:
                return p.getMemberName();
            case 4:
                return p.getAmount();
            case 5:
                return p.getMethod();
            case 6:
                return p.getStatus();
            default:
                return p.getNotes();
        }
    }
}
