package com.smartgym.gui.workout;

import com.smartgym.gui.common.BackgroundTask;
import com.smartgym.gui.common.FormDialog;
import com.smartgym.gui.common.FormPanel;
import com.smartgym.gui.common.RoundedButton;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.model.Role;
import com.smartgym.model.Trainer;
import com.smartgym.model.WorkoutExercise;
import com.smartgym.model.WorkoutPlan;
import com.smartgym.service.SessionManager;
import com.smartgym.service.TrainerService;
import com.smartgym.service.WorkoutService;
import com.smartgym.util.Constants;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Create or edit a workout plan.
 *  - Create: the plan AND its exercises are entered here and saved together (one database transaction).
 *  - Edit:   only name, goal, level and description change; exercises are edited on the plans screen.
 */
public class WorkoutPlanFormDialog extends FormDialog {

    private static final long serialVersionUID = 1L;

    private final transient WorkoutService workoutService = new WorkoutService();
    private final transient TrainerService trainerService = new TrainerService();
    private final transient SessionManager session = SessionManager.getInstance();
    private final transient WorkoutPlan existing;
    private final transient Runnable onSaved;

    private final JTextField nameField = UiFactory.textField(20);
    private final JTextField goalField = UiFactory.textField(20);
    private final JComboBox<String> levelCombo = new JComboBox<>(Constants.WORKOUT_LEVELS);
    private final JTextField descriptionField = UiFactory.textField(20);
    private final JComboBox<Trainer> trainerCombo = new JComboBox<>();

    private final ExerciseTableModel exerciseModel = new ExerciseTableModel();
    private final JTable exerciseTable = UiFactory.createTable(exerciseModel);
    private final RoundedButton addExerciseButton = new RoundedButton("+  Add exercise", Theme.ACCENT);
    private final RoundedButton removeExerciseButton = RoundedButton.secondary("Remove selected");

    public WorkoutPlanFormDialog(Window owner, WorkoutPlan existing, Runnable onSaved) {
        super(owner,
                existing == null ? "New Workout Plan" : "Edit Workout Plan",
                existing == null ? "Name the plan and add its exercises" : "Change the details of " + existing.getPlanName(),
                existing == null ? "Create Plan" : "Save Changes");
        this.existing = existing;
        this.onSaved = onSaved;
        build();
        if (existing != null) {
            nameField.setText(existing.getPlanName());
            goalField.setText(existing.getGoal());
            levelCombo.setSelectedItem(existing.getLevel());
            descriptionField.setText(existing.getDescription());
        } else if (session.getRole() == Role.ADMIN) {
            loadTrainers();
        }
    }

    @Override
    protected JComponent buildForm() {
        nameField.setName("plan.name");
        goalField.setName("plan.goal");
        levelCombo.setName("plan.level");
        descriptionField.setName("plan.description");
        trainerCombo.setName("plan.trainer");
        exerciseTable.setName("plan.exercises");
        addExerciseButton.setName("plan.addExercise");
        removeExerciseButton.setName("plan.removeExercise");
        UiFactory.styleCombo(levelCombo);
        UiFactory.styleCombo(trainerCombo);

        FormPanel form = new FormPanel();
        form.addField("PLAN NAME", nameField);
        form.addFieldPair("GOAL", goalField, "LEVEL", levelCombo);
        form.addField("DESCRIPTION", descriptionField);

        if (existing == null) {
            if (session.getRole() == Role.ADMIN) {
                form.addField("CREATED BY TRAINER", trainerCombo);
            }
            form.addSection("Exercises");
            UiFactory.styleTable(exerciseTable);
            UiFactory.setColumnWidths(exerciseTable, 50, 140, 50, 55, 65, 100);
            exerciseModel.addTableModelListener(e -> clearError());
            JPanel tablePanel = new JPanel(new BorderLayout());
            tablePanel.setOpaque(false);
            tablePanel.add(UiFactory.scroll(exerciseTable), BorderLayout.CENTER);
            tablePanel.setPreferredSize(new Dimension(420, 150));
            form.addComponent(tablePanel);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            buttons.setOpaque(false);
            buttons.add(addExerciseButton);
            buttons.add(removeExerciseButton);
            form.addComponent(buttons);

            addExerciseButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // the "saver" does nothing in the background; the exercise is just collected in the table
                    new ExerciseFormDialog(WorkoutPlanFormDialog.this, null, ex -> { }, exerciseModel::addExercise)
                            .setVisible(true);
                }
            });
            removeExerciseButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int row = exerciseTable.getSelectedRow();
                    if (row >= 0) {
                        exerciseModel.removeExercise(exerciseTable.convertRowIndexToModel(row));
                    }
                }
            });
        }
        return form;
    }

    private void loadTrainers() {
        BackgroundTask.run(this, trainerService::getAllTrainers, (List<Trainer> trainers) -> {
            for (Trainer t : trainers) {
                trainerCombo.addItem(t);
            }
        }, ex -> showError("Could not load the trainers: " + ex.getMessage()));
    }

    @Override
    protected void onSave() {
        final WorkoutPlan plan = new WorkoutPlan();
        plan.setPlanName(nameField.getText());
        plan.setGoal(goalField.getText());
        plan.setLevel((String) levelCombo.getSelectedItem());
        plan.setDescription(descriptionField.getText().trim());

        if (existing != null) {
            plan.setWorkoutPlanId(existing.getWorkoutPlanId());
            plan.setCreatedBy(existing.getCreatedBy());
            runSave(() -> {
                workoutService.updatePlan(plan);
                return Boolean.TRUE;
            }, ok -> {
                dispose();
                onSaved.run();
            });
            return;
        }

        // who created the plan: the logged-in trainer, or the trainer chosen by the admin
        if (session.getRole() == Role.TRAINER) {
            plan.setCreatedBy(session.getTrainerId());
        } else {
            Trainer chosen = (Trainer) trainerCombo.getSelectedItem();
            if (chosen == null) {
                showError("Please choose the trainer who owns this plan.");
                return;
            }
            plan.setCreatedBy(chosen.getTrainerId());
        }
        final List<WorkoutExercise> exercises = exerciseModel.getExercises();   // copied on the Swing thread
        runSave(() -> workoutService.createPlan(plan, exercises), id -> {
            dispose();
            onSaved.run();
        });
    }
}
