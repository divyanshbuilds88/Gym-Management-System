package com.smartgym.gui.trainers;

import com.smartgym.gui.common.FormDialog;
import com.smartgym.gui.common.FormPanel;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.model.Trainer;
import com.smartgym.service.TrainerService;
import com.smartgym.util.ValidationUtil;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/** Add / edit a trainer. Extends the shared FormDialog. */
public class TrainerFormDialog extends FormDialog {

    private static final long serialVersionUID = 1L;
    private static final String[] SUGGESTIONS = {"Strength & Muscle Building", "Cardio & Weight Loss",
        "Yoga & Flexibility", "CrossFit", "Nutrition & Diet", "Zumba & Dance", "Boxing & Martial Arts"};

    private final transient TrainerService service = new TrainerService();
    private final transient Trainer existing;
    private final transient Runnable onSaved;

    private final JTextField nameField = UiFactory.textField(20);
    private final JComboBox<String> specCombo = new JComboBox<>(SUGGESTIONS);
    private final JTextField phoneField = UiFactory.textField(12);
    private final JTextField emailField = UiFactory.textField(12);
    private final AvailabilityPicker availability = new AvailabilityPicker();
    private final JTextField salaryField = UiFactory.textField(10);
    private final JSpinner experienceSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 50, 1));

    // only when adding
    private final JCheckBox loginCheck = new JCheckBox("Create a login account for this trainer");
    private final JTextField usernameField = UiFactory.textField(10);
    private final JPasswordField passwordField = UiFactory.passwordField(10);

    public TrainerFormDialog(Window owner, Trainer existing, Runnable onSaved) {
        super(owner,
                existing == null ? "Add Trainer" : "Edit Trainer",
                existing == null ? "Enter the new trainer's details" : "Update the details of " + existing.getFullName(),
                existing == null ? "Add Trainer" : "Save Changes");
        this.existing = existing;
        this.onSaved = onSaved;
        build();
        if (existing != null) {
            nameField.setText(existing.getFullName());
            specCombo.setSelectedItem(existing.getSpecialization());
            phoneField.setText(existing.getPhone());
            emailField.setText(existing.getEmail());
            availability.setText(existing.getAvailability());
            salaryField.setText(String.valueOf(existing.getSalary()));
            experienceSpinner.setValue(existing.getExperienceYears());
        }
    }

    @Override
    protected JComponent buildForm() {
        nameField.setName("trainer.name");
        specCombo.setName("trainer.spec");
        phoneField.setName("trainer.phone");
        emailField.setName("trainer.email");
        salaryField.setName("trainer.salary");
        experienceSpinner.setName("trainer.experience");
        loginCheck.setName("trainer.loginCheck");
        usernameField.setName("trainer.username");
        passwordField.setName("trainer.password");

        UiFactory.styleCombo(specCombo);
        specCombo.setEditable(true);                          // pick a suggestion OR type your own
        UiFactory.styleSpinner(experienceSpinner);

        FormPanel form = new FormPanel();
        form.addField("FULL NAME", nameField);
        form.addField("SPECIALIZATION  (choose or type your own)", specCombo);
        form.addFieldPair("PHONE (10 digits)", phoneField, "EMAIL", emailField);
        form.addField("AVAILABILITY", availability);
        form.addFieldPair("SALARY (Rs. per month)", salaryField, "EXPERIENCE (years)", experienceSpinner);

        if (existing == null) {
            loginCheck.setOpaque(false);
            usernameField.setEnabled(false);
            passwordField.setEnabled(false);
            loginCheck.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    boolean on = e.getStateChange() == ItemEvent.SELECTED;
                    usernameField.setEnabled(on);
                    passwordField.setEnabled(on);
                }
            });
            form.addSection("Login account (optional)");
            form.addComponent(loginCheck);
            form.addFieldPair("USERNAME", usernameField, "PASSWORD", passwordField);
        }
        return form;
    }

    @Override
    protected void onSave() {
        double salary = ValidationUtil.parseDoubleOrMinusOne(salaryField.getText());
        if (salary < 0) {
            showError("Salary must be a number, for example 25000.");
            return;
        }
        if (!availability.isValidRange()) {
            showError("The closing time must be later than the opening time.");
            return;
        }
        final Trainer t = new Trainer();
        if (existing != null) {
            t.setTrainerId(existing.getTrainerId());
            t.setUserId(existing.getUserId());
        }
        t.setFullName(nameField.getText());
        Object spec = specCombo.getEditor().getItem();            // works for typed text as well as chosen items
        t.setSpecialization(spec == null ? "" : spec.toString());
        t.setPhone(phoneField.getText());
        t.setEmail(emailField.getText());
        t.setAvailability(availability.getText());
        t.setSalary(salary);
        t.setExperienceYears((Integer) experienceSpinner.getValue());

        final boolean withLogin = existing == null && loginCheck.isSelected();
        final String username = withLogin ? usernameField.getText() : null;
        final String password = withLogin ? new String(passwordField.getPassword()) : null;

        runSave(() -> {
            if (existing == null) {
                service.addTrainer(t, username, password);
            } else {
                service.updateTrainer(t);
            }
            return Boolean.TRUE;
        }, ok -> {
            dispose();
            onSaved.run();
        });
    }
}
