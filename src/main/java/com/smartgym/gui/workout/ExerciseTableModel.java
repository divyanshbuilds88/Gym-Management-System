package com.smartgym.gui.workout;

import com.smartgym.model.WorkoutExercise;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/** Table data for the exercises of one workout plan. */
public class ExerciseTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"Day", "Exercise", "Sets", "Reps", "Minutes", "Muscle group"};

    private transient List<WorkoutExercise> rows = new ArrayList<>();

    public void setExercises(List<WorkoutExercise> list) {
        this.rows = new ArrayList<>(list);
        fireTableDataChanged();
    }

    /** A copy of the current rows (used when a new plan is saved with its exercises). */
    public List<WorkoutExercise> getExercises() {
        return new ArrayList<>(rows);
    }

    public WorkoutExercise getExercise(int modelRow) {
        return rows.get(modelRow);
    }

    public void addExercise(WorkoutExercise e) {
        rows.add(e);
        fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
    }

    public void removeExercise(int modelRow) {
        rows.remove(modelRow);
        fireTableRowsDeleted(modelRow, modelRow);
    }

    @Override
    public int getRowCount() { return rows.size(); }

    @Override
    public int getColumnCount() { return COLUMNS.length; }

    @Override
    public String getColumnName(int column) { return COLUMNS[column]; }

    @Override
    public Class<?> getColumnClass(int column) {
        return (column >= 2 && column <= 4) ? Integer.class : String.class;
    }

    @Override
    public Object getValueAt(int row, int column) {
        WorkoutExercise e = rows.get(row);
        switch (column) {
            case 0:
                return e.getScheduleDay();
            case 1:
                return e.getExerciseName();
            case 2:
                return e.getSets();
            case 3:
                return e.getRepetitions();
            case 4:
                return e.getDurationMinutes();
            default:
                return e.getTargetMuscleGroup();
        }
    }
}
