package com.smartgym.gui.members;

import com.smartgym.model.Member;

import javax.swing.table.AbstractTableModel;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The data behind the members table. The table asks this model for the number of rows and
 * for the value of each cell (the MVC pattern: Model = this class, View = JTable).
 * Because getColumnClass() says which cells hold numbers and dates, clicking a column header sorts correctly.
 */
public class MemberTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] COLUMNS = {"Code", "Name", "Age", "Gender", "Phone", "Email", "Plan", "Expires", "Status"};

    private transient List<Member> members = new ArrayList<>();

    public void setMembers(List<Member> newMembers) {
        this.members = new ArrayList<>(newMembers);
        fireTableDataChanged();                                  // tell the table to redraw
    }

    public Member getMember(int modelRow) {
        return members.get(modelRow);
    }

    @Override
    public int getRowCount() {
        return members.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        switch (column) {
            case 2:
                return Integer.class;
            case 7:
                return LocalDate.class;
            default:
                return String.class;
        }
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    @Override
    public Object getValueAt(int row, int column) {
        Member m = members.get(row);
        switch (column) {
            case 0:
                return m.getMemberCode();
            case 1:
                return m.getFullName();
            case 2:
                return m.getAge();
            case 3:
                return m.getGender();
            case 4:
                return m.getPhone();
            case 5:
                return m.getEmail();
            case 6:
                return m.getPlanName();
            case 7:
                return m.getMembershipExpiry();
            default:
                return m.getMembershipStatus();
        }
    }
}
