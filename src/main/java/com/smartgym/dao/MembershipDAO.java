package com.smartgym.dao;

import com.smartgym.exception.DAOException;
import com.smartgym.model.Membership;

import java.sql.Connection;
import java.util.List;

public interface MembershipDAO {

    /** Uses the caller's connection so it can join a transaction (membership + payment). */
    int insert(Connection con, Membership membership) throws DAOException;

    Membership findById(int membershipId) throws DAOException;

    List<Membership> findAll() throws DAOException;

    List<Membership> findByMember(int memberId) throws DAOException;

    /** The most recent membership of a member, or null if the member never bought one. */
    Membership findLatestByMember(int memberId) throws DAOException;

    /** Memberships that end within the next 'days' days (used by the notification thread). */
    List<Membership> findExpiringWithin(int days) throws DAOException;

    boolean updateStatus(int membershipId, String status) throws DAOException;

    /** Calls the stored procedure sp_expire_memberships (CallableStatement). Returns rows changed. */
    int expireOverdueMemberships() throws DAOException;
}
