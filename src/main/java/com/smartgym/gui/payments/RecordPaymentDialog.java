package com.smartgym.gui.payments;

import com.smartgym.gui.common.BackgroundTask;
import com.smartgym.gui.common.FormDialog;
import com.smartgym.gui.common.FormPanel;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.model.Member;
import com.smartgym.service.MemberService;
import com.smartgym.service.PaymentService;
import com.smartgym.util.Constants;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.Window;

/**
 * Record an extra payment (a locker fee, a balance, a training package ...). The payment is simulated:
 * no real payment gateway is involved. After saving, the receipt is shown.
 */
public class RecordPaymentDialog extends FormDialog {

    private static final long serialVersionUID = 1L;

    private final transient MemberService memberService = new MemberService();
    private final transient PaymentService paymentService = new PaymentService();
    private final transient Integer preselectMemberId;
    private final transient Runnable onSaved;

    private final JComboBox<Member> memberCombo = new JComboBox<>();
    private final JTextField amountField = UiFactory.textField(10);
    private final JComboBox<String> methodCombo = new JComboBox<>(Constants.PAYMENT_METHODS);
    private final JComboBox<String> statusCombo = new JComboBox<>(Constants.PAYMENT_STATUSES);
    private final JTextField notesField = UiFactory.textField(20);

    public RecordPaymentDialog(Window owner, Integer preselectMemberId, Runnable onSaved) {
        super(owner, "Record Payment", "Record money received from a member (simulated payment)", "Save Payment");
        this.preselectMemberId = preselectMemberId;
        this.onSaved = onSaved;
        build();
        BackgroundTask.run(this, memberService::getAllMembers, members -> {
            for (Member m : members) {
                memberCombo.addItem(m);
                if (preselectMemberId != null && m.getMemberId() == preselectMemberId) {
                    memberCombo.setSelectedItem(m);
                }
            }
        }, ex -> showError("Could not load the members: " + ex.getMessage()));
    }

    @Override
    protected JComponent buildForm() {
        memberCombo.setName("rp.member");
        amountField.setName("rp.amount");
        methodCombo.setName("rp.method");
        statusCombo.setName("rp.status");
        notesField.setName("rp.notes");
        UiFactory.styleCombo(memberCombo);
        UiFactory.styleCombo(methodCombo);
        UiFactory.styleCombo(statusCombo);

        FormPanel form = new FormPanel();
        form.addField("MEMBER", memberCombo);
        form.addFieldPair("AMOUNT (Rs.)", amountField, "PAYMENT METHOD", methodCombo);
        form.addField("STATUS", statusCombo);
        form.addField("NOTES (what is this payment for?)", notesField);
        return form;
    }

    @Override
    protected void onSave() {
        final Member member = (Member) memberCombo.getSelectedItem();
        if (member == null) {
            showError("Please choose a member.");
            return;
        }
        final double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim());   // a negative or zero amount is refused by the service
        } catch (NumberFormatException e) {
            showError("Amount must be a number, for example 750.");
            return;
        }
        final String method = (String) methodCombo.getSelectedItem();
        final String status = (String) statusCombo.getSelectedItem();
        final String notes = notesField.getText().trim();
        final Window owner = getOwner();

        runSave(() -> paymentService.recordPayment(member.getMemberId(), null, amount, method, status, notes), payment -> {
            dispose();
            onSaved.run();
            new ReceiptDialog(owner, payment).setVisible(true);
        });
    }
}
