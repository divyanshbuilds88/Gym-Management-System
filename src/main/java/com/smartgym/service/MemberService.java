package com.smartgym.service;

import com.smartgym.dao.MemberDAO;
import com.smartgym.dao.UserDAO;
import com.smartgym.dao.impl.MemberDAOImpl;
import com.smartgym.dao.impl.UserDAOImpl;
import com.smartgym.exception.DAOException;
import com.smartgym.exception.InvalidMemberException;
import com.smartgym.exception.SmartGymException;
import com.smartgym.exception.UnauthorizedException;
import com.smartgym.model.Member;
import com.smartgym.model.Role;
import com.smartgym.model.User;
import com.smartgym.util.ActivityLog;
import com.smartgym.util.Constants;
import com.smartgym.util.DatabaseConnection;
import com.smartgym.util.DbUtil;
import com.smartgym.util.PasswordUtil;
import com.smartgym.util.ValidationUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** Business rules for members. Admin writes; admin and trainers read; a member reads only themself. */
public class MemberService extends BaseService {

    private final MemberDAO memberDAO = new MemberDAOImpl();
    private final UserDAO userDAO = new UserDAOImpl();

    // ---------------------------------------------------------------- reading

    public List<Member> getAllMembers() throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        try {
            return memberDAO.findAll();
        } catch (DAOException e) {
            throw dbError("loading members", e);
        }
    }

    public List<Member> searchMembers(String keyword) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        try {
            return ValidationUtil.isBlank(keyword) ? memberDAO.findAll() : memberDAO.search(keyword);
        } catch (DAOException e) {
            throw dbError("searching members", e);
        }
    }

    public Member getMember(int memberId) throws SmartGymException {
        if (!session.isOwnMemberId(memberId)) {
            session.requireRole(Role.ADMIN, Role.TRAINER);
        }
        try {
            Member m = memberDAO.findById(memberId);
            if (m == null) {
                throw new InvalidMemberException("Member not found.");
            }
            return m;
        } catch (DAOException e) {
            throw dbError("loading member", e);
        }
    }

    public List<Member> getRecentMembers(int limit) throws SmartGymException {
        session.requireRole(Role.ADMIN, Role.TRAINER);
        try {
            return memberDAO.findRecent(limit);
        } catch (DAOException e) {
            throw dbError("loading recent members", e);
        }
    }

    // ---------------------------------------------------------------- adding

    /** Adds a member WITHOUT a login account (method overloading: see the next method). */
    public int addMember(Member member) throws SmartGymException {
        return addMember(member, null, null);
    }

    /**
     * Adds a member. If username/password are given, a MEMBER login account is created too.
     * Both inserts run in ONE transaction: either both are saved or neither.
     */
    public int addMember(Member member, String username, String password) throws SmartGymException {
        session.requireRole(Role.ADMIN);
        validate(member);
        boolean createLogin = !ValidationUtil.isBlank(username);
        if (createLogin) {
            if (!ValidationUtil.isValidUsername(username)) {
                throw new InvalidMemberException("Username must be 4-30 letters, digits or underscore.");
            }
            if (!ValidationUtil.isStrongPassword(password)) {
                throw new InvalidMemberException("Password must be at least 6 characters with a letter and a digit.");
            }
        }

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);                                   // BEGIN transaction

            if (createLogin) {
                String salt = PasswordUtil.generateSalt();
                User user = new User(username.trim(), PasswordUtil.hash(salt, password), salt, Role.MEMBER,
                        member.getFullName(), member.getEmail(), member.getPhone());
                member.setUserId(userDAO.insert(con, user));
            }
            member.setMemberCode(memberDAO.generateNextCode());
            int id = memberDAO.insert(con, member);

            con.commit();                                               // COMMIT
            member.setMemberId(id);
            ActivityLog.log("ADDED member " + member.getMemberCode() + " by " + session.getCurrentUser().getUsername());
            return id;
        } catch (SQLException e) {
            DbUtil.rollbackQuietly(con);
            throw new SmartGymException("Could not connect to the database. " + rootMessage(e), e);
        } catch (DAOException e) {
            DbUtil.rollbackQuietly(con);                                // ROLLBACK
            if (e.isDuplicateKey()) {
                throw new InvalidMemberException("A member or login with this phone, email or username already exists.");
            }
            throw dbError("adding member", e);
        } finally {
            DbUtil.closeQuietly(con);
        }
    }

    // ---------------------------------------------------------------- update / delete

    public void updateMember(Member member) throws SmartGymException {
        session.requireRole(Role.ADMIN);
        validate(member);
        try {
            if (!memberDAO.update(member)) {
                throw new InvalidMemberException("Member not found - it may have been deleted.");
            }
        } catch (DAOException e) {
            if (e.isDuplicateKey()) {
                throw new InvalidMemberException("Another member already uses this phone number or email.");
            }
            throw dbError("updating member", e);
        }
    }

    public void deleteMember(int memberId) throws SmartGymException {
        session.requireRole(Role.ADMIN);
        try {
            if (!memberDAO.delete(memberId)) {
                throw new InvalidMemberException("Member not found - it may already be deleted.");
            }
            ActivityLog.log("DELETED member id " + memberId + " by " + session.getCurrentUser().getUsername());
        } catch (DAOException e) {
            throw dbError("deleting member", e);
        }
    }

    // ---------------------------------------------------------------- validation

    /** Cleans (trims) and checks all member fields. */
    private void validate(Member m) throws InvalidMemberException {
        if (m == null) {
            throw new InvalidMemberException("No member data supplied.");
        }
        if (m.getFullName() != null) {
            m.setFullName(m.getFullName().trim());
        }
        if (m.getEmail() != null) {
            m.setEmail(m.getEmail().trim().toLowerCase());
        }
        if (m.getPhone() != null) {
            m.setPhone(m.getPhone().trim());
        }
        if (!ValidationUtil.isValidName(m.getFullName())) {
            throw new InvalidMemberException("Name must be 2-100 characters (letters and spaces only).");
        }
        if (!ValidationUtil.isValidAge(m.getAge())) {
            throw new InvalidMemberException("Age must be between 10 and 100.");
        }
        if (!ValidationUtil.isOneOf(m.getGender(), Constants.GENDERS)) {
            throw new InvalidMemberException("Please select a gender.");
        }
        if (!ValidationUtil.isValidPhone(m.getPhone())) {
            throw new InvalidMemberException("Phone must be a 10-digit mobile number starting with 6-9.");
        }
        if (!ValidationUtil.isValidEmail(m.getEmail())) {
            throw new InvalidMemberException("Please enter a valid email address.");
        }
        if (m.getAddress() != null && m.getAddress().length() > 255) {
            throw new InvalidMemberException("Address is too long (maximum 255 characters).");
        }
        if (!ValidationUtil.isNotInFuture(m.getJoiningDate())) {
            throw new InvalidMemberException("Joining date is required and cannot be in the future.");
        }
    }

    /** Used by services that need a member to exist; throws if missing. */
    Member requireMember(int memberId) throws SmartGymException {
        try {
            Member m = memberDAO.findById(memberId);
            if (m == null) {
                throw new InvalidMemberException("Member not found.");
            }
            return m;
        } catch (DAOException e) {
            throw dbError("loading member", e);
        }
    }

    /** Throws UnauthorizedException unless admin/trainer or the member's own id. */
    void requireAdminTrainerOrSelf(int memberId) throws UnauthorizedException {
        if (!session.isOwnMemberId(memberId)) {
            session.requireRole(Role.ADMIN, Role.TRAINER);
        }
    }
}
