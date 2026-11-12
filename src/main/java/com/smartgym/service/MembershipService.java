package com.smartgym.service;

import com.smartgym.dao.MemberDAO;
import com.smartgym.dao.MembershipDAO;
import com.smartgym.dao.MembershipPlanDAO;
import com.smartgym.dao.PaymentDAO;
import com.smartgym.dao.impl.MemberDAOImpl;
import com.smartgym.dao.impl.MembershipDAOImpl;
import com.smartgym.dao.impl.MembershipPlanDAOImpl;
import com.smartgym.dao.impl.PaymentDAOImpl;
import com.smartgym.exception.DAOException;
import com.smartgym.exception.InvalidInputException;
import com.smartgym.exception.InvalidMemberException;
import com.smartgym.exception.InvalidPaymentException;
import com.smartgym.exception.MembershipExpiredException;
import com.smartgym.exception.SmartGymException;
import com.smartgym.model.Member;
import com.smartgym.model.Membership;
import com.smartgym.model.MembershipPlan;
import com.smartgym.model.Payment;
import com.smartgym.model.Role;
import com.smartgym.util.ActivityLog;
import com.smartgym.util.Constants;
import com.smartgym.util.DatabaseConnection;
import com.smartgym.util.DbUtil;
import com.smartgym.util.ValidationUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Membership plans, buying / renewing memberships, expiry tracking. */
public class MembershipService extends BaseService {

    private final MembershipPlanDAO planDAO = new MembershipPlanDAOImpl();
    private final MembershipDAO membershipDAO = new MembershipDAOImpl();
    private final MemberDAO memberDAO = new MemberDAOImpl();
    private final PaymentDAO paymentDAO = new PaymentDAOImpl();

    // ================================================================ plans

    public List<MembershipPlan> getAllPlans() throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER, Role.MEMBER);
        try {
            return planDAO.findAll();
        } catch (DAOException e) {
            throw dbError("loading plans", e);
        }
    }

    public List<MembershipPlan> getActivePlans() throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER, Role.MEMBER);
        try {
            return planDAO.findAllActive();
        } catch (DAOException e) {
            throw dbError("loading plans", e);
        }
    }

    public int addPlan(MembershipPlan plan) throws SmartGymException {
        session.requireRole(Role.ADMIN);
        validatePlan(plan);
        try {
            return planDAO.insert(plan);
        } catch (DAOException e) {
            if (e.isDuplicateKey()) {
                throw new InvalidInputException("A plan named '" + plan.getPlanName() + "' already exists.");
            }
            throw dbError("adding plan", e);
        }
    }

    public void updatePlan(MembershipPlan plan) throws SmartGymException {
        session.requireRole(Role.ADMIN);
        validatePlan(plan);
        try {
            if (!planDAO.update(plan)) {
                throw new InvalidInputException("Plan not found.");
            }
        } catch (DAOException e) {
            if (e.isDuplicateKey()) {
                throw new InvalidInputException("A plan named '" + plan.getPlanName() + "' already exists.");
            }
            throw dbError("updating plan", e);
        }
    }

    public void deletePlan(int planId) throws SmartGymException {
        session.requireRole(Role.ADMIN);
        try {
            if (!planDAO.delete(planId)) {
                throw new InvalidInputException("Plan not found.");
            }
        } catch (DAOException e) {
            if (e.isForeignKeyViolation()) {
                throw new InvalidInputException("This plan is used by existing memberships. Deactivate it instead of deleting it.");
            }
            throw dbError("deleting plan", e);
        }
    }

    private void validatePlan(MembershipPlan p) throws InvalidInputException {
        if (p == null || ValidationUtil.isBlank(p.getPlanName()) || p.getPlanName().trim().length() > 50) {
            throw new InvalidInputException("Plan name is required (maximum 50 characters).");
        }
        p.setPlanName(p.getPlanName().trim());
        if (!ValidationUtil.isInRange(p.getDurationMonths(), 1, 60)) {
            throw new InvalidInputException("Duration must be between 1 and 60 months.");
        }
        if (!ValidationUtil.isPositive(p.getFee()) || p.getFee() > 1_000_000) {
            throw new InvalidInputException("Fee must be greater than 0 and at most 1,000,000.");
        }
        p.setFee(Math.round(p.getFee() * 100) / 100.0);          // round to 2 decimals (Math class)
        if (p.getDescription() != null && p.getDescription().length() > 255) {
            throw new InvalidInputException("Description is too long (maximum 255 characters).");
        }
    }

    // ================================================================ buying / renewing

    /** Expiry date = start date + plan duration. */
    public static LocalDate calculateExpiry(LocalDate start, int durationMonths) {
        return start.plusMonths(durationMonths);
    }

    /** Assigns a plan to a member starting on a chosen date. Returns the payment (receipt) that was created. */
    public Payment assignMembership(int memberId, int planId, LocalDate startDate, String method, boolean paidNow)
            throws SmartGymException {
        session.requireRole(Role.ADMIN);
        if (startDate == null) {
            throw new InvalidInputException("Start date is required.");
        }
        return buy(memberId, planId, startDate, method, paidNow);
    }

    /**
     * Renews a membership. If the member is still active, the new period starts when the
     * current one ends (no days are lost); otherwise it starts today.
     */
    public Payment renewMembership(int memberId, int planId, String method, boolean paidNow) throws SmartGymException {
        session.requireRole(Role.ADMIN);
        LocalDate start = LocalDate.now();
        try {
            for (Membership ms : membershipDAO.findByMember(memberId)) {
                if (Constants.ACTIVE.equals(ms.getStatus()) && ms.getExpiryDate().isAfter(start)) {
                    start = ms.getExpiryDate();                       // continue after the latest active period
                }
            }
        } catch (DAOException e) {
            throw dbError("checking current membership", e);
        }
        return buy(memberId, planId, start, method, paidNow);
    }

    /** One TRANSACTION: save the membership and its payment together. */
    private Payment buy(int memberId, int planId, LocalDate start, String method, boolean paidNow)
            throws SmartGymException {
        if (!ValidationUtil.isOneOf(method, Constants.PAYMENT_METHODS)) {
            throw new InvalidPaymentException("Please choose a payment method.");
        }
        Connection con = null;
        try {
            Member member = memberDAO.findById(memberId);
            if (member == null) {
                throw new InvalidMemberException("Member not found.");
            }
            MembershipPlan plan = planDAO.findById(planId);
            if (plan == null || !plan.isActive()) {
                throw new InvalidInputException("This plan is not available.");
            }
            if (!ValidationUtil.isPositive(plan.getFee())) {
                throw new InvalidPaymentException("Plan fee must be greater than zero.");
            }

            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            Membership ms = new Membership();
            ms.setMemberId(memberId);
            ms.setPlanId(planId);
            ms.setStartDate(start);
            ms.setExpiryDate(calculateExpiry(start, plan.getDurationMonths()));
            ms.setStatus(Constants.ACTIVE);
            int membershipId = membershipDAO.insert(con, ms);

            Payment pay = new Payment();
            pay.setReceiptNo(paymentDAO.generateReceiptNo(con));
            pay.setMemberId(memberId);
            pay.setMembershipId(membershipId);
            pay.setAmount(plan.getFee());
            pay.setPaymentDate(LocalDate.now());
            pay.setMethod(method);
            pay.setStatus(paidNow ? Constants.PAID : Constants.PENDING);
            pay.setNotes(plan.getPlanName() + " plan (" + start + " to " + ms.getExpiryDate() + ")");
            pay.setPaymentId(paymentDAO.insert(con, pay));
            pay.setMemberName(member.getFullName());
            pay.setMemberCode(member.getMemberCode());

            con.commit();
            ActivityLog.log("MEMBERSHIP " + plan.getPlanName() + " for " + member.getMemberCode()
                    + " receipt " + pay.getReceiptNo());
            return pay;
        } catch (SQLException e) {
            DbUtil.rollbackQuietly(con);
            throw new SmartGymException("Could not connect to the database. " + rootMessage(e), e);
        } catch (DAOException e) {
            DbUtil.rollbackQuietly(con);
            throw dbError("saving membership", e);
        } catch (SmartGymException e) {
            DbUtil.rollbackQuietly(con);
            throw e;                                                 // validation errors pass through unchanged
        } finally {
            DbUtil.closeQuietly(con);
        }
    }

    // ================================================================ checking / tracking

    /**
     * Throws MembershipExpiredException unless the member has at least one ACTIVE, unexpired membership.
     * Used before check-in and before booking a trainer session.
     */
    public void requireActiveMembership(int memberId) throws SmartGymException {
        try {
            List<Membership> list = membershipDAO.findByMember(memberId);
            if (list.isEmpty()) {
                throw new MembershipExpiredException("This member has no membership. Please buy a plan first.");
            }
            for (Membership ms : list) {
                if (Constants.ACTIVE.equals(ms.getStatus())) {
                    return;                                          // found one valid membership
                }
            }
            throw new MembershipExpiredException("Membership expired on "
                    + list.get(0).getExpiryDate() + ". Please renew first.");
        } catch (DAOException e) {
            throw dbError("checking membership", e);
        }
    }

    public List<Membership> getAllMemberships() throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        try {
            return membershipDAO.findAll();
        } catch (DAOException e) {
            throw dbError("loading memberships", e);
        }
    }

    public List<Membership> getMembershipsOfMember(int memberId) throws SmartGymException {
        if (!session.isOwnMemberId(memberId)) {
            session.requireRole(Role.ADMIN, Role.TRAINER);
        }
        try {
            return membershipDAO.findByMember(memberId);
        } catch (DAOException e) {
            throw dbError("loading memberships", e);
        }
    }

    public List<Membership> getExpiringSoon(int days) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        try {
            return membershipDAO.findExpiringWithin(days);
        } catch (DAOException e) {
            throw dbError("loading expiring memberships", e);
        }
    }

    public void cancelMembership(int membershipId) throws SmartGymException {
        session.requireRole(Role.ADMIN);
        try {
            if (!membershipDAO.updateStatus(membershipId, Constants.CANCELLED)) {
                throw new InvalidInputException("Membership not found.");
            }
        } catch (DAOException e) {
            throw dbError("cancelling membership", e);
        }
    }

    /** Runs the stored procedure that marks overdue memberships as EXPIRED. Returns how many changed. */
    public int expireOverdueMemberships() throws SmartGymException {
        session.requireRole(Role.ADMIN);
        try {
            return membershipDAO.expireOverdueMemberships();
        } catch (DAOException e) {
            throw dbError("expiring memberships", e);
        }
    }
}
