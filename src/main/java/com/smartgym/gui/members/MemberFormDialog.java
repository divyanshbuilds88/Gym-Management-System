package com.smartgym.gui.members;

import com.smartgym.exception.SmartGymException;
import com.smartgym.gui.common.BackgroundTask;
import com.smartgym.gui.common.DateSpinner;
import com.smartgym.gui.common.FormDialog;
import com.smartgym.gui.common.FormPanel;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.gui.payments.ReceiptDialog;
import com.smartgym.model.Member;
import com.smartgym.model.MembershipPlan;
import com.smartgym.model.Payment;
import com.smartgym.service.MemberService;
import com.smartgym.service.MembershipService;
import com.smartgym.util.Constants;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.time.LocalDate;

/**
 * The Add Member / Edit Member window. It extends FormDialog (the shared base class).
 * When adding, the user can also create a login account and buy a membership plan in the same step.
 */
public class MemberFormDialog extends FormDialog {

    private static final long serialVersionUID = 1L;

    /** What the background task returns when a member was added. */
    private static final class AddResult {
        Payment payment;
        String planWarning;
    }

    private final transient MemberService memberService = new MemberService();
    private final transient MembershipService membershipService = new MembershipService();
    private final transient Member existing;                   // null = we are adding a new member
    private final transient Runnable onSaved;

    private final JTextField nameField = UiFactory.textField(20);
    private final JSpinner ageSpinner = new JSpinner(new SpinnerNumberModel(25, 10, 100, 1));
    private final JRadioButton maleRadio = new JRadioButton("Male", true);
    private final JRadioButton femaleRadio = new JRadioButton("Female");
    private final JRadioButton otherRadio = new JRadioButton("Other");
    private final JTextField phoneField = UiFactory.textField(12);
    private final JTextField emailField = UiFactory.textField(12);
    private final JTextArea addressArea = new JTextArea(2, 20);
    private final DateSpinner joinedSpinner = new DateSpinner(LocalDate.now());

    // only used when adding
    private final JCheckBox loginCheck = new JCheckBox("Create a login account for this member");
    private final JTextField usernameField = UiFactory.textField(10);
    private final JPasswordField passwordField = UiFactory.passwordField(10);
    private final JCheckBox planCheck = new JCheckBox("Assign a membership plan now");
    private final JComboBox<MembershipPlan> planCombo = new JComboBox<>();
    private final DateSpinner startSpinner = new DateSpinner(LocalDate.now());
    private final JComboBox<String> methodCombo = new JComboBox<>(Constants.PAYMENT_METHODS);
    private final JCheckBox paidCheck = new JCheckBox("Payment received now", true);

    public MemberFormDialog(Window owner, Member existing, Runnable onSaved) {
        super(owner,
                existing == null ? "Add Member" : "Edit Member",
                existing == null ? "Enter the new member's details" : "Update the details of " + existing.getFullName(),
                existing == null ? "Add Member" : "Save Changes");
        this.existing = existing;
        this.onSaved = onSaved;
        build();                                              // the form fields exist now
        if (existing != null) {
            fill(existing);
        } else {
            loadPlans();
        }
    }

    // ================================================================ form

    @Override
    protected JComponent buildForm() {
        nameField.setName("member.name");
        ageSpinner.setName("member.age");
        phoneField.setName("member.phone");
        emailField.setName("member.email");
        addressArea.setName("member.address");
        usernameField.setName("member.username");
        passwordField.setName("member.password");
        loginCheck.setName("member.loginCheck");
        planCheck.setName("member.planCheck");
        planCombo.setName("member.plan");

        UiFactory.styleSpinner(ageSpinner);
        UiFactory.styleCombo(planCombo);
        UiFactory.styleCombo(methodCombo);
        planCombo.setRenderer(new PlanRenderer());

        ButtonGroup genders = new ButtonGroup();
        JPanel genderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        genderPanel.setOpaque(false);
        for (JRadioButton r : new JRadioButton[] {maleRadio, femaleRadio, otherRadio}) {
            r.setOpaque(false);
            r.setFocusPainted(false);
            genders.add(r);
            genderPanel.add(r);
        }
        addressArea.setFont(Theme.regular(14));
        addressArea.setLineWrap(true);
        addressArea.setWrapStyleWord(true);
        addressArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        FormPanel form = new FormPanel();
        form.addField("FULL NAME", nameField);
        form.addFieldPair("AGE", ageSpinner, "GENDER", genderPanel);
        form.addFieldPair("PHONE (10 digits)", phoneField, "EMAIL", emailField);
        form.addField("ADDRESS", UiFactory.scroll(addressArea));
        form.addField("JOINING DATE", joinedSpinner);

        if (existing == null) {
            loginCheck.setOpaque(false);
            planCheck.setOpaque(false);
            paidCheck.setOpaque(false);
            usernameField.setEnabled(false);
            passwordField.setEnabled(false);
            planCombo.setEnabled(false);
            startSpinner.setEnabled(false);
            methodCombo.setEnabled(false);
            paidCheck.setEnabled(false);

            // ItemListener: called when a check box is ticked or unticked
            loginCheck.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    boolean on = e.getStateChange() == ItemEvent.SELECTED;
                    usernameField.setEnabled(on);
                    passwordField.setEnabled(on);
                }
            });
            planCheck.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    boolean on = e.getStateChange() == ItemEvent.SELECTED;
                    planCombo.setEnabled(on);
                    startSpinner.setEnabled(on);
                    methodCombo.setEnabled(on);
                    paidCheck.setEnabled(on);
                }
            });

            form.addSection("Login account (optional)");
            form.addComponent(loginCheck);
            form.addFieldPair("USERNAME", usernameField, "PASSWORD", passwordField);
            form.addSection("Membership (optional)");
            form.addComponent(planCheck);
            form.addField("PLAN", planCombo);
            form.addFieldPair("START DATE", startSpinner, "PAYMENT METHOD", methodCombo);
            form.addComponent(paidCheck);
        }
        return form;
    }

    /** Fills the form with an existing member (edit mode). */
    private void fill(Member m) {
        nameField.setText(m.getFullName());
        ageSpinner.setValue(m.getAge());
        maleRadio.setSelected("MALE".equals(m.getGender()));
        femaleRadio.setSelected("FEMALE".equals(m.getGender()));
        otherRadio.setSelected("OTHER".equals(m.getGender()));
        phoneField.setText(m.getPhone());
        emailField.setText(m.getEmail());
        addressArea.setText(m.getAddress() == null ? "" : m.getAddress());
        if (m.getJoiningDate() != null) {
            joinedSpinner.setDate(m.getJoiningDate());
        }
    }

    private void loadPlans() {
        BackgroundTask.run(this, membershipService::getActivePlans, plans -> {
            for (MembershipPlan p : plans) {
                planCombo.addItem(p);
            }
        }, ex -> showError("Could not load the membership plans: " + ex.getMessage()));
    }

    // ================================================================ saving

    private String selectedGender() {
        if (femaleRadio.isSelected()) {
            return "FEMALE";
        }
        return otherRadio.isSelected() ? "OTHER" : "MALE";
    }

    @Override
    protected void onSave() {
        final Member m = new Member();
        if (existing != null) {
            m.setMemberId(existing.getMemberId());
            m.setMemberCode(existing.getMemberCode());
            m.setUserId(existing.getUserId());
        }
        m.setFullName(nameField.getText());
        m.setAge((Integer) ageSpinner.getValue());
        m.setGender(selectedGender());
        m.setPhone(phoneField.getText());
        m.setEmail(emailField.getText());
        m.setAddress(addressArea.getText().trim());
        m.setJoiningDate(joinedSpinner.getDate());

        if (existing != null) {
            runSave(() -> {
                memberService.updateMember(m);
                return Boolean.TRUE;
            }, ok -> {
                dispose();
                onSaved.run();
            });
            return;
        }

        final boolean withLogin = loginCheck.isSelected();
        final String username = withLogin ? usernameField.getText() : null;
        final String password = withLogin ? new String(passwordField.getPassword()) : null;
        final boolean withPlan = planCheck.isSelected();
        final MembershipPlan plan = (MembershipPlan) planCombo.getSelectedItem();
        if (withPlan && plan == null) {
            showError("Please choose a membership plan.");
            return;
        }
        final LocalDate start = startSpinner.getDate();
        final String method = (String) methodCombo.getSelectedItem();
        final boolean paid = paidCheck.isSelected();
        final Window owner = getOwner();

        runSave(() -> {
            AddResult result = new AddResult();
            int id = memberService.addMember(m, username, password);
            if (withPlan) {
                try {
                    result.payment = membershipService.assignMembership(id, plan.getPlanId(), start, method, paid);
                } catch (SmartGymException e) {
                    result.planWarning = e.getMessage();           // the member is saved; only the plan failed
                }
            }
            return result;
        }, result -> {
            dispose();
            onSaved.run();
            if (result.planWarning != null) {
                JOptionPane.showMessageDialog(owner, "The member was added, but the plan could not be assigned:\n"
                        + result.planWarning, "Smart Gym", JOptionPane.WARNING_MESSAGE);
            } else if (result.payment != null) {
                new ReceiptDialog(owner, result.payment).setVisible(true);
            }
        });
    }

    /** Shows "Monthly - 1 month - Rs. 1,500" in the plan drop-down. */
    private static class PlanRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof MembershipPlan) {
                MembershipPlan p = (MembershipPlan) value;
                setText(p.getPlanName() + "  -  " + p.getDurationMonths()
                        + (p.getDurationMonths() == 1 ? " month" : " months") + "  -  Rs. " + String.format("%,.0f", p.getFee()));
            }
            return this;
        }
    }
}
