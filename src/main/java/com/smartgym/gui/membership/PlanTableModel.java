package com.smartgym.gui.membership;

import com.smartgym.model.MembershipPlan;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/** Table data for membership plans. */
public class PlanTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private final boolean showActive;
    private final String[] columns;
    private transient List<MembershipPlan> rows = new ArrayList<>();

    public PlanTableModel(boolean showActive) {
        this.showActive = showActive;
        this.columns = showActive
                ? new String[] {"Plan", "Months", "Fee (Rs.)", "Description", "Offered"}
                : new String[] {"Plan", "Months", "Fee (Rs.)", "Description"};
    }

    public void setPlans(List<MembershipPlan> list) {
        this.rows = new ArrayList<>(list);
        fireTableDataChanged();
    }

    public MembershipPlan getPlan(int modelRow) {
        return rows.get(modelRow);
    }

    @Override
    public int getRowCount() { return rows.size(); }

    @Override
    public int getColumnCount() { return columns.length; }

    @Override
    public String getColumnName(int column) { return columns[column]; }

    @Override
    public Class<?> getColumnClass(int column) {
        switch (column) {
            case 1:
                return Integer.class;
            case 2:
                return Double.class;
            default:
                return String.class;
        }
    }

    @Override
    public Object getValueAt(int row, int column) {
        MembershipPlan p = rows.get(row);
        switch (column) {
            case 0:
                return p.getPlanName();
            case 1:
                return p.getDurationMonths();
            case 2:
                return p.getFee();
            case 3:
                return p.getDescription();
            default:
                return showActive ? (p.isActive() ? "Yes" : "No") : "";
        }
    }
}
