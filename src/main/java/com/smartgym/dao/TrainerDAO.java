package com.smartgym.dao;

import com.smartgym.exception.DAOException;
import com.smartgym.model.Trainer;

import java.sql.Connection;
import java.util.List;

public interface TrainerDAO extends CrudDAO<Trainer> {

    int insert(Connection con, Trainer trainer) throws DAOException;

    List<Trainer> search(String keyword) throws DAOException;

    Trainer findByUserId(int userId) throws DAOException;

    boolean updateContact(int trainerId, String phone, String email) throws DAOException;
}
