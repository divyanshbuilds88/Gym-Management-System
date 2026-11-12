package com.smartgym.gui.payments;

import com.smartgym.gui.common.BackgroundTask;
import com.smartgym.gui.common.Refreshable;
import com.smartgym.gui.common.RoundedButton;
import com.smartgym.gui.common.StatusCellRenderer;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.gui.common.WrapLayout;
import com.smartgym.gui.dashboard.StatCard;
import com.smartgym.model.Payment;
import com.smartgym.service.PaymentService;
import com.smartgym.util.Constants;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Admin screen for payments: filter, search, record a payment, mark a pending payment as paid,
 * and open / print / save a receipt. The three cards always add up what is currently shown in the table.
 */
public class PaymentsPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;

    private final transient PaymentService service = new PaymentService();
    private final PaymentTableModel model = new PaymentTableModel(true);
    private final JTable table = UiFactory.createTable(model);
    private final transient TableRowSorter<PaymentTableModel> sorter = new TableRowSorter<>(model);

    private final StatCard collectedCard = new StatCard("Collected (shown)", Theme.SUCCESS, "payments");
    private final StatCard pendingCard = new StatCard("Pending dues (shown)", Theme.AMBER, "attendance");
    private final StatCard countCard = new StatCard("Payments shown", Theme.INFO, "reports");

    private final JTextField filterField = UiFactory.searchField("Search receipt, member or notes", 22);
    private final JComboBox<String> statusCombo = new JComboBox<>(new String[] {"All statuses", "Paid", "Pending", "Failed"});
    private final JComboBox<String> methodCombo = new JComboBox<>(new String[] {"All methods", "CASH", "CARD", "UPI", "NET_BANKING"});
    private final JLabel countLabel = UiFactory.muted(" ");

    private final RoundedButton recordButton = new RoundedButton("+  Record Payment", Theme.ACCENT);
    private final RoundedButton receiptButton = RoundedButton.secondary("View Receipt");
    private final RoundedButton paidButton = RoundedButton.secondary("Mark as Paid");
    private final RoundedButton refreshButton = RoundedButton.secondaryWithIcon("Refresh", "refresh");

    public PaymentsPanel() {
        super(new BorderLayout(0, 14));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        JPanel cards = new JPanel(new GridLayout(1, 3, 16, 0));
        cards.setOpaque(false);
        cards.add(collectedCard);
        cards.add(pendingCard);
        cards.add(countCard);
        top.add(cards, BorderLayout.NORTH);

        UiFactory.styleCombo(statusCombo);
        UiFactory.styleCombo(methodCombo);
        JPanel bar = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(false);
        bar.add(filterField);
        bar.add(statusCombo);
        bar.add(methodCombo);
        bar.add(recordButton);
        bar.add(receiptButton);
        bar.add(paidButton);
        bar.add(refreshButton);
        top.add(bar, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        UiFactory.styleTable(table);
        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(model.columnOf(6)).setCellRenderer(new StatusCellRenderer());
        UiFactory.setColumnWidths(table, 90, 105, 85, 150, 105, 125, 100, 220);
        add(UiFactory.scroll(table), BorderLayout.CENTER);
        add(countLabel, BorderLayout.SOUTH);

        table.setName("pay.table");
        filterField.setName("pay.filter");
        statusCombo.setName("pay.status");
        methodCombo.setName("pay.method");
        recordButton.setName("pay.record");
        receiptButton.setName("pay.receipt");
        paidButton.setName("pay.markPaid");
        collectedCard.setName("pay.collected");
        pendingCard.setName("pay.pending");

        wireEvents();
        updateButtons();
    }

    // ================================================================ events

    private void wireEvents() {
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
        statusCombo.addActionListener(e -> applyFilter());
        methodCombo.addActionListener(e -> applyFilter());

        recordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Payment p = selected();
                new RecordPaymentDialog(window(), p == null ? null : p.getMemberId(), PaymentsPanel.this::refreshData)
                        .setVisible(true);
            }
        });
        receiptButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showReceipt();
            }
        });
        paidButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                markPaid();
            }
        });
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshData();
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtons();
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    showReceipt();
                }
            }
        });
    }

    private void updateButtons() {
        Payment p = selected();
        receiptButton.setEnabled(p != null);
        paidButton.setEnabled(p != null && Constants.PENDING.equals(p.getStatus()));
    }

    // ================================================================ filtering and totals

    private void applyFilter() {
        final String status = ((String) statusCombo.getSelectedItem()).toUpperCase();
        final String method = (String) methodCombo.getSelectedItem();
        final String text = filterField.getText().trim().toLowerCase();
        final int receiptCol = model.columnOf(0);
        final int codeCol = model.columnOf(2);
        final int memberCol = model.columnOf(3);
        final int methodCol = model.columnOf(5);
        final int statusCol = model.columnOf(6);
        final int notesCol = model.columnOf(7);
        sorter.setRowFilter(new RowFilter<PaymentTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends PaymentTableModel, ? extends Integer> entry) {
                if (!status.startsWith("ALL") && !status.equals(entry.getStringValue(statusCol))) {
                    return false;
                }
                if (!method.startsWith("All") && !method.equals(entry.getStringValue(methodCol))) {
                    return false;
                }
                return text.isEmpty()
                        || entry.getStringValue(receiptCol).toLowerCase().contains(text)
                        || entry.getStringValue(codeCol).toLowerCase().contains(text)
                        || entry.getStringValue(memberCol).toLowerCase().contains(text)
                        || entry.getStringValue(notesCol).toLowerCase().contains(text);
            }
        });
        updateTotals();
        updateButtons();
    }

    /** Adds up the rows that are currently visible (after filtering). */
    private void updateTotals() {
        double collected = 0;
        double pending = 0;
        for (int i = 0; i < table.getRowCount(); i++) {
            Payment p = model.getPayment(table.convertRowIndexToModel(i));
            if (Constants.PAID.equals(p.getStatus())) {
                collected += p.getAmount();
            } else if (Constants.PENDING.equals(p.getStatus())) {
                pending += p.getAmount();
            }
        }
        collectedCard.setValue(String.format("Rs. %,.0f", collected));
        pendingCard.setValue(String.format("Rs. %,.0f", pending));
        countCard.setValue(String.valueOf(table.getRowCount()));
        countLabel.setText("Showing " + table.getRowCount() + " of " + model.getRowCount()
                + " payments   |   double-click a row to open its receipt");
    }

    // ================================================================ data and actions

    @Override
    public void refreshData() {
        BackgroundTask.run(this, service::getAllPayments, list -> {
            model.setPayments(list);
            applyFilter();
        });
    }

    private Window window() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private Payment selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : model.getPayment(table.convertRowIndexToModel(row));
    }

    private void showReceipt() {
        Payment p = selected();
        if (p != null) {
            new ReceiptDialog(window(), p).setVisible(true);
        }
    }

    /** Changes a PENDING payment to PAID, then shows the receipt of the paid payment. */
    private void markPaid() {
        final Payment p = selected();
        if (p == null) {
            return;
        }
        BackgroundTask.run(this, () -> {
            service.markAsPaid(p.getPaymentId());
            return service.getPayment(p.getPaymentId());               // reload to get the new status
        }, updated -> {
            refreshData();
            new ReceiptDialog(window(), updated).setVisible(true);
        });
    }
}
