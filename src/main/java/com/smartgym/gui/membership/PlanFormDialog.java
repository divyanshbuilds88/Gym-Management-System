package com.smartgym.gui.membership;

import com.smartgym.gui.common.FormDialog;
import com.smartgym.gui.common.FormPanel;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.model.MembershipPlan;
import com.smartgym.service.MembershipService;
import com.smartgym.util.ValidationUtil;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.Window;

/** Add / edit a membership plan (Monthly, Quarterly ...). */
public class PlanFormDialog extends FormDialog {

    private static final long serialVersionUID = 1L;

    private final transient MembershipService service = new MembershipService();
    private final transient MembershipPlan existing;
    private final transient Runnable onSaved;

    private final JTextField nameField = UiFactory.textField(16);
    private final JSpinner monthsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 60, 1));
    private final JTextField feeField = UiFactory.textField(10);
    private final JTextField descriptionField = UiFactory.textField(20);
    private final JCheckBox activeCheck = new JCheckBox("Offer this plan to new members", true);

    public PlanFormDialog(Window owner, MembershipPlan existing, Runnable onSaved) {
        super(owner,
                existing == null ? "Add Membership Plan" : "Edit Membership Plan",
                existing == null ? "Create a new plan" : "Change the plan " + existing.getPlanName(),
                existing == null ? "Add Plan" : "Save Changes");
        this.existing = existing;
        this.onSaved = onSaved;
        build();
        if (existing != null) {
            nameField.setText(existing.getPlanName());
            monthsSpinner.setValue(existing.getDurationMonths());
            feeField.setText(String.valueOf(existing.getFee()));
            descriptionField.setText(existing.getDescription());
            activeCheck.setSelected(existing.isActive());
        }
    }

    @Override
    protected JComponent buildForm() {
        nameField.setName("plan.name");
        monthsSpinner.setName("plan.months");
        feeField.setName("plan.fee");
        descriptionField.setName("plan.description");
        UiFactory.styleSpinner(monthsSpinner);
        activeCheck.setOpaque(false);

        FormPanel form = new FormPanel();
        form.addField("PLAN NAME", nameField);
        form.addFieldPair("DURATION (months)", monthsSpinner, "FEE (Rs.)", feeField);
        form.addField("DESCRIPTION", descriptionField);
        form.addComponent(activeCheck);
        return form;
    }

    @Override
    protected void onSave() {
        double fee = ValidationUtil.parseDoubleOrMinusOne(feeField.getText());
        if (fee < 0) {
            showError("Fee must be a number, for example 1500.");
            return;
        }
        final MembershipPlan plan = new MembershipPlan();
        if (existing != null) {
            plan.setPlanId(existing.getPlanId());
        }
        plan.setPlanName(nameField.getText());
        plan.setDurationMonths((Integer) monthsSpinner.getValue());
        plan.setFee(fee);
        plan.setDescription(descriptionField.getText().trim());
        plan.setActive(activeCheck.isSelected());

        runSave(() -> {
            if (existing == null) {
                service.addPlan(plan);
            } else {
                service.updatePlan(plan);
            }
            return Boolean.TRUE;
        }, ok -> {
            dispose();
            onSaved.run();
        });
    }
}
