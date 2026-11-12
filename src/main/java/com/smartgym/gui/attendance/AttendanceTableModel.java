package com.smartgym.gui.attendance;

import com.smartgym.model.Attendance;

import javax.swing.table.AbstractTableModel;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Table data for gym visits. The date and member columns can be left out. */
public class AttendanceTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[] ALL_NAMES = {"Date", "Code", "Member", "Check-in", "Check-out", "Time spent", "Status"};

    private final int[] columns;
    private transient List<Attendance> rows = new ArrayList<>();

    public AttendanceTableModel(boolean showDate, boolean showMember) {
        List<Integer> list = new ArrayList<>();
        if (showDate) {
            list.add(0);
        }
        if (showMember) {
            list.add(1);
            list.add(2);
        }
        list.add(3);
        list.add(4);
        list.add(5);
        list.add(6);
        columns = new int[list.size()];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = list.get(i);
        }
    }

    public void setVisits(List<Attendance> list) {
        this.rows = new ArrayList<>(list);
        fireTableDataChanged();
    }

    public Attendance getVisit(int modelRow) {
        return rows.get(modelRow);
    }

    public int statusColumn() {
        for (int i = 0; i < columns.length; i++) {
            if (columns[i] == 6) {
                return i;
            }
        }
        return -1;
    }

    /** "1h 10m" or "45m" - or "-" when the member has not left yet. */
    static String formatDuration(Attendance a) {
        long minutes = a.getDurationMinutes();
        if (minutes < 0) {
            return "-";
        }
        return minutes >= 60 ? (minutes / 60) + "h " + (minutes % 60) + "m" : minutes + "m";
    }

    @Override
    public int getRowCount() { return rows.size(); }

    @Override
    public int getColumnCount() { return columns.length; }

    @Override
    public String getColumnName(int column) { return ALL_NAMES[columns[column]]; }

    @Override
    public Class<?> getColumnClass(int column) {
        return columns[column] == 0 ? LocalDate.class : String.class;
    }

    @Override
    public Object getValueAt(int row, int column) {
        Attendance a = rows.get(row);
        switch (columns[column]) {
            case 0:
                return a.getAttendanceDate();
            case 1:
                return a.getMemberCode();
            case 2:
                return a.getMemberName();
            case 3:
                return a.getCheckInTime().format(TIME);
            case 4:
                return a.getCheckOutTime() == null ? "still inside" : a.getCheckOutTime().format(TIME);
            case 5:
                return formatDuration(a);
            default:
                return a.isCheckedOut() ? "DONE" : "INSIDE";
        }
    }
}
