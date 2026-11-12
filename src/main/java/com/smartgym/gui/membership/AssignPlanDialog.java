package com.smartgym.gui.membership;

import com.smartgym.gui.common.BackgroundTask;
import com.smartgym.gui.common.DateSpinner;
import com.smartgym.gui.common.FormDialog;
import com.smartgym.gui.common.FormPanel;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.gui.payments.ReceiptDialog;
import com.smartgym.model.Member;
import com.smartgym.model.MembershipPlan;
import com.smartgym.service.MemberService;
import com.smartgym.service.MembershipService;
import com.smartgym.util.Constants;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.util.List;

/**
 * Buy or renew a membership for a member (also creates the payment and shows its receipt).
 *  - "Assign" mode: pick member, plan and start date.
 *  - "Renew" mode: the start date is worked out by the service (continues after the current period).
 */
public class AssignPlanDialog extends FormDialog {

    private static final long serialVersionUID = 1L;

    /** Result of loading the drop-down lists in the background. */
    private static final class Lists {
        List<Member> members;
        List<MembershipPlan> plans;
    }

    private final transient MemberService memberService = new MemberService();
    private final transient MembershipService membershipService = new MembershipService();
    private final transient Member preselected;
    private final boolean renew;
    private final transient Runnable onDone;

    private final JComboBox<Member> memberCombo = new JComboBox<>();
    private final JComboBox<MembershipPlan> planCombo = new JComboBox<>();
    private final DateSpinner startSpinner = new DateSpinner(LocalDate.now());
    private final JComboBox<String> methodCombo = new JComboBox<>(Constants.PAYMENT_METHODS);
    private final JCheckBox paidCheck = new JCheckBox("Payment received now", true);
    private final JLabel summaryLabel = UiFactory.label(" ", 13, true, Theme.TEXT);

    public AssignPlanDialog(Window owner, Member preselected, boolean renew, Runnable onDone) {
        super(owner,
                renew ? "Renew Membership" : "Assign Membership Plan",
                renew ? "Extend a member's membership" : "Choose a member and a plan",
                renew ? "Renew" : "Assign Plan");
        this.preselected = preselected;
        this.renew = renew;
        this.onDone = onDone;
        build();
        load();
    }

    @Override
    protected JComponent buildForm() {
        memberCombo.setName("assign.member");
        planCombo.setName("assign.plan");
        methodCombo.setName("assign.method");
        paidCheck.setName("assign.paid");
        startSpinner.setName("assign.start");
        UiFactory.styleCombo(memberCombo);
        UiFactory.styleCombo(planCombo);
        UiFactory.styleCombo(methodCombo);
        planCombo.setRenderer(new PlanRenderer());
        paidCheck.setOpaque(false);

        if (preselected != null) {
            memberCombo.addItem(preselected);
            memberCombo.setEnabled(false);                  // the member was chosen in the table already
        }
        if (renew) {
            startSpinner.setEnabled(false);
        }

        ActionListener refresh = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateSummary();
            }
        };
        planCombo.addActionListener(refresh);
        startSpinner.addChangeListener(e -> updateSummary());

        summaryLabel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        summaryLabel.setOpaque(true);
        summaryLabel.setBackground(new java.awt.Color(0xFFF7ED));

        FormPanel form = new FormPanel();
        form.addField("MEMBER", memberCombo);
        form.addField("MEMBERSHIP PLAN", planCombo);
        if (renew) {
            form.addField("START DATE", startSpinner);
            form.addComponent(UiFactory.muted("<html>The new period starts when the current one ends, or today if it has already expired.</html>"));
        } else {
            form.addField("START DATE", startSpinner);
        }
        form.addField("PAYMENT METHOD", methodCombo);
        form.addComponent(paidCheck);
        form.addComponent(summaryLabel);
        return form;
    }

    private void load() {
        BackgroundTask.run(this, () -> {
            Lists lists = new Lists();
            if (preselected == null) {
                lists.members = memberService.getAllMembers();
            }
            lists.plans = membershipService.getActivePlans();
            return lists;
        }, lists -> {
            if (lists.members != null) {
                for (Member m : lists.members) {
                    memberCombo.addItem(m);
                }
            }
            for (MembershipPlan p : lists.plans) {
                planCombo.addItem(p);
            }
            updateSummary();
        }, ex -> showError("Could not load the lists: " + ex.getMessage()));
    }

    private void updateSummary() {
        MembershipPlan plan = (MembershipPlan) planCombo.getSelectedItem();
        if (plan == null) {
            summaryLabel.setText(" ");
            return;
        }
        String amount = "Amount to pay: Rs. " + String.format("%,.2f", plan.getFee());
        if (renew) {
            summaryLabel.setText(amount + "   |   adds " + plan.getDurationMonths()
                    + (plan.getDurationMonths() == 1 ? " month" : " months"));
        } else {
            LocalDate start = startSpinner.getDate();
            LocalDate end = MembershipService.calculateExpiry(start, plan.getDurationMonths());
            summaryLabel.setText(amount + "   |   valid " + start.format(UiFactory.DATE_FORMAT)
                    + " to " + end.format(UiFactory.DATE_FORMAT));
        }
    }

    @Override
    protected void onSave() {
        final Member member = (Member) memberCombo.getSelectedItem();
        final MembershipPlan plan = (MembershipPlan) planCombo.getSelectedItem();
        if (member == null) {
            showError("Please choose a member.");
            return;
        }
        if (plan == null) {
            showError("Please choose a membership plan.");
            return;
        }
        final LocalDate start = startSpinner.getDate();
        final String method = (String) methodCombo.getSelectedItem();
        final boolean paid = paidCheck.isSelected();
        final Window owner = getOwner();

        runSave(() -> renew
                ? membershipService.renewMembership(member.getMemberId(), plan.getPlanId(), method, paid)
                : membershipService.assignMembership(member.getMemberId(), plan.getPlanId(), start, method, paid),
            payment -> {
                dispose();
                onDone.run();
                new ReceiptDialog(owner, payment).setVisible(true);
            });
    }

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
