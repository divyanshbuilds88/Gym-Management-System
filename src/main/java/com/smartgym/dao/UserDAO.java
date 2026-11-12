package com.smartgym.dao;

import com.smartgym.exception.DAOException;
import com.smartgym.model.User;

import java.sql.Connection;

/** Database operations for the users (login accounts) table. */
public interface UserDAO {
    int insert(User user) throws DAOException;

    /** Same as insert but uses the caller's connection, so it can be part of a transaction. */
    int insert(Connection con, User user) throws DAOException;

    User findByUsername(String username) throws DAOException;

    User findById(int userId) throws DAOException;

    boolean usernameExists(String username) throws DAOException;

    boolean updateContact(int userId, String fullName, String email, String phone) throws DAOException;

    boolean updatePassword(int userId, String passwordHash, String salt) throws DAOException;

    boolean delete(int userId) throws DAOException;
}
