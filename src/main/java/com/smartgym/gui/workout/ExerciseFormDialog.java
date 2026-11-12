package com.smartgym.gui.workout;

import com.smartgym.gui.common.FormDialog;
import com.smartgym.gui.common.FormPanel;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.model.WorkoutExercise;
import com.smartgym.util.Constants;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.Window;
import java.util.function.Consumer;

/**
 * Add / edit ONE exercise. What "save" means is decided by the caller through the Saver interface:
 *   - inside the "new plan" window it only collects the exercise in a list,
 *   - on the plans screen it writes to the database through WorkoutService.
 * (Passing behaviour into a class through an interface: the Strategy pattern.)
 */
public class ExerciseFormDialog extends FormDialog {

    private static final long serialVersionUID = 1L;
    private static final String[] MUSCLES = {"Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Glutes", "Full Body", "Cardio"};

    /** The action performed when Save is pressed. Runs on a background thread. */
    public interface Saver {
        void save(WorkoutExercise exercise) throws Exception;
    }

    private final transient WorkoutExercise existing;
    private final transient Saver saver;
    private final transient Consumer<WorkoutExercise> onSaved;

    private final JTextField nameField = UiFactory.textField(20);
    private final JSpinner setsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 20, 1));
    private final JSpinner repsSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 200, 1));
    private final JSpinner minutesSpinner = new JSpinner(new SpinnerNumberModel(15, 1, 240, 5));
    private final JComboBox<String> muscleCombo = new JComboBox<>(MUSCLES);
    private final JComboBox<String> dayCombo = new JComboBox<>(Constants.DAYS);

    public ExerciseFormDialog(Window owner, WorkoutExercise existing, Saver saver, Consumer<WorkoutExercise> onSaved) {
        super(owner,
                existing == null ? "Add Exercise" : "Edit Exercise",
                existing == null ? "Describe the exercise" : "Change " + existing.getExerciseName(),
                existing == null ? "Add Exercise" : "Save Changes");
        this.existing = existing;
        this.saver = saver;
        this.onSaved = onSaved;
        build();
        if (existing != null) {
            nameField.setText(existing.getExerciseName());
            setsSpinner.setValue(existing.getSets());
            repsSpinner.setValue(existing.getRepetitions());
            minutesSpinner.setValue(existing.getDurationMinutes());
            muscleCombo.setSelectedItem(existing.getTargetMuscleGroup());
            dayCombo.setSelectedItem(existing.getScheduleDay());
        }
    }

    @Override
    protected JComponent buildForm() {
        nameField.setName("ex.name");
        setsSpinner.setName("ex.sets");
        repsSpinner.setName("ex.reps");
        minutesSpinner.setName("ex.minutes");
        muscleCombo.setName("ex.muscle");
        dayCombo.setName("ex.day");
        UiFactory.styleSpinner(setsSpinner);
        UiFactory.styleSpinner(repsSpinner);
        UiFactory.styleSpinner(minutesSpinner);
        UiFactory.styleCombo(muscleCombo);
        UiFactory.styleCombo(dayCombo);
        muscleCombo.setEditable(true);
        muscleCombo.setSelectedIndex(-1);                     // empty: the user must choose or type one

        FormPanel form = new FormPanel();
        form.addField("EXERCISE NAME", nameField);
        form.addFieldPair("SETS", setsSpinner, "REPETITIONS", repsSpinner);
        form.addFieldPair("DURATION (minutes)", minutesSpinner, "DAY OF THE WEEK", dayCombo);
        form.addField("TARGET MUSCLE GROUP", muscleCombo);
        return form;
    }

    @Override
    protected void onSave() {
        if (nameField.getText().trim().isEmpty()) {
            showError("Exercise name is required.");
            return;
        }
        Object chosen = muscleCombo.getEditor().getItem();
        if (chosen == null || chosen.toString().trim().isEmpty()) {
            showError("Please choose the target muscle group.");
            return;
        }
        final WorkoutExercise e = new WorkoutExercise();
        if (existing != null) {
            e.setExerciseId(existing.getExerciseId());
            e.setWorkoutPlanId(existing.getWorkoutPlanId());
        }
        e.setExerciseName(nameField.getText().trim());
        e.setSets((Integer) setsSpinner.getValue());
        e.setRepetitions((Integer) repsSpinner.getValue());
        e.setDurationMinutes((Integer) minutesSpinner.getValue());
        Object muscle = muscleCombo.getEditor().getItem();
        e.setTargetMuscleGroup(muscle == null ? "" : muscle.toString());
        e.setScheduleDay((String) dayCombo.getSelectedItem());

        runSave(() -> {
            saver.save(e);
            return e;
        }, saved -> {
            dispose();
            onSaved.accept(saved);
        });
    }
}
