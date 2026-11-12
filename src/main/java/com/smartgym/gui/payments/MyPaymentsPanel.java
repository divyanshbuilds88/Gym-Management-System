package com.smartgym.gui.payments;

import com.smartgym.exception.InvalidInputException;
import com.smartgym.gui.common.BackgroundTask;
import com.smartgym.gui.common.Refreshable;
import com.smartgym.gui.common.RoundedButton;
import com.smartgym.gui.common.RoundedPanel;
import com.smartgym.gui.common.StatusCellRenderer;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.gui.dashboard.StatCard;
import com.smartgym.model.Payment;
import com.smartgym.service.PaymentService;
import com.smartgym.service.SessionManager;
import com.smartgym.util.Constants;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/** "My Payments" for a member: what they paid, what is still due, and their receipts. */
public class MyPaymentsPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final transient PaymentService service = new PaymentService();
    private final PaymentTableModel model = new PaymentTableModel(false);
    private final JTable table = UiFactory.createTable(model);
    private final StatCard paidCard = new StatCard("Total paid", Theme.SUCCESS, "payments");
    private final StatCard dueCard = new StatCard("Pending dues", Theme.AMBER, "attendance");
    private final RoundedButton receiptButton = new RoundedButton("View Receipt", Theme.ACCENT);

    public MyPaymentsPanel() {
        super(new BorderLayout(0, 16));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JPanel cards = new JPanel(new GridLayout(1, 2, 16, 0));
        cards.setOpaque(false);
        cards.add(paidCard);
        cards.add(dueCard);
        add(cards, BorderLayout.NORTH);

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(UiFactory.label("My payments", 15, true, Theme.TEXT), BorderLayout.WEST);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(receiptButton);
        head.add(buttons, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        UiFactory.styleTable(table);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(model.columnOf(6)).setCellRenderer(new StatusCellRenderer());
        UiFactory.setColumnWidths(table, 95, 110, 110, 100, 100, 260);
        card.add(UiFactory.scroll(table), BorderLayout.CENTER);
        card.add(UiFactory.muted("Pending dues can be paid at the front desk. Double-click a row to open its receipt."),
                BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);

        receiptButton.setEnabled(false);
        table.setName("myp.table");
        receiptButton.setName("myp.receipt");
        paidCard.setName("myp.paid");
        dueCard.setName("myp.due");

        receiptButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showReceipt();
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> receiptButton.setEnabled(table.getSelectedRow() >= 0));
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    showReceipt();
                }
            }
        });
    }

    @Override
    public void refreshData() {
        final Integer memberId = SessionManager.getInstance().getMemberId();
        BackgroundTask.run(this, () -> {
            if (memberId == null) {
                throw new InvalidInputException("No member profile is linked to this account.");
            }
            return service.getPaymentsOfMember(memberId);
        }, (List<Payment> list) -> {
            model.setPayments(list);
            double pending = 0;
            for (Payment p : list) {
                if (Constants.PENDING.equals(p.getStatus())) {
                    pending += p.getAmount();
                }
            }
            paidCard.setValue(String.format("Rs. %,.0f", PaymentService.totalPaid(list)));
            dueCard.setValue(String.format("Rs. %,.0f", pending));
        });
    }

    private void showReceipt() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            Payment p = model.getPayment(table.convertRowIndexToModel(row));
            new ReceiptDialog(SwingUtilities.getWindowAncestor(this), p).setVisible(true);
        }
    }
}
