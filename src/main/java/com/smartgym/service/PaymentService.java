package com.smartgym.service;

import com.smartgym.dao.MemberDAO;
import com.smartgym.dao.PaymentDAO;
import com.smartgym.dao.impl.MemberDAOImpl;
import com.smartgym.dao.impl.PaymentDAOImpl;
import com.smartgym.exception.DAOException;
import com.smartgym.exception.InvalidMemberException;
import com.smartgym.exception.InvalidPaymentException;
import com.smartgym.exception.SmartGymException;
import com.smartgym.exception.UnauthorizedException;
import com.smartgym.model.Member;
import com.smartgym.model.Payment;
import com.smartgym.model.Role;
import com.smartgym.util.ActivityLog;
import com.smartgym.util.Constants;
import com.smartgym.util.ValidationUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Simulated payments (no real gateway), pending dues and printable receipts. */
public class PaymentService extends BaseService {

    private static final int RECEIPT_WIDTH = 46;

    private final PaymentDAO paymentDAO = new PaymentDAOImpl();
    private final MemberDAO memberDAO = new MemberDAOImpl();

    /** Records an extra payment (for example a pending balance paid later). */
    public Payment recordPayment(int memberId, Integer membershipId, double amount, String method,
                                 String status, String notes) throws SmartGymException {
        session.requireRole(Role.ADMIN);
        if (!ValidationUtil.isPositive(amount) || amount > 1_000_000) {
            throw new InvalidPaymentException("Amount must be greater than 0 and at most 1,000,000.");
        }
        if (!ValidationUtil.isOneOf(method, Constants.PAYMENT_METHODS)) {
            throw new InvalidPaymentException("Please choose a payment method.");
        }
        if (!ValidationUtil.isOneOf(status, Constants.PAYMENT_STATUSES)) {
            throw new InvalidPaymentException("Please choose a payment status.");
        }
        if (notes != null && notes.length() > 255) {
            throw new InvalidPaymentException("Notes are too long (maximum 255 characters).");
        }
        try {
            Member member = memberDAO.findById(memberId);
            if (member == null) {
                throw new InvalidMemberException("Member not found.");
            }
            Payment p = new Payment();
            p.setReceiptNo(paymentDAO.generateReceiptNo());
            p.setMemberId(memberId);
            p.setMembershipId(membershipId);
            p.setAmount(Math.round(amount * 100) / 100.0);           // 2 decimals
            p.setPaymentDate(LocalDate.now());
            p.setMethod(method);
            p.setStatus(status);
            p.setNotes(notes);
            p.setPaymentId(paymentDAO.insert(p));
            p.setMemberName(member.getFullName());
            p.setMemberCode(member.getMemberCode());
            ActivityLog.log("PAYMENT " + p.getReceiptNo() + " " + p.getAmount() + " " + p.getStatus());
            return p;
        } catch (DAOException e) {
            throw dbError("saving payment", e);
        }
    }

    /** Changes a PENDING payment to PAID. */
    public void markAsPaid(int paymentId) throws SmartGymException {
        session.requireRole(Role.ADMIN);
        try {
            Payment p = paymentDAO.findById(paymentId);
            if (p == null) {
                throw new InvalidPaymentException("Payment not found.");
            }
            if (Constants.PAID.equals(p.getStatus())) {
                throw new InvalidPaymentException("This payment is already marked as PAID.");
            }
            paymentDAO.updateStatus(paymentId, Constants.PAID);
            ActivityLog.log("PAYMENT " + p.getReceiptNo() + " marked PAID");
        } catch (DAOException e) {
            throw dbError("updating payment", e);
        }
    }

    public List<Payment> getAllPayments() throws SmartGymException {
        session.requireRole(Role.ADMIN);
        try {
            return paymentDAO.findAll();
        } catch (DAOException e) {
            throw dbError("loading payments", e);
        }
    }

    public List<Payment> getPendingPayments() throws SmartGymException {
        session.requireRole(Role.ADMIN);
        try {
            return paymentDAO.findByStatus(Constants.PENDING);
        } catch (DAOException e) {
            throw dbError("loading pending payments", e);
        }
    }

    public List<Payment> getPaymentsOfMember(int memberId) throws SmartGymException {
        if (!session.isOwnMemberId(memberId)) {
            session.requireRole(Role.ADMIN);
        }
        try {
            return paymentDAO.findByMember(memberId);
        } catch (DAOException e) {
            throw dbError("loading payment history", e);
        }
    }

    public Payment getPayment(int paymentId) throws SmartGymException {
        try {
            Payment p = paymentDAO.findById(paymentId);
            if (p == null) {
                throw new InvalidPaymentException("Payment not found.");
            }
            if (!session.isOwnMemberId(p.getMemberId())) {
                session.requireRole(Role.ADMIN);
            }
            return p;
        } catch (DAOException e) {
            throw dbError("loading payment", e);
        }
    }

    /** Adds up the amounts of PAID payments in a list. */
    public static double totalPaid(List<Payment> payments) {
        double total = 0;
        for (Payment p : payments) {
            if (Constants.PAID.equals(p.getStatus())) {
                total += p.getAmount();
            }
        }
        return Math.round(total * 100) / 100.0;
    }

    // ================================================================ receipts

    /** Builds the plain-text receipt that the GUI shows and prints. */
    public String buildReceiptText(Payment p) {
        StringBuilder sb = new StringBuilder();
        String line = "=".repeat(RECEIPT_WIDTH);
        String thin = "-".repeat(RECEIPT_WIDTH);
        sb.append(line).append('\n');
        sb.append(center("SMART GYM")).append('\n');
        sb.append(center("PAYMENT RECEIPT")).append('\n');
        sb.append(line).append('\n');
        sb.append(String.format("%-14s: %s%n", "Receipt No", p.getReceiptNo()));
        sb.append(String.format("%-14s: %s%n", "Date",
                p.getPaymentDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))));
        sb.append(thin).append('\n');
        sb.append(String.format("%-14s: %s (%s)%n", "Member", p.getMemberName(), p.getMemberCode()));
        sb.append(String.format("%-14s: %s%n", "Details", p.getNotes() == null ? "-" : p.getNotes()));
        sb.append(String.format("%-14s: %s%n", "Method", p.getMethod()));
        sb.append(String.format("%-14s: %s%n", "Status", p.getStatus()));
        sb.append(thin).append('\n');
        sb.append(String.format("%-14s: Rs. %.2f%n", "AMOUNT", p.getAmount()));
        sb.append(line).append('\n');
        sb.append(center("Thank you! Stay fit.")).append('\n');
        sb.append(center("(Simulated payment - not a tax invoice)")).append('\n');
        return sb.toString();
    }

    private String center(String text) {
        int pad = Math.max(0, (RECEIPT_WIDTH - text.length()) / 2);
        return " ".repeat(pad) + text;
    }

    /** FILE WRITING: saves the receipt to a .txt file chosen by the user. */
    public void saveReceiptToFile(Payment p, File file) throws SmartGymException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write(buildReceiptText(p));
        } catch (IOException e) {
            throw new SmartGymException("Could not save the receipt file: " + e.getMessage(), e);
        }
    }

    /** Needed by the GUI to know if a viewer may see this payment. */
    void requireOwnerOrAdmin(int memberId) throws UnauthorizedException {
        if (!session.isOwnMemberId(memberId)) {
            session.requireRole(Role.ADMIN);
        }
    }
}
