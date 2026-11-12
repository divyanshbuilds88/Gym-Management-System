package com.smartgym.gui.membership;

import com.smartgym.model.Membership;
import com.smartgym.util.Constants;

import javax.swing.table.AbstractTableModel;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Table data for memberships. With showMember = false the member columns are left out (member's own view). */
public class MembershipTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private final boolean showMember;
    private final String[] columns;
    private transient List<Membership> rows = new ArrayList<>();

    public MembershipTableModel(boolean showMember) {
        this.showMember = showMember;
        this.columns = showMember
                ? new String[] {"Code", "Member", "Plan", "Start", "Expiry", "Status", "Days left"}
                : new String[] {"Plan", "Start", "Expiry", "Status", "Days left"};
    }

    public void setMemberships(List<Membership> list) {
        this.rows = new ArrayList<>(list);
        fireTableDataChanged();
    }

    public Membership getMembership(int modelRow) {
        return rows.get(modelRow);
    }

    /** Index of the Status column (needed to attach the coloured renderer). */
    public int statusColumn() {
        return showMember ? 5 : 3;
    }

    @Override
    public int getRowCount() { return rows.size(); }

    @Override
    public int getColumnCount() { return columns.length; }

    @Override
    public String getColumnName(int column) { return columns[column]; }

    /** Converts a visible column number into the position in the full 7-column layout. */
    private int full(int column) {
        return showMember ? column : column + 2;
    }

    @Override
    public Class<?> getColumnClass(int column) {
        switch (full(column)) {
            case 3:
            case 4:
                return LocalDate.class;
            case 6:
                return Long.class;
            default:
                return String.class;
        }
    }

    @Override
    public Object getValueAt(int row, int column) {
        Membership m = rows.get(row);
        switch (full(column)) {
            case 0:
                return m.getMemberCode();
            case 1:
                return m.getMemberName();
            case 2:
                return m.getPlanName();
            case 3:
                return m.getStartDate();
            case 4:
                return m.getExpiryDate();
            case 5:
                return m.getStatus();
            default:
                return Constants.ACTIVE.equals(m.getStatus()) ? Long.valueOf(m.getDaysLeft()) : null;
        }
    }
}
