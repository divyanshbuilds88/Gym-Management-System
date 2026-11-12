package com.smartgym.dao;

import com.smartgym.exception.DAOException;
import com.smartgym.model.Payment;

import java.sql.Connection;
import java.util.List;

public interface PaymentDAO {
    int insert(Connection con, Payment payment) throws DAOException;

    int insert(Payment payment) throws DAOException;

    Payment findById(int paymentId) throws DAOException;

    List<Payment> findAll() throws DAOException;

    List<Payment> findByMember(int memberId) throws DAOException;

    List<Payment> findByStatus(String status) throws DAOException;

    boolean updateStatus(int paymentId, String status) throws DAOException;

    /** Next receipt number, for example RCP-0006. */
    String generateReceiptNo(Connection con) throws DAOException;

    String generateReceiptNo() throws DAOException;
}
