package com.smartgym.gui.scheduling;

import com.smartgym.model.TrainerSchedule;

import javax.swing.table.AbstractTableModel;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Table data for training sessions. Trainer and member columns can be left out (member's own view). */
public class SessionTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[] ALL_NAMES = {"Date", "Time", "Minutes", "Trainer", "Member", "Status", "Notes"};

    private final int[] columns;                      // which of the 7 "full" columns are shown
    private transient List<TrainerSchedule> rows = new ArrayList<>();

    public SessionTableModel(boolean showTrainer, boolean showMember) {
        List<Integer> list = new ArrayList<>();
        list.add(0);
        list.add(1);
        list.add(2);
        if (showTrainer) {
            list.add(3);
        }
        if (showMember) {
            list.add(4);
        }
        list.add(5);
        list.add(6);
        columns = new int[list.size()];
        for (int i = 0; i < columns.length; i++) {
            columns[i] = list.get(i);
        }
    }

    public void setSessions(List<TrainerSchedule> list) {
        this.rows = new ArrayList<>(list);
        fireTableDataChanged();
    }

    public TrainerSchedule getSession(int modelRow) {
        return rows.get(modelRow);
    }

    /** Position of the column with the given "full" number (0..6) among the visible columns, or -1. */
    public int columnOf(int fullIndex) {
        for (int i = 0; i < columns.length; i++) {
            if (columns[i] == fullIndex) {
                return i;
            }
        }
        return -1;
    }

    public int statusColumn() {
        return columnOf(5);
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
            case 0:
                return LocalDate.class;
            case 2:
                return Integer.class;
            default:
                return String.class;
        }
    }

    @Override
    public Object getValueAt(int row, int column) {
        TrainerSchedule s = rows.get(row);
        switch (columns[column]) {
            case 0:
                return s.getSessionDate();
            case 1:
                return s.getStartTime().format(TIME) + " - " + s.getEndTime().format(TIME);
            case 2:
                return s.getDurationMinutes();
            case 3:
                return s.getTrainerName();
            case 4:
                return s.getMemberName();
            case 5:
                return s.getStatus();
            default:
                return s.getNotes();
        }
    }
}
