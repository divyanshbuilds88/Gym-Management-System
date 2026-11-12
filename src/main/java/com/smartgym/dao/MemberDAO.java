package com.smartgym.dao;

import com.smartgym.exception.DAOException;
import com.smartgym.model.Member;

import java.sql.Connection;
import java.util.List;

public interface MemberDAO extends CrudDAO<Member> {

    int insert(Connection con, Member member) throws DAOException;

    /** Searches name, member code, phone and email. */
    List<Member> search(String keyword) throws DAOException;

    Member findByCode(String memberCode) throws DAOException;

    Member findByUserId(int userId) throws DAOException;

    /** Next free member code, for example GYM0006. */
    String generateNextCode() throws DAOException;

    List<Member> findRecent(int limit) throws DAOException;

    boolean updateContact(int memberId, String phone, String email, String address) throws DAOException;
}
