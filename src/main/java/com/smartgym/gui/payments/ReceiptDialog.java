package com.smartgym.gui.payments;

import com.smartgym.exception.SmartGymException;
import com.smartgym.gui.common.BackgroundTask;
import com.smartgym.gui.common.RoundedButton;
import com.smartgym.gui.common.Theme;
import com.smartgym.gui.common.UiFactory;
import com.smartgym.model.Payment;
import com.smartgym.service.PaymentService;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterException;
import java.io.File;

/** Shows a payment receipt as text. It can be printed or saved to a .txt file. */
public class ReceiptDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final transient PaymentService paymentService = new PaymentService();
    private final transient Payment payment;
    private final JTextArea textArea = new JTextArea();

    public ReceiptDialog(Window owner, Payment payment) {
        super(owner, "Receipt " + payment.getReceiptNo(), ModalityType.APPLICATION_MODAL);
        this.payment = payment;

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(18, 22, 16, 22));
        root.add(UiFactory.label("Payment receipt", 20, true, Theme.TEXT), BorderLayout.NORTH);

        textArea.setText(paymentService.buildReceiptText(payment));
        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));      // fixed-width font keeps columns aligned
        textArea.setMargin(new java.awt.Insets(12, 14, 12, 14));
        textArea.setBackground(new Color(0xFFFDF7));
        JScrollPane scroll = UiFactory.scroll(textArea);
        scroll.setPreferredSize(new Dimension(580, 340));
        root.add(scroll, BorderLayout.CENTER);

        RoundedButton print = new RoundedButton("Print", Theme.ACCENT);
        RoundedButton save = RoundedButton.secondary("Save as file...");
        RoundedButton close = RoundedButton.secondary("Close");
        print.setName("receipt.print");
        save.setName("receipt.save");
        textArea.setName("receipt.text");
        close.setName("receipt.close");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(save);
        buttons.add(print);
        buttons.add(close);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);

        print.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                printReceipt();
            }
        });
        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveToFile();
            }
        });
        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(owner);
    }

    private void printReceipt() {
        try {
            textArea.print();                                   // opens the system print dialog
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "Could not print: " + ex.getMessage(), "Print", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveToFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(payment.getReceiptNo() + ".txt"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            final File file = chooser.getSelectedFile();
            BackgroundTask.run(this, () -> {
                paymentService.saveReceiptToFile(payment, file);
                return Boolean.TRUE;
            }, ok -> JOptionPane.showMessageDialog(this, "Receipt saved to\n" + file.getAbsolutePath(),
                    "Receipt", JOptionPane.INFORMATION_MESSAGE), ex -> {
                if (ex instanceof SmartGymException) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Receipt", JOptionPane.ERROR_MESSAGE);
                } else {
                    BackgroundTask.showError(this, ex);
                }
            });
        }
    }
}
