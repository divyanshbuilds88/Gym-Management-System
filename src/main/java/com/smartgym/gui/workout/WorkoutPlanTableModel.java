package com.smartgym.gui.workout;

import com.smartgym.model.WorkoutPlan;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/** Table data for workout plans. */
public class WorkoutPlanTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"Plan", "Goal", "Level", "Created by", "Description"};

    private transient List<WorkoutPlan> plans = new ArrayList<>();

    public void setPlans(List<WorkoutPlan> list) {
        this.plans = new ArrayList<>(list);
        fireTableDataChanged();
    }

    public WorkoutPlan getPlan(int modelRow) {
        return plans.get(modelRow);
    }

    @Override
    public int getRowCount() { return plans.size(); }

    @Override
    public int getColumnCount() { return COLUMNS.length; }

    @Override
    public String getColumnName(int column) { return COLUMNS[column]; }

    @Override
    public Object getValueAt(int row, int column) {
        WorkoutPlan p = plans.get(row);
        switch (column) {
            case 0:
                return p.getPlanName();
            case 1:
                return p.getGoal();
            case 2:
                return p.getLevel();
            case 3:
                return p.getTrainerName();
            default:
                return p.getDescription();
        }
    }
}
