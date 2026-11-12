package com.smartgym.gui.workout;

import com.smartgym.model.MemberWorkout;

import javax.swing.table.AbstractTableModel;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Table data for "workout plan assigned to member". With showMember = false the member column is left out. */
public class AssignmentTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private final boolean showMember;
    private final String[] columns;
    private transient List<MemberWorkout> rows = new ArrayList<>();

    public AssignmentTableModel(boolean showMember) {
        this.showMember = showMember;
        this.columns = showMember
                ? new String[] {"Member", "Workout plan", "Trainer", "Assigned", "Ends", "Status"}
                : new String[] {"Workout plan", "Trainer", "Assigned", "Ends", "Status"};
    }

    public void setAssignments(List<MemberWorkout> list) {
        this.rows = new ArrayList<>(list);
        fireTableDataChanged();
    }

    public MemberWorkout getAssignment(int modelRow) {
        return rows.get(modelRow);
    }

    public int statusColumn() {
        return showMember ? 5 : 4;
    }

    private int full(int column) {
        return showMember ? column : column + 1;
    }

    @Override
    public int getRowCount() { return rows.size(); }

    @Override
    public int getColumnCount() { return columns.length; }

    @Override
    public String getColumnName(int column) { return columns[column]; }

    @Override
    public Class<?> getColumnClass(int column) {
        int c = full(column);
        return (c == 3 || c == 4) ? LocalDate.class : String.class;
    }

    @Override
    public Object getValueAt(int row, int column) {
        MemberWorkout a = rows.get(row);
        switch (full(column)) {
            case 0:
                return a.getMemberName();
            case 1:
                return a.getPlanName();
            case 2:
                return a.getTrainerName();
            case 3:
                return a.getAssignedDate();
            case 4:
                return a.getEndDate();
            default:
                return a.getStatus();
        }
    }
}
