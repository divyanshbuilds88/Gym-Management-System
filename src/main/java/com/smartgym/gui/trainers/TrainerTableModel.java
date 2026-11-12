package com.smartgym.gui.trainers;

import com.smartgym.model.Trainer;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/** Table data for trainers. The salary column is only included for the admin. */
public class TrainerTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private final boolean showSalary;
    private final String[] columns;
    private transient List<Trainer> trainers = new ArrayList<>();

    public TrainerTableModel(boolean showSalary) {
        this.showSalary = showSalary;
        this.columns = showSalary
                ? new String[] {"Name", "Specialization", "Phone", "Email", "Availability", "Experience", "Salary (Rs.)"}
                : new String[] {"Name", "Specialization", "Phone", "Email", "Availability", "Experience"};
    }

    public void setTrainers(List<Trainer> list) {
        this.trainers = new ArrayList<>(list);
        fireTableDataChanged();
    }

    public Trainer getTrainer(int modelRow) {
        return trainers.get(modelRow);
    }

    @Override
    public int getRowCount() { return trainers.size(); }

    @Override
    public int getColumnCount() { return columns.length; }

    @Override
    public String getColumnName(int column) { return columns[column]; }

    @Override
    public Class<?> getColumnClass(int column) {
        switch (column) {
            case 5:
                return Integer.class;
            case 6:
                return Double.class;
            default:
                return String.class;
        }
    }

    @Override
    public Object getValueAt(int row, int column) {
        Trainer t = trainers.get(row);
        switch (column) {
            case 0:
                return t.getFullName();
            case 1:
                return t.getSpecialization();
            case 2:
                return t.getPhone();
            case 3:
                return t.getEmail();
            case 4:
                return t.getAvailability();
            case 5:
                return t.getExperienceYears();
            default:
                return showSalary ? Double.valueOf(t.getSalary()) : null;
        }
    }
}
